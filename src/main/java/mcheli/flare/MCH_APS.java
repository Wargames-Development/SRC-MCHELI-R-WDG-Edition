package mcheli.flare;

import mcheli.MCH_FMURUtil;
import mcheli.MCH_MOD;
import mcheli.MCH_PacketEffectExplosion;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.network.packets.PacketIronCurtainUse;
import mcheli.weapon.*;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import java.util.List;

public class MCH_APS {

    //冷却时长 0代表冷却结束
    public int tick;
    //生效时长 0代表使用结束
    public int useTick;
    //APS生效时间
    public int useTime;
    //APS等待时间
    public int waitTime;

    public World worldObj;

    public MCH_EntityAircraft aircraft;

    public int range;

    public Entity user;

    public MCH_APS(World w, MCH_EntityAircraft ac) {
        this.worldObj = w;
        this.aircraft = ac;
    }

    public boolean onUse(Entity user) {
        if (!(user instanceof EntityLivingBase) || this.tick != 0) {
            return false;
        }

        this.user = user;
        this.tick = this.waitTime;
        this.useTick = this.useTime;
        if (worldObj.isRemote) {
            if (range == 100) {
                W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ, "iron_curtain", 3.0F, 1.0F);
                aircraft.ironCurtainRunningTick = useTick;
            } else {
                W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ, "aps_activate", 3.0F, 1.0F);
            }
        } else {
            aircraft.getEntityData().setBoolean("APSUsing", true);
            if (range == 100) {
                W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ, "iron_curtain", 10.0F, 1.0F);
                aircraft.ironCurtainRunningTick = useTick;
                MCH_MOD.getPacketHandler().sendToAll(new PacketIronCurtainUse(aircraft.getEntityId(), useTick));
            } else {
                W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ, "aps_activate", 3.0F, 1.0F);
            }
        }
        return true;
    }

    public void onUpdate() {
        if (this.aircraft != null && !this.aircraft.isDead) {
            if (this.tick > 0) {
                --this.tick;
            }
            if (this.useTick > 0) {
                --this.useTick;
                if (useTick == 0) {
                    W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ, "aps_deactivate", 3.0F, 1.0F);
                    onEnd();
                }
            }
            if (this.useTick > 0 && !this.worldObj.isRemote) {
                this.onUsing();
            }
            if (!this.isUsing() && this.aircraft.getEntityData().getBoolean("APSUsing")) {
                this.aircraft.getEntityData().setBoolean("APSUsing", false);
            }
        }
    }

    private void sendInterceptEffect(Entity projectile, float size, boolean smoking) {
        W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ, "aps_shoot", 5.0F, 1.0F);
        // Reuse the existing visual-effect packet without creating a damaging server explosion.
        MCH_PacketEffectExplosion.ExplosionParam effect = MCH_PacketEffectExplosion.create();
        effect.exploderID = user != null ? user.getEntityId() : -1;
        effect.posX = projectile.posX;
        effect.posY = projectile.posY;
        effect.posZ = projectile.posZ;
        effect.size = size;
        effect.inWater = false;
        effect.isSmoking = smoking;
        MCH_PacketEffectExplosion.send(effect);
    }

    private boolean isHostileMissile(MCH_EntityBaseBullet bullet) {
        if (!(this.user instanceof EntityLivingBase) || !(bullet.shootingEntity instanceof EntityLivingBase)) {
            return false;
        }
        if (bullet.shootingAircraft == this.aircraft || bullet.shootingEntity == this.aircraft || bullet.shootingEntity == this.user) {
            return false;
        }

        EntityLivingBase operator = (EntityLivingBase)this.user;
        EntityLivingBase shooter = (EntityLivingBase)bullet.shootingEntity;
        return operator.getTeam() == null || shooter.getTeam() == null || !operator.isOnSameTeam(shooter);
    }

    private void onUsing() {
        // Projectile removal and FMUR integration are authoritative server operations.
        if (worldObj.isRemote || range == 100 || !(user instanceof EntityLivingBase)) {
            return;
        }
        List list = worldObj.getEntitiesWithinAABBExcludingEntity(aircraft, aircraft.boundingBox.expand(range, range, range));
        for (Object obj : list) {
            Entity entity = (Entity) obj;

            boolean isBullet = entity.getClass().getName().contains("EntityBullet");
            boolean isGrenade = entity.getClass().getName().contains("EntityGrenade");
            // APS recognizes guided threat classes directly. WeaponInfo.canBeIntercepted
            // is reserved for weapon-on-weapon interceptor targeting.
            boolean isMissile = entity instanceof MCH_EntityAAMissile
                || entity instanceof MCH_EntityRocket
                || entity instanceof MCH_EntityATMissile
                || entity instanceof MCH_EntityASMissile
                || entity instanceof MCH_EntityTvMissile;

            if (!isBullet && !isGrenade && !isMissile) continue;

            if (isBullet) {
                if (MCH_FMURUtil.bulletDestructedByAPS(entity, (EntityLivingBase) user)) {
                    sendInterceptEffect(entity, 1.0F, false);
                }
                continue;
            }

            if (isGrenade) {
                if (MCH_FMURUtil.grenadeDestructedByAPS(entity, (EntityLivingBase) user)) {
                    sendInterceptEffect(entity, 2.0F, true);
                }
                continue;
            }

            if (isMissile) {
                MCH_EntityBaseBullet bullet = (MCH_EntityBaseBullet) entity;
                if (isHostileMissile(bullet)) {
                    bullet.setDead();
                    sendInterceptEffect(entity, 3.0F, true);
                    if (bullet.shootingEntity instanceof EntityPlayerMP) {
                        MCH_FMURUtil.sendAPSMarker((EntityPlayerMP) bullet.shootingEntity);
                    }
                }
            }
        }
    }

    private void onEnd() {
        if (range == 100) {
            aircraft.ironCurtainRunningTick = 0;
            aircraft.ironCurtainWaveTimer = 0;
            aircraft.ironCurtainCurrentFactor = 0.5f;
            aircraft.ironCurtainLastFactor = 0.5f;
        }
    }

    public boolean isInPreparation() {
        return this.tick != 0;
    }

    public boolean isUsing() {
        return this.useTick > 0;
    }
}
