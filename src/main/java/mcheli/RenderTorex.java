package mcheli;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class RenderTorex extends Render {

    private static final ResourceLocation CLOUDLET_TEXTURE =
        new ResourceLocation("mcheli:textures/nuke.png");
    private static final ResourceLocation FLASH_TEXTURE =
        new ResourceLocation("mcheli:textures/nuke.png");

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

        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean lightingEnabled = GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean depthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        if (cullEnabled) {
            GL11.glDisable(GL11.GL_CULL_FACE);
        }
        if (lightingEnabled) {
            GL11.glDisable(GL11.GL_LIGHTING);
        }

        GL11.glEnable(GL11.GL_BLEND);
        // srcRGB, dstRGB, srcAlpha, dstAlpha
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);

        GL11.glAlphaFunc(GL11.GL_GREATER, 0.0F);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_LIGHTING);
        if (depthTestEnabled) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }

        this.bindTexture(CLOUDLET_TEXTURE);

        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();

        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        double distSq = player == null ? 0.0D : player.getDistanceSq(
            cloud.posX,
            cloud.posY + cloud.coreHeight * 0.5D,
            cloud.posZ
        );
        int drawLimit = getCloudletDrawLimit(distSq);

        List<EntityNukeTorex.Cloudlet> list;
        boolean doSort = cloud.cloudlets.size() <= 3000 || distSq < 10000.0D;
        if (doSort) {
            ArrayList<EntityNukeTorex.Cloudlet> sorted = new ArrayList<>(cloud.cloudlets);
            sorted.sort(this.cloudSorter);
            list = sorted;
        } else {
            list = cloud.cloudlets;
        }

        int size = list.size();
        int step = Math.max(1, size / Math.max(1, drawLimit));

        for (int i = 0; i < size; i += step) {
            EntityNukeTorex.Cloudlet c = list.get(i);
            Vec3 pos = c.getInterpPos(partialTicks);
            double rx = pos.xCoord - cloud.posX;
            double ry = pos.yCoord - cloud.posY;
            double rz = pos.zCoord - cloud.posZ;
            tessellateCloudlet(tess, rx, ry, rz, pos.xCoord, pos.yCoord, pos.zCoord, c, partialTicks, cloud.ticksExisted);
        }

        tess.draw();

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        if (depthTestEnabled) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }
        if (lightingEnabled) {
            GL11.glEnable(GL11.GL_LIGHTING);
        }
        if (cullEnabled) {
            GL11.glEnable(GL11.GL_CULL_FACE);
        }
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
        GL11.glDisable(GL11.GL_BLEND);

        GL11.glPopMatrix();
    }

    /** 爆心闪光贴图，叠加模式 additive（texture = flash） */
    private void renderFlash(EntityNukeTorex cloud, float partialTicks) {
        GL11.glPushMatrix();

        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean lightingEnabled = GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean depthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        if (cullEnabled) {
            GL11.glDisable(GL11.GL_CULL_FACE);
        }
        if (lightingEnabled) {
            GL11.glDisable(GL11.GL_LIGHTING);
        }

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        GL11.glAlphaFunc(GL11.GL_GREATER, 0.0F);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_LIGHTING);
        if (depthTestEnabled) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }

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
                cloud.posX + ox,
                cloud.posY + oy + cloud.coreHeight,
                cloud.posZ + oz,
                (float) (25.0D * cloud.rollerSize),
                alpha,
                partialTicks
            );
        }

        tess.draw();

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        if (depthTestEnabled) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }
        if (lightingEnabled) {
            GL11.glEnable(GL11.GL_LIGHTING);
        }
        if (cullEnabled) {
            GL11.glEnable(GL11.GL_CULL_FACE);
        }
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
        GL11.glDisable(GL11.GL_BLEND);

        GL11.glPopMatrix();
    }

    /** 画单个 cloudlet（始终对着摄像机的四边形） */
    private void tessellateCloudlet(Tessellator tess,
                                    double posX, double posY, double posZ,
                                    double worldX, double worldY, double worldZ,
                                    EntityNukeTorex.Cloudlet cloud, float partialTicks, int cloudAgeTick) {

        float alpha = cloud.getAlpha();
        float scale = cloud.getScale();
        BillboardAxes axes = this.buildBillboardAxes(worldX, worldY, worldZ);

        float brightness = (cloud.type == EntityNukeTorex.TorexType.CONDENSATION)
            ? 0.9F
            : (0.75F * cloud.colorMod);
        float earlyBoost = 1.0F + Math.max(0.0F, 1.0F - (cloudAgeTick + partialTicks) / 120.0F) * 0.65F;
        brightness = Math.min(1.8F, brightness * 1.45F * earlyBoost);

        Vec3 color = cloud.getInterpColor(partialTicks);
        float cr = Math.min(1.0F, (float) color.xCoord * brightness);
        float cg = Math.min(1.0F, (float) color.yCoord * brightness);
        float cb = Math.min(1.0F, (float) color.zCoord * brightness);

        tess.setColorRGBA_F(
            cr,
            cg,
            cb,
            alpha
        );

        double rx = axes.rightX * scale;
        double ry = axes.rightY * scale;
        double rz = axes.rightZ * scale;
        double ux = axes.upX * scale;
        double uy = axes.upY * scale;
        double uz = axes.upZ * scale;

        tess.addVertexWithUV(
            posX - rx - ux,
            posY - ry - uy,
            posZ - rz - uz,
            1.0D, 1.0D
        );
        tess.addVertexWithUV(
            posX - rx + ux,
            posY - ry + uy,
            posZ - rz + uz,
            1.0D, 0.0D
        );
        tess.addVertexWithUV(
            posX + rx + ux,
            posY + ry + uy,
            posZ + rz + uz,
            0.0D, 0.0D
        );
        tess.addVertexWithUV(
            posX + rx - ux,
            posY + ry - uy,
            posZ + rz - uz,
            0.0D, 1.0D
        );
    }

    /** 画爆心闪光（也是一个朝向相机的四边形） */
    private void tessellateFlash(Tessellator tess,
                                 double posX, double posY, double posZ,
                                 double worldX, double worldY, double worldZ,
                                 float scale, float alpha, float partialTicks) {

        BillboardAxes axes = this.buildBillboardAxes(worldX, worldY, worldZ);
        float flashAlpha = Math.min(1.0F, alpha * 1.25F);

        tess.setColorRGBA_F(1.0F, 1.0F, 1.0F, flashAlpha);

        double rx = axes.rightX * scale;
        double ry = axes.rightY * scale;
        double rz = axes.rightZ * scale;
        double ux = axes.upX * scale;
        double uy = axes.upY * scale;
        double uz = axes.upZ * scale;

        tess.addVertexWithUV(
            posX - rx - ux,
            posY - ry - uy,
            posZ - rz - uz,
            1.0D, 1.0D
        );
        tess.addVertexWithUV(
            posX - rx + ux,
            posY - ry + uy,
            posZ - rz + uz,
            1.0D, 0.0D
        );
        tess.addVertexWithUV(
            posX + rx + ux,
            posY + ry + uy,
            posZ + rz + uz,
            0.0D, 0.0D
        );
        tess.addVertexWithUV(
            posX + rx - ux,
            posY + ry - uy,
            posZ + rz - uz,
            0.0D, 1.0D
        );
    }

    private int getCloudletDrawLimit(double distSq) {
        int particleSetting = Minecraft.getMinecraft().gameSettings.particleSetting;
        int baseLimit = particleSetting == 0 ? 6200 : (particleSetting == 1 ? 3600 : 2000);
        double distScale = distSq > 40000.0D ? 0.35D
            : (distSq > 14400.0D ? 0.55D : (distSq > 6400.0D ? 0.75D : 1.0D));
        return Math.max(300, (int) (baseLimit * distScale));
    }

    private BillboardAxes buildBillboardAxes(double worldX, double worldY, double worldZ) {
        double lookX = this.renderManager.viewerPosX - worldX;
        double lookY = this.renderManager.viewerPosY - worldY;
        double lookZ = this.renderManager.viewerPosZ - worldZ;
        double lookLen = Math.sqrt(lookX * lookX + lookY * lookY + lookZ * lookZ);
        if (lookLen < 1.0E-6D) {
            return BillboardAxes.fromActiveRenderInfo();
        }
        lookX /= lookLen;
        lookY /= lookLen;
        lookZ /= lookLen;

        double upX = 0.0D;
        double upY = 1.0D;
        double upZ = 0.0D;
        if (Math.abs(lookY) > 0.98D) {
            upX = 1.0D;
            upY = 0.0D;
            upZ = 0.0D;
        }

        double rightX = upY * lookZ - upZ * lookY;
        double rightY = upZ * lookX - upX * lookZ;
        double rightZ = upX * lookY - upY * lookX;
        double rightLen = Math.sqrt(rightX * rightX + rightY * rightY + rightZ * rightZ);
        if (rightLen < 1.0E-6D) {
            return BillboardAxes.fromActiveRenderInfo();
        }
        rightX /= rightLen;
        rightY /= rightLen;
        rightZ /= rightLen;

        double finalUpX = lookY * rightZ - lookZ * rightY;
        double finalUpY = lookZ * rightX - lookX * rightZ;
        double finalUpZ = lookX * rightY - lookY * rightX;

        return new BillboardAxes(rightX, rightY, rightZ, finalUpX, finalUpY, finalUpZ);
    }

    private static class BillboardAxes {
        final double rightX;
        final double rightY;
        final double rightZ;
        final double upX;
        final double upY;
        final double upZ;

        BillboardAxes(double rightX, double rightY, double rightZ, double upX, double upY, double upZ) {
            this.rightX = rightX;
            this.rightY = rightY;
            this.rightZ = rightZ;
            this.upX = upX;
            this.upY = upY;
            this.upZ = upZ;
        }

        static BillboardAxes fromActiveRenderInfo() {
            double rightX = ActiveRenderInfo.rotationX;
            double rightY = ActiveRenderInfo.rotationXZ;
            double rightZ = ActiveRenderInfo.rotationZ;
            double rightLen = Math.sqrt(rightX * rightX + rightY * rightY + rightZ * rightZ);
            if (rightLen > 1.0E-6D) {
                rightX /= rightLen;
                rightY /= rightLen;
                rightZ /= rightLen;
            } else {
                rightX = 1.0D;
                rightY = 0.0D;
                rightZ = 0.0D;
            }
            double upX = ActiveRenderInfo.rotationXY;
            double upY = ActiveRenderInfo.rotationXZ;
            double upZ = ActiveRenderInfo.rotationYZ;
            double upLen = Math.sqrt(upX * upX + upY * upY + upZ * upZ);
            if (upLen > 1.0E-6D) {
                upX /= upLen;
                upY /= upLen;
                upZ /= upLen;
            } else {
                upX = 0.0D;
                upY = 1.0D;
                upZ = 0.0D;
            }
            return new BillboardAxes(
                rightX, rightY, rightZ,
                upX, upY, upZ
            );
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        // 实际不会用到，因为渲染过程中手动 bindTexture
        return null;
    }
}
