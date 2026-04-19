package mcheli;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class MCH_HBMUtil {

    public static boolean isHBMLoaded = false;

    private static Class<?> nukeExplosionMK5Class;
    private static Class<?> nukeTorexClass;
    private static Class<?> explosionChaosClass;
    private static Class<?> explosionCreatorClass;
    private static Class<?> explosionSmallCreatorClass;
    private static Class<?> EntityBulletBaseMK4Class;
    private static Class<?> PacketThreading;
    private static Class<?> explosionVNTClass;


    static {
        try {
            nukeExplosionMK5Class = Class.forName("com.hbm.entity.logic.EntityNukeExplosionMK5");
            nukeTorexClass = Class.forName("com.hbm.entity.effect.EntityNukeTorex");
            explosionChaosClass = Class.forName("com.hbm.explosion.ExplosionChaos");
            explosionCreatorClass = Class.forName("com.hbm.particle.helper.ExplosionCreator");
            explosionSmallCreatorClass = Class.forName("com.hbm.particle.helper.ExplosionSmallCreator");
            EntityBulletBaseMK4Class = Class.forName("com.hbm.entity.projectile.EntityBulletBaseMK4");
            PacketThreading = Class.forName("com.hbm.handler.threading.PacketThreading");
            explosionVNTClass = Class.forName("com.hbm.explosion.vanillant.ExplosionVNT");
            isHBMLoaded = true;
        } catch (ClassNotFoundException e) {
            isHBMLoaded = false;
            e.printStackTrace();
        }
    }

    public static Object EntityNukeExplosionMK5_statFac(World world, int r, double posX, double posY, double posZ) {
        if (!isHBMLoaded) {
            return null;
        }
        try {
            Method statFacMethod = nukeExplosionMK5Class.getMethod("statFac", World.class, int.class, double.class, double.class, double.class);
            return statFacMethod.invoke(null, world, r, posX, posY, posZ);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void EntityNukeTorex_statFac(World world, double posX, double posY, double posZ, float nukeYield, int type) {
        if (!isHBMLoaded) {
            return;
        }
        try {
            Method statFacMethod = nukeTorexClass.getMethod("statFac", World.class, double.class, double.class, double.class, float.class, int.class);
            statFacMethod.invoke(null, world, posX, posY, posZ, nukeYield, type);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void ExplosionChaos_spawnClorine(World world, double posX, double posY, double posZ, float chemYield) {
        if (!isHBMLoaded) {
            return;
        }
        try {
            Method spawnChlorineMethod = explosionChaosClass.getMethod("spawnChlorine", World.class, double.class, double.class, double.class, float.class, double.class, int.class);
            spawnChlorineMethod.invoke(null, world, posX, posY, posZ, chemYield, 1.25, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void ExplosionCreator_composeEffect(World world, double posX, double posY, double posZ, int explosionBlockSize) {
        if (!isHBMLoaded) {
            return;
        }
        try {
            Method composeEffectMethod;
            if (explosionBlockSize < 5) {
                composeEffectMethod = explosionCreatorClass.getMethod("composeEffectSmall", World.class, double.class, double.class, double.class);
            } else if (explosionBlockSize < 10) {
                composeEffectMethod = explosionCreatorClass.getMethod("composeEffectStandard", World.class, double.class, double.class, double.class);
            } else {
                composeEffectMethod = explosionCreatorClass.getMethod("composeEffectLarge", World.class, double.class, double.class, double.class);
            }
            composeEffectMethod.invoke(null, world, posX, posY, posZ);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void ExplosionSmallCreator_composeEffect(World world, double posX, double posY, double posZ, int explosionBlockSize) {
        if (!isHBMLoaded) {
            return;
        }
        try {
            Method composeEffectMethod;
            composeEffectMethod = explosionSmallCreatorClass.getMethod("composeEffect", World.class, double.class, double.class, double.class, int.class, float.class, float.class);
            if (explosionBlockSize < 3) {
                composeEffectMethod.invoke(null, world, posX, posY, posZ, 5, 1F, 0.5F);
            } else if (explosionBlockSize < 10) {
                composeEffectMethod.invoke(null, world, posX, posY, posZ, 10, 1F, 0.5F);
            } else {
                composeEffectMethod.invoke(null, world, posX, posY, posZ, 15, 3.5F, 1.25F);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void Frag_Effect(World world, double posX, double posY, double posZ) {
        if (!isHBMLoaded) {
            return;
        }
        try {
            Class<?> BulletConfigClass = Class.forName("com.hbm.items.weapon.sedna.BulletConfig");
            Class<?>[] EntityBulletBaseMK4ParamTypes = {World.class, BulletConfigClass, float.class, float.class, float.class, float.class};
            Object fragBulletConfig = Class.forName("com.hbm.items.weapon.grenade.ItemGrenadeFilling").getDeclaredField("fragmentation").get(null);

            for (int i = 0; i < 25; i++) {
                Object bullet = EntityBulletBaseMK4Class.getConstructor(EntityBulletBaseMK4ParamTypes)
                    .newInstance(world, fragBulletConfig, 10F, 0F, world.rand.nextFloat() * 2F * (float)Math.PI, (world.rand.nextFloat() - 0.5F) * 2F * (float)Math.PI);
                Method setPosition = bullet.getClass().getMethod("setPosition", double.class, double.class, double.class);
                setPosition.invoke(bullet, posX, posY + 0.05, posZ);
                world.spawnEntityInWorld((Entity)bullet);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void WP_Effect(World world, double posX, double posY, double posZ, int dim) {
        if (!isHBMLoaded) {
            return;
        }
        try {
            for (int i = 0; i < 3; i++) {
                NBTTagCompound haze = new NBTTagCompound();
                haze.setString("type", "haze");
                Class<?> auxPacketClass = Class.forName("com.hbm.packet.toclient.AuxParticlePacketNT");
                Constructor<?> auxConstructor = auxPacketClass.getConstructor(NBTTagCompound.class, double.class, double.class, double.class);
                Object auxPacket = auxConstructor.newInstance(haze, posX + world.rand.nextGaussian() * 4, posY, posZ + world.rand.nextGaussian() * 4);
                Class<?> targetPointClass = Class.forName("cpw.mods.fml.common.network.NetworkRegistry$TargetPoint");
                Constructor<?> tpConstructor = targetPointClass.getConstructor(int.class, double.class, double.class, double.class, double.class);
                Object targetPoint = tpConstructor.newInstance(dim, posX, posY, posZ, 150.0);
                Class<?> packetThreadingClass = Class.forName("com.hbm.handler.threading.PacketThreading");
                Method createMethod = packetThreadingClass.getMethod("createAllAroundThreadedPacket", Class.forName("cpw.mods.fml.common.network.simpleimpl.IMessage"), targetPointClass);
                createMethod.invoke(null, auxPacket, targetPoint);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Object ExplosionVNT(World world, double posX, double posY, double posZ, float explosionPower) {
        if (!isHBMLoaded) {
            return null;
        }
        try {
            Class<?>[] explosionVNTParamTypes = {World.class, double.class, double.class, double.class, float.class, Entity.class};
            Constructor<?> explosionVNTConstructor = explosionVNTClass.getConstructor(explosionVNTParamTypes);
            return explosionVNTConstructor.newInstance(world, posX, posY, posZ, explosionPower, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void ExplosionVNT_Explode(Object ExplosionVNT, boolean isDestroyBlock) {
        if (!isHBMLoaded) {
            return;
        }
        try {
            if (isDestroyBlock) {
                Class<?> IBlockAllocator = Class.forName("com.hbm.explosion.vanillant.interfaces.IBlockAllocator");
                Method setBlockAllocator = ExplosionVNT.getClass().getMethod("setBlockAllocator", IBlockAllocator);
                Object BlockAllocatorStandard = Class.forName("com.hbm.explosion.vanillant.standard.BlockAllocatorStandard").getConstructor().newInstance();
                setBlockAllocator.invoke(ExplosionVNT, BlockAllocatorStandard);
                Class<?> IBlockProcessor = Class.forName("com.hbm.explosion.vanillant.interfaces.IBlockProcessor");
                Method setBlockProcessor = ExplosionVNT.getClass().getMethod("setBlockProcessor", IBlockProcessor);
                Object BlockProcessorStandard = Class.forName("com.hbm.explosion.vanillant.standard.BlockProcessorStandard").getConstructor().newInstance();
                setBlockProcessor.invoke(ExplosionVNT, BlockProcessorStandard);
            }
            Method explodeMethod = ExplosionVNT.getClass().getMethod("explode");
            explodeMethod.invoke(ExplosionVNT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
