package ua.wilds.wiki;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.commands.Commands;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import ua.wilds.wiki.data.Entry;
import ua.wilds.wiki.data.WikiData;
import ua.wilds.wiki.jei.Jei;
import ua.wilds.wiki.ui.WikiScreen;

import java.lang.reflect.Field;

/**
 * Wilds Wiki: press O (or /wiki, /вікі) for the encyclopedia of the pack.
 * Over an item in any inventory or in JEI it opens that item; looking at a block or a mob it opens that one.
 */
@Mod(value = WildsWiki.ID, dist = Dist.CLIENT)
public final class WildsWiki {
    public static final String ID = "wilds_wiki";
    public static final Logger LOG = LogUtils.getLogger();
    public static final KeyMapping KEY = new KeyMapping("key.wilds_wiki.open", KeyConflictContext.UNIVERSAL, InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O, "key.categories.wilds_wiki");
    public static boolean jei;
    private static Runnable pending;

    public WildsWiki(IEventBus modBus) {
        jei = ModList.get().isLoaded("jei");
        modBus.addListener(RegisterKeyMappingsEvent.class, e -> e.register(KEY));
        modBus.addListener(RegisterClientReloadListenersEvent.class,
                e -> e.registerReloadListener((ResourceManagerReloadListener) rm -> WikiData.invalidate()));
        NeoForge.EVENT_BUS.addListener(RegisterClientCommandsEvent.class, WildsWiki::commands);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> tick());
        NeoForge.EVENT_BUS.addListener(ScreenEvent.KeyPressed.Pre.class, WildsWiki::screenKey);
    }

    private static void commands(RegisterClientCommandsEvent e) {
        for (String name : new String[]{"wiki", "вікі"}) {
            e.getDispatcher().register(Commands.literal(name)
                    .executes(c -> {
                        later(null, null);
                        return 1;
                    })
                    .then(Commands.argument("query", StringArgumentType.greedyString()).executes(c -> {
                        later(null, StringArgumentType.getString(c, "query"));
                        return 1;
                    })));
        }
    }

    /** a command runs while the chat screen is still open: show the wiki on the next tick */
    private static void later(Entry e, String query) {
        pending = () -> {
            // "/wiki iceandfire:dragonbone" or "/wiki entity:cataclysm:ignis" (links from quests) opens that page itself
            Entry exact = e == null && query != null && query.matches("[a-z_]+:[a-z0-9_.:/-]+") ? WikiData.get().resolve(query) : null;
            open(null, exact != null ? exact : e, exact != null ? null : query);
        };
    }

    public static void open(Screen parent, Entry e, String query) {
        try {
            Minecraft.getInstance().setScreen(new WikiScreen(parent, e, query));
        } catch (Exception ex) {
            LOG.error("Wilds Wiki: cannot open", ex);
        }
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (ua.wilds.wiki.ui.SelfTest.ON) ua.wilds.wiki.ui.SelfTest.tick();
        if (ua.wilds.wiki.ui.PhotoMode.ON) ua.wilds.wiki.ui.PhotoMode.tick();
        if (pending != null && mc.screen == null) {
            Runnable r = pending;
            pending = null;
            r.run();
            return;
        }
        while (KEY.consumeClick()) {
            if (mc.screen == null && mc.player != null) open(null, lookedAt(mc), null);
        }
    }

    /** The block in reach or the mob up to 48 blocks away under the crosshair, else nothing (the home page opens). */
    private static Entry lookedAt(Minecraft mc) {
        WikiData data = WikiData.get();
        Entity cam = mc.getCameraEntity();
        if (cam == null || mc.level == null) return null;
        double range = 48;
        Vec3 eye = cam.getEyePosition(1), look = cam.getViewVector(1), end = eye.add(look.scale(range));
        HitResult blockHit = cam.pick(range, 1, false);
        double max = blockHit.getType() == HitResult.Type.MISS ? range * range : blockHit.getLocation().distanceToSqr(eye);
        AABB box = cam.getBoundingBox().expandTowards(look.scale(range)).inflate(1);
        EntityHitResult eh = ProjectileUtil.getEntityHitResult(cam, eye, end, box, en -> !en.isSpectator() && en.isPickable(), max);
        if (eh != null) {
            Entry e = data.byEntity.get(eh.getEntity().getType());
            if (e != null) return e;
        }
        if (mc.hitResult instanceof BlockHitResult bh && bh.getType() == HitResult.Type.BLOCK) {
            BlockState st = mc.level.getBlockState(bh.getBlockPos());
            return data.forBlock(st.getBlock());
        }
        return null;
    }

    private static void screenKey(ScreenEvent.KeyPressed.Pre e) {
        if (!KEY.isActiveAndMatches(InputConstants.getKey(e.getKeyCode(), e.getScanCode()))) return;
        Screen s = e.getScreen();
        if (s instanceof WikiScreen || typing(s) || (jei && Jei.typing())) return;
        ItemStack stack = ItemStack.EMPTY;
        if (s instanceof AbstractContainerScreen<?> cs) {
            Slot slot = cs.getSlotUnderMouse();
            if (slot != null && slot.hasItem()) stack = slot.getItem();
        }
        if (stack.isEmpty() && jei) stack = Jei.hovered();
        if (stack.isEmpty()) return;
        Entry entry = WikiData.get().byItem.get(stack.getItem());
        if (entry == null) return;
        open(s, entry, null);
        e.setCanceled(true);
    }

    private static Field creativeSearch;

    /** true when the key would type a letter: a text field has focus (chat, anvil, recipe book, creative search) */
    private static boolean typing(Screen s) {
        if (focusedText(s.getFocused())) return true;
        if (s instanceof CreativeModeInventoryScreen) {
            try {
                if (creativeSearch == null) {
                    creativeSearch = CreativeModeInventoryScreen.class.getDeclaredField("searchBox");
                    creativeSearch.setAccessible(true);
                }
                EditBox box = (EditBox) creativeSearch.get(s);
                return box != null && box.isVisible() && box.isFocused();
            } catch (ReflectiveOperationException | RuntimeException ex) {
                return true; // unknown: better no wiki than eating a typed letter
            }
        }
        return false;
    }

    private static boolean focusedText(GuiEventListener f) {
        if (f instanceof EditBox box) return box.isFocused() && box.isVisible();
        if (f instanceof RecipeBookComponent rb) return rb.isVisible();
        if (f instanceof ContainerEventHandler c) return focusedText(c.getFocused());
        return false;
    }
}
