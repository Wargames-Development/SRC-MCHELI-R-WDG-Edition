package mcheli.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.weapon.MCH_WeaponAAMissile;
import mcheli.weapon.MCH_WeaponBase;
import mcheli.weapon.MCH_WeaponSet;
import mcheli.wrapper.W_SoundUpdater;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderWorldLastEvent;

/** Audio feedback for a vehicle-mounted infrared AAM seeker. */
public class MCH_RenderIRSeeker {
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
    }

    private void updateTone(Minecraft mc, EntityPlayer player, MCH_WeaponBase weapon, boolean active) {
        if (!active) {
            resetTone();
            return;
        }
        int max = Math.max(1, weapon.getLockCountMax());
        double progress = Math.min(1.0D, (double)weapon.getLockCount() / (double)max);
        // Progress alone is not a fireable lock: distant aircraft may exist only
        // in the snapshot stream, with no target ID until acquisition completes.
        boolean locked = weapon.optionParameter1 > 0;
        if (locked) {
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
}
