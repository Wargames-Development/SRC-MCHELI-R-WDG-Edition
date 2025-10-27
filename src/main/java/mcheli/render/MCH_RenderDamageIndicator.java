package mcheli.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mcheli.MCH_DamageIndicator;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.plane.MCP_EntityPlane;
import mcheli.tank.MCH_EntityTank;
import mcheli.vehicle.MCH_EntityVehicle;
import mcheli.wrapper.W_MOD;
import mcheli.wrapper.W_ScaledResolution;
import mcheli.wrapper.modelloader.W_ModelCustom;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * 载具HUD渲染器 - 包含伤害指示器渲染。
 *
 * <p>本类负责在玩家骑乘载具时，在屏幕左上角绘制载具的3D模型以及受到攻击的提示信息。
 * 为了防止渲染区域超出屏幕，渲染区域的位置通过 {@link #horizontalOffset} 和 {@link #verticalOffset} 调整。
 * 此外，伤害指示器的绘制使用了相对坐标和方向，这些信息已经在服务器端根据载具的俯仰、滚转和水平旋转
 * 进行了变换。</p>
 */
public class MCH_RenderDamageIndicator {

    /**
     * 模型自旋角度，用于让模型缓慢旋转展示。
     */
    private static float spinYaw = 0.0F;
    /**
     * 模型渲染时用于计算缩放的像素基准值。调整此值可以改变模型在框中的大小。
     */
    private static final double BOX_PIXELS = 220.0;
    /**
     * 额外的缩放比例，用于微调模型大小。
     */
    private static final float EXTRA_ZOOM = 1.2F;

    /**
     * 渲染区域相对于屏幕右上角的水平偏移像素。
     * 调整此值可以让HUD离开屏幕边缘，避免被截断。
     */
    private static final int horizontalOffset = 70;
    /**
     * 渲染区域相对于屏幕顶部的垂直偏移像素。
     * 调整此值可以让HUD离开屏幕边缘，避免被截断。
     */
    private static final int verticalOffset = 30;

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if(true) {
            return;
        }
        render(Minecraft.getMinecraft(), event.partialTicks);
    }

    /**
     * 主渲染入口。玩家骑乘载具时在HUD上绘制模型和伤害指示器。
     *
     * @param mc          客户端实例
     * @param partialTicks 渲染补间因子
     */
    public static void render(Minecraft mc, float partialTicks) {
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;

        EntityPlayer player = mc.thePlayer;
        if (!(player.ridingEntity instanceof MCH_EntityAircraft)) {
            return;
        }

        MCH_EntityAircraft ac = (MCH_EntityAircraft) player.ridingEntity;
        if (ac.getAcInfo() == null) return;

        if (!(ac.getAcInfo().model instanceof W_ModelCustom)) return;

        W_ModelCustom model = (W_ModelCustom) ac.getAcInfo().model;
        if (model == null) return;

        // 获取伤害指示器列表（客户端通过数据包接收的，已经是相对坐标和相对方向）
        List<MCH_DamageIndicator> damageIndicators = ac.damageIndicatorList;

        // 绑定载具贴图
        ResourceLocation texture = getTextureForAircraft(ac);
        if (texture != null) {
            mc.getTextureManager().bindTexture(texture);
        }

        // 使用ScaledResolution获取屏幕宽高（单位：HUD缩放后的像素）
        W_ScaledResolution sr = new W_ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        // 基础框大小和外边距，以800px高度为参考自适应缩放
        final int baseBoxSize = 140;
        final int baseMargin = 6;

        // 根据屏幕高度调整框的大小，避免在低分辨率屏幕上过大
        float scaleFactor = Math.min(1.0f, (float) sh / 800.0f);
        int boxSize = (int) (baseBoxSize * scaleFactor);
        int margin = (int) (baseMargin * scaleFactor);

        // 计算渲染框中心位置（左上角偏移），注意需要减去水平偏移并加上垂直偏移
        double drawX = sw - margin - boxSize * 0.5 - horizontalOffset;
        double drawY = margin + boxSize * 0.5 + verticalOffset;

        // 根据模型的尺寸计算缩放因子，使模型完整显示在框内
        double modelSize = Math.abs(model.size) < 0.01 ? 0.01 : Math.abs(model.size);
        double scale = (BOX_PIXELS / modelSize) * EXTRA_ZOOM;

        // 计算模型中心，模型坐标范围由加载的OBJ模型提供
        double cx = (model.maxX - model.minX) * 0.5 + model.minX;
        double cy = (model.maxY - model.minY) * 0.5 + model.minY;
        double cz = (model.maxZ - model.minZ) * 0.5 + model.minZ;
        double[] center = new double[]{cx, cy, cz};

        // 自旋角度增加，让模型缓慢旋转
        spinYaw += (1.8F * partialTicks);
        if (spinYaw > 360.0F) spinYaw -= 360.0F;

        // 渲染模型和伤害指示器
        renderModel(mc, model, drawX, drawY, scale, center, damageIndicators, boxSize, margin);
    }

    /**
     * 根据载具类型获取对应的贴图路径。
     */
    private static ResourceLocation getTextureForAircraft(MCH_EntityAircraft ac) {
        if (ac.getTextureName() == null) return null;

        String texturePath;
        if (ac instanceof MCH_EntityHeli) {
            texturePath = "textures/helicopters/" + ac.getTextureName() + ".png";
        } else if (ac instanceof MCP_EntityPlane) {
            texturePath = "textures/planes/" + ac.getTextureName() + ".png";
        } else if (ac instanceof MCH_EntityTank) {
            texturePath = "textures/tanks/" + ac.getTextureName() + ".png";
        } else if (ac instanceof MCH_EntityVehicle) {
            texturePath = "textures/vehicles/" + ac.getTextureName() + ".png";
        } else {
            texturePath = null;
        }
        return texturePath != null ? new ResourceLocation(W_MOD.DOMAIN, texturePath) : null;
    }

    /**
     * 渲染3D模型及其伤害指示器。使用OpenGL配置剪裁区域，以防渲染超出框外。
     */
    private static void renderModel(Minecraft mc, W_ModelCustom model,
                                    double drawX, double drawY,
                                    double scale, double[] center,
                                    List<MCH_DamageIndicator> damageIndicators,
                                    int boxSize, int margin) {
        GL11.glPushMatrix();
        try {
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);

            // 开启深度测试，确保模型之间的遮挡关系正确
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(true);

            // 使用剪裁区域限制绘制范围，防止模型超出HUD框
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            W_ScaledResolution sr = new W_ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
            int scaleFactor = sr.getScaleFactor();
            int x = (sr.getScaledWidth() - boxSize - margin - horizontalOffset) * scaleFactor;
            int y = (margin + verticalOffset) * scaleFactor;
            int width = boxSize * scaleFactor;
            int height = boxSize * scaleFactor;
            GL11.glScissor(x, y, width, height);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);

            // 设置OpenGL状态
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glCullFace(GL11.GL_BACK);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

            // 设置模型位置和旋转：放置到屏幕位置、翻转Y轴（MC坐标系不同）并绕Y轴旋转
            GL11.glTranslated(drawX, drawY, 300.0);
            GL11.glRotated(180.0, 1.0, 0.0, 0.0);
            GL11.glRotated(spinYaw, 0.0, 1.0, 0.0);
            GL11.glScaled(scale, scale, -scale);
            GL11.glTranslated(-center[0], -center[1], -center[2]);

            // 绘制模型
            model.renderAll(0, model.getFaceNum());

            // 绘制伤害指示器：这些指示器的坐标和方向已经在服务器端变换至相对坐标
            if (damageIndicators != null && !damageIndicators.isEmpty()) {
                renderDamageIndicators(damageIndicators);
            }

        } catch (Throwable t) {
            System.err.println("Error in depth render: " + t.getMessage());
            t.printStackTrace();
        } finally {
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    /**
     * 批量绘制伤害指示器，每个指示器描述了一次子弹击中载具的位置和方向。
     * 深度测试保持开启，这样线条和标记会被载具模型正确遮挡。
     */
    private static void renderDamageIndicators(List<MCH_DamageIndicator> indicators) {
        GL11.glPushMatrix();
        try {
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
            // 关闭纹理、剔除，启用混合
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            // 深度测试保持开启，让线条和标记与模型正确交互
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            // 线条宽度
            GL11.glLineWidth(2.5F);
            for (MCH_DamageIndicator indicator : indicators) {
                if (indicator.relativeHitPos != null && indicator.relativeDir != null) {
                    renderDamageIndicator(indicator);
                }
            }
        } finally {
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    /**
     * 绘制单个伤害指示器。
     *
     * <p>使用相对命中点和相对弹道方向计算线段的起点。为了保证线段长度与伤害成正比，先归一化方向向量，然后乘以计算出的长度。</p>
     */
    private static void renderDamageIndicator(MCH_DamageIndicator indicator) {
        Vec3 hitPos = indicator.relativeHitPos;
        Vec3 bulletDir = indicator.relativeDir;
        if (hitPos == null || bulletDir == null) return;

        // 根据伤害计算线段长度，伤害越高线段越长
        double baseLength = 0.5;
        double lineLength = baseLength * (0.7 + indicator.damage / 150.0);

        // 归一化弹道方向，使其仅表示方向不受速度影响
        Vec3 dirNorm = bulletDir.normalize();

        // 起点为命中点沿着子弹方向反向偏移一定距离
        Vec3 lineStart = Vec3.createVectorHelper(
            hitPos.xCoord - dirNorm.xCoord * lineLength,
            hitPos.yCoord - dirNorm.yCoord * lineLength,
            hitPos.zCoord - dirNorm.zCoord * lineLength
        );
        Vec3 lineEnd = hitPos;

        // 根据伤害设置红色透明度，伤害越高颜色越鲜艳
        float colorIntensity = (float) Math.min(1.0, 0.4 + indicator.damage / 120.0);
        GL11.glColor4f(1.0f, 0.0f, 0.0f, colorIntensity);

        // 绘制线条
        GL11.glBegin(GL11.GL_LINES);
        try {
            GL11.glVertex3d(lineStart.xCoord, lineStart.yCoord, lineStart.zCoord);
            GL11.glVertex3d(lineEnd.xCoord, lineEnd.yCoord, lineEnd.zCoord);
        } finally {
            GL11.glEnd();
        }

        // 在命中点绘制一个小球体标记，大小根据伤害变化
        GL11.glPushMatrix();
        GL11.glTranslated(hitPos.xCoord, hitPos.yCoord, hitPos.zCoord);
        GL11.glColor4f(1.0f, 0.0f, 0.0f, colorIntensity * 0.8f);
        double sphereRadius = 0.02 + 0.03 * (indicator.damage / 100.0);
        drawSimpleSphere(sphereRadius, 6, 4);
        GL11.glPopMatrix();

        // 恢复颜色为白色
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    /**
     * 绘制一个简化球体。使用三角形扇形构造上下两半球。
     */
    private static void drawSimpleSphere(double radius, int slices, int stacks) {
        // 上半球
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex3d(0, radius, 0);
        for (int i = 0; i <= slices; i++) {
            double theta = 2.0 * Math.PI * i / slices;
            double x = Math.sin(theta) * radius;
            double z = Math.cos(theta) * radius;
            GL11.glVertex3d(x, 0, z);
        }
        GL11.glEnd();
        // 下半球
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex3d(0, -radius, 0);
        for (int i = 0; i <= slices; i++) {
            double theta = 2.0 * Math.PI * i / slices;
            double x = Math.sin(theta) * radius;
            double z = Math.cos(theta) * radius;
            GL11.glVertex3d(x, 0, z);
        }
        GL11.glEnd();
    }
}
