package mcheli.weapon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Lib;
import mcheli.MCH_PlayerViewHandler;
import mcheli.MCH_RayTracer;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.tank.MCH_EntityTank;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class MCH_WeaponASMissile extends MCH_WeaponBase {

    private int lockCount;
    private int prevLockCount;

    // Matches any waypoint filename that contains "GPS" (case-insensitive)
// and ends with _x,y,z.json (coords are extracted from the final underscore group).
    private static final Pattern JM_GPS_COORDS = Pattern.compile(
            "^.*GPS.*_(-?\\d+),(-?\\d+),(-?\\d+)\\.json$",
            Pattern.CASE_INSENSITIVE
    );
    @SideOnly(Side.CLIENT)
    private Vec3 tryGetGpsFromJourneyMapWaypoint(EntityPlayer player) {
        try {
            File mcDir = Minecraft.getMinecraft().mcDataDir;
            File jmRoot = new File(mcDir, "journeymap/data");
            if (!jmRoot.exists() || !jmRoot.isDirectory()) return null;

            Vec3 v = findNewestEnabledGpsWaypointCoordsInTree(new File(jmRoot, "mp"), player);
            if (v != null) return v;

            return findNewestEnabledGpsWaypointCoordsInTree(new File(jmRoot, "sp"), player);

        } catch (Exception e) {
            System.out.println("[GPS-JM] Exception: " + e);
            return null;
        }
    }

    private int extractInt(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return 0;

        int colon = json.indexOf(":", idx);
        if (colon < 0) return 0;

        int start = colon + 1;
        while (start < json.length() &&
                (json.charAt(start) == ' ' || json.charAt(start) == '\n')) {
            start++;
        }

        int end = start;
        while (end < json.length() &&
                (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }

        return Integer.parseInt(json.substring(start, end));
    }
    @SideOnly(Side.CLIENT)
    private boolean isJourneyMapWaypointEnabled(File jsonFile) {
        try {
            // Simple & fast: read file and look for `"enable": false`
            // (JourneyMap writes it exactly like that)
            String json = new String(java.nio.file.Files.readAllBytes(jsonFile.toPath()), "UTF-8");
            return !json.contains("\"enable\": false");
        } catch (Exception e) {
            System.out.println("[GPS-JM] Failed reading enable flag: " + jsonFile.getName() + " err=" + e);
            return false;
        }
    }

    @SideOnly(Side.CLIENT)
    private File[] collectGpsWaypointFilesRecursive(File node) {
        java.util.ArrayList<File> out = new java.util.ArrayList<File>();
        collectGpsWaypointFilesRecursive0(node, out);
        return out.toArray(new File[out.size()]);
    }

    @SideOnly(Side.CLIENT)
    private void collectGpsWaypointFilesRecursive0(File node, java.util.ArrayList<File> out) {
        if (node == null || !node.exists()) return;

        if (node.isDirectory()) {
            File[] kids = node.listFiles();
            if (kids == null) return;
            for (File k : kids) collectGpsWaypointFilesRecursive0(k, out);
            return;
        }

        // Only care about JSON files in any folder (but practically waypoints/)
        if (!node.isFile()) return;
        if (!node.getName().toLowerCase().endsWith(".json")) return;

        // Must match your GPS naming rule
        if (JM_GPS_COORDS.matcher(node.getName()).matches()) {
            out.add(node);
        }
    }
    @SideOnly(Side.CLIENT)
    private Vec3 findNewestEnabledGpsWaypointCoordsInTree(File root, EntityPlayer player) {
        if (root == null || !root.exists() || !root.isDirectory()) return null;

        File[] gpsFiles = collectGpsWaypointFilesRecursive(root);
        if (gpsFiles == null || gpsFiles.length == 0) return null;

        // Newest first (deterministic tie-break by name)
        java.util.Arrays.sort(gpsFiles, new java.util.Comparator<File>() {
            @Override public int compare(File a, File b) {
                long da = a.lastModified();
                long db = b.lastModified();
                if (da == db) return a.getName().compareToIgnoreCase(b.getName());
                return (da < db) ? 1 : -1;
            }
        });

        // Walk newest -> oldest until we find an enabled one
        for (File f : gpsFiles) {
            if (!isJourneyMapWaypointEnabled(f)) {
                System.out.println("[GPS-JM] Skipping disabled waypoint: " + f.getName());
                continue;
            }

            // Read coords from JSON (recommended)
            try {
                String json = new String(java.nio.file.Files.readAllBytes(f.toPath()), "UTF-8");
                int x = extractInt(json, "\"x\"");
                int y = extractInt(json, "\"y\"");
                int z = extractInt(json, "\"z\"");

                System.out.println("[GPS-JM] Using ENABLED waypoint: " + f.getName()
                        + " => xyz=" + x + "," + y + "," + z);

                return W_WorldFunc.getWorldVec3(player.worldObj, x + 0.5, y + 0.5, z + 0.5);

            } catch (Exception e) {
                System.out.println("[GPS-JM] Failed reading coords: " + f.getName() + " err=" + e);
                // keep searching older files
            }
        }

        // None enabled / none readable
        System.out.println("[GPS-JM] No enabled GPS waypoint found under: " + root.getAbsolutePath());
        return null;
    }

    @SideOnly(Side.CLIENT)
    private File findNewestGpsWaypointFileRecursive(File node) {
        if (node == null || !node.exists()) return null;

        if (node.isDirectory()) {
            File[] kids = node.listFiles();
            if (kids == null || kids.length == 0) return null;

            // If we are in a "waypoints" directory, evaluate files directly (fast path)
            if ("waypoints".equalsIgnoreCase(node.getName())) {
                // Deterministic: newest modified GPS file wins
                Arrays.sort(kids, new Comparator<File>() {
                    @Override public int compare(File a, File b) {
                        long da = a.lastModified();
                        long db = b.lastModified();
                        if (da == db) return a.getName().compareToIgnoreCase(b.getName());
                        return (da < db) ? 1 : -1; // newest first
                    }
                });

                for (File f : kids) {
                    if (f != null && f.isFile() && JM_GPS_COORDS.matcher(f.getName()).matches()) {
                        return f; // newest matching file
                    }
                }
                return null;
            }

            // Otherwise recurse and keep the newest match
            File best = null;
            long bestTime = -1;

            for (File k : kids) {
                File found = findNewestGpsWaypointFileRecursive(k);
                if (found != null) {
                    long t = found.lastModified();
                    if (best == null || t > bestTime) {
                        best = found;
                        bestTime = t;
                    }
                }
            }
            return best;
        }

        return null;
    }
    public MCH_WeaponASMissile(World world, Vec3 position, float yaw, float pitch, String name, MCH_WeaponInfo weaponInfo) {
        super(world, position, yaw, pitch, name, weaponInfo);
        this.acceleration = 3.0F;  // 加速度
        this.explosionPower = 9;   // 爆炸威力
        this.power = 40;           // 武器威力
        this.interval = -350;      // 射击间隔
    }

    public boolean isCooldownCountReloadTime() {
        return true;
    }


    public void update(int countWait) {
        super.update(countWait);
        if (this.worldObj != null && this.worldObj.isRemote) {
            if (this.lockCount != this.prevLockCount) {
                this.prevLockCount = this.lockCount;
            } else {
                this.lockCount = this.prevLockCount = 0;
            }
        }
    }

    @Override
    public boolean shot(MCH_WeaponParam prm) {
        if (getInfo().isGPSMissile) {
            if (!super.worldObj.isRemote) {
                this.playSound(prm.entity);
                float yaw, pitch;
                if (prm.entity instanceof MCH_EntityTank) {
                    MCH_EntityTank tank = (MCH_EntityTank) prm.entity;
                    yaw = prm.user.rotationYaw;
                    pitch = prm.user.rotationPitch;
                    yaw += prm.randYaw;
                    pitch += prm.randPitch;
                    int wid = tank.getCurrentWeaponID(prm.user);
                    MCH_AircraftInfo.Weapon w = tank.getAcInfo().getWeaponById(wid);
                    float minPitch = w == null ? tank.getAcInfo().minRotationPitch : w.minPitch;
                    float maxPitch = w == null ? tank.getAcInfo().maxRotationPitch : w.maxPitch;
                    float playerYaw = MathHelper.wrapAngleTo180_float(tank.getRotYaw() - yaw);
                    float playerPitch = tank.getRotPitch() * MathHelper.cos((float) (playerYaw * Math.PI / 180.0D))
                        + -tank.getRotRoll() * MathHelper.sin((float) (playerYaw * Math.PI / 180.0D));
                    float playerYawRel = MathHelper.wrapAngleTo180_float(yaw - tank.getRotYaw());
                    float yawLimit = (w == null ? 360F : w.maxYaw);
                    float relativeYaw = MCH_Lib.RNG(playerYawRel, -yawLimit, yawLimit);
                    yaw = MathHelper.wrapAngleTo180_float(tank.getRotYaw() + relativeYaw);
                    pitch = MCH_Lib.RNG(pitch, playerPitch + minPitch, playerPitch + maxPitch);
                    pitch = MCH_Lib.RNG(pitch, -90.0F, 90.0F);
                } else {
                    yaw = prm.rotYaw;
                    pitch = prm.rotPitch;
                }
                double tX = -MathHelper.sin(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F);
                double tZ = MathHelper.cos(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F);
                double tY = -MathHelper.sin(pitch / 180.0F * 3.1415927F);
                MCH_EntityASMissile missile = new MCH_EntityASMissile(this.worldObj, prm.posX, prm.posY, prm.posZ, tX, tY, tZ, yaw, pitch, this.acceleration);
                missile.setInfoByName(this.name);
                missile.setParameterFromWeapon(this, prm.entity, prm.user);

                MCH_GPSPosition gpsPosition;
                if ((gpsPosition = MCH_GPSPosition.get(prm.user)) != null) {
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
                this.worldObj.spawnEntityInWorld(missile);
                playSound(prm.entity);
            } else {
                super.optionParameter1 = this.getCurrentMode();
                MCH_PlayerViewHandler.applyRecoil(getInfo().getRecoilPitch(), getInfo().getRecoilYaw(), getInfo().recoilRecoverFactor);
            }
            return true;
        } else {
            float yaw = prm.user.rotationYaw;
            float pitch = prm.user.rotationPitch;
            double targetX = -MathHelper.sin(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);
            double targetZ = MathHelper.cos(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);
            double targetY = -MathHelper.sin(pitch / 180.0F * (float) Math.PI);
            double dist = MathHelper.sqrt_double(targetX * targetX + targetY * targetY + targetZ * targetZ);
            double maxDist = 1500.0;
            double segmentLength = 100.0;
            int numSegments = (int) (maxDist / segmentLength);
            targetX = targetX * maxDist / dist;
            targetY = targetY * maxDist / dist;
            targetZ = targetZ * maxDist / dist;
            Vec3 src = W_WorldFunc.getWorldVec3(this.worldObj, prm.entity.posX, prm.entity.posY + prm.entity.getEyeHeight(), prm.entity.posZ);
            MovingObjectPosition hitResult = null;
            for (int i = 1; i <= numSegments; i++) {
                Vec3 currentDst = W_WorldFunc.getWorldVec3(this.worldObj,
                    prm.entity.posX + targetX * i / numSegments,
                    prm.entity.posY + prm.entity.getEyeHeight() + targetY * i / numSegments,
                    prm.entity.posZ + targetZ * i / numSegments);
                List<MovingObjectPosition> hitResults = MCH_RayTracer.rayTraceAllBlocks(this.worldObj, src, currentDst, false, true, true);
                if (hitResults != null && !hitResults.isEmpty()) {
                    hitResult = hitResults.get(0);
                    break;
                }
                src = currentDst;
            }
            if (hitResult == null) {
                hitResult = new MovingObjectPosition(null, src.addVector(targetX, targetY, targetZ));  // 使用目标点作为默认值
            }
            if (!this.worldObj.isRemote) {
                MCH_EntityASMissile missile = new MCH_EntityASMissile(this.worldObj, prm.posX, prm.posY, prm.posZ, targetX, targetY, targetZ, yaw, pitch, this.acceleration);
                missile.setInfoByName(this.name);
                missile.setParameterFromWeapon(this, prm.entity, prm.user);
                missile.targetPosX = hitResult.hitVec.xCoord;
                missile.targetPosY = hitResult.hitVec.yCoord;
                missile.targetPosZ = hitResult.hitVec.zCoord;
                missile.originTargetPosX = hitResult.hitVec.xCoord;
                missile.originTargetPosY = hitResult.hitVec.yCoord;
                missile.originTargetPosZ = hitResult.hitVec.zCoord;
                missile.targeting = true;
                this.worldObj.spawnEntityInWorld(missile);
                playSound(prm.entity);
            } else {
                if (prm.user instanceof EntityPlayer) {
                    MCH_GPSPosition.set(hitResult.hitVec.xCoord, hitResult.hitVec.yCoord, hitResult.hitVec.zCoord, true, prm.user);
                }
            }
            return true;
        }
    }

    @SideOnly(Side.CLIENT)
    public void clientLock(MCH_WeaponParam prm) {
        // --- JourneyMap GPS waypoint override ---
        if (prm.user instanceof EntityPlayer) {
            Vec3 jm = tryGetGpsFromJourneyMapWaypoint((EntityPlayer)prm.user);
            if (jm != null) {
                MCH_GPSPosition.set(jm.xCoord, jm.yCoord, jm.zCoord, true, prm.user);
                return; // Skip look-at-block GPS marking
            }
        }
        Minecraft.getMinecraft().getSoundHandler().playSound(
            new PositionedSoundRecord(new ResourceLocation("mcheli:mark"), 10.0F, 1.0F,
                (float) prm.user.posX, (float) prm.user.posY, (float) prm.user.posZ));
        float yaw = prm.user.rotationYaw;
        float pitch = prm.user.rotationPitch;
        if(Minecraft.getMinecraft().gameSettings.thirdPersonView != 0) {
            EntityLivingBase e = Minecraft.getMinecraft().renderViewEntity;
            yaw = e.rotationYaw;
            pitch = e.rotationPitch;
        }
        double targetX = -MathHelper.sin(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);
        double targetZ = MathHelper.cos(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);
        double targetY = -MathHelper.sin(pitch / 180.0F * (float) Math.PI);
        double dist = MathHelper.sqrt_double(targetX * targetX + targetY * targetY + targetZ * targetZ);
        double maxDist = 1500.0;
        double segmentLength = 100.0;
        int numSegments = (int) (maxDist / segmentLength);
        double posX = RenderManager.renderPosX;
        double posY = RenderManager.renderPosY;
        double posZ = RenderManager.renderPosZ;
        targetX = targetX * maxDist / dist;
        targetY = targetY * maxDist / dist;
        targetZ = targetZ * maxDist / dist;
        Vec3 src = W_WorldFunc.getWorldVec3(this.worldObj, posX, posY, posZ);
        MovingObjectPosition hitResult = null;
        for (int i = 1; i <= numSegments; i++) {
            Vec3 currentDst = W_WorldFunc.getWorldVec3(this.worldObj,
                posX + targetX * i / numSegments,
                posY + targetY * i / numSegments,
                posZ + targetZ * i / numSegments);
            List<MovingObjectPosition> hitResults = MCH_RayTracer.rayTraceAllBlocks(this.worldObj, src, currentDst, false, true, true);
            if (hitResults != null && !hitResults.isEmpty()) {
                hitResult = hitResults.get(0);
                break;
            }
            src = currentDst;
        }
        if (hitResult == null) {
            hitResult = new MovingObjectPosition(null, src.addVector(targetX, targetY, targetZ));
        }
        MCH_GPSPosition.set(hitResult.hitVec.xCoord, hitResult.hitVec.yCoord, hitResult.hitVec.zCoord, true, prm.user);
    }

    @Override
    public boolean lock(MCH_WeaponParam prm) {
        if (super.worldObj.isRemote) {
            if (lockCount <= weaponInfo.lockTime) {
                if (lockCount == 1) {
                }
                lockCount++;
                if (lockCount == weaponInfo.lockTime) {
                    lockCount = 0;
                    clientLock(prm);
                }
            }
        }
        return false;
    }

    public float getLockTime() {
        return (float) lockCount / weaponInfo.lockTime;
    }
}
