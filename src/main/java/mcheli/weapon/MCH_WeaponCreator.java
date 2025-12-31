package mcheli.weapon;

import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.Objects;

public class MCH_WeaponCreator {

    public static MCH_EntityBaseBullet createEntity(String type, World w, double posX, double posY, double posZ, double mX, double mY, double mZ, float rotationYaw, float rotationPitch, double speed) {
        MCH_EntityBaseBullet e;
        switch (type.toLowerCase()) {
            case "aamissile": {
                e = new MCH_EntityAAMissile(w, posX, posY, posZ, mX, mY, mZ, rotationYaw, rotationPitch, speed);
                break;
            }
            case "atmissile": {
                e = new MCH_EntityATMissile(w, posX, posY, posZ, mX, mY, mZ, rotationYaw, rotationPitch, speed);
                break;
            }
            case "asmissile": {
                e = new MCH_EntityASMissile(w, posX, posY, posZ, mX, mY, mZ, rotationYaw, rotationPitch, speed);
                break;
            }
            case "tvmissile": {
                e = new MCH_EntityTvMissile(w, posX, posY, posZ, mX, mY, mZ, rotationYaw, rotationPitch, speed);
                break;
            }
            case "mkrocket": {
                e = new MCH_EntityMarkerRocket(w, posX, posY, posZ, mX, mY, mZ, rotationYaw, rotationPitch, speed);
                break;
            }
            case "machinegun1":
            case "machinegun2":
            case "railgun": {
                e = new MCH_EntityBullet(w, posX, posY, posZ, mX, mY, mZ, rotationYaw, rotationPitch, speed);
                break;
            }
            case "bomb": {
                e = new MCH_EntityBomb(w, posX, posY, posZ, mX, mY, mZ, rotationYaw, rotationPitch, speed);
                break;
            }
            case "rocket":
            default: {
                e = new MCH_EntityRocket(w, posX, posY, posZ, mX, mY, mZ, rotationYaw, rotationPitch, speed);
                break;
            }
        }
        return e;
    }


    public static void setEntityInfo(MCH_EntityBaseBullet e, Entity user) {
        if(e == null || e.getInfo() == null) {
            return;
        }
        switch (e.getInfo().type.toLowerCase()) {
            case "aamissile": {
                break;
            }
            case "atmissile": {
                break;
            }
            case "asmissile": {
                MCH_GPSPosition gpsPosition;
                MCH_EntityASMissile missile = (MCH_EntityASMissile) e;
                if(missile.getInfo().isGPSMissile) {
                    if ((gpsPosition = MCH_GPSPosition.get(user)) != null) {
                        if (gpsPosition.isActive) {
                            missile.targetPosX = gpsPosition.x;
                            missile.targetPosY = gpsPosition.y;
                            missile.targetPosZ = gpsPosition.z;
                            missile.originTargetPosX = gpsPosition.x;
                            missile.originTargetPosY = gpsPosition.y;
                            missile.originTargetPosZ = gpsPosition.z;
                            missile.targeting = true;
                        }
                    }
                }
                break;
            }
            case "tvmissile": {
                break;
            }
            case "mkrocket": {
                ((MCH_EntityMarkerRocket) e).setMarkerStatus(1);
                break;
            }
            case "machinegun1":
            case "machinegun2":
            case "railgun":
            case "bomb":
            case "rocket":
            default: {
                break;
            }
        }
    }

    public static MCH_WeaponBase createWeapon(World w, String weaponName, Vec3 v, float yaw, float pitch, MCH_IEntityLockChecker lockChecker, boolean onTurret) {
        MCH_WeaponInfo info = MCH_WeaponInfoManager.get(weaponName);
        if (info != null && !Objects.equals(info.type, "")) {
            MCH_WeaponBase weapon = null;
            if (info.type.compareTo("machinegun1") == 0) {
                weapon = new MCH_WeaponMachineGun1(w, v, yaw, pitch, weaponName, info);
            }

            if (info.type.compareTo("machinegun2") == 0) {
                weapon = new MCH_WeaponMachineGun2(w, v, yaw, pitch, weaponName, info);
            }

            if (info.type.compareTo("railgun") == 0) {
                weapon = new MCH_WeaponRailgun(w, v, yaw, pitch, weaponName, info);
            }

            if (info.type.compareTo("laser") == 0) {
                weapon = new MCH_WeaponLaser(w, v, yaw, pitch, weaponName, info);
            }

            if (info.type.compareTo("tvmissile") == 0) {
                weapon = new MCH_WeaponTvMissile(w, v, yaw, pitch, weaponName, info);
            }

            if (info.type.compareTo("torpedo") == 0) {
                weapon = new MCH_WeaponTorpedo(w, v, yaw, pitch, weaponName, info);
            }

            if (info.type.compareTo("cas") == 0) {
                weapon = new MCH_WeaponCAS(w, v, yaw, pitch, weaponName, info);
            }

            if (info.type.compareTo("rocket") == 0) {
                weapon = new MCH_WeaponRocket(w, v, yaw, pitch, weaponName, info);
            }

            if (info.type.compareTo("asmissile") == 0) {
                weapon = new MCH_WeaponASMissile(w, v, yaw, pitch, weaponName, info);
            }

            if (info.type.compareTo("aamissile") == 0) {
                weapon = new MCH_WeaponAAMissile(w, v, yaw, pitch, weaponName, info);
            }

            if (info.type.compareTo("atmissile") == 0) {
                weapon = new MCH_WeaponATMissile(w, v, yaw, pitch, weaponName, info);
            }

            if (info.type.compareTo("bomb") == 0) {
                weapon = new MCH_WeaponBomb(w, v, yaw, pitch, weaponName, info);
            }

            if (info.type.compareTo("mkrocket") == 0) {
                weapon = new MCH_WeaponMarkerRocket(w, v, yaw, pitch, weaponName, info);
            }

            if (info.type.compareTo("dummy") == 0) {
                weapon = new MCH_WeaponDummy(w, v, yaw, pitch, weaponName, info);
            }

            if (info.type.compareTo("smoke") == 0) {
                weapon = new MCH_WeaponSmoke(w, v, yaw, pitch, weaponName, info);
            }

            if (info.type.compareTo("dispenser") == 0) {
                weapon = new MCH_WeaponDispenser(w, v, yaw, pitch, weaponName, info);
            }

            if (info.type.compareTo("targetingpod") == 0) {
                weapon = new MCH_WeaponTargetingPod(w, v, yaw, pitch, weaponName, info);
            }

            if (weapon != null) {
                weapon.displayName = info.displayName;
                weapon.power = info.power;
                weapon.acceleration = info.acceleration;
                weapon.explosionPower = info.explosion;
                weapon.explosionPowerInWater = info.explosionInWater;
                weapon.interval = info.delay;
                weapon.setLockCountMax(info.lockTime);
                weapon.setLockChecker(lockChecker);
                weapon.numMode = info.modeNum;
                weapon.piercing = info.piercing;
                weapon.heatCount = info.heatCount;
                weapon.onTurret = onTurret;
                if (info.maxHeatCount > 0 && weapon.heatCount < 2) {
                    weapon.heatCount = 2;
                }

                if (w.isRemote) {
                    if (weapon.interval < 4) {
                        ++weapon.interval;
                    } else if (weapon.interval < 7) {
                        weapon.interval += 2;
                    }

//                    else if (((MCH_WeaponBase) weapon).interval < 10) {
//                        ((MCH_WeaponBase) weapon).interval += 3;
//                    } else if (((MCH_WeaponBase) weapon).interval < 20) {
//                        ((MCH_WeaponBase) weapon).interval += 6;
//                    } else {
//                        ((MCH_WeaponBase) weapon).interval += 10;
//                        if (((MCH_WeaponBase) weapon).interval >= 40) {
//                            ((MCH_WeaponBase) weapon).interval = -((MCH_WeaponBase) weapon).interval;
//                        }
//                    }

                    ++weapon.heatCount;
                    weapon.cartridge = info.cartridge;
                }

                weapon.modifyCommonParameters();
            }

            return weapon;
        } else {
            return null;
        }
    }

}
