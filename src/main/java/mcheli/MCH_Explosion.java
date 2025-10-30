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

        // If smoke effects should be generated
        if (isSmoking) {
            // Iterate through all affected block positions
            Iterator<ChunkPosition> blockPosIterator = affectedBlockPositions.iterator();
            int spawnCount = 0;
            // Determine how many flares can be spawned based on explosion size
            int remainingFlares = (int) Math.max(explosionSize - 1, 10);

            while (blockPosIterator.hasNext()) {
                // Get the current block coordinates
                ChunkPosition pos = blockPosIterator.next();
                int blockX = W_ChunkPosition.getChunkPosX(pos);
                int blockY = W_ChunkPosition.getChunkPosY(pos);
                int blockZ = W_ChunkPosition.getChunkPosZ(pos);
                // Update block ID (return value unused, but may trigger internal logic)
                W_WorldFunc.getBlockId(world, blockX, blockY, blockZ);
                spawnCount++;

                // Generate random particle position within the block
                double randX = blockX + world.rand.nextFloat();
                double randY = blockY + world.rand.nextFloat();
                double randZ = blockZ + world.rand.nextFloat();

                // Compute the vector from the explosion center to the particle
                double dirX = randX - explosionX;
                double dirY = randY - explosionY;
                double dirZ = randZ - explosionZ;

                // Normalize the vector
                double distance = MathHelper.sqrt_double(dirX * dirX + dirY * dirY + dirZ * dirZ);
                dirX /= distance;
                dirY /= distance;
                dirZ /= distance;

                // Compute velocity scaling factor based on distance and explosion size, adding randomness
                double velocityScale = 0.5D / (distance / explosionSize + 0.1D);
                velocityScale *= world.rand.nextFloat() * world.rand.nextFloat() + 0.3F;

                // Apply scaling to get actual movement speed
                dirX *= velocityScale * 0.5D;
                dirY *= velocityScale * 0.5D;
                dirZ *= velocityScale * 0.5D;

                // Compute midpoint between explosion center and random particle location as emission point
                double particleX = (randX + explosionX) / 2.0D;
                double particleY = (randY + explosionY) / 2.0D;
                double particleZ = (randZ + explosionZ) / 2.0D;

                // Random angle for flare and debris direction
                double angle = Math.PI * world.rand.nextInt(360) / 180.0D;

                // For large explosions, generate flares (limited by remainingFlares)
                if (explosionSize >= 6.0F && remainingFlares > 0) {
                    // Compute flare speed coefficient based on explosion size and randomness
                    double flareSpeed = Math.min(explosionSize / 16.0F, 0.6D) *
                        (0.5F + world.rand.nextFloat() * 0.5F);
                    // Create and spawn a flare entity
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

                // Every 4 blocks processed, generate a burst of debris dust particles
                if (spawnCount % 4 == 0) {
                    // Compute dust particle velocity scale based on explosion size
                    float dustVelocity = Math.min(explosionSize / 3.0F, 2.0F) *
                        (0.5F + world.rand.nextFloat() * 0.5F);
                    // Spawn debris dust particles
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

                // Determine particle spawn interval based on explosion size
                int modInterval = (int) (Math.max(explosionSize, 4.0F));
                if (explosionSize <= 1.0F || spawnCount % modInterval == 0) {
                    // Randomly adjust velocity direction for variation
                    if (world.rand.nextBoolean()) {
                        dirY *= 4.0D;
                        dirX *= 0.1D;
                        dirZ *= 0.1D;
                    } else {
                        dirY *= 0.2D;
                        dirX *= 2.0D;
                        dirZ *= 2.0D;
                    }

                    // Create explosion particle parameters
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
                    // Randomize particle color (RGB channels)
                    particle.r = particle.g = particle.b =
                        0.3F + world.rand.nextFloat() * 0.4F;
                    particle.r += 0.1F;
                    particle.g += 0.05F;
                    particle.a = 0.4F + world.rand.nextFloat() * 0.4F;

                    // Randomize particle lifespan
                    particle.age = 5 + world.rand.nextInt(10);
                    particle.age = (int) ((float) particle.age *
                        (Math.max(explosionSize / 2, 6.0F)));
                    particle.gravity = 0.001f;
                    particle.diffusible = true;


                    // Spawn explosion particle
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
     * Certain entities are excluded from result calculations and logging —
     * this maintains consistency with vanilla behavior.
     */
    private static boolean isIgnorableEntity(Entity e) {
        return (e instanceof EntityItem)
            || (e instanceof EntityExpBottle)
            || (e instanceof EntityXPOrb)
            || W_Entity.isEntityFallingBlock(e);
    }

    /**
     * Damage multipliers for different target types —
     * extracted into a dedicated method for easier maintenance.
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
     * Calculates the minimum Euclidean distance from a point to an AABB (Axis-Aligned Bounding Box).
     * Returns 0 if the point lies inside the box.
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
        // === 1) Sample explosion rays to collect affected blocks (used later by doExplosionB for actual processing) ===
        final int RAYS = 16;
        final double STEP = 0.30000001192092896D;
        final Set<ChunkPosition> affected = new HashSet<>();

        for (int xi = 0; xi < RAYS; xi++) {
            for (int yi = 0; yi < RAYS; yi++) {
                for (int zi = 0; zi < RAYS; zi++) {
                    // Only emit rays from the surface of the cube (to avoid redundant directions)
                    if (xi != 0 && xi != RAYS - 1 && yi != 0 && yi != RAYS - 1 && zi != 0 && zi != RAYS - 1) {
                        continue;
                    }

                    // Map [0, RAYS-1] to [-1, 1] to obtain a normalized direction vector
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

                    // March along the ray, attenuating the blast power and recording affected blocks
                    for (float step = 0.3F; blast > 0.0F; blast -= 0.22500001F) {
                        final int bx = MathHelper.floor_double(px);
                        final int by = MathHelper.floor_double(py);
                        final int bz = MathHelper.floor_double(pz);

                        final int blockId = W_WorldFunc.getBlockId(this.world, bx, by, bz);
                        if (blockId > 0) {
                            final Block block = W_WorldFunc.getBlock(this.world, bx, by, bz);
                            float resistance;
                            if (this.exploder != null) {
                                // Compute explosion resistance based on the exploder entity (e.g., missile, TNT, etc.)
                                resistance = W_Entity.getBlockExplosionResistance(this.exploder, this, this.world, bx, by, bz, block);
                            } else {
                                // Fallback: query block’s own resistance
                                resistance = block.getExplosionResistance(this.exploder, this.world, bx, by, bz,
                                    this.explosionX, this.explosionY, this.explosionZ);
                            }
                            // Reduce blast strength more severely in water environments
                            if (param.isInWater) {
                                resistance *= this.world.rand.nextFloat() * 0.2F + 0.2F;
                            }
                            blast -= (resistance + 0.3F) * 0.3F;
                        }

                        // Record block as affected if blast strength remains and the explosion is allowed to affect it
                        if (blast > 0.0F && (this.exploder == null ||
                            W_Entity.shouldExplodeBlock(this.exploder, this, this.world, bx, by, bz, blockId, blast))) {
                            affected.add(new ChunkPosition(bx, by, bz));
                        }

                        // Advance the ray
                        px += dx * STEP;
                        py += dy * STEP;
                        pz += dz * STEP;
                    }
                }
            }
        }
        this.affectedBlockPositions.addAll(affected);

        // === 2) Handle entity damage and knockback ===
        final float sizeBefore = this.explosionSize;
        this.explosionSize *= 2.0F; // Vanilla behavior: double radius for entity search

        // Define the search bounding box around the explosion
        final int minX = MathHelper.floor_double(this.explosionX - this.explosionSize - 1.0D);
        final int maxX = MathHelper.floor_double(this.explosionX + this.explosionSize + 1.0D);
        final int minY = MathHelper.floor_double(this.explosionY - this.explosionSize - 1.0D);
        final int maxY = MathHelper.floor_double(this.explosionY + this.explosionSize + 1.0D);
        final int minZ = MathHelper.floor_double(this.explosionZ - this.explosionSize - 1.0D);
        final int maxZ = MathHelper.floor_double(this.explosionZ + this.explosionSize + 1.0D);

        // Collect all entities within the explosion’s influence area
        final List list = this.world.getEntitiesWithinAABBExcludingEntity(
            this.exploder, W_AxisAlignedBB.getAABB(minX, minY, minZ, maxX, maxY, maxZ));

        // Explosion center as a vector
        final Vec3 center = W_WorldFunc.getWorldVec3(this.world, this.explosionX, this.explosionY, this.explosionZ);

        // Attribute explosion ownership: if triggered by a player, assign to that player
        this.exploder = param.player;

        // Base “point-blank” damage — theoretical damage at rDist=0 with no obstruction (for direct hits)
        final float pointBlankBase = (float) ((int) (8.0D * (double) this.explosionSize + 1.0D));

        // === 2.1 Process indirect entities (excluding direct-hit targets) ===
        for (Object o : list) {
            final Entity e = (Entity) o;

            // Skip direct-hit entity; handled separately below
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

            // Occlusion + distance factor used for knockback vector calculation (keeps vanilla-like behavior)
            double density = this.getBlockDensity(center, e.boundingBox);
            final double attenForKnock = (1.0D - rDist) * density;
            final double attenForDamage = Math.max(0.0D, attenForKnock);

            float damage = (float) ((int) (((attenForDamage * attenForDamage + attenForDamage) / 2.0D) * 8.0D
                * (double) this.explosionSize + 1.0D));

            if (damage > 0.0F && this.result != null && !isIgnorableEntity(e)) {
                this.result.hitEntity = true;
                MCH_Lib.DbgLog(this.world, "MCH_Explosion.doExplosionA:Damage=%.1f:HitEntity=%s", damage, e.getClass());
            }

            // Apply global damage immunity and configuration-based modifiers
            MCH_Lib.applyEntityHurtResistantTimeConfig(e);
            DamageSource ds = DamageSource.setExplosionSource(this);
            damage = MCH_Config.applyDamageVsEntity(e, ds, damage);
            damage = applyTypeMultipliers(e, damage, param);

            // Apply calculated damage to the entity
            e.attackEntityFrom(ds, damage);

            // Apply knockback and record resulting motion vector
            final double kb = EnchantmentProtection.func_92092_a(e, attenForKnock);
            if (e instanceof EntityLivingBase) {
                e.motionX += vx * kb * 0.4D;
                e.motionY += vy * kb * 0.1D;
                e.motionZ += vz * kb * 0.4D;
            }
            if (e instanceof EntityPlayer) {
                this.field_77288_k.put(e, W_WorldFunc.getWorldVec3(this.world, vx * attenForKnock, vy * attenForKnock, vz * attenForKnock));
            }

            // Ignite nearby entities — maintains vanilla-style behavior (scales linearly with distance from explosion center)
            if (damage > 0.0F && param.countSetFireEntity > 0) {
                final double fireFactor = 1.0D - vLen / (double) this.explosionSize;
                if (fireFactor > 0.0D) {
                    e.setFire((int) (fireFactor * (double) param.countSetFireEntity));
                }
            }
        }

        // === 2.2 Direct-hit entity: handled separately with fixed damage ===
        if (param.directAttackEntity != null && !isIgnorableEntity(param.directAttackEntity)) {
            final Entity e = param.directAttackEntity;

            // Use minimum distance from explosion center to AABB to calculate knockback/fire attenuation
            // (damage remains fixed, does not attenuate)
            final double minDistToBox = distancePointToAABB(this.explosionX, this.explosionY, this.explosionZ, e.boundingBox);
            final double rDistBox = Math.min(1.0D, minDistToBox / (double) this.explosionSize);

            // Direction vector from explosion center to target eye height
            double vx = e.posX - this.explosionX;
            double vy = e.posY + (double) e.getEyeHeight() - this.explosionY;
            double vz = e.posZ - this.explosionZ;
            final double vLen = MathHelper.sqrt_double(vx * vx + vy * vy + vz * vz);
            if (vLen != 0.0D) {
                vx /= vLen;
                vy /= vLen;
                vz /= vLen;
            }

            // Knockback and vector registration factor — affected by distance and occlusion
            double density = this.getBlockDensity(center, e.boundingBox);
            final double attenForKnock = (1.0D - rDistBox) * density;

            // === Fixed damage: point-blank damage not reduced by distance or occlusion ===
            float damage = pointBlankBase;

            if (this.result != null) {
                this.result.hitEntity = true;
                MCH_Lib.DbgLog(this.world, "MCH_Explosion.doExplosionA:Damage=%.1f:DirectHit=%s", damage, e.getClass());
            }

            // Apply global immunity and configuration-based modifiers
            MCH_Lib.applyEntityHurtResistantTimeConfig(e);
            DamageSource ds = DamageSource.setExplosionSource(this);
            damage = MCH_Config.applyDamageVsEntity(e, ds, damage);
            damage = applyTypeMultipliers(e, damage, param);

            // Apply fixed damage
            e.attackEntityFrom(ds, damage);

            // Apply knockback and record motion vector (attenuated by rDistBox and occlusion)
            final double kb = EnchantmentProtection.func_92092_a(e, attenForKnock);
            if (e instanceof EntityLivingBase) {
                e.motionX += vx * kb * 0.4D;
                e.motionY += vy * kb * 0.1D;
                e.motionZ += vz * kb * 0.4D;
            }
            if (e instanceof EntityPlayer) {
                this.field_77288_k.put(e, W_WorldFunc.getWorldVec3(this.world, vx * attenForKnock, vy * attenForKnock, vz * attenForKnock));
            }

            // Ignite based on AABB distance factor for more realistic close-range fire behavior
            if (damage > 0.0F && param.countSetFireEntity > 0) {
                final double fireFactor = 1.0D - rDistBox; // derived from minimum AABB distance
                if (fireFactor > 0.0D) {
                    e.setFire((int) (fireFactor * (double) param.countSetFireEntity));
                }
            }
        }

        // Restore original explosion radius
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

            return (double) ((float) i / (float) j);
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
