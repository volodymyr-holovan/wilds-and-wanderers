package ua.wilds.core.npc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import ua.wilds.core.net.Net;
import ua.wilds.core.quests.Quests;
import ua.wilds.core.skills.Perks;
import ua.wilds.core.skills.SkillState;
import ua.wilds.core.skills.SkillXp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Conversations with quest givers. The server decides everything: what the NPC says and which answers the player has,
 * from the giver's quests and this player's progress with them; the client only shows the page (see DialogueScreen)
 * and sends back the number of the answer. Item and creature names travel as translatable parts, so every player
 * reads them in their own language.
 * <p>
 * Everything is per player (player data "wilds_core:npc"): one giver offers the same jobs to every player, and each
 * player takes, does and is paid for them separately. Per quest: state (new, declined, accepted, done, failed), who
 * gave it, whether the player asked each question, tried to haggle or lie. Per giver ("@" + persona): whether they
 * stopped trusting the player (after a lie that failed) and from which job on.
 * <p>
 * A giver with a chain gives the next job only after the previous one is done. Answers: questions about the story,
 * accept, decline, haggle for a better reward (once per job; the chance grows with the diplomat skills), lie that the
 * job is done (only with the skill «Красномовство», only in the most valuable jobs, a small chance; on failure the
 * giver no longer trusts the player: the current job can still be finished, but no further ones are given),
 * turn the job in, give up a taken job, goodbye.
 */
public final class Dialogue {
    public static final int NEW = 0, DECLINED = 1, ACCEPTED = 2, DONE = 3, FAILED = 4;
    /** what a player sees above a giver: nothing, a job to take, a job done (come for the reward), a job in progress */
    public static final int MARK_NONE = 0, MARK_NEW = 1, MARK_READY = 2, MARK_WORKING = 3;
    private static final String DATA = "wilds_core:npc";
    private static final Map<UUID, Session> sessions = new HashMap<>();

    private record Option(String label, String action) {}

    private static final class Session {
        final Wanderer npc;
        List<Option> options = new ArrayList<>();

        Session(Wanderer npc) {
            this.npc = npc;
        }
    }

    private Dialogue() {}

    public static void init() {
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 10 != 0) return;
            Session s = sessions.get(p.getUUID());
            if (s != null && (!s.npc.isAlive() || s.npc.distanceToSqr(p) > 100 || s.npc.level() != p.level())) close(p);
        });
    }

    // ---------------------------------------------------------------- player data

    private static CompoundTag all(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag kept = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (!kept.contains(DATA)) kept.put(DATA, new CompoundTag());
        return kept.getCompound(DATA);
    }

    private static CompoundTag of(Player p, String key) {
        CompoundTag a = all(p);
        if (!a.contains(key)) a.put(key, new CompoundTag());
        return a.getCompound(key);
    }

    public static int state(Player p, String key) {
        CompoundTag a = all(p);
        return a.contains(key) ? a.getCompound(key).getInt("s") : NEW;
    }

    static UUID giver(Player p, String key) {
        CompoundTag a = all(p);
        if (!a.contains(key)) return null;
        CompoundTag d = a.getCompound(key);
        return d.hasUUID("npc") ? d.getUUID("npc") : null;
    }

    private static void setState(Player p, String key, int s) {
        of(p, key).putInt("s", s);
    }

    /** the player has talked to a quest giver at least once */
    static boolean hasMet(Player p) {
        return all(p).getBoolean("@met");
    }

    /** the stage from which on the giver no longer trusts this player (0: trusts) */
    private static int distrust(Player p, String persona) {
        CompoundTag a = all(p);
        return a.contains("@" + persona) ? a.getCompound("@" + persona).getInt("distrust") : 0;
    }

    public static boolean busy(ServerPlayer p) {
        return sessions.containsKey(p.getUUID());
    }

    /**
     * the giver's job this player is at: the first one of the chain not done yet; null when every job is done, one
     * failed (the giver died while the player had it), or the giver no longer trusts the player
     */
    static NpcQuest current(Player p, String persona) {
        int distrust = distrust(p, persona);
        for (NpcQuest q : NpcQuest.chain(persona)) {
            int s = state(p, q.key);
            if (s == DONE) continue;
            if (s == FAILED || (distrust > 0 && q.stage > distrust)) return null;
            return q;
        }
        return null;
    }

    /** the sign this player sees above the giver */
    static int mark(ServerPlayer p, Wanderer npc) {
        NpcQuest q = current(p, npc.questKey());
        if (q == null) return MARK_NONE;
        int s = state(p, q.key);
        if (s != ACCEPTED) return MARK_NEW;
        if (!npc.getUUID().equals(giver(p, q.key))) return MARK_NONE;   // promised to the same persona standing elsewhere
        return finished(p, q) ? MARK_READY : MARK_WORKING;
    }

    /** the NPC starts a conversation by itself only when there is something to say */
    static boolean wantsToTalk(ServerPlayer p, Wanderer npc) {
        NpcQuest q = current(p, npc.questKey());
        if (q == null) return false;
        int s = state(p, q.key);
        if (s == NEW) return true;
        return s == ACCEPTED && npc.getUUID().equals(giver(p, q.key)) && finished(p, q);
    }

    // ---------------------------------------------------------------- the conversation

    static void open(ServerPlayer p, Wanderer npc) {
        if (npc.persona() == null) return;
        sessions.put(p.getUUID(), new Session(npc));
        npc.setTalking(true);
        npc.getLookControl().setLookAt(p);
        all(p).putBoolean("@met", true);
        Quests.tutorial(p, "talk_npc");
        main(p, true);
    }

    public static void close(ServerPlayer p) {
        Session s = sessions.remove(p.getUUID());
        if (s != null) {
            boolean other = sessions.values().stream().anyMatch(x -> x.npc == s.npc);
            if (!other) s.npc.setTalking(false);
            NpcWorld.syncMarks(p, true);
        }
        try {
            PacketDistributor.sendToPlayer(p, new Net.DialoguePage(0, ""));
        } catch (RuntimeException ignored) {
        }
    }

    /** the player picked answer number i */
    public static void choose(ServerPlayer p, int i) {
        Session s = sessions.get(p.getUUID());
        if (s == null || i < 0 || i >= s.options.size()) return;
        NpcQuest q = current(p, s.npc.questKey());
        String action = s.options.get(i).action;
        if (q == null && action.equals("rumor")) {
            List<String> r = NpcQuest.rumors();
            NpcQuest any = s.npc.persona();
            if (any != null && !r.isEmpty()) page(p, s, any, text(r.get(p.getRandom().nextInt(r.size()))), List.of(new Option("Дякую. Бувай.", "end")));
            else close(p);
            return;
        }
        if (q == null && !action.equals("end")) {
            main(p, false);
            return;
        }
        switch (action) {
            case "end" -> close(p);
            case "back" -> main(p, false);
            case "accept" -> accept(p, s, q);
            case "decline" -> {
                setState(p, q.key, DECLINED);
                page(p, s, q, text(line(q, "decline", "Шкода. Якщо передумаєте — я тут.")), List.of(new Option("Бувай.", "end")));
            }
            case "abandon" -> {
                setState(p, q.key, DECLINED);
                Quests.withdraw(p, q.key);
                page(p, s, q, text("Що ж... Мабуть, я надто багато просив. Якщо передумаєте — приходьте."), List.of(new Option("Бувай.", "end")));
            }
            case "lie" -> lie(p, s, q);
            case "haggle" -> haggle(p, s, q);
            case "turn_in" -> turnIn(p, s, q);
            case "rumor" -> {
                List<String> r = NpcQuest.rumors();
                String words = r.isEmpty() ? "Тихо нині в окрузі." : r.get(p.getRandom().nextInt(r.size()));
                page(p, s, q, text(words), List.of(new Option("Цікаво. Повернімося до справи.", "back"), new Option("Дякую. Бувай.", "end")));
            }
            default -> {
                if (action.startsWith("qa:")) {
                    int n = Integer.parseInt(action.substring(3));
                    if (n >= 0 && n < q.questions.size()) {
                        of(p, q.key).putBoolean("asked" + n, true);
                        page(p, s, q, text(q.questions.get(n)[1]), List.of(new Option("Розумію. Повернімося до справи.", "back")));
                    }
                }
            }
        }
    }

    private static String line(NpcQuest q, String k, String def) {
        String v = q.line(k);
        return v.isEmpty() ? def : v;
    }

    private static MutableComponent text(String s) {
        return Component.literal(s);
    }

    private static void main(ServerPlayer p, boolean first) {
        Session s = sessions.get(p.getUUID());
        if (s == null) return;
        String persona = s.npc.questKey();
        NpcQuest q = current(p, persona);
        if (q == null) {
            List<NpcQuest> chain = NpcQuest.chain(persona);
            NpcQuest last = chain.isEmpty() ? s.npc.persona() : chain.get(chain.size() - 1);
            String words = distrust(p, persona) > 0 && chain.stream().anyMatch(c -> state(p, c.key) != DONE)
                    ? "Ми з вами вже все сказали одне одному. Іншої роботи для вас у мене нема."
                    : line(last, "after", "Дякую ще раз, мандрівнику. Нехай дорога буде легкою.");
            page(p, s, last, text(words), List.of(new Option("Що чути в окрузі?", "rumor"), new Option("І тобі всього доброго.", "end")));
            return;
        }
        int st = state(p, q.key);
        CompoundTag d = of(p, q.key);
        List<Option> opts = new ArrayList<>();
        MutableComponent body = Component.empty();
        if (st == ACCEPTED && !s.npc.getUUID().equals(giver(p, q.key))) {
            // the same persona standing somewhere else: the job is already promised there
            page(p, s, q, text("Ви вже пообіцяли цю справу комусь іншому. Спершу закінчіть там."), List.of(new Option("Бувай.", "end")));
            return;
        }
        if (st == ACCEPTED) {
            if (finished(p, q)) {
                body.append(line(q, "done", "Ви повернулися! Невже вдалося?"));
                opts.add(new Option(turnInLabel(q), "turn_in"));
            } else {
                body.append(line(q, "remind", "Як просувається справа?")).append("\n\n").append(taskText(q).withStyle(ChatFormatting.GRAY));
                if (canLie(p, q)) opts.add(new Option(lieLabel(p, "Я вже все зробив."), "lie"));
                opts.add(new Option("Ще працюю над цим.", "end"));
                opts.add(new Option("Я передумав. (Відмовитися)", "abandon"));
            }
        } else {
            // the story is told the first time this job comes up (also when it follows the previous one at once)
            boolean tell = first || !d.getBoolean("told");
            d.putBoolean("told", true);
            body.append(tell ? line(q, "greet", "Доброго дня, мандрівнику.") : "Отже?");
            if (tell) for (String para : q.story) body.append("\n\n").append(para);
            body.append("\n\n").append(line(q, "ask", "Допоможете мені?"));
            body.append("\n\n").append(taskText(q).withStyle(ChatFormatting.GRAY));
            body.append("\n").append(rewardText(q, d.getBoolean("haggled")));
            for (int i = 0; i < q.questions.size(); i++) {
                if (!d.getBoolean("asked" + i)) opts.add(new Option(q.questions.get(i)[0], "qa:" + i));
            }
            opts.add(new Option("Добре, я візьмуся.", "accept"));
            if (!NpcQuest.rumors().isEmpty()) opts.add(new Option("Що чути в окрузі?", "rumor"));
            if (q.canHaggle && !d.getBoolean("haggle_tried")) opts.add(new Option(chanceLabel("За таку справу слід заплатити більше. (Торгуватися)", haggleChance(p)), "haggle"));
            if (canLie(p, q)) opts.add(new Option(lieLabel(p, "Таке я вже робив — плати одразу."), "lie"));
            opts.add(new Option("Ні, це не для мене.", "decline"));
        }
        page(p, s, q, body, opts);
    }

    private static String turnInLabel(NpcQuest q) {
        return q.taskType.equals("item") ? "Ось, тримайте. (Віддати)" : "Справу зроблено. (Здати)";
    }

    /** haggling: open to everyone, better with the diplomat skills */
    private static double haggleChance(ServerPlayer p) {
        return Math.min(0.75, 0.25 + SkillState.perk(p, Perks.PERSUASION) + 0.15 * SkillState.level(p, "silver_tongue"));
    }

    /** lying: a small chance, only with «Красномовство» and only in the most valuable jobs */
    private static double lieChance(ServerPlayer p) {
        return Math.min(0.35, 0.08 + 0.5 * SkillState.perk(p, Perks.PERSUASION) + 0.07 * SkillState.level(p, "silver_tongue"));
    }

    private static boolean canLie(ServerPlayer p, NpcQuest q) {
        return q.valuable && SkillState.level(p, "silver_tongue") > 0 && !of(p, q.key).getBoolean("lied");
    }

    private static String lieLabel(ServerPlayer p, String label) {
        return chanceLabel("§d[Красномовство]§r " + label + " (Збрехати)", lieChance(p));
    }

    private static String chanceLabel(String label, double chance) {
        return label + " §8[" + Math.round(chance * 100) + "%]";
    }

    static MutableComponent taskText(NpcQuest q) {
        String what = line(q, "task", "");
        if (!what.isEmpty()) return Component.literal("Завдання: " + what);
        ResourceLocation rl = ResourceLocation.tryParse(q.taskId);
        String count = q.taskCount > 1 ? " ×" + q.taskCount : "";
        return switch (q.taskType) {
            case "item" -> Component.literal("Завдання: принести ").append(rl == null ? Component.literal(q.taskId)
                    : new ItemStack(BuiltInRegistries.ITEM.get(rl)).getHoverName()).append(count);
            case "kill" -> Component.literal("Завдання: перемогти ").append(rl == null ? Component.literal(q.taskId)
                    : BuiltInRegistries.ENTITY_TYPE.get(rl).getDescription()).append(count);
            default -> Component.literal("Завдання: дивіться книгу квестів (J)");
        };
    }

    private static MutableComponent rewardText(NpcQuest q, boolean haggled) {
        MutableComponent out = Component.literal("Нагорода: ");
        boolean any = false;
        for (String spec : q.rewardItems) {
            ItemStack s = parse(spec);
            if (s.isEmpty()) continue;
            if (any) out.append(", ");
            out.append(s.getHoverName()).append(s.getCount() > 1 ? " ×" + s.getCount() : "");
            any = true;
        }
        List<String> rest = new ArrayList<>();
        if (q.loot != null) rest.add("скриня здобичі");
        if (q.xp > 0) rest.add(Math.round(q.xp * (haggled ? 1.5 : 1)) + " досвіду");
        if (q.skillXp > 0) rest.add(Math.round(q.skillXp * (haggled ? 1.5 : 1)) + " досвіду навичок");
        if (!rest.isEmpty()) out.append((any ? ", " : "") + String.join(", ", rest));
        else if (!any) out.append("вдячність");
        if (haggled) out.append(Component.literal(" (виторгувано більше)").withStyle(ChatFormatting.GREEN));
        return out.withStyle(ChatFormatting.GRAY);
    }

    private static ItemStack parse(String spec) {
        String[] a = spec.split("\\*");
        ResourceLocation rl = ResourceLocation.tryParse(a[0]);
        Item item = rl == null ? Items.AIR : BuiltInRegistries.ITEM.get(rl);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, a.length > 1 ? Integer.parseInt(a[1]) : 1);
    }

    private static void accept(ServerPlayer p, Session s, NpcQuest q) {
        setState(p, q.key, ACCEPTED);
        of(p, q.key).putUUID("npc", s.npc.getUUID());
        Quests.reveal(p, q.key);
        page(p, s, q, text(line(q, "accept", "Дякую! Я чекатиму тут.") + "\n\n§7Завдання з'явилося в книзі квестів (J), розділ «Доручення»."),
                List.of(new Option("Бувай.", "end")));
    }

    private static void lie(ServerPlayer p, Session s, NpcQuest q) {
        if (!canLie(p, q)) {
            main(p, false);
            return;
        }
        CompoundTag d = of(p, q.key);
        d.putBoolean("lied", true);
        if (p.getRandom().nextDouble() < lieChance(p)) {
            reward(p, q, d.getBoolean("haggled"));
            if (state(p, q.key) == ACCEPTED) Quests.complete(p, q.key);
            else Quests.withdraw(p, q.key);
            setState(p, q.key, DONE);
            page(p, s, q, text(line(q, "lie_ok", "Справді? От і чудово! Тримайте, ви заслужили.")), List.of(new Option("Радий був допомогти.", "end")));
        } else {
            // caught: the job can still be done, but there will be no next one, and no better pay
            CompoundTag g = of(p, "@" + q.persona);
            g.putInt("distrust", q.stage);
            d.putBoolean("haggled", false);
            d.putBoolean("haggle_tried", true);
            page(p, s, q, text(line(q, "lie_fail", "Ви мене за дурня маєте? Я бачу, що це неправда.")
                            + "\n\n§c(Замовник вам більше не довіряє: цю справу ще можна виконати, але інших доручень він вам не дасть.)"),
                    List.of(new Option("Гаразд, гаразд...", "back")));
        }
    }

    private static void haggle(ServerPlayer p, Session s, NpcQuest q) {
        CompoundTag d = of(p, q.key);
        if (d.getBoolean("haggle_tried") || !q.canHaggle) {
            main(p, false);
            return;
        }
        d.putBoolean("haggle_tried", true);
        if (p.getRandom().nextDouble() < haggleChance(p)) {
            d.putBoolean("haggled", true);
            page(p, s, q, text(line(q, "haggle_ok", "Ех, умієте ви торгуватися. Гаразд, заплачу більше.")), List.of(new Option("Отож бо.", "back")));
        } else {
            page(p, s, q, text(line(q, "haggle_fail", "Більше в мене нема. Беріть, що пропоную, або йдіть.")), List.of(new Option("Гаразд.", "back")));
        }
    }

    /** the job is done: FTB Quests says so (its tasks track it), or, without it, the items are in the inventory */
    static boolean finished(ServerPlayer p, NpcQuest q) {
        if (q.taskType.equals("item") && count(p, q.taskId) < q.taskCount) return false;
        if (Quests.ftb) return Quests.done(p, q.key) || (q.taskType.equals("item") && count(p, q.taskId) >= q.taskCount);
        return q.taskType.equals("item");
    }

    private static int count(ServerPlayer p, String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return 0;
        Item item = BuiltInRegistries.ITEM.get(rl);
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(item)) n += s.getCount();
        return n;
    }

    private static void turnIn(ServerPlayer p, Session s, NpcQuest q) {
        if (state(p, q.key) != ACCEPTED || !finished(p, q)) {
            main(p, false);
            return;
        }
        if (q.taskType.equals("item")) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(q.taskId));
            int left = q.taskCount;
            for (ItemStack st : p.getInventory().items) {
                if (left <= 0) break;
                if (!st.is(item)) continue;
                int take = Math.min(left, st.getCount());
                st.shrink(take);
                left -= take;
            }
        }
        reward(p, q, of(p, q.key).getBoolean("haggled"));
        Quests.complete(p, q.key);
        setState(p, q.key, DONE);
        NpcQuest next = current(p, q.persona);
        List<Option> opts = next != null ? List.of(new Option("Чи можу я допомогти ще чимось?", "back"), new Option("Нема за що.", "end"))
                : List.of(new Option("Нема за що.", "end"));
        page(p, s, q, text(line(q, "thanks", "Дякую вам від щирого серця!")), opts);
    }

    private static void reward(ServerPlayer p, NpcQuest q, boolean haggled) {
        double k = haggled ? 1.5 : 1;
        for (String spec : q.rewardItems) {
            ItemStack s = parse(spec);
            if (!s.isEmpty() && !p.getInventory().add(s)) p.drop(s, false);
        }
        if (q.xp > 0) p.giveExperiencePoints((int) Math.round(q.xp * k));
        if (q.skillXp > 0) SkillXp.add(p, (int) Math.round(q.skillXp * k));
        if (q.loot != null) {
            ResourceLocation rl = ResourceLocation.tryParse(q.loot);
            if (rl != null) {
                LootTable t = p.server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, rl));
                LootParams params = new LootParams.Builder(p.serverLevel()).withParameter(LootContextParams.ORIGIN, p.position())
                        .withParameter(LootContextParams.THIS_ENTITY, p).create(LootContextParamSets.CHEST);
                int rolls = haggled ? 2 : 1;
                for (int i = 0; i < rolls; i++) {
                    for (ItemStack s : t.getRandomItems(params)) if (!p.getInventory().add(s)) p.drop(s, false);
                }
            }
        }
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.2f);
        p.sendSystemMessage(Component.translatable("wilds_core.npc.reward", q.name).withStyle(ChatFormatting.GOLD));
    }

    /** a giver died before the job was done */
    static void fail(ServerPlayer p, NpcQuest q) {
        setState(p, q.key, FAILED);
        Quests.withdraw(p, q.key);
        if (busy(p)) close(p);
    }

    private static void page(ServerPlayer p, Session s, NpcQuest q, MutableComponent text, List<Option> options) {
        s.options = new ArrayList<>(options);
        JsonObject o = new JsonObject();
        o.addProperty("name", q.name);
        o.addProperty("title", q.title + (q.stages > 1 ? " · доручення " + q.stage + " з " + q.stages : ""));
        String player = p.getGameProfile().getName();
        MutableComponent body = Component.empty();
        body.append(text);
        o.addProperty("text", Component.Serializer.toJson(body, p.registryAccess()).replace("{player}", player));
        JsonArray a = new JsonArray();
        for (Option opt : options) a.add(opt.label);
        o.add("options", a);
        PacketDistributor.sendToPlayer(p, new Net.DialoguePage(s.npc.getId(), o.toString()));
    }
}
