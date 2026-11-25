/*
 * Decompiled with CFR 0_123.
 *
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.texture.TextureManager
 *  net.minecraft.util.ResourceLocation
 *  org.lwjgl.opengl.GL11
 */
package mcheli.wrapper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

public class W_TextureUtil {
    private static final W_TextureUtil instance = new W_TextureUtil();
    private static final Map<ResourceLocation, TextureParam> DIMENSIONS = new HashMap<>();

    private W_TextureUtil() {
    }

    public static TextureParam getTextureInfo(String domain, String name) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation(domain, name));
        return readBoundTextureInfo();
    }

    public static TextureParam getTextureInfo(ResourceLocation texture) {
        TextureParam info = DIMENSIONS.get(texture);
        if (info == null) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
            info = readBoundTextureInfo();
            DIMENSIONS.put(texture, info);
        }
        return info;
    }

    private static TextureParam readBoundTextureInfo() {
        TextureParam info = instance.new TextureParam();
        info.width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        info.height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        return info;
    }

    public static boolean preload(TextureManager manager, ResourceLocation texture) {
        ITextureObject loaded = manager.getTexture(texture);
        // Forge retains its missing-texture entry across reloads. Retry it when packs change.
        return loaded != null && loaded != TextureUtil.missingTexture
                || manager.loadTexture(texture, new SimpleTexture(texture));
    }

    public static void clearTextureInfo() {
        DIMENSIONS.clear();
    }

    public class TextureParam {
        public int width;
        public int height;
    }

}

