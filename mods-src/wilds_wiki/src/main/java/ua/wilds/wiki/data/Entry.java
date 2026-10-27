package ua.wilds.wiki.data;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/** One page of the wiki: an item (with its block, if it places one), a block without item, a mob, an effect, a biome... */
public final class Entry {
    public enum Kind {
        TOPIC, ITEM, BLOCK, ENTITY, EFFECT, ENCHANTMENT, BIOME, STRUCTURE;

        public Component label() {
            return Component.translatable("wilds_wiki.kind." + name().toLowerCase());
        }
    }

    public final Kind kind;
    public final ResourceLocation id;
    /** "item:minecraft:diamond": stable key for links, history and articles */
    public final String key;
    public Component name;
    public String nameEn = "";
    public String modName = "";
    public ItemStack icon = ItemStack.EMPTY;
    public Holder<MobEffect> effect;
    public ItemStack stack = ItemStack.EMPTY;
    public Block block;
    public EntityType<?> entityType;
    public final List<Article> articles = new ArrayList<>();
    /** article keywords and other aliases that only serve the search */
    public final List<String> keywords = new ArrayList<>();
    public final List<String> tagNames = new ArrayList<>();
    public final List<String> tagNamesEn = new ArrayList<>();

    public Entry(Kind kind, ResourceLocation id) {
        this.kind = kind;
        this.id = id;
        this.key = key(kind, id);
    }

    public static String key(Kind kind, ResourceLocation id) {
        return kind.name().toLowerCase() + ":" + id;
    }

    public void renderIcon(GuiGraphics g, int x, int y) {
        if (effect != null) {
            TextureAtlasSprite sprite = Minecraft.getInstance().getMobEffectTextures().get(effect);
            g.blit(x - 1, y - 1, 0, 18, 18, sprite);
        } else if (!icon.isEmpty()) {
            g.renderFakeItem(icon, x, y);
        }
    }
}
