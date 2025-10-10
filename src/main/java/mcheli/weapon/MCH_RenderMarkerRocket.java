package mcheli.weapon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.wrapper.W_Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_RenderMarkerRocket extends MCH_RenderBulletBase {

   public void renderBullet(Entity entity, double posX, double posY, double posZ, float yaw, float tickTime) {
      GL11.glPushMatrix();
      GL11.glTranslated(posX, posY, posZ);
      GL11.glRotatef(-entity.rotationYaw, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(entity.rotationPitch, 1.0F, 0.0F, 0.0F);
      this.renderModel(MCH_DefaultBulletModels.Rocket);
      GL11.glPopMatrix();
   }

   protected ResourceLocation getEntityTexture(Entity entity) {
      return W_Render.TEX_DEFAULT;
   }
}
