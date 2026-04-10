package mcheli.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mcheli.MCH_EntityInfo;
import mcheli.MCH_EntityInfoClientTracker;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.weapon.MCH_WeaponBase;
import mcheli.weapon.MCH_WeaponInfo;
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

import java.util.ArrayList;
import java.util.List;

public class MCH_RenderLeadCircle {
    private static final ResourceLocation PRE_AIM = new ResourceLocation(W_MOD.DOMAIN, "textures/pre-aim_circle.png");
    private static final int ICON_SIZE_PX = 28;
    private static final double FIRE_CONTROL_MAX_ANGLE = 5.0D;
    private static final double FIRE_CONTROL_MAX_RANGE = 1000.0D;
    private static int fireControlLockedTargetId = -1;
    private static boolean fireControlKeyPrevDown = false;

    public static int getFireControlLockedTargetId() {
        return fireControlLockedTargetId;
    }

    public static int handleFireControlLockKey(boolean keyDown, EntityPlayer player, MCH_EntityAircraft ac) {
        if (!keyDown) {
            fireControlKeyPrevDown = false;
            return 0;
        }
        if (fireControlKeyPrevDown) {
            return 0;
        }
        fireControlKeyPrevDown = true;
        if (fireControlLockedTargetId > 0) {
            fireControlLockedTargetId = -1;
            return -1;
        }
        if (player == null || ac == null) {
            return 2;
        }
        MCH_WeaponSet currentWs = ac.getCurrentWeapon(player);
        if (currentWs == null || currentWs.getInfo() == null) {
            return 2;
        }
        MCH_WeaponInfo info = currentWs.getInfo();
        if (!info.enableBVR || !isSupportedWeaponType(info.type)) {
            return 2;
        }
        MCH_EntityInfo target = findFireControlTarget(ac, player);
        if (target == null) {
            return 2;
        }
        fireControlLockedTargetId = target.entityId;
        return 1;
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null || mc.theWorld == null) {
            return;
        }
        if (mc.gameSettings.thirdPersonView != 0) {
            return;
        }
        MCH_EntityAircraft ac = null;
        if (player.ridingEntity instanceof MCH_EntityAircraft) {
            ac = (MCH_EntityAircraft) player.ridingEntity;
        } else if (player.ridingEntity instanceof MCH_EntitySeat) {
            ac = ((MCH_EntitySeat) player.ridingEntity).getParent();
        } else if (player.ridingEntity instanceof MCH_EntityUavStation) {
            ac = ((MCH_EntityUavStation) player.ridingEntity).getControlAircract();
        }
        if (ac == null) {
            return;
        }
        MCH_WeaponSet currentWs = ac.getCurrentWeapon(player);
        if (currentWs == null || currentWs.getInfo() == null) {
            return;
        }
        MCH_WeaponInfo info = currentWs.getInfo();
        if (!info.enableBVR || !isSupportedWeaponType(info.type)) {
            return;
        }
        MCH_WeaponBase weapon = currentWs.getCurrentWeapon();
        if (weapon == null) {
            return;
        }
        MCH_EntityInfo target = selectTarget();
        if (target == null) {
            return;
        }
        LeadSolution solution = solveLead(ac, target, weapon, info, event.partialTicks);
        if (solution == null) {
            return;
        }
        drawLeadCircle(solution.leadPos);
        drawDashedLine(solution.lockBoxPos, solution.leadPos);
    }

    private static boolean isSupportedWeaponType(String type) {
        if (type == null) {
            return false;
        }
        return type.equalsIgnoreCase("machinegun1")
            || type.equalsIgnoreCase("machinegun2")
            || type.equalsIgnoreCase("railgun");
    }

    private MCH_EntityInfo selectTarget() {
        if (fireControlLockedTargetId <= 0) {
            return null;
        }
        MCH_EntityInfo locked = MCH_EntityInfoClientTracker.getEntityInfo(fireControlLockedTargetId);
        if (locked == null || !isVehicle(locked.entityClassName)) {
            fireControlLockedTargetId = -1;
            return null;
        }
        return locked;
    }

    private static MCH_EntityInfo findFireControlTarget(MCH_EntityAircraft ac, EntityPlayer player) {
        List<MCH_EntityInfo> all = new ArrayList<MCH_EntityInfo>(MCH_EntityInfoClientTracker.getAllTrackedEntities());
        MCH_EntityInfo best = null;
        double bestDistSq = Double.MAX_VALUE;
        double maxRangeSq = FIRE_CONTROL_MAX_RANGE * FIRE_CONTROL_MAX_RANGE;
        for (MCH_EntityInfo entity : all) {
            if (!isVehicle(entity.entityClassName)) {
                continue;
            }
            double dx = entity.posX - ac.posX;
            double dy = entity.posY - ac.posY;
            double dz = entity.posZ - ac.posZ;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq > maxRangeSq) {
                continue;
            }
            double angle = calculateAngle(player, entity.posX, entity.posY, entity.posZ);
            if (angle > FIRE_CONTROL_MAX_ANGLE) {
                continue;
            }
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                best = entity;
            }
        }
        return best;
    }

    private static boolean isVehicle(String className) {
        return className != null && (className.contains("MCH_EntityHeli")
            || className.contains("MCP_EntityPlane")
            || className.contains("MCH_EntityTank")
            || className.contains("MCH_EntityVehicle"));
    }

    private static double calculateAngle(EntityPlayer viewer, double x, double y, double z) {
        double dx = x - viewer.posX;
        double dy = y - viewer.posY;
        double dz = z - viewer.posZ;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 1.0E-6D) {
            return 0.0D;
        }
        dx /= dist;
        dy /= dist;
        dz /= dist;
        double yawRad = Math.toRadians(viewer.rotationYaw);
        double pitchRad = Math.toRadians(viewer.rotationPitch);
        double fx = -Math.sin(yawRad) * Math.cos(pitchRad);
        double fy = -Math.sin(pitchRad);
        double fz = Math.cos(yawRad) * Math.cos(pitchRad);
        double fLen = Math.sqrt(fx * fx + fy * fy + fz * fz);
        if (fLen > 1.0E-6D) {
            fx /= fLen;
            fy /= fLen;
            fz /= fLen;
        }
        double dot = dx * fx + dy * fy + dz * fz;
        dot = Math.max(-1.0D, Math.min(1.0D, dot));
        return Math.toDegrees(Math.acos(dot));
    }

    private LeadSolution solveLead(MCH_EntityAircraft ac, MCH_EntityInfo target, MCH_WeaponBase weapon, MCH_WeaponInfo info, float partialTicks) {
        double acX = ac.prevPosX + (ac.posX - ac.prevPosX) * partialTicks;
        double acY = ac.prevPosY + (ac.posY - ac.prevPosY) * partialTicks;
        double acZ = ac.prevPosZ + (ac.posZ - ac.prevPosZ) * partialTicks;
        Vec3 shotPos = weapon.getShotPos(ac);
        double sx = acX + shotPos.xCoord;
        double sy = acY + shotPos.yCoord;
        double sz = acZ + shotPos.zCoord;

        double targetX = target.lastTickPosX + (target.posX - target.lastTickPosX) * partialTicks;
        double targetY = target.lastTickPosY + (target.posY - target.lastTickPosY) * partialTicks + 1.0D;
        double targetZ = target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * partialTicks;
        double targetVx = target.posX - target.lastTickPosX;
        double targetVy = target.posY - target.lastTickPosY;
        double targetVz = target.posZ - target.lastTickPosZ;

        double speed = weapon.acceleration;
        if (info.speedDependsAircraft) {
            speed += Math.sqrt(ac.motionX * ac.motionX + ac.motionY * ac.motionY + ac.motionZ * ac.motionZ);
        }
        if (speed <= 1.0E-6D) {
            return null;
        }

        double rx = targetX - sx;
        double ry = targetY - sy;
        double rz = targetZ - sz;
        double a = targetVx * targetVx + targetVy * targetVy + targetVz * targetVz - speed * speed;
        double b = 2.0D * (rx * targetVx + ry * targetVy + rz * targetVz);
        double c = rx * rx + ry * ry + rz * rz;
        double t = -1.0D;
        if (Math.abs(a) < 1.0E-6D) {
            if (Math.abs(b) > 1.0E-6D) {
                t = -c / b;
            }
        } else {
            double d = b * b - 4.0D * a * c;
            if (d < 0.0D) {
                return null;
            }
            double sqrtD = Math.sqrt(d);
            double t1 = (-b - sqrtD) / (2.0D * a);
            double t2 = (-b + sqrtD) / (2.0D * a);
            if (t1 > 0.0D && t2 > 0.0D) {
                t = Math.min(t1, t2);
            } else if (t1 > 0.0D) {
                t = t1;
            } else if (t2 > 0.0D) {
                t = t2;
            }
        }
        if (t <= 0.0D || t > 200.0D) {
            return null;
        }
        LeadSolution result = new LeadSolution();
        result.leadPos = Vec3.createVectorHelper(targetX + targetVx * t, targetY + targetVy * t, targetZ + targetVz * t);
        result.lockBoxPos = Vec3.createVectorHelper(targetX, targetY, targetZ);
        return result;
    }

    private void drawLeadCircle(Vec3 lead) {
        Minecraft mc = Minecraft.getMinecraft();
        RenderManager rm = RenderManager.instance;
        double x = lead.xCoord - rm.viewerPosX;
        double y = lead.yCoord - rm.viewerPosY;
        double z = lead.zCoord - rm.viewerPosZ;
        double dist = Math.sqrt(x * x + y * y + z * z);
        if (dist < 0.5D) {
            return;
        }
        ScaledResolution sc = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        double fovRad = Math.toRadians(mc.gameSettings.fovSetting);
        float sPerPixel = (float) ((2.0D * dist * Math.tan(fovRad * 0.5D)) / sc.getScaledHeight_double());

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
        GL11.glColor4f(0.1F, 1.0F, 0.1F, 0.95F);
        mc.getTextureManager().bindTexture(PRE_AIM);
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

    private void drawDashedLine(Vec3 from, Vec3 to) {
        RenderManager rm = RenderManager.instance;
        double fx = from.xCoord - rm.viewerPosX;
        double fy = from.yCoord - rm.viewerPosY;
        double fz = from.zCoord - rm.viewerPosZ;
        double tx = to.xCoord - rm.viewerPosX;
        double ty = to.yCoord - rm.viewerPosY;
        double tz = to.zCoord - rm.viewerPosZ;

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_STIPPLE);
        GL11.glLineWidth(2.0F);
        GL11.glLineStipple(1, (short) 0x00FF);
        GL11.glColor4f(0.1F, 1.0F, 0.1F, 0.9F);
        Tessellator tess = Tessellator.instance;
        tess.startDrawing(GL11.GL_LINES);
        tess.addVertex(fx, fy, fz);
        tess.addVertex(tx, ty, tz);
        tess.draw();
        GL11.glDisable(GL11.GL_LINE_STIPPLE);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }

    private static class LeadSolution {
        Vec3 leadPos;
        Vec3 lockBoxPos;
    }
}
