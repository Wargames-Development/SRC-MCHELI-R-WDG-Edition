package mcheli.flare;

import mcheli.MCH_Explosion;
import mcheli.MCH_ExplosionParam;
import mcheli.MCH_FMURUtil;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.network.packets.PacketIronCurtainUse;
import mcheli.particles.MCH_ParticleParam;
import mcheli.particles.MCH_ParticlesUtil;
import mcheli.weapon.*;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

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
    private final Random rand = new Random();

    public MCH_APS(World w, MCH_EntityAircraft ac) {
        this.worldObj = w;
        this.aircraft = ac;
    }

    public boolean onUse(Entity user) {
        boolean result = false;
        System.out.println("MCH_APS.onUse");
        this.user = user;
        if (worldObj.isRemote) {
            if (tick == 0) {
                tick = waitTime;
                useTick = useTime;
                result = true;
                if (range == 100) {
                    W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ, "iron_curtain", 3.0F, 1.0F);
                    aircraft.ironCurtainRunningTick = useTick;
                } else {
                    W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ, "aps_activate", 3.0F, 1.0F);
                }
            }
        } else {
            result = true;
            tick = waitTime;
            useTick = useTime;
            aircraft.getEntityData().setBoolean("APSUsing", true);
            if (range == 100) {
                W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ, "iron_curtain", 10.0F, 1.0F);
                aircraft.ironCurtainRunningTick = useTick;
                MCH_MOD.getPacketHandler().sendToAll(new PacketIronCurtainUse(aircraft.getEntityId(), useTick));
            } else {
                W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ, "aps_activate", 3.0F, 1.0F);
            }
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
                if (useTick == 0) {
                    W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ, "aps_deactivate", 3.0F, 1.0F);
                    onEnd();
                }
            }
            if (this.useTick > 0) {
                this.onUsing();
            }
            if (!this.isUsing() && this.aircraft.getEntityData().getBoolean("APSUsing")) {
                this.aircraft.getEntityData().setBoolean("APSUsing", false);
            }
        }
    }

    private void spawnFlameLine(double ax, double ay, double az, double bx, double by, double bz) {
        if (!worldObj.isRemote) return;

        double dx = bx - ax;
        double dy = by - ay;
        double dz = bz - az;
        int numParticles = 8 + rand.nextInt(6);

        for (int i = 0; i < numParticles; ++i) {
            double t = rand.nextDouble();
            double px = ax + dx * t + (rand.nextDouble() - 0.5D) * 0.6D;
            double py = ay + dy * t + (rand.nextDouble() - 0.5D) * 0.6D;
            double pz = az + dz * t + (rand.nextDouble() - 0.5D) * 0.6D;

            MCH_ParticleParam prm = new MCH_ParticleParam(worldObj, "smoke", px, py, pz);
            prm.setColor(0.9F, 1.0F, 0.6F + rand.nextFloat() * 0.3F, rand.nextFloat() * 0.15F);
            prm.size = 0.6F + rand.nextFloat() * 0.8F;
            prm.age = 8 + rand.nextInt(12);
            prm.gravity = -0.008F;
            MCH_ParticlesUtil.spawnParticle(prm);
        }
    }

    private void onUsing() {
        if (range == 100) {
            return;
        }
        List list = worldObj.getEntitiesWithinAABBExcludingEntity(aircraft, aircraft.boundingBox.expand(range, range, range));
        for (Object obj : list) {
            Entity entity = (Entity) obj;

            boolean isBullet = entity.getClass().getName().contains("EntityBullet");
            boolean isGrenade = entity.getClass().getName().contains("EntityGrenade");
            boolean isMissile = entity instanceof MCH_EntityAAMissile
                || entity instanceof MCH_EntityRocket
                || entity instanceof MCH_EntityATMissile
                || entity instanceof MCH_EntityASMissile
                || entity instanceof MCH_EntityTvMissile;

            if (!isBullet && !isGrenade && !isMissile) continue;

            if (isBullet) {
                if (MCH_FMURUtil.bulletDestructedByAPS(entity, (EntityLivingBase) user)) {
                    spawnFlameLine(aircraft.posX, aircraft.posY, aircraft.posZ, entity.posX, entity.posY, entity.posZ);
                    if (!worldObj.isRemote) {
                        W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ, "aps_shoot", 5.0F, 1.0F);
                    }
                }
                continue;
            }

            if (isGrenade) {
                if (MCH_FMURUtil.grenadeDestructedByAPS(entity, (EntityLivingBase) user)) {
                    spawnFlameLine(aircraft.posX, aircraft.posY, aircraft.posZ, entity.posX, entity.posY, entity.posZ);
                    if (!worldObj.isRemote) {
                        W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ, "aps_shoot", 5.0F, 1.0F);
                        MCH_ExplosionParam param = MCH_ExplosionParam.builder()
                            .exploder(user)
                            .player(user instanceof EntityPlayer ? (EntityPlayer) user : null)
                            .x(entity.posX).y(entity.posY).z(entity.posZ)
                            .size(2.0F)
                            .sizeBlock(0.0F)
                            .isPlaySound(true)
                            .isSmoking(true)
                            .isFlaming(false)
                            .isDestroyBlock(true)
                            .countSetFireEntity(0)
                            .isInWater(false)
                            .build();
                        MCH_Explosion.newExplosion(worldObj, param);
                    }
                }
                continue;
            }

            if (isMissile) {
                MCH_EntityBaseBullet bullet = (MCH_EntityBaseBullet) entity;
                if (bullet.shootingEntity instanceof EntityLivingBase && user instanceof EntityLivingBase && !((EntityLivingBase) user).isOnSameTeam((EntityLivingBase) bullet.shootingEntity)) {
                    spawnFlameLine(aircraft.posX, aircraft.posY, aircraft.posZ, entity.posX, entity.posY, entity.posZ);
                    if (!worldObj.isRemote) {
                        bullet.setDead();
                        W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ, "aps_shoot", 5.0F, 1.0F);
                        if (bullet.shootingEntity instanceof EntityPlayerMP) {
                            MCH_FMURUtil.sendAPSMarker((EntityPlayerMP) bullet.shootingEntity);
                        }
                        MCH_ExplosionParam param = MCH_ExplosionParam.builder()
                            .exploder(user)
                            .player(user instanceof EntityPlayer ? (EntityPlayer) user : null)
                            .x(entity.posX).y(entity.posY).z(entity.posZ)
                            .size(3.0F)
                            .sizeBlock(0.0F)
                            .isPlaySound(true)
                            .isSmoking(true)
                            .isFlaming(false)
                            .isDestroyBlock(true)
                            .countSetFireEntity(0)
                            .isInWater(false)
                            .build();
                        MCH_Explosion.newExplosion(worldObj, param);
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
