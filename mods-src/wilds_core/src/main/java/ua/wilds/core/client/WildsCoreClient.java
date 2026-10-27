package ua.wilds.core.client;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import ua.wilds.core.WildsCore;
import ua.wilds.core.client.music.MusicDirector;
import ua.wilds.core.skills.ClientAbilities;

/** Client side of Wilds Core. */
@Mod(value = WildsCore.ID, dist = Dist.CLIENT)
public final class WildsCoreClient {
    public WildsCoreClient(IEventBus modBus) {
        modBus.addListener((RegisterClientReloadListenersEvent e) -> e.registerReloadListener((ResourceManagerReloadListener) MusicDirector::reload));
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent e) -> e.register(SkillInput.HEAVY));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            e.registerEntityRenderer(WildsCore.THROWN_WEAPON.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.25f, false));
            e.registerEntityRenderer(WildsCore.WANDERER.get(), ua.wilds.core.client.npc.WandererRenderer::new);
        });
        ua.wilds.core.net.Net.dialogueClient = ua.wilds.core.client.npc.DialogueScreen::page;
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> MusicDirector.tick());
        NeoForge.EVENT_BUS.addListener(SkillInput::tick);
        NeoForge.EVENT_BUS.addListener(SkillInput::onInteraction);
        NeoForge.EVENT_BUS.addListener(SkillInput::render);
        NeoForge.EVENT_BUS.addListener((AttackEntityEvent e) -> {
            if (e.getEntity().level().isClientSide()) MusicDirector.playerAttacked(e.getTarget());
        });
        NeoForge.EVENT_BUS.addListener(TutorialTracker::onOpen);
        // one equipment screen with an attributes panel (see gear/)
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOW, ua.wilds.core.client.gear.GearScreens::onOpening);
        NeoForge.EVENT_BUS.addListener(ua.wilds.core.client.gear.GearScreens::tick);
        NeoForge.EVENT_BUS.addListener(ua.wilds.core.client.gear.GearScreens::onInit);
        NeoForge.EVENT_BUS.addListener(ua.wilds.core.client.gear.GearScreens::onRender);
        NeoForge.EVENT_BUS.addListener(ua.wilds.core.client.gear.GearScreens::afterRender);
        NeoForge.EVENT_BUS.addListener(ua.wilds.core.client.gear.GearScreens::onClick);
        NeoForge.EVENT_BUS.addListener(ua.wilds.core.client.gear.GearScreens::afterClick);
        NeoForge.EVENT_BUS.addListener(ua.wilds.core.client.gear.GearScreens::onKey);
        NeoForge.EVENT_BUS.addListener(ua.wilds.core.client.gear.GearScreens::onKeyUp);
        NeoForge.EVENT_BUS.addListener(ua.wilds.core.client.gear.GearScreens::onChar);
        NeoForge.EVENT_BUS.addListener(ua.wilds.core.client.gear.AttributesPanel::onInit);
        NeoForge.EVENT_BUS.addListener(ua.wilds.core.client.gear.AttributesPanel::onRender);
        NeoForge.EVENT_BUS.addListener(ua.wilds.core.client.gear.AttributesPanel::onScroll);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn e) -> {
            TutorialTracker.reset();
            ClientAbilities.clear();
        });
    }
}
