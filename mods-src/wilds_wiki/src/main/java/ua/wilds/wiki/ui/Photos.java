package ua.wilds.wiki.ui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import ua.wilds.wiki.WildsWiki;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Photos of structures and biomes ship as small JPEGs (a PNG of a landscape is several times bigger): assets/wilds_wiki/photos/
 * &lt;structure|biome&gt;/&lt;ns&gt;/&lt;path&gt;.jpg. Minecraft only reads PNG, so they are decoded here once and kept as textures.
 */
public final class Photos {
    public static final int W = 400, H = 225;
    private static final Map<String, ResourceLocation> loaded = new HashMap<>();

    private Photos() {}

    /** the texture of the photo, or null when the pack has none */
    public static ResourceLocation get(String kind, ResourceLocation id) {
        String key = kind + "/" + id.getNamespace() + "/" + id.getPath();
        if (loaded.containsKey(key)) return loaded.get(key);
        ResourceLocation tex = null;
        Optional<Resource> r = Minecraft.getInstance().getResourceManager()
                .getResource(ResourceLocation.fromNamespaceAndPath(WildsWiki.ID, "photos/" + key + ".jpg"));
        if (r.isPresent()) {
            try (InputStream in = r.get().open()) {
                BufferedImage img = ImageIO.read(in);
                if (img != null) {
                    NativeImage n = new NativeImage(img.getWidth(), img.getHeight(), false);
                    for (int y = 0; y < img.getHeight(); y++) {
                        for (int x = 0; x < img.getWidth(); x++) {
                            int p = img.getRGB(x, y); // ARGB -> ABGR
                            n.setPixelRGBA(x, y, 0xFF000000 | (p & 0xFF) << 16 | (p & 0xFF00) | (p >> 16 & 0xFF));
                        }
                    }
                    tex = ResourceLocation.fromNamespaceAndPath(WildsWiki.ID, "photo/" + key.replace('/', '_'));
                    Minecraft.getInstance().getTextureManager().register(tex, new DynamicTexture(n));
                }
            } catch (Exception ex) {
                WildsWiki.LOG.warn("Wilds Wiki: cannot read photo {}", key, ex);
            }
        }
        loaded.put(key, tex);
        return tex;
    }

    /** after a resource reload the textures are gone */
    public static void clear() {
        loaded.clear();
    }
}
