package mcheli.weapon;

import mcheli.MCH_Config;
import mcheli.particles.MCH_ParticleParam;
import mcheli.particles.MCH_ParticlesUtil;
import mcheli.wrapper.W_MovingObjectPosition;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.init.Blocks;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class MCH_EntityMarkerRocket extends MCH_EntityBaseBullet {

    public int countDown;


    public MCH_EntityMarkerRocket(World par1World) {
        super(par1World);
        this.setMarkerStatus(0);
        this.countDown = 0;
    }

    public MCH_EntityMarkerRocket(World par1World, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {
        super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
        this.setMarkerStatus(0);
        this.countDown = 0;
    }

    protected void entityInit() {
        super.entityInit();
        this.getDataWatcher().addObject(28, (byte) 0);
    }

    public int getMarkerStatus() {
        return this.getDataWatcher().getWatchableObjectByte(28);
    }

    public void setMarkerStatus(int n) {
        if (!super.worldObj.isRemote) {
            this.getDataWatcher().updateObject(28, (byte) n);
        }

    }

    public boolean attackEntityFrom(DamageSource par1DamageSource, float par2) {
        return false;
    }

    public void onUpdate() {
        int status = this.getMarkerStatus();
        if (super.worldObj.isRemote) {
            if (this.getInfo() == null) {
                super.onUpdate();
            }

            if (this.getInfo() != null && !this.getInfo().disableSmoke && status != 0) {
                if (status == 1) {
                    super.onUpdate();
                    this.spawnExplosionParticle(this.getInfo().trajectoryParticleName, 3, 5.0F * this.getInfo().smokeSize * 0.5F);
                } else {
                    float num = super.rand.nextFloat() * 0.3F;
                    this.spawnParticle("explode", 5, (float) (10 + super.rand.nextInt(4)), super.rand.nextFloat() * 0.2F + 0.8F, num, num, (super.rand.nextFloat() - 0.5F) * 0.7F, 0.3F + super.rand.nextFloat() * 0.3F, (super.rand.nextFloat() - 0.5F) * 0.7F);
                }
            }
        } else if (status != 0 && !this.isInWater()) {
            if (status == 1) {
                super.onUpdate();
            } else if (this.countDown > 0) {
                --this.countDown;
                if (this.countDown == 40) {
                    int var5 = getInfo().markerRocketSpawnNum;

                    for (int i = 0; i < var5; ++i) {
                        MCH_EntityBomb e = new MCH_EntityBomb(super.worldObj,
                                super.posX + (double) ((super.rand.nextFloat() - 0.5F) * getInfo().markerRocketSpawnDiff),
                                getInfo().markerRocketSpawnHeight + super.rand.nextFloat() * 10.0F + (float) (i * 30),
                                super.posZ + (double) ((super.rand.nextFloat() - 0.5F) * getInfo().markerRocketSpawnDiff),
                                0.0D, -0.5D, 0.0D, 0.0F, 90.0F, getInfo().markerRocketSpawnSpeed);
                        e.setName(this.getName());
                        e.explosionPower = this.getInfo().explosion;
                        e.explosionPowerInWater = this.getInfo().explosionInWater;
                        e.setPower(getInfo().power);
                        e.piercing = 0;
                        e.shootingAircraft = super.shootingAircraft;
                        e.shootingEntity = super.shootingEntity;
                        super.worldObj.spawnEntityInWorld(e);
                    }
                }
            } else {
                this.setDead();
            }
        } else {
            this.setDead();
        }

    }

    public void spawnParticle(String name, int num, float size, float r, float g, float b, float mx, float my, float mz) {
        if (super.worldObj.isRemote) {
            if (name.isEmpty() || num < 1 || num > 50) {
                return;
            }

            double x = (super.posX - super.prevPosX) / (double) num;
            double y = (super.posY - super.prevPosY) / (double) num;
            double z = (super.posZ - super.prevPosZ) / (double) num;

            for (int i = 0; i < num; ++i) {
                MCH_ParticleParam prm = new MCH_ParticleParam(super.worldObj, "smoke", super.prevPosX + x * (double) i, super.prevPosY + y * (double) i, super.prevPosZ + z * (double) i);
                prm.motionX = mx;
                prm.motionY = my;
                prm.motionZ = mz;
                prm.size = size + super.rand.nextFloat();
                prm.setColor(1.0F, r, g, b);
                prm.isEffectWind = true;
                MCH_ParticlesUtil.spawnParticle(prm);
            }
        }

    }

    @Override
    public void onImpact(MovingObjectPosition m, float damageFactor) {
        // Execute impact handling only on the server side
        if (!super.worldObj.isRemote) {
            // If an entity was hit: activate marker state and attach the rocket to the entity
            if (m.entityHit != null || W_MovingObjectPosition.isHitTypeEntity(m)) {
                // Set marker status to (2) to begin countdown and trigger support bomb drop
                this.setMarkerStatus(2);

                // Move the rocket above the entity (approximately 1.1 blocks higher)
                double px = m.entityHit.posX;
                double py = m.entityHit.posY + (double) m.entityHit.height + 1.1D;
                double pz = m.entityHit.posZ;
                this.setPosition(px, py, pz);

                // Update previous frame positions to ensure particle effects render correctly
                this.prevPosX = this.posX;
                this.prevPosY = this.posY;
                this.prevPosZ = this.posZ;

                // Start countdown — the bomb will be dropped during onUpdate()
                this.countDown = 100;
            }
            // If no entity was hit, handle block impact logic instead
            else {
                int x = m.blockX;
                int y = m.blockY;
                int z = m.blockZ;
                switch (m.sideHit) {
                    case 0:
                        --y;
                        break;
                    case 1:
                        ++y;
                        break;
                    case 2:
                        --z;
                        break;
                    case 3:
                        ++z;
                        break;
                    case 4:
                        --x;
                        break;
                    case 5:
                        ++x;
                        break;
                }

                // When hitting an air block, create fire and attach the marker rocket
                if (super.worldObj.isAirBlock(x, y, z)) {
                    if (MCH_Config.Explosion_FlamingBlock.prmBool) {
                        W_WorldFunc.setBlock(super.worldObj, x, y, z, Blocks.fire);
                    }

                    int noAirBlockCount = 0;
                    for (int i = y + 1; i < 256; ++i) {
                        if (!super.worldObj.isAirBlock(x, i, z)) {
                            ++noAirBlockCount;
                            if (noAirBlockCount >= 5) {
                                break;
                            }
                        }
                    }

                    if (noAirBlockCount < 5) {
                        this.setMarkerStatus(2);
                        this.setPosition((double) x + 0.5D, (double) y + 1.1D, (double) z + 0.5D);
                        super.prevPosX = super.posX;
                        super.prevPosY = super.posY;
                        super.prevPosZ = super.posZ;
                        this.countDown = 100;
                    } else {
                        this.setDead();
                    }
                } else {
                    this.setDead();
                }
            }
        }
    }


    public MCH_BulletModel getDefaultBulletModel() {
        return MCH_DefaultBulletModels.Rocket;
    }
}