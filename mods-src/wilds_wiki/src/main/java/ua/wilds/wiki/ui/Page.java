package ua.wilds.wiki.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import ua.wilds.wiki.data.Entry;
import ua.wilds.wiki.data.WikiData;

import java.util.List;

/** What page elements can ask of the screen that shows them. */
public interface Page {
    Font font();

    WikiData data();

    int mouseX();

    int mouseY();

    float partialTick();

    void open(Entry e);

    /** tooltips are collected while drawing and shown after the page's clipping is off */
    void tooltip(List<Component> lines);

    void tooltip(ItemStack stack);

    /** called when the element under the mouse wants its JEI overlay drawn at the end of the frame */
    void overlay(Runnable r);

    /** a link is under the mouse: the screen shows a hand pointer */
    default void hoverLink() {}
}
