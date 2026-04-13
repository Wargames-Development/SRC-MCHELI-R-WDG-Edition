package mcheli.lweapon;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Config;
import mcheli.MCH_KeyName;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.gltd.MCH_EntityGLTD;
import mcheli.gui.MCH_Gui;
import mcheli.weapon.MCH_GPSPosition;
import mcheli.weapon.MCH_WeaponBase;
import mcheli.weapon.MCH_WeaponGuidanceSystem;
import mcheli.wrapper.W_McClient;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;


@SideOnly(Side.CLIENT)
public class MCH_GuiLightWeapon extends MCH_Gui {

    public MCH_GuiLightWeapon(Minecraft minecraft) {
        super(minecraft);
    }

    public void initGui() {
        super.initGui();
    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    public boolean isDrawGui(EntityPlayer player) {
        if (MCH_ItemLightWeaponBase.isHeld(player)) {
            Entity re = player.ridingEntity;
            if (!(re instanceof MCH_EntityAircraft) && !(re instanceof MCH_EntityGLTD)) {
                return true;
            }
        }

        return false;
    }

    public void drawGui(EntityPlayer player, boolean isThirdPersonView) {
        if (!isThirdPersonView) {
            GL11.glLineWidth((float) MCH_Gui.scaleFactor);
            if (this.isDrawGui(player)) {
                MCH_ItemLightWeaponBase item = (MCH_ItemLightWeaponBase) player.getHeldItem().getItem();
                MCH_WeaponGuidanceSystem gs = MCH_ClientLightWeaponTickHandler.gs;
                MCH_WeaponBase weapon = MCH_ClientLightWeaponTickHandler.weapon;
                if (weapon != null && weapon.getInfo() != null) {
                    int currentMode = weapon.getCurrentMode();
                    PotionEffect pe = player.getActivePotionEffect(Potion.nightVision);
                    if (pe != null) {
                        this.drawNightVisionNoise();
                    }

                    GL11.glEnable(3042);
                    GL11.glColor4f(0.0F, 0.0F, 0.0F, 1.0F);
                    int srcBlend = GL11.glGetInteger(3041);
                    int dstBlend = GL11.glGetInteger(3040);
                    GL11.glBlendFunc(770, 771);
                    boolean seeker = gs != null && weapon.getGuidanceSystem() instanceof MCH_WeaponGuidanceSystem;
                    int lockCount = seeker ? gs.getLockCount() : weapon.getLockCount();
                    int lockCountMax = seeker ? gs.getLockCountMax() : weapon.getLockCountMax();
                    double dist = 0.0D;
                    if (seeker && gs.getTargetEntity() != null) {
                        double canFire = gs.getTargetEntity().posX - player.posX;
                        double dz = gs.getTargetEntity().posZ - player.posZ;
                        dist = Math.sqrt(canFire * canFire + dz * dz);
                    }

                    boolean canFire1 = !seeker || currentMode == 0 || dist >= 40.0D || lockCount <= 0;
                    ItemStack heldItem = player.getHeldItem();
                    String weaponName = MCH_ItemLightWeaponBase.getName(heldItem);
                    String hudType = item.getHudType(heldItem);
                    if ("fgm148".equalsIgnoreCase(hudType)) {
                        this.drawGuiFGM148(player, gs, canFire1, player.getHeldItem());
                        this.drawKeyBind(-805306369, true);
                        //DONE: add another conditional for RPG/T sight
                        //todo: test
                    } else if ("rpg7".equalsIgnoreCase(weaponName)) {
                        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                        W_McClient.MOD_bindTexture("textures/gui/rpg.png");
                        double size;
                        for (size = 512.0D; size < (double) super.width || size < (double) super.height; size *= 2.0D) {
                            ;
                        }
                        this.drawTexturedModalRectRotate(-(size - (double) super.width) / 2.0D, -(size - (double) super.height) / 2.0D - 20.0D, size, size, 0.0D, 0.0D, 256.0D, 256.0D, 0.0F);
                        this.drawKeyBind(-805306369, false);
                        GL11.glBlendFunc(srcBlend, dstBlend);
                        GL11.glDisable(3042);

                    } else if ("fim92".equalsIgnoreCase(hudType) || "fim192".equalsIgnoreCase(hudType)) {

                        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                        String scope = !item.getScopeTexture().isEmpty() ? item.getScopeTexture() : "stinger";
                        W_McClient.MOD_bindTexture("textures/gui/" + scope + ".png");

                        double size;
                        for (size = 512.0D; size < (double) super.width || size < (double) super.height; size *= 2.0D) {
                            ;
                        }


                        this.drawTexturedModalRectRotate(-(size - (double) super.width) / 2.0D, -(size - (double) super.height) / 2.0D - 20.0D, size, size, 0.0D, 0.0D, 256.0D, 256.0D, 0.0F);
                        this.drawKeyBind(-805306369, false);
                    } else if ("asmissile".equalsIgnoreCase(hudType) || "tvmissile".equalsIgnoreCase(hudType)) {
                        this.drawGenericMissileScope(item);
                        this.drawKeyBind(-805306369, true);
                        this.drawMissileModeAndStatus(player, hudType, seeker, lockCount, lockCountMax, currentMode);
                    }

                    GL11.glBlendFunc(srcBlend, dstBlend);
                    GL11.glDisable(3042);
                    this.drawLock(-14101432, -2161656, lockCount, lockCountMax);
                    if (seeker) {
                        this.drawRange(player, gs, canFire1, -14101432, -2161656);
                    } else {
                        this.drawCenteredString(lockCountMax > 0 ? this.getTextLock() : this.getTextGuide(), super.centerX, super.centerY + 50, canFire1 ? -14101432 : -2161656);
                    }
                }

            }
        }
    }

    public void drawNightVisionNoise() {
        GL11.glEnable(3042);
        GL11.glColor4f(0.0F, 1.0F, 0.0F, 0.3F);
        int srcBlend = GL11.glGetInteger(3041);
        int dstBlend = GL11.glGetInteger(3040);
        GL11.glBlendFunc(1, 1);
        W_McClient.MOD_bindTexture("textures/gui/alpha.png");
        this.drawTexturedModalRectRotate(0.0D, 0.0D, (double) super.width, (double) super.height, (double) super.rand.nextInt(256), (double) super.rand.nextInt(256), 256.0D, 256.0D, 0.0F);
        GL11.glBlendFunc(srcBlend, dstBlend);
        GL11.glDisable(3042);
    }

    void drawLock(int color, int colorLock, int cntLock, int cntMax) {
        if (cntMax <= 0) {
            return;
        }
        int posX = super.centerX;
        int posY = super.centerY + 20;
        boolean WID = true;
        boolean INV = true;
        double[] var10000 = new double[]{(double) (posX - 20), (double) (posY - 10), (double) (posX - 20), (double) (posY - 20), (double) (posX - 20), (double) (posY - 20), (double) (posX - 10), (double) (posY - 20), (double) (posX - 20), (double) (posY + 10), (double) (posX - 20), (double) (posY + 20), (double) (posX - 20), (double) (posY + 20), (double) (posX - 10), (double) (posY + 20), (double) (posX + 20), (double) (posY - 10), (double) (posX + 20), (double) (posY - 20), (double) (posX + 20), (double) (posY - 20), (double) (posX + 10), (double) (posY - 20), (double) (posX + 20), (double) (posY + 10), (double) (posX + 20), (double) (posY + 20), (double) (posX + 20), (double) (posY + 20), (double) (posX + 10), (double) (posY + 20)};
        drawRect(posX - 20, posY + 20 + 1, posX - 20 + 40, posY + 20 + 1 + 1 + 3 + 1, color);
        float lock = (float) cntLock / (float) cntMax;
        drawRect(posX - 20 + 1, posY + 20 + 1 + 1, posX - 20 + 1 + (int) (38.0D * (double) lock), posY + 20 + 1 + 1 + 3, -2161656);
    }

    void drawRange(EntityPlayer player, MCH_WeaponGuidanceSystem gs, boolean canFire, int color1, int color2) {
        //if {
        //if is not rpg
        String msgLockDist = "[--.--]";

        int color = color2;
        if (gs.getLockCount() > 0) {
            Entity target = gs.getLockingEntity();
            if (target != null) {
                double dx = target.posX - player.posX;
                double dz = target.posZ - player.posZ;
                msgLockDist = String.format("[%.2f]", new Object[]{Double.valueOf(Math.sqrt(dx * dx + dz * dz))});
                color = canFire ? color1 : color2;
                MCH_Config var10000 = MCH_MOD.config;
                if (!MCH_Config.HideKeybind.prmBool && gs.isLockComplete()) {
                    var10000 = MCH_MOD.config;
                    String k = MCH_KeyName.getDescOrName(MCH_Config.KeyAttack.prmInt);
                    this.drawCenteredString("Shot : " + k, super.centerX, super.centerY + 65, -805306369);
                }
            }
        }

        this.drawCenteredString(msgLockDist, super.centerX, super.centerY + 50, color);
    }
    //}

    void drawGuiFGM148(EntityPlayer player, MCH_WeaponGuidanceSystem gs, boolean canFire, ItemStack itemStack) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        double fac = (double) super.width / 800.0D < (double) super.height / 700.0D ? (double) super.width / 800.0D : (double) super.height / 700.0D;
        int size = (int) (1024.0D * fac);
        size = size / 64 * 64;
        fac = (double) size / 1024.0D;
        double left = (double) (-(size - super.width) / 2);
        double top = (double) (-(size - super.height) / 2 - 20);
        double right = left + (double) size;
        double bottom = top + (double) size;
        Vec3 pos = MCH_ClientLightWeaponTickHandler.getMartEntityPos();

        if (gs.getLockCount() > 0) {
            int x = MCH_Gui.scaleFactor > 0 ? MCH_Gui.scaleFactor : 2;
            if (pos == null) {
                pos = Vec3.createVectorHelper((double) (super.width / 2 * x), (double) (super.height / 2 * x), 0.0D);
            }

            double IX = 280.0D * fac;
            double IY = 370.0D * fac;
            double cx = pos.xCoord / (double) x;
            double cy = (double) super.height - pos.yCoord / (double) x;
            double sx = MCH_Lib.RNG(cx, left + IX, right - IX);
            double sy = MCH_Lib.RNG(cy, top + IY, bottom - IY);
            if (gs.getLockCount() >= gs.getLockCountMax() / 2) {
//            Minecraft.getMinecraft().fontRenderer.drawString(
//                    String.format("[%.1f,%.1f]", cx, cy),
//                    (int) (cx + 10), (int) cy,
//                    0xFFFFFF
//            );
                this.drawLine(new double[]{-1.0D, sy, (double) (super.width + 1), sy, sx, -1.0D, sx, (double) (super.height + 1)}, -1593835521);
            }

            if (player.ticksExisted % 6 >= 3) {
                pos = MCH_ClientLightWeaponTickHandler.getMartEntityBBPos();
                if (pos == null) {
                    pos = Vec3.createVectorHelper((double) ((super.width / 2 - 65) * x), (double) ((super.height / 2 + 50) * x), 0.0D);
                }

                double bx = pos.xCoord / (double) x;
                double by = (double) super.height - pos.yCoord / (double) x;
                double dx = Math.abs(cx - bx);
                double dy = Math.abs(cy - by);
                double p = 1.0D - (double) gs.getLockCount() / (double) gs.getLockCountMax();
                dx = MCH_Lib.RNG(dx, 25.0D, 70.0D);
                dy = MCH_Lib.RNG(dy, 15.0D, 70.0D);
                dx += (70.0D - dx) * p;
                dy += (70.0D - dy) * p;
                byte lx = 10;
                byte ly = 6;
                this.drawLine(new double[]{sx - dx, sy - dy + (double) ly, sx - dx, sy - dy, sx - dx + (double) lx, sy - dy}, -1593835521, 3);
                this.drawLine(new double[]{sx + dx, sy - dy + (double) ly, sx + dx, sy - dy, sx + dx - (double) lx, sy - dy}, -1593835521, 3);
                dy /= 6.0D;
                this.drawLine(new double[]{sx - dx, sy + dy - (double) ly, sx - dx, sy + dy, sx - dx + (double) lx, sy + dy}, -1593835521, 3);
                this.drawLine(new double[]{sx + dx, sy + dy - (double) ly, sx + dx, sy + dy, sx + dx - (double) lx, sy + dy}, -1593835521, 3);
            }
        }

        drawRect(-1, -1, (int) left + 1, super.height + 1, -16777216);
        drawRect((int) right - 1, -1, super.width + 1, super.height + 1, -16777216);
        drawRect(-1, -1, super.width + 1, (int) top + 1, -16777216);
        drawRect(-1, (int) bottom - 1, super.width + 1, super.height + 1, -16777216);
        GL11.glEnable(3042);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        MCH_ItemLightWeaponBase weaponItem = itemStack.getItem() instanceof MCH_ItemLightWeaponBase ? (MCH_ItemLightWeaponBase) itemStack.getItem() : null;
        String scopeTexture = weaponItem != null && !weaponItem.getScopeTexture().isEmpty() ? weaponItem.getScopeTexture() : "javelin";
        String scopeTexture2 = weaponItem != null && !weaponItem.getScopeOverlayTexture2().isEmpty() ? weaponItem.getScopeOverlayTexture2() : "javelin2";
        W_McClient.MOD_bindTexture("textures/gui/" + scopeTexture + ".png");
        this.drawTexturedModalRectRotate(left, top, (double) size, (double) size, 0.0D, 0.0D, 256.0D, 256.0D, 0.0F);
        W_McClient.MOD_bindTexture("textures/gui/" + scopeTexture2 + ".png");
        PotionEffect pe = player.getActivePotionEffect(Potion.nightVision);
        int zoomLength = 1;
        if (weaponItem != null) {
            float[] zooms = weaponItem.getZoomLevels();
            if (zooms.length > 0) {
                zoomLength = zooms.length;
            } else if (MCH_ClientLightWeaponTickHandler.weapon != null && MCH_ClientLightWeaponTickHandler.weapon.getInfo() != null && MCH_ClientLightWeaponTickHandler.weapon.getInfo().zoom != null) {
                zoomLength = MCH_ClientLightWeaponTickHandler.weapon.getInfo().zoom.length;
            }
        }
        double y;
        double w;
        double h;
        double x1;
        if (pe == null) {
            x1 = 247.0D;
            y = 211.0D;
            w = 380.0D;
            h = 350.0D;
            this.drawTexturedRect(left + x1 * fac, top + y * fac, (w - x1) * fac, (h - y) * fac, x1, y, w - x1, h - y, 1024.0D, 1024.0D);
        }

        if (player.getItemInUseDuration() <= 60) {
            x1 = 130.0D;
            y = 334.0D;
            w = 257.0D;
            h = 455.0D;
            this.drawTexturedRect(left + x1 * fac, top + y * fac, (w - x1) * fac, (h - y) * fac, x1, y, w - x1, h - y, 1024.0D, 1024.0D);
        }

        if (MCH_ClientLightWeaponTickHandler.selectedZoom == 0) {
            x1 = 387.0D;
            y = 211.0D;
            w = 510.0D;
            h = 350.0D;
            this.drawTexturedRect(left + x1 * fac, top + y * fac, (w - x1) * fac, (h - y) * fac, x1, y, w - x1, h - y, 1024.0D, 1024.0D);
        }

        if (MCH_ClientLightWeaponTickHandler.selectedZoom == zoomLength - 1) {
            x1 = 511.0D;
            y = 211.0D;
            w = 645.0D;
            h = 350.0D;
            this.drawTexturedRect(left + x1 * fac, top + y * fac, (w - x1) * fac, (h - y) * fac, x1, y, w - x1, h - y, 1024.0D, 1024.0D);
        }

        if (gs.getLockCount() > 0) {
            x1 = 643.0D;
            y = 211.0D;
            w = 775.0D;
            h = 350.0D;
            this.drawTexturedRect(left + x1 * fac, top + y * fac, (w - x1) * fac, (h - y) * fac, x1, y, w - x1, h - y, 1024.0D, 1024.0D);
        }

        if (MCH_ClientLightWeaponTickHandler.weaponMode == 1) {
            x1 = 768.0D;
            y = 340.0D;
            w = 890.0D;
            h = 455.0D;
            this.drawTexturedRect(left + x1 * fac, top + y * fac, (w - x1) * fac, (h - y) * fac, x1, y, w - x1, h - y, 1024.0D, 1024.0D);
        } else {
            x1 = 768.0D;
            y = 456.0D;
            w = 890.0D;
            h = 565.0D;
            this.drawTexturedRect(left + x1 * fac, top + y * fac, (w - x1) * fac, (h - y) * fac, x1, y, w - x1, h - y, 1024.0D, 1024.0D);
        }

        if (!canFire) {
            x1 = 379.0D;
            y = 670.0D;
            w = 511.0D;
            h = 810.0D;
            this.drawTexturedRect(left + x1 * fac, top + y * fac, (w - x1) * fac, (h - y) * fac, x1, y, w - x1, h - y, 1024.0D, 1024.0D);
        }

        if (itemStack.getItemDamage() >= itemStack.getMaxDamage()) {
            x1 = 512.0D;
            y = 670.0D;
            w = 645.0D;
            h = 810.0D;
            this.drawTexturedRect(left + x1 * fac, top + y * fac, (w - x1) * fac, (h - y) * fac, x1, y, w - x1, h - y, 1024.0D, 1024.0D);
        }

        if (gs.getLockCount() < gs.getLockCountMax()) {
            x1 = 646.0D;
            y = 670.0D;
            w = 776.0D;
            h = 810.0D;
            this.drawTexturedRect(left + x1 * fac, top + y * fac, (w - x1) * fac, (h - y) * fac, x1, y, w - x1, h - y, 1024.0D, 1024.0D);
        }

        if (pe != null) {
            x1 = 768.0D;
            y = 562.0D;
            w = 890.0D;
            h = 694.0D;
            this.drawTexturedRect(left + x1 * fac, top + y * fac, (w - x1) * fac, (h - y) * fac, x1, y, w - x1, h - y, 1024.0D, 1024.0D);
        }

    }

    public void drawKeyBind(int color, boolean canSwitchMode) {
        int OffX = super.centerX + 55;
        int OffY = super.centerY + 40;
        this.drawString("CAM MODE :", OffX, OffY + 10, color);
        this.drawString("ZOOM      :", OffX, OffY + 20, color);
        if (canSwitchMode) {
            this.drawString("MODE      :", OffX, OffY + 30, color);
        }

        OffX += 60;
        MCH_Config var10001 = MCH_MOD.config;
        this.drawString(MCH_KeyName.getDescOrName(MCH_Config.KeyCameraMode.prmInt), OffX, OffY + 10, color);
        var10001 = MCH_MOD.config;
        this.drawString(MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt), OffX, OffY + 20, color);
        if (canSwitchMode) {
            var10001 = MCH_MOD.config;
            this.drawString(MCH_KeyName.getDescOrName(MCH_Config.KeySwWeaponMode.prmInt), OffX, OffY + 30, color);
        }

    }

    private void drawGenericMissileScope(MCH_ItemLightWeaponBase item) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        String scope = !item.getScopeTexture().isEmpty() ? item.getScopeTexture() : "stinger";
        W_McClient.MOD_bindTexture("textures/gui/" + scope + ".png");
        double size;
        for (size = 512.0D; size < (double) super.width || size < (double) super.height; size *= 2.0D) {
            ;
        }
        this.drawTexturedModalRectRotate(-(size - (double) super.width) / 2.0D, -(size - (double) super.height) / 2.0D - 20.0D, size, size, 0.0D, 0.0D, 256.0D, 256.0D, 0.0F);
    }

    private void drawMissileModeAndStatus(EntityPlayer player, String hudType, boolean seeker, int lockCount, int lockCountMax, int currentMode) {
        String modeText = this.getTextMode() + " " + (currentMode + 1);
        String typeText = this.getTextASMissile();
        if ("tvmissile".equalsIgnoreCase(hudType)) {
            typeText = currentMode == 0 ? this.getTextTvMode() : this.getTextTaMode();
        }
        String statusText;
        if (player.getItemInUseDuration() <= 10) {
            statusText = this.getTextAim();
        } else if (lockCountMax > 0 && lockCount < lockCountMax) {
            statusText = this.getTextTracking();
        } else if (seeker && lockCount <= 0) {
            statusText = this.getTextSearch();
        } else {
            statusText = this.getTextFire();
        }
        this.drawCenteredString(typeText, super.centerX, super.centerY - 56, -14101432);
        this.drawCenteredString(modeText, super.centerX, super.centerY - 44, -14101432);
        if ("tvmissile".equalsIgnoreCase(hudType) && currentMode == 1) {
            boolean laserLocked = this.isLaserPointLocked(player);
            this.drawCenteredString(laserLocked ? this.getTextLaserLocked() : this.getTextLaserNotLocked(), super.centerX, super.centerY + 62, laserLocked ? -14101432 : -2161656);
        } else {
            this.drawCenteredString(statusText, super.centerX, super.centerY + 62, -2161656);
        }
    }

    private boolean isEnglishLocale() {
        String lang = FMLClientHandler.instance().getClient().gameSettings.language;
        return lang != null && lang.toLowerCase().startsWith("en");
    }

    private String getTextMode() {
        return this.isEnglishLocale() ? "MODE" : "模式";
    }

    private String getTextASMissile() {
        return this.isEnglishLocale() ? "AS MISSILE" : "对地导弹";
    }

    private String getTextTvMode() {
        return this.isEnglishLocale() ? "TV MODE" : "电视制导";
    }

    private String getTextTaMode() {
        return this.isEnglishLocale() ? "LASER GUIDE" : "激光制导";
    }

    private String getTextAim() {
        return this.isEnglishLocale() ? "AIM" : "瞄准";
    }

    private String getTextTracking() {
        return this.isEnglishLocale() ? "TRACKING" : "跟踪中";
    }

    private String getTextSearch() {
        return this.isEnglishLocale() ? "SEARCH" : "搜索";
    }

    private String getTextFire() {
        return "";
    }

    private String getTextLock() {
        return this.isEnglishLocale() ? "[LOCK]" : "[锁定]";
    }

    private String getTextGuide() {
        return this.isEnglishLocale() ? "[GUIDE]" : "[引导]";
    }

    private boolean isLaserPointLocked(EntityPlayer player) {
        MCH_GPSPosition gps = MCH_GPSPosition.currentClientGPSPosition;
        return gps != null && gps.isActive && gps.owner != null && player != null && gps.owner.getEntityId() == player.getEntityId();
    }

    private String getTextLaserLocked() {
        return this.isEnglishLocale() ? "Laser Locked" : "激光点已锁定";
    }

    private String getTextLaserNotLocked() {
        return this.isEnglishLocale() ? "Laser Not Locked" : "激光点未锁定";
    }
}
