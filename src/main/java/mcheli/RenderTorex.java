package mcheli;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;

public class RenderTorex extends Render {

    private static final ResourceLocation CLOUDLET_TEXTURE =
        new ResourceLocation("hbm:textures/particle/particle_base.png");
    private static final ResourceLocation FLASH_TEXTURE =
        new ResourceLocation("hbm:textures/particle/flare.png");

    @Override
    public void doRender(Entity entity, double x, double y, double z,
                         float yaw, float partialTicks) {

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);

        boolean fogEnabled = GL11.glIsEnabled(GL11.GL_FOG);
        if (fogEnabled) {
            GL11.glDisable(GL11.GL_FOG);
        }

        EntityNukeTorex cloud = (EntityNukeTorex) entity;

        // 主体云渲染
        renderCloudlets(cloud, partialTicks);

        // 爆心闪光（前 100tick）
        if (cloud.ticksExisted < 101) {
            renderFlash(cloud, partialTicks);
        }

        // 屏幕闪白时间戳（只在爆炸刚开始的前 10tick 内更新）
//        if (cloud.ticksExisted < 10 &&
//            System.currentTimeMillis() - ModEventHandlerClient.flashTimestamp > 1000L) {
//            ModEventHandlerClient.flashTimestamp = System.currentTimeMillis();
//        }

        // 震屏：在客户端播放了核爆音效之后，延迟一段时间触发一次
//        if (cloud.didPlaySound && !cloud.didShake &&
//            System.currentTimeMillis() - ModEventHandlerClient.shakeTimestamp > 1000L) {
//
//            ModEventHandlerClient.shakeTimestamp = System.currentTimeMillis();
//            cloud.didShake = true;
//
//            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
//            if (player != null) {
//                player.hurtTime = 15;
//                player.maxHurtTime = 15;
//                player.attackedAtYaw = 0.0F;
//            }
//        }

        if (fogEnabled) {
            GL11.glEnable(GL11.GL_FOG);
        }

        GL11.glPopMatrix();
    }

    /** 按距离从远到近排序 cloudlet，用于正确的透明混合 */
    private final Comparator<EntityNukeTorex.Cloudlet> cloudSorter =
        (c1, c2) -> {
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            if (player == null) return 0;

            double d1 = player.getDistanceSq(c1.posX, c1.posY, c1.posZ);
            double d2 = player.getDistanceSq(c2.posX, c2.posY, c2.posZ);

            return Double.compare(d2, d1);
        };

    /** 包一层 GL 状态，渲染所有云团（texture = cloudlet） */
    private void renderCloudlets(EntityNukeTorex cloud, float partialTicks) {
        GL11.glPushMatrix();

        GL11.glEnable(GL11.GL_BLEND);
        // srcRGB, dstRGB, srcAlpha, dstAlpha
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);

        GL11.glAlphaFunc(GL11.GL_GREATER, 0.0F);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDepthMask(false);

        RenderHelper.enableStandardItemLighting();

        this.bindTexture(CLOUDLET_TEXTURE);

        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();

        // 拷贝一份并按距离排序（从远到近）
        ArrayList<EntityNukeTorex.Cloudlet> list = new ArrayList<>(cloud.cloudlets);
        list.sort(this.cloudSorter);

        for (EntityNukeTorex.Cloudlet c : list) {
            Vec3 pos = c.getInterpPos(partialTicks);
            double rx = pos.xCoord - cloud.posX;
            double ry = pos.yCoord - cloud.posY;
            double rz = pos.zCoord - cloud.posZ;
            tessellateCloudlet(tess, rx, ry, rz, c, partialTicks);
        }

        tess.draw();

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        RenderHelper.disableStandardItemLighting();
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
        GL11.glDisable(GL11.GL_BLEND);

        GL11.glPopMatrix();
    }

    /** 爆心闪光贴图，叠加模式 additive（texture = flash） */
    private void renderFlash(EntityNukeTorex cloud, float partialTicks) {
        GL11.glPushMatrix();

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        GL11.glAlphaFunc(GL11.GL_GREATER, 0.0F);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDepthMask(false);

        RenderHelper.enableStandardItemLighting();

        this.bindTexture(FLASH_TEXTURE);

        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();

        double age = Math.min(cloud.ticksExisted + partialTicks, 100.0D);
        float alpha = (float) ((100.0D - age) / 100.0D);

        Random rand = new Random(cloud.getEntityId());

        for (int i = 0; i < 3; i++) {
            float ox = (float) (rand.nextGaussian() * 0.5D * cloud.rollerSize);
            float oy = (float) (rand.nextGaussian() * 0.5D * cloud.rollerSize);
            float oz = (float) (rand.nextGaussian() * 0.5D * cloud.rollerSize);

            tessellateFlash(
                tess,
                ox,
                oy + cloud.coreHeight,
                oz,
                (float) (25.0D * cloud.rollerSize),
                alpha,
                partialTicks
            );
        }

        tess.draw();

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        RenderHelper.disableStandardItemLighting();
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
        GL11.glDisable(GL11.GL_BLEND);

        GL11.glPopMatrix();
    }

    /** 画单个 cloudlet（始终对着摄像机的四边形） */
    private void tessellateCloudlet(Tessellator tess,
                                    double posX, double posY, double posZ,
                                    EntityNukeTorex.Cloudlet cloud, float partialTicks) {

        float alpha = cloud.getAlpha();
        float scale = cloud.getScale();

        // 摄像机朝向参数（相机 billboarding）
        float rotX  = ActiveRenderInfo.rotationX;
        float rotXZ = ActiveRenderInfo.rotationXZ;
        float rotZ  = ActiveRenderInfo.rotationZ;
        float rotYZ = ActiveRenderInfo.rotationYZ;
        float rotXY = ActiveRenderInfo.rotationXY;

        float brightness = (cloud.type == EntityNukeTorex.TorexType.CONDENSATION)
            ? 0.9F
            : (0.75F * cloud.colorMod);

        Vec3 color = cloud.getInterpColor(partialTicks);

        tess.setColorRGBA_F(
            (float) color.xCoord * brightness,
            (float) color.yCoord * brightness,
            (float) color.zCoord * brightness,
            alpha
        );

        // 四个顶点（带 UV），标准粒子朝向相机
        tess.addVertexWithUV(
            posX - (rotX * scale) - (rotZ * scale),
            posY - (rotXY * scale),
            posZ - (rotXZ * scale) - (rotYZ * scale),
            1.0D, 1.0D
        );
        tess.addVertexWithUV(
            posX - (rotX * scale) + (rotZ * scale),
            posY + (rotXY * scale),
            posZ - (rotXZ * scale) + (rotYZ * scale),
            1.0D, 0.0D
        );
        tess.addVertexWithUV(
            posX + (rotX * scale) + (rotZ * scale),
            posY + (rotXY * scale),
            posZ + (rotXZ * scale) + (rotYZ * scale),
            0.0D, 0.0D
        );
        tess.addVertexWithUV(
            posX + (rotX * scale) - (rotZ * scale),
            posY - (rotXY * scale),
            posZ + (rotXZ * scale) - (rotYZ * scale),
            0.0D, 1.0D
        );
    }

    /** 画爆心闪光（也是一个朝向相机的四边形） */
    private void tessellateFlash(Tessellator tess,
                                 double posX, double posY, double posZ,
                                 float scale, float alpha, float partialTicks) {

        float rotX  = ActiveRenderInfo.rotationX;
        float rotXZ = ActiveRenderInfo.rotationXZ;
        float rotZ  = ActiveRenderInfo.rotationZ;
        float rotYZ = ActiveRenderInfo.rotationYZ;
        float rotXY = ActiveRenderInfo.rotationXY;

        tess.setColorRGBA_F(1.0F, 1.0F, 1.0F, alpha);

        tess.addVertexWithUV(
            posX - (rotX * scale) - (rotZ * scale),
            posY - (rotXY * scale),
            posZ - (rotXZ * scale) - (rotYZ * scale),
            1.0D, 1.0D
        );
        tess.addVertexWithUV(
            posX - (rotX * scale) + (rotZ * scale),
            posY + (rotXY * scale),
            posZ - (rotXZ * scale) + (rotYZ * scale),
            1.0D, 0.0D
        );
        tess.addVertexWithUV(
            posX + (rotX * scale) + (rotZ * scale),
            posY + (rotXY * scale),
            posZ + (rotXZ * scale) + (rotYZ * scale),
            0.0D, 0.0D
        );
        tess.addVertexWithUV(
            posX + (rotX * scale) - (rotZ * scale),
            posY - (rotXY * scale),
            posZ + (rotXZ * scale) - (rotYZ * scale),
            0.0D, 1.0D
        );
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        // 实际不会用到，因为渲染过程中手动 bindTexture
        return null;
    }
}
