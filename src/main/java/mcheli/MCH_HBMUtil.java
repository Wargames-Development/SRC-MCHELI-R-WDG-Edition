package mcheli;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class MCH_HBMUtil {
    private static Class<?> nukeExplosionMK5Class;
    private static Class<?> nukeTorexClass;
    private static Class<?> explosionChaosClass;
    private static Class<?> explosionCreatorClass;
    private static Class<?> explosionSmallCreatorClass;
    private static Class<?> explosionNTClass;


    static {
        try {
            nukeExplosionMK5Class = Class.forName("com.hbm.entity.logic.EntityNukeExplosionMK5");
            nukeTorexClass = Class.forName("com.hbm.entity.effect.EntityNukeTorex");
            explosionChaosClass = Class.forName("com.hbm.explosion.ExplosionChaos");
            explosionCreatorClass = Class.forName("com.hbm.particle.helper.ExplosionCreator");
            explosionSmallCreatorClass = Class.forName("com.hbm.particle.helper.ExplosionSmallCreator");
            explosionNTClass = Class.forName("com.hbm.explosion.ExplosionNT");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Object EntityNukeExplosionMK5_statFac(World world, int r, double posX, double posY, double posZ) {
        try {
            if (nukeExplosionMK5Class != null) {
                Method statFacMethod = nukeExplosionMK5Class.getMethod("statFac", World.class, int.class, double.class, double.class, double.class);
                return statFacMethod.invoke(null, world, r, posX, posY, posZ);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void EntityNukeTorex_statFac(World world, double posX, double posY, double posZ, float nukeYield) {
        try {
            if (nukeTorexClass != null) {
                Method statFacMethod = nukeTorexClass.getMethod("statFacStandard", World.class, double.class, double.class, double.class, float.class);
                statFacMethod.invoke(null, world, posX, posY, posZ, nukeYield);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void ExplosionChaos_spawnClorine(World world, double posX, double posY, double posZ, float chemYield) {
        try {
            if (explosionChaosClass != null) {
                Method spawnChlorineMethod = explosionChaosClass.getMethod("spawnChlorine", World.class, double.class, double.class, double.class, float.class, double.class, int.class);
                spawnChlorineMethod.invoke(null, world, posX, posY, posZ, chemYield, 1.25, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void ExplosionCreator_composeEffect(World world, double posX, double posY, double posZ, int explosionBlockSize) {
        try {
            if (explosionCreatorClass != null) {
                Method composeEffectMethod;
                if(explosionBlockSize<5) {
                    composeEffectMethod = explosionCreatorClass.getMethod("composeEffectSmall", World.class, double.class, double.class, double.class);
                } else if (explosionBlockSize<10) {
                    composeEffectMethod = explosionCreatorClass.getMethod("composeEffectStandard", World.class, double.class, double.class, double.class);
                }
                else {
                    composeEffectMethod = explosionCreatorClass.getMethod("composeEffectLarge", World.class, double.class, double.class, double.class);
                }
                composeEffectMethod.invoke(null, world, posX, posY, posZ);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void ExplosionSmallCreator_composeEffect(World world, double posX, double posY, double posZ, int explosionBlockSize) {
        try {
            if (explosionSmallCreatorClass != null) {
                Method composeEffectMethod;
                composeEffectMethod = explosionSmallCreatorClass.getMethod("composeEffect", World.class, double.class, double.class, double.class, int.class, float.class, float.class);
                if(explosionBlockSize<3) {
                    composeEffectMethod.invoke(null, world, posX, posY, posZ,5, 1F, 0.5F);
                } else if (explosionBlockSize<10) {
                    composeEffectMethod.invoke(null, world, posX, posY, posZ,10, 1F, 0.5F);                }
                else {
                    composeEffectMethod.invoke(null, world, posX, posY, posZ,15, 3.5F, 1.25F);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Object ExplosionNT_instance_init(World world, Entity entity, double posX, double posY, double posZ, float explosionPower) {
        try {
            if (explosionNTClass != null) {
                Class<?>[] explosionNTParamTypes = {World.class, Entity.class, double.class, double.class, double.class, float.class};
                Constructor<?> explosionNTConstructor = explosionNTClass.getConstructor(explosionNTParamTypes);
                return explosionNTConstructor.newInstance(world, entity, posX, posY, posZ, explosionPower);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void ExplosionNT_instance_overrideResolutionAndExplode(Object explosionNTInstance, int resolution) {
        try {
            if (explosionNTInstance != null) {
                Method overrideResolutionMethod = explosionNTInstance.getClass().getMethod("overrideResolution", int.class);
                overrideResolutionMethod.invoke(explosionNTInstance, resolution);
                Method explodeMethod = explosionNTInstance.getClass().getMethod("explode");
                explodeMethod.invoke(explosionNTInstance);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void ExplosionNT_instance_addAttrib(Object explosionNTInstance, String attrib) {
        try {
            if (explosionNTInstance != null) {
                Class<?> exAttribClass = Class.forName("com.hbm.explosion.ExplosionNT$ExAttrib");
                Object Attrib = Enum.valueOf((Class<Enum>) exAttribClass, attrib);
                Method addAttribMethod = explosionNTClass.getMethod("addAttrib", exAttribClass);
                addAttribMethod.invoke(explosionNTInstance,Attrib);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
