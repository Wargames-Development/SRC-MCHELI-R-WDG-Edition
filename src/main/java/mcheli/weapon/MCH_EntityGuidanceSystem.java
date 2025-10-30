package mcheli.weapon;

import net.minecraft.entity.Entity;

public abstract class MCH_EntityGuidanceSystem implements MCH_IGuidanceSystem {

    public int lockCount;
    public int lockSoundCount;
    public int continueLockCount;
    public int lockCountMax;
    public int prevLockCount;
    public boolean canLockInWater;
    public boolean canLockOnGround;
    public boolean canLockInAir;
    public boolean ridableOnly;
    public double lockRange;
    public int lockAngle;
    public MCH_IEntityLockChecker checker;

    /**
     * Whether this is an infrared-guided missile — can be affected by flares.
     */
    public boolean isHeatSeekerMissile = true;

    /**
     * Whether this is a radar-guided missile — can be affected by chaff.
     */
    public boolean isRadarMissile = false;

    /**
     * Whether this is a semi-active radar missile that requires continuous guidance.
     */
    public boolean passiveRadar = false;

    /**
     * Countdown (in ticks) before a semi-active radar missile loses lock after guidance stops.
     */
    public int passiveRadarLockOutCount = 20;

    /**
     * PD (Pulse Doppler) radar maximum lock angle — exceeding this will cause lock loss.
     * Can also be used for rear-aspect IR missiles.
     */
    public float pdHDNMaxDegree = 1000f;

    /**
     * PD radar lock loss delay — missile will lose lock after this many ticks
     * if the maximum angle threshold is exceeded.
     */
    public int pdHDNMaxDegreeLockOutCount = 10;

    /**
     * Duration (in ticks) of countermeasure resistance.
     * A value of -1 disables countermeasure resistance.
     */
    public int antiFlareCount = -1;

    /**
     * Multipath radar clutter detection height — aircraft below this altitude
     * will cause radar-guided missiles to lose lock.
     */
    public int lockMinHeight = 12;

    /**
     * Whether the missile can lock onto other missile entities.
     */
    public boolean canLockMissile = false;


    public boolean canLockEntity(Entity entity) {
        return false;
    }

    public boolean isLockingEntity(Entity entity) {
        return false;
    }

    protected abstract void setLockCountMax(int i);

    protected abstract boolean lock(Entity user);

    protected abstract int getLockCount();

    protected abstract int getLockCountMax();

    protected abstract Entity getLastLockEntity();
}
