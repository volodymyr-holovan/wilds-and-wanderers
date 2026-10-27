package ua.wilds.core.client.gear;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import ua.wilds.core.WildsCore;

import java.lang.reflect.Method;

/**
 * One equipment screen. Accessories (with Curios routed into it by the Accessories Compatibility Layer, and the Aether's
 * items in the shared slots) holds every ring, belt, glove, charm, spell book and backpack of every mod; its screen also
 * has the inventory, armour, crafting grid and the player. So the inventory key opens that screen right away instead
 * of the vanilla inventory, and everything one wears is visible without another tab. Beside it, each with its own
 * button and open together if the player likes: the vanilla recipe book ({@link RecipeBookPanel}) and the attributes
 * panel ({@link AttributesPanel}); see {@link #layout}. Creative mode keeps the vanilla screens.
 * <p>
 * The screen's layout options are Accessories' own and are kept per player in the world (so they stay as the player
 * leaves them). Once per world the pack sets its defaults: every slot visible in {@link #COLUMNS} columns without
 * scrolling, the crafting grid shown.
 * <p>
 * Accessories is called by reflection: without it (or with a version that renamed things) the vanilla inventory opens
 * as usual.
 * <p>
 * Self-test for the pack's tooling (-Dwilds_core.uishot=true): some seconds after joining, opens the inventory,
 * takes screenshots of it (plain, with cosmetics, with the recipe book and attributes open, after placing a recipe
 * from the book, at another GUI scale), logs the equipment slots, into screenshots/wilds_ui_*.png.
 */
public final class GearScreens {
    private static final String ACCESSORIES_GUI = "io.wispforest.accessories.client.gui.";
    private static Method open;
    private static boolean tried;
    /** set while the vanilla inventory must open as it is (the self-test, or the screen asked for it) */
    private static boolean vanillaOnce;
    static int COLUMNS = Integer.getInteger("wilds_core.gear_columns", 3);
    private static final java.nio.file.Path DONE = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("wilds_core-gear-defaults.txt");

    private GearScreens() {}

    public static boolean isGearScreen(Screen s) {
        return s != null && s.getClass().getName().startsWith(ACCESSORIES_GUI);
    }

    private static Method opener() {
        if (!tried) {
            tried = true;
            if (ModList.get().isLoaded("accessories")) {
                try {
                    open = Class.forName("io.wispforest.accessories.client.AccessoriesClient").getMethod("attemptToOpenScreen");
                } catch (ReflectiveOperationException | LinkageError e) {
                    WildsCore.LOG.warn("Wilds Core: Accessories screen not reachable, the vanilla inventory stays", e);
                }
            }
        }
        return open;
    }

    /** the vanilla inventory is about to open: open the equipment screen instead */
    public static void onOpening(ScreenEvent.Opening e) {
        if (!(e.getNewScreen() instanceof InventoryScreen) || vanillaOnce) {
            vanillaOnce = false;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isCreative() || isGearScreen(e.getCurrentScreen()) || opener() == null) return;
        e.setCanceled(true);
        mc.execute(() -> {
            try {
                if (!(boolean) opener().invoke(null)) {
                    vanillaOnce = true;
                    mc.setScreen(new InventoryScreen(mc.player));
                }
            } catch (ReflectiveOperationException | RuntimeException ex) {
                WildsCore.LOG.warn("Wilds Core: cannot open the Accessories screen", ex);
                open = null;
                vanillaOnce = true;
                mc.setScreen(new InventoryScreen(mc.player));
            }
        });
    }

    // ---------------------------------------------------------------- the side windows: recipe book, attributes

    public static void onInit(ScreenEvent.Init.Post e) {
        if (!isGearScreen(e.getScreen())) return;
        RecipeBookPanel.onInit(e);
    }

    /**
     * Every frame, before the screen draws: the recipe book left of the screen's content, the attributes panel right of
     * it, the whole row centred; the content moves aside for them. A screen too narrow for all three gets a narrower
     * attributes panel with smaller letters, never overlapping windows. Positions come from the screen's real layout, so a
     * bigger crafting grid or other columns of slots move everything along.
     */
    public static void onRender(ScreenEvent.Render.Pre e) {
        if (isGearScreen(e.getScreen())) layout(e.getScreen());
    }

    static void layout(Screen s) {
        if (!(s instanceof AbstractContainerScreen<?> c)) return;
        GearLayout.tidy(s);
        int[] content = GearLayout.content(s);
        if (content == null) return;
        int gap = 3, cw = content[2] - content[0];
        boolean book = RecipeBookPanel.open(), attrs = AttributesPanel.shown();
        int bw = book ? RecipeBookPanel.TABS + RecipeBookPanel.W + gap : 0;
        int aw = attrs ? Math.min(AttributesPanel.naturalWidth(), s.width - 4 - bw - cw - gap) : 0;
        int total = bw + cw + (attrs ? gap + aw : 0);
        int left = Math.max(2, (s.width - total) / 2);
        int contentLeft = left + bw;
        GearLayout.moveContentTo(s, contentLeft);

        RecipeBookPanel.place(s, left + RecipeBookPanel.TABS);
        GearLayout.standIn(s, "book", left, RecipeBookPanel.top(s), book ? RecipeBookPanel.TABS + RecipeBookPanel.W : 0, RecipeBookPanel.H);
        int ax = contentLeft + cw + gap, ay = content[1], ah = content[3] - content[1];
        aw = Math.min(aw, s.width - 2 - ax);
        // too narrow even for small letters (a huge GUI scale): the attributes give way rather than cover the rest
        if (aw < 48) aw = 0;
        AttributesPanel.place(ax, ay, attrs ? aw : 0, ah);
        GearLayout.standIn(s, "attributes", ax, ay, attrs ? aw : 0, ah);

        // the two buttons flank the hotbar: the recipe book left of it (as in the vanilla inventory), attributes right
        int invLeft = Integer.MAX_VALUE, invRight = Integer.MIN_VALUE, hotbarY = Integer.MIN_VALUE;
        for (Slot slot : c.getMenu().slots) {
            if (!slot.isActive() || !(slot.container instanceof Inventory)) continue;
            invLeft = Math.min(invLeft, slot.x);
            invRight = Math.max(invRight, slot.x + 16);
            if (slot.getContainerSlot() < 9) hotbarY = Math.max(hotbarY, slot.y);
        }
        if (hotbarY == Integer.MIN_VALUE) {
            RecipeBookPanel.placeButton(Integer.MIN_VALUE, 0);
            AttributesPanel.placeButton(Integer.MIN_VALUE, 0);
            return;
        }
        // the content moves this frame: place the buttons where its slots will be
        int moved = contentLeft - content[0];
        RecipeBookPanel.placeButton(c.getGuiLeft() + invLeft + moved - 34, c.getGuiTop() + hotbarY - 1);
        AttributesPanel.placeButton(c.getGuiLeft() + invRight + moved + 6, c.getGuiTop() + hotbarY - 1);
    }

    public static void afterRender(ScreenEvent.Render.Post e) {
        if (isGearScreen(e.getScreen())) RecipeBookPanel.render(e);
    }

    public static void onClick(ScreenEvent.MouseButtonPressed.Pre e) {
        if (!isGearScreen(e.getScreen())) return;
        RecipeBookPanel.onClick(e);
        if (!e.isCanceled()) AttributesPanel.onClick(e);
    }

    public static void afterClick(ScreenEvent.MouseButtonPressed.Post e) {
        if (isGearScreen(e.getScreen())) RecipeBookPanel.afterClick(e);
    }

    public static void onKey(ScreenEvent.KeyPressed.Pre e) {
        if (isGearScreen(e.getScreen())) RecipeBookPanel.onKey(e);
    }

    public static void onKeyUp(ScreenEvent.KeyReleased.Pre e) {
        if (isGearScreen(e.getScreen())) RecipeBookPanel.onKeyUp(e);
    }

    public static void onChar(ScreenEvent.CharacterTyped.Pre e) {
        if (isGearScreen(e.getScreen())) RecipeBookPanel.onChar(e);
    }

    // ---------------------------------------------------------------- the pack's defaults for the screen, once per world

    private static String worldKey(Minecraft mc) {
        if (mc.getCurrentServer() != null) return "server:" + mc.getCurrentServer().ip;
        return mc.getSingleplayerServer() != null ? "local:" + mc.getSingleplayerServer().getWorldData().getLevelName() : "?";
    }

    private static int defaultsAt = -1;

    /** a few seconds after joining a world the pack has not set up yet: send Accessories the pack's layout */
    private static void defaults(Minecraft mc) {
        if (mc.player == null || opener() == null) {
            defaultsAt = -1;
            return;
        }
        if (defaultsAt < 0) defaultsAt = mc.player.tickCount + 60;
        if (mc.player.tickCount != defaultsAt) return;
        String key = worldKey(mc) + "|" + mc.player.getUUID();
        try {
            java.util.List<String> done = java.nio.file.Files.exists(DONE) ? java.nio.file.Files.readAllLines(DONE) : new java.util.ArrayList<>();
            if (done.contains(key)) return;
            Class<?> prop = Class.forName("io.wispforest.accessories.networking.holder.HolderProperty");
            Class<?> change = Class.forName("io.wispforest.accessories.networking.holder.SyncHolderChange");
            Method of = change.getMethod("of", prop, Object.class);
            Method send = Class.forName("io.wispforest.accessories.networking.AccessoriesNetworking").getMethod("sendToServer", Record.class);
            Method local = prop.getMethod("setData", net.minecraft.world.entity.player.Player.class, Object.class);
            Object[][] values = {{"COLUMN_AMOUNT_PROP", COLUMNS}, {"CRAFTING_GRID_PROP", true}, {"GROUP_FILTER_PROP", false},
                    {"SIDE_WIDGET_POSITION_PROP", Boolean.getBoolean("wilds_core.gear_side")}};
            for (Object[] v : values) {
                Object p = prop.getField((String) v[0]).get(null);
                local.invoke(p, mc.player, v[1]);
                send.invoke(null, of.invoke(null, p, v[1]));
            }
            done.add(key);
            java.nio.file.Files.write(DONE, done);
            WildsCore.LOG.info("Wilds Core: equipment screen set up for this world ({} columns, crafting grid)", COLUMNS);
        } catch (ReflectiveOperationException | RuntimeException | java.io.IOException | LinkageError ex) {
            WildsCore.LOG.warn("Wilds Core: cannot set up the equipment screen", ex);
        }
    }

    // ---------------------------------------------------------------- self-test

    private static final boolean SHOTS = Boolean.getBoolean("wilds_core.uishot");
    private static int t = -1, talk = -1, scaleWas;
    private static boolean bookWas, attrsWas;

    /** the equipment slots this player has, by kind, as the server set them up (e.g. one «hand» slot, not two) */
    private static void logSlots(Minecraft mc) {
        try {
            Object cap = Class.forName("io.wispforest.accessories.api.AccessoriesCapability").getMethod("get", net.minecraft.world.entity.LivingEntity.class)
                    .invoke(null, mc.player);
            java.util.Map<?, ?> containers = (java.util.Map<?, ?>) cap.getClass().getMethod("getContainers").invoke(cap);
            StringBuilder sb = new StringBuilder();
            for (var en : new java.util.TreeMap<>(containers).entrySet()) {
                Object size = en.getValue().getClass().getMethod("getSize").invoke(en.getValue());
                sb.append(en.getKey()).append('=').append(size).append(' ');
            }
            WildsCore.LOG.info("Wilds Core: UI self-test: equipment slots {}", sb.toString().trim());
            WildsCore.LOG.info("Wilds Core: UI self-test: attributes {}", AttributesPanel.describe(mc.player));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            WildsCore.LOG.info("Wilds Core: UI self-test: cannot list equipment slots: {}", ex.toString());
        }
    }

    /** what clicking a recipe in the book does: the server moves the ingredients into the crafting grid */
    private static void placeRecipe(Minecraft mc, String id) {
        var holder = mc.level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse(id));
        if (holder.isEmpty() || mc.player == null || mc.gameMode == null) {
            WildsCore.LOG.info("Wilds Core: UI self-test: no recipe {}", id);
            return;
        }
        mc.gameMode.handlePlaceRecipe(mc.player.containerMenu.containerId, holder.get(), false);
    }

    private static void logGrid(Minecraft mc) {
        if (mc.player == null) return;
        var menu = mc.player.containerMenu;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(10, menu.slots.size()); i++) {
            Slot slot = menu.slots.get(i);
            sb.append(i).append(':').append(slot.getClass().getSimpleName()).append('=').append(slot.getItem()).append(' ');
        }
        WildsCore.LOG.info("Wilds Core: UI self-test: first slots after placing a recipe: {}", sb.toString().trim());
    }

    public static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        defaults(mc);
        if (isGearScreen(mc.screen)) RecipeBookPanel.tick();
        if (!SHOTS) return;
        if (mc.player == null || mc.level == null) return;
        // a quest giver that started talking (scenarios/npc.txt puts one next to the player): its page, then goodbye
        if (mc.screen instanceof ua.wilds.core.client.npc.DialogueScreen && talk < 0) talk = 0;
        if (talk >= 0 && talk < 1000) {
            if (++talk == 60) shot(mc, "6_dialogue");
            if (talk == 70) {
                if (mc.screen != null) mc.screen.onClose();
                talk = 1000;
            }
            return;
        }
        if (t < 0 && mc.screen != null) return;
        t++;
        switch (t) {
            case 290 -> shot(mc, "0_world");
            case 300 -> mc.setScreen(new InventoryScreen(mc.player));
            case 380 -> shot(mc, "1_inventory");
            case 390 -> call(mc.screen, "showCosmeticState", true);
            case 430 -> shot(mc, "2_cosmetic");
            case 440 -> call(mc.screen, "showCosmeticState", false);
            case 445 -> shot(mc, "2b_layout");
            case 446 -> {
                // the screen's own settings (the flask button): their labels must fit
                privateCall(mc.screen, "showAdvancedOptions", true);
                privateCall(mc.screen, "swapBottomComponentHolder", null);
            }
            case 449 -> shot(mc, "2c_options");
            case 450 -> {
                privateCall(mc.screen, "showAdvancedOptions", false);
                privateCall(mc.screen, "swapBottomComponentHolder", null);
            }
            case 452 -> {
                // everything open at once: the recipe book and the attributes beside the equipment
                bookWas = RecipeBookPanel.open();
                attrsWas = AttributesPanel.shown();
                if (!bookWas) RecipeBookPanel.toggle();
                if (!attrsWas) AttributesPanel.toggle();
                logSlots(mc);
            }
            case 480 -> shot(mc, "3_all_open");
            case 485 -> placeRecipe(mc, "minecraft:oak_planks");
            case 510 -> {
                logGrid(mc);
                shot(mc, "3b_recipe_placed");
            }
            case 515 -> {
                scaleWas = mc.options.guiScale().get();
                mc.options.guiScale().set(scaleWas == 3 ? 2 : 3);
                mc.resizeDisplay();
            }
            case 545 -> shot(mc, "3c_other_scale");
            case 550 -> {
                mc.options.guiScale().set(scaleWas);
                mc.resizeDisplay();
                // the player's own choices come back as they were
                if (RecipeBookPanel.open() != bookWas) RecipeBookPanel.toggle();
                if (AttributesPanel.shown() != attrsWas) AttributesPanel.toggle();
            }
            case 580 -> mc.setScreen(null);
            case 590 -> {
                // the illager runes (Iron's Spells' archevoker logbook): Cyrillic must not fall back to squares
                net.minecraft.network.chat.Style runes = net.minecraft.network.chat.Style.EMPTY.withFont(net.minecraft.resources.ResourceLocation.withDefaultNamespace("illageralt"));
                net.minecraft.network.chat.Style sga = net.minecraft.network.chat.Style.EMPTY.withFont(net.minecraft.resources.ResourceLocation.withDefaultNamespace("alt"));
                net.minecraft.network.chat.Component page = net.minecraft.network.chat.Component.literal("Abc xyz\n\n")
                        .append(net.minecraft.network.chat.Component.literal("Привіт, абетка ґєїі ЮЯ — «так»\n\n").withStyle(runes))
                        .append(net.minecraft.network.chat.Component.literal("Чари: Привіт ЮЯ — «так»\n\n").withStyle(sga))
                        .append(net.minecraft.network.chat.Component.translatable("item.irons_spellbooks.archevoker_log.entry_1.1").withStyle(runes));
                mc.setScreen(new net.minecraft.client.gui.screens.inventory.BookViewScreen(
                        new net.minecraft.client.gui.screens.inventory.BookViewScreen.BookAccess(java.util.List.of(page))));
            }
            case 640 -> shot(mc, "5_runes");
            case 645 -> {
                // the quest book: every chapter must have its name
                mc.setScreen(null);
                try {
                    Class.forName("dev.ftb.mods.ftbquests.client.ClientQuestFile").getMethod("openGui").invoke(null);
                } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
                    WildsCore.LOG.info("Wilds Core: UI self-test: no quest book: {}", ex.toString());
                }
            }
            case 690 -> {
                shot(mc, "7_quests");
                try {
                    Object file = Class.forName("dev.ftb.mods.ftbquests.client.ClientQuestFile").getField("INSTANCE").get(null);
                    StringBuilder sb = new StringBuilder();
                    for (Object ch : (java.util.Collection<?>) file.getClass().getMethod("getAllChapters").invoke(file)) {
                        sb.append(((net.minecraft.network.chat.Component) ch.getClass().getMethod("getTitle").invoke(ch)).getString()).append(" | ");
                    }
                    WildsCore.LOG.info("Wilds Core: UI self-test: quest chapters {}", sb);
                } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
                    WildsCore.LOG.info("Wilds Core: UI self-test: cannot list chapters: {}", ex.toString());
                }
            }
            case 700 -> {
                mc.setScreen(null);
                WildsCore.LOG.info("Wilds Core: UI self-test done");
            }
            default -> {
            }
        }
    }

    private static void call(Screen s, String name, boolean arg) {
        if (s == null) return;
        try {
            s.getClass().getMethod(name, boolean.class).invoke(s, arg);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            WildsCore.LOG.info("Wilds Core: UI self-test: {} has no {}", s.getClass().getSimpleName(), name);
        }
    }

    /** a private method of the screen (arg null: none) */
    private static void privateCall(Screen s, String name, Boolean arg) {
        if (s == null) return;
        try {
            Method m = arg == null ? s.getClass().getDeclaredMethod(name) : s.getClass().getDeclaredMethod(name, boolean.class);
            m.setAccessible(true);
            if (arg == null) m.invoke(s); else m.invoke(s, arg);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            WildsCore.LOG.info("Wilds Core: UI self-test: {} has no {}", s.getClass().getSimpleName(), name);
        }
    }

    private static void shot(Minecraft mc, String name) {
        WildsCore.LOG.info("Wilds Core: UI self-test screenshot {} of {}", name, mc.screen == null ? "no screen" : mc.screen.getClass().getName());
        Screenshot.grab(mc.gameDirectory, "wilds_ui_" + name + ".png", mc.getMainRenderTarget(), m -> {});
    }
}
