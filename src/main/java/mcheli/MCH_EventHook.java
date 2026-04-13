package mcheli;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.aircraft.MCH_ItemAircraft;
import mcheli.chain.MCH_ItemChain;
import mcheli.command.MCH_Command;
import mcheli.mob.MCH_EntityGunner;
import mcheli.weapon.MCH_EntityBaseBullet;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_EntityPlayer;
import mcheli.wrapper.W_EventHook;
import mcheli.wrapper.W_Lib;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.EntityEvent.CanUpdate;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;

import java.util.List;
import java.util.UUID;

public class MCH_EventHook extends W_EventHook {
    private static final double MONSTER_GUNNER_TARGET_RANGE_H = 24.0D;
    private static final double MONSTER_GUNNER_TARGET_RANGE_V = 8.0D;

    public void commandEvent(CommandEvent event) {
        MCH_Command.onCommandEvent(event);
    }


    public void entitySpawn(EntityJoinWorldEvent event) {
        if (W_Lib.isEntityLivingBase(event.entity) && !W_EntityPlayer.isPlayer(event.entity)) {
            event.entity.renderDistanceWeight *= MCH_Config.MobRenderDistanceWeight.prmDouble;
        } else if (event.entity instanceof MCH_EntityAircraft) {
            MCH_EntityAircraft b = (MCH_EntityAircraft) event.entity;
            if (!b.worldObj.isRemote && !b.isCreatedSeats()) {
                b.createSeats(UUID.randomUUID().toString());
            }
        } else if (W_EntityPlayer.isPlayer(event.entity)) {
            Entity e = event.entity;
            boolean b1 = Float.isNaN(e.rotationPitch);
            b1 |= Float.isNaN(e.prevRotationPitch);
            b1 |= Float.isInfinite(e.rotationPitch);
            b1 |= Float.isInfinite(e.prevRotationPitch);
            if (b1) {
                MCH_Lib.Log(event.entity, "### EntityJoinWorldEvent Error:Player invalid rotation pitch(" + e.rotationPitch + ")", new Object[0]);
                e.rotationPitch = 0.0F;
                e.prevRotationPitch = 0.0F;
            }

            b1 = Float.isInfinite(e.rotationYaw);
            b1 |= Float.isInfinite(e.prevRotationYaw);
            b1 |= Float.isNaN(e.rotationYaw);
            b1 |= Float.isNaN(e.prevRotationYaw);
            if (b1) {
                MCH_Lib.Log(event.entity, "### EntityJoinWorldEvent Error:Player invalid rotation yaw(" + e.rotationYaw + ")");
                e.rotationYaw = 0.0F;
                e.prevRotationYaw = 0.0F;
            }

            if (!e.worldObj.isRemote && event.entity instanceof EntityPlayerMP) {
                MCH_Lib.DbgLog(false, "EntityJoinWorldEvent:" + event.entity);
                MCH_PacketNotifyServerSettings.send((EntityPlayerMP) event.entity);
            }
        }

    }


    public void livingAttackEvent(LivingAttackEvent event) {
        MCH_EntityAircraft ac = this.getRiddenAircraft(event.entity);
        if (ac == null)
            return;
        if (ac.getAcInfo() == null)
            return;
        if (ac.isDestroyed())
            return;
        if ((ac.getAcInfo()).damageFactor > 0.0F)
            return;
        Entity attackEntity = event.source.getEntity();
        if (attackEntity == null) {
            event.setCanceled(true);
        } else if (W_Entity.isEqual(attackEntity, event.entity)) {
            event.setCanceled(true);
        } else if (ac.isMountedEntity(attackEntity)) {
            event.setCanceled(true);
        } else {
            MCH_EntityAircraft atkac = getRiddenAircraft(attackEntity);
            if (W_Entity.isEqual(atkac, ac))
                event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerAttack(AttackEntityEvent evt) {
        if (getRiddenAircraft(evt.entityPlayer) != null) {
            evt.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingSetAttackTarget(LivingSetAttackTargetEvent event) {
        if (event == null || event.entityLiving == null || event.entityLiving.worldObj == null || event.entityLiving.worldObj.isRemote) {
            return;
        }
        if (!(event.entityLiving instanceof IMob) || !(event.entityLiving instanceof EntityLiving)) {
            return;
        }

        EntityLiving attacker = (EntityLiving) event.entityLiving;
        EntityLivingBase target = event.target;
        if (target != null && !(target instanceof EntityPlayer)) {
            return;
        }

        MCH_EntityGunner gunner = this.findNearestFriendlyGunner(attacker);
        if (gunner != null && attacker.getAttackTarget() != gunner) {
            attacker.setAttackTarget(gunner);
        }
    }


    public void livingHurtEvent(LivingHurtEvent event) {
        MCH_EntityAircraft ac = getRiddenAircraft(event.entity);
        if (ac == null)
            return;
        if (ac.getAcInfo() == null)
            return;
        if (ac.isDestroyed())
            return;
        Entity attackEntity = event.source.getEntity();
        if (attackEntity == null) {
            //ac.attackEntityFrom(event.source, event.ammount * 2.0F);
            event.ammount *= (ac.getAcInfo()).damageFactor;
        } else if (W_Entity.isEqual(attackEntity, event.entity)) {
            //ac.attackEntityFrom(event.source, event.ammount * 2.0F);
            event.ammount *= (ac.getAcInfo()).damageFactor;
        } else if (ac.isMountedEntity(attackEntity)) {
            event.ammount = 0.0F;
            event.setCanceled(true);
        } else {
            MCH_EntityAircraft atkac = getRiddenAircraft(attackEntity);
            if (W_Entity.isEqual(atkac, ac)) {
                event.ammount = 0.0F;
                event.setCanceled(true);
            } else {
                //ac.attackEntityFrom(event.source, event.ammount * 2.0F);
                event.ammount *= (ac.getAcInfo()).damageFactor;
            }
        }
    }

    public MCH_EntityAircraft getRiddenAircraft(Entity entity) {
        MCH_EntityAircraft ac = null;
        Entity ridden = entity.ridingEntity;
        if (ridden instanceof MCH_EntityAircraft) {
            ac = (MCH_EntityAircraft) ridden;
        } else if (ridden instanceof MCH_EntitySeat) {
            ac = ((MCH_EntitySeat) ridden).getParent();
        }
        if (ac == null) {
            List list = entity.worldObj.getEntitiesWithinAABB(MCH_EntityAircraft.class, entity.boundingBox.expand(50.0D, 50.0D, 50.0D));
            if (list != null) {
                for (Object o : list) {
                    MCH_EntityAircraft tmp = (MCH_EntityAircraft) o;
                    if (tmp.isMountedEntity(entity)) {
                        return tmp;
                    }
                }
            }
        }
        return ac;
    }

    private MCH_EntityGunner findNearestFriendlyGunner(EntityLiving attacker) {
        AxisAlignedBB box = attacker.boundingBox.expand(MONSTER_GUNNER_TARGET_RANGE_H, MONSTER_GUNNER_TARGET_RANGE_V, MONSTER_GUNNER_TARGET_RANGE_H);
        List list = attacker.worldObj.getEntitiesWithinAABB(MCH_EntityGunner.class, box);
        MCH_EntityGunner best = null;
        double bestDist = Double.MAX_VALUE;
        for (Object obj : list) {
            if (!(obj instanceof MCH_EntityGunner)) {
                continue;
            }
            MCH_EntityGunner gunner = (MCH_EntityGunner) obj;
            if (gunner.isDead || gunner.getHealth() <= 0.0F || gunner.isCreative) {
                continue;
            }
            if (gunner.getTargetType() != MCH_EntityGunner.TARGET_MONSTER) {
                continue;
            }
            double dist = attacker.getDistanceSqToEntity(gunner);
            if (dist < bestDist) {
                bestDist = dist;
                best = gunner;
            }
        }
        return best;
    }


    public void entityInteractEvent(EntityInteractEvent event) {
        ItemStack item = event.entityPlayer.getHeldItem();
        if (item != null) {
            if (item.getItem() instanceof MCH_ItemChain) {
                MCH_ItemChain.interactEntity(item, event.target, event.entityPlayer, event.entityPlayer.worldObj);
                event.setCanceled(true);
            } else if (item.getItem() instanceof MCH_ItemAircraft) {
                ((MCH_ItemAircraft) item.getItem()).rideEntity(item, event.target, event.entityPlayer);
            }

        }
    }


    public void entityCanUpdate(CanUpdate event) {
        if (event.entity instanceof MCH_EntityBaseBullet) {
            MCH_EntityBaseBullet bullet = (MCH_EntityBaseBullet) event.entity;
            bullet.setDead();
        }

    }
}
