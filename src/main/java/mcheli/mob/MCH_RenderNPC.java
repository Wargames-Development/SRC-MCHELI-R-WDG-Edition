package mcheli.mob;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;

@SideOnly(Side.CLIENT)
public class MCH_RenderNPC extends RenderBiped {

    private static final ResourceLocation STEVE_TEXTURE = new ResourceLocation("minecraft", "textures/entity/steve.png");
    private ResourceLocation cachedSkin;
    private ResourceLocation cachedFallback;

    public MCH_RenderNPC() {
        super(new ModelBiped(0.0F), 0.5F);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        if (this.cachedSkin == null) {
            this.cachedSkin = parseLocation(MCH_TechNpcConfig.getSkin());
        }
        if (this.cachedFallback == null) {
            this.cachedFallback = parseLocation(MCH_TechNpcConfig.getSkinFallback());
        }
        if (exists(this.cachedSkin)) {
            return this.cachedSkin;
        }
        if (exists(this.cachedFallback)) {
            return this.cachedFallback;
        }
        return STEVE_TEXTURE;
    }

    private ResourceLocation parseLocation(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return STEVE_TEXTURE;
        }
        String s = raw.trim();
        int idx = s.indexOf(':');
        if (idx > 0 && idx < s.length() - 1) {
            return new ResourceLocation(s.substring(0, idx), s.substring(idx + 1));
        }
        return new ResourceLocation("mcheli", s);
    }

    private boolean exists(ResourceLocation rl) {
        if (rl == null) {
            return false;
        }
        try {
            Minecraft.getMinecraft().getResourceManager().getResource(rl);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }
}
