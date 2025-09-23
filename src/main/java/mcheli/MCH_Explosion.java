package mcheli;

import mcheli.flare.MCH_EntityFlare;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.particles.MCH_ParticleParam;
import mcheli.particles.MCH_ParticlesUtil;
import mcheli.plane.MCP_EntityPlane;
import mcheli.tank.MCH_EntityTank;
import mcheli.vehicle.MCH_EntityVehicle;
import mcheli.weapon.MCH_EntityBaseBullet;
import mcheli.wrapper.*;
import net.minecraft.block.Block;
import net.minecraft.enchantment.EnchantmentProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityExpBottle;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

import java.util.*;

public class MCH_Explosion extends Explosion {

    private static Random explosionRNG = new Random();
    public final int field_77289_h = 16;
    public World world;
    public Map field_77288_k = new HashMap();
    public boolean isDestroyBlock;
    public int countSetFireEntity;
    public boolean isPlaySound;
    public boolean isInWater;
    public EntityPlayer explodedPlayer;
    public float explosionSizeBlock;
    public MCH_DamageFactor damageFactor = null;
    MCH_Explosion.ExplosionResult result;
    private float damageVsLiving = 1f;
    private float damageVsPlayer = 1f;
    private float damageVsPlane = 1f;
    private float damageVsVehicle = 1f;
    private float damageVsTank = 1f;
    private float damageVsHeli = 1f;

    public MCH_Explosion(World par1World, Entity exploder, Entity player, double x, double y, double z, float size) {
        super(par1World, exploder, x, y, z, size);
        this.world = par1World;
        this.isDestroyBlock = false;
        this.explosionSizeBlock = size;
        this.countSetFireEntity = 0;
        this.isPlaySound = true;
        this.isInWater = false;
        this.result = null;
        this.explodedPlayer = player instanceof EntityPlayer ? (EntityPlayer) player : null;
    }

    public static MCH_Explosion.ExplosionResult newExplosion(World w, Entity entityExploded, Entity player, double x, double y, double z, float size, float sizeBlock, boolean playSound, boolean isSmoking, boolean isFlaming, boolean isDestroyBlock, int countSetFireEntity) {
        return newExplosion(w, entityExploded, player, x, y, z, size, sizeBlock, playSound, isSmoking, isFlaming, isDestroyBlock, countSetFireEntity, null);
    }

    public static MCH_Explosion.ExplosionResult newExplosion(World w, Entity entityExploded, Entity player, double x, double y, double z,
                                                             float size, float sizeBlock, boolean playSound, boolean isSmoking, boolean isFlaming,
                                                             boolean isDestroyBlock, int countSetFireEntity, MCH_DamageFactor df) {
        return newExplosion(w, entityExploded, player, x, y, z, size, sizeBlock, playSound, isSmoking, isFlaming, isDestroyBlock, countSetFireEntity, df, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static MCH_Explosion.ExplosionResult newExplosion(World w, Entity entityExploded, Entity player, double x, double y, double z,
                                                             float size, float sizeBlock, boolean playSound, boolean isSmoking, boolean isFlaming,
                                                             boolean isDestroyBlock, int countSetFireEntity, MCH_DamageFactor df,
                                                             float damageVsPlayer, float damageVsLiving, float damageVsPlane, float damageVsHeli, float damageVsTank, float damageVsVehicle) {
        if (w.isRemote) {
            return null;
        } else {
            MCH_Explosion exp = new MCH_Explosion(w, entityExploded, player, x, y, z, size);
            exp.isSmoking = isSmoking;
            exp.isFlaming = isFlaming;
            exp.isDestroyBlock = w.getGameRules().getGameRuleBooleanValue("mobGriefing") && isDestroyBlock;
            exp.explosionSizeBlock = sizeBlock;
            exp.countSetFireEntity = countSetFireEntity;
            exp.isPlaySound = playSound;
            exp.isInWater = false;
            exp.result = exp.newExplosionResult();
            exp.damageFactor = df;
            exp.damageVsPlayer = damageVsPlayer;
            exp.damageVsLiving = damageVsLiving;
            exp.damageVsPlane = damageVsPlane;
            exp.damageVsHeli = damageVsHeli;
            exp.damageVsTank = damageVsTank;
            exp.damageVsVehicle = damageVsVehicle;
            exp.doExplosionA();
            exp.doExplosionB(true);
            MCH_PacketEffectExplosion.ExplosionParam param = MCH_PacketEffectExplosion.create();
            param.exploderID = W_Entity.getEntityId(entityExploded);
            param.posX = x;
            param.posY = y;
            param.posZ = z;
            param.size = size;
            param.inWater = false;
            param.isSmoking = exp.isSmoking;
            MCH_PacketEffectExplosion.send(param);
            return exp.result;
        }
    }

    public static MCH_Explosion.ExplosionResult newExplosionInWater(World w, Entity entityExploded, Entity player, double x, double y, double z, float size, float sizeBlock,
                                                                    boolean playSound, boolean isSmoking, boolean isFlaming, boolean isDestroyBlock, int countSetFireEntity, MCH_DamageFactor df,
                                                                    float damageVsPlayer, float damageVsLiving, float damageVsPlane, float damageVsHeli, float damageVsTank, float damageVsVehicle) {
        if (w.isRemote) {
            return null;
        } else {
            MCH_Explosion exp = new MCH_Explosion(w, entityExploded, player, x, y, z, size);
            exp.isSmoking = isSmoking;
            exp.isFlaming = isFlaming;
            exp.isDestroyBlock = w.getGameRules().getGameRuleBooleanValue("mobGriefing") && isDestroyBlock;
            exp.explosionSizeBlock = sizeBlock;
            exp.countSetFireEntity = countSetFireEntity;
            exp.isPlaySound = playSound;
            exp.isInWater = true;
            exp.result = exp.newExplosionResult();
            exp.damageFactor = df;
            exp.damageVsPlayer = damageVsPlayer;
            exp.damageVsLiving = damageVsLiving;
            exp.damageVsPlane = damageVsPlane;
            exp.damageVsHeli = damageVsHeli;
            exp.damageVsTank = damageVsTank;
            exp.damageVsVehicle = damageVsVehicle;
            exp.doExplosionA();
            exp.doExplosionB(true);
            MCH_PacketEffectExplosion.ExplosionParam param = MCH_PacketEffectExplosion.create();
            param.exploderID = W_Entity.getEntityId(entityExploded);
            param.posX = x;
            param.posY = y;
            param.posZ = z;
            param.size = size;
            param.inWater = true;
            param.isSmoking = exp.isSmoking;
            MCH_PacketEffectExplosion.send(param);
            return exp.result;
        }
    }

    public static void playExplosionSound(World w, double x, double y, double z) {
        Random rand = new Random();
        W_WorldFunc.DEF_playSoundEffect(w, x, y, z, "random.explode", 4.0F, (1.0F + (rand.nextFloat() - rand.nextFloat()) * 0.2F) * 0.7F);
    }

    public static void effectExplosion(World world, Entity exploder, double explosionX, double explosionY, double explosionZ, float explosionSize, boolean isSmoking) {
        ArrayList affectedBlockPositions = new ArrayList();
        boolean field_77289_h = true;
        HashSet hashset = new HashSet();

        int i;
        int j;
        int k;
        double d0;
        double d1;
        double d2;
        for (i = 0; i < 16; ++i) {
            for (j = 0; j < 16; ++j) {
                for (k = 0; k < 16; ++k) {
                    if (i == 0 || i == 15 || j == 0 || j == 15 || k == 0 || k == 15) {
                        double iterator = (double) ((float) i / 15.0F * 2.0F - 1.0F);
                        double l = (double) ((float) j / 15.0F * 2.0F - 1.0F);
                        double flareCnt = (double) ((float) k / 15.0F * 2.0F - 1.0F);
                        double d6 = Math.sqrt(iterator * iterator + l * l + flareCnt * flareCnt);
                        iterator /= d6;
                        l /= d6;
                        flareCnt /= d6;
                        float f1 = explosionSize * (0.7F + world.rand.nextFloat() * 0.6F);
                        d0 = explosionX;
                        d1 = explosionY;
                        d2 = explosionZ;

                        for (float mz = 0.3F; f1 > 0.0F; f1 -= mz * 0.75F) {
                            int l1 = MathHelper.floor_double(d0);
                            int d61 = MathHelper.floor_double(d1);
                            int j1 = MathHelper.floor_double(d2);
                            int d7 = W_WorldFunc.getBlockId(world, l1, d61, j1);
                            if (d7 > 0) {
                                Block block = W_Block.getBlockById(d7);
                                float px = block.getExplosionResistance(exploder, world, l1, d61, j1, explosionX, explosionY, explosionZ);
                                f1 -= (px + 0.3F) * mz;
                            }

                            if (f1 > 0.0F) {
                                hashset.add(new ChunkPosition(l1, d61, j1));
                            }

                            d0 += iterator * (double) mz;
                            d1 += l * (double) mz;
                            d2 += flareCnt * (double) mz;
                        }
                    }
                }
            }
        }

        affectedBlockPositions.addAll(hashset);
        if (explosionSize >= 2.0F && isSmoking) {
            MCH_ParticlesUtil.DEF_spawnParticle("hugeexplosion", explosionX, explosionY, explosionZ, 1.0D, 0.0D, 0.0D, 10.0F);
        } else {
            MCH_ParticlesUtil.DEF_spawnParticle("largeexplode", explosionX, explosionY, explosionZ, 1.0D, 0.0D, 0.0D, 10.0F);
        }

        // 如果需要生成冒烟效果
        if (isSmoking) {
            // 遍历受到爆炸影响的所有方块位置
            Iterator<ChunkPosition> blockPosIterator = affectedBlockPositions.iterator();
            int spawnCount = 0;
            // 根据爆炸大小确定可以生成的曳光弹数量
            int remainingFlares = (int) Math.max(explosionSize - 1, 10);

            while (blockPosIterator.hasNext()) {
                // 获取当前块坐标
                ChunkPosition pos = blockPosIterator.next();
                int blockX = W_ChunkPosition.getChunkPosX(pos);
                int blockY = W_ChunkPosition.getChunkPosY(pos);
                int blockZ = W_ChunkPosition.getChunkPosZ(pos);
                // 更新方块ID（返回值未使用，但可能触发内部逻辑）
                W_WorldFunc.getBlockId(world, blockX, blockY, blockZ);
                spawnCount++;

                // 在方块内部随机生成粒子位置
                double randX = blockX + world.rand.nextFloat();
                double randY = blockY + world.rand.nextFloat();
                double randZ = blockZ + world.rand.nextFloat();

                // 计算从爆炸中心到粒子位置的向量
                double dirX = randX - explosionX;
                double dirY = randY - explosionY;
                double dirZ = randZ - explosionZ;

                // 归一化该向量
                double distance = MathHelper.sqrt_double(dirX * dirX + dirY * dirY + dirZ * dirZ);
                dirX /= distance;
                dirY /= distance;
                dirZ /= distance;

                // 计算与距离和爆炸规模相关的速度缩放因子，并加入随机性
                double velocityScale = 0.5D / (distance / explosionSize + 0.1D);
                velocityScale *= world.rand.nextFloat() * world.rand.nextFloat() + 0.3F;

                // 应用缩放因子以获得实际运动速度
                dirX *= velocityScale * 0.5D;
                dirY *= velocityScale * 0.5D;
                dirZ *= velocityScale * 0.5D;

                // 计算爆炸中心到随机粒子位置的中点，用作发射源
                double particleX = (randX + explosionX) / 2.0D;
                double particleY = (randY + explosionY) / 2.0D;
                double particleZ = (randZ + explosionZ) / 2.0D;

                // 随机角度，用于生成曳光弹和碎片运动的方向
                double angle = Math.PI * world.rand.nextInt(360) / 180.0D;

                // 大爆炸时生成曳光弹，数量受 remainingFlares 限制
                if (explosionSize >= 6.0F && remainingFlares > 0) {
                    // 计算曳光弹的速度系数，受爆炸大小和随机数影响
                    double flareSpeed = Math.min(explosionSize / 16.0F, 0.6D) *
                            (0.5F + world.rand.nextFloat() * 0.5F);
                    // 创建并发射曳光弹实体
                    world.spawnEntityInWorld(
                            new MCH_EntityFlare(
                                    world,
                                    particleX,
                                    particleY + 2.0D,
                                    particleZ,
                                    Math.sin(angle) * flareSpeed,
                                    (1.0D + dirY / 5.0D) * flareSpeed,
                                    Math.cos(angle) * flareSpeed,
                                    2.0F,
                                    0,
                                    -0.025
                            )
                    );
                    remainingFlares--;
                }

                // 每处理四个方块位置，就生成一束碎块尘埃粒子
                if (spawnCount % 4 == 0) {
                    // 根据爆炸大小计算尘埃粒子的速度尺度
                    float dustVelocity = Math.min(explosionSize / 3.0F, 2.0F) *
                            (0.5F + world.rand.nextFloat() * 0.5F);
                    // 生成碎块尘埃粒子
                    MCH_ParticlesUtil.spawnParticleTileDust(
                            world,
                            (int) (particleX + 0.5D),
                            (int) (particleY - 0.5D),
                            (int) (particleZ + 0.5D),
                            particleX,
                            particleY + 1.0D,
                            particleZ,
                            Math.sin(angle) * dustVelocity,
                            0.5D + dirY / 5.0D * dustVelocity,
                            Math.cos(angle) * dustVelocity,
                            Math.min(explosionSize / 2.0F, 3.0F) *
                                    (0.5F + world.rand.nextFloat() * 0.5F)
                    );
                }

                // 根据爆炸大小确定生成爆炸粒子的间隔
                int modInterval = (int) (Math.max(explosionSize, 4.0F));
                if (explosionSize <= 1.0F || spawnCount % modInterval == 0) {
                    // 随机调整粒子的速度方向以增加多样性
                    if (world.rand.nextBoolean()) {
                        dirY *= 3.0D;
                        dirX *= 0.1D;
                        dirZ *= 0.1D;
                    } else {
                        dirY *= 0.2D;
                        dirX *= 3.0D;
                        dirZ *= 3.0D;
                    }

                    // 创建爆炸粒子参数对象
                    MCH_ParticleParam particle = new MCH_ParticleParam(
                            world,
                            "explode",
                            particleX,
                            particleY,
                            particleZ,
                            dirX,
                            dirY,
                            dirZ,
                            explosionSize < 8.0F ? (explosionSize < 2.0F ? 2.0F : explosionSize * 2.0F) : 16.0F
                    );
                    // 为粒子随机设置颜色值（RGB通道）
                    particle.r = particle.g = particle.b =
                            0.3F + world.rand.nextFloat() * 0.4F;
                    particle.r += 0.1F;
                    particle.g += 0.05F;

                    // 随机设置粒子的生命周期
                    particle.age = 5 + world.rand.nextInt(10);
                    particle.age = (int) ((float) particle.age *
                            (Math.max(explosionSize, 4.0F)));
                    particle.diffusible = true;

                    // 发射爆炸粒子
                    MCH_ParticlesUtil.spawnParticle(particle);
                }
            }
        }


    }

    public static void DEF_effectExplosion(World world, Entity exploder, double explosionX, double explosionY, double explosionZ, float explosionSize, boolean isSmoking) {
        ArrayList affectedBlockPositions = new ArrayList();
        boolean field_77289_h = true;
        HashSet hashset = new HashSet();

        int i;
        int j;
        int k;
        double d0;
        double d1;
        double d2;
        for (i = 0; i < 16; ++i) {
            for (j = 0; j < 16; ++j) {
                for (k = 0; k < 16; ++k) {
                    if (i == 0 || i == 15 || j == 0 || j == 15 || k == 0 || k == 15) {
                        double iterator = (double) ((float) i / 15.0F * 2.0F - 1.0F);
                        double l = (double) ((float) j / 15.0F * 2.0F - 1.0F);
                        double d5 = (double) ((float) k / 15.0F * 2.0F - 1.0F);
                        double d6 = Math.sqrt(iterator * iterator + l * l + d5 * d5);
                        iterator /= d6;
                        l /= d6;
                        d5 /= d6;
                        float f1 = explosionSize * (0.7F + world.rand.nextFloat() * 0.6F);
                        d0 = explosionX;
                        d1 = explosionY;
                        d2 = explosionZ;

                        for (float d61 = 0.3F; f1 > 0.0F; f1 -= d61 * 0.75F) {
                            int l1 = MathHelper.floor_double(d0);
                            int d7 = MathHelper.floor_double(d1);
                            int j1 = MathHelper.floor_double(d2);
                            int k1 = W_WorldFunc.getBlockId(world, l1, d7, j1);
                            if (k1 > 0) {
                                Block block = W_Block.getBlockById(k1);
                                float f3 = block.getExplosionResistance(exploder, world, l1, d7, j1, explosionX, explosionY, explosionZ);
                                f1 -= (f3 + 0.3F) * d61;
                            }

                            if (f1 > 0.0F) {
                                hashset.add(new ChunkPosition(l1, d7, j1));
                            }

                            d0 += iterator * (double) d61;
                            d1 += l * (double) d61;
                            d2 += d5 * (double) d61;
                        }
                    }
                }
            }
        }

        affectedBlockPositions.addAll(hashset);
        if (explosionSize >= 2.0F && isSmoking) {
            MCH_ParticlesUtil.DEF_spawnParticle("hugeexplosion", explosionX, explosionY, explosionZ, 1.0D, 0.0D, 0.0D, 10.0F);
        } else {
            MCH_ParticlesUtil.DEF_spawnParticle("largeexplode", explosionX, explosionY, explosionZ, 1.0D, 0.0D, 0.0D, 10.0F);
        }

        if (isSmoking) {
            Iterator var39 = affectedBlockPositions.iterator();

            while (var39.hasNext()) {
                ChunkPosition chunkposition = (ChunkPosition) var39.next();
                i = W_ChunkPosition.getChunkPosX(chunkposition);
                j = W_ChunkPosition.getChunkPosY(chunkposition);
                k = W_ChunkPosition.getChunkPosZ(chunkposition);
                W_WorldFunc.getBlockId(world, i, j, k);
                d0 = (double) ((float) i + world.rand.nextFloat());
                d1 = (double) ((float) j + world.rand.nextFloat());
                d2 = (double) ((float) k + world.rand.nextFloat());
                double d3 = d0 - explosionX;
                double d4 = d1 - explosionY;
                double d51 = d2 - explosionZ;
                double var40 = (double) MathHelper.sqrt_double(d3 * d3 + d4 * d4 + d51 * d51);
                d3 /= var40;
                d4 /= var40;
                d51 /= var40;
                double var41 = 0.5D / (var40 / (double) explosionSize + 0.1D);
                var41 *= (double) (world.rand.nextFloat() * world.rand.nextFloat() + 0.3F);
                d3 *= var41;
                d4 *= var41;
                d51 *= var41;
                MCH_ParticlesUtil.DEF_spawnParticle("explode", (d0 + explosionX * 1.0D) / 2.0D, (d1 + explosionY * 1.0D) / 2.0D, (d2 + explosionZ * 1.0D) / 2.0D, d3, d4, d51, 10.0F);
                MCH_ParticlesUtil.DEF_spawnParticle("smoke", d0, d1, d2, d3, d4, d51, 10.0F);
            }
        }

    }

    public static void effectExplosionInWater(World world, Entity exploder, double explosionX, double explosionY, double explosionZ, float explosionSize, boolean isSmoking) {
        if (explosionSize > 0.0F) {
            int range = (int) ((double) explosionSize + 0.5D) / 1;
            int ex = (int) (explosionX + 0.5D);
            int ey = (int) (explosionY + 0.5D);
            int ez = (int) (explosionZ + 0.5D);

            for (int y = -range; y <= range; ++y) {
                if (ey + y >= 1) {
                    for (int x = -range; x <= range; ++x) {
                        for (int z = -range; z <= range; ++z) {
                            int d = x * x + y * y + z * z;
                            if (d < range * range && W_Block.isEqualTo(W_WorldFunc.getBlock(world, ex + x, ey + y, ez + z), W_Block.getWater())) {
                                int n = explosionRNG.nextInt(2);

                                for (int i = 0; i < n; ++i) {
                                    MCH_ParticleParam prm = new MCH_ParticleParam(world, "splash", (double) (ex + x), (double) (ey + y), (double) (ez + z), (double) x / (double) range * ((double) explosionRNG.nextFloat() - 0.2D), 1.0D - Math.sqrt((double) (x * x + z * z)) / (double) range + (double) explosionRNG.nextFloat() * 0.4D * (double) range * 0.4D, (double) z / (double) range * ((double) explosionRNG.nextFloat() - 0.2D), (float) (explosionRNG.nextInt(range) * 3 + range));
                                    MCH_ParticlesUtil.spawnParticle(prm);
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    public boolean isRemote() {
        return this.world.isRemote;
    }

    public void doExplosionA() {
        HashSet hashset = new HashSet();
        int i = 0;

        while (true) {
            this.getClass();
            int j;
            int k;
            double d0;
            double d1;
            double d2;
            if (i >= 16) {
                float var33 = super.explosionSize;
                super.affectedBlockPositions.addAll(hashset);
                super.explosionSize *= 2.0F;
                i = MathHelper.floor_double(super.explosionX - (double) super.explosionSize - 1.0D);
                j = MathHelper.floor_double(super.explosionX + (double) super.explosionSize + 1.0D);
                k = MathHelper.floor_double(super.explosionY - (double) super.explosionSize - 1.0D);
                int l1 = MathHelper.floor_double(super.explosionY + (double) super.explosionSize + 1.0D);
                int var34 = MathHelper.floor_double(super.explosionZ - (double) super.explosionSize - 1.0D);
                int j2 = MathHelper.floor_double(super.explosionZ + (double) super.explosionSize + 1.0D);
                List var35 = this.world.getEntitiesWithinAABBExcludingEntity(super.exploder, W_AxisAlignedBB.getAABB((double) i, (double) k, (double) var34, (double) j, (double) l1, (double) j2));
                Vec3 vec3 = W_WorldFunc.getWorldVec3(this.world, super.explosionX, super.explosionY, super.explosionZ);
                super.exploder = this.explodedPlayer;

                for (int var37 = 0; var37 < var35.size(); ++var37) {
                    Entity entity = (Entity) var35.get(var37);
                    double var38 = entity.getDistance(super.explosionX, super.explosionY, super.explosionZ) / (double) super.explosionSize;
                    if (var38 <= 1.0D) {
                        d0 = entity.posX - super.explosionX;
                        d1 = entity.posY + (double) entity.getEyeHeight() - super.explosionY;
                        d2 = entity.posZ - super.explosionZ;
                        double var39 = (double) MathHelper.sqrt_double(d0 * d0 + d1 * d1 + d2 * d2);
                        if (var39 != 0.0D) {
                            d0 /= var39;
                            d1 /= var39;
                            d2 /= var39;
                            double var40 = this.getBlockDensity(vec3, entity.boundingBox);
                            double var41 = (1.0D - var38) * var40;
                            float damage = (float) ((int) ((var41 * var41 + var41) / 2.0D * 8.0D * (double) super.explosionSize + 1.0D));
                            if (damage > 0.0F && this.result != null && !(entity instanceof EntityItem) && !(entity instanceof EntityExpBottle) && !(entity instanceof EntityXPOrb) && !W_Entity.isEntityFallingBlock(entity)) {
                                if (entity instanceof MCH_EntityBaseBullet && super.exploder instanceof EntityPlayer) {
                                    if (!W_Entity.isEqual(((MCH_EntityBaseBullet) entity).shootingEntity, super.exploder)) {
                                        this.result.hitEntity = true;
                                        MCH_Lib.DbgLog(this.world, "MCH_Explosion.doExplosionA:Damage=%.1f:HitEntityBullet=" + entity.getClass(), damage);
                                    }
                                } else {
                                    MCH_Lib.DbgLog(this.world, "MCH_Explosion.doExplosionA:Damage=%.1f:HitEntity=" + entity.getClass(), damage);
                                    this.result.hitEntity = true;
                                }
                            }

                            MCH_Lib.applyEntityHurtResistantTimeConfig(entity);
                            DamageSource ds = DamageSource.setExplosionSource(this);
                            MCH_Config var36 = MCH_MOD.config;
                            damage = MCH_Config.applyDamageVsEntity(entity, ds, damage);
                            damage *= this.damageFactor != null ? this.damageFactor.getDamageFactor(entity) : 1.0F;
                            if (entity instanceof EntityPlayer) damage *= damageVsPlayer;
                            else if (entity instanceof EntityLivingBase) damage *= damageVsLiving;
                            else if (entity instanceof MCP_EntityPlane) damage *= damageVsPlane;
                            else if (entity instanceof MCH_EntityHeli) damage *= damageVsHeli;
                            else if (entity instanceof MCH_EntityTank) damage *= damageVsTank;
                            else if (entity instanceof MCH_EntityVehicle) damage *= damageVsVehicle;
                            W_Entity.attackEntityFrom(entity, ds, damage);
                            double d11 = EnchantmentProtection.func_92092_a(entity, var41);
                            if (!(entity instanceof MCH_EntityBaseBullet)) {
                                entity.motionX += d0 * d11 * 0.4D;
                                entity.motionY += d1 * d11 * 0.1D;
                                entity.motionZ += d2 * d11 * 0.4D;
                            }

                            if (entity instanceof EntityPlayer) {
                                this.field_77288_k.put(entity, W_WorldFunc.getWorldVec3(this.world, d0 * var41, d1 * var41, d2 * var41));
                            }

                            if (damage > 0.0F && this.countSetFireEntity > 0) {
                                double fireFactor = 1.0D - var39 / (double) super.explosionSize;
                                if (fireFactor > 0.0D) {
                                    entity.setFire((int) (fireFactor * (double) this.countSetFireEntity));
                                }
                            }
                        }
                    }
                }

                super.explosionSize = var33;
                return;
            }

            j = 0;

            while (true) {
                this.getClass();
                if (j >= 16) {
                    ++i;
                    break;
                }

                k = 0;

                while (true) {
                    this.getClass();
                    if (k >= 16) {
                        ++j;
                        break;
                    }

                    label134:
                    {
                        if (i != 0) {
                            this.getClass();
                            if (i != 16 - 1 && j != 0) {
                                this.getClass();
                                if (j != 16 - 1 && k != 0) {
                                    this.getClass();
                                    if (k != 16 - 1) {
                                        break label134;
                                    }
                                }
                            }
                        }

                        float var10000 = (float) i;
                        this.getClass();
                        double f = (double) (var10000 / (16.0F - 1.0F) * 2.0F - 1.0F);
                        var10000 = (float) j;
                        this.getClass();
                        double i2 = (double) (var10000 / (16.0F - 1.0F) * 2.0F - 1.0F);
                        var10000 = (float) k;
                        this.getClass();
                        double list = (double) (var10000 / (16.0F - 1.0F) * 2.0F - 1.0F);
                        double k2 = Math.sqrt(f * f + i2 * i2 + list * list);
                        f /= k2;
                        i2 /= k2;
                        list /= k2;
                        float d7 = this.explosionSizeBlock * (0.7F + this.world.rand.nextFloat() * 0.6F);
                        d0 = super.explosionX;
                        d1 = super.explosionY;
                        d2 = super.explosionZ;

                        for (float f2 = 0.3F; d7 > 0.0F; d7 -= 0.22500001F) {
                            int d8 = MathHelper.floor_double(d0);
                            int i1 = MathHelper.floor_double(d1);
                            int d9 = MathHelper.floor_double(d2);
                            int k1 = W_WorldFunc.getBlockId(this.world, d8, i1, d9);
                            if (k1 > 0) {
                                Block d10 = W_WorldFunc.getBlock(this.world, d8, i1, d9);
                                float f3;
                                if (super.exploder != null) {
                                    f3 = W_Entity.getBlockExplosionResistance(super.exploder, this, this.world, d8, i1, d9, d10);
                                } else {
                                    f3 = d10.getExplosionResistance(super.exploder, this.world, d8, i1, d9, super.explosionX, super.explosionY, super.explosionZ);
                                }

                                if (this.isInWater) {
                                    f3 *= this.world.rand.nextFloat() * 0.2F + 0.2F;
                                }

                                d7 -= (f3 + 0.3F) * 0.3F;
                            }

                            if (d7 > 0.0F && (super.exploder == null || W_Entity.shouldExplodeBlock(super.exploder, this, this.world, d8, i1, d9, k1, d7))) {
                                hashset.add(new ChunkPosition(d8, i1, d9));
                            }

                            d0 += f * 0.30000001192092896D;
                            d1 += i2 * 0.30000001192092896D;
                            d2 += list * 0.30000001192092896D;
                        }
                    }

                    ++k;
                }
            }
        }
    }

    private double getBlockDensity(Vec3 vec3, AxisAlignedBB p_72842_2_) {
        double d0 = 1.0D / ((p_72842_2_.maxX - p_72842_2_.minX) * 2.0D + 1.0D);
        double d1 = 1.0D / ((p_72842_2_.maxY - p_72842_2_.minY) * 2.0D + 1.0D);
        double d2 = 1.0D / ((p_72842_2_.maxZ - p_72842_2_.minZ) * 2.0D + 1.0D);
        if (d0 >= 0.0D && d1 >= 0.0D && d2 >= 0.0D) {
            int i = 0;
            int j = 0;

            for (float f = 0.0F; f <= 1.0F; f = (float) ((double) f + d0)) {
                for (float f1 = 0.0F; f1 <= 1.0F; f1 = (float) ((double) f1 + d1)) {
                    for (float f2 = 0.0F; f2 <= 1.0F; f2 = (float) ((double) f2 + d2)) {
                        double d3 = p_72842_2_.minX + (p_72842_2_.maxX - p_72842_2_.minX) * (double) f;
                        double d4 = p_72842_2_.minY + (p_72842_2_.maxY - p_72842_2_.minY) * (double) f1;
                        double d5 = p_72842_2_.minZ + (p_72842_2_.maxZ - p_72842_2_.minZ) * (double) f2;
                        if (this.world.func_147447_a(Vec3.createVectorHelper(d3, d4, d5), vec3, false, true, false) == null) {
                            ++i;
                        }

                        ++j;
                    }
                }
            }

            return (double) ((float) i / (float) j);
        } else {
            return 0.0D;
        }
    }

    public void doExplosionB(boolean par1) {
        if (this.isPlaySound) {
            W_WorldFunc.DEF_playSoundEffect(this.world, super.explosionX, super.explosionY, super.explosionZ, "random.explode", 4.0F, (1.0F + (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.2F) * 0.7F);
        }

        MCH_Config var10000;
        Iterator iterator;
        ChunkPosition chunkposition;
        int i;
        int j;
        int k;
        int l;
        Block b;
        if (super.isSmoking) {
            iterator = super.affectedBlockPositions.iterator();

            while (iterator.hasNext()) {
                chunkposition = (ChunkPosition) iterator.next();
                i = W_ChunkPosition.getChunkPosX(chunkposition);
                j = W_ChunkPosition.getChunkPosY(chunkposition);
                k = W_ChunkPosition.getChunkPosZ(chunkposition);
                l = W_WorldFunc.getBlockId(this.world, i, j, k);
                if (l > 0 && this.isDestroyBlock && this.explosionSizeBlock > 0.0F) {
                    var10000 = MCH_MOD.config;
                    if (MCH_Config.Explosion_DestroyBlock.prmBool) {
                        b = W_Block.getBlockById(l);
                        if (b.canDropFromExplosion(this)) {
                            b.dropBlockAsItemWithChance(this.world, i, j, k, this.world.getBlockMetadata(i, j, k), 1.0F / this.explosionSizeBlock, 0);
                        }

                        b.onBlockExploded(this.world, i, j, k, this);
                    }
                }
            }
        }

        if (super.isFlaming) {
            var10000 = MCH_MOD.config;
            if (MCH_Config.Explosion_FlamingBlock.prmBool) {
                iterator = super.affectedBlockPositions.iterator();

                while (iterator.hasNext()) {
                    chunkposition = (ChunkPosition) iterator.next();
                    i = W_ChunkPosition.getChunkPosX(chunkposition);
                    j = W_ChunkPosition.getChunkPosY(chunkposition);
                    k = W_ChunkPosition.getChunkPosZ(chunkposition);
                    l = W_WorldFunc.getBlockId(this.world, i, j, k);
                    b = W_WorldFunc.getBlock(this.world, i, j - 1, k);
                    if (l == 0 && b != null && b.isOpaqueCube() && explosionRNG.nextInt(3) == 0) {
                        W_WorldFunc.setBlock(this.world, i, j, k, Blocks.fire);
                    }
                }
            }
        }

    }

    public MCH_Explosion.ExplosionResult newExplosionResult() {
        return new MCH_Explosion.ExplosionResult();
    }

    public class ExplosionResult {

        public boolean hitEntity = false;


    }
}