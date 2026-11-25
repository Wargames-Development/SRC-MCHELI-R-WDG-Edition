package mcheli.hud;

import mcheli.wrapper.W_TextureUtil;
import mcheli.wrapper.W_MOD;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class MCH_HudItemTexture extends MCH_HudItem {

    private final ResourceLocation texture;
    private final String left;
    private final String top;
    private final String width;
    private final String height;
    private final String uLeft;
    private final String vTop;
    private final String uWidth;
    private final String vHeight;
    private final String rot;


    public MCH_HudItemTexture(int fileLine, String name, String left, String top, String width, String height, String uLeft, String vTop, String uWidth, String vHeight, String rot) {
        super(fileLine);
        this.texture = new ResourceLocation(W_MOD.DOMAIN, "textures/gui/" + name + ".png");
        this.left = toFormula(left);
        this.top = toFormula(top);
        this.width = toFormula(width);
        this.height = toFormula(height);
        this.uLeft = toFormula(uLeft);
        this.vTop = toFormula(vTop);
        this.uWidth = toFormula(uWidth);
        this.vHeight = toFormula(vHeight);
        this.rot = toFormula(rot);
    }

    void preloadTexture(TextureManager manager) {
        if (W_TextureUtil.preload(manager, this.texture)) {
            W_TextureUtil.getTextureInfo(this.texture);
        }
    }

    public void execute() {
        GL11.glEnable(3042);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        W_TextureUtil.TextureParam size = W_TextureUtil.getTextureInfo(this.texture);
        this.drawTexture(this.texture, MCH_HudItem.centerX + calc(this.left), MCH_HudItem.centerY + calc(this.top), calc(this.width), calc(this.height), calc(this.uLeft), calc(this.vTop), calc(this.uWidth), calc(this.vHeight), (float) calc(this.rot), size.width > 0 ? size.width : 256, size.height > 0 ? size.height : 256);
    }
}
