package ua.wilds.wiki.data;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * What is around the player, for the wiki's "Тут і зараз" list: the last biomes (caves included: underground the biome
 * is the cave's), the dimension, the last mobs seen nearby, the last items whose tooltip the player looked at, and the
 * last event: a boss bar that appeared. Wiki entry keys, newest first; only for this session.
 */
public final class Recent {
    public static final List<String> places = new ArrayList<>(), mobs = new ArrayList<>(), items = new ArrayList<>();
    public static String dimension;
    /** the last boss bar's title, and the entry of the mob it belongs to when one was found */
    public static Component event;
    public static String eventKey;
    private static final Set<UUID> bossBars = new HashSet<>();

    private Recent() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ItemTooltipEvent.class, e -> {
            ItemStack s = e.getItemStack();
            if (!s.isEmpty() && e.getEntity() != null) push(items, "item:" + BuiltInRegistries.ITEM.getKey(s.getItem()), 5);
        });
        NeoForge.EVENT_BUS.addListener(CustomizeGuiOverlayEvent.BossEventProgress.class, e -> boss(e.getBossEvent().getId(), e.getBossEvent().getName()));
    }

    public static void tick(Minecraft mc) {
        if (mc.level == null || mc.player == null || mc.player.tickCount % 20 != 0) return;
        Player p = mc.player;
        dimension = mc.level.dimension().location().toString();
        mc.level.getBiome(p.blockPosition()).unwrapKey().ifPresent(k -> push(places, "biome:" + k.location(), 5));
        List<Entity> near = new ArrayList<>();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof LivingEntity le && !(e instanceof Player) && le.isAlive() && e.distanceToSqr(p) < 32 * 32) near.add(e);
        }
        near.sort(Comparator.comparingDouble((Entity e) -> e.distanceToSqr(p)).reversed());   // the nearest ends first
        for (Entity e : near) push(mobs, "entity:" + BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()), 10);
    }

    private static void boss(UUID id, Component name) {
        if (!bossBars.add(id)) return;
        event = name;
        eventKey = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        String want = name.getString();
        double best = Double.MAX_VALUE;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity) || !e.getDisplayName().getString().equals(want)) continue;
            double d = e.distanceToSqr(mc.player);
            if (d < best) {
                best = d;
                eventKey = "entity:" + BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
            }
        }
    }

    private static void push(List<String> list, String key, int max) {
        if (!list.isEmpty() && list.get(0).equals(key)) return;
        list.remove(key);
        list.add(0, key);
        while (list.size() > max) list.remove(list.size() - 1);
    }

    /** the dimension's name for people */
    public static Component dimensionName() {
        if (dimension == null) return Component.empty();
        ResourceLocation id = ResourceLocation.tryParse(dimension);
        if (id == null) return Component.literal(dimension);
        String key = "wilds_wiki.dimension." + id.getNamespace() + "." + id.getPath().replace('/', '.');
        return switch (dimension) {
            case "minecraft:overworld" -> Component.literal("Звичайний світ");
            case "minecraft:the_nether" -> Component.literal("Незер");
            case "minecraft:the_end" -> Component.literal("Енд");
            default -> net.minecraft.client.resources.language.I18n.exists(key) ? Component.translatable(key)
                    : Component.literal(pretty(id.getPath()));
        };
    }

    private static String pretty(String path) {
        String s = path.substring(path.lastIndexOf('/') + 1).replace('_', ' ');
        return s.isEmpty() ? path : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
