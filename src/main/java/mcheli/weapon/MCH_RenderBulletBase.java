package mcheli.weapon;

import mcheli.MCH_Color;
import mcheli.wrapper.W_Block;
import mcheli.wrapper.W_MOD;
import mcheli.wrapper.W_Render;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public abstract class MCH_RenderBulletBase extends W_Render {

    public static final ResourceLocation TEX_FLAME = new ResourceLocation(W_MOD.DOMAIN, "textures/exhaust_flame.png");
    public static final ResourceLocation TEX_FLAME_1 = new ResourceLocation(W_MOD.DOMAIN, "textures/exhaust_flame_1.png");
    public static final ResourceLocation TEX_DELAY_FUSE_MARKER = new ResourceLocation(W_MOD.DOMAIN, "textures/delayfuse_bomb.png");

    public void doRender(Entity e, double var2, double var4, double var6, float var8, float var9) {
        int dstBlend;
        if (e instanceof MCH_EntityBaseBullet && ((MCH_EntityBaseBullet) e).getInfo() != null) {
            MCH_Color srcBlend = ((MCH_EntityBaseBullet) e).getInfo().color;

            for (dstBlend = 0; dstBlend < 3; ++dstBlend) {
                Block b = W_WorldFunc.getBlock(e.worldObj, (int) (e.posX + 0.5D), (int) (e.posY + 1.5D - (double) dstBlend), (int) (e.posZ + 0.5D));
                if (b != null && b == W_Block.getWater()) {
                    srcBlend = ((MCH_EntityBaseBullet) e).getInfo().colorInWater;
                    break;
                }
            }

            GL11.glColor4f(srcBlend.r, srcBlend.g, srcBlend.b, srcBlend.a);
        } else {
            GL11.glColor4f(0.75F, 0.75F, 0.75F, 1.0F);
        }

        GL11.glAlphaFunc(516, 0.001F);
        GL11.glEnable(2884);
        GL11.glEnable(3042);
        int var13 = GL11.glGetInteger(3041);
        dstBlend = GL11.glGetInteger(3040);
        GL11.glBlendFunc(770, 771);
        this.renderBullet(e, var2, var4, var6, var8, var9);
        GL11.glColor4f(0.75F, 0.75F, 0.75F, 1.0F);
        GL11.glBlendFunc(var13, dstBlend);
        GL11.glDisable(3042);

        postRender(e, var2, var4, var6, var8, var9);
    }

    public void renderModel(MCH_EntityBaseBullet e) {
        MCH_BulletModel model = e.getBulletModel();
        if (model != null) {
            this.bindTexture("textures/bullets/" + model.name + ".png");
            model.model.renderAll();
        }

    }

    public void renderModel(MCH_BulletModel model) {
        if (model != null) {
            this.bindTexture("textures/bullets/" + model.name + ".png");
            model.model.renderAll();
        }
    }

    public abstract void renderBullet(Entity var1, double var2, double var4, double var6, float var8, float var9);

    public void postRender(Entity var1, double var2, double var4, double var6, float var8, float var9) {
        if (!(var1 instanceof MCH_EntityBaseBullet)) {
            return;
        }
        MCH_EntityBaseBullet bullet = (MCH_EntityBaseBullet)var1;
        if (!bullet.hasDelayFuseMarker()) {
            return;
        }
        double x = var2 + bullet.getDelayFuseMarkerX() - bullet.posX;
        double y = var4 + bullet.getDelayFuseMarkerY() - bullet.posY + 0.3D;
        double z = var6 + bullet.getDelayFuseMarkerZ() - bullet.posZ;
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        RenderManager rm = RenderManager.instance;
        GL11.glRotatef(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(rm.playerViewX, 1.0F, 0.0F, 0.0F);
        GL11.glScalef(-3.75F, -3.75F, 3.75F);
        GL11.glDisable(2896);
        GL11.glDisable(2884);
        GL11.glDepthMask(false);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glColor4f(1.0F, 0.1F, 0.1F, 0.95F);
        this.bindTexture("textures/delayfuse_bomb.png");
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertexWithUV(-0.5D, 0.5D, 0.0D, 0.0D, 1.0D);
        tess.addVertexWithUV(0.5D, 0.5D, 0.0D, 1.0D, 1.0D);
        tess.addVertexWithUV(0.5D, -0.5D, 0.0D, 1.0D, 0.0D);
        tess.addVertexWithUV(-0.5D, -0.5D, 0.0D, 0.0D, 0.0D);
        tess.draw();
        GL11.glEnable(2884);
        GL11.glEnable(2896);
        GL11.glDepthMask(true);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }

    ;
}
