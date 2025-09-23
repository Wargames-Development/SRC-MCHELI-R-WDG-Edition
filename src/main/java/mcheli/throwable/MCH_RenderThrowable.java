package mcheli.throwable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.wrapper.W_Render;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_RenderThrowable extends W_Render {

   /** 半径：调大可增加光晕范围 */
   private static final float HALO_RADIUS        = 5.0F;
   /** 中心亮度，外圈会渐变至 0 */
   private static final float HALO_CENTER_ALPHA  = 0.75F;
   /** 分段数，越大越圆滑 */
   private static final int   HALO_SEGMENTS      = 48;
   /** 向摄像机方向微移，避免与模型共面引起闪烁 */
   private static final float HALO_Z_OFFSET      = 0.02F;

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

      // 保存并设置全亮，让光晕和模型看上去发光
      final float prevX = OpenGlHelper.lastBrightnessX;
      final float prevY = OpenGlHelper.lastBrightnessY;
      OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);

      // ==== 渲染光晕（仅手持照明弹时） ====
      if (info.handFlare) {
         renderHaloFacingCamera(HALO_RADIUS, HALO_CENTER_ALPHA, HALO_SEGMENTS);
      }

      // ==== 渲染模型 ====
      // 将模型旋转到实体角度
      GL11.glRotatef(entity.rotationYaw, 0.0F, -1.0F, 0.0F);
      GL11.glRotatef(entity.rotationPitch, 1.0F, 0.0F, 0.0F);

      this.setCommonRenderParam(true, entity.getBrightnessForRender(partialTicks));
      if (info.model != null) {
         this.bindTexture("textures/throwable/" + info.name + ".png");
         info.model.renderAll();
      }
      this.restoreCommonRenderParam();

      // 还原亮度
      OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevX, prevY);

      GL11.glPopMatrix();
   }

   @Override
   protected ResourceLocation getEntityTexture(Entity entity) {
      return W_Render.TEX_DEFAULT;
   }


   private void renderHaloFacingCamera(float radius, float centerAlpha, int segments) {
      // 取得玩家视角
      RenderManager rm = (this.renderManager != null) ? this.renderManager : RenderManager.instance;
      float viewYaw  = (rm != null) ? rm.playerViewY : 0.0F;
      float viewPitch= (rm != null) ? rm.playerViewX : 0.0F;

      GL11.glPushMatrix();

      // billboard：反向旋转到与摄像机一致的朝向
      GL11.glRotatef(-viewYaw,  0.0F, 1.0F, 0.0F);
      GL11.glRotatef(viewPitch, 1.0F, 0.0F, 0.0F);
      // 向摄像机方向微移，防止闪烁
      GL11.glTranslatef(0.0F, 0.0F, HALO_Z_OFFSET);

      // 保存当前渲染状态
      boolean depthEnabled   = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
      boolean alphaEnabled   = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
      boolean textureEnabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
      boolean lightingEnabled= GL11.glIsEnabled(GL11.GL_LIGHTING);
      boolean cullEnabled    = GL11.glIsEnabled(GL11.GL_CULL_FACE);
      boolean blendEnabled   = GL11.glIsEnabled(GL11.GL_BLEND);

      // 设定光晕绘制所需的状态：禁用深度测试、纹理、光照，启用加法混合
      GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glDepthMask(false);
      GL11.glDisable(GL11.GL_ALPHA_TEST);
      GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_CULL_FACE);
      GL11.glDisable(GL11.GL_TEXTURE_2D);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

      // 绘制光晕：中心亮，边缘透明
      GL11.glBegin(GL11.GL_TRIANGLE_FAN);
      // 中心顶点，使用中心透明度
      GL11.glColor4f(1.0F, 1.0F, 1.0F, centerAlpha);
      GL11.glVertex3f(0.0F, 0.0F, 0.0F);
      // 外圈顶点，透明度渐变到 0；最后一个顶点与第一个重合形成闭合
      for (int i = 0; i <= segments; ++i) {
         double angle = (2.0 * Math.PI) * i / (double) segments;
         float px = (float)Math.cos(angle) * radius;
         float py = (float)Math.sin(angle) * radius;
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.0F);
         GL11.glVertex3f(px, py, 0.0F);
      }
      GL11.glEnd();

      // 恢复渲染状态
      if (textureEnabled) GL11.glEnable(GL11.GL_TEXTURE_2D); else GL11.glDisable(GL11.GL_TEXTURE_2D);
      if (lightingEnabled) GL11.glEnable(GL11.GL_LIGHTING);   else GL11.glDisable(GL11.GL_LIGHTING);
      if (cullEnabled)    GL11.glEnable(GL11.GL_CULL_FACE);   else GL11.glDisable(GL11.GL_CULL_FACE);
      if (blendEnabled)   GL11.glEnable(GL11.GL_BLEND);        else GL11.glDisable(GL11.GL_BLEND);
      // 还原为常规混合模式
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      if (alphaEnabled)   GL11.glEnable(GL11.GL_ALPHA_TEST);    else GL11.glDisable(GL11.GL_ALPHA_TEST);
      GL11.glDepthMask(true);
      if (depthEnabled)   GL11.glEnable(GL11.GL_DEPTH_TEST);    else GL11.glDisable(GL11.GL_DEPTH_TEST);

      GL11.glPopMatrix();
   }


}
