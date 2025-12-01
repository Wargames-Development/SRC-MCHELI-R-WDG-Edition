package mcheli.weapon;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.*;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntityHitBox;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.aircraft.MCH_PacketNotifyHitBullet;
import mcheli.chain.MCH_EntityChain;
import mcheli.flare.MCH_EntityChaff;
import mcheli.flare.MCH_EntityFlare;
import mcheli.network.packets.PacketLockTarget;
import mcheli.network.packets.PacketPlaySound;
import mcheli.particles.MCH_ParticleParam;
import mcheli.particles.MCH_ParticlesUtil;
import mcheli.vector.Vector3f;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_EntityPlayer;
import mcheli.wrapper.W_MovingObjectPosition;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityCloudFX;
import net.minecraft.client.particle.EntityDiggingFX;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.*;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class MCH_EntityBaseBullet extends W_Entity implements MCH_IChunkLoader {

    public static final int DATAWT_RESERVE1 = 26;
    public static final int DATAWT_TARGET_ENTITY = 27;
    public static final int DATAWT_MARKER_STAT = 28;
    public static final int DATAWT_NAME = 29;
    public static final int DATAWT_BULLET_MODEL = 30;
    public static final int DATAWT_BOMBLET_FLAG = 31;
    public Entity shootingEntity;
    public Entity shootingAircraft;
    public int explosionPower;
    public int explosionPowerInWater;
    public double acceleration;
    public double accelerationFactor;
    public Entity targetEntity;
    public int piercing;
    public int delayFuse;
    public int sprinkleTime;
    public int spawnedBulletNum;
    public byte isBomblet;
    public double prevPosX2;
    public double prevPosY2;
    public double prevPosZ2;
    public double prevMotionX;
    public double prevMotionY;
    public double prevMotionZ;
    public boolean antiFlareUse;
    public int antiFlareTick;
    public int numLockedChaff = 0;
    public int airburstDist = 0;
    public Vec3 initPos;
    boolean doingTopAttack = false;
    boolean speedAddedFromAircraft = false;
    private int countOnUpdate;
    private int power;
    private MCH_WeaponInfo weaponInfo;
    private MCH_BulletModel model;
    private ForgeChunkManager.Ticket chunkLoaderTicket;
    private List<ChunkCoordIntPair> loadedChunks = new ArrayList<>();
    private double airburstTravelled = 0.0D;
    private boolean airburstTriggered = false;

    public MCH_EntityBaseBullet(World par1World) {
        super(par1World);
        this.countOnUpdate = 0;
        this.setSize(1.0F, 1.0F);
        super.prevRotationYaw = super.rotationYaw;
        super.prevRotationPitch = super.rotationPitch;
        this.targetEntity = null;
        this.setPower(1);
        this.acceleration = 1.0D;
        this.accelerationFactor = 1.0D;
        this.piercing = 0;
        this.explosionPower = 0;
        this.explosionPowerInWater = 0;
        this.delayFuse = 0;
        this.sprinkleTime = 0;
        this.isBomblet = -1;
        this.weaponInfo = null;
        super.ignoreFrustumCheck = true;
        if (par1World.isRemote) {
            this.model = null;
        }

    }

    public MCH_EntityBaseBullet(World par1World, double px, double py, double pz, double mx, double my, double mz, float yaw, float pitch, double acceleration) {
        this(par1World);
        this.setSize(1.0F, 1.0F);
        this.setLocationAndAngles(px, py, pz, yaw, pitch);
        this.setPosition(px, py, pz);
        super.prevRotationYaw = yaw;
        super.prevRotationPitch = pitch;
        super.yOffset = 0.0F;
        if (acceleration > 3.9D) {
            acceleration = 3.9D;
        }
        double d = MathHelper.sqrt_double(mx * mx + my * my + mz * mz);
        super.motionX = mx * acceleration / d;
        super.motionY = my * acceleration / d;
        super.motionZ = mz * acceleration / d;
        this.prevMotionX = super.motionX;
        this.prevMotionY = super.motionY;
        this.prevMotionZ = super.motionZ;
        this.acceleration = acceleration;
        this.initPos = Vec3.createVectorHelper(px, py, pz);
    }

    public void init(ForgeChunkManager.Ticket ticket) {
        if (!worldObj.isRemote) {
            if (ticket != null) {
                if (chunkLoaderTicket == null) {
                    chunkLoaderTicket = ticket;
                    chunkLoaderTicket.bindEntity(this);
                    chunkLoaderTicket.getModData();
                }
                ForgeChunkManager.forceChunk(chunkLoaderTicket, new ChunkCoordIntPair(chunkCoordX, chunkCoordZ));
            }
        }
    }

    public void checkAndLoadChunks() {
        int currentChunkX = MathHelper.floor_double(posX) >> 4;
        int currentChunkZ = MathHelper.floor_double(posZ) >> 4;
        loadChunksInBulletPath(currentChunkX, currentChunkZ, motionX, motionZ);
    }

    public void loadNeighboringChunks(int chunkX, int chunkZ) {
        if (!worldObj.isRemote && chunkLoaderTicket != null) {
            for (ChunkCoordIntPair chunk : loadedChunks) {
                ForgeChunkManager.unforceChunk(chunkLoaderTicket, chunk);
            }
            loadedChunks.clear();
            ChunkCoordIntPair[] neighboringChunks = {
                new ChunkCoordIntPair(chunkX, chunkZ),
                new ChunkCoordIntPair(chunkX + 1, chunkZ),
                new ChunkCoordIntPair(chunkX - 1, chunkZ),
                new ChunkCoordIntPair(chunkX, chunkZ + 1),
                new ChunkCoordIntPair(chunkX, chunkZ - 1),
                new ChunkCoordIntPair(chunkX + 1, chunkZ + 1),
                new ChunkCoordIntPair(chunkX - 1, chunkZ - 1),
                new ChunkCoordIntPair(chunkX + 1, chunkZ - 1),
                new ChunkCoordIntPair(chunkX - 1, chunkZ + 1)
            };
            for (ChunkCoordIntPair chunk : neighboringChunks) {
                loadedChunks.add(chunk);
                ForgeChunkManager.forceChunk(chunkLoaderTicket, chunk);
            }
        }
    }

    public void loadChunksInBulletPath(int currentChunkX, int currentChunkZ, double motionX, double motionZ) {
        if (!worldObj.isRemote && chunkLoaderTicket != null) {
            for (ChunkCoordIntPair chunk : loadedChunks) {
                ForgeChunkManager.unforceChunk(chunkLoaderTicket, chunk);
            }
            loadedChunks.clear();
            int nextChunkX = currentChunkX + (motionX > 0 ? 1 : (motionX < 0 ? -1 : 0));
            int nextChunkZ = currentChunkZ + (motionZ > 0 ? 1 : (motionZ < 0 ? -1 : 0));
            ChunkCoordIntPair[] chunksToLoad = {
                new ChunkCoordIntPair(currentChunkX, currentChunkZ),
                new ChunkCoordIntPair(nextChunkX, currentChunkZ),
                new ChunkCoordIntPair(currentChunkX, nextChunkZ),
                new ChunkCoordIntPair(nextChunkX, nextChunkZ)
            };
            for (ChunkCoordIntPair chunk : chunksToLoad) {
                if (!loadedChunks.contains(chunk)) {
                    loadedChunks.add(chunk);
                    ForgeChunkManager.forceChunk(chunkLoaderTicket, chunk);
                }
            }
        }
    }

    private void clearChunkLoaders() {
        for (ChunkCoordIntPair chunk : loadedChunks) {
            ForgeChunkManager.unforceChunk(chunkLoaderTicket, chunk);
        }
    }

    public void setLocationAndAngles(double par1, double par3, double par5, float par7, float par8) {
        super.setLocationAndAngles(par1, par3, par5, par7, par8);
        this.prevPosX2 = par1;
        this.prevPosY2 = par3;
        this.prevPosZ2 = par5;
    }

    protected void entityInit() {
        this.getDataWatcher().addObject(27, 0);
        this.getDataWatcher().addObject(29, "");
        this.getDataWatcher().addObject(30, "");
        this.getDataWatcher().addObject(31, (byte) 0);
    }

    public void setAirburstDist(int airburstDist) {
        this.airburstDist = airburstDist;
    }

    public String getName() {
        return this.getDataWatcher().getWatchableObjectString(29);
    }

    public void setName(String s) {
        if (s != null && !s.isEmpty()) {
            this.weaponInfo = MCH_WeaponInfoManager.get(s);
            if (this.weaponInfo != null) {
                if (!super.worldObj.isRemote) {
                    this.getDataWatcher().updateObject(29, s);
                }
                this.onSetWeaponInfo();
            }
        }

    }

    public MCH_WeaponInfo getInfo() {
        return this.weaponInfo;
    }

    public void onSetWeaponInfo() {
        if (!super.worldObj.isRemote) {
            this.isBomblet = 0;
        }

        if (this.getInfo().bomblet > 0) {
            this.sprinkleTime = this.getInfo().bombletSTime;
        }

        this.piercing = this.getInfo().piercing;
        if (this instanceof MCH_EntityBullet) {
            if (this.getInfo().acceleration > 4.0F) {
                this.accelerationFactor = this.getInfo().acceleration / 4.0F;
            }
        } else if (this instanceof MCH_EntityRocket && this.isBomblet == 0 && this.getInfo().acceleration > 4.0F) {
            this.accelerationFactor = this.getInfo().acceleration / 4.0F;
        }
        if (getInfo() != null && getInfo().enableChunkLoader) {
            init(ForgeChunkManager.requestTicket(MCH_MOD.instance, worldObj, ForgeChunkManager.Type.ENTITY));
        }
    }

    public void setDead() {
        super.setDead();
    }

    public void setBomblet() {
        this.isBomblet = 1;
        this.sprinkleTime = 0;
        super.dataWatcher.updateObject(31, (byte) 1);
    }

    public byte getBomblet() {
        return super.dataWatcher.getWatchableObjectByte(31);
    }

    public void setTargetEntity(Entity entity) {
        this.targetEntity = entity;
        if (!super.worldObj.isRemote) {
            if (entity != null) {
                this.getDataWatcher().updateObject(27, W_Entity.getEntityId(entity));
            } else {
                this.getDataWatcher().updateObject(27, 0);
            }
        }

    }

    public void clientSetTargetEntity(Entity entity) {
        if (super.worldObj.isRemote) {
            this.targetEntity = entity;
            if (entity != null) {
                MCH_MOD.getPacketHandler().sendToServer(new PacketLockTarget(entity.getEntityId(), this.getEntityId()));
            } else {
                MCH_MOD.getPacketHandler().sendToServer(new PacketLockTarget(0, this.getEntityId()));
            }
        }

    }

    public int getTargetEntityID() {
        return this.targetEntity != null ? W_Entity.getEntityId(this.targetEntity) : this.getDataWatcher().getWatchableObjectInt(27);
    }

    public MCH_BulletModel getBulletModel() {
        if (this.getInfo() == null) {
            return null;
        } else if (this.isBomblet < 0) {
            return null;
        } else {
            if (this.model == null) {
                if (this.isBomblet == 1) {
                    this.model = this.getInfo().bombletModel;
                } else {
                    this.model = this.getInfo().bulletModel;
                }

                if (this.model == null) {
                    this.model = this.getDefaultBulletModel();
                }
            }

            return this.model;
        }
    }

    public abstract MCH_BulletModel getDefaultBulletModel();

    public void sprinkleBomblet() {
    }

    public void spawnExplosionParticle(String name, int num, float size) {
        if (super.worldObj.isRemote) {
            if (name.isEmpty() || num < 1 || num > 50) {
                return;
            }

            double x = (super.posX - super.prevPosX) / (double) num;
            double y = (super.posY - super.prevPosY) / (double) num;
            double z = (super.posZ - super.prevPosZ) / (double) num;
            double x2 = (super.prevPosX - this.prevPosX2) / (double) num;
            double y2 = (super.prevPosY - this.prevPosY2) / (double) num;
            double z2 = (super.prevPosZ - this.prevPosZ2) / (double) num;
            int i;
            if (name.equals("explode")) {
                for (i = 0; i < num; ++i) {
                    MCH_ParticleParam prm = new MCH_ParticleParam(super.worldObj, "smoke", (super.prevPosX + x * (double) i + this.prevPosX2 + x2 * (double) i) / 2.0D, (super.prevPosY + y * (double) i + this.prevPosY2 + y2 * (double) i) / 2.0D, (super.prevPosZ + z * (double) i + this.prevPosZ2 + z2 * (double) i) / 2.0D);
                    prm.size = size + super.rand.nextFloat();
                    MCH_ParticlesUtil.spawnParticle(prm);
                }
            } else {
                for (i = 0; i < num; ++i) {
                    MCH_ParticlesUtil.DEF_spawnParticle(name, (super.prevPosX + x * (double) i + this.prevPosX2 + x2 * (double) i) / 2.0D, (super.prevPosY + y * (double) i + this.prevPosY2 + y2 * (double) i) / 2.0D, (super.prevPosZ + z * (double) i + this.prevPosZ2 + z2 * (double) i) / 2.0D, 0.0D, 0.0D, 0.0D, 50.0F);
                }
            }
        }

    }

    public void DEF_spawnParticle(String name, int num, float size) {
        if (super.worldObj.isRemote) {
            if (name.isEmpty() || num < 1 || num > 50) {
                return;
            }

            double x = (super.posX - super.prevPosX) / (double) num;
            double y = (super.posY - super.prevPosY) / (double) num;
            double z = (super.posZ - super.prevPosZ) / (double) num;
            double x2 = (super.prevPosX - this.prevPosX2) / (double) num;
            double y2 = (super.prevPosY - this.prevPosY2) / (double) num;
            double z2 = (super.prevPosZ - this.prevPosZ2) / (double) num;

            for (int i = 0; i < num; ++i) {
                MCH_ParticlesUtil.DEF_spawnParticle(name, (super.prevPosX + x * (double) i + this.prevPosX2 + x2 * (double) i) / 2.0D, (super.prevPosY + y * (double) i + this.prevPosY2 + y2 * (double) i) / 2.0D, (super.prevPosZ + z * (double) i + this.prevPosZ2 + z2 * (double) i) / 2.0D, 0.0D, 0.0D, 0.0D, 150.0F);
            }
        }

    }

    public int getCountOnUpdate() {
        return this.countOnUpdate;
    }

    public void clearCountOnUpdate() {
        this.countOnUpdate = 0;
    }

    @SideOnly(Side.CLIENT)
    public boolean isInRangeToRenderDist(double par1) {
        double d1 = super.boundingBox.getAverageEdgeLength() * 4.0D;
        d1 *= 64.0D;
        return par1 < d1 * d1;
    }

    public void setParameterFromWeapon(MCH_WeaponBase w, Entity entity, Entity user) {
        this.explosionPower = w.explosionPower;
        this.explosionPowerInWater = w.explosionPowerInWater;
        this.setPower(w.power);
        this.piercing = w.piercing;
        this.shootingAircraft = entity;
        this.shootingEntity = user;
    }

    public void setParameterFromWeapon(Entity entity, Entity user) {
        this.shootingAircraft = entity;
        this.shootingEntity = user;
    }

    public void setParameterFromWeapon(MCH_EntityBaseBullet b, Entity entity, Entity user) {
        this.explosionPower = b.explosionPower;
        this.explosionPowerInWater = b.explosionPowerInWater;
        this.setPower(b.getPower());
        this.piercing = b.piercing;
        this.shootingAircraft = entity;
        this.shootingEntity = user;
    }

    public void guidanceToPos(double targetPosX, double targetPosY, double targetPosZ) {

        if (getInfo().tickEndHoming > 0 && ticksExisted > getInfo().tickEndHoming) {
            return;
        }

        double tx = targetPosX - this.posX;
        double ty = targetPosY - this.posY;
        double tz = targetPosZ - this.posZ;

        double d = MathHelper.sqrt_double(tx * tx + ty * ty + tz * tz);
        if (d < 1.0E-6D) {
            return;
        }

        double accel = this.acceleration;
        double mx = tx * accel / d;
        double my = ty * accel / d;
        double mz = tz * accel / d;

        Vector3f missileDirection = new Vector3f(this.motionX, this.motionY, this.motionZ);
        Vector3f targetDirection = new Vector3f(tx, ty, tz);
        double angle = Math.abs(Vector3f.angle(missileDirection, targetDirection));
        double maxAllowedAngle = Math.toRadians(getInfo().maxDegreeOfMissile);
        if (angle > maxAllowedAngle) {
            return;
        }
        double turning = getInfo().turningFactor;
        this.motionX = this.motionX + (mx - this.motionX) * turning;
        this.motionY = this.motionY + (my - this.motionY) * turning;
        this.motionZ = this.motionZ + (mz - this.motionZ) * turning;
        double a = Math.atan2(this.motionZ, this.motionX);
        this.rotationYaw = (float) (a * 180.0D / Math.PI) - 90.0F;
        double r = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
        this.rotationPitch = -((float) (Math.atan2(this.motionY, r) * 180.0D / Math.PI));
    }


    public void guidanceToTarget(double targetPosX, double targetPosY, double targetPosZ) {
        this.guidanceToTarget(targetPosX, targetPosY, targetPosZ, 1.0F);
    }

    public void guidanceToTarget(double targetPosX, double targetPosY, double targetPosZ, float accelerationFactor) {

        // 如果有结束寻的时间（tickEndHoming > 0）且当前已超过这个时间，停止寻的
        if (getInfo().tickEndHoming > 0 && ticksExisted > getInfo().tickEndHoming) {
            return;
        }

        // 先判断是否存在有效的目标实体
        if (targetEntity == null || targetEntity.isDead) {
            return;
        }

        //----------------------------------------------------------------------------
        // 1. 如果需要预测目标位置，则根据目标当前速度做一个简单的预测
        //----------------------------------------------------------------------------
        if (getInfo().predictTargetPos) {
            // 当前导弹到目标的距离（用来粗略估计“到达时间”）
            double currentDistance = MathHelper.sqrt_double(
                (targetPosX - posX) * (targetPosX - posX)
                    + (targetPosY - posY) * (targetPosY - posY)
                    + (targetPosZ - posZ) * (targetPosZ - posZ)
            );

            // 当前导弹的速度（也可以根据历史帧或更复杂的模型来求平均速度）
            double missileSpeed = MathHelper.sqrt_double(
                motionX * motionX + motionY * motionY + motionZ * motionZ
            );
            // 避免分母为0
            if (missileSpeed < 0.0001D) {
                missileSpeed = this.acceleration;
                // 或者用一个固定值替代，比如武器配置里的初速度
            }

            // 粗略估算导弹到达目标的时间
            double timeToTarget = currentDistance / missileSpeed;

            // 根据目标速度，来对目标位置做一个简单的线性预测
            double vx = targetEntity.motionX;
            double vy = targetEntity.motionY;
            double vz = targetEntity.motionZ;

            // 这里你也可以考虑加速度、重力等更复杂的因子
            targetPosX += vx * timeToTarget;
            targetPosY += vy * timeToTarget;
            targetPosZ += vz * timeToTarget;
        }
        //----------------------------------------------------------------------------
        // 2. 计算目标位置与当前实体位置之间的差值
        //----------------------------------------------------------------------------
        double tx = targetPosX - this.posX;
        double ty = targetPosY - this.posY;
        double tz = targetPosZ - this.posZ;

        // 计算与目标的距离
        double d = MathHelper.sqrt_double(tx * tx + ty * ty + tz * tz);

        //----------------------------------------------------------------------------
        // 3. 根据距离和预设加速度计算导弹运动矢量分量
        //----------------------------------------------------------------------------
        double mx = tx * this.acceleration / d;
        double my = ty * this.acceleration / d;
        double mz = tz * this.acceleration / d;

        // 计算导弹当前运动方向（水平分量）
        Vector3f missileDirection = new Vector3f(this.motionX, this.motionY, this.motionZ);
        // 计算目标方向（在水平面上）
        Vector3f targetDirection = new Vector3f(tx, ty, tz);

        // 计算导弹运动方向与目标方向之间的夹角
        double angle = Math.abs(Vector3f.angle(missileDirection, targetDirection));
        double maxAllowedAngle = Math.toRadians(getInfo().maxDegreeOfMissile);

        // 如果角度超过允许值，且不是顶攻模式（doingTopAttack），则解除锁定
        if (angle > maxAllowedAngle && !doingTopAttack) {
            setTargetEntity(null);
            return;
        }

        //----------------------------------------------------------------------------
        // 4. 判断目标运动方向与导弹方向的夹角是否大于PD系统的允许阈值
        //----------------------------------------------------------------------------
        Vector3f targetVelocity = new Vector3f(
            (float) targetEntity.motionX,
            (float) targetEntity.motionY,
            (float) targetEntity.motionZ
        );
        double velocityAngle = Math.abs(Vector3f.angle(missileDirection, targetVelocity));
        if (velocityAngle > getInfo().pdHDNMaxDegree) {
            setTargetEntity(null);
            return;
        }

        // 如果是某些类型的空空导弹，且目标在地面上，则解锁
        if (this instanceof MCH_EntityAAMissile
            && MCH_WeaponGuidanceSystem.isEntityOnGround(targetEntity, weaponInfo.lockMinHeight)) {
            setTargetEntity(null);
            return;
        }

        //----------------------------------------------------------------------------
        // 5. 使用平滑加权平均值来更新当前实体的运动速度
        //----------------------------------------------------------------------------
        this.motionX = this.motionX + (mx - this.motionX) * getInfo().turningFactor;
        this.motionY = this.motionY + (my - this.motionY) * getInfo().turningFactor;
        this.motionZ = this.motionZ + (mz - this.motionZ) * getInfo().turningFactor;

        //----------------------------------------------------------------------------
        // 6. 更新导弹朝向（Yaw/Pitch）
        //----------------------------------------------------------------------------
        // Yaw：根据XZ平面速度方向计算
        double a = Math.atan2(this.motionZ, this.motionX);
        this.rotationYaw = (float) (a * 180.0D / Math.PI) - 90.0F;

        // Pitch：根据Y方向与在XZ平面上的水平速度大小计算
        double r = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
        this.rotationPitch = -((float) (Math.atan2(this.motionY, r) * 180.0D / Math.PI));
    }


    public boolean checkValid() {
        if (this.shootingEntity == null && this.shootingAircraft == null) {
            return false;
        } else if (this.shootingEntity != null && this.shootingEntity.isDead) {
            return false;
        } else {
            if (this.shootingAircraft != null && this.shootingAircraft.isDead) {
                ;
            }

            Entity shooter = this.shootingEntity != null ? this.shootingEntity : this.shootingAircraft;
            double x = super.posX - shooter.posX;
            double z = super.posZ - shooter.posZ;
            return x * x + z * z < 3.38724E7D;
        }
    }

    public float getGravity() {
        return this.getInfo() != null ? this.getInfo().gravity : 0.0F;
    }

    public float getGravityInWater() {
        return this.getInfo() != null ? this.getInfo().gravityInWater : 0.0F;
    }

    public void onUpdate() {

        if (!worldObj.isRemote) {
            if (shootingAircraft instanceof MCH_EntityAircraft && !speedAddedFromAircraft && getInfo().speedDependsAircraft) {
                MCH_EntityAircraft ac = (MCH_EntityAircraft) shootingAircraft;
                double s = Math.sqrt(ac.motionX * ac.motionX + ac.motionY * ac.motionY + ac.motionZ * ac.motionZ);
                acceleration += s;
                double d = MathHelper.sqrt_double(motionX * motionX + motionY * motionY + motionZ * motionZ);
                super.motionX = motionX * acceleration / d;
                super.motionY = motionY * acceleration / d;
                super.motionZ = motionZ * acceleration / d;
                speedAddedFromAircraft = true;
            }
        }

        if (getInfo() != null && getInfo().enableChunkLoader) {
            checkAndLoadChunks();
        }

        //更新锁定的目标
        if (super.worldObj.isRemote && this.countOnUpdate == 0) {
            int f3 = this.getTargetEntityID();
            if (f3 > 0) {
                this.setTargetEntity(super.worldObj.getEntityByID(f3));
            }
        }

        //抗干扰
        if (!worldObj.isRemote && antiFlareUse) {
            if (antiFlareTick > 0) {
                antiFlareTick--;
            } else {
                setTargetEntity(null);
                antiFlareUse = false;
            }
        }


        if (this.prevMotionX != super.motionX || this.prevMotionY != super.motionY || this.prevMotionZ != super.motionZ) {
            double var5 = (double) ((float) Math.atan2(super.motionZ, super.motionX));
            super.rotationYaw = (float) (var5 * 180.0D / 3.141592653589793D) - 90.0F;
            double r = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
            super.rotationPitch = -((float) (Math.atan2(super.motionY, r) * 180.0D / 3.141592653589793D));
        }

        this.prevMotionX = super.motionX;
        this.prevMotionY = super.motionY;
        this.prevMotionZ = super.motionZ;
        ++this.countOnUpdate;
        if (this.countOnUpdate > 10000000) {
            this.clearCountOnUpdate();
        }

        this.prevPosX2 = super.prevPosX;
        this.prevPosY2 = super.prevPosY;
        this.prevPosZ2 = super.prevPosZ;
        super.onUpdate();
        if (this.getInfo() == null) {
            if (this.countOnUpdate >= 2) {
                MCH_Lib.Log((Entity) this, "##### MCH_EntityBaseBullet onUpdate() Weapon info null %d, %s, Name=%s", new Object[]{Integer.valueOf(W_Entity.getEntityId(this)), this.getEntityName(), this.getName()});
                this.setDead();
                return;
            }

            this.setName(this.getName());
            if (this.getInfo() == null) {
                return;
            }
        }

        if (super.worldObj.isRemote && this.isBomblet < 0) {
            this.isBomblet = this.getBomblet();
        }

        if (!super.worldObj.isRemote) {
            if ((int) super.posY <= 255 && !super.worldObj.blockExists((int) super.posX, (int) super.posY, (int) super.posZ)) {
                if (this.getInfo().delayFuse <= 0) {
                    this.setDead();
                    return;
                }

                if (this.delayFuse == 0) {
                    this.delayFuse = this.getInfo().delayFuse;
                }
            }

            if (this.delayFuse > 0) {
                --this.delayFuse;
                if (this.delayFuse == 0) {
                    this.onUpdateTimeout();
                    this.setDead();
                    return;
                }
            }

            if (!this.checkValid()) {
                this.setDead();
                return;
            }

            if (this.getInfo().timeFuse > 0 && this.getCountOnUpdate() > this.getInfo().timeFuse) {
                this.onUpdateTimeout();
                this.setDead();
                return;
            }

            if (this.getInfo().explosionAltitude > 0 && MCH_Lib.getBlockIdY(this, 3, -this.getInfo().explosionAltitude) != 0) {
                MovingObjectPosition var6 = new MovingObjectPosition((int) super.posX, (int) super.posY, (int) super.posZ, 0, Vec3.createVectorHelper(super.posX, super.posY, super.posZ));
                this.onImpact(var6, 1.0F);
            }
        }

        if (!this.isInWater()) {

            double currentSpeed = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
            if (currentSpeed == 0) currentSpeed = 0.000001F;

            double dirX = motionX / currentSpeed;
            double dirY = motionY / currentSpeed;
            double dirZ = motionZ / currentSpeed;

            if (ticksExisted > getInfo().speedFactorStartTick
                && ticksExisted < getInfo().speedFactorEndTick) {
                super.motionX += dirX * getInfo().speedFactor;
                super.motionY += dirY * getInfo().speedFactor;
                super.motionZ += dirZ * getInfo().speedFactor;
                acceleration += getInfo().speedFactor;
            }

            super.motionY += this.getGravity();
            super.motionX -= dirX * getInfo().dragInAir;
            super.motionZ -= dirZ * getInfo().dragInAir;
        } else {
            super.motionY += this.getGravityInWater();
        }


        if (!super.isDead) {
            onUpdateCollided();
            onUpdateAirburst();
        }

        super.posX += super.motionX * this.accelerationFactor;
        super.posY += super.motionY * this.accelerationFactor;
        super.posZ += super.motionZ * this.accelerationFactor;
        if (super.worldObj.isRemote) {
            this.updateSplash();
        }

        if (this.isInWater()) {
            float var7 = 0.25F;
            super.worldObj.spawnParticle("bubble", super.posX - super.motionX * (double) var7, super.posY - super.motionY * (double) var7, super.posZ - super.motionZ * (double) var7, super.motionX, super.motionY, super.motionZ);
        }

        this.setPosition(super.posX, super.posY, super.posZ);

        onUpdateSpreader();
    }

    private void onUpdateAirburst() {

        int abDist = this.airburstDist;
        if (this.airburstTriggered || abDist <= 5 || abDist >= 300) {
            return;
        }

        double targetDist = (double) abDist + 3.0D;

        double dx = this.motionX * this.accelerationFactor;
        double dy = this.motionY * this.accelerationFactor;
        double dz = this.motionZ * this.accelerationFactor;
        double segLen = Math.sqrt(dx * dx + dy * dy + dz * dz);

        double newTravel = this.airburstTravelled + segLen;

        if (segLen > 0.0D && newTravel >= targetDist) {
            double remain = targetDist - this.airburstTravelled;
            double t = remain / segLen;
            double ex = this.posX + dx * t;
            double ey = this.posY + dy * t;
            double ez = this.posZ + dz * t;

            if (!this.worldObj.isRemote) {
                if (this.getInfo().explosion > 0) {
                    this.newExplosion(ex, ey, ez, getInfo().explosionAirburst,
                        (float) this.getInfo().explosionBlock, false);
                } else if (this.explosionPower < 0) {
                    this.playExplosionSound();
                }

                if (this.getInfo() != null && this.getInfo().enableChunkLoader) {
                    this.clearChunkLoaders();
                }

                if (this.getInfo() != null) {
                    PacketPlaySound.sendSoundPacket(
                        ex, ey, ez, this.getInfo().hitSoundRange, this.dimension,
                        this.getInfo().hitSound, true);
                }

                this.setDead();
            }

            this.airburstTriggered = true;
            this.airburstTravelled = 0.0D;
        } else {
            this.airburstTravelled = newTravel;
        }
    }

    public void updateSplash() {
        if (this.getInfo() != null) {
            if (this.getInfo().power > 0) {
                if (!W_WorldFunc.isBlockWater(super.worldObj, (int) (super.prevPosX + 0.5D), (int) (super.prevPosY + 0.5D), (int) (super.prevPosZ + 0.5D)) && W_WorldFunc.isBlockWater(super.worldObj, (int) (super.posX + 0.5D), (int) (super.posY + 0.5D), (int) (super.posZ + 0.5D))) {
                    double x = super.posX - super.prevPosX;
                    double y = super.posY - super.prevPosY;
                    double z = super.posZ - super.prevPosZ;
                    double d = Math.sqrt(x * x + y * y + z * z);
                    if (d <= 0.15D) {
                        return;
                    }

                    x /= d;
                    y /= d;
                    z /= d;
                    double px = super.prevPosX;
                    double py = super.prevPosY;
                    double pz = super.prevPosZ;

                    for (int i = 0; (double) i <= d; ++i) {
                        px += x;
                        py += y;
                        pz += z;
                        if (W_WorldFunc.isBlockWater(super.worldObj, (int) (px + 0.5D), (int) (py + 0.5D), (int) (pz + 0.5D))) {
                            float pwr = this.getInfo().power < 20 ? (float) this.getInfo().power : 20.0F;
                            int n = super.rand.nextInt(1 + (int) pwr / 3) + (int) pwr / 2 + 1;
                            pwr *= 0.03F;

                            for (int j = 0; j < n; ++j) {
                                MCH_ParticleParam prm = new MCH_ParticleParam(super.worldObj, "splash", px, py + 0.5D, pz, (double) pwr * (super.rand.nextDouble() - 0.5D) * 0.3D, (double) pwr * (super.rand.nextDouble() * 0.5D + 0.5D) * 1.8D, (double) pwr * (super.rand.nextDouble() - 0.5D) * 0.3D, pwr * 5.0F);
                                MCH_ParticlesUtil.spawnParticle(prm);
                            }

                            return;
                        }
                    }
                }

            }
        }
    }

    public void onUpdateTimeout() {
        if (this.isInWater()) {
            if (this.explosionPowerInWater > 0) {
                this.newExplosion(super.posX, super.posY, super.posZ, (float) this.explosionPowerInWater, (float) this.explosionPowerInWater, true);
            }
        } else if (this.explosionPower > 0) {
            this.newExplosion(super.posX, super.posY, super.posZ, (float) this.explosionPower, (float) this.getInfo().explosionBlock, false);
        } else if (this.explosionPower < 0) {
            this.playExplosionSound();
        }

    }

    public void onUpdateBomblet() {
        if (!super.worldObj.isRemote && this.sprinkleTime > 0 && !super.isDead) {
            --this.sprinkleTime;
            if (this.sprinkleTime == 0) {
                for (int i = 0; i < this.getInfo().bomblet; ++i) {
                    this.sprinkleBomblet();
                }

                this.setDead();
            }
        }

    }

    public void boundBullet(int sideHit) {
        switch (sideHit) {
            case 0:
                if (super.motionY > 0.0D) {
                    super.motionY = -super.motionY * (double) this.getInfo().bound;
                }
                break;
            case 1:
                if (super.motionY < 0.0D) {
                    super.motionY = -super.motionY * (double) this.getInfo().bound;
                }
                break;
            case 2:
                if (super.motionZ > 0.0D) {
                    super.motionZ = -super.motionZ * (double) this.getInfo().bound;
                } else {
                    super.posZ += super.motionZ;
                }
                break;
            case 3:
                if (super.motionZ < 0.0D) {
                    super.motionZ = -super.motionZ * (double) this.getInfo().bound;
                } else {
                    super.posZ += super.motionZ;
                }
                break;
            case 4:
                if (super.motionX > 0.0D) {
                    super.motionX = -super.motionX * (double) this.getInfo().bound;
                } else {
                    super.posX += super.motionX;
                }
                break;
            case 5:
                if (super.motionX < 0.0D) {
                    super.motionX = -super.motionX * (double) this.getInfo().bound;
                } else {
                    super.posX += super.motionX;
                }
        }

    }

    protected void onUpdateCollided() {
        float damageFactor = 1.0F;
        double mx = super.motionX * this.accelerationFactor;
        double my = super.motionY * this.accelerationFactor;
        double mz = super.motionZ * this.accelerationFactor;
        MovingObjectPosition m = null;

        Vec3 src;
        Vec3 dir;
        for (int entity = 0; entity < 5; ++entity) {
            src = W_WorldFunc.getWorldVec3(super.worldObj, super.posX, super.posY, super.posZ);
            dir = W_WorldFunc.getWorldVec3(super.worldObj, super.posX + mx, super.posY + my, super.posZ + mz);
            m = W_WorldFunc.clip(super.worldObj, src, dir);
            boolean list = false;
            if (this.shootingEntity != null && W_MovingObjectPosition.isHitTypeTile(m)) {
                Block d0 = W_WorldFunc.getBlock(super.worldObj, m.blockX, m.blockY, m.blockZ);
                if (MCH_Config.bulletBreakableBlocks.contains(d0)) {
                    W_WorldFunc.destroyBlock(super.worldObj, m.blockX, m.blockY, m.blockZ, true);
                    list = true;
                }
            }

            if (!list) {
                break;
            }
        }

        src = W_WorldFunc.getWorldVec3(super.worldObj, super.posX, super.posY, super.posZ);
        dir = W_WorldFunc.getWorldVec3(super.worldObj, super.posX + mx, super.posY + my, super.posZ + mz);
        if (this.getInfo().delayFuse > 0) {
            if (m != null) {
                this.boundBullet(m.sideHit);
                if (this.delayFuse == 0) {
                    this.delayFuse = this.getInfo().delayFuse;
                }
            }

        } else {
            if (m != null) {
                dir = W_WorldFunc.getWorldVec3(super.worldObj, m.hitVec.xCoord, m.hitVec.yCoord, m.hitVec.zCoord);
            }

            Entity hitEntity = null;
            List entities = super.worldObj.getEntitiesWithinAABBExcludingEntity(this, super.boundingBox.addCoord(mx, my, mz).expand(21.0D, 21.0D, 21.0D));
            double d2 = 0.0D;
            MovingObjectPosition result = m;
            for (Object o : entities) {
                Entity entity = (Entity) o;
                if (this.canBeCollidedEntity(entity) && shootingAircraft != o) {
                    float f = 0.3F;
                    MovingObjectPosition movingObjectPosition = entity.boundingBox.expand(f, f, f).calculateIntercept(src, dir);
                    if (movingObjectPosition != null) {
                        double d1 = src.distanceTo(movingObjectPosition.hitVec);
                        if (d1 < d2 || d2 == 0.0D) {
                            hitEntity = entity;
                            d2 = d1;
                            result = movingObjectPosition;
                        }
                    }
                }
            }

            if (result != null) {
                dir = Vec3.createVectorHelper(result.hitVec.xCoord - this.posX, result.hitVec.yCoord - this.posY, result.hitVec.zCoord - this.posZ);
                double d = 1.0;
                if (mx != 0.0) {
                    d = dir.xCoord / mx;
                } else if (my != 0.0) {
                    d = dir.yCoord / my;
                } else if (mz != 0.0) {
                    d = dir.zCoord / mz;
                }
                if (d < 0.0) {
                    d = -d;
                }

                Vec3 newHitVec = Vec3.createVectorHelper(posX + mx * d, posY + my * d, posZ + mz * d);
                if (hitEntity != null) {
                    this.onImpact(new MovingObjectPosition(hitEntity, newHitVec), damageFactor);
                } else {
                    this.onImpact(result, damageFactor);
                }
            }
        }
    }

    public boolean canBeCollidedEntity(Entity entity) {
        if (entity instanceof MCH_EntityChain) {
            return false;
        } else if (!entity.canBeCollidedWith()) {
            return false;
        } else {
            if (entity instanceof MCH_EntityBaseBullet) {
                if (super.worldObj.isRemote) {
                    return false;
                }

                MCH_EntityBaseBullet i$ = (MCH_EntityBaseBullet) entity;
                if (W_Entity.isEqual(i$.shootingAircraft, this.shootingAircraft)) {
                    return false;
                }

                if (W_Entity.isEqual(i$.shootingEntity, this.shootingEntity)) {
                    return false;
                }
            }

            if (entity instanceof MCH_EntitySeat) {
                return false;
            } else if (entity instanceof MCH_EntityHitBox) {
                return false;
            } else if (W_Entity.isEqual(entity, this.shootingEntity)) {
                return false;
            } else {
                if (this.shootingAircraft instanceof MCH_EntityAircraft) {
                    if (W_Entity.isEqual(entity, this.shootingAircraft)) {
                        return false;
                    }

                    if (((MCH_EntityAircraft) this.shootingAircraft).isMountedEntity(entity)) {
                        return false;
                    }
                }

                MCH_Config var10000 = MCH_MOD.config;
                Iterator i$1 = MCH_Config.IgnoreBulletHitList.iterator();

                String s;
                do {
                    if (!i$1.hasNext()) {
                        return true;
                    }

                    s = (String) i$1.next();
                } while (!entity.getClass().getName().toLowerCase().contains(s.toLowerCase()));

                return false;
            }
        }
    }

    public void notifyHitBullet() {
        if (this.shootingAircraft instanceof MCH_EntityAircraft && W_EntityPlayer.isPlayer(this.shootingEntity)) {
            MCH_PacketNotifyHitBullet.send((MCH_EntityAircraft) this.shootingAircraft, (EntityPlayer) this.shootingEntity);
        }

        if (W_EntityPlayer.isPlayer(this.shootingEntity)) {
            MCH_PacketNotifyHitBullet.send(null, (EntityPlayer) this.shootingEntity);
        }

    }

    protected void onImpact(MovingObjectPosition m, float damageFactor) {
        float p;
        double hitX = 0;
        double hitY = 0;
        double hitZ = 0;
        double dx = 0.00001D;
        double dy = 0.00001D;
        double dz = 0.00001D;
        if (!super.worldObj.isRemote) {
            if (m.entityHit != null) {
                if (m.entityHit instanceof MCH_EntityBaseBullet && !this.getInfo().canBeIntercepted) {
                    return;
                }
                if (m.entityHit instanceof MCH_EntityFlare || m.entityHit instanceof MCH_EntityChaff) {
                    return;
                }

                Vec3 hitVec = Vec3.createVectorHelper(m.hitVec.xCoord, m.hitVec.yCoord, m.hitVec.zCoord);
                if (weaponInfo != null && weaponInfo.enableBulletDecay && initPos != null) {
                    float decayFactor = 1f;
                    float dist = (float) initPos.distanceTo(hitVec);
                    for (MCH_IBulletDecay decay : weaponInfo.bulletDecay) {
                        decayFactor = decay.calculateDecayFactor(dist);
                    }
                    damageFactor *= decayFactor;
                }
                //药水效果
                List<EntityLivingBase> livingList = new ArrayList<>();
                if (m.entityHit instanceof EntityLivingBase) {
                    livingList.add((EntityLivingBase) m.entityHit);
                }
                if (m.entityHit instanceof MCH_EntityAircraft) {
                    MCH_EntityAircraft ac = (MCH_EntityAircraft) m.entityHit;
                    if (ac.riddenByEntity instanceof EntityLivingBase) {
                        livingList.add((EntityLivingBase) ac.riddenByEntity);
                    }
                    if (ac.getSeats() != null) {
                        for (MCH_EntitySeat seat : ac.getSeats()) {
                            if (seat != null && seat.riddenByEntity instanceof EntityLivingBase) {
                                livingList.add((EntityLivingBase) seat.riddenByEntity);
                            }
                        }
                    }
                }
                for (EntityLivingBase livingBase : livingList) {
                    float dist = 0;
                    if (initPos != null) {
                        dist = (float) initPos.distanceTo(hitVec);
                    }
                    for (MCH_PotionEffect effect : getInfo().potionEffect) {
                        if ((effect.startDist < 0 && effect.endDist < 0)
                            || (effect.startDist <= dist && dist < effect.endDist)) {
                            livingBase.addPotionEffect(new PotionEffect(effect.potionEffect));
                        }
                    }
                }
                this.onImpactEntity(m.entityHit, damageFactor, hitVec);
                this.piercing--;
                hitX = m.hitVec.xCoord + dx;
                hitY = m.hitVec.yCoord + dy;
                hitZ = m.hitVec.zCoord + dz;
            }

            if (m.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                Block d0 = W_WorldFunc.getBlock(super.worldObj, m.blockX, m.blockY, m.blockZ);
                Material mat = d0.getMaterial();
                if (mat == Material.leaves || mat == Material.plants || d0 == Blocks.iron_bars || d0 instanceof BlockDoublePlant) {
                    return;
                }
                if (d0 instanceof BlockGlass || d0 instanceof BlockStainedGlass || d0 instanceof BlockPane || d0 instanceof BlockStainedGlassPane) {
                    return;
                }
                hitX = m.hitVec.xCoord + dx;
                hitY = m.hitVec.yCoord + dy;
                hitZ = m.hitVec.zCoord + dz;
            }
            p = (float) this.explosionPower * damageFactor;
            float i = (float) this.explosionPowerInWater * damageFactor;
            if (this.piercing > 0) {
                --this.piercing;
                if (p > 0.0F) {
                    this.newExplosion(hitX, hitY, hitZ, 1.0F, 1.0F, false);
                }
            } else {
                if (i == 0.0F) {
                    if (this.getInfo().isFAE) {
                        this.newFAExplosion(super.posX, super.posY, super.posZ, p, (float) this.getInfo().explosionBlock);
                    } else if (p > 0.0F) {
                        this.newExplosion(hitX, hitY, hitZ, p, (float) this.getInfo().explosionBlock, false, m.entityHit);
                    } else if (p < 0.0F) {
                        this.playExplosionSound();
                    }
                } else if (m.entityHit != null) {
                    if (this.isInWater()) {
                        this.newExplosion(hitX, hitY, hitZ, i, i, true, m.entityHit);
                    } else {
                        this.newExplosion(hitX, hitY, hitZ, p, (float) this.getInfo().explosionBlock, false, m.entityHit);
                    }
                } else if (!this.isInWater() && !MCH_Lib.isBlockInWater(super.worldObj, m.blockX, m.blockY, m.blockZ)) {
                    if (p > 0.0F) {
                        this.newExplosion(hitX, hitY, hitZ, p, (float) this.getInfo().explosionBlock, false, m.entityHit);
                    } else if (p < 0.0F) {
                        this.playExplosionSound();
                    }
                } else {
                    this.newExplosion(m.blockX, m.blockY, m.blockZ, i, i, true);
                }

                if (getInfo() != null && getInfo().enableChunkLoader) {
                    clearChunkLoaders();
                }

                if (getInfo() != null) {
                    PacketPlaySound.sendSoundPacket(posX, posY, posZ, getInfo().hitSoundRange, dimension, getInfo().hitSound, true);
                }

                this.setDead();
            }
        } else if (this.getInfo() != null) {
//            p = (float)this.getInfo().power;
//            for(int var11 = 0; (float)var11 < p / 3.0F; ++var11) {
//                MCH_ParticlesUtil.spawnParticleTileCrack(super.worldObj,
//                        m.blockX, m.blockY, m.blockZ,
//                        m.hitVec.xCoord + ((double)super.rand.nextFloat() - 0.5D) * (double)p / 10.0D,
//                        m.hitVec.yCoord + 0.1D,
//                        m.hitVec.zCoord + ((double)super.rand.nextFloat() - 0.5D) * (double)p / 10.0D,
//                        -super.motionX * (double)p / 2.0D, (double)(p / 2.0F), -super.motionZ * (double)p / 2.0D);
//            }
            if (m.entityHit == null) {
                spawnBlockPar(m, m.blockX, m.blockY, m.blockZ);
            }
//            if (m.entityHit == null) {
//                worldObj.spawnEntityInWorld(new EntityDebugDot(worldObj, new com.flansmod.common.vector.Vector3f(m.hitVec.xCoord, m.hitVec.yCoord, m.hitVec.zCoord), 100, 1F, 0F, 0F));
//            } else {
//                worldObj.spawnEntityInWorld(new EntityDebugDot(worldObj, new com.flansmod.common.vector.Vector3f(m.hitVec.xCoord, m.hitVec.yCoord, m.hitVec.zCoord), 100, 0F, 1F, 0F));
//            }

            if (m.entityHit instanceof MCH_EntityAircraft) {
                MCH_EntityAircraft ac = (MCH_EntityAircraft) m.entityHit;
                if (ac.ironCurtainRunningTick > 0) {
                    spawnIronCurtainParticle(m, m.blockX, m.blockY, m.blockZ);
                }
            }
        }

    }

    @SideOnly(Side.CLIENT)
    public void spawnIronCurtainParticle(MovingObjectPosition raytraceResult, int xTile, int yTile, int zTile) {
        // 定义暗红色参数（RGB：0.5, 0.1, 0.1）
        final float DARK_RED_R = 0.5f;
        final float DARK_RED_G = 0.1f;
        final float DARK_RED_B = 0.1f;

        int num = getInfo().flakParticlesCrack + rand.nextInt(3);
        float scale = 1.0F;
        for (int i = 0; i < num; i++) {
            EntityDiggingFX fx = new EntityDiggingFX(
                this.worldObj,
                raytraceResult.hitVec.xCoord + (rand.nextFloat() - 0.5D) * width,
                raytraceResult.hitVec.yCoord + 0.1D,
                raytraceResult.hitVec.zCoord + (rand.nextFloat() - 0.5D) * width,
                0, 0, 0,
                worldObj.getBlock(xTile, yTile, zTile),
                this.worldObj.getBlockMetadata(xTile, yTile, zTile)
            );

            // 覆盖原有颜色设置
            fx.setRBGColorF(DARK_RED_R, DARK_RED_G, DARK_RED_B); // 强制设置为暗红色
            fx.multipleParticleScaleBy(scale * 0.8f); // 适当缩小粒子尺寸

            // 调整运动参数
            fx.motionX += getInfo().flakParticlesDiff * (rand.nextGaussian() * 0.5);
            fx.motionZ += getInfo().flakParticlesDiff * (rand.nextGaussian() * 0.5);
            fx.motionY += getInfo().flakParticlesDiff * Math.abs(rand.nextGaussian());

            Minecraft.getMinecraft().effectRenderer.addEffect(fx);
        }

        for (int i = 0; i < 50 + getInfo().flakParticlesDiff; i++) {
            EntityCloudFX obj = new EntityCloudFX(
                worldObj,
                raytraceResult.hitVec.xCoord + (rand.nextFloat() - 0.5D) * width,
                raytraceResult.hitVec.yCoord + rand.nextGaussian() * height,
                raytraceResult.hitVec.zCoord + (rand.nextFloat() - 0.5D) * width,
                0D, 0D, 0D
            ) {
                // 重写渲染方法确保颜色固定
                @Override
                public void renderParticle(Tessellator tessellator, float partialTicks,
                                           float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
                    GL11.glColor4f(DARK_RED_R, DARK_RED_G, DARK_RED_B, 1.0f);
                    super.renderParticle(tessellator, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
                }
            };

            // 设置粒子参数
            obj.setRBGColorF(DARK_RED_R, DARK_RED_G, DARK_RED_B);
            obj.motionX = rand.nextGaussian() / 100; // 增加运动速度
            obj.motionY = rand.nextGaussian() / 100;
            obj.motionZ = rand.nextGaussian() / 100;
            obj.renderDistanceWeight = 350D; // 增加可见距离

            FMLClientHandler.instance().getClient().effectRenderer.addEffect(obj);
        }
    }

    @SideOnly(Side.CLIENT)
    public void spawnBlockPar(MovingObjectPosition raytraceResult, int xTile, int yTile, int zTile) {
        int num = getInfo().flakParticlesCrack + rand.nextInt(3);
        float scale = 1.0F;
        for (int i = 0; i < num; i++) {
            EntityDiggingFX fx = (new EntityDiggingFX(this.worldObj,
                raytraceResult.hitVec.xCoord + (rand.nextFloat() - 0.5D) * width,
                raytraceResult.hitVec.yCoord + 0.1D,
                raytraceResult.hitVec.zCoord + (rand.nextFloat() - 0.5D) * width,
                0, 0, 0, worldObj.getBlock(xTile, yTile, zTile),
                this.worldObj.getBlockMetadata(xTile, yTile, zTile))).applyRenderColor(this.worldObj.getBlockMetadata(xTile, yTile, zTile));
            fx.motionX += getInfo().flakParticlesDiff / 2 * rand.nextGaussian();
            fx.motionZ += getInfo().flakParticlesDiff / 2 * rand.nextGaussian();
            fx.motionY += getInfo().flakParticlesDiff * Math.abs(rand.nextGaussian());
            fx.multipleParticleScaleBy(scale);
            Minecraft.getMinecraft().effectRenderer.addEffect(fx);
        }

        for (int i = 0; i < getInfo().numParticlesFlak; i++) {
            EntityFX obj = new EntityCloudFX(worldObj,
                raytraceResult.hitVec.xCoord + rand.nextGaussian(),
                raytraceResult.hitVec.yCoord + rand.nextGaussian(),
                raytraceResult.hitVec.zCoord + rand.nextGaussian(), 0D, 0D, 0D);
            obj.motionX = rand.nextGaussian() / 200;
            obj.motionY = rand.nextGaussian() / 200;
            obj.motionZ = rand.nextGaussian() / 200;
            obj.renderDistanceWeight = 250D;
            FMLClientHandler.instance().getClient().effectRenderer.addEffect(obj);
        }
    }

    public void onImpactEntity(Entity entity, float damageFactor, Vec3 hitVec) {
        if (!entity.isDead) {
            MCH_Lib.DbgLog(super.worldObj, "MCH_EntityBaseBullet.onImpactEntity:Damage=%d:" + entity.getClass(), this.getPower());
            MCH_Lib.applyEntityHurtResistantTimeConfig(entity);
            DamageSource ds = DamageSource.causeThrownDamage(this, this.shootingEntity);
            ds = MCH_IndicatedDamageSource.build(ds, hitVec, Vec3.createVectorHelper(motionX, motionY, motionZ));
            float damage = MCH_Config.applyDamageVsEntity(entity, ds, (float) this.getPower() * damageFactor);
            damage *= this.getInfo() != null ? this.getInfo().getDamageFactor(entity) : 1.0F;
            entity.attackEntityFrom(ds, damage);
            if (this instanceof MCH_EntityBullet && entity instanceof EntityVillager && this.shootingEntity != null && this.shootingEntity.ridingEntity instanceof MCH_EntitySeat) {
                MCH_Achievement.addStat(this.shootingEntity, MCH_Achievement.aintWarHell, 1);
            }
        }

        this.notifyHitBullet();
    }

    public void newFAExplosion(double x, double y, double z, float exp, float expBlock) {
        MCH_ExplosionParam param = MCH_ExplosionParam.builder()
            .exploder(this)
            .player(this.shootingEntity instanceof EntityPlayer ? (EntityPlayer) this.shootingEntity : null)
            .x(x).y(y).z(z)
            .size(exp)
            .sizeBlock(expBlock)
            .isPlaySound(true)
            .isSmoking(true)
            .isFlaming(this.getInfo().flaming)
            .isDestroyBlock(false)
            .countSetFireEntity(15)
            .isInWater(false)
            .damageVsPlayer(getInfo().explosionDamageVsPlayer)
            .damageVsLiving(getInfo().explosionDamageVsLiving)
            .damageVsPlane(getInfo().explosionDamageVsPlane)
            .damageVsHeli(getInfo().explosionDamageVsHeli)
            .damageVsTank(getInfo().explosionDamageVsTank)
            .damageVsVehicle(getInfo().explosionDamageVsVehicle)
            .damageVsShip(getInfo().explosionDamageVsShip)
            .explosionThroughWall(getInfo().explosionThroughWall)
            .build();
        MCH_Explosion.ExplosionResult result = MCH_Explosion.newExplosion(super.worldObj, param);
        if (result != null && result.hitEntity) {
            this.notifyHitBullet();
        }
    }

    public void newExplosion(double x, double y, double z, float exp, float expBlock, boolean inWater) {
        newExplosion(x, y, z, exp, expBlock, inWater, null);
    }

    public void newExplosion(double x, double y, double z, float exp, float expBlock, boolean inWater, Entity directAttackEntity) {
        MCH_Explosion.ExplosionResult result;
        boolean playSound = (this.isBomblet != 1) || (super.rand.nextInt(3) == 0);
        EntityPlayer creditedPlayer = (this.shootingEntity instanceof EntityPlayer)
            ? (EntityPlayer) this.shootingEntity
            : null;
        if (!inWater) {
            //HBM爆炸效果
            if (this.getInfo().explosionType.contains("hbmNT") && MCH_HBMUtil.isHBMLoaded) {
                Object explosionNTInstance = MCH_HBMUtil.ExplosionNT_instance_init(super.worldObj, null, x, y, z, getInfo().effectYield);
                if (explosionNTInstance != null && !this.getInfo().disableDestroyBlock) {
                    MCH_HBMUtil.ExplosionNT_instance_addAttrib(explosionNTInstance, "NOHURT");
                    MCH_HBMUtil.ExplosionNT_instance_overrideResolutionAndExplode(explosionNTInstance, 64);
                }
                if (this.getInfo().explosionType.equals("hbmNT_Bomb")) {
                    MCH_HBMUtil.ExplosionCreator_composeEffect(worldObj, x + 0.5, y + 1, z + 0.5, getInfo().effectYield);
                } else if (this.getInfo().explosionType.equals("hbmNT_Shell")) {
                    MCH_HBMUtil.ExplosionSmallCreator_composeEffect(worldObj, x + 0.5, y + 1, z + 0.5, getInfo().effectYield);
                }
                MCH_ExplosionParam param = MCH_ExplosionParam.builder()
                    .exploder(this)
                    .player(creditedPlayer)
                    .x(x).y(y).z(z)
                    .size(exp)
                    .sizeBlock(expBlock)
                    .isPlaySound(playSound)
                    .isSmoking(false)
                    .isFlaming(this.getInfo().flaming)
                    .isDestroyBlock(false)
                    .isInWater(false)
                    .directAttackEntity(directAttackEntity)
                    .damageVsPlayer(getInfo().explosionDamageVsPlayer)
                    .damageVsLiving(getInfo().explosionDamageVsLiving)
                    .damageVsPlane(getInfo().explosionDamageVsPlane)
                    .damageVsHeli(getInfo().explosionDamageVsHeli)
                    .damageVsTank(getInfo().explosionDamageVsTank)
                    .damageVsVehicle(getInfo().explosionDamageVsVehicle)
                    .damageVsShip(getInfo().explosionDamageVsShip)
                    .explosionThroughWall(getInfo().explosionThroughWall)
                    .build();
                result = MCH_Explosion.newExplosion(super.worldObj, param);
            }
            //普通爆炸效果
            else {
                MCH_ExplosionParam param = MCH_ExplosionParam.builder()
                    .exploder(this)
                    .player(creditedPlayer)
                    .x(x).y(y).z(z)
                    .size(exp)
                    .sizeBlock(expBlock)
                    .isPlaySound(playSound)
                    .isSmoking(true)
                    .isFlaming(this.getInfo().flaming)
                    .isDestroyBlock(getInfo().explosionBlock > 0)
                    .isInWater(false)
                    .directAttackEntity(directAttackEntity)
                    .damageVsPlayer(getInfo().explosionDamageVsPlayer)
                    .damageVsLiving(getInfo().explosionDamageVsLiving)
                    .damageVsPlane(getInfo().explosionDamageVsPlane)
                    .damageVsHeli(getInfo().explosionDamageVsHeli)
                    .damageVsTank(getInfo().explosionDamageVsTank)
                    .damageVsVehicle(getInfo().explosionDamageVsVehicle)
                    .damageVsShip(getInfo().explosionDamageVsShip)
                    .explosionThroughWall(getInfo().explosionThroughWall)
                    .build();
                result = MCH_Explosion.newExplosion(super.worldObj, param);
            }
        } else {
            //水下爆炸
            MCH_ExplosionParam param = MCH_ExplosionParam.builder()
                .exploder(this)
                .player(creditedPlayer)
                .x(x).y(y).z(z)
                .size(exp)
                .sizeBlock(expBlock)
                .isPlaySound(playSound)
                .isSmoking(true)
                .isFlaming(this.getInfo().flaming)
                .isDestroyBlock(getInfo().explosionBlock > 0)
                .isInWater(true)
                .directAttackEntity(directAttackEntity)
                .damageVsPlayer(getInfo().explosionDamageVsPlayer)
                .damageVsLiving(getInfo().explosionDamageVsLiving)
                .damageVsPlane(getInfo().explosionDamageVsPlane)
                .damageVsHeli(getInfo().explosionDamageVsHeli)
                .damageVsTank(getInfo().explosionDamageVsTank)
                .damageVsVehicle(getInfo().explosionDamageVsVehicle)
                .damageVsShip(getInfo().explosionDamageVsShip)
                .explosionThroughWall(getInfo().explosionThroughWall)
                .build();
            result = MCH_Explosion.newExplosion(super.worldObj, param);
        }

        if (this.getInfo().nukeYield > 0 && MCH_HBMUtil.isHBMLoaded) {
            if (!this.getInfo().nukeEffectOnly) {
                worldObj.spawnEntityInWorld((Entity) MCH_HBMUtil.EntityNukeExplosionMK5_statFac(super.worldObj, this.getInfo().nukeYield, this.posX + 0.5, this.posY + 0.5, this.posZ + 0.5));
            }
            //EntityNukeTorex.statFac(super.worldObj, this.posX + 0.5, this.posY + 0.5, this.posZ + 0.5, (float) this.getInfo().nukeYield, 0);
            MCH_HBMUtil.EntityNukeTorex_statFac(super.worldObj, this.posX + 0.5, this.posY + 0.5, this.posZ + 0.5, (float) this.getInfo().nukeYield, getInfo().effectYield);
        }

        if (this.getInfo().chemYield > 0 && MCH_HBMUtil.isHBMLoaded) {
            MCH_HBMUtil.ExplosionChaos_spawnClorine(super.worldObj, posX, posY + 0.5, posZ, this.getInfo().chemYield);
        }


        if (result != null && result.hitEntity) {
            this.notifyHitBullet();
        }

    }

    public void playExplosionSound() {
        MCH_Explosion.playExplosionSound(super.worldObj, super.posX, super.posY, super.posZ);
    }

    public void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {
        par1NBTTagCompound.setTag("direction", this.newDoubleNBTList(super.motionX, super.motionY, super.motionZ));
        par1NBTTagCompound.setString("WeaponName", this.getName());
    }

    public void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {
        this.setDead();
    }

    public boolean canBeCollidedWith() {
        return true;
    }

    public float getCollisionBorderSize() {
        return 1.0F;
    }

    public boolean attackEntityFrom(DamageSource ds, float par2) {

        if (this.isEntityInvulnerable()) {
            return false;
        } else if (!super.worldObj.isRemote && par2 > 0.0F && ds.getDamageType().equalsIgnoreCase("thrown")) {
            this.setBeenAttacked();
            MovingObjectPosition m = new MovingObjectPosition((int) (super.posX + 0.5D), (int) (super.posY + 0.5D), (int) (super.posZ + 0.5D), 0, Vec3.createVectorHelper(super.posX + 0.5D, super.posY + 0.5D, super.posZ + 0.5D));
            this.onImpact(m, 1.0F);
            return true;
        } else {
            return false;
        }
    }

    @SideOnly(Side.CLIENT)
    public float getShadowSize() {
        return 0.0F;
    }

    public float getBrightness(float par1) {
        return 1.0F;
    }

    @SideOnly(Side.CLIENT)
    public int getBrightnessForRender(float par1) {
        return 15728880;
    }

    public int getPower() {
        return this.power;
    }

    public void setPower(int power) {
        this.power = power;
    }


    protected void scanForTargets() {
        if (numLockedChaff >= getInfo().numLockedChaffMax) {
            setTargetEntity(null);
            return;
        }
        Vector3f missileDirection = new Vector3f((float) super.motionX, (float) super.motionY, (float) super.motionZ);
        double range = getInfo().maxLockOnRange;
        List<Entity> list = worldObj.getEntitiesWithinAABB(
            Entity.class,
            AxisAlignedBB.getBoundingBox(posX - range, posY - range, posZ - range,
                posX + range, posY + range, posZ + range)
        );

        if (list != null && !list.isEmpty()) {
            double closestAngle = Double.MAX_VALUE;
            Entity closestTarget = null;

            // 记录最近的箔条及其距离
            double nearestChaffDistSq = Double.MAX_VALUE;
            Entity nearestChaff = null;

            for (Entity entity : list) {
                // AA 导弹的目标判定
                if (this instanceof MCH_EntityAAMissile) {
                    // 发现箔条时先处理
                    if (entity instanceof MCH_EntityChaff) {
                        // 计算与导弹方向的夹角，确保在锁定范围内
                        double dx = entity.posX - super.posX;
                        double dy = entity.posY - super.posY;
                        double dz = entity.posZ - super.posZ;
                        Vector3f targetDir = new Vector3f((float) dx, (float) dy, (float) dz);
                        double angle = Math.abs(Vector3f.angle(missileDirection, targetDir));
                        if (angle > Math.toRadians(getInfo().maxLockOnAngle)) continue;
                        double distSq = dx * dx + dy * dy + dz * dz;
                        if (distSq < nearestChaffDistSq) {
                            nearestChaffDistSq = distSq;
                            nearestChaff = entity;
                        }
                    }
                    // 未发现箔条时按原有逻辑扫描飞机
                    else if (entity instanceof MCH_EntityAircraft) {
                        if (W_Entity.isEqual(entity, shootingAircraft)) continue;
                        if (shootingEntity instanceof EntityLivingBase && entity.riddenByEntity instanceof EntityPlayer
                            && ((EntityPlayer) entity.riddenByEntity).isOnSameTeam((EntityLivingBase) shootingEntity)) {
                            continue;
                        }
                        // 排除地面上的目标
                        if (MCH_WeaponGuidanceSystem.isEntityOnGround(entity, getInfo().lockMinHeight)) continue;

                        double dx = entity.posX - super.posX;
                        double dy = entity.posY - super.posY;
                        double dz = entity.posZ - super.posZ;
                        Vector3f targetDir = new Vector3f((float) dx, (float) dy, (float) dz);
                        double angle = Math.abs(Vector3f.angle(missileDirection, targetDir));
                        if (angle > Math.toRadians(getInfo().maxLockOnAngle)) continue;

                        if (angle < closestAngle) {
                            closestAngle = angle;
                            closestTarget = entity;
                        }
                    }
                }
                // AT 导弹的逻辑不变…
                else if (this instanceof MCH_EntityATMissile) {
                    // 保持原有地面目标选择逻辑
                    if (entity instanceof MCH_EntityAircraft) {
                        if (W_Entity.isEqual(entity, shootingAircraft)) continue;
                        if (shootingEntity instanceof EntityLivingBase && entity.riddenByEntity instanceof EntityPlayer
                            && ((EntityPlayer) entity.riddenByEntity).isOnSameTeam((EntityLivingBase) shootingEntity)) {
                            continue;
                        }
                        boolean isTargetOnGround = MCH_WeaponGuidanceSystem.isEntityOnGround(entity, getInfo().lockMinHeight);
                        if (!isTargetOnGround) continue;
                        double dx = entity.posX - super.posX;
                        double dy = entity.posY - super.posY;
                        double dz = entity.posZ - super.posZ;
                        Vector3f targetDirection = new Vector3f((float) dx, (float) dy, (float) dz);
                        double angle = Math.abs(Vector3f.angle(missileDirection, targetDirection));
                        if (angle > Math.toRadians(getInfo().maxLockOnAngle)) continue;
                        if (angle < closestAngle) {
                            closestAngle = angle;
                            closestTarget = entity;
                        }
                    } else if (!getInfo().ridableOnly && entity instanceof EntityLivingBase && entity.ridingEntity == null) {
                        if (W_Entity.isEqual(entity, shootingEntity)) continue;
                        if (shootingEntity instanceof EntityLivingBase && ((EntityLivingBase) entity).isOnSameTeam((EntityLivingBase) shootingEntity)) {
                            continue;
                        }
                        boolean isTargetOnGround = MCH_WeaponGuidanceSystem.isEntityOnGround(entity, getInfo().lockMinHeight);
                        if (!isTargetOnGround) continue;
                        double dx = entity.posX - super.posX;
                        double dy = entity.posY - super.posY;
                        double dz = entity.posZ - super.posZ;
                        Vector3f targetDirection = new Vector3f((float) dx, (float) dy, (float) dz);
                        double angle = Math.abs(Vector3f.angle(missileDirection, targetDirection));
                        if (angle > Math.toRadians(getInfo().maxLockOnAngle)) continue;
                        if (angle < closestAngle) {
                            closestAngle = angle;
                            closestTarget = entity;
                        }
                    }
                }
            }
            // 优先锁定箔条
            if (nearestChaff != null) {
                targetEntity = nearestChaff;
                numLockedChaff++;
            } else if (closestTarget != null) {
                targetEntity = closestTarget;
            }
        }
    }


    public void onUpdateSpreader() {
        if (!super.worldObj.isRemote) {
            if (this.getInfo().spawnBulletInAir && this.spawnedBulletNum < getInfo().spawnBulletMaxNum && !super.isDead) {
                if (this.ticksExisted > 5 && this.ticksExisted % getInfo().spawnBulletIntervalTick == 0) {
                    ++this.spawnedBulletNum;
                    for (int i = 0; i < this.getInfo().spawnBulletPerNum; ++i) {
                        double mX = 1e-6, mY = 1e-6, mZ = 1e-6, speed = 0.001;
                        if (getInfo().spawnBulletInheritSpeed) {
                            mX = motionX;
                            mY = motionY;
                            mZ = motionZ;
                            speed = acceleration;
                        }
                        MCH_EntityRocket e = new MCH_EntityRocket(super.worldObj, posX, posY, posZ, mX, mY, mZ, rotationYaw, rotationPitch, speed);
                        e.setName(getInfo().bombletModelName);
                        e.setParameterFromWeapon(shootingAircraft, shootingEntity);
                        e.setPower(e.getInfo().power);
                        e.explosionPower = e.getInfo().explosion;
                        e.explosionPowerInWater = e.getInfo().explosionInWater;
                        float MOTION = this.getInfo().bombletDiff;
                        e.motionX += ((double) super.rand.nextFloat() - 0.5D) * (double) MOTION;
                        e.motionY += ((double) super.rand.nextFloat() - 0.5D) * (double) MOTION;
                        e.motionZ += ((double) super.rand.nextFloat() - 0.5D) * (double) MOTION;

                        super.worldObj.spawnEntityInWorld(e);
                    }
                }
            }
        }
    }

}
