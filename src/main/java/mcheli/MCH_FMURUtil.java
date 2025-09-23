package mcheli;

import com.flansmod.common.mob.EntitySoldier;
import com.flansmod.common.mob.api.SoldierAPI;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAILeapAtTarget;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.scoreboard.Team;

import java.lang.reflect.Method;

public class MCH_FMURUtil {

    private static boolean isFMURLoaded;
    private static Class<?> FMUR_APIClass;
    private static Class<?> FMUR_SoldierAPIClass;

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
//        return isSoldier_Fast(entity);
        if(isFMURLoaded) {
            return entity instanceof EntitySoldier;
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
        if(isFMURLoaded) {
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
        } else {
            return null;
        }
    }

    public static boolean isSoldier_Fast(Entity entity) {
        if(isFMURLoaded) {
            return SoldierAPI.isSoldier(entity);
        } else {
            return false;
        }
    }

    public static Team getSoldierTeam_Fast(Entity entity) {
        if(isFMURLoaded) {
            return SoldierAPI.getSoldierTeam(entity);
        } else {
            return null;
        }
    }


}
