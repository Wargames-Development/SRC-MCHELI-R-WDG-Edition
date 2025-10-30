package mcheli.weapon;

import mcheli.MCH_Explosion;
import mcheli.MCH_ExplosionParam;
import mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_WeaponBomb extends MCH_WeaponBase {

    public MCH_WeaponBomb(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
        super(w, v, yaw, pitch, nm, wi);
        super.acceleration = 0.5F;
        super.explosionPower = 9;
        super.power = 35;
        super.interval = -90;
        if (w.isRemote) {
            super.interval -= 10;
        }

    }

    public boolean shot(MCH_WeaponParam prm) {
        // Self-destruct behavior
        if (this.getInfo() != null && this.getInfo().destruct) {
            if (prm.entity instanceof MCH_EntityAircraft) {
                MCH_EntityAircraft e1 = (MCH_EntityAircraft) prm.entity;
                // UAV with no seats: explode + self-destruct
                if (e1.isUAV() && e1.getSeatNum() == 0) {
                    if (!super.worldObj.isRemote) {
                        MCH_ExplosionParam p = MCH_ExplosionParam.builder()
                            .exploder(null) // Same as original (no explicit exploder)
                            .player(prm.user instanceof EntityPlayer ? (EntityPlayer) prm.user : null)
                            .x(e1.posX).y(e1.posY).z(e1.posZ)
                            .size((float) this.getInfo().explosion)
                            .sizeBlock((float) this.getInfo().explosionBlock)
                            .isPlaySound(true)
                            .isSmoking(true)
                            .isFlaming(this.getInfo().flaming)
                            .isDestroyBlock(true)
                            .countSetFireEntity(0)
                            .isInWater(false)
                            .damageVsPlayer(getInfo().explosionDamageVsPlayer)
                            .damageVsLiving(getInfo().explosionDamageVsLiving)
                            .damageVsPlane(getInfo().explosionDamageVsPlane)
                            .damageVsHeli(getInfo().explosionDamageVsHeli)
                            .damageVsTank(getInfo().explosionDamageVsTank)
                            .damageVsVehicle(getInfo().explosionDamageVsVehicle)
                            .damageVsShip(getInfo().explosionDamageVsShip)
                            .build();
                        MCH_Explosion.newExplosion(super.worldObj, p);
                        this.playSound(prm.entity);
                    }
                    e1.destruct();
                }
                // Non-UAV: explosion + additional damage
                if (!e1.isUAV()) {
                    if (!super.worldObj.isRemote) {
                        MCH_ExplosionParam p = MCH_ExplosionParam.builder()
                            .exploder(null)
                            .player(prm.user instanceof EntityPlayer ? (EntityPlayer) prm.user : null)
                            .x(e1.posX).y(e1.posY).z(e1.posZ)
                            .size((float) this.getInfo().explosion)
                            .sizeBlock((float) this.getInfo().explosionBlock)
                            .isPlaySound(true)
                            .isSmoking(true)
                            .isFlaming(this.getInfo().flaming)
                            .isDestroyBlock(true)
                            .countSetFireEntity(0)
                            .isInWater(false)
                            .damageVsPlayer(getInfo().explosionDamageVsPlayer)
                            .damageVsLiving(getInfo().explosionDamageVsLiving)
                            .damageVsPlane(getInfo().explosionDamageVsPlane)
                            .damageVsHeli(getInfo().explosionDamageVsHeli)
                            .damageVsTank(getInfo().explosionDamageVsTank)
                            .damageVsVehicle(getInfo().explosionDamageVsVehicle)
                            .damageVsShip(getInfo().explosionDamageVsShip)
                            .build();
                        MCH_Explosion.newExplosion(super.worldObj, p);
                        this.playSound(prm.entity);
                    }
                    if (prm.user instanceof EntityPlayer) {
                        e1.attackEntityFrom(DamageSource.inWall, 1000000);
                    }
                }
            }
        } else if (!super.worldObj.isRemote) {
            this.playSound(prm.entity);
            MCH_EntityBomb e = new MCH_EntityBomb(super.worldObj, prm.posX, prm.posY, prm.posZ, prm.entity.motionX, prm.entity.motionY, prm.entity.motionZ, prm.entity.rotationYaw, 0.0F, (double) super.acceleration);
            e.setName(super.name);
            e.setParameterFromWeapon(this, prm.entity, prm.user);
            e.motionX = prm.entity.motionX;
            e.motionY = prm.entity.motionY;
            e.motionZ = prm.entity.motionZ;
            super.worldObj.spawnEntityInWorld(e);
        }

        return true;
    }
}
