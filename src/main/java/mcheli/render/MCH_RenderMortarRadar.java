package mcheli.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.util.ArrayList;
import java.util.List;
import mcheli.MCH_EntityInfo;
import mcheli.MCH_EntityInfoClientTracker;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponSet;
import mcheli.wrapper.W_MOD;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;

public class MCH_RenderMortarRadar {

    private static final ResourceLocation RADAR = new ResourceLocation(W_MOD.DOMAIN, "textures/mortar_radar.png");
    private static final ResourceLocation CROSS = new ResourceLocation(W_MOD.DOMAIN, "textures/mortar_cross.png");
    private static final ResourceLocation TARGET = new ResourceLocation(W_MOD.DOMAIN, "textures/mortar_target.png");
    private static final int RADAR_SIZE = 250;
    private static final int RADAR_CENTER_X = 150;
    private static final int RADAR_CENTER_Y = 280;
    private static final int CROSS_SIZE = 21;
    private static final int TARGET_SIZE = 2;
    private static final double SCREEN_HEIGHT_REFERENCE = 520.0D;
    private static final double MIN_DISTANCE = 20.0D;
    private static final double DEFAULT_MAX_DISTANCE = 300.0D;

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        World world = mc.theWorld;
        if (player == null || world == null) {
            return;
        }

        MCH_EntityAircraft ac = getAircraft(player);
        if (ac == null) {
            return;
        }

        MCH_WeaponSet weaponSet = ac.getCurrentWeapon(player);
        MCH_WeaponInfo weaponInfo = weaponSet != null ? weaponSet.getInfo() : null;
        MCH_AircraftInfo aircraftInfo = ac.getAcInfo();
        boolean weaponHasMortarRadar = weaponInfo != null && weaponInfo.hasMortarRadar;
        boolean vehicleHasMortarRadar = aircraftInfo != null && aircraftInfo.hasMortarRadar;
        if (!weaponHasMortarRadar && !vehicleHasMortarRadar) {
            return;
        }

        double maxDistance = weaponHasMortarRadar && weaponInfo.mortarRadarMaxDist > 0.0D
            ? weaponInfo.mortarRadarMaxDist
            : vehicleHasMortarRadar && aircraftInfo.mortarRadarMaxDist > 0.0D
                ? aircraftInfo.mortarRadarMaxDist
                : DEFAULT_MAX_DISTANCE;
        double currentDistance = weaponInfo != null && weaponInfo.displayMortarDistance
            ? ac.getLandInDistance(player)
            : -1.0D;

        ScaledResolution resolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        double centerX = resolution.getScaledHeight_double() * RADAR_CENTER_X / SCREEN_HEIGHT_REFERENCE;
        double centerY = resolution.getScaledHeight_double() * RADAR_CENTER_Y / SCREEN_HEIGHT_REFERENCE;
        double radarSize = resolution.getScaledHeight_double() * RADAR_SIZE / SCREEN_HEIGHT_REFERENCE;
        double radarRadius = radarSize / 2.0D;
        double crossSize = resolution.getScaledHeight_double() * CROSS_SIZE / SCREEN_HEIGHT_REFERENCE;
        double targetSize = resolution.getScaledHeight_double() * TARGET_SIZE / SCREEN_HEIGHT_REFERENCE;

        double playerX = interpolate(player.posX, player.lastTickPosX, event.partialTicks);
        double playerZ = interpolate(player.posZ, player.lastTickPosZ, event.partialTicks);
        Vec3 lookVector = getDirection(player, event.partialTicks);

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        drawTexture(mc, RADAR, centerX, centerY, radarSize);

        for (MCH_EntityInfo entity : getServerLoadedEntity()) {
            if (!isValidEntity(entity, player, ac)) {
                continue;
            }

            double entityX = interpolate(entity.posX, entity.lastTickPosX, event.partialTicks);
            double entityY = interpolate(entity.posY, entity.lastTickPosY, event.partialTicks);
            double entityZ = interpolate(entity.posZ, entity.lastTickPosZ, event.partialTicks);
            double deltaX = entityX - playerX;
            double deltaZ = entityZ - playerZ;
            double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            if (distance > maxDistance) {
                continue;
            }

            int groundY = ac.worldObj.getHeightValue((int)entityX, (int)entityZ);
            if (entityY - groundY > 10.0D) {
                continue;
            }

            Vec3 targetHorizontal = Vec3.createVectorHelper(deltaX, 0.0D, deltaZ).normalize();
            Vec3 lookHorizontal = Vec3.createVectorHelper(lookVector.xCoord, 0.0D, lookVector.zCoord).normalize();
            double dot = lookHorizontal.dotProduct(targetHorizontal);
            double angle = Math.toDegrees(Math.acos(Math.max(-1.0D, Math.min(1.0D, dot))));
            if (lookHorizontal.crossProduct(targetHorizontal).yCoord < 0.0D) {
                angle = -angle;
            }

            double distanceRatio = clampDistanceRatio(distance, maxDistance);
            double renderRadius = radarRadius * distanceRatio;
            double radians = Math.toRadians(angle);
            double markerX = centerX + renderRadius * Math.sin(-radians);
            double markerY = centerY - renderRadius * Math.cos(radians);

            drawTexture(mc, TARGET, markerX, markerY, targetSize);
            String label = getRadarName(entity, ac) + "[" + (int)distance + "]";
            int textWidth = mc.fontRenderer.getStringWidth(label);
            mc.fontRenderer.drawString(label, (int)(markerX - textWidth / 2.0D), (int)markerY, 0xFFFFFF, true);
        }

        if (currentDistance >= MIN_DISTANCE) {
            double renderRadius = radarRadius * clampDistanceRatio(currentDistance, maxDistance);
            drawTexture(mc, CROSS, centerX, centerY - renderRadius, crossSize);
        }

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private MCH_EntityAircraft getAircraft(EntityPlayer player) {
        if (player.ridingEntity instanceof MCH_EntityAircraft) {
            return (MCH_EntityAircraft)player.ridingEntity;
        }
        if (player.ridingEntity instanceof MCH_EntitySeat) {
            return ((MCH_EntitySeat)player.ridingEntity).getParent();
        }
        if (player.ridingEntity instanceof MCH_EntityUavStation) {
            return ((MCH_EntityUavStation)player.ridingEntity).getControlAircract();
        }
        return null;
    }

    private void drawTexture(Minecraft mc, ResourceLocation texture, double centerX, double centerY, double size) {
        mc.renderEngine.bindTexture(texture);
        double halfSize = size / 2.0D;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(centerX - halfSize, centerY + halfSize, 0.0D, 0.0D, 1.0D);
        tessellator.addVertexWithUV(centerX + halfSize, centerY + halfSize, 0.0D, 1.0D, 1.0D);
        tessellator.addVertexWithUV(centerX + halfSize, centerY - halfSize, 0.0D, 1.0D, 0.0D);
        tessellator.addVertexWithUV(centerX - halfSize, centerY - halfSize, 0.0D, 0.0D, 0.0D);
        tessellator.draw();
    }

    private double clampDistanceRatio(double distance, double maxDistance) {
        double range = maxDistance - MIN_DISTANCE;
        if (range <= 0.0D) {
            return 0.0D;
        }
        return Math.min(Math.max((distance - MIN_DISTANCE) / range, 0.0D), 1.0D);
    }

    private String getRadarName(MCH_EntityInfo entity, MCH_EntityAircraft ac) {
        String name = ac.getNameOnMyRadar(entity);
        if (name == null || name.isEmpty() || "?".equals(name)) {
            name = fallbackRadarName(entity);
        }
        if (name == null || name.isEmpty() || "?".equals(name)) {
            name = entity.entityName != null ? entity.entityName : "?";
        }
        return name;
    }

    private static String fallbackRadarName(MCH_EntityInfo entity) {
        MCH_AircraftInfo info = MCH_AircraftInfo.allAircraftInfo.getOrDefault(entity.entityName, null);
        if (info == null) {
            return "?";
        }
        String[] candidates = {
            info.nameOnModernAARadar, info.nameOnAdvancedAARadar, info.nameOnEarlyAARadar,
            info.nameOnModernASRadar, info.nameOnEarlyASRadar
        };
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isEmpty()) {
                return candidate;
            }
        }
        return "?";
    }

    private boolean isValidEntity(MCH_EntityInfo entity, EntityPlayer player, MCH_EntityAircraft ac) {
        if (entity == null || entity.entityClassName == null) {
            return false;
        }
        if (entity.getHorizonalDistanceSqToEntity(player) < MIN_DISTANCE * MIN_DISTANCE) {
            return false;
        }
        if (isSameTeamEntity(entity, player, ac)) {
            return false;
        }

        String className = entity.entityClassName;
        if (className.contains("MCH_EntityBaseBullet") || className.contains("MCH_EntityBullet")
            || className.contains("MCH_EntityRocket") || className.contains("MCH_EntityAAMissile")
            || className.contains("MCH_EntityATMissile") || className.contains("MCH_EntityASMissile")
            || className.contains("MCH_EntityTvMissile") || className.contains("MCH_EntityTorpedo")
            || className.contains("MCH_EntityBomb") || className.contains("MCH_EntityDispensedItem")
            || className.contains("MCH_EntityMarkerRocket") || className.contains("MCH_EntityChaff")
            || className.contains("MCH_EntityFlare")) {
            return false;
        }
        return className.contains("MCH_EntityHeli") || className.contains("MCP_EntityPlane")
            || className.contains("MCH_EntityTank") || className.contains("MCH_EntityVehicle")
            || className.contains("EntityPlayer") || className.contains("MCH_EntityNPC")
            || className.contains("MCH_EntityGunner") || className.contains("EntitySoldier");
    }

    private boolean isSameTeamEntity(MCH_EntityInfo info, EntityPlayer player, MCH_EntityAircraft ac) {
        if (ac.worldObj == null) {
            return false;
        }
        Entity entity = ac.worldObj.getEntityByID(info.entityId);
        if (entity instanceof MCH_EntityAircraft) {
            return ((MCH_EntityAircraft)entity).isMountedSameTeamEntity(player);
        }
        if (entity instanceof EntityLivingBase && player.getTeam() != null
            && ((EntityLivingBase)entity).getTeam() != null) {
            return player.isOnSameTeam((EntityLivingBase)entity);
        }
        return false;
    }

    public Vec3 getDirection(Entity entity, float partialTicks) {
        if (partialTicks == 1.0F) {
            float yawCos = MathHelper.cos(-entity.rotationYaw * 0.017453292F - (float)Math.PI);
            float yawSin = MathHelper.sin(-entity.rotationYaw * 0.017453292F - (float)Math.PI);
            float pitchCos = -MathHelper.cos(-entity.rotationPitch * 0.017453292F);
            float pitchSin = MathHelper.sin(-entity.rotationPitch * 0.017453292F);
            return Vec3.createVectorHelper(yawSin * pitchCos, pitchSin, yawCos * pitchCos);
        }

        float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
        float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
        float yawCos = MathHelper.cos(-yaw * 0.017453292F - (float)Math.PI);
        float yawSin = MathHelper.sin(-yaw * 0.017453292F - (float)Math.PI);
        float pitchCos = -MathHelper.cos(-pitch * 0.017453292F);
        float pitchSin = MathHelper.sin(-pitch * 0.017453292F);
        return Vec3.createVectorHelper(yawSin * pitchCos, pitchSin, yawCos * pitchCos);
    }

    private double interpolate(double current, double previous, float partialTicks) {
        return previous + (current - previous) * partialTicks;
    }

    public List<MCH_EntityInfo> getServerLoadedEntity() {
        return new ArrayList<MCH_EntityInfo>(MCH_EntityInfoClientTracker.getAllTrackedEntities());
    }
}
