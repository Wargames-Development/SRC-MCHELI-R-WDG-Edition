package mcheli.aircraft;

import mcheli.*;
import mcheli.gui.MCH_GuiGPSInput;
import mcheli.network.packets.PacketAirburstDistReset;
import mcheli.network.packets.PacketRadarSwitchState;
import mcheli.network.packets.PacketUseWeapon;
import mcheli.render.MCH_RenderLeadCircle;
import mcheli.render.MCH_RenderRWR;
import mcheli.weapon.MCH_WeaponBase;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponSet;
import mcheli.wrapper.W_Network;
import mcheli.wrapper.W_PacketBase;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Keyboard;

public abstract class MCH_AircraftClientTickHandler extends MCH_ClientTickHandlerBase {
    public MCH_Key KeyUp;
    public MCH_Key KeyDown;
    public MCH_Key KeyRight;
    public MCH_Key KeyLeft;
    public MCH_Key KeyUseWeapon;
    public MCH_Key KeySwitchWeapon1;
    public MCH_Key KeySwitchWeapon2;
    public MCH_Key KeySwWeaponMode;
    public MCH_Key KeyUnmount;
    public MCH_Key KeyUnmountForce;
    public MCH_Key KeyExtra;
    public MCH_Key KeyFlare;
    public MCH_Key KeyCameraMode;
    public MCH_Key KeyFreeLook;
    public MCH_Key KeyGUI;
    public MCH_Key KeyGearUpDown;
    public MCH_Key KeyPutToRack;
    public MCH_Key KeyDownFromRack;
    public MCH_Key KeyBrake;
    public MCH_Key KeyCurrentWeaponLock;
    /**
     * 箔条按键
     */
    public MCH_Key KeyChaff;
    /**
     * 维修按键
     */
    public MCH_Key KeyMaintenance;
    /**
     * APS按键
     */
    public MCH_Key KeyAPS;
    /**
     * 电子战干扰按键
     */
    public MCH_Key KeyECMJammer;
    public MCH_Key KeyAirburstDistReset;
    public MCH_Key KeyOpenGPSPanel;
    public MCH_Key KeyFireControlLock;
    public MCH_Key KeyRadarSwitch;
    protected boolean isRiding = false;
    protected boolean isBeforeRiding = false;

    public MCH_AircraftClientTickHandler(Minecraft minecraft, MCH_Config config) {
        super(minecraft);
        updateKeybind(config);
    }

    public void updateKeybind(MCH_Config config) {
        this.KeyUp = new MCH_Key(MCH_Config.KeyUp.prmInt);
        this.KeyDown = new MCH_Key(MCH_Config.KeyDown.prmInt);
        this.KeyRight = new MCH_Key(MCH_Config.KeyRight.prmInt);
        this.KeyLeft = new MCH_Key(MCH_Config.KeyLeft.prmInt);
        this.KeyUseWeapon = new MCH_Key(MCH_Config.KeyUseWeapon.prmInt);
        this.KeySwitchWeapon1 = new MCH_Key(MCH_Config.KeySwitchWeapon1.prmInt);
        this.KeySwitchWeapon2 = new MCH_Key(MCH_Config.KeySwitchWeapon2.prmInt);
        this.KeySwWeaponMode = new MCH_Key(MCH_Config.KeySwWeaponMode.prmInt);
        this.KeyUnmount = new MCH_Key(MCH_Config.KeyUnmount.prmInt);
        this.KeyUnmountForce = new MCH_Key(42);
        this.KeyExtra = new MCH_Key(MCH_Config.KeyExtra.prmInt);
        this.KeyFlare = new MCH_Key(MCH_Config.KeyFlare.prmInt);
        this.KeyCameraMode = new MCH_Key(MCH_Config.KeyCameraMode.prmInt);
        this.KeyFreeLook = new MCH_Key(MCH_Config.KeyFreeLook.prmInt);
        this.KeyGUI = new MCH_Key(MCH_Config.KeyGUI.prmInt);
        this.KeyGearUpDown = new MCH_Key(MCH_Config.KeyGearUpDown.prmInt);
        this.KeyPutToRack = new MCH_Key(MCH_Config.KeyPutToRack.prmInt);
        this.KeyDownFromRack = new MCH_Key(MCH_Config.KeyDownFromRack.prmInt);
        this.KeyBrake = new MCH_Key(MCH_Config.KeySwitchHovering.prmInt);
        this.KeyCurrentWeaponLock = new MCH_Key(MCH_Config.KeyCurrentWeaponLock.prmInt);
        this.KeyChaff = new MCH_Key(MCH_Config.KeyChaff.prmInt);
        this.KeyMaintenance = new MCH_Key(MCH_Config.KeyMaintenance.prmInt);
        this.KeyAPS = new MCH_Key(MCH_Config.KeyAPS.prmInt);
        this.KeyECMJammer = new MCH_Key(MCH_Config.KeyECMJammer.prmInt);
        this.KeyAirburstDistReset = new MCH_Key(MCH_Config.KeyAirburstDistReset.prmInt);
        this.KeyOpenGPSPanel = new MCH_Key(MCH_Config.KeyOpenGPSPanel.prmInt);
        this.KeyFireControlLock = new MCH_Key(MCH_Config.KeyFireControlLock.prmInt);
        this.KeyRadarSwitch = new MCH_Key(MCH_Config.KeyRadarSwitch.prmInt);
    }

    protected void commonPlayerControlInGUI(EntityPlayer player, MCH_EntityAircraft ac, boolean isPilot, MCH_PacketPlayerControlBase pc) {
    }

    public boolean commonPlayerControl(EntityPlayer player, MCH_EntityAircraft ac, boolean isPilot, MCH_PacketPlayerControlBase pc) {
        if (Keyboard.isKeyDown(MCH_Config.KeyFreeLook.prmInt)) {
            if (this.KeyGUI.isKeyDown() || this.KeyExtra.isKeyDown()) {
                MCH_PacketSeatPlayerControl psc = new MCH_PacketSeatPlayerControl();
                if (isPilot) {
                    psc.switchSeat = (byte) (this.KeyGUI.isKeyDown() ? 1 : 2);
                } else {
                    ac.keepOnRideRotation = true;
                    psc.switchSeat = 3;
                }
                W_Network.sendToServer((W_PacketBase) psc);
                return false;
            }
        } else if (!isPilot && ac.getSeatNum() > 1) {
            MCH_PacketSeatPlayerControl psc = new MCH_PacketSeatPlayerControl();
            if (this.KeyGUI.isKeyDown()) {
                psc.switchSeat = 1;
                W_Network.sendToServer((W_PacketBase) psc);
                return false;
            }
            if (this.KeyExtra.isKeyDown()) {
                psc.switchSeat = 2;
                W_Network.sendToServer((W_PacketBase) psc);
                return false;
            }
        }
        boolean send = false;
        if (this.KeyCameraMode.isKeyDown())
            if (ac.haveSearchLight()) {
                if (ac.canSwitchSearchLight((Entity) player)) {
                    pc.switchSearchLight = true;
                    playSoundOK();
                    send = true;
                }
            } else if (ac.canSwitchCameraMode()) {
                int beforeMode = ac.getCameraMode(player);
                ac.switchCameraMode(player);
                int mode = ac.getCameraMode(player);
                if (mode != beforeMode) {
                    pc.switchCameraMode = (byte) (mode + 1);
                    playSoundOK();
                    send = true;
                }
            } else {
                playSoundNG();
            }
        if (this.KeyUnmount.isKeyDown() && !ac.isDestroyed() && ac.getSizeInventory() > 0 && !isPilot)
            MCH_PacketIndOpenScreen.send(3);
        if (isPilot) {
            if (this.KeyUnmount.isKeyDown()) {
                pc.isUnmount = 2;
                send = true;
            }
            if (this.KeyPutToRack.isKeyDown()) {
                ac.checkRideRack();
                if (ac.canRideRack()) {
                    pc.putDownRack = 3;
                    send = true;
                } else if (ac.canPutToRack()) {
                    pc.putDownRack = 1;
                    send = true;
                }
            } else if (this.KeyDownFromRack.isKeyDown()) {
                if (ac.ridingEntity != null) {
                    pc.isUnmount = 3;
                    send = true;
                } else if (ac.canDownFromRack()) {
                    pc.putDownRack = 2;
                    send = true;
                }
            }
            if (this.KeyGearUpDown.isKeyDown() && ac.getAcInfo().haveLandingGear()) {
                if (ac.canFoldLandingGear()) {
                    pc.switchGear = 1;
                    send = true;
                } else if (ac.canUnfoldLandingGear()) {
                    pc.switchGear = 2;
                    send = true;
                }
            }
            if (this.KeyFreeLook.isKeyDown())
                if (ac.canSwitchFreeLook()) {
                    pc.switchFreeLook = (byte) (ac.isFreeLookMode() ? 2 : 1);
                    send = true;
                }
            if (this.KeyGUI.isKeyDown()) {
                pc.openGui = true;
                send = true;
            }
            if (ac.isRepelling()) {
                pc.throttleDown = ac.throttleDown = false;
                pc.throttleUp = ac.throttleUp = false;
                pc.moveRight = ac.moveRight = false;
                pc.moveLeft = ac.moveLeft = false;
            } else if (ac.hasBrake() && this.KeyBrake.isKeyPress()) {
                send |= this.KeyBrake.isKeyDown();
                pc.throttleDown = ac.throttleDown = false;
                pc.throttleUp = ac.throttleUp = false;
                double dx = ac.posX - ac.prevPosX;
                double dz = ac.posZ - ac.prevPosZ;
                double dist = dx * dx + dz * dz;
                if (ac.getCurrentThrottle() <= 0.03D && dist < 0.01D) {
                    pc.moveRight = ac.moveRight = false;
                    pc.moveLeft = ac.moveLeft = false;
                }
                pc.useBrake = true;
            } else {
                send |= this.KeyBrake.isKeyUp();
                MCH_Key[] dKey = {this.KeyUp, this.KeyDown, this.KeyRight, this.KeyLeft};
                for (MCH_Key k : dKey) {
                    if (k.isKeyDown() || k.isKeyUp()) {
                        send = true;
                        break;
                    }
                }
                pc.throttleDown = ac.throttleDown = this.KeyDown.isKeyPress();
                pc.throttleUp = ac.throttleUp = this.KeyUp.isKeyPress();
                pc.moveRight = ac.moveRight = this.KeyRight.isKeyPress();
                pc.moveLeft = ac.moveLeft = this.KeyLeft.isKeyPress();
            }
        }
        if (!ac.isDestroyed() && this.KeyFlare.isKeyDown()) {
            if (ac.getSeatIdByEntity(player) <= 1)
                if (ac.canUseFlare() && ac.useFlare(ac.getCurrentFlareType())) {
                    pc.useFlareType = (byte) ac.getCurrentFlareType();
                    ac.nextFlareType();
                    send = true;
                } else {
                    playSoundNG();
                }
        }
        if (!ac.isDestroyed() && this.KeyChaff.isKeyDown()) {
            if (ac.getSeatIdByEntity(player) <= 1) {
                if (ac.canUseChaff() && ac.useChaff()) {
                    pc.useChaff = true;
                    send = true;
                } else {
                    playSoundNG();
                }
            }
        }
        if (!ac.isDestroyed() && this.KeyMaintenance.isKeyDown()) {
            if (ac.getSeatIdByEntity(player) <= 1) {
                if (ac.canUseMaintenance() && ac.useMaintenance()) {
                    pc.useMaintenance = true;
                    send = true;
                } else {
                    playSoundNG();
                }
            }
        }
        if (!ac.isDestroyed() && this.KeyAPS.isKeyDown()) {
            if (ac.getSeatIdByEntity(player) <= 1) {
                if (ac.canUseAPS() && ac.useAPS(player)) {
                    pc.useAPS = true;
                    send = true;
                } else {
                    playSoundNG();
                }
            }
        }
        if (!ac.isDestroyed() && this.KeyECMJammer.isKeyDown()) {
            if (ac.getSeatIdByEntity(player) <= 1) {
                if (ac.canUseECMJammer() && ac.useECMJammer(player)) {
                    pc.useECMJammer = true;
                    send = true;
                } else {
                    playSoundNG();
                }
            }
        }
        if (!ac.isDestroyed() && !ac.isPilotReloading()) {
            if (ac.getSeatIdByEntity(player) <= 1) {
                if (ac.getAcInfo() != null && ac.getAcInfo().enableRadar && this.KeyRadarSwitch.isKeyDown()) {
                    boolean newRadarEnabled = !ac.isRadarEnabledRuntime();
                    ac.setRadarEnabledRuntime(newRadarEnabled, true);
                    MCH_RenderRWR.handleRadarPowerStateChanged(ac, newRadarEnabled);
                    MCH_MOD.getPacketHandler().sendToServer(new PacketRadarSwitchState(ac.getEntityId(), newRadarEnabled));
                    playSoundOK();
                }
                boolean armCurrentWeapon = MCH_RenderRWR.isArmCurrentWeapon(ac, player);
                boolean armNarrowBandMode = MCH_RenderRWR.isArmNarrowBandCurrentWeapon(ac, player);
                int fireControlToggle;
                if (armNarrowBandMode) {
                    fireControlToggle = MCH_RenderRWR.handleArmSelectKey(this.KeyFireControlLock.isKeyDown(), player, ac);
                } else if (armCurrentWeapon) {
                    fireControlToggle = 0;
                } else if (ac.getAcInfo() != null && ac.getAcInfo().enableRadar && ac.isRadarEnabledRuntime()) {
                    fireControlToggle = MCH_RenderRWR.handleRadarSelectKey(this.KeyFireControlLock.isKeyDown(), player, ac);
                } else {
                    fireControlToggle = MCH_RenderLeadCircle.handleFireControlLockKey(this.KeyFireControlLock.isKeyDown(), player, ac);
                }
                if (fireControlToggle == 1 || fireControlToggle == -1) {
                    playSoundOK();
                } else if (fireControlToggle == 2) {
                    playSoundNG();
                }
            }
            if (this.KeyOpenGPSPanel.isKeyDown()) {
                this.mc.displayGuiScreen(new MCH_GuiGPSInput(player));
            }

            boolean lockKeyPress = this.KeyCurrentWeaponLock.isKeyPress();
            boolean armCurrentWeapon = MCH_RenderRWR.isArmCurrentWeapon(ac, player);
            boolean armNarrowBandMode = MCH_RenderRWR.isArmNarrowBandCurrentWeapon(ac, player);
            if (armCurrentWeapon) {
                // ARM窄频使用RWR目标层锁定，不走常规武器锁定与雷达STT上报
                ac.currentWeaponUnlock(player);
                MCH_RenderRWR.clearRadarTrackForArmMode(ac);
                if (armNarrowBandMode) {
                    int armTrackToggle = MCH_RenderRWR.handleArmTrackToggleKey(lockKeyPress, player, ac);
                    if (armTrackToggle == 1 || armTrackToggle == -1) {
                        playSoundOK();
                    } else if (armTrackToggle == 2) {
                        playSoundNG();
                    }
                }
            } else {
                if (lockKeyPress) {
                    ac.currentWeaponLock(player);
                    send = true;
                } else {
                    ac.currentWeaponUnlock(player);
                }
            }
            boolean radarEnabled = ac.getAcInfo() != null && ac.getAcInfo().enableRadar && ac.isRadarEnabledRuntime();
            if (radarEnabled) {
                int acmToggle = MCH_RenderRWR.handleRadarAcmToggleKey(MCH_Key.isKeyDown(-98), player, ac);
                if (acmToggle == 1 || acmToggle == -1) {
                    playSoundOK();
                } else if (acmToggle == 2) {
                    playSoundNG();
                }
                boolean weaponNeedsRightLock = shouldKeepWeaponRightLock(ac, player);
                boolean hasRadarTracking = MCH_RenderRWR.getRadarTrackingTargetId(ac) > 0;
                boolean allowRadarToggle = !weaponNeedsRightLock || !hasRadarTracking;
                if (allowRadarToggle && !armCurrentWeapon) {
                    int trackToggle = MCH_RenderRWR.handleRadarTrackToggleKey(lockKeyPress, player, ac);
                    if (trackToggle == 1 || trackToggle == -1) {
                        playSoundOK();
                    } else if (trackToggle == 2) {
                        playSoundNG();
                    }
                }
            }

            updateAheadPreSolve(player, ac);

            if (this.KeySwitchWeapon1.isKeyDown() || this.KeySwitchWeapon2.isKeyDown() || getMouseWheel() != 0) {
                if (getMouseWheel() > 0) {
                    pc.switchWeapon = (byte) ac.getNextWeaponID((Entity) player, -1);
                } else {
                    pc.switchWeapon = (byte) ac.getNextWeaponID((Entity) player, 1);
                }
                setMouseWheel(0);
                ac.switchWeapon((Entity) player, pc.switchWeapon);
                send = true;
            } else if (this.KeySwWeaponMode.isKeyDown()) {
                MCH_WeaponSet ws = ac.getCurrentWeapon(player);
                MCH_WeaponInfo info = ws != null ? ws.getInfo() : null;
                boolean canDataLinkToggle = info != null && info.enableDataLink && !info.onlyDataLink && (info.activeRadar || info.passiveRadar || info.semiActiveRadar) && !info.antiRadiationMissile;
                if (canDataLinkToggle) {
                    ws.toggleDataLinkMode();
                    playSoundOK();
                } else {
                    ac.switchCurrentWeaponMode(player);
                }
            } else if (this.KeyUseWeapon.isKeyPress()) {
                if (ac.useCurrentWeapon(player)) {
                    MCH_MOD.getPacketHandler().sendToServer(new PacketUseWeapon(
                        ac.getCurrentWeapon(player).getLastUsedOptionParameter1(),
                        ac.getCurrentWeapon(player).getLastUsedOptionParameter2(),
                        ac.prevPosX,
                        ac.prevPosY,
                        ac.prevPosZ
                    ));
                }
            }

        }
        return (send || player.ticksExisted % 100 == 0);
    }

    private boolean shouldKeepWeaponRightLock(MCH_EntityAircraft ac, EntityPlayer player) {
        if (ac == null || player == null) {
            return false;
        }
        MCH_WeaponSet ws = ac.getCurrentWeapon(player);
        if (ws == null || ws.getCurrentWeapon() == null || ws.getCurrentWeapon().getInfo() == null) {
            return false;
        }
        MCH_WeaponInfo info = ws.getCurrentWeapon().getInfo();
        String type = info.type != null ? info.type.toLowerCase() : "";
        if (info.enableDataLink && info.onlyDataLink) {
            ws.setDataLinkMode(true);
        }
        // These weapon modes rely on right-click guidance/lock and should keep their original behavior.
        return info.passiveRadar || info.isGPSMissile || info.laserGuidance || "tvmissile".equals(type);
    }

    private void updateAheadPreSolve(EntityPlayer player, MCH_EntityAircraft ac) {
        if (ac.getSeatIdByEntity(player) > 1) {
            return;
        }
        MCH_WeaponSet ws = ac.getCurrentWeapon(player);
        if (ws == null || ws.getInfo() == null || ws.getCurrentWeapon() == null) {
            return;
        }
        MCH_WeaponInfo info = ws.getInfo();
        MCH_WeaponBase wb = ws.getCurrentWeapon();
        if (!info.ahead) {
            return;
        }
        if (info.spawnBulletInAir) {
            syncAirburstDistance(ac, wb, 0);
            return;
        }
        int lockTargetId = MCH_RenderLeadCircle.getLeadLockedTargetId(ac);
        if (lockTargetId <= 0) {
            syncAirburstDistance(ac, wb, 0);
            return;
        }
        int interval = Math.max(1, info.aheadSolveIntervalTick);
        if (player.ticksExisted % interval != 0) {
            return;
        }
        MCH_EntityInfo target = MCH_EntityInfoClientTracker.getEntityInfo(lockTargetId);
        if (target == null) {
            syncAirburstDistance(ac, wb, 0);
            return;
        }
        Vec3 shotPos = wb.getShotPos(ac);
        double sx = ac.posX + shotPos.xCoord;
        double sy = ac.posY + shotPos.yCoord;
        double sz = ac.posZ + shotPos.zCoord;
        double tx = target.posX;
        double ty = target.posY + 1.0D;
        double tz = target.posZ;
        double tvx = target.posX - target.lastTickPosX;
        double tvy = target.posY - target.lastTickPosY;
        double tvz = target.posZ - target.lastTickPosZ;
        double speed = wb.acceleration;
        if (info.speedDependsAircraft) {
            speed += Math.sqrt(ac.motionX * ac.motionX + ac.motionY * ac.motionY + ac.motionZ * ac.motionZ);
        }
        if (speed <= 1.0E-6D) {
            syncAirburstDistance(ac, wb, 0);
            return;
        }
        double rx = tx - sx;
        double ry = ty - sy;
        double rz = tz - sz;
        double a = tvx * tvx + tvy * tvy + tvz * tvz - speed * speed;
        double b = 2.0D * (rx * tvx + ry * tvy + rz * tvz);
        double c = rx * rx + ry * ry + rz * rz;
        double t = -1.0D;
        if (Math.abs(a) < 1.0E-6D) {
            if (Math.abs(b) > 1.0E-6D) {
                t = -c / b;
            }
        } else {
            double d = b * b - 4.0D * a * c;
            if (d < 0.0D) {
                syncAirburstDistance(ac, wb, 0);
                return;
            }
            double sqrtD = Math.sqrt(d);
            double t1 = (-b - sqrtD) / (2.0D * a);
            double t2 = (-b + sqrtD) / (2.0D * a);
            if (t1 > 0.0D && t2 > 0.0D) {
                t = Math.min(t1, t2);
            } else if (t1 > 0.0D) {
                t = t1;
            } else if (t2 > 0.0D) {
                t = t2;
            }
        }
        if (t <= 0.0D || t > 600.0D) {
            syncAirburstDistance(ac, wb, 0);
            return;
        }
        double px = tx + tvx * t;
        double py = ty + tvy * t;
        double pz = tz + tvz * t;
        double impactDist = Math.sqrt((px - sx) * (px - sx) + (py - sy) * (py - sy) + (pz - sz) * (pz - sz));
        int triggerDist = (int) Math.floor(impactDist - info.proximityFuseDist);
        if (triggerDist <= 5 || triggerDist >= 3000) {
            triggerDist = 0;
        }
        syncAirburstDistance(ac, wb, triggerDist);
    }

    private void syncAirburstDistance(MCH_EntityAircraft ac, MCH_WeaponBase wb, int dist) {
        if (wb.airburstDist == dist) {
            return;
        }
        wb.setAirburstDist(dist);
        MCH_MOD.getPacketHandler().sendToServer(new PacketAirburstDistReset(ac.getEntityId(), dist));
    }
}
