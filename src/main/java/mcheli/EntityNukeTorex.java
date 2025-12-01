package mcheli;

import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.awt.Color;
import java.util.ArrayList;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IntHashMap;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class EntityNukeTorex extends Entity {

    public double coreHeight = 3.0D;
    public double convectionHeight = 3.0D;
    public double torusWidth = 3.0D;
    public double rollerSize = 1.0D;
    public double heat = 1.0D;
    public double lastSpawnY = -1.0D;
    public boolean didShake = false;
    public boolean didPlaySound = false;

    public ArrayList<Cloudlet> cloudlets = new ArrayList<Cloudlet>();

    public EntityNukeTorex(World world) {
        super(world);
        this.preventEntitySpawning = true;
        this.setSize(1.0F, 50.0F);
    }

    @Override
    protected void entityInit() {
        this.dataWatcher.addObject(10, Float.valueOf(1.0F)); // scale
        this.dataWatcher.addObject(11, Integer.valueOf(0));  // type
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getBrightnessForRender(float partialTicks) {
        return 15728880;
    }

    @Override
    public float getBrightness(float partialTicks) {
        return 1.0F;
    }

    @Override
    public void onUpdate() {
        double s = 1.5D;
        double cs = 1.5D;
        int maxAge = getMaxAge();

        if (this.worldObj.isRemote) {

            if (this.ticksExisted == 1) {
                setScale((float) s);
            }

            if (this.lastSpawnY == -1.0D) {
                this.lastSpawnY = this.posY - 3.0D;
            }

            if (this.ticksExisted < 100) {
                this.worldObj.skylightSubtracted = 2;
            }

            int spawnTarget = Math.max(
                this.worldObj.getTopSolidOrLiquidBlock(
                    (int) Math.floor(this.posX),
                    (int) Math.floor(this.posZ)
                ) - 3,
                1
            );

            double moveSpeed = 0.5D;
            if (Math.abs(spawnTarget - this.lastSpawnY) < moveSpeed) {
                this.lastSpawnY = spawnTarget;
            } else {
                this.lastSpawnY += moveSpeed * Math.signum(spawnTarget - this.lastSpawnY);
            }

            double range = (this.torusWidth - this.rollerSize) * 0.25D;
            double simSpeed = getSimulationSpeed();
            int toSpawn = (int) Math.ceil(10.0D * simSpeed * simSpeed);
            int lifetime = Math.min(
                this.ticksExisted * this.ticksExisted + 200,
                maxAge - this.ticksExisted + 200
            );

            // 主体云柱随机 cloudlets
            for (int i = 0; i < toSpawn; i++) {
                double x = this.posX + this.rand.nextGaussian() * range;
                double z = this.posZ + this.rand.nextGaussian() * range;
                Cloudlet cloud = new Cloudlet(
                    x,
                    this.lastSpawnY,
                    z,
                    (float) (this.rand.nextDouble() * 2.0D * Math.PI),
                    0,
                    lifetime
                );
                cloud.setScale(
                    1.0F + this.ticksExisted * 0.005F * (float) cs,
                    5.0F * (float) cs
                );
                this.cloudlets.add(cloud);
            }

            // 冲击波云环（SHOCK）
            if (this.ticksExisted < 150) {
                int cloudCount = this.ticksExisted * 5;
                int shockLife = Math.max(300 - this.ticksExisted * 20, 50);

                for (int j = 0; j < cloudCount; j++) {
                    Vec3 vec = Vec3.createVectorHelper(
                        (this.ticksExisted * 1.5D + this.rand.nextDouble()) * 1.5D,
                        0.0D,
                        0.0D
                    );
                    float rot = (float) (2.0D * Math.PI * this.rand.nextDouble());
                    vec.rotateAroundY(rot);

                    Cloudlet cloud = new Cloudlet(
                        vec.xCoord + this.posX,
                        this.worldObj.getTopSolidOrLiquidBlock(
                            (int) (vec.xCoord + this.posX) + 1,
                            (int) (vec.zCoord + this.posZ)
                        ),
                        vec.zCoord + this.posZ,
                        rot,
                        0,
                        shockLife,
                        TorexType.SHOCK
                    ).setScale(7.0F, 2.0F)
                        .setMotion((this.ticksExisted > 15) ? 0.75D : 0.0D);

                    this.cloudlets.add(cloud);
                }
            }

            // 上升中间的 RING 云环
            if (this.ticksExisted < 130.0D * s) {
                lifetime = (int) (lifetime * s);
                for (int i = 0; i < 2; i++) {
                    Cloudlet cloud = new Cloudlet(
                        this.posX,
                        this.posY + this.coreHeight,
                        this.posZ,
                        (float) (this.rand.nextDouble() * 2.0D * Math.PI),
                        0,
                        lifetime,
                        TorexType.RING
                    );
                    cloud.setScale(
                        1.0F + this.ticksExisted * 0.0025F * (float) (cs * cs),
                        3.0F * (float) (cs * cs)
                    );
                    this.cloudlets.add(cloud);
                }
            }

            // 高空冷凝云（第一层）
            if (this.ticksExisted > 130.0D * s && this.ticksExisted < 600.0D * s) {
                for (int i = 0; i < 20; i++) {
                    for (int j = 0; j < 4; j++) {
                        float angle = (float) (2.0D * Math.PI * this.rand.nextDouble());
                        Vec3 vec = Vec3.createVectorHelper(
                            this.torusWidth + this.rollerSize * (5.0D + this.rand.nextDouble()),
                            0.0D,
                            0.0D
                        );
                        vec.rotateAroundZ((float) (0.06981317007977318D * j)); // ~4°
                        vec.rotateAroundY(angle);

                        Cloudlet cloud = new Cloudlet(
                            this.posX + vec.xCoord,
                            this.posY + this.coreHeight - 5.0D + j * s,
                            this.posZ + vec.zCoord,
                            angle,
                            0,
                            (int) ((20 + this.ticksExisted / 10) *
                                (1.0D + this.rand.nextDouble() * 0.1D)),
                            TorexType.CONDENSATION
                        );
                        cloud.setScale(0.125F * (float) cs, 3.0F * (float) cs);
                        this.cloudlets.add(cloud);
                    }
                }
            }

            // 高空冷凝云（第二层）
            if (this.ticksExisted > 200.0D * s && this.ticksExisted < 600.0D * s) {
                for (int i = 0; i < 20; i++) {
                    for (int j = 0; j < 4; j++) {
                        float angle = (float) (2.0D * Math.PI * this.rand.nextDouble());
                        Vec3 vec = Vec3.createVectorHelper(
                            this.torusWidth + this.rollerSize * (3.0D + this.rand.nextDouble() * 0.5D),
                            0.0D,
                            0.0D
                        );
                        vec.rotateAroundZ((float) (0.06981317007977318D * j));
                        vec.rotateAroundY(angle);

                        Cloudlet cloud = new Cloudlet(
                            this.posX + vec.xCoord,
                            this.posY + this.coreHeight + 25.0D + j * cs,
                            this.posZ + vec.zCoord,
                            angle,
                            0,
                            (int) ((20 + this.ticksExisted / 10) *
                                (1.0D + this.rand.nextDouble() * 0.1D)),
                            TorexType.CONDENSATION
                        );
                        cloud.setScale(0.125F * (float) cs, 3.0F * (float) cs);
                        this.cloudlets.add(cloud);
                    }
                }
            }

            // 更新所有 cloudlet
            for (Cloudlet cloud : this.cloudlets) {
                cloud.update();
            }

            this.coreHeight += 0.15D / s;
            this.torusWidth += 0.05D / s;
            this.rollerSize = this.torusWidth * 0.35D;
            this.convectionHeight = this.coreHeight + this.rollerSize;

            int maxHeat = (int) (50.0D * cs);
            this.heat = maxHeat - Math.pow(maxHeat * this.ticksExisted / (double) maxAge, 1.0D);

            this.cloudlets.removeIf(c -> c.isDead);
        }

        if (!this.worldObj.isRemote && this.ticksExisted > maxAge) {
            this.setDead();
        }
    }

    public EntityNukeTorex setScale(float scale) {
        if (!this.worldObj.isRemote) {
            this.dataWatcher.updateObject(10, Float.valueOf(scale));
        }
        this.coreHeight = this.coreHeight / 1.5D * scale;
        this.convectionHeight = this.convectionHeight / 1.5D * scale;
        this.torusWidth = this.torusWidth / 1.5D * scale;
        this.rollerSize = this.rollerSize / 1.5D * scale;
        return this;
    }

    public EntityNukeTorex setType(int type) {
        this.dataWatcher.updateObject(11, Integer.valueOf(type));
        return this;
    }

    public double getSimulationSpeed() {
        int lifetime = getMaxAge();
        int simSlow = lifetime / 4;
        int simStop = lifetime / 2;
        int life = this.ticksExisted;

        if (life > simStop) {
            return 0.0D;
        }
        if (life > simSlow) {
            return 1.0D - (life - simSlow) / (double) (simStop - simSlow);
        }
        return 1.0D;
    }

    public double getScale() {
        return this.dataWatcher.getWatchableObjectFloat(10);
    }

    public double getSaturation() {
        double d = this.ticksExisted / (double) getMaxAge();
        return 1.0D - d * d * d * d;
    }

    public double getGreying() {
        int lifetime = getMaxAge();
        int greying = lifetime * 3 / 4;
        if (this.ticksExisted > greying) {
            return 1.0D +
                (this.ticksExisted - greying) / (double) (lifetime - greying);
        }
        return 1.0D;
    }

    public float getAlpha() {
        int lifetime = getMaxAge();
        int fadeOut = lifetime * 3 / 4;
        int life = this.ticksExisted;

        if (life > fadeOut) {
            float fac = (life - fadeOut) / (float) (lifetime - fadeOut);
            return 1.0F - fac;
        }
        return 1.0F;
    }

    public int getMaxAge() {
        double s = getScale();
        return (int) (900.0D * s);
    }

    public class Cloudlet {

        public double posX;
        public double posY;
        public double posZ;

        public double prevPosX;
        public double prevPosY;
        public double prevPosZ;

        public double motionX;
        public double motionY;
        public double motionZ;

        public int age;
        public int cloudletLife;

        public float angle;
        public boolean isDead = false;

        float rangeMod = 1.0F;
        public float colorMod = 1.0F;

        public Vec3 color;
        public Vec3 prevColor;

        public TorexType type;

        private float startingScale;
        private float growingScale;
        private double motionMult;

        public Cloudlet(double posX, double posY, double posZ,
                        float angle, int age, int maxAge) {
            this(posX, posY, posZ, angle, age, maxAge, TorexType.STANDARD);
        }

        public Cloudlet(double posX, double posY, double posZ,
                        float angle, int age, int maxAge, TorexType type) {
            this.startingScale = 1.0F;
            this.growingScale = 5.0F;
            this.motionMult = 1.0D;

            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;

            this.age = age;
            this.cloudletLife = maxAge;
            this.angle = angle;

            this.rangeMod = 0.3F + EntityNukeTorex.this.rand.nextFloat() * 0.7F;
            this.colorMod = 0.8F + EntityNukeTorex.this.rand.nextFloat() * 0.2F;

            this.type = type;
            updateColor();
        }

        private void update() {
            this.age++;
            if (this.age > this.cloudletLife) {
                this.isDead = true;
            }

            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;

            Vec3 simPos = Vec3.createVectorHelper(
                EntityNukeTorex.this.posX - this.posX,
                0.0D,
                EntityNukeTorex.this.posZ - this.posZ
            );
            double simPosX = EntityNukeTorex.this.posX + simPos.lengthVector();
            double simPosZ = EntityNukeTorex.this.posZ + 0.0D;

            if (this.type == TorexType.STANDARD) {
                Vec3 convection = getConvectionMotion(simPosX, simPosZ);
                Vec3 lift = getLiftMotion(simPosX, simPosZ);
                double factor = MathHelper.clamp_double(
                    (this.posY - EntityNukeTorex.this.posY) /
                        EntityNukeTorex.this.coreHeight,
                    0.0D,
                    1.0D
                );

                this.motionX = convection.xCoord * factor +
                    lift.xCoord * (1.0D - factor);
                this.motionY = convection.yCoord * factor +
                    lift.yCoord * (1.0D - factor);
                this.motionZ = convection.zCoord * factor +
                    lift.zCoord * (1.0D - factor);

            } else if (this.type == TorexType.SHOCK) {
                double factor = MathHelper.clamp_double(
                    (this.posY - EntityNukeTorex.this.posY) /
                        EntityNukeTorex.this.coreHeight,
                    0.0D,
                    1.0D
                );
                Vec3 motion = Vec3.createVectorHelper(1.0D, 0.0D, 0.0D);
                motion.rotateAroundY(this.angle);

                this.motionX = motion.xCoord * factor;
                this.motionY = motion.yCoord * factor;
                this.motionZ = motion.zCoord * factor;

            } else if (this.type == TorexType.RING) {
                Vec3 motion = getRingMotion(simPosX, simPosZ);
                this.motionX = motion.xCoord;
                this.motionY = motion.yCoord;
                this.motionZ = motion.zCoord;

            } else if (this.type == TorexType.CONDENSATION) {
                Vec3 motion = getCondensationMotion();
                this.motionX = motion.xCoord;
                this.motionY = motion.yCoord;
                this.motionZ = motion.zCoord;
            }

            double mult = this.motionMult * EntityNukeTorex.this.getSimulationSpeed();
            this.posX += this.motionX * mult;
            this.posY += this.motionY * mult;
            this.posZ += this.motionZ * mult;

            updateColor();
        }

        private Vec3 getCondensationMotion() {
            Vec3 delta = Vec3.createVectorHelper(
                this.posX - EntityNukeTorex.this.posX,
                0.0D,
                this.posZ - EntityNukeTorex.this.posZ
            );
            double speed = 2.0E-5D * EntityNukeTorex.this.ticksExisted;
            delta.xCoord *= speed;
            delta.zCoord *= speed;
            return delta;
        }

        private Vec3 getRingMotion(double simPosX, double simPosZ) {
            if (simPosX > EntityNukeTorex.this.posX +
                EntityNukeTorex.this.torusWidth * 2.0D) {
                return Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
            }

            Vec3 torusPos = Vec3.createVectorHelper(
                EntityNukeTorex.this.posX + EntityNukeTorex.this.torusWidth,
                EntityNukeTorex.this.posY + EntityNukeTorex.this.coreHeight * 0.5D,
                EntityNukeTorex.this.posZ
            );
            Vec3 delta = Vec3.createVectorHelper(
                torusPos.xCoord - simPosX,
                torusPos.yCoord - this.posY,
                torusPos.zCoord - simPosZ
            );

            double roller = EntityNukeTorex.this.rollerSize * this.rangeMod * 0.25D;
            double dist = delta.lengthVector() / roller - 1.0D;
            double func = 1.0D - Math.pow(Math.E, -dist);
            float angle = (float) (func * Math.PI * 0.5D);

            Vec3 rot = Vec3.createVectorHelper(
                -delta.xCoord / dist,
                -delta.yCoord / dist,
                -delta.zCoord / dist
            );
            rot.rotateAroundZ(angle);

            Vec3 motion = Vec3.createVectorHelper(
                torusPos.xCoord + rot.xCoord - simPosX,
                torusPos.yCoord + rot.yCoord - this.posY,
                torusPos.zCoord + rot.zCoord - simPosZ
            );

            double speed = 0.001D;
            motion.xCoord *= speed;
            motion.yCoord *= speed;
            motion.zCoord *= speed;

            motion = motion.normalize();
            motion.rotateAroundY(this.angle);
            return motion;
        }

        private Vec3 getConvectionMotion(double simPosX, double simPosZ) {
            Vec3 torusPos = Vec3.createVectorHelper(
                EntityNukeTorex.this.posX + EntityNukeTorex.this.torusWidth,
                EntityNukeTorex.this.posY + EntityNukeTorex.this.coreHeight,
                EntityNukeTorex.this.posZ
            );
            Vec3 delta = Vec3.createVectorHelper(
                torusPos.xCoord - simPosX,
                torusPos.yCoord - this.posY,
                torusPos.zCoord - simPosZ
            );

            double roller = EntityNukeTorex.this.rollerSize * this.rangeMod;
            double dist = delta.lengthVector() / roller - 1.0D;
            double func = 1.0D - Math.pow(Math.E, -dist);
            float angle = (float) (func * Math.PI * 0.5D);

            Vec3 rot = Vec3.createVectorHelper(
                -delta.xCoord / dist,
                -delta.yCoord / dist,
                -delta.zCoord / dist
            );
            rot.rotateAroundZ(angle);

            Vec3 motion = Vec3.createVectorHelper(
                torusPos.xCoord + rot.xCoord - simPosX,
                torusPos.yCoord + rot.yCoord - this.posY,
                torusPos.zCoord + rot.zCoord - simPosZ
            );
            motion = motion.normalize();
            motion.rotateAroundY(this.angle);

            return motion;
        }

        private Vec3 getLiftMotion(double simPosX, double simPosZ) {
            double scale = MathHelper.clamp_double(
                1.0D - simPosX - EntityNukeTorex.this.posX +
                    EntityNukeTorex.this.torusWidth,
                0.0D,
                1.0D
            );
            Vec3 motion = Vec3.createVectorHelper(
                EntityNukeTorex.this.posX - this.posX,
                EntityNukeTorex.this.posY + EntityNukeTorex.this.convectionHeight - this.posY,
                EntityNukeTorex.this.posZ - this.posZ
            );
            motion = motion.normalize();
            motion.xCoord *= scale;
            motion.yCoord *= scale;
            motion.zCoord *= scale;
            return motion;
        }

        private void updateColor() {
            this.prevColor = this.color;

            double exX = EntityNukeTorex.this.posX;
            double exY = EntityNukeTorex.this.posY + EntityNukeTorex.this.coreHeight;
            double exZ = EntityNukeTorex.this.posZ;

            double distX = exX - this.posX;
            double distY = exY - this.posY;
            double distZ = exZ - this.posZ;
            double distSq = distX * distX + distY * distY + distZ * distZ;

            distSq /= EntityNukeTorex.this.heat;
            double dist = Math.sqrt(distSq);
            dist = Math.max(dist, 1.0D);
            double col = 2.0D / dist;

            int type = EntityNukeTorex.this.dataWatcher.getWatchableObjectInt(11);

            if (type == 1) {
                this.color = Vec3.createVectorHelper(
                    Math.max(col * 1.0D, 0.25D),
                    Math.max(col * 2.0D, 0.25D),
                    Math.max(col * 0.5D, 0.25D)
                );
            } else if (type == 2) {
                Color hsvColor = Color.getHSBColor(
                    this.angle / 2.0F / (float) Math.PI,
                    1.0F,
                    1.0F
                );

                if (this.type == TorexType.RING) {
                    this.color = Vec3.createVectorHelper(
                        Math.max(col * 1.0D, 0.25D),
                        Math.max(col * 1.0D, 0.25D),
                        Math.max(col * 1.0D, 0.25D)
                    );
                } else {
                    this.color = Vec3.createVectorHelper(
                        hsvColor.getRed() / 255.0D,
                        hsvColor.getGreen() / 255.0D,
                        hsvColor.getBlue() / 255.0D
                    );
                }
            } else {
                this.color = Vec3.createVectorHelper(
                    Math.max(col * 2.0D, 0.25D),
                    Math.max(col * 1.5D, 0.25D),
                    Math.max(col * 0.5D, 0.25D)
                );
            }
        }

        public Vec3 getInterpPos(float partialTicks) {
            float scale = (float) EntityNukeTorex.this.getScale();
            Vec3 base = Vec3.createVectorHelper(
                this.prevPosX + (this.posX - this.prevPosX) * partialTicks,
                this.prevPosY + (this.posY - this.prevPosY) * partialTicks,
                this.prevPosZ + (this.posZ - this.prevPosZ) * partialTicks
            );

            if (this.type != TorexType.SHOCK) {
                base.xCoord = (base.xCoord - EntityNukeTorex.this.posX) * scale +
                    EntityNukeTorex.this.posX;
                base.yCoord = (base.yCoord - EntityNukeTorex.this.posY) * scale +
                    EntityNukeTorex.this.posY;
                base.zCoord = (base.zCoord - EntityNukeTorex.this.posZ) * scale +
                    EntityNukeTorex.this.posZ;
            }
            return base;
        }

        public Vec3 getInterpColor(float partialTicks) {
            if (this.type == TorexType.CONDENSATION) {
                return Vec3.createVectorHelper(1.0D, 1.0D, 1.0D);
            }

            double greying = EntityNukeTorex.this.getGreying();
            if (this.type == TorexType.RING) {
                greying++;
            }

            double r = (this.prevColor.xCoord +
                (this.color.xCoord - this.prevColor.xCoord) * partialTicks) * greying;
            double g = (this.prevColor.yCoord +
                (this.color.yCoord - this.prevColor.yCoord) * partialTicks) * greying;
            double b = (this.prevColor.zCoord +
                (this.color.zCoord - this.prevColor.zCoord) * partialTicks) * greying;

            return Vec3.createVectorHelper(r, g, b);
        }

        public float getAlpha() {
            // 保留原逻辑写法（age/cloudletLife 是 int 运算）
            float alpha = (1.0F - this.age / (float) this.cloudletLife) *
                EntityNukeTorex.this.getAlpha();
            if (this.type == TorexType.CONDENSATION) {
                alpha *= 0.25D;
            }
            return alpha;
        }

        public float getScale() {
            float base = this.startingScale +
                this.age / (float) this.cloudletLife * this.growingScale;
            if (this.type != TorexType.SHOCK) {
                base *= (float) EntityNukeTorex.this.getScale();
            }
            return base;
        }

        public Cloudlet setScale(float start, float grow) {
            this.startingScale = start;
            this.growingScale = grow;
            return this;
        }

        public Cloudlet setMotion(double mult) {
            this.motionMult = mult;
            return this;
        }
    }

    public enum TorexType {
        STANDARD,
        SHOCK,
        RING,
        CONDENSATION
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        // 无持久化
    }

    @Override
    public boolean writeToNBTOptional(NBTTagCompound nbt) {
        return false;
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        this.setDead();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean isInRangeToRenderDist(double distance) {
        return true;
    }

    public static void statFacStandard(World world, double x, double y, double z, float scale) {
        statFac(world, x, y, z, scale, 0);
    }

    public static void statFacBale(World world, double x, double y, double z, float scale) {
        statFac(world, x, y, z, scale, 1);
    }

    public static void statFac(World world, double x, double y, double z, float scale, int type) {
        EntityNukeTorex torex =
            new EntityNukeTorex(world).setScale(
                MathHelper.clamp_float(
                    (float) squirt(scale * 0.01D) * 1.5F,
                    0.5F,
                    5.0F
                )
            );
        torex.setType(type);
        torex.setPosition(x, y, z);
        torex.ignoreFrustumCheck = true;
        world.spawnEntityInWorld(torex);
        setTrackingRange(world, torex, 1000);
    }

    public static void setTrackingRange(World world, Entity e, int range) {
        if (world instanceof WorldServer) {
            WorldServer server = (WorldServer) world;
            EntityTrackerEntry entry = getTrackerEntry(server, e.getEntityId());
            if (entry != null) {
                entry.blocksDistanceThreshold = range;
            }
        }
    }

    public static EntityTrackerEntry getTrackerEntry(WorldServer world, int entityId) {
        EntityTracker entitytracker = world.getEntityTracker();
        IntHashMap map = ReflectionHelper.getPrivateValue(EntityTracker.class, entitytracker, new String[] { "trackedEntityIDs", "field_72794_c" });
        EntityTrackerEntry entry = (EntityTrackerEntry)map.lookup(entityId);
        return entry;
    }

    public static double squirt(double x) {
        return Math.sqrt(x + 1.0D / (x + 2.0D) * (x + 2.0D)) - 1.0D / (x + 2.0D);
    }
}
