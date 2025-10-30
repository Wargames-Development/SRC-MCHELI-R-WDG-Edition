package mcheli.throwable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Camera;
import mcheli.wrapper.W_Render;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_RenderThrowable extends W_Render {

    /**
     * 光晕半径
     */
    private static final float _RADIUS = 6.0F;
    /**
     * 光晕亮度
     */
    private static final float BRIGHTNESS = 1.0F;

    public MCH_RenderThrowable() {
        super.shadowSize = 0.0F;
    }

    @Override
    public void doRender(Entity entity, double posX, double posY, double posZ,
                         float yaw, float partialTicks) {
        if (!(entity instanceof MCH_EntityThrowable)) {
            return;
        }

        MCH_EntityThrowable throwable = (MCH_EntityThrowable) entity;
        MCH_ThrowableInfo info = throwable.getInfo();
        if (info == null) {
            return;
        }

        GL11.glPushMatrix();
        // 移动到实体位置
        GL11.glTranslated(posX, posY, posZ);

        // 保存并设置全亮
        final float prevBrightnessX = OpenGlHelper.lastBrightnessX;
        final float prevBrightnessY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);

        // ==== 渲染光晕（仅手持照明弹时） ====
        if (info.handFlare) {
            renderLight(entity, partialTicks);
        }

        // ==== 渲染模型 ====
        // 将模型旋转到实体角度
        GL11.glRotatef(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks, 0.0F, -1.0F, 0.0F);
        GL11.glRotatef(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks, 1.0F, 0.0F, 0.0F);

        this.setCommonRenderParam(true, entity.getBrightnessForRender(partialTicks));
        if (info.model != null) {
            this.bindTexture("textures/throwable/" + info.name + ".png");
            info.model.renderAll();
        }
        this.restoreCommonRenderParam();

        // 还原亮度
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevBrightnessX, prevBrightnessY);

        GL11.glPopMatrix();
    }

    /**
     * 渲染圆形光晕 - 热成像模式下禁用透明度
     *
     * @param entity       实体
     * @param partialTicks 部分刻
     */
    private void renderLight(Entity entity, float partialTicks) {
        float RADIUS = _RADIUS;
        boolean isThermalVision = MCH_Camera.currentCameraMode == MCH_Camera.MODE_THERMALVISION;

        if (isThermalVision) {
            RADIUS += 4f;
        }

        // 参考探照灯渲染的设置
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glEnable(GL11.GL_BLEND);

        // 根据模式选择不同的混合函数
        if (isThermalVision) {
            // 热成像模式：禁用透明度，使用不透明混合
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ZERO); // 不透明混合
        } else {
            // 正常模式：使用透明混合
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
        }

        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(false); // 禁用深度写入，这样光晕不会被方块挡住

        GL11.glPushMatrix();

        // 使光晕始终朝向玩家
        GL11.glRotatef(-RenderManager.instance.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(RenderManager.instance.playerViewX, 1.0F, 0.0F, 0.0F);

        // 稍微向前移动以避免z-fighting
        GL11.glTranslatef(0.0F, 0.0F, 0.1F);

        Tessellator tessellator = Tessellator.instance;

        // 使用多层同心圆环实现径向渐变
        int layers = 16; // 层数，越多渐变越平滑
        int segments = 32; // 每层的分段数

        for (int layer = 0; layer < layers; layer++) {
            float innerRadius = (layer * RADIUS) / layers;
            float outerRadius = ((layer + 1) * RADIUS) / layers;

            // 计算内圈和外圈的颜色和透明度
            float innerProgress = innerRadius / RADIUS;
            float outerProgress = outerRadius / RADIUS;

            float innerRed, innerGreen, innerBlue, innerAlpha;
            float outerRed, outerGreen, outerBlue, outerAlpha;

            if (isThermalVision) {
                // 热成像模式：品红色，完全不透明
                innerRed = 1.0F;
                innerGreen = 0.0F;
                innerBlue = 1.0F;
                innerAlpha = 1.0F; // 完全不透明

                outerRed = 1.0F;
                outerGreen = 0.0F;
                outerBlue = 1.0F;
                outerAlpha = 1.0F; // 完全不透明
            } else {
                // 正常模式：红色到白色渐变
                innerRed = 1.0F;
                innerGreen = innerProgress; // 从0到1
                innerBlue = innerProgress;  // 从0到1
                innerAlpha = 1.0F - innerProgress * innerProgress; // 使用平方曲线使中心更不透明

                outerRed = 1.0F;
                outerGreen = outerProgress; // 从0到1
                outerBlue = outerProgress;  // 从0到1
                outerAlpha = 1.0F - outerProgress * outerProgress; // 使用平方曲线使中心更不透明
            }

            // 应用亮度
            innerRed *= BRIGHTNESS;
            innerGreen *= BRIGHTNESS;
            innerBlue *= BRIGHTNESS;
            outerRed *= BRIGHTNESS;
            outerGreen *= BRIGHTNESS;
            outerBlue *= BRIGHTNESS;

            tessellator.startDrawing(GL11.GL_QUAD_STRIP);

            for (int i = 0; i <= segments; i++) {
                double angle = 2.0 * Math.PI * i / segments;
                double sin = Math.sin(angle);
                double cos = Math.cos(angle);

                // 内圈点
                double xInner = sin * innerRadius;
                double yInner = cos * innerRadius;
                tessellator.setColorRGBA_F(innerRed, innerGreen, innerBlue, innerAlpha);
                tessellator.addVertex(xInner, yInner, 0.0D);

                // 外圈点
                double xOuter = sin * outerRadius;
                double yOuter = cos * outerRadius;
                tessellator.setColorRGBA_F(outerRed, outerGreen, outerBlue, outerAlpha);
                tessellator.addVertex(xOuter, yOuter, 0.0D);
            }

            tessellator.draw();
        }

        GL11.glPopMatrix();

        // 恢复设置
        GL11.glDepthMask(true);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_ALPHA_TEST);

        // 恢复混合函数
        if (isThermalVision) {
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        } else {
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }

        RenderHelper.enableStandardItemLighting();
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return W_Render.TEX_DEFAULT;
    }
}
