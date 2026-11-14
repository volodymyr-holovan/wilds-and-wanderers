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

    public static final net.neoforged.neoforge.registries.DeferredRegister.Blocks BLOCKS = net.neoforged.neoforge.registries.DeferredRegister.createBlocks(ID);
    public static final net.neoforged.neoforge.registries.DeferredRegister.Items ITEMS = net.neoforged.neoforge.registries.DeferredRegister.createItems(ID);
    /** the copper golem's docking station (golem.GolemDock) */
    public static final net.neoforged.neoforge.registries.DeferredBlock<ua.wilds.core.golem.GolemDock> GOLEM_DOCK = BLOCKS.register("golem_dock",
            () -> new ua.wilds.core.golem.GolemDock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.COLOR_ORANGE).requiresCorrectToolForDrops().strength(3f, 6f)
                    .sound(net.minecraft.world.level.block.SoundType.COPPER).noOcclusion()
                    .lightLevel(s -> s.getValue(ua.wilds.core.golem.GolemDock.OCCUPIED) ? 7 : 0)));
    public static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.BlockItem> GOLEM_DOCK_ITEM = ITEMS.registerSimpleBlockItem(GOLEM_DOCK);

    public WildsCore(IEventBus modBus) {
        LogNoise.install();
        PartyPeace.register();
        OpBreak.register();
        if (net.neoforged.fml.ModList.get().isLoaded("ftbquests") && net.neoforged.fml.ModList.get().isLoaded("ftbteams")) ua.wilds.core.quests.PersonalQuests.register();
        if (net.neoforged.fml.ModList.get().isLoaded("ftbquests")) ua.wilds.core.quests.QuestFanfare.register();
        if (net.neoforged.fml.ModList.get().isLoaded("ftbquests")) ua.wilds.core.quests.KillCredit.register();
        if (net.neoforged.fml.ModList.get().isLoaded("friendsandfoes")) ua.wilds.core.golem.Golems.register();
        ENTITIES.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey().location().getPath().equals("functional_blocks")) e.accept(GOLEM_DOCK_ITEM.get());
        });
        ua.wilds.core.world.FrameClick.register();
        ua.wilds.core.skills.LegacyRows.ATTACHMENTS.register(modBus);
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
        ua.wilds.core.worldgen.WorldgenAudit.register();
        ua.wilds.core.worldgen.CaveBiomes.register();
        // test bench for the MineColonies fixes: only on a test server started with -Dwilds_core.colonytest=true
        if (ua.wilds.core.colony.ColonyStats.ON && net.neoforged.fml.ModList.get().isLoaded("minecolonies")) ua.wilds.core.colony.ColonyTest.register();
    }
}
