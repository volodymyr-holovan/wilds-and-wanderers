package ua.wilds.core.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import ua.wilds.core.WildsCore;
import ua.wilds.core.quests.Quests;
import ua.wilds.core.skills.ClientAbilities;
import ua.wilds.core.skills.branch.CombatSkills;
import ua.wilds.core.skills.branch.DiplomatSkills;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Network messages of Wilds Core. Every payload is optional, so a vanilla client can still join. */
public final class Net {
    private Net() {}

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> typeOf(String name) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(WildsCore.ID, name));
    }

    /** client -> server: the player did something the tutorial quests wait for */
    public record Tutorial(String event) implements CustomPacketPayload {
        public static final Type<Tutorial> TYPE = typeOf("tutorial");
        public static final StreamCodec<ByteBuf, Tutorial> CODEC = ByteBufCodecs.stringUtf8(64).map(Tutorial::new, Tutorial::event);

        public Type<Tutorial> type() {
            return TYPE;
        }
    }

    /** server -> client: the player's abilities and their levels (the client handles the inputs of some of them) */
    public record SkillSync(Map<String, Integer> abilities) implements CustomPacketPayload {
        public static final Type<SkillSync> TYPE = typeOf("skills");
        public static final StreamCodec<ByteBuf, SkillSync> CODEC = ByteBufCodecs.map(HashMap::new, ByteBufCodecs.stringUtf8(48), ByteBufCodecs.VAR_INT, 64)
                .map(SkillSync::new, s -> new HashMap<>(s.abilities));

        public Type<SkillSync> type() {
            return TYPE;
        }
    }

    /** client -> server: an ability input. action: "throw", "heavy" (with the entity aimed at), "pacify" */
    public record Act(String action, int target) implements CustomPacketPayload {
        public static final Type<Act> TYPE = typeOf("act");
        public static final StreamCodec<ByteBuf, Act> CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(16), Act::action,
                ByteBufCodecs.VAR_INT, Act::target, Act::new);

        public Type<Act> type() {
            return TYPE;
        }
    }

    /** server -> client: a page of a conversation (npc entity id, JSON {name, title, text, options}); npc 0 closes it */
    public record DialoguePage(int npc, String json) implements CustomPacketPayload {
        public static final Type<DialoguePage> TYPE = typeOf("dialogue");
        public static final StreamCodec<ByteBuf, DialoguePage> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, DialoguePage::npc,
                ByteBufCodecs.stringUtf8(32767), DialoguePage::json, DialoguePage::new);

        public Type<DialoguePage> type() {
            return TYPE;
        }
    }

    /** client -> server: the answer picked (its number), or -1 when the player closed the conversation */
    public record DialogueChoice(int index) implements CustomPacketPayload {
        public static final Type<DialogueChoice> TYPE = typeOf("choice");
        public static final StreamCodec<ByteBuf, DialogueChoice> CODEC = ByteBufCodecs.VAR_INT.map(DialogueChoice::new, DialogueChoice::index);

        public Type<DialogueChoice> type() {
            return TYPE;
        }
    }

    /** server -> client: the signs above the quest givers near the player (entity id -> Dialogue.MARK_*), all of them */
    public record NpcMarks(Map<Integer, Integer> marks) implements CustomPacketPayload {
        public static final Type<NpcMarks> TYPE = typeOf("npc_marks");
        public static final StreamCodec<ByteBuf, NpcMarks> CODEC = ByteBufCodecs.map(HashMap::new, ByteBufCodecs.VAR_INT, ByteBufCodecs.VAR_INT, 512)
                .map(NpcMarks::new, m -> new HashMap<>(m.marks));

        public Type<NpcMarks> type() {
            return TYPE;
        }
    }

    /** client -> server: the item finder, the ids of the items whose name matches what the player typed */
    public record FinderQuery(List<String> ids) implements CustomPacketPayload {
        public static final Type<FinderQuery> TYPE = typeOf("finder_query");
        public static final StreamCodec<ByteBuf, FinderQuery> CODEC = ByteBufCodecs.stringUtf8(128).apply(ByteBufCodecs.list(512))
                .map(FinderQuery::new, FinderQuery::ids);

        public Type<FinderQuery> type() {
            return TYPE;
        }
    }

    /** server -> client: the places around the player that hold one of those items */
    public record FinderHits(List<BlockPos> places) implements CustomPacketPayload {
        public static final Type<FinderHits> TYPE = typeOf("finder_hits");
        public static final StreamCodec<ByteBuf, FinderHits> CODEC = BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(256))
                .map(FinderHits::new, FinderHits::places);

        public Type<FinderHits> type() {
            return TYPE;
        }
    }

    /** client -> server: the storage the player just opened, to show its status for the copper golem */
    public record SortQuery(BlockPos pos) implements CustomPacketPayload {
        public static final Type<SortQuery> TYPE = typeOf("sort_query");
        public static final StreamCodec<ByteBuf, SortQuery> CODEC = BlockPos.STREAM_CODEC.map(SortQuery::new, SortQuery::pos);

        public Type<SortQuery> type() {
            return TYPE;
        }
    }

    /** server -> client: that storage's status ("" for none to show) */
    public record SortLabel(BlockPos pos, String label) implements CustomPacketPayload {
        public static final Type<SortLabel> TYPE = typeOf("sort_label");
        public static final StreamCodec<ByteBuf, SortLabel> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, SortLabel::pos,
                ByteBufCodecs.stringUtf8(64), SortLabel::label, SortLabel::new);

        public Type<SortLabel> type() {
            return TYPE;
        }
    }

    /** set by the client entry point: shows a storage's status in its title */
    public static java.util.function.BiConsumer<BlockPos, String> sortLabelClient = (p, l) -> {};

    /** set by the client entry point: shows the item finder's answer */
    public static java.util.function.Consumer<List<BlockPos>> finderClient = h -> {};

    /** set by the client entry point: shows a conversation page (the server never calls it) */
    public static java.util.function.BiConsumer<Integer, String> dialogueClient = (n, j) -> {};

    public static void register(RegisterPayloadHandlersEvent e) {
        PayloadRegistrar r = e.registrar("1").optional();
        r.playToServer(Tutorial.TYPE, Tutorial.CODEC, (msg, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer p) Quests.tutorial(p, msg.event());
        }));
        r.playToClient(SkillSync.TYPE, SkillSync.CODEC, (msg, ctx) -> ctx.enqueueWork(() -> ClientAbilities.set(msg.abilities())));
        r.playToClient(DialoguePage.TYPE, DialoguePage.CODEC, (msg, ctx) -> ctx.enqueueWork(() -> dialogueClient.accept(msg.npc(), msg.json())));
        r.playToClient(NpcMarks.TYPE, NpcMarks.CODEC, (msg, ctx) -> ctx.enqueueWork(() -> ua.wilds.core.npc.Wanderer.clientMarks = Map.copyOf(msg.marks())));
        r.playToServer(DialogueChoice.TYPE, DialogueChoice.CODEC, (msg, ctx) -> ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer p)) return;
            if (msg.index() < 0) ua.wilds.core.npc.Dialogue.close(p);
            else ua.wilds.core.npc.Dialogue.choose(p, msg.index());
        }));
        r.playToServer(FinderQuery.TYPE, FinderQuery.CODEC, (msg, ctx) -> ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer p)) return;
            List<BlockPos> hits = ua.wilds.core.finder.Finder.find(p, msg.ids());
            if (hits != null) net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, new FinderHits(hits));
        }));
        r.playToClient(FinderHits.TYPE, FinderHits.CODEC, (msg, ctx) -> ctx.enqueueWork(() -> finderClient.accept(msg.places())));
        r.playToServer(SortQuery.TYPE, SortQuery.CODEC, (msg, ctx) -> ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer p) || !(p.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
            if (p.blockPosition().distSqr(msg.pos()) > 12 * 12 || !level.isLoaded(msg.pos())) return;
            String label = net.neoforged.fml.ModList.get().isLoaded("friendsandfoes") ? ua.wilds.core.golem.Golems.label(level, msg.pos()) : "";
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, new SortLabel(msg.pos(), label));
        }));
        r.playToClient(SortLabel.TYPE, SortLabel.CODEC, (msg, ctx) -> ctx.enqueueWork(() -> sortLabelClient.accept(msg.pos(), msg.label())));
        r.playToServer(Act.TYPE, Act.CODEC, (msg, ctx) -> ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer p)) return;
            switch (msg.action()) {
                case "throw" -> CombatSkills.throwWeapon(p);
                case "heavy" -> CombatSkills.heavyStrike(p, msg.target());
                case "pacify" -> DiplomatSkills.pacify(p);
                default -> {
                }
            }
        }));
    }
}
