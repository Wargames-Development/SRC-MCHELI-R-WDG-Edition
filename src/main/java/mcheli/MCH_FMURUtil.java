package mcheli;

import com.flansmod.common.guns.EntityDamageSourceFlans;
import com.flansmod.common.guns.EntityBullet;
import com.flansmod.common.mob.EntitySoldier;
import com.flansmod.common.mob.EnumFaction;
import com.flansmod.common.mob.SoldierType;
import com.flansmod.common.mob.api.SoldierAPI;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import mcheli.weapon.MCH_WeaponBase;
import mcheli.weapon.MCH_WeaponParam;

public class MCH_FMURUtil {
    private static final Set<String> missingMountedBullets = new HashSet<String>();

    public static void registerMountedBulletHandler() {
        if (isFMURLoaded) net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new MountedBulletHandler());
    }

    public static boolean spawnMountedBullet(MCH_WeaponBase weapon, MCH_WeaponParam prm, float yaw, float pitch) {
        if (weapon.worldObj.isRemote || !(prm.entity instanceof MCH_EntityAircraft)
            || !(prm.user instanceof EntityLivingBase) || prm.user.isDead) return false;
        if (isFMURLoaded && MountedBulletHandler.spawn(weapon, prm, yaw, pitch)) return true;
        if (missingMountedBullets.add(weapon.getInfo().fmurBullet)) {
            MCH_Lib.Log("[FMUR] Cannot fire mounted weapon %s: ammunition %s / tracer %s unavailable or spawn rejected",
                weapon.name, weapon.getInfo().fmurBullet, weapon.getInfo().fmurTracerBullet);
        }
        return false;
    }

    // Loaded only when FMUR is present; common callers expose no FMUR event types.
    public static final class MountedBulletHandler {
        private static final String SOURCE = "MCHRMountedAircraft";

        private static boolean spawn(MCH_WeaponBase weapon, MCH_WeaponParam prm, float yaw, float pitch) {
            int interval = weapon.getInfo().fmurTracerInterval;
            boolean tracer = interval > 0 && weapon.fmurShotsSinceTracer >= interval - 1;
            String ammo = tracer ? weapon.getInfo().fmurTracerBullet : weapon.getInfo().fmurBullet;
            com.flansmod.common.guns.type.BulletType type = com.flansmod.common.guns.type.BulletType.getBullet(ammo);
            if (type == null) return false;
            EntityBullet bullet = new EntityBullet(weapon.worldObj,
                net.minecraft.util.Vec3.createVectorHelper(prm.posX, prm.posY, prm.posZ), yaw, pitch,
                (EntityLivingBase) prm.user, weapon.getInfo().fmurBulletDamage, type, weapon.acceleration, type, 0.0F, 0.0F);
            // MCHR has already applied mount rotation and spread. FMUR owns the trajectory from here.
            if (weapon.getInfo().speedDependsAircraft) {
                double speed = weapon.acceleration + Math.sqrt(prm.entity.motionX * prm.entity.motionX
                    + prm.entity.motionY * prm.entity.motionY + prm.entity.motionZ * prm.entity.motionZ);
                double factor = speed / weapon.acceleration;
                bullet.motionX *= factor;
                bullet.motionY *= factor;
                bullet.motionZ *= factor;
            }
            bullet.getEntityData().setString(SOURCE, prm.entity.getUniqueID().toString());
            if (!weapon.worldObj.spawnEntityInWorld(bullet)) return false;
            weapon.fmurShotsSinceTracer = interval <= 0 || tracer ? 0 : weapon.fmurShotsSinceTracer + 1;
            return true;
        }

        @cpw.mods.fml.common.eventhandler.SubscribeEvent
        public void onBulletHit(com.flansmod.common.eventhandlers.BulletHitEvent event) {
            EntityBullet bullet = event.getBullet();
            String source = bullet.getEntityData().getString(SOURCE);
            // FMUR also predicts collisions on clients; its spawn packet omits Forge entity data.
            // Infer cosmetic source immunity there, while the server uses only the recorded UUID.
            if (source.isEmpty() && bullet.worldObj.isRemote) {
                MCH_EntityAircraft mount = MCH_EntityAircraft.getAircraft_RiddenOrControl(bullet.owner);
                if (mount != null) source = mount.getUniqueID().toString();
            }
            if (source.isEmpty()) return;
            Entity hit = event.getHitEntity();
            if (event.getHit() instanceof com.flansmod.common.guns.raytracing.SoldierBulletHit) {
                hit = ((com.flansmod.common.guns.raytracing.SoldierBulletHit) event.getHit()).hitbox.soldier;
            }
            if (hit == null) return;
            // Match native MCHR rounds: auxiliary hitboxes/seats cannot consume a shot.
            if (hit instanceof MCH_EntitySeat || hit instanceof mcheli.aircraft.MCH_EntityHitBox) {
                event.setCanceled(true);
                return;
            }
            MCH_EntityAircraft aircraft = hit instanceof MCH_EntityAircraft ? (MCH_EntityAircraft) hit
                : MCH_EntityAircraft.getAircraft_RiddenOrControl(hit);
            if (aircraft != null && source.equals(aircraft.getUniqueID().toString())) event.setCanceled(true);
        }
    }

    private static boolean isFMURLoaded;
    private static Class<?> FMUR_APIClass;
    private static Class<?> FMUR_SoldierAPIClass;

    public static boolean isFMURLoaded() {
        return isFMURLoaded;
    }

    public static boolean isFlansAPSThreat(Entity entity) {
        if (!isFMURLoaded || !(entity instanceof EntityBullet)) return false;
        EntityBullet bullet = (EntityBullet)entity;
        return bullet.type != null && bullet.type.canBeDestructByAPS
            && bullet.type.explosionRadius > 0.0F;
    }

    public static Entity getFlansBulletOwner(Entity entity) {
        return isFMURLoaded && entity instanceof EntityBullet ? ((EntityBullet)entity).owner : null;
    }

    static {
        try {
            FMUR_APIClass = Class.forName("com.flansmod.api.FMUR_API");
            FMUR_SoldierAPIClass = Class.forName("com.flansmod.common.mob.api.SoldierAPI");
            isFMURLoaded = true;
        } catch (ClassNotFoundException e) {
            isFMURLoaded = false;
            e.printStackTrace();
        }
    }

    public static boolean bulletDestructedByAPS(Entity entity, EntityLivingBase user) {
        try {
            if (FMUR_APIClass != null) {
                Method method = FMUR_APIClass.getMethod("bulletDestructedByAPS", Entity.class, EntityLivingBase.class);
                return (boolean) method.invoke(null, entity, user);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean grenadeDestructedByAPS(Entity entity, EntityLivingBase user) {
        try {
            if (FMUR_APIClass != null) {
                Method method = FMUR_APIClass.getMethod("grenadeDestructedByAPS", Entity.class, EntityLivingBase.class);
                return (boolean) method.invoke(null, entity, user);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void sendAPSMarker(EntityPlayerMP playerMP) {
        try {
            if (FMUR_APIClass != null) {
                Method method = FMUR_APIClass.getMethod("sendAPSMarker", EntityPlayerMP.class);
                method.invoke(null, playerMP);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isSoldier(Entity entity) {
//        try {
//            if (FMUR_SoldierAPIClass != null) {
//                Method method = FMUR_SoldierAPIClass.getMethod("isSoldier", Object.class);
//                return (boolean) method.invoke(null, entity);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return false;
//
        if (isFMURLoaded) {
            return isSoldier_Fast(entity);
        } else {
            return false;
        }
    }

    public static Team getSoldierTeam(Entity entity) {
//        try {
//            if (FMUR_SoldierAPIClass != null) {
//                Method method = FMUR_SoldierAPIClass.getMethod("getSoldierTeam", Object.class);
//                return (Team) method.invoke(null, entity);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//        return getSoldierTeam_Fast(entity);
        if (isFMURLoaded) {
            return getSoldierTeam_Fast(entity);
        } else {
            return null;
        }
    }

    public static boolean isSoldier_Fast(Entity entity) {
        return entity instanceof EntitySoldier;
    }

    public static Team getSoldierTeam_Fast(Entity entity) {
        if (entity instanceof EntitySoldier) {
            EntitySoldier s = (EntitySoldier) entity;
            if (s.team == null) {
                return s.owner == null ? null : s.owner.getTeam();
            } else {
                return s.team;
            }
        } else {
            return null;
        }
    }

    public static boolean isFMURExplosion(DamageSource damageSource) {
        if (isFMURLoaded) {
            return isFMURExplosion_Fast(damageSource);
        } else {
            return false;
        }
    }

    public static boolean isFMURExplosion_Fast(DamageSource damageSource) {
        if (damageSource instanceof EntityDamageSourceFlans) {
            EntityDamageSourceFlans source = (EntityDamageSourceFlans) damageSource;
            return source.flansExplosion;
        }
        return false;
    }

    public static void spawnAiSoldierOnAircraft(World world, Entity aircraft, Team team) {
        if (!isFMURLoaded) return;
        if (world == null || world.isRemote) return;
        if (aircraft == null || aircraft.isDead) return;
        if (!(aircraft instanceof MCH_EntityAircraft)) return;

        MCH_EntityAircraft ac = (MCH_EntityAircraft) aircraft;
        MCH_FmurDebug.log(aircraft, "[spawnAiSoldierOnAircraft] called for aircraft id=%d", ac.getEntityId());

        MCH_EntitySeat targetSeat = null;
        MCH_EntitySeat[] seats = ac.getSeats();
        MCH_FmurDebug.log(aircraft, "[spawnAiSoldierOnAircraft] seats=%s length=%d", seats != null ? "non-null" : "null", seats != null ? seats.length : -1);
        if (seats != null) {
            for (int i = 0; i < seats.length; i++) {
                MCH_EntitySeat seat = seats[i];
                if (seat == null) {
                    MCH_FmurDebug.log(aircraft, "[spawnAiSoldierOnAircraft] seat[%d] is null", i);
                    continue;
                }
                MCH_FmurDebug.log(aircraft, "[spawnAiSoldierOnAircraft] seat[%d]: seatID=%d isDead=%s ridden=%s", i, seat.seatID,
                    String.valueOf(seat.isDead), seat.riddenByEntity != null ? seat.riddenByEntity.getClass().getSimpleName() : "null");
                if (!seat.isDead && seat.riddenByEntity == null) {
                    targetSeat = seat;
                    break;
                }
            }
        }
        if (targetSeat == null) {
            MCH_FmurDebug.log(aircraft, "[spawnAiSoldierOnAircraft] No empty seat found, aborting");
            return;
        }

        MCH_FmurDebug.log(aircraft, "[spawnAiSoldierOnAircraft] Spawning AI soldier on seat[%d](seatEntityId=%d)", targetSeat.seatID, targetSeat.getEntityId());
        EntitySoldier soldier = new EntitySoldier(
            world,
            targetSeat.posX, targetSeat.posY, targetSeat.posZ,
            null,
            team,
            EnumFaction.ASS,
            SoldierType.randomType(),
            null,
            null,
            null,
            "APC_Gunner_AI"
        );
        soldier.setInvisible(true);
        world.spawnEntityInWorld(soldier);
        MCH_FmurDebug.log(aircraft, "[spawnAiSoldierOnAircraft] EntitySoldier spawned id=%d", soldier.getEntityId());
        soldier.mountEntity(targetSeat);
        MCH_FmurDebug.log(aircraft, "[spawnAiSoldierOnAircraft] Soldier mounted on seat, seat.riddenByEntity=%s",
            targetSeat.riddenByEntity != null ? targetSeat.riddenByEntity.getClass().getSimpleName() : "null");
        SoldierAPI.soldierMap.put(soldier.getEntityId(), soldier);
        MCH_FmurDebug.log(aircraft, "[spawnAiSoldierOnAircraft] Done, soldierMap.size=%d", SoldierAPI.soldierMap.size());
    }

}
