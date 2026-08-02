package mcheli.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.weapon.MCH_WeaponAAMissile;
import mcheli.weapon.MCH_WeaponBase;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponSet;
import mcheli.wrapper.W_SoundUpdater;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;

/** Minimal HUD and audio feedback for a vehicle-mounted infrared AAM seeker. */
public class MCH_RenderIRSeeker {
    private static final double DISPLAY_DISTANCE = 64.0D;
    private static final int CIRCLE_SEGMENTS = 64;
    private static final float SEARCH_VOLUME = 3.0F;
    private static final float LOCK_MIN_VOLUME = 3.5F;
    private static final float LOCK_MAX_VOLUME = 6.0F;
    private static final int TONE_NONE = 0;
    private static final int TONE_SEARCH = 1;
    private static final int TONE_LOCK = 2;

    private W_SoundUpdater toneUpdater;
    private int activeTone = TONE_NONE;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null || mc.theWorld == null) {
            resetTone();
            return;
        }

        MCH_EntityAircraft aircraft = getAircraft(player);
        MCH_WeaponSet weaponSet = aircraft != null ? aircraft.getCurrentWeapon(player) : null;
        MCH_WeaponBase weapon = weaponSet != null ? weaponSet.getCurrentWeapon() : null;
        if (aircraft == null || aircraft.isDestroyed() || !(weapon instanceof MCH_WeaponAAMissile)
            || !((MCH_WeaponAAMissile)weapon).isPureHeatSeeker()) {
            resetTone();
            return;
        }

        boolean seekerActive = mc.currentScreen == null;
        updateTone(mc, player, weapon, seekerActive);
        if (mc.gameSettings.hideGUI) {
            return;
        }

        MCH_WeaponInfo info = weapon.getInfo();
        float cueYaw;
        float cuePitch;
        if (info.enableHMS) {
            cueYaw = interpolateRotation(player.prevRotationYaw, player.rotationYaw, event.partialTicks);
            cuePitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * event.partialTicks;
        } else {
            cueYaw = interpolateRotation(aircraft.prevRotationYaw, aircraft.rotationYaw, event.partialTicks)
                + interpolateRotation(weaponSet.prevRotationYaw, weaponSet.rotationYaw, event.partialTicks)
                + weapon.fixRotationYaw;
            cuePitch = aircraft.prevRotationPitch + (aircraft.rotationPitch - aircraft.prevRotationPitch) * event.partialTicks
                + weaponSet.prevRotationPitch + (weaponSet.rotationPitch - weaponSet.prevRotationPitch) * event.partialTicks
                + weapon.fixRotationPitch;
        }
        drawSeekerCircle(cueYaw, cuePitch, info.maxLockOnAngle);
    }

    private void updateTone(Minecraft mc, EntityPlayer player, MCH_WeaponBase weapon, boolean active) {
        if (!active) {
            resetTone();
            return;
        }
        int max = Math.max(1, weapon.getLockCountMax());
        double progress = Math.min(1.0D, (double)weapon.getLockCount() / (double)max);
        boolean locked = weapon.optionParameter1 > 0 || progress >= 1.0D;
        if (locked || progress > 0.0D) {
            setActiveTone(mc, TONE_LOCK, player);
            float volume = (float)(LOCK_MIN_VOLUME + (LOCK_MAX_VOLUME - LOCK_MIN_VOLUME) * progress);
            float pitch = (float)(1.0D + 0.2D * progress);
            this.toneUpdater.setEntitySoundVolume(player, volume);
            this.toneUpdater.setEntitySoundPitch(player, pitch);
            this.toneUpdater.updateSoundLocation(player);
        } else {
            setActiveTone(mc, TONE_SEARCH, player);
            this.toneUpdater.setEntitySoundVolume(player, SEARCH_VOLUME);
            this.toneUpdater.setEntitySoundPitch(player, 1.0F);
            this.toneUpdater.updateSoundLocation(player);
        }
    }

    private void setActiveTone(Minecraft mc, int tone, EntityPlayer player) {
        if (this.activeTone == tone && this.toneUpdater != null) {
            return;
        }
        if (this.toneUpdater != null) {
            this.toneUpdater.stopEntitySound(player);
        }
        String sound = tone == TONE_LOCK ? "irlock" : "alert";
        float volume = tone == TONE_LOCK ? LOCK_MIN_VOLUME : SEARCH_VOLUME;
        this.toneUpdater = new W_SoundUpdater(mc, player);
        this.toneUpdater.initEntitySound(sound);
        this.toneUpdater.playEntitySound(sound, player, volume, 1.0F, true);
        this.activeTone = tone;
    }

    private void resetTone() {
        if (this.toneUpdater != null) {
            this.toneUpdater.stopEntitySound(null);
        }
        this.toneUpdater = null;
        this.activeTone = TONE_NONE;
    }

    private static MCH_EntityAircraft getAircraft(EntityPlayer player) {
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

    private static float interpolateRotation(float previous, float current, float partialTicks) {
        float delta = current - previous;
        while (delta < -180.0F) {
            delta += 360.0F;
        }
        while (delta >= 180.0F) {
            delta -= 360.0F;
        }
        return previous + partialTicks * delta;
    }

    private static void drawSeekerCircle(float yaw, float pitch, int lockAngle) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double dirX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double dirY = -Math.sin(pitchRad);
        double dirZ = Math.cos(yawRad) * Math.cos(pitchRad);
        double centerX = dirX * DISPLAY_DISTANCE;
        double centerY = dirY * DISPLAY_DISTANCE;
        double centerZ = dirZ * DISPLAY_DISTANCE;
        double angle = Math.max(1.0D, Math.min(45.0D, lockAngle));
        double radius = DISPLAY_DISTANCE * Math.tan(Math.toRadians(angle));

        RenderManager renderManager = RenderManager.instance;
        GL11.glPushMatrix();
        GL11.glTranslated(centerX, centerY, centerZ);
        GL11.glRotatef(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(1.5F);
        GL11.glColor4f(0.2F, 1.0F, 0.2F, 0.9F);

        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_LINE_LOOP);
        for (int i = 0; i < CIRCLE_SEGMENTS; ++i) {
            double a = Math.PI * 2.0D * (double)i / (double)CIRCLE_SEGMENTS;
            tessellator.addVertex(Math.cos(a) * radius, Math.sin(a) * radius, 0.0D);
        }
        tessellator.draw();

        GL11.glLineWidth(1.0F);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }
}
