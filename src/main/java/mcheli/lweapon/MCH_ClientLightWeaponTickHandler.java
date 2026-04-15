package mcheli.lweapon;

import mcheli.*;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.gltd.MCH_EntityGLTD;
import mcheli.weapon.MCH_IEntityLockChecker;
import mcheli.weapon.MCH_IGuidanceSystem;
import mcheli.weapon.MCH_LaserGuidanceSystem;
import mcheli.weapon.MCH_WeaponBase;
import mcheli.weapon.MCH_WeaponCreator;
import mcheli.weapon.MCH_GPSPosition;
import mcheli.weapon.MCH_WeaponGuidanceSystem;
import mcheli.weapon.MCH_WeaponParam;
import mcheli.wrapper.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class MCH_ClientLightWeaponTickHandler extends MCH_ClientTickHandlerBase {

    public static FloatBuffer screenPos = BufferUtils.createFloatBuffer(3);
    public static FloatBuffer matModel = BufferUtils.createFloatBuffer(16);
    public static FloatBuffer matProjection = BufferUtils.createFloatBuffer(16);
    public static IntBuffer matViewport = BufferUtils.createIntBuffer(16);
    public static int reloadCount;
    public static int lockonSoundCount;
    public static int weaponMode;
    public static int selectedZoom;
    public static Entity markEntity = null;
    public static Vec3 markPos = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
    public static MCH_WeaponGuidanceSystem gs = new MCH_WeaponGuidanceSystem();
    public static double lockRange = 120.0D;
    protected static MCH_WeaponBase weapon;
    private static FloatBuffer screenPosBB = BufferUtils.createFloatBuffer(3);
    public MCH_Key KeyAttack;
    public MCH_Key KeyUseWeapon;
    public MCH_Key KeySwWeaponMode;
    public MCH_Key KeyZoom;
    public MCH_Key KeyCameraMode;
    public MCH_Key[] Keys;
    protected boolean isHeldItem = false;
    protected boolean isBeforeHeldItem = false;
    protected EntityPlayer prevThePlayer = null;
    protected ItemStack prevItemStack = null;
    private int laserAimKeepTicks = 0;
    private boolean fovZoomActive = false;
    private float baseFov = 70.0F;


    public MCH_ClientLightWeaponTickHandler(Minecraft minecraft, MCH_Config config) {
        super(minecraft);
        this.updateKeybind(config);
        gs.canLockInAir = false;
        gs.canLockOnGround = false;
        gs.canLockInWater = false;
        gs.setLockCountMax(40);
        gs.lockRange = 120.0D;
        lockonSoundCount = 0;
        this.initWeaponParam((EntityPlayer) null);
    }

    public static MCH_WeaponBase getCurrentWeapon() {
        return weapon;
    }

    public static void markEntity(Entity entity, double x, double y, double z) {
        if (gs != null && gs.getLockingEntity() == entity) {
            GL11.glGetFloat(2982, matModel);
            GL11.glGetFloat(2983, matProjection);
            GL11.glGetInteger(2978, matViewport);
            GLU.gluProject((float) x, (float) y, (float) z, matModel, matProjection, matViewport, screenPos);
            MCH_AircraftInfo i = entity instanceof MCH_EntityAircraft ? ((MCH_EntityAircraft) entity).getAcInfo() : null;
            float w = i != null ? i.markerWidth : (entity.width > entity.height ? entity.width : entity.height);
            float h = i != null ? i.markerHeight : entity.height;
            GLU.gluProject((float) x + w, (float) y + h, (float) z + w, matModel, matProjection, matViewport, screenPosBB);
            markEntity = entity;
        }
    }

    public static Vec3 getMartEntityPos() {
        return gs != null && gs.getLockingEntity() == markEntity && markEntity != null ? Vec3.createVectorHelper((double) screenPos.get(0), (double) screenPos.get(1), (double) screenPos.get(2)) : null;
    }

    public static Vec3 getMartEntityBBPos() {
        return gs != null && gs.getLockingEntity() == markEntity && markEntity != null ? Vec3.createVectorHelper((double) screenPosBB.get(0), (double) screenPosBB.get(1), (double) screenPosBB.get(2)) : null;
    }

    public static int getPotionNightVisionDuration(EntityPlayer player) {
        PotionEffect cpe = player.getActivePotionEffect(Potion.nightVision);
        return player != null && cpe != null ? cpe.getDuration() : 0;
    }

    public void initWeaponParam(EntityPlayer player) {
        reloadCount = 0;
        weaponMode = 0;
        selectedZoom = 0;
    }

    public void updateKeybind(MCH_Config config) {
        this.KeyAttack = new MCH_Key(MCH_Config.KeyAttack.prmInt);
        this.KeyUseWeapon = new MCH_Key(MCH_Config.KeyUseWeapon.prmInt);
        this.KeySwWeaponMode = new MCH_Key(MCH_Config.KeySwWeaponMode.prmInt);
        this.KeyZoom = new MCH_Key(MCH_Config.KeyZoom.prmInt);
        this.KeyCameraMode = new MCH_Key(MCH_Config.KeyCameraMode.prmInt);
        this.Keys = new MCH_Key[]{this.KeyAttack, this.KeyUseWeapon, this.KeySwWeaponMode, this.KeyZoom, this.KeyCameraMode};
    }

    protected void onTick(boolean inGUI) {
        MCH_Key[] player = this.Keys;
        int is = player.length;

        for (int pc = 0; pc < is; ++pc) {
            MCH_Key RELOAD_CNT = player[pc];
            RELOAD_CNT.update();
        }

        this.isBeforeHeldItem = this.isHeldItem;
        EntityClientPlayerMP var6 = super.mc.thePlayer;
        if (this.prevThePlayer == null || this.prevThePlayer != var6) {
            this.initWeaponParam(var6);
            this.prevThePlayer = var6;
        }

        ItemStack var7 = var6 != null ? var6.getHeldItem() : null;
        if (this.laserAimKeepTicks > 0) {
            --this.laserAimKeepTicks;
        }
        if (var6 == null || var6.ridingEntity instanceof MCH_EntityGLTD || var6.ridingEntity instanceof MCH_EntityAircraft) {
            var7 = null;
        }

        if (gs.getLockingEntity() == null) {
            markEntity = null;
        }

        if (var7 != null && var7.getItem() instanceof MCH_ItemLightWeaponBase) {
            MCH_ItemLightWeaponBase var8 = (MCH_ItemLightWeaponBase) var7.getItem();
            if (this.prevItemStack == null || !this.prevItemStack.isItemEqual(var7) && !this.prevItemStack.getUnlocalizedName().equals(var7.getUnlocalizedName())) {
                this.initWeaponParam(var6);
                weapon = MCH_WeaponCreator.createWeapon(var6.worldObj, var8.getWeaponInfoName(var7), Vec3.createVectorHelper(0.0D, 0.0D, 0.0D), 0.0F, 0.0F, (MCH_IEntityLockChecker) null, false);
                if (weapon != null && weapon.getInfo() != null && weapon.getGuidanceSystem() != null) {
                    MCH_IGuidanceSystem guidance = weapon.getGuidanceSystem();
                    if (guidance instanceof MCH_WeaponGuidanceSystem) {
                        gs = (MCH_WeaponGuidanceSystem) guidance;
                    } else if (gs != null) {
                        gs.clearLock();
                    }
                }
                if (weapon != null) {
                    weaponMode = weapon.getCurrentMode();
                }
            }

            if (weapon == null) {
                return;
            }
            weapon.setCurrentMode(weaponMode);
            weaponMode = weapon.getCurrentMode();

            boolean rawAiming = var6.getItemInUseDuration() > 10;
            // Firing can briefly drop itemInUseDuration to 0 for one tick. Keep aiming state through that transition.
            boolean fireTransitionAiming = this.KeyAttack.isKeyPress() && this.shouldUpdateLaserPoint();
            boolean keepAiming = this.laserAimKeepTicks > 0 && this.shouldUpdateLaserPoint();
            boolean aiming = rawAiming || fireTransitionAiming || keepAiming;
            if (this.isSeekerWeapon()) {
                gs.setWorld(var6.worldObj);
                double rangeOverride = var8.getLockRangeOverride();
                gs.lockRange = rangeOverride > 0.0D ? rangeOverride : lockRange;
            }
            float[] zoomLevels = this.getZoomLevels(var8);
            if (aiming) {
                selectedZoom %= zoomLevels.length;
                float zoom = this.normalizeZoomFactor(zoomLevels[selectedZoom]);
                W_Reflection.setCameraZoom(zoom);
                this.applyAimingFovZoom(zoom);
            } else {
                W_Reflection.restoreCameraZoom();
                this.restoreAimingFovZoom();
            }
//add not rpg check here
            // if("rpg7".equalsIgnoreCase(!MCH_ItemLightWeaponBase.getName(player.getHeldItem()))) {

            // }
            if (var7.getItemDamage() < var7.getMaxDamage()) {
                if (aiming) {
                    MCH_WeaponParam lockPrm = this.createWeaponParam(var6, this.getEffectiveWeaponMode(), this.getCurrentLockEntityId());
                    if (this.isSeekerWeapon()) {
                        gs.lock(var6);
                    } else {
                        weapon.lock(lockPrm);
                        this.updateLaserPoint(var6);
                    }
                    int lockCount = this.getWeaponLockCount();
                    int lockCountMax = this.getWeaponLockCountMax();
                    if (lockCount > 0 && lockCountMax > 0) {
                        if (lockonSoundCount > 0) {
                            --lockonSoundCount;
                        } else {
                            lockonSoundCount = 7;
                            lockonSoundCount = (int) ((double) lockonSoundCount * (1.0D - (double) lockCount / (double) lockCountMax));
                            if (lockonSoundCount < 3) {
                                lockonSoundCount = 2;
                            }

                            W_McClient.MOD_playSoundFX("lockon", 1.0F, 1.0F);
                        }
                    }
                } else {
                    W_Reflection.restoreCameraZoom();
                    this.unlockWeapon(var6);
                }

                reloadCount = 0;
            } else {
                lockonSoundCount = 0;
                // Keep laser mark update alive while aiming even if magazine is empty.
                if (aiming) {
                    this.updateLaserPoint(var6);
                }
                if (W_EntityPlayer.hasItem(var6, var8.bullet) && var6.getItemInUseCount() <= 0) {
                    int reloadTick = Math.max(1, var8.getReloadTick());
                    int reloadSoundTick = Math.max(1, reloadTick / 6);
                    if (reloadCount == reloadSoundTick) {
                        W_McClient.MOD_playSoundFX(var8.getReloadSound(), 1.0F, 1.0F);
                    }

                    boolean var10 = true;
                    if (reloadCount < reloadTick) {
                        ++reloadCount;
                        if (reloadCount == reloadTick) {
                            this.onCompleteReload();
                        }
                    }
                } else {
                    reloadCount = 0;
                }
                if (!aiming || !this.shouldUpdateLaserPoint()) {
                    this.unlockWeapon(var6);
                }
            }

            if (!inGUI) {
                this.playerControl(var6, var7, (MCH_ItemLightWeaponBase) var7.getItem());
            }

            this.isHeldItem = MCH_ItemLightWeaponBase.isHeld(var6);
        } else {
            lockonSoundCount = 0;
            reloadCount = 0;
            this.laserAimKeepTicks = 0;
            this.restoreAimingFovZoom();
            if (this.prevThePlayer != null) {
                this.unlockWeapon(this.prevThePlayer);
            }
            this.isHeldItem = false;
        }

        if (this.isBeforeHeldItem != this.isHeldItem) {
            MCH_Lib.DbgLog(true, "LWeapon cancel", new Object[0]);
            if (!this.isHeldItem) {
                if (getPotionNightVisionDuration(var6) < 250) {
                    MCH_PacketLightWeaponPlayerControl var9 = new MCH_PacketLightWeaponPlayerControl();
                    var9.camMode = 1;
                    System.out.println("pre sent dogshit to the server");
                    W_Network.sendToServer(var9);
                    System.out.println("sent dogshit to the server");
                    prevThePlayer.removePotionEffect(Potion.nightVision.getId());
                }

                W_Reflection.restoreCameraZoom();
                this.restoreAimingFovZoom();
            }
        }

        int lightWeaponCount = countLightWeapons(var6);
        if (lightWeaponCount > 1) {
            var6.addPotionEffect(new PotionEffect(Potion.moveSlowdown.getId(), 200, 2, true));
        } else {
            prevThePlayer.removePotionEffect(Potion.moveSlowdown.getId());
        }


        this.prevItemStack = var7;
        if (gs != null) {
            gs.update();
        }
    }

    private int countLightWeapons(EntityPlayer player) {
        int count = 0;
        for (ItemStack itemStack : player.inventory.mainInventory) {
            if (itemStack != null && itemStack.getItem() instanceof MCH_ItemLightWeaponBase) {
                count++;
            }
        }
        return count;
    }

    protected void onCompleteReload() {
        MCH_PacketLightWeaponPlayerControl pc = new MCH_PacketLightWeaponPlayerControl();
        pc.cmpReload = 1;
        W_Network.sendToServer(pc);
    }

    public void playerControl(EntityPlayer player, ItemStack is, MCH_ItemLightWeaponBase item) {
        MCH_PacketLightWeaponPlayerControl pc = new MCH_PacketLightWeaponPlayerControl();
        String heldName = MCH_ItemLightWeaponBase.getName(is);
        boolean send = false;
        boolean autoShot = false;
        MCH_Config var10000 = MCH_MOD.config;
        if (MCH_Config.LWeaponAutoFire.prmBool && is.getItemDamage() < is.getMaxDamage() && this.isLockComplete(player)) {
            autoShot = true;
            //rpg stuff here
        }
        if ("rpg7".equalsIgnoreCase(heldName)) {
            if (this.KeyAttack.isKeyDown()) {
                pc.useWeapon = true;
                pc.useWeaponPosX = player.posX;
                pc.useWeaponPosY = player.posY;
                pc.useWeaponPosZ = player.posZ;
                send = true;
            }
        }

        if (this.KeySwWeaponMode.isKeyDown() && weapon.numMode > 1 && (weapon.getInfo() == null || weapon.getInfo().fixMode <= 0)) {
            weaponMode = (weaponMode + 1) % weapon.numMode;
            weapon.setCurrentMode(weaponMode);
            W_McClient.MOD_playSoundFX("pi", 0.5F, 0.9F);
        }

        if (this.KeyAttack.isKeyPress() || autoShot && !("rpg7".equalsIgnoreCase(heldName))) {
            boolean pe = false;
            if (is.getItemDamage() < is.getMaxDamage() && this.isLockComplete(player)) {
                boolean canFire = true;
                if (this.getEffectiveWeaponMode() > 0 && this.isSeekerWeapon() && gs.getTargetEntity() != null) {
                    double dx = gs.getTargetEntity().posX - player.posX;
                    double dz = gs.getTargetEntity().posZ - player.posZ;
                    canFire = Math.sqrt(dx * dx + dz * dz) >= 40.0D;
                }

                if (canFire) {
                    pc.useWeapon = true;
                    if (this.isTVMissileWeapon()) {
                        pc.useWeaponOption1 = this.getEffectiveWeaponMode();
                        pc.useWeaponOption2 = 0;
                    } else {
                        pc.useWeaponOption1 = this.getCurrentLockEntityId();
                        pc.useWeaponOption2 = this.getEffectiveWeaponMode();
                    }
                    pc.useWeaponPosX = player.posX;
                    pc.useWeaponPosY = player.posY;
                    pc.useWeaponPosZ = player.posZ;
                    if (!this.shouldUpdateLaserPoint()) {
                        this.unlockWeapon(player);
                    } else {
                        // Prevent a one-tick use-duration drop from kicking player out of aiming state after firing.
                        this.laserAimKeepTicks = 40;
                        // Keep using-state alive through the firing frame to avoid brief scope drop.
                        if (is != null && is.getItem() instanceof MCH_ItemLightWeaponBase) {
                            player.setItemInUse(is, ((MCH_ItemLightWeaponBase) is.getItem()).getMaxItemUseDuration(is));
                        }
                    }
                    send = true;
                    pe = true;
                }
            }
            if (this.KeyAttack.isKeyPress()) {
                if ("rpg7".equalsIgnoreCase(heldName)) {
                    boolean canFire = false;
                    if (is.getItemDamage() < is.getMaxDamage() && this.KeyZoom.isKeyDown()) {//todo: check for scoped
                        canFire = true;
                    }
                    if (canFire) {
                        pc.useWeapon = true;
                    }
                }
            }

            //if(this.KeyAttack.isKeyDown() && !pe && player.getItemInUseDuration() > 5 && ) {
            //add expression to check if isn't rpg also TODOne?: stop lock on feature for rpg
            //&& //check rpg here
            //TODOne: change RPG scope to be the T one
            if (this.KeyAttack.isKeyDown() && !pe && player.getItemInUseDuration() > 5 && !("rpg7".equalsIgnoreCase(heldName))) {
                playSoundNG();
            }
        }

        if (this.KeyZoom.isKeyDown()) {
            int pe1 = selectedZoom;
            selectedZoom = (selectedZoom + 1) % this.getZoomLevels(item).length;
            if (pe1 != selectedZoom) {
                playSound("zoom", 0.5F, 1.0F);
            }
        }

        //if (lightWeaponCount > 1) {
        //         var6.addPotionEffect(new PotionEffect(Potion.moveSlowdown.getId(), 200, 2, true));
        //      } else {
        //         prevThePlayer.removePotionEffect(Potion.moveSlowdown.getId());
        //      }

        if (this.KeyCameraMode.isKeyDown()) {
            EntityClientPlayerMP var6 = super.mc.thePlayer;
            PotionEffect pe2 = player.getActivePotionEffect(Potion.nightVision);
            if (item.isNightVisionEnabled()) {
                MCH_Lib.DbgLog(true, "LWeapon NV %s", new Object[]{pe2 != null ? "ON->OFF" : "OFF->ON"});
                if (pe2 != null) {
                    var6.removePotionEffect(Potion.nightVision.getId());
                    pc.camMode = 1;
                    send = true;
                    W_McClient.MOD_playSoundFX("pi", 0.5F, 0.9F);
                } else if (player.getItemInUseDuration() > 60) {
                    var6.addPotionEffect(new PotionEffect(Potion.nightVision.getId(), 250, 0, false));
                    pc.camMode = 2;
                    send = true;
                    W_McClient.MOD_playSoundFX("pi", 0.5F, 0.9F);
                } else {
                    playSoundNG();
                }
            } else {
                playSoundNG();
            }
        }

        if (send) {
            W_Network.sendToServer(pc);
        }

    }

    private float[] getZoomLevels(MCH_ItemLightWeaponBase item) {
        float[] override = item.getZoomLevels();
        if (override.length > 0) {
            return override;
        }
        if (weapon != null && weapon.getInfo() != null && weapon.getInfo().zoom != null && weapon.getInfo().zoom.length > 0) {
            return weapon.getInfo().zoom;
        }
        return new float[]{1.0F};
    }

    private boolean isSeekerWeapon() {
        return weapon != null && weapon.getGuidanceSystem() instanceof MCH_WeaponGuidanceSystem;
    }

    private boolean isTVMissileWeapon() {
        return weapon != null && weapon.getInfo() != null && "tvmissile".equalsIgnoreCase(weapon.getInfo().type);
    }

    private boolean isTVLaserModeWeapon() {
        return this.isTVMissileWeapon() && this.getEffectiveWeaponMode() == 1;
    }

    private boolean isLaserGuidedWeapon() {
        if (weapon == null || weapon.getInfo() == null) {
            return false;
        }
        if (weapon.getGuidanceSystem() instanceof MCH_LaserGuidanceSystem) {
            return true;
        }
        return weapon.getInfo().laserGuidance;
    }

    private boolean shouldUpdateLaserPoint() {
        if (!this.isLaserGuidedWeapon()) {
            return false;
        }
        // TV missile mode 0 is pure TV guide and must not publish a laser point.
        return !this.isTVMissileWeapon() || this.getEffectiveWeaponMode() == 1;
    }

    private void updateLaserPoint(EntityPlayer player) {
        if (player == null || !this.shouldUpdateLaserPoint()) {
            return;
        }
        if (weapon != null && weapon.getGuidanceSystem() instanceof MCH_LaserGuidanceSystem) {
            MCH_LaserGuidanceSystem laser = (MCH_LaserGuidanceSystem) weapon.getGuidanceSystem();
            // Keep updating laser target even when ammo is empty, so mark point follows current sight.
            laser.targeting = true;
            laser.update();
            MCH_GPSPosition.set(laser.targetPosX, laser.targetPosY, laser.targetPosZ, true, player);
            return;
        }
        Vec3 src = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3 look = player.getLook(1.0F);
        Vec3 end = src.addVector(look.xCoord * 1500.0D, look.yCoord * 1500.0D, look.zCoord * 1500.0D);
        MovingObjectPosition hit = player.worldObj.rayTraceBlocks(src, end);
        Vec3 pos = hit != null && hit.hitVec != null ? hit.hitVec : end;
        MCH_GPSPosition.set(pos.xCoord, pos.yCoord, pos.zCoord, true, player);
    }

    private int getEffectiveWeaponMode() {
        return weapon != null ? weapon.getCurrentMode() : weaponMode;
    }

    private int getWeaponLockCount() {
        if (this.isSeekerWeapon()) {
            return gs.getLockCount();
        }
        return weapon != null ? weapon.getLockCount() : 0;
    }

    private int getWeaponLockCountMax() {
        if (this.isSeekerWeapon()) {
            return gs.getLockCountMax();
        }
        return weapon != null ? weapon.getLockCountMax() : 0;
    }

    private boolean isLockComplete(EntityPlayer player) {
        if (this.isSeekerWeapon()) {
            return gs.isLockComplete();
        }
        int max = this.getWeaponLockCountMax();
        if (max > 0) {
            return this.getWeaponLockCount() >= max;
        }
        return player != null && player.getItemInUseDuration() > 10;
    }

    private int getCurrentLockEntityId() {
        return this.isSeekerWeapon() ? W_Entity.getEntityId(gs.lastLockEntity) : 0;
    }

    private MCH_WeaponParam createWeaponParam(EntityPlayer player, int option2, int option1) {
        MCH_WeaponParam prm = new MCH_WeaponParam();
        prm.entity = player;
        prm.user = player;
        prm.option1 = option1;
        prm.option2 = option2;
        prm.setPosition(player.posX, player.posY, player.posZ);
        prm.rotYaw = player.rotationYaw;
        prm.rotPitch = player.rotationPitch;
        return prm;
    }

    private void unlockWeapon(EntityPlayer player) {
        this.laserAimKeepTicks = 0;
        this.restoreAimingFovZoom();
        if (player != null && this.shouldClearGpsMarkerOnUnlock()) {
            MCH_GPSPosition.set(0.0D, 0.0D, 0.0D, false, player);
        }
        if (this.isSeekerWeapon()) {
            gs.clearLock();
        }
        if (weapon != null && player != null) {
            weapon.onUnlock(this.createWeaponParam(player, this.getEffectiveWeaponMode(), this.getCurrentLockEntityId()));
        }
    }

    private boolean shouldClearGpsMarkerOnUnlock() {
        if (weapon == null || weapon.getInfo() == null || weapon.getInfo().type == null) {
            return false;
        }
        String type = weapon.getInfo().type;
        // B方案: 仅GPS制导武器允许清空GPS点，激光制导/TV子模式不再清空GPS点。
        if ("asmissile".equalsIgnoreCase(type)) {
            return weapon.getInfo().isGPSMissile;
        }
        return false;
    }

    private void applyAimingFovZoom(float zoom) {
        if (super.mc == null || super.mc.gameSettings == null) {
            return;
        }
        if (!this.fovZoomActive) {
            this.baseFov = super.mc.gameSettings.fovSetting;
            this.fovZoomActive = true;
        }
        float targetFov = this.baseFov / zoom;
        if (targetFov < 5.0F) {
            targetFov = 5.0F;
        }
        super.mc.gameSettings.fovSetting = targetFov;
    }

    private void restoreAimingFovZoom() {
        if (!this.fovZoomActive || super.mc == null || super.mc.gameSettings == null) {
            return;
        }
        super.mc.gameSettings.fovSetting = this.baseFov;
        this.fovZoomActive = false;
    }

    private float normalizeZoomFactor(float rawZoom) {
        // Support both conventions:
        // - zoom >= 1.0 : magnification factor (2x, 4x...)
        // - zoom  < 1.0 : FOV scale (0.5 means 2x magnification)
        if (rawZoom <= 0.0F) {
            return 1.0F;
        }
        if (rawZoom < 1.0F) {
            return 1.0F / rawZoom;
        }
        return rawZoom;
    }

}
