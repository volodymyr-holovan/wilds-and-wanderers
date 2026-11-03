package ua.wilds.core.client.gear;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import ua.wilds.core.net.Net;
import ua.wilds.core.skills.ClientAbilities;

/** The pocket crafting table (skill pocket_crafting): a button by the inventory that opens a 3x3 crafting grid. */
final class PocketCraftButton extends AbstractButton {
    private static PocketCraftButton button;
    private static final ItemStack ICON = new ItemStack(Items.CRAFTING_TABLE);

    private PocketCraftButton() {
        super(0, -100, 20, 20, Component.literal("Кишеньковий верстак"));
        setTooltip(Tooltip.create(Component.literal("Кишеньковий верстак: сітка 3×3 будь-де (навичка «Торба»)")));
    }

    static void onInit(ScreenEvent.Init.Post e) {
        button = new PocketCraftButton();
        e.addListener(button);
    }

    static void place(int x, int y) {
        if (button == null) return;
        button.visible = x > Integer.MIN_VALUE && ClientAbilities.has("pocket_crafting");
        button.setX(x);
        button.setY(y);
    }

    public void onPress() {
        PacketDistributor.sendToServer(new Net.Act("craft", 0));
    }

    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.renderWidget(g, mouseX, mouseY, partial);
        g.renderItem(ICON, getX() + 2, getY() + 2);
    }

    /** drawn again over the screen: owo draws its layout over the screen's widgets */
    static void renderOver(GuiGraphics g, int mouseX, int mouseY, float partial) {
        if (button == null || !button.visible) return;
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        button.render(g, mouseX, mouseY, partial);
        g.pose().popPose();
    }

    protected void updateWidgetNarration(NarrationElementOutput out) {
        defaultButtonNarrationText(out);
    }

}
