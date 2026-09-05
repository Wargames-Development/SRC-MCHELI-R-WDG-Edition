package mcheli;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class MCH_HBMUtil {

    public static boolean isHBMLoaded = false;

    private static Class<?> nukeExplosionMK5Class;
    private static Class<?> nukeTorexClass;
    private static Class<?> explosionChaosClass;
    private static Class<?> explosionCreatorClass;
    private static Class<?> explosionSmallCreatorClass;
    private static Class<?> mediumBunkerBusterClass;
    private static Class<?> EntityBulletBaseMK4Class;
    private static Class<?> PacketThreading;
    private static Class<?> explosionVNTClass;
    private static Class<?> integrationsClass;
    private static final Set<String> warnedFailures = new HashSet<String>();


    static {
        nukeExplosionMK5Class = optionalClass("com.hbm.entity.logic.EntityNukeExplosionMK5");
        nukeTorexClass = optionalClass("com.hbm.entity.effect.EntityNukeTorex");
        explosionChaosClass = optionalClass("com.hbm.explosion.ExplosionChaos");
        explosionCreatorClass = optionalClass("com.hbm.particle.helper.ExplosionCreator");
        explosionSmallCreatorClass = optionalClass("com.hbm.particle.helper.ExplosionSmallCreator");
        mediumBunkerBusterClass = optionalClass("com.hbm.entity.missile.EntityMissileTier2$EntityMissileBusterStrong");
        EntityBulletBaseMK4Class = optionalClass("com.hbm.entity.projectile.EntityBulletBaseMK4");
        PacketThreading = optionalClass("com.hbm.handler.threading.PacketThreading");
        explosionVNTClass = optionalClass("com.hbm.explosion.vanillant.ExplosionVNT");
        isHBMLoaded = nukeExplosionMK5Class != null
            || nukeTorexClass != null
            || explosionChaosClass != null
            || explosionCreatorClass != null
            || explosionSmallCreatorClass != null
            || mediumBunkerBusterClass != null
            || EntityBulletBaseMK4Class != null
            || PacketThreading != null
            || explosionVNTClass != null;

        try {
            integrationsClass = Class.forName("mcheli.wgc.Integrations");
        } catch (ClassNotFoundException e) {
            integrationsClass = null;
        }
    }

    private static Class<?> optionalClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static void warnOnce(String key, Exception e) {
        if (warnedFailures.add(key)) {
            String message = e.getMessage();
            System.err.println("[MCHR][HBM] Optional bridge action failed: " + key + " (" + e.getClass().getSimpleName() + (message != null ? ": " + message : "") + ")");
        }
    }

    public static Object EntityNukeExplosionMK5_statFac(World world, int r, double posX, double posY, double posZ, UUID ownerParty) {
        if (nukeExplosionMK5Class == null) {
            return null;
        }
        try {
            Method statFacMethod = nukeExplosionMK5Class.getMethod("statFac", World.class, int.class, double.class, double.class, double.class, UUID.class);
            return statFacMethod.invoke(null, world, r, posX, posY, posZ, ownerParty);
        } catch (Exception e) {
            warnOnce("EntityNukeExplosionMK5.statFac", e);
        }
        return null;
    }

    public static Object EntityNukeExplosionMK5_statFac(World world, int r, double posX, double posY, double posZ) {
        return EntityNukeExplosionMK5_statFac(world, r, posX, posY, posZ, null);
    }

    public static void EntityNukeTorex_statFac(World world, double posX, double posY, double posZ, float nukeYield, int type) {
        if (nukeTorexClass == null) {
            return;
        }
        try {
            Method statFacMethod = nukeTorexClass.getMethod("statFac", World.class, double.class, double.class, double.class, float.class, int.class);
            statFacMethod.invoke(null, world, posX, posY, posZ, nukeYield, type);
        } catch (Exception e) {
            warnOnce("EntityNukeTorex.statFac", e);
        }
    }

    public static void ExplosionChaos_spawnClorine(World world, double posX, double posY, double posZ, float chemYield) {
        if (explosionChaosClass == null) {
            return;
        }
        try {
            Method spawnChlorineMethod = explosionChaosClass.getMethod("spawnChlorine", World.class, double.class, double.class, double.class, float.class, double.class, int.class);
            spawnChlorineMethod.invoke(null, world, posX, posY, posZ, chemYield, 1.25, 0);
        } catch (Exception e) {
            warnOnce("ExplosionChaos.spawnChlorine", e);
        }
    }

    public static Set<ChunkCoordIntPair> getContamProtectedChunksWGC(UUID ownerParty, World world, int x, int z, int radius) {
        if (!isHBMLoaded) {
            return Collections.emptySet();
        }
        try {
            if (integrationsClass != null) {
                Method method = integrationsClass.getMethod("getContamProtectedChunksWGC", UUID.class, World.class, int.class, int.class, int.class);
                Object result = method.invoke(null, ownerParty, world, x, z, radius);
                if (result instanceof Set) {
                    return (Set<ChunkCoordIntPair>) result;
                }
            }
        } catch (Exception e) {
            warnOnce("Integrations.getContamProtectedChunksWGC", e);
        }
        return Collections.emptySet();
    }

    public static void ExplosionCreator_composeEffect(World world, double posX, double posY, double posZ, int explosionBlockSize) {
        if (explosionCreatorClass == null) {
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
            warnOnce("ExplosionCreator.composeEffect", e);
        }
    }

    public static void ExplosionSmallCreator_composeEffect(World world, double posX, double posY, double posZ, int explosionBlockSize) {
        if (explosionSmallCreatorClass == null) {
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
            warnOnce("ExplosionSmallCreator.composeEffect", e);
        }
    }

    public static void Frag_Effect(World world, double posX, double posY, double posZ) {
        if (EntityBulletBaseMK4Class == null) {
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
            warnOnce("Frag_Effect", e);
        }
    }

    public static void WP_Effect(World world, double posX, double posY, double posZ, int dim) {
        if (PacketThreading == null) {
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
            warnOnce("WP_Effect", e);
        }
    }

    public static Object ExplosionVNT(World world, double posX, double posY, double posZ, float explosionPower, UUID ownerParty, Entity exploder) {
        if (explosionVNTClass == null) {
            return null;
        }
        try {
            Class<?>[] explosionVNTParamTypes = {World.class, double.class, double.class, double.class, float.class, UUID.class, Entity.class};
            Constructor<?> explosionVNTConstructor = explosionVNTClass.getConstructor(explosionVNTParamTypes);
            return explosionVNTConstructor.newInstance(world, posX, posY, posZ, explosionPower, ownerParty, exploder);
        } catch (Exception e) {
            try {
                Class<?>[] explosionVNTParamTypes = {World.class, double.class, double.class, double.class, float.class, UUID.class};
                Constructor<?> explosionVNTConstructor = explosionVNTClass.getConstructor(explosionVNTParamTypes);
                return explosionVNTConstructor.newInstance(world, posX, posY, posZ, explosionPower, ownerParty);
            } catch (Exception fallback) {
                try {
                    Class<?>[] explosionVNTParamTypes = {World.class, double.class, double.class, double.class, float.class, Entity.class};
                    Constructor<?> explosionVNTConstructor = explosionVNTClass.getConstructor(explosionVNTParamTypes);
                    return explosionVNTConstructor.newInstance(world, posX, posY, posZ, explosionPower, exploder);
                } catch (Exception legacyFallback) {
                    warnOnce("ExplosionVNT.constructor", legacyFallback);
                }
            }
        }
        return null;
    }

    public static Object ExplosionVNT(World world, double posX, double posY, double posZ, float explosionPower) {
        return ExplosionVNT(world, posX, posY, posZ, explosionPower, null, null);
    }

    public static boolean ExplosionVNT_Explode(Object ExplosionVNT, boolean isDestroyBlock) {
        return ExplosionVNT_Explode(ExplosionVNT, isDestroyBlock, true);
    }

    public static boolean ExplosionVNT_Explode(Object ExplosionVNT, boolean isDestroyBlock, boolean playStandardSfx) {
        if (ExplosionVNT == null) {
            return false;
        }
        boolean destroyBlocks = isDestroyBlock && MCH_Config.Explosion_DestroyBlock.prmBool;
        try {
            try {
                Method makeStandard = ExplosionVNT.getClass().getMethod("makeStandard");
                makeStandard.invoke(ExplosionVNT);
            } catch (NoSuchMethodException e) {
                prepareStandardVNTExplosion(ExplosionVNT, destroyBlocks, playStandardSfx);
            }
            if (!playStandardSfx) {
                clearVNTSFX(ExplosionVNT);
            }
            if (!destroyBlocks) {
                clearVNTBlockProcessors(ExplosionVNT);
            }
            Method explodeMethod = ExplosionVNT.getClass().getMethod("explode");
            explodeMethod.invoke(ExplosionVNT);
            return true;
        } catch (Exception e) {
            warnOnce("ExplosionVNT.explode", e);
        }
        return false;
    }

    private static void prepareStandardVNTExplosion(Object ExplosionVNT, boolean isDestroyBlock, boolean playStandardSfx) throws Exception {
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
        Class<?> IEntityProcessor = Class.forName("com.hbm.explosion.vanillant.interfaces.IEntityProcessor");
        Method setEntityProcessor = ExplosionVNT.getClass().getMethod("setEntityProcessor", IEntityProcessor);
        Object EntityProcessorStandard = Class.forName("com.hbm.explosion.vanillant.standard.EntityProcessorStandard").getConstructor().newInstance();
        setEntityProcessor.invoke(ExplosionVNT, EntityProcessorStandard);
        Class<?> IPlayerProcessor = Class.forName("com.hbm.explosion.vanillant.interfaces.IPlayerProcessor");
        Method setPlayerProcessor = ExplosionVNT.getClass().getMethod("setPlayerProcessor", IPlayerProcessor);
        Object PlayerProcessorStandard = Class.forName("com.hbm.explosion.vanillant.standard.PlayerProcessorStandard").getConstructor().newInstance();
        setPlayerProcessor.invoke(ExplosionVNT, PlayerProcessorStandard);

        if (playStandardSfx) {
            Class<?> IExplosionSFX = Class.forName("com.hbm.explosion.vanillant.interfaces.IExplosionSFX");
            Object ExplosionEffectStandard = Class.forName("com.hbm.explosion.vanillant.standard.ExplosionEffectStandard").getConstructor().newInstance();
            Method setSFX = ExplosionVNT.getClass().getMethod("setSFX", java.lang.reflect.Array.newInstance(IExplosionSFX, 0).getClass());
            Object sfx = java.lang.reflect.Array.newInstance(IExplosionSFX, 1);
            java.lang.reflect.Array.set(sfx, 0, ExplosionEffectStandard);
            setSFX.invoke(ExplosionVNT, new Object[]{sfx});
        }
    }

    private static void clearVNTSFX(Object ExplosionVNT) {
        try {
            Class<?> IExplosionSFX = Class.forName("com.hbm.explosion.vanillant.interfaces.IExplosionSFX");
            Method setSFX = ExplosionVNT.getClass().getMethod("setSFX", java.lang.reflect.Array.newInstance(IExplosionSFX, 0).getClass());
            Object sfx = java.lang.reflect.Array.newInstance(IExplosionSFX, 0);
            setSFX.invoke(ExplosionVNT, new Object[]{sfx});
        } catch (Exception e) {
            warnOnce("ExplosionVNT.clearSFX", e);
        }
    }

    private static void clearVNTBlockProcessors(Object ExplosionVNT) throws Exception {
        Class<?> IBlockAllocator = Class.forName("com.hbm.explosion.vanillant.interfaces.IBlockAllocator");
        Method setBlockAllocator = ExplosionVNT.getClass().getMethod("setBlockAllocator", IBlockAllocator);
        setBlockAllocator.invoke(ExplosionVNT, new Object[]{null});
        Class<?> IBlockProcessor = Class.forName("com.hbm.explosion.vanillant.interfaces.IBlockProcessor");
        Method setBlockProcessor = ExplosionVNT.getClass().getMethod("setBlockProcessor", IBlockProcessor);
        setBlockProcessor.invoke(ExplosionVNT, new Object[]{null});
    }

    public static void ExplosionNT_instance_setOwnerParty(Object explosionNTInstance, UUID ownerParty) {
        if (explosionNTInstance == null) {
            return;
        }
        try {
            if (explosionNTInstance != null) {
                Method setOwnerPartyMethod = explosionNTInstance.getClass().getMethod("setOwnerParty", UUID.class);
                setOwnerPartyMethod.invoke(explosionNTInstance, ownerParty);
            }
        } catch (Exception e) {
            warnOnce("ExplosionNT.setOwnerParty", e);
        }
    }

    public static boolean spawnMediumBunkerBuster(World world, double posX, double posY, double posZ,
                                                   double motionX, double motionY, double motionZ,
                                                   UUID ownerParty) {
        if (mediumBunkerBusterClass == null || world == null || world.isRemote) {
            return false;
        }

        double speed = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        if (speed < 1.0E-6D) {
            motionX = 0.0D;
            motionY = -1.0D;
            motionZ = 0.0D;
            speed = 1.0D;
        }

        double dirX = motionX / speed;
        double dirY = motionY / speed;
        double dirZ = motionZ / speed;
        double spawnX = posX - dirX * 0.05D;
        double spawnY = posY - dirY * 0.05D;
        double spawnZ = posZ - dirZ * 0.05D;
        int targetX = (int)Math.floor(spawnX + dirX * 10000.0D);
        int targetZ = (int)Math.floor(spawnZ + dirZ * 10000.0D);

        Entity entity = null;
        try {
            Constructor<?> constructor = mediumBunkerBusterClass.getConstructor(
                World.class, float.class, float.class, float.class,
                int.class, int.class, UUID.class
            );
            Object bunkerBuster = constructor.newInstance(
                world, (float)spawnX, (float)spawnY, (float)spawnZ,
                targetX, targetZ, ownerParty
            );
            if (!(bunkerBuster instanceof Entity)) {
                return false;
            }

            entity = (Entity)bunkerBuster;
            entity.motionX = dirX;
            entity.motionY = dirY;
            entity.motionZ = dirZ;
            entity.rotationYaw = (float)(Math.atan2(dirX, dirZ) * 180.0D / Math.PI);
            entity.rotationPitch = (float)(Math.atan2(dirY, Math.sqrt(dirX * dirX + dirZ * dirZ)) * 180.0D / Math.PI) - 90.0F;
            entity.prevRotationYaw = entity.rotationYaw;
            entity.prevRotationPitch = entity.rotationPitch;

            mediumBunkerBusterClass.getField("velocity").setDouble(bunkerBuster, speed);
            mediumBunkerBusterClass.getField("decelY").setDouble(bunkerBuster, 0.0D);
            mediumBunkerBusterClass.getField("accelXZ").setDouble(bunkerBuster, 0.0D);

            boolean spawned = world.spawnEntityInWorld(entity);
            if (!spawned) {
                entity.setDead();
            }
            return spawned;
        } catch (Exception e) {
            if (entity != null) {
                entity.setDead();
            }
            warnOnce("spawnMediumBunkerBuster", e);
        }
        return false;
    }
}
