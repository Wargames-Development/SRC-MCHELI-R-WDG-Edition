package mcheli;

import mcheli.flare.MCH_EntityFlare;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.particles.MCH_ParticleParam;
import mcheli.particles.MCH_ParticlesUtil;
import mcheli.plane.MCP_EntityPlane;
import mcheli.tank.MCH_EntityTank;
import mcheli.vehicle.MCH_EntityVehicle;
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
    public World world;
    public Map field_77288_k = new HashMap();
    public MCH_ExplosionParam param;
    public boolean destroyBlocksByRule;
    MCH_Explosion.ExplosionResult result;

    public MCH_Explosion(World world, MCH_ExplosionParam param) {
        super(world, param.exploder, param.x, param.y, param.z, param.size);
        this.world = world;
        this.param = param;
        this.result = newExplosionResult();
        this.isSmoking = param.isSmoking;
        this.isFlaming = param.isFlaming;
        this.destroyBlocksByRule = world.getGameRules().getGameRuleBooleanValue("mobGriefing") && param.isDestroyBlock;
    }

    public static MCH_Explosion.ExplosionResult newExplosion(World w, MCH_ExplosionParam p) {
        if (w.isRemote) {
            return null;
        }
        MCH_Explosion exp = new MCH_Explosion(w, p);
        exp.doExplosionA();
        exp.doExplosionB(true);
        MCH_PacketEffectExplosion.ExplosionParam net = MCH_PacketEffectExplosion.create();
        net.exploderID = W_Entity.getEntityId(p.exploder);
        net.posX = p.x;
        net.posY = p.y;
        net.posZ = p.z;
        net.size = p.size;
        net.inWater = p.isInWater;
        net.isSmoking = p.isSmoking;
        MCH_PacketEffectExplosion.send(net);
        return exp.result;
    }

    public static void playExplosionSound(World w, double x, double y, double z) {
        Random rand = new Random();
        W_WorldFunc.DEF_playSoundEffect(w, x, y, z, "random.explode", 4.0F, (1.0F + (rand.nextFloat() - rand.nextFloat()) * 0.2F) * 0.7F);
    }

    public static void effectExplosion(World world, Entity exploder, double explosionX, double explosionY, double explosionZ, float explosionSize, boolean isSmoking) {
        ArrayList affectedBlockPositions = new ArrayList();
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
                        dirY *= 4.0D;
                        dirX *= 0.1D;
                        dirZ *= 0.1D;
                    } else {
                        dirY *= 0.2D;
                        dirX *= 2.0D;
                        dirZ *= 2.0D;
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
                        explosionSize < 8.0F ? (explosionSize < 2.0F ? 1.5F : explosionSize * 1.5F) : 12.0F
                    );
                    // 为粒子随机设置颜色值（RGB通道）
                    particle.r = particle.g = particle.b =
                        0.3F + world.rand.nextFloat() * 0.4F;
                    particle.r += 0.1F;
                    particle.g += 0.05F;
                    particle.a = 0.4F + world.rand.nextFloat() * 0.4F;

                    // 随机设置粒子的生命周期
                    particle.age = 5 + world.rand.nextInt(10);
                    particle.age = (int) ((float) particle.age *
                        (Math.max(explosionSize / 2, 6.0F)));
                    particle.gravity = 0.001f;
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

    /**
     * 某些实体不参与 result 统计与日志，保持与原版一致的排除项
     */
    private static boolean isIgnorableEntity(Entity e) {
        return (e instanceof EntityItem)
            || (e instanceof EntityExpBottle)
            || (e instanceof EntityXPOrb)
            || W_Entity.isEntityFallingBlock(e);
    }

    /**
     * 不同目标类型的伤害倍率（原逻辑抽取成函数，便于维护）
     */
    private static float applyTypeMultipliers(Entity e, float damage, MCH_ExplosionParam param) {
        if (e instanceof EntityPlayer) {
            return damage * param.damageVsPlayer;
        } else if (e instanceof EntityLivingBase) {
            return damage * param.damageVsLiving;
        } else if (e instanceof MCP_EntityPlane) {
            MCP_EntityPlane plane = (MCP_EntityPlane) e;
            if (plane.getAcInfo() != null && plane.getAcInfo().isFloat) {
                return damage * param.damageVsShip;
            } else {
                return damage * param.damageVsPlane;
            }
        } else if (e instanceof MCH_EntityHeli) {
            return damage * param.damageVsHeli;
        } else if (e instanceof MCH_EntityTank) {
            return damage * param.damageVsTank;
        } else if (e instanceof MCH_EntityVehicle) {
            return damage * param.damageVsVehicle;
        }
        return damage;
    }

    /**
     * 计算点到 AABB 的最小欧氏距离（在盒内则为 0）
     */
    private static double distancePointToAABB(double px, double py, double pz, net.minecraft.util.AxisAlignedBB bb) {
        double dx = 0.0D;
        if (px < bb.minX) dx = bb.minX - px;
        else if (px > bb.maxX) dx = px - bb.maxX;
        double dy = 0.0D;
        if (py < bb.minY) dy = bb.minY - py;
        else if (py > bb.maxY) dy = py - bb.maxY;
        double dz = 0.0D;
        if (pz < bb.minZ) dz = bb.minZ - pz;
        else if (pz > bb.maxZ) dz = pz - bb.maxZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public boolean isRemote() {
        return this.world.isRemote;
    }

    @Override
    public void doExplosionA() {
        // === 1) 采样射线，收集会受影响的方块（供 doExplosionB 真正处理） ===
        final int RAYS = 16;
        final double STEP = 0.30000001192092896D;
        final Set<ChunkPosition> affected = new HashSet<>();

        for (int xi = 0; xi < RAYS; xi++) {
            for (int yi = 0; yi < RAYS; yi++) {
                for (int zi = 0; zi < RAYS; zi++) {
                    // 只在“立方体表面”发射射线（减少重复）
                    if (xi != 0 && xi != RAYS - 1 && yi != 0 && yi != RAYS - 1 && zi != 0 && zi != RAYS - 1) {
                        continue;
                    }

                    // 将 [0,RAYS-1] 映射到 [-1,1]，得到方向向量
                    double dx = xi / (RAYS - 1.0D) * 2.0D - 1.0D;
                    double dy = yi / (RAYS - 1.0D) * 2.0D - 1.0D;
                    double dz = zi / (RAYS - 1.0D) * 2.0D - 1.0D;
                    double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    dx /= len;
                    dy /= len;
                    dz /= len;

                    float blast = param.sizeBlock * (0.7F + this.world.rand.nextFloat() * 0.6F);
                    double px = this.explosionX;
                    double py = this.explosionY;
                    double pz = this.explosionZ;

                    // 沿射线行进，衰减爆轰强度并记录方块
                    for (float step = 0.3F; blast > 0.0F; blast -= 0.22500001F) {
                        final int bx = MathHelper.floor_double(px);
                        final int by = MathHelper.floor_double(py);
                        final int bz = MathHelper.floor_double(pz);

                        final int blockId = W_WorldFunc.getBlockId(this.world, bx, by, bz);
                        if (blockId > 0) {
                            final Block block = W_WorldFunc.getBlock(this.world, bx, by, bz);
                            float resistance;
                            if (this.exploder != null) {
                                resistance = W_Entity.getBlockExplosionResistance(this.exploder, this, this.world, bx, by, bz, block);
                            } else {
                                resistance = block.getExplosionResistance(this.exploder, this.world, bx, by, bz,
                                    this.explosionX, this.explosionY, this.explosionZ);
                            }
                            if (param.isInWater) {
                                resistance *= this.world.rand.nextFloat() * 0.2F + 0.2F;
                            }
                            blast -= (resistance + 0.3F) * 0.3F;
                        }

                        if (blast > 0.0F && (this.exploder == null ||
                            W_Entity.shouldExplodeBlock(this.exploder, this, this.world, bx, by, bz, blockId, blast))) {
                            affected.add(new ChunkPosition(bx, by, bz));
                        }

                        px += dx * STEP;
                        py += dy * STEP;
                        pz += dz * STEP;
                    }
                }
            }
        }
        this.affectedBlockPositions.addAll(affected);

        // === 2) 处理实体伤害与击退 ===
        final float sizeBefore = this.explosionSize;
        this.explosionSize *= 2.0F; // 原版做法：扩大一次半径用于实体搜索

        final int minX = MathHelper.floor_double(this.explosionX - this.explosionSize - 1.0D);
        final int maxX = MathHelper.floor_double(this.explosionX + this.explosionSize + 1.0D);
        final int minY = MathHelper.floor_double(this.explosionY - this.explosionSize - 1.0D);
        final int maxY = MathHelper.floor_double(this.explosionY + this.explosionSize + 1.0D);
        final int minZ = MathHelper.floor_double(this.explosionZ - this.explosionSize - 1.0D);
        final int maxZ = MathHelper.floor_double(this.explosionZ + this.explosionSize + 1.0D);

        final List list = this.world.getEntitiesWithinAABBExcludingEntity(
            this.exploder, W_AxisAlignedBB.getAABB(minX, minY, minZ, maxX, maxY, maxZ));

        final Vec3 center = W_WorldFunc.getWorldVec3(this.world, this.explosionX, this.explosionY, this.explosionZ);

        // 伤害归属：如果由玩家引发，设置为玩家（保持原有行为）
        this.exploder = param.player;

        // “点爆”基础伤害：在 rDist=0、无遮挡 时的理论伤害（用于直击实体）
        final float pointBlankBase = (float) ((int) (8.0D * (double) this.explosionSize + 1.0D));

        // === 2.1 普通实体（排除直击对象） ===
        for (Object o : list) {
            final Entity e = (Entity) o;

            // 排除“直击实体”，它将被单独处理
            if (param.directAttackEntity != null && W_Entity.isEqual(e, param.directAttackEntity)) {
                continue;
            }

            final double rDist = e.getDistance(this.explosionX, this.explosionY, this.explosionZ) / (double) this.explosionSize;
            if (rDist > 1.0D) {
                continue;
            }

            double vx = e.posX - this.explosionX;
            double vy = e.posY + (double) e.getEyeHeight() - this.explosionY;
            double vz = e.posZ - this.explosionZ;
            final double vLen = MathHelper.sqrt_double(vx * vx + vy * vy + vz * vz);
            if (vLen == 0.0D) {
                continue;
            }
            vx /= vLen;
            vy /= vLen;
            vz /= vLen;

            // 用于击退/向量登记的遮挡+距离因子（保持原版感觉）
            double density = param.explosionThroughWall ? 1.0D : this.getBlockDensity(center, e.boundingBox);
            final double attenForKnock = (1.0D - rDist) * density;
            final double attenForDamage = Math.max(0.0D, attenForKnock);

            float damage = (float) ((int) (((attenForDamage * attenForDamage + attenForDamage) / 2.0D) * 8.0D
                * (double) this.explosionSize + 1.0D));

            if (damage > 0.0F && this.result != null && !isIgnorableEntity(e)) {
                this.result.hitEntity = true;
                MCH_Lib.DbgLog(this.world, "MCH_Explosion.doExplosionA:Damage=%.1f:HitEntity=%s", damage, e.getClass());
            }

            // 统一应用伤害免疫/配置
            MCH_Lib.applyEntityHurtResistantTimeConfig(e);
            DamageSource ds = DamageSource.setExplosionSource(this);
            damage = MCH_Config.applyDamageVsEntity(e, ds, damage);
            damage = applyTypeMultipliers(e, damage, param);

            // 施加伤害
            e.attackEntityFrom(ds, damage);

            // 击退与向量登记
            final double kb = EnchantmentProtection.func_92092_a(e, attenForKnock);
            if (e instanceof EntityLivingBase) {
                e.motionX += vx * kb * 0.4D;
                e.motionY += vy * kb * 0.1D;
                e.motionZ += vz * kb * 0.4D;
            }
            if (e instanceof EntityPlayer) {
                this.field_77288_k.put(e, W_WorldFunc.getWorldVec3(this.world, vx * attenForKnock, vy * attenForKnock, vz * attenForKnock));
            }

            // 点燃：保持原行为（随“中心距离”线性）
            if (damage > 0.0F && param.countSetFireEntity > 0) {
                final double fireFactor = 1.0D - vLen / (double) this.explosionSize;
                if (fireFactor > 0.0D) {
                    e.setFire((int) (fireFactor * (double) param.countSetFireEntity));
                }
            }
        }

        // === 2.2 直击实体：单独固定伤害 ===
        if (param.directAttackEntity != null && !isIgnorableEntity(param.directAttackEntity)) {
            final Entity e = param.directAttackEntity;

            // 用“爆点到 AABB 的最小距离”来计算击退/点燃的衰减（伤害固定为点爆，不衰减）
            final double minDistToBox = distancePointToAABB(this.explosionX, this.explosionY, this.explosionZ, e.boundingBox);
            final double rDistBox = Math.min(1.0D, minDistToBox / (double) this.explosionSize);

            // 方向向量仍用“爆心 -> 眼睛高度”的方向
            double vx = e.posX - this.explosionX;
            double vy = e.posY + (double) e.getEyeHeight() - this.explosionY;
            double vz = e.posZ - this.explosionZ;
            final double vLen = MathHelper.sqrt_double(vx * vx + vy * vy + vz * vz);
            if (vLen != 0.0D) {
                vx /= vLen;
                vy /= vLen;
                vz /= vLen;
            }

            // 击退/登记按距离+遮挡（但不影响“固定伤害”）
            double density = param.explosionThroughWall ? 1.0D : this.getBlockDensity(center, e.boundingBox);
            final double attenForKnock = (1.0D - rDistBox) * density;

            // === 固定伤害：点爆伤害，不随距离/遮挡衰减 ===
            float damage = pointBlankBase;

            if (this.result != null) {
                this.result.hitEntity = true;
                MCH_Lib.DbgLog(this.world, "MCH_Explosion.doExplosionA:Damage=%.1f:DirectHit=%s", damage, e.getClass());
            }

            // 统一应用伤害免疫/配置
            MCH_Lib.applyEntityHurtResistantTimeConfig(e);
            DamageSource ds = DamageSource.setExplosionSource(this);
            damage = MCH_Config.applyDamageVsEntity(e, ds, damage);
            damage = applyTypeMultipliers(e, damage, param);

            // 施加伤害（固定伤害）
            e.attackEntityFrom(ds, damage);

            // 击退与向量登记：随 rDistBox 与遮挡衰减
            final double kb = EnchantmentProtection.func_92092_a(e, attenForKnock);
            if (e instanceof EntityLivingBase) {
                e.motionX += vx * kb * 0.4D;
                e.motionY += vy * kb * 0.1D;
                e.motionZ += vz * kb * 0.4D;
            }
            if (e instanceof EntityPlayer) {
                this.field_77288_k.put(e, W_WorldFunc.getWorldVec3(this.world, vx * attenForKnock, vy * attenForKnock, vz * attenForKnock));
            }

            // 点燃：用 AABB 距离做线性系数，可更贴近“贴脸爆”表现
            if (damage > 0.0F && param.countSetFireEntity > 0) {
                final double fireFactor = 1.0D - rDistBox; // 基于对 AABB 的最小距离
                if (fireFactor > 0.0D) {
                    e.setFire((int) (fireFactor * (double) param.countSetFireEntity));
                }
            }
        }

        // 还原爆炸半径
        this.explosionSize = sizeBefore;
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

            return (float) i / (float) j;
        } else {
            return 0.0D;
        }
    }

    public void doExplosionB(boolean par1) {
        if (param.isPlaySound) {
            W_WorldFunc.DEF_playSoundEffect(this.world, super.explosionX, super.explosionY, super.explosionZ, "random.explode", 4.0F, (1.0F + (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.2F) * 0.7F);
        }

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
                if (l > 0 && destroyBlocksByRule && param.sizeBlock > 0.0F) {
                    if (MCH_Config.Explosion_DestroyBlock.prmBool) {
                        b = W_Block.getBlockById(l);
                        if (b.canDropFromExplosion(this)) {
                            b.dropBlockAsItemWithChance(this.world, i, j, k, this.world.getBlockMetadata(i, j, k), 1.0F / param.sizeBlock, 0);
                        }

                        b.onBlockExploded(this.world, i, j, k, this);
                    }
                }
            }
        }

        if (super.isFlaming) {
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
        return new MCH_Explosion.ExplosionResult(this);
    }

    public class ExplosionResult {

        public boolean hitEntity = false;
        public MCH_Explosion explosion;

        public ExplosionResult(MCH_Explosion explosion) {
            this.explosion = explosion;
        }

    }
}
