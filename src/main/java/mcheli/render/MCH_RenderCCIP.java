package mcheli.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.weapon.MCH_WeaponSet;
import mcheli.wrapper.W_MOD;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;

public class MCH_RenderCCIP {
    private static final ResourceLocation CCIP = new ResourceLocation(W_MOD.DOMAIN, "textures/ccip.png");
    private static final int ICON_SIZE_PX = 32;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null || mc.theWorld == null) {
            return;
        }
        MCH_EntityAircraft ac = null;
        if (player.ridingEntity instanceof MCH_EntityAircraft) {
            ac = (MCH_EntityAircraft)player.ridingEntity;
        } else if (player.ridingEntity instanceof MCH_EntitySeat) {
            ac = ((MCH_EntitySeat)player.ridingEntity).getParent();
        } else if (player.ridingEntity instanceof MCH_EntityUavStation) {
            ac = ((MCH_EntityUavStation)player.ridingEntity).getControlAircract();
        }
        if (ac == null) {
            return;
        }
        MCH_WeaponSet currentWs = ac.getCurrentWeapon(player);
        if (currentWs == null || currentWs.getInfo() == null || currentWs.getInfo().type == null || !currentWs.getInfo().type.equalsIgnoreCase("rocket") || !currentWs.getInfo().ccip) {
            return;
        }
        Vec3 impact = ac.getPredictedImpactPoint(player);
        if (impact == null) {
            return;
        }
        RenderManager rm = RenderManager.instance;
        final double x = impact.xCoord - rm.viewerPosX;
        final double y = impact.yCoord - rm.viewerPosY + 0.2D;
        final double z = impact.zCoord - rm.viewerPosZ;
        final double dist = Math.sqrt(x * x + y * y + z * z);
        if (dist < 0.5D) {
            return;
        }
        ScaledResolution sc = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        double fovRad = Math.toRadians(mc.gameSettings.fovSetting);
        float sPerPixel = (float)((2.0D * dist * Math.tan(fovRad * 0.5D)) / sc.getScaledHeight_double());

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glRotatef(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(rm.playerViewX, 1.0F, 0.0F, 0.0F);
        GL11.glScalef(-sPerPixel, -sPerPixel, sPerPixel);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glColor4f(0.1F, 1.0F, 0.1F, 0.9F);
        mc.getTextureManager().bindTexture(CCIP);
        Tessellator tess = Tessellator.instance;
        float half = ICON_SIZE_PX * 0.5F;
        tess.startDrawingQuads();
        tess.addVertexWithUV(-half, half, 0.0D, 0.0D, 1.0D);
        tess.addVertexWithUV(half, half, 0.0D, 1.0D, 1.0D);
        tess.addVertexWithUV(half, -half, 0.0D, 1.0D, 0.0D);
        tess.addVertexWithUV(-half, -half, 0.0D, 0.0D, 0.0D);
        tess.draw();
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }
}
