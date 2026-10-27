package ua.wilds.core;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import ua.wilds.core.net.Net;
import ua.wilds.core.quests.Quests;
import ua.wilds.core.skills.Skills;
import ua.wilds.core.skills.ThrownWeapon;

/**
 * Wilds Core: the pack's own gameplay. Each feature lives in its own package and registers itself here;
 * integrations with other mods are switched on only when those mods are installed.
 */
@Mod(WildsCore.ID)
public final class WildsCore {
    public static final String ID = "wilds_core";
    public static final Logger LOG = LogUtils.getLogger();

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, ID);
    public static final DeferredHolder<EntityType<?>, EntityType<ThrownWeapon>> THROWN_WEAPON = ENTITIES.register("thrown_weapon",
            () -> EntityType.Builder.<ThrownWeapon>of(ThrownWeapon::new, MobCategory.MISC).sized(0.5f, 0.5f)
                    .clientTrackingRange(6).updateInterval(10).build("thrown_weapon"));

    public static final DeferredHolder<EntityType<?>, EntityType<ua.wilds.core.npc.Wanderer>> WANDERER = ENTITIES.register("wanderer",
            () -> EntityType.Builder.<ua.wilds.core.npc.Wanderer>of(ua.wilds.core.npc.Wanderer::new, MobCategory.MISC).sized(0.6f, 1.8f)
                    .clientTrackingRange(10).build("wanderer"));

    public WildsCore(IEventBus modBus) {
        ENTITIES.register(modBus);
        modBus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e) ->
                e.put(WANDERER.get(), ua.wilds.core.npc.Wanderer.attributes().build()));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.AddReloadListenerEvent e) ->
                e.addListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) ua.wilds.core.npc.NpcQuest::reload));
        ua.wilds.core.npc.NpcWorld.init();
        ua.wilds.core.npc.Dialogue.init();
        ua.wilds.core.world.Currencies.init();
        modBus.addListener(Net::register);
        Quests.init();
        Skills.init();
    }
}
