package mcheli;

import com.flansmod.common.guns.EntityDamageSourceFlans;
import com.flansmod.common.mob.EntitySoldier;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.DamageSource;

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


}
