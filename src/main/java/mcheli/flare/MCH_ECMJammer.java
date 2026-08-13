package mcheli.flare;

import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.network.PacketBase;
import mcheli.network.packets.PacketECMJammerUse;
import mcheli.particles.MCH_ParticleParam;
import mcheli.particles.MCH_ParticlesUtil;
import mcheli.weapon.MCH_EntityBaseBullet;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

public class MCH_ECMJammer {

    private static final int RADAR_JAM_REFRESH_INTERVAL = 10;
    private static final int RADAR_JAM_GRACE_TICKS = 15;
    private static final int MISSILE_JAM_HOLD_TICKS = 3;

    //冷却时长 0代表冷却结束
    public int tick;
    //生效时长 0代表使用结束
    public int useTick;
    //维修系统生效时间
    public int useTime;
    //维修系统等待时间
    public int waitTime;

    public World worldObj;

    public MCH_EntityAircraft aircraft;

    public MCH_ECMJammer(World w, MCH_EntityAircraft ac) {
        this.worldObj = w;
        this.aircraft = ac;
    }

    public boolean onUse(Entity e) {
        boolean result = false;

        if (worldObj.isRemote) {
            if (tick == 0) {
                tick = waitTime;
                useTick = useTime;
                result = true;
                W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ, "iron_curtain", 3.0F, 1.0F);
            }
        } else {
            result = true;
            tick = waitTime;
            useTick = useTime;

            aircraft.getEntityData().setBoolean("ECMJammerUsing", true);
            this.jamRadarMissilesInRange();
            int type = aircraft.getAcInfo() != null ? aircraft.getAcInfo().ecmJammerType : 0;
            W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ, "iron_curtain", 10.0F, 1.0F);
            MCH_MOD.getPacketHandler().sendToAll(
                new PacketECMJammerUse(aircraft.getEntityId(), useTick, type, RADAR_JAM_GRACE_TICKS));
        }

        return result;
    }

    public void onUpdate() {
        if (this.aircraft != null && !this.aircraft.isDead) {
            if (this.tick > 0) {
                --this.tick;
            }
            if (this.useTick > 0) {
                --this.useTick;
            }
            if (this.useTick > 0) {
                this.onUsing();
            }
            if (!this.isUsing() && this.aircraft.getEntityData().getBoolean("ECMJammerUsing")) {
                this.aircraft.getEntityData().setBoolean("ECMJammerUsing", false);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Object> getLoadedEntityListSafe(net.minecraft.world.World w) {
        // 1) Try normal Forge/vanilla method (some environments have it)
        try {
            return (List<Object>) w.getLoadedEntityList();
        } catch (Throwable ignored) {
        }

        // 2) Fallback: access the field directly (most 1.7.10 Worlds have this)
        try {
            Field f = w.getClass().getField("loadedEntityList");
            return (List<Object>) f.get(w);
        } catch (Throwable ignored) {
        }

        // 3) Last resort: declared field access
        try {
            Field f = w.getClass().getDeclaredField("loadedEntityList");
            f.setAccessible(true);
            return (List<Object>) f.get(w);
        } catch (Throwable ignored) {
        }

        return Collections.emptyList();
    }


    private void onUsing() {
        if (this.aircraft == null) {
            return;
        }
        if (!worldObj.isRemote) {
            if (this.aircraft.ticksExisted % 2 == 0) {
                this.jamRadarMissilesInRange();
            }
            if (this.aircraft.getAcInfo() != null
                && this.aircraft.getAcInfo().ecmJammerType == 1
                && this.useTick % RADAR_JAM_REFRESH_INTERVAL == 0) {
                MCH_MOD.getPacketHandler().sendToAll(new PacketECMJammerUse(
                    this.aircraft.getEntityId(), this.useTick, 1, RADAR_JAM_GRACE_TICKS));
            }
            return;
        }
        if (this.aircraft.ticksExisted % 2 != 0) {
            return;
        }
        float radius = (float)Math.max(this.aircraft.width * 0.7D, 1.1D);
        double baseY = this.aircraft.posY + this.aircraft.height * 0.55D;
        for (int i = 0; i < 8; ++i) {
            float angle = (float)((this.aircraft.ticksExisted * 9 + i * 45) * Math.PI / 180.0D);
            double px = this.aircraft.posX + (double)(Math.cos(angle) * radius);
            double py = baseY + (double)((i % 2 == 0 ? 0.18F : -0.12F) + (float)Math.sin(angle * 2.0F) * 0.06F);
            double pz = this.aircraft.posZ + (double)(Math.sin(angle) * radius);
            double mx = Math.cos(angle) * 0.010D;
            double mz = Math.sin(angle) * 0.010D;
            MCH_ParticleParam prm = new MCH_ParticleParam(worldObj, "smoke", px, py, pz, mx, 0.005D, mz, 2.4F);
            prm.setColor(0.52F, 0.40F, 0.78F, 1.00F);
            prm.age = 16 + this.worldObj.rand.nextInt(7);
            prm.diffusible = true;
            prm.toWhite = true;
            prm.gravity = -0.01F;
            prm.motionYUpAge = 1.5F;
            MCH_ParticlesUtil.spawnParticle(prm);
        }
    }

    private void jamRadarMissilesInRange() {
        if (this.aircraft == null || this.aircraft.getAcInfo() == null) {
            return;
        }
        double range = this.aircraft.getAcInfo().ecmJammerRange;
        if (range <= 0.0D) {
            return;
        }
        AxisAlignedBB area = AxisAlignedBB.getBoundingBox(
            this.aircraft.boundingBox.minX - range, -30000000.0D, this.aircraft.boundingBox.minZ - range,
            this.aircraft.boundingBox.maxX + range, 30000000.0D, this.aircraft.boundingBox.maxZ + range);
        List missiles = this.worldObj.getEntitiesWithinAABB(MCH_EntityBaseBullet.class, area);
        if (missiles == null) {
            return;
        }
        double rangeSq = range * range;
        for (Object o : missiles) {
            MCH_EntityBaseBullet missile = (MCH_EntityBaseBullet)o;
            if (missile == null || missile.isDead || missile.getInfo() == null || this.isFriendlyMissile(missile)) {
                continue;
            }
            double dx = missile.posX - this.aircraft.posX;
            double dz = missile.posZ - this.aircraft.posZ;
            if (dx * dx + dz * dz <= rangeSq) {
                missile.applyRadarJamming(MISSILE_JAM_HOLD_TICKS);
            }
        }
    }

    private boolean isFriendlyMissile(MCH_EntityBaseBullet missile) {
        if (W_Entity.isEqual(missile.shootingAircraft, this.aircraft)
            || W_Entity.isEqual(missile.shootingEntity, this.aircraft)
            || this.aircraft.isMountedEntity(missile.shootingEntity)) {
            return true;
        }
        for (int jammerSeat = 0; jammerSeat <= this.aircraft.getSeatNum(); ++jammerSeat) {
            Entity jammerCrew = this.aircraft.getEntityBySeatId(jammerSeat);
            if (!(jammerCrew instanceof EntityLivingBase)) {
                continue;
            }
            if (W_Entity.isEqual(jammerCrew, missile.shootingEntity)
                || this.isSameTeam((EntityLivingBase)jammerCrew, missile.shootingEntity)) {
                return true;
            }
            if (missile.shootingAircraft instanceof MCH_EntityAircraft) {
                MCH_EntityAircraft shootingAircraft = (MCH_EntityAircraft)missile.shootingAircraft;
                for (int shooterSeat = 0; shooterSeat <= shootingAircraft.getSeatNum(); ++shooterSeat) {
                    Entity shooter = shootingAircraft.getEntityBySeatId(shooterSeat);
                    if (W_Entity.isEqual(jammerCrew, shooter)
                        || this.isSameTeam((EntityLivingBase)jammerCrew, shooter)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isSameTeam(EntityLivingBase jammerCrew, Entity other) {
        return other instanceof EntityLivingBase && jammerCrew.getTeam() != null
            && ((EntityLivingBase)other).getTeam() != null
            && jammerCrew.isOnSameTeam((EntityLivingBase)other);
    }


    public boolean isInPreparation() {
        return this.tick != 0;
    }

    public boolean isUsing() {
        return this.useTick > 0;
    }
}
