package mcheli;

import com.flansmod.common.guns.EntityDamageSourceFlans;
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

public class MCH_FMURUtil {

    private static boolean isFMURLoaded;
    private static Class<?> FMUR_APIClass;
    private static Class<?> FMUR_SoldierAPIClass;

    public static boolean isFMURLoaded() {
        return isFMURLoaded;
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
