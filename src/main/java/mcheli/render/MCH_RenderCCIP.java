package mcheli.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponSet;
import mcheli.wrapper.W_MOD;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MCH_RenderCCIP {
    private static final ResourceLocation DEFAULT_CCIP = new ResourceLocation(W_MOD.DOMAIN, "textures/ccip.png");
    private static final Map<String, ResourceLocation> CCIP_TEXTURE_CACHE = new HashMap<String, ResourceLocation>();
    private static final Map<Integer, CcipRenderState> CCIP_RENDER_STATE = new HashMap<Integer, CcipRenderState>();
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
        if (currentWs == null || currentWs.getInfo() == null || currentWs.getInfo().type == null || !isCCIPSupportedType(currentWs.getInfo().type) || !currentWs.getInfo().ccip) {
            return;
        }
        MCH_WeaponInfo info = currentWs.getInfo();
        Vec3 impact = ac.getPredictedImpactPoint(player);
        if (impact == null) {
            return;
        }
        int currentWeaponId = ac.getCurrentWeaponID(player);
        int stateKey = ac.getEntityId();
        int nowTick = player.ticksExisted;
        CcipRenderState state = CCIP_RENDER_STATE.get(Integer.valueOf(stateKey));
        if (state == null) {
            state = new CcipRenderState();
            CCIP_RENDER_STATE.put(Integer.valueOf(stateKey), state);
        }
        if (!state.initialized || state.weaponId != currentWeaponId || nowTick - state.lastSeenTick > 8) {
            state.x = impact.xCoord;
            state.y = impact.yCoord;
            state.z = impact.zCoord;
            state.initialized = true;
            state.weaponId = currentWeaponId;
        } else {
            double dx = impact.xCoord - state.x;
            double dy = impact.yCoord - state.y;
            double dz = impact.zCoord - state.z;
            double err = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double alpha = Math.max(0.16D, Math.min(0.62D, 0.20D + err * 0.08D));
            state.x += dx * alpha;
            state.y += dy * alpha;
            state.z += dz * alpha;
        }
        state.lastSeenTick = nowTick;
        impact = Vec3.createVectorHelper(state.x, state.y, state.z);
        if (CCIP_RENDER_STATE.size() > 24 && (nowTick & 31) == 0) {
            Iterator<Map.Entry<Integer, CcipRenderState>> it = CCIP_RENDER_STATE.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, CcipRenderState> entry = it.next();
                if (nowTick - entry.getValue().lastSeenTick > 80) {
                    it.remove();
                }
            }
        }
        boolean hasNearbyEntity = hasEntityAroundImpact(ac, player, impact, 3.0D);
        ResourceLocation ccipTexture = getCCIPTexture(info.ccipTexture);
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
        GL11.glRotatef(ac.getRotRoll(), 0.0F, 0.0F, 1.0F);
        GL11.glScalef(-sPerPixel, -sPerPixel, sPerPixel);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_LIGHTING);
        if (hasNearbyEntity) {
            GL11.glColor4f(1.0F, 0.1F, 0.1F, 0.9F);
        } else {
            GL11.glColor4f(0.1F, 1.0F, 0.1F, 0.9F);
        }
        mc.getTextureManager().bindTexture(ccipTexture);
        Tessellator tess = Tessellator.instance;
        float ccipFactor = info.ccipFactor;
        if (ccipFactor < 0.1F) {
            ccipFactor = 0.1F;
        } else if (ccipFactor > 10.0F) {
            ccipFactor = 10.0F;
        }
        float half = ICON_SIZE_PX * 0.5F * ccipFactor;
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

    private static ResourceLocation getCCIPTexture(String textureName) {
        String name = textureName == null ? "" : textureName.trim();
        if (name.isEmpty()) {
            return DEFAULT_CCIP;
        }
        String key = name.toLowerCase();
        ResourceLocation cached = CCIP_TEXTURE_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        ResourceLocation texture = new ResourceLocation(W_MOD.DOMAIN, "textures/" + key + ".png");
        CCIP_TEXTURE_CACHE.put(key, texture);
        return texture;
    }

    private static boolean hasEntityAroundImpact(MCH_EntityAircraft ac, EntityPlayer player, Vec3 impact, double radius) {
        AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(
            impact.xCoord - radius, impact.yCoord - radius, impact.zCoord - radius,
            impact.xCoord + radius, impact.yCoord + radius, impact.zCoord + radius
        );
        List list = ac.worldObj.getEntitiesWithinAABB(Entity.class, aabb);
        if (list == null || list.isEmpty()) {
            return false;
        }
        for (Object obj : list) {
            if (!(obj instanceof Entity)) {
                continue;
            }
            Entity entity = (Entity) obj;
            if (entity == player || entity == ac || entity.isDead) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static boolean isCCIPSupportedType(String type) {
        return type.equalsIgnoreCase("rocket")
            || type.equalsIgnoreCase("atmissile")
            || type.equalsIgnoreCase("tvmissile");
    }

    private static class CcipRenderState {
        double x;
        double y;
        double z;
        int lastSeenTick;
        int weaponId = -1;
        boolean initialized;
    }
}
