package mcheli.flare;

import mcheli.MCH_Explosion;
import mcheli.MCH_ExplosionParam;
import mcheli.MCH_FMURUtil;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.network.packets.PacketIronCurtainUse;
import mcheli.weapon.*;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import java.util.List;

public class MCH_APS {

    // Cooldown duration; 0 indicates cooldown finished
    public int tick;
    // Active duration; 0 indicates effect has ended
    public int useTick;
    // APS active duration
    public int useTime;
    // APS cooldown duration
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
        boolean result = false;
        System.out.println("MCH_APS.onUse");
        this.user = user;
        if (worldObj.isRemote) {
            if (tick == 0) {
                tick = waitTime;
                useTick = useTime;
                result = true;
                if(range == 100) {
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
            if(range == 100) {
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
                if(useTick == 0) {
                    W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ, "aps_deactivate", 3.0F, 1.0F);
                    onEnd();
                }
            }
            if(this.useTick > 0) {
                this.onUsing();
            }
            if (!this.isUsing() && this.aircraft.getEntityData().getBoolean("APSUsing")) {
                this.aircraft.getEntityData().setBoolean("APSUsing", false);
            }
        }
    }

    private void onUsing() {
        if(worldObj.isRemote) {
        } else {
            if(range == 100) {
                return;
            }
            List list = worldObj.getEntitiesWithinAABBExcludingEntity(aircraft, aircraft.boundingBox.expand(range, range, range));
            for (Object obj : list) {
                Entity entity = (Entity) obj;

                if(entity.getClass().getName().contains("EntityBullet")) {
                    if(MCH_FMURUtil.bulletDestructedByAPS(entity, (EntityLivingBase) user)) {
                        W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ, "aps_shoot", 5.0F, 1.0F);
                    }
                }

                if(entity.getClass().getName().contains("EntityGrenade")) {
                    if(MCH_FMURUtil.grenadeDestructedByAPS(entity, (EntityLivingBase) user)) {
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

                if(entity instanceof MCH_EntityAAMissile
                        || entity instanceof MCH_EntityRocket
                        || entity instanceof MCH_EntityATMissile
                        || entity instanceof MCH_EntityASMissile
                        || entity instanceof MCH_EntityTvMissile
                ) {
                    MCH_EntityBaseBullet bullet = (MCH_EntityBaseBullet) entity;
                    if(bullet.shootingEntity instanceof EntityPlayer && !((EntityPlayer) user).isOnSameTeam((EntityLivingBase) bullet.shootingEntity)) {
                        bullet.setDead();
                        W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ, "aps_shoot", 5.0F, 1.0F);
                        MCH_FMURUtil.sendAPSMarker((EntityPlayerMP) bullet.shootingEntity);
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
        if(range == 100) {
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
