package mcheli;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.aircraft.*;
import mcheli.command.MCH_GuiTitle;
import mcheli.economy.MCH_EconomyClientData;
import mcheli.gltd.MCH_ClientGLTDTickHandler;
import mcheli.gltd.MCH_EntityGLTD;
import mcheli.gltd.MCH_GuiGLTD;
import mcheli.gui.MCH_Gui;
import mcheli.helicopter.MCH_ClientHeliTickHandler;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.helicopter.MCH_GuiHeli;
import mcheli.lweapon.MCH_ClientLightWeaponTickHandler;
import mcheli.lweapon.MCH_GuiLightWeapon;
import mcheli.lweapon.MCH_ItemLightWeaponBase;
import mcheli.mob.MCH_GuiSpawnGunner;
import mcheli.multiplay.MCH_GuiScoreboard;
import mcheli.multiplay.MCH_GuiTargetMarker;
import mcheli.multiplay.MCH_MultiplayClient;
import mcheli.plane.MCP_ClientPlaneTickHandler;
import mcheli.plane.MCP_EntityPlane;
import mcheli.plane.MCP_GuiPlane;
import mcheli.tank.MCH_ClientTankTickHandler;
import mcheli.tank.MCH_EntityTank;
import mcheli.tank.MCH_GuiTank;
import mcheli.tool.MCH_ClientToolTickHandler;
import mcheli.tool.MCH_GuiWrench;
import mcheli.tool.MCH_ItemWrench;
import mcheli.tool.rangefinder.MCH_GuiRangeFinder;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.vehicle.MCH_ClientVehicleTickHandler;
import mcheli.vehicle.MCH_EntityVehicle;
import mcheli.vehicle.MCH_GuiVehicle;
import mcheli.weapon.MCH_GPSPosition;
import mcheli.weapon.MCH_LaserStateStore;
import mcheli.weapon.MCH_RenderLaser;
import mcheli.weapon.MCH_WeaponSet;
import mcheli.wrapper.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mcheli.network.packets.PacketLockTargetBVR;
import mcheli.render.MCH_RenderBVRLockBox;
import mcheli.weapon.MCH_EntityAAMissile;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.MCH_MOD;

@SideOnly(Side.CLIENT)
public class MCH_ClientCommonTickHandler extends W_TickHandler {

    public static final float hitTotalDamageScaleOrigin = 2.0f;
    private static final ResourceLocation cross3rd = new ResourceLocation(W_MOD.DOMAIN, "textures/3rdCross.png");
    private static final ResourceLocation TEX_ICON_SL = new ResourceLocation("mcheli", "textures/gui/economy/coin_sl.png");
    private static final ResourceLocation TEX_ICON_GE = new ResourceLocation("mcheli", "textures/gui/economy/coin_ge.png");
    private static final ResourceLocation TEX_ICON_RP = new ResourceLocation("mcheli", "textures/gui/economy/coin_rp.png");
    public static MCH_ClientCommonTickHandler instance;
    public static int cameraMode = 0;
    public static MCH_EntityAircraft ridingAircraft = null;
    public static boolean isDrawScoreboard = false;
    public static int sendLDCount = 0;
    public static boolean isLocked = false;
    public static int lockedSoundCount = 0;
    public static int hitDisplayCountdown;
    public static float hitTotalDamage;
    public static int hitTotalDamageClearCountdown;
    public static float hitTotalDamageScale = 2.0f;
    public static List<HitMessage> hitList = new ArrayList<>();
    public static int nukeFlashElapsedTick = -1;
    public static int nukeFlashDurationTick = 0;
    public static float nukeFlashPeakAlpha = 0.0F;
    public static float nukeFlashPow01 = 0.0F;
    public static boolean enableNew3rdCamera = true;
    public static boolean showVehicleCrossHair = false;
    @SideOnly(Side.CLIENT)
    public static EntityLivingBase camera;
    private static double prevMouseDeltaX;
    private static double prevMouseDeltaY;
    private static double mouseDeltaX = 0.0D;
    private static double mouseDeltaY = 0.0D;
    private static double mouseRollDeltaX = 0.0D;
    private static double mouseRollDeltaY = 0.0D;
    private static boolean isRideAircraft = false;
    private static float prevTick = 0.0F;
    private final RenderItem economyHudItemRenderer = new RenderItem();
    public MCH_GuiCommon gui_Common;
    public MCH_Gui gui_Heli;
    public MCH_Gui gui_Plane;
    public MCH_Gui gui_Tank;
    public MCH_Gui gui_GLTD;
    public MCH_Gui gui_Vehicle;
    public MCH_Gui gui_LWeapon;
    public MCH_Gui gui_Wrench;
    public MCH_Gui gui_SwnGnr;
    public MCH_Gui gui_EMarker;
    public MCH_Gui gui_RngFndr;
    public MCH_Gui gui_Title;
    public MCH_Gui[] guis;
    public MCH_Gui[] guiTicks;
    public MCH_ClientTickHandlerBase[] ticks;
    public MCH_Key[] Keys;
    public MCH_Key KeyCamDistUp;
    public MCH_Key KeyCamDistDown;
    public MCH_Key KeyScoreboard;
    public MCH_Key KeyMultiplayManager;
    // Client-side cache of missile IDs we should keep sending BVR lock packets to
    private static final java.util.HashMap<Integer, Long> BVR_MISSILE_ID_CACHE = new java.util.HashMap<Integer, Long>();
    private static final long BVR_MISSILE_ID_TTL_MS = 30_000L; // keep for 30 seconds

    //Tracks the last camera mode like normal, thermal, nigth vision
    private int lastAppliedCameraMode = -1;

    private int[] getAndUpdateTrackedBvrMissileIds(MCH_EntityAircraft ac) {
        long now = System.currentTimeMillis();

        // 1) Add newly seen missiles from the loaded entity list (only ones near us / our aircraft)
        if (mc != null && mc.theWorld != null && mc.thePlayer != null) {
            for (Object o : mc.theWorld.loadedEntityList) {
                if (!(o instanceof mcheli.weapon.MCH_EntityAAMissile)) continue;
                mcheli.weapon.MCH_EntityAAMissile msl = (mcheli.weapon.MCH_EntityAAMissile) o;

                mcheli.weapon.MCH_WeaponInfo info = msl.getInfo();
                if (info == null) continue;
                if (!info.enableBVR || !(info.passiveRadar || info.semiActiveRadar)) continue;

                // Heuristic ownership: only cache missiles that are close to our controlled aircraft or player shortly after launch.
                // This avoids depending on shootingEntity/shootingAircraft which are often null on client.
                double dSq = (ac != null) ? msl.getDistanceSqToEntity(ac) : msl.getDistanceSqToEntity(mc.thePlayer);

                // Within 300 blocks of our aircraft/player OR just spawned recently (so we likely fired it)
                if (dSq < (300.0 * 300.0) || msl.ticksExisted < 40) {
                    int id = msl.getEntityId();
                    if (!BVR_MISSILE_ID_CACHE.containsKey(id)) {
                        BVR_MISSILE_ID_CACHE.put(id, now);
                    }

                }
            }
        }

        // 2) Cull expired entries
        java.util.Iterator<java.util.Map.Entry<Integer, Long>> it = BVR_MISSILE_ID_CACHE.entrySet().iterator();

        while (it.hasNext()) {
            java.util.Map.Entry<Integer, Long> e = it.next();
            if (now - e.getValue() > BVR_MISSILE_ID_TTL_MS) {
                it.remove();
            }
        }
        // 2b) Cull entries whose entity no longer exists (exploded / despawned)
        java.util.Iterator<java.util.Map.Entry<Integer, Long>> it2 = BVR_MISSILE_ID_CACHE.entrySet().iterator();
        while (it2.hasNext()) {
            java.util.Map.Entry<Integer, Long> e = it2.next();
            Entity ent = mc.theWorld.getEntityByID(e.getKey());
            if (ent == null || ent.isDead) {
                it2.remove();
            }
        }

        // 3) Return as int[]
        int[] out = new int[BVR_MISSILE_ID_CACHE.size()];
        int i = 0;
        for (Integer id : BVR_MISSILE_ID_CACHE.keySet()) out[i++] = id;
        return out;
    }

    public MCH_ClientCommonTickHandler(Minecraft minecraft, MCH_Config config) {
        super(minecraft);
        this.gui_Common = new MCH_GuiCommon(minecraft);
        this.gui_Heli = new MCH_GuiHeli(minecraft);
        this.gui_Plane = new MCP_GuiPlane(minecraft);
        this.gui_Tank = new MCH_GuiTank(minecraft);
        this.gui_GLTD = new MCH_GuiGLTD(minecraft);
        this.gui_Vehicle = new MCH_GuiVehicle(minecraft);
        this.gui_LWeapon = new MCH_GuiLightWeapon(minecraft);
        this.gui_Wrench = new MCH_GuiWrench(minecraft);
        this.gui_SwnGnr = new MCH_GuiSpawnGunner(minecraft);
        this.gui_RngFndr = new MCH_GuiRangeFinder(minecraft);
        this.gui_EMarker = new MCH_GuiTargetMarker(minecraft);
        this.gui_Title = new MCH_GuiTitle(minecraft);
        this.guis = new MCH_Gui[]{this.gui_RngFndr, this.gui_LWeapon, this.gui_Heli, this.gui_Plane, this.gui_Tank, this.gui_GLTD, this.gui_Vehicle};
        this.guiTicks = new MCH_Gui[]{this.gui_Common, this.gui_Heli, this.gui_Plane, this.gui_Tank, this.gui_GLTD, this.gui_Vehicle, this.gui_LWeapon, this.gui_Wrench, this.gui_SwnGnr, this.gui_RngFndr, this.gui_EMarker, this.gui_Title};
        this.ticks = new MCH_ClientTickHandlerBase[]{new MCH_ClientHeliTickHandler(minecraft, config), new MCP_ClientPlaneTickHandler(minecraft, config), new MCH_ClientTankTickHandler(minecraft, config), new MCH_ClientGLTDTickHandler(minecraft, config), new MCH_ClientVehicleTickHandler(minecraft, config), new MCH_ClientLightWeaponTickHandler(minecraft, config), new MCH_ClientSeatTickHandler(minecraft, config), new MCH_ClientToolTickHandler(minecraft, config)};
        this.updatekeybind(config);
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static double getCurrentStickX() {
        return mouseRollDeltaX;
    }

    public static double getCurrentStickY() {
        double inv = 1.0D;
        if (Minecraft.getMinecraft().gameSettings.invertMouse) {
            inv = -inv;
        }

        if (MCH_Config.InvertMouse.prmBool) {
            inv = -inv;
        }

        return mouseRollDeltaY * inv;
    }

    public static double getMaxStickLength() {
        return 40.0D;
    }

    public static void addHitMessage(HitMessage message) {
        int maxMessage = 5;
        if (hitList.size() >= maxMessage) {
            hitList.remove(0);
        }
        hitList.add(message);
    }

    public static void addTotalDamage(float damage) {
        if (damage < 5) {
        } else if (damage < 12) {
            hitTotalDamageScale = 2.2f;
        } else if (damage < 35) {
            hitTotalDamageScale = 2.4f;
        } else if (damage < 60) {
            hitTotalDamageScale = 2.6f;
        } else {
            hitTotalDamageScale = 2.8f;
        }
        hitTotalDamage += damage;
    }

    public void updatekeybind(MCH_Config config) {
        this.KeyCamDistUp = new MCH_Key(MCH_Config.KeyCameraDistUp.prmInt);
        this.KeyCamDistDown = new MCH_Key(MCH_Config.KeyCameraDistDown.prmInt);
        this.KeyScoreboard = new MCH_Key(MCH_Config.KeyScoreboard.prmInt);
        this.KeyMultiplayManager = new MCH_Key(MCH_Config.KeyMultiplayManager.prmInt);
        this.Keys = new MCH_Key[]{this.KeyCamDistUp, this.KeyCamDistDown, this.KeyScoreboard, this.KeyMultiplayManager};
        MCH_ClientTickHandlerBase[] arr$ = this.ticks;

        for (MCH_ClientTickHandlerBase t : arr$) {
            t.updateKeybind(config);
        }

    }

    private void sendBvrSarhGuidancePackets() {
        if (mc == null || mc.theWorld == null || mc.thePlayer == null) return;

        // Only when player is in an aircraft/seat/UAV control and has a weapon selected that supports BVR SARH
        mcheli.aircraft.MCH_EntityAircraft ac = null;
        if (mc.thePlayer.ridingEntity instanceof mcheli.aircraft.MCH_EntityAircraft) {
            ac = (mcheli.aircraft.MCH_EntityAircraft) mc.thePlayer.ridingEntity;
        } else if (mc.thePlayer.ridingEntity instanceof mcheli.aircraft.MCH_EntitySeat) {
            ac = ((mcheli.aircraft.MCH_EntitySeat) mc.thePlayer.ridingEntity).getParent();
        } else if (mc.thePlayer.ridingEntity instanceof mcheli.uav.MCH_EntityUavStation) {
            ac = ((mcheli.uav.MCH_EntityUavStation) mc.thePlayer.ridingEntity).getControlAircract();
        }
        if (ac == null || ac.getCurrentWeapon(mc.thePlayer) == null || ac.getCurrentWeapon(mc.thePlayer).getCurrentWeapon() == null) return;

        MCH_WeaponInfo wi = ac.getCurrentWeapon(mc.thePlayer).getCurrentWeapon().getInfo();

        if (wi == null) return;

        // We only care about passive radar + BVR
        if (!wi.enableBVR || !wi.passiveRadar) return;

        // Throttle traffic a bit
        if ((mc.thePlayer.ticksExisted % 5) != 0) return;

        int[] missileIds = getAndUpdateTrackedBvrMissileIds(ac);

        if (missileIds.length == 0) return;

        // Use the “best locked” target picked by the renderer (red box)
        MCH_EntityInfo tgt = MCH_RenderBVRLockBox.bestLockedEntity;

        if (tgt != null && (System.currentTimeMillis() - MCH_RenderBVRLockBox.bestLockedEntityTimeMs) < 500L) {
            int px = (int) Math.floor(tgt.posX);
            int py = (int) Math.floor(tgt.posY);
            int pz = (int) Math.floor(tgt.posZ);
            if (py <= 0) py = 1;


            for (int mslId : missileIds) {
                MCH_MOD.getPacketHandler().sendToServer(
                        new PacketLockTargetBVR(mslId, tgt.entityId, px, py, pz)
                );
            }
        } else {
            // No hard-lock target => disable BVR guidance
            for (int mslId : missileIds) {
                MCH_MOD.getPacketHandler().sendToServer(new PacketLockTargetBVR(mslId, 0, 0, -1, 0));
            }
        }
    }

    public void onTick() {
        Minecraft minecraft = Minecraft.getMinecraft();
        MCH_ClientTickHandlerBase.initRotLimit();
        MCH_Key[] player = this.Keys;
        int inOtherGui = player.length;

        for (int ac = 0; ac < inOtherGui; ++ac) {
            MCH_Key len$ = player[ac];
            len$.update();
        }

        EntityClientPlayerMP var7 = super.mc.thePlayer;
        if (var7 != null && super.mc.currentScreen == null) {
            if (MCH_ServerSettings.enableCamDistChange && (this.KeyCamDistUp.isKeyDown() || this.KeyCamDistDown.isKeyDown())) {
                inOtherGui = (int) W_Reflection.getThirdPersonDistance();
                if (this.KeyCamDistUp.isKeyDown() && inOtherGui < 60) {
                    inOtherGui += 4;
                    if (inOtherGui > 60) {
                        inOtherGui = 60;
                    }

                    W_Reflection.setThirdPersonDistance((float) inOtherGui);
                } else if (this.KeyCamDistDown.isKeyDown()) {
                    inOtherGui -= 4;
                    if (inOtherGui < 4) {
                        inOtherGui = 4;
                    }

                    W_Reflection.setThirdPersonDistance((float) inOtherGui);
                }
            }

            if (super.mc.currentScreen == null) {
                label85:
                {
                    if (super.mc.isSingleplayer()) {
                        if (!MCH_Config.DebugLog) {
                            break label85;
                        }
                    }

                    isDrawScoreboard = this.KeyScoreboard.isKeyPress();
                    if (!isDrawScoreboard && this.KeyMultiplayManager.isKeyDown()) {
                        MCH_PacketIndOpenScreen.send(5);
                    }
                }
            }
        }

        if (sendLDCount < 10) {
            ++sendLDCount;
        } else {
            MCH_MultiplayClient.sendImageData();
            sendLDCount = 0;
        }

        boolean var12 = super.mc.currentScreen != null;
        MCH_ClientTickHandlerBase[] var8 = this.ticks;
        int var10 = var8.length;

        int i$;
        for (i$ = 0; i$ < var10; ++i$) {
            MCH_ClientTickHandlerBase g = var8[i$];
            g.onTick(var12);
        }

        MCH_Gui[] var9 = this.guiTicks;
        var10 = var9.length;

        for (i$ = 0; i$ < var10; ++i$) {
            MCH_Gui var13 = var9[i$];
            var13.onTick();
        }

        MCH_EntityAircraft var11 = MCH_EntityAircraft.getAircraft_RiddenOrControl(var7);
        if (var7 != null && var11 != null && !var11.isDestroyed()) {
            if (isLocked && lockedSoundCount == 0) {
                isLocked = false;
                lockedSoundCount = 20;
                MCH_ClientTickHandlerBase.playSound("locked");
            }
        } else {
            lockedSoundCount = 0;
            isLocked = false;
        }

        if (lockedSoundCount > 0) {
            --lockedSoundCount;
        }

        if (hitDisplayCountdown > 0) {
            hitDisplayCountdown--;
            if (hitDisplayCountdown == 0) {
                hitList.clear();
            }
        }

        if (hitTotalDamageClearCountdown > 0) {
            hitTotalDamageClearCountdown--;
            if (hitTotalDamageClearCountdown == 0) {
                hitTotalDamage = 0;
            }
        }
        if (hitTotalDamageScale > hitTotalDamageScaleOrigin) {
            hitTotalDamageScale *= 0.95f;
            hitTotalDamageScale = Math.max(hitTotalDamageScale, hitTotalDamageScaleOrigin);
        }
        tickNukeFlashEffect();

        if (var7 != null) {
            this.ensureCameraShaderState(var7);
        } else {
            cameraMode = 0;
            MCH_Camera.currentCameraMode = 0;
            if (W_EntityRenderer.hasActiveShader()) {
                W_EntityRenderer.deactivateShader();
            }
        }

        //第三人称摄像机视角
        if (enableNew3rdCamera
            && minecraft.thePlayer.ridingEntity instanceof MCH_EntityAircraft
            && minecraft.gameSettings.thirdPersonView != 0) {
            if (camera == null) {
                camera = new MCH_3rdCamera(minecraft.theWorld, (MCH_EntityAircraft) minecraft.thePlayer.ridingEntity);
                minecraft.thePlayer.worldObj.spawnEntityInWorld(camera);
            }
            minecraft.renderViewEntity = camera;
            showVehicleCrossHair = true;
        } else {
            if (camera != null) {
                camera.setDead();
                camera = null;
            }
            showVehicleCrossHair = false;
        }

        // GPS cleanup: avoid one-tick flicker around shot/reload transitions for handheld laser guidance.
        boolean holdingLightWeapon = minecraft.thePlayer.getHeldItem() != null
            && minecraft.thePlayer.getHeldItem().getItem() instanceof MCH_ItemLightWeaponBase;
        boolean usingLightWeapon = MCH_ItemLightWeaponBase.isHeld(minecraft.thePlayer);
        boolean ownGpsActive = MCH_GPSPosition.currentClientGPSPosition != null
            && MCH_GPSPosition.currentClientGPSPosition.isActive
            && MCH_GPSPosition.currentClientGPSPosition.owner != null
            && MCH_GPSPosition.currentClientGPSPosition.owner.getEntityId() == minecraft.thePlayer.getEntityId();
        if (minecraft.thePlayer.ridingEntity == null && !usingLightWeapon && !(holdingLightWeapon && ownGpsActive)) {
            MCH_GPSPosition.currentClientGPSPosition.isActive = false;
        }

        MCH_LaserStateStore.expireClientStates(minecraft.theWorld.getTotalWorldTime(), MCH_LaserStateStore.DEFAULT_TTL_TICKS);
        MCH_RenderLaser.tickBeams();
    }

    public void onTickPre() {
        if (super.mc.thePlayer != null && super.mc.theWorld != null) {
            this.onTick();
        }

    }

    private int getExpectedCameraMode(EntityPlayer player) {
        if (player == null) {
            return 0;
        }
        ridingAircraft = MCH_EntityAircraft.getAircraft_RiddenOrControl(player);
        if (ridingAircraft != null) {
            return ridingAircraft.getCameraMode(player);
        }
        if (player.ridingEntity instanceof MCH_EntityGLTD) {
            MCH_EntityGLTD gltd = (MCH_EntityGLTD) player.ridingEntity;
            return gltd.camera.getMode(0);
        }
        return 0;
    }

    private void ensureCameraShaderState(EntityPlayer player) {
        int expectedMode = this.getExpectedCameraMode(player);
        cameraMode = expectedMode;
        MCH_Camera.currentCameraMode = expectedMode;

        if (!W_EntityRenderer.isShaderSupport()) {
            if (W_EntityRenderer.hasActiveShader()) {
                W_EntityRenderer.deactivateShader();
            }
            return;
        }

        String expectedShader = "";
        if (expectedMode == MCH_Camera.MODE_NIGHTVISION) {
            expectedShader = "nightvision";
        } else if (expectedMode == MCH_Camera.MODE_THERMALVISION) {
            expectedShader = "thermal";
        }

        if (expectedShader.isEmpty()) {
            if (W_EntityRenderer.hasActiveShader()) {
                W_EntityRenderer.deactivateShader();
            }
            return;
        }

        if (!W_EntityRenderer.isShaderActive(expectedShader)) {
            W_EntityRenderer.activateShader(expectedShader);
        }
    }

    public void onTickPost() {
        if (super.mc.thePlayer != null && super.mc.theWorld != null) {
            MCH_GuiTargetMarker.onClientTick();
            sendBvrSarhGuidancePackets(); // <-- NEW: BVR SARH guidance does NOT require RMB
        }
        MCH_PlayerViewHandler.onUpdate();

    }

    public void updateMouseDelta(boolean stickMode, float partialTicks) {
        prevMouseDeltaX = mouseDeltaX;
        prevMouseDeltaY = mouseDeltaY;
        mouseDeltaX = 0.0D;
        mouseDeltaY = 0.0D;
        if (super.mc.inGameHasFocus && Display.isActive() && super.mc.currentScreen == null) {
            if (stickMode) {
                if (Math.abs(mouseRollDeltaX) < getMaxStickLength() * 0.2D) {
                    mouseRollDeltaX *= 1.0F - 0.15F * partialTicks;
                }

                if (Math.abs(mouseRollDeltaY) < getMaxStickLength() * 0.2D) {
                    mouseRollDeltaY *= 1.0F - 0.15F * partialTicks;
                }
            }

            super.mc.mouseHelper.mouseXYChange();
            float f1 = super.mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
            float f2 = f1 * f1 * f1 * 8.0F;
            double ms = MCH_Config.MouseSensitivity.prmDouble * 0.1D;
            mouseDeltaX = ms * (double) super.mc.mouseHelper.deltaX * (double) f2;
            mouseDeltaY = ms * (double) super.mc.mouseHelper.deltaY * (double) f2;
            byte inv = 1;
            if (super.mc.gameSettings.invertMouse) {
                inv = -1;
            }

            if (MCH_Config.InvertMouse.prmBool) {
                inv *= -1;
            }

            mouseRollDeltaX += mouseDeltaX;
            mouseRollDeltaY += mouseDeltaY * (double) inv;
            double dist = mouseRollDeltaX * mouseRollDeltaX + mouseRollDeltaY * mouseRollDeltaY;
            if (dist > 1.0D) {
                dist = MathHelper.sqrt_double(dist);
                double d = dist;
                if (dist > getMaxStickLength()) {
                    d = getMaxStickLength();
                }

                mouseRollDeltaX /= dist;
                mouseRollDeltaY /= dist;
                mouseRollDeltaX *= d;
                mouseRollDeltaY *= d;
            }
        }

    }

    public void onRenderTickPre(float partialTicks) {
        MCH_GuiTargetMarker.clearMarkEntityPos();
        if (!MCH_ServerSettings.enableDebugBoundingBox) {
            RenderManager.debugBoundingBox = false;
        }

        MCH_ClientEventHook.haveSearchLightAircraft.clear();
        if (super.mc != null && super.mc.theWorld != null) {
            for (Object currentItemstack : Minecraft.getMinecraft().theWorld.loadedEntityList) {
                if (currentItemstack instanceof MCH_EntityAircraft && ((MCH_EntityAircraft) currentItemstack).haveSearchLight()) {
                    MCH_ClientEventHook.haveSearchLightAircraft.add(currentItemstack);
                }
            }
        }

        if (super.mc == null || super.mc.thePlayer == null || super.mc.theWorld == null) {
            applyLocalCameraMode(0);
            return;
        }

        if (!W_McClient.isGamePaused()) {
            EntityClientPlayerMP var17 = super.mc.thePlayer;
            if (var17 != null) {
                ItemStack var18 = var17.getCurrentEquippedItem();
                if (var18 != null && var18.getItem() instanceof MCH_ItemWrench && var17.getItemInUseCount() > 0) {
                    W_Reflection.setItemRendererProgress(1.0F);
                }

                this.ensureCameraShaderState(var17);

                applyLocalCameraMode(localMode);

                MCH_EntityAircraft var19 = null;
                if (!(var17.ridingEntity instanceof MCH_EntityHeli) && !(var17.ridingEntity instanceof MCP_EntityPlane) && !(var17.ridingEntity instanceof MCH_EntityTank)) {
                    if (var17.ridingEntity instanceof MCH_EntityUavStation) {
                        var19 = ((MCH_EntityUavStation) var17.ridingEntity).getControlAircract();
                    } else if (var17.ridingEntity instanceof MCH_EntityVehicle) {
                        MCH_EntityAircraft stickMode = (MCH_EntityAircraft) var17.ridingEntity;
                        stickMode.setupAllRiderRenderPosition(partialTicks, var17);
                    }
                } else {
                    var19 = (MCH_EntityAircraft) var17.ridingEntity;
                }

                boolean var20 = false;
                if (var19 instanceof MCH_EntityHeli) {
                    var20 = MCH_Config.MouseControlStickModeHeli.prmBool;
                }

                if (var19 instanceof MCP_EntityPlane) {
                    var20 = MCH_Config.MouseControlStickModePlane.prmBool;
                }

                for (int de = 0; de < 10 && prevTick > partialTicks; ++de) {
                    --prevTick;
                }

                float p;
                float r;
                if (var19 != null && var19.canMouseRot()) {
                    if (!isRideAircraft) {
                        var19.onInteractFirst(var17);
                    }

                    isRideAircraft = true;
                    this.updateMouseDelta(var20, partialTicks);
                    boolean var22 = false;
                    float var23 = 0.0F;
                    float var25 = 0.0F;
                    MCH_SeatInfo var26 = var19.getSeatInfo(var17);
                    if (var26 != null && var26.fixRot && var19.getIsGunnerMode(var17) && !var19.isGunnerLookMode(var17)) {
                        var22 = true;
                        var23 = var26.fixYaw;
                        var25 = var26.fixPitch;
                        mouseRollDeltaX *= 0.0D;
                        mouseRollDeltaY *= 0.0D;
                        mouseDeltaX *= 0.0D;
                        mouseDeltaY *= 0.0D;
                    } else if (var19.isPilot(var17)) {
                        MCH_AircraftInfo.CameraPosition var28 = var19.getCameraPosInfo();
                        if (var28 != null) {
                            var23 = var28.yaw;
                            var25 = var28.pitch;
                        }
                    }

                    if (var19.getAcInfo() == null) {
                        var17.setAngles((float) mouseDeltaX, (float) mouseDeltaY);
                    } else {
                        var19.setAngles(var17, var22, var23, var25, (float) (mouseDeltaX + prevMouseDeltaX) / 2.0F, (float) (mouseDeltaY + prevMouseDeltaY) / 2.0F, (float) mouseRollDeltaX, (float) mouseRollDeltaY, partialTicks - prevTick);
                    }

                    var19.setupAllRiderRenderPosition(partialTicks, var17);
                    double var29 = MathHelper.sqrt_double(mouseRollDeltaX * mouseRollDeltaX + mouseRollDeltaY * mouseRollDeltaY);
                    if (!var20 || var29 < getMaxStickLength() * 0.1D) {
                        mouseRollDeltaX *= 0.95D;
                        mouseRollDeltaY *= 0.95D;
                    }

                    p = MathHelper.wrapAngleTo180_float(var19.getRotRoll());
                    r = MathHelper.wrapAngleTo180_float(var19.getRotYaw() - var17.rotationYaw);
                    p *= MathHelper.cos((float) ((double) r * 3.141592653589793D / 180.0D));
                    if (var19.getTVMissile() != null && W_Lib.isClientPlayer(var19.getTVMissile().shootingEntity) && var19.getIsGunnerMode(var17)) {
                        p = 0.0F;
                    }

                    W_Reflection.setCameraRoll(p);
                    this.correctViewEntityDummy(var17);
                } else {
                    MCH_EntitySeat var21 = var17.ridingEntity instanceof MCH_EntitySeat ? (MCH_EntitySeat) var17.ridingEntity : null;
                    if (var21 != null && var21.getParent() != null) {
                        this.updateMouseDelta(var20, partialTicks);
                        var19 = var21.getParent();
                        boolean wi = false;
                        MCH_SeatInfo seatInfo = var19.getSeatInfo(var17);
                        if (seatInfo != null && seatInfo.fixRot && var19.getIsGunnerMode(var17) && !var19.isGunnerLookMode(var17)) {
                            wi = true;
                            mouseRollDeltaX *= 0.0D;
                            mouseRollDeltaY *= 0.0D;
                            mouseDeltaX *= 0.0D;
                            mouseDeltaY *= 0.0D;
                        }

                        Vec3 v = Vec3.createVectorHelper(mouseDeltaX, mouseRollDeltaY, 0.0D);
                        W_Vec3.rotateAroundZ((float) ((double) (var19.calcRotRoll(partialTicks) / 180.0F) * 3.141592653589793D), v);
                        MCH_WeaponSet ws = var19.getCurrentWeapon(var17);
                        mouseDeltaY *= ws != null && ws.getInfo() != null ? (double) ws.getInfo().cameraRotationSpeedPitch : 1.0D;
                        var17.setAngles((float) mouseDeltaX, (float) mouseDeltaY);
                        float y = var19.getRotYaw();
                        p = var19.getRotPitch();
                        r = var19.getRotRoll();
                        var19.setRotYaw(var19.calcRotYaw(partialTicks));
                        var19.setRotPitch(var19.calcRotPitch(partialTicks));
                        var19.setRotRoll(var19.calcRotRoll(partialTicks));
                        float revRoll = 0.0F;
                        if (wi) {
                            var17.rotationYaw = var19.getRotYaw() + seatInfo.fixYaw;
                            //System.out.println("yaw6");
                            var17.rotationPitch = var19.getRotPitch() + seatInfo.fixPitch;
                            if (var17.rotationPitch > 90.0F) {
                                var17.prevRotationPitch -= (var17.rotationPitch - 90.0F) * 2.0F;
                                var17.rotationPitch -= (var17.rotationPitch - 90.0F) * 2.0F;
                                var17.prevRotationYaw += 180.0F;
                                var17.rotationYaw += 180.0F;
                                //System.out.println("yaw7");
                                revRoll = 180.0F;
                            } else if (var17.rotationPitch < -90.0F) {
                                var17.prevRotationPitch -= (var17.rotationPitch - 90.0F) * 2.0F;
                                var17.rotationPitch -= (var17.rotationPitch - 90.0F) * 2.0F;
                                var17.prevRotationYaw += 180.0F;
                                var17.rotationYaw += 180.0F;
                                //System.out.println("yaw8");
                                revRoll = 180.0F;
                            }
                        }

                        var19.setupAllRiderRenderPosition(partialTicks, var17);
                        var19.setRotYaw(y);
                        //System.out.println("yaw9");
                        var19.setRotPitch(p);
                        var19.setRotRoll(r);
                        mouseRollDeltaX *= 0.9D;
                        mouseRollDeltaY *= 0.9D;
                        float roll = MathHelper.wrapAngleTo180_float(var19.getRotRoll());
                        float yaw = MathHelper.wrapAngleTo180_float(var19.getRotYaw() - var17.rotationYaw);
                        //System.out.println("yaw10");
                        roll *= MathHelper.cos((float) ((double) yaw * 3.141592653589793D / 180.0D));
                        //System.out.println("yaw11");
                        if (var19.getTVMissile() != null && W_Lib.isClientPlayer(var19.getTVMissile().shootingEntity) && var19.getIsGunnerMode(var17)) {
                            roll = 0.0F;
                        }

                        W_Reflection.setCameraRoll(roll + revRoll);
                        this.correctViewEntityDummy(var17);
                    } else {
                        if (isRideAircraft) {
                            W_Reflection.setCameraRoll(0.0F);
                            isRideAircraft = false;
                        }

                        mouseRollDeltaX = 0.0D;
                        mouseRollDeltaY = 0.0D;
                    }
                }

                if (var19 != null) {
                    if (var19.getSeatIdByEntity(var17) == 0 && !var19.isDestroyed()) {
                        var19.lastRiderYaw = var17.rotationYaw;
                        //System.out.println("yaw12");
                        var19.prevLastRiderYaw = var17.prevRotationYaw;
                        //System.out.println("yaw13");
                        var19.lastRiderPitch = var17.rotationPitch;
                        var19.prevLastRiderPitch = var17.prevRotationPitch;
                    }

                    var19.updateWeaponsRotation();
                }

                MCH_ViewEntityDummy var24 = MCH_ViewEntityDummy.getInstance(var17.worldObj);
                if (var24 != null) {
                    var24.rotationYaw = var17.rotationYaw;
                    //System.out.println("yaw14");
                    var24.prevRotationYaw = var17.prevRotationYaw;
                    //System.out.println("yaw15");
                    if (var19 != null) {
                        MCH_WeaponSet var27 = var19.getCurrentWeapon(var17);
                        if (var27 != null && var27.getInfo() != null && var27.getInfo().fixCameraPitch) {
                            var24.rotationPitch = var24.prevRotationPitch = 0.0F;
                        }
                    }
                }

                prevTick = partialTicks;
            }
        }
    }

    public void correctViewEntityDummy(Entity entity) {
        MCH_ViewEntityDummy de = MCH_ViewEntityDummy.getInstance(entity.worldObj);
        if (de != null) {
            if (de.rotationYaw - de.prevRotationYaw > 180.0F) {
                //System.out.println("yaw16");
                de.prevRotationYaw += 360.0F;
            } else if (de.rotationYaw - de.prevRotationYaw < -180.0F) {
                de.prevRotationYaw -= 360.0F;
                //System.out.println("yaw17");
            }
        }

    }

    public void onPlayerTickPre(EntityPlayer player) {
        if (player.worldObj.isRemote) {
            ItemStack currentItemstack = player.getCurrentEquippedItem();
            if (currentItemstack != null && currentItemstack.getItem() instanceof MCH_ItemWrench && player.getItemInUseCount() > 0 && player.getItemInUse() != currentItemstack) {
                int maxdm = currentItemstack.getMaxDamage();
                int dm = currentItemstack.getItemDamage();
                if (dm <= maxdm && dm > 0) {
                    player.setItemInUse(currentItemstack, player.getItemInUseCount());
                }
            }
        }

    }

    public void onPlayerTickPost(EntityPlayer player) {
    }

    @SubscribeEvent
    public void onRenderGameOverlayEvent(RenderGameOverlayEvent event) {
        float partialTicks = event.partialTicks;
        if (event.type == RenderGameOverlayEvent.ElementType.CROSSHAIRS && mc.thePlayer == null) {
            event.setCanceled(true);
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.thePlayer;
        ScaledResolution scaledresolution = new ScaledResolution(minecraft, minecraft.displayWidth, minecraft.displayHeight);
        int i = scaledresolution.getScaledWidth();
        int j = scaledresolution.getScaledHeight();

        if (!event.isCancelable() && event.type == RenderGameOverlayEvent.ElementType.HELMET) {
            Minecraft.getMinecraft().entityRenderer.setupOverlayRendering();
            renderNukeFlashOverlay(i, j, partialTicks);

            //渲染失明效果
            if (player != null && player.isPotionActive(Potion.blindness)
                && (player.ridingEntity instanceof MCH_EntityAircraft || player.ridingEntity instanceof MCH_EntitySeat || player.ridingEntity instanceof MCH_EntityUavStation)) {
                int amp = player.getActivePotionEffect(Potion.blindness).getAmplifier();
                int dur = player.getActivePotionEffect(Potion.blindness).getDuration();
                float alpha = 0.85f + Math.min(0.06f * (amp + 1), 0.17f);
                if (dur < 40) alpha *= (dur / 40.0f);
                GL11.glPushMatrix();
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDepthMask(false);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                Tessellator t = Tessellator.instance;
                t.startDrawingQuads();
                t.setColorRGBA(0, 0, 0, (int) (Math.max(0.0f, Math.min(0.98f, alpha)) * 255));
                t.addVertex(0, j, 0);
                t.addVertex(i, j, 0);
                t.addVertex(i, 0, 0);
                t.addVertex(0, 0, 0);
                t.draw();
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glDepthMask(true);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glPopMatrix();
            }

            if (this.mc.currentScreen == null || this.mc.currentScreen instanceof GuiChat || this.mc.currentScreen.getClass().toString().contains("GuiDriveableController")) {
                for (MCH_Gui gui : this.guis) {
                    if (drawGui(gui, partialTicks))
                        break;
                }
                drawGui(this.gui_Common, partialTicks);
                drawGui(this.gui_Wrench, partialTicks);
                drawGui(this.gui_EMarker, partialTicks);
                if (isDrawScoreboard)
                    MCH_GuiScoreboard.drawList(this.mc, this.mc.fontRenderer, false);
                drawGui(this.gui_Title, partialTicks);
            }

            //渲染第三人称准心
            if (player != null && showVehicleCrossHair) {
                final int scrW = scaledresolution.getScaledWidth();
                final int scrH = scaledresolution.getScaledHeight();
                final float cx = scrW * 0.5f;
                final float cy = scrH * 0.5f;
                final float sizePx = 32.0f;
                final float half = sizePx * 0.5f;
                GL11.glPushMatrix();
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDepthMask(false);

                GL11.glTranslatef(cx, cy, 0.0f);

                mc.getTextureManager().bindTexture(cross3rd);
                Tessellator t = Tessellator.instance;
                t.startDrawingQuads();
                t.addVertexWithUV(-half, half, 0, 0, 1);
                t.addVertexWithUV(half, half, 0, 1, 1);
                t.addVertexWithUV(half, -half, 0, 1, 0);
                t.addVertexWithUV(-half, -half, 0, 0, 0);
                t.draw();
                GL11.glDepthMask(true);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glPopMatrix();
            }
        }

        //渲染命中信息
        if (!event.isCancelable() && event.type == RenderGameOverlayEvent.ElementType.HOTBAR) {
            drawEconomyGainToast(i, j);
            int mortarRadarShift = 0;
            if (player != null && player.ridingEntity instanceof MCH_EntitySeat && ((MCH_EntitySeat) player.ridingEntity).getParent().isMortarRadarEnabledRuntime()) {
                double scale = j / 500.0;
                mortarRadarShift = (int) (480.0 * scale / 2.0 + 480.0 * scale * 0.03);
            } else if (player != null && player.ridingEntity instanceof MCH_EntityAircraft && ((MCH_EntityAircraft) player.ridingEntity).isMortarRadarEnabledRuntime()) {
                double scale = j / 500.0;
                mortarRadarShift = (int) (480.0 * scale / 2.0 + 480.0 * scale * 0.03);
            }
            if (!hitList.isEmpty() && hitTotalDamage > 0) {
                int x = (int) (i * 0.6f) + mortarRadarShift;
                int y = (int) (j * 0.4f);
                GL11.glPushMatrix();
                float scale = hitTotalDamageScale;
                GL11.glScalef(scale, scale, scale);
                mc.fontRenderer.drawString(-(int) hitTotalDamage + "", (int) (x / scale), (int) (y / scale), 0xffffff, true);
                GL11.glPopMatrix();
            }
            int baseX = (int) (i * 0.6f) + mortarRadarShift;
            for (int idx = hitList.size() - 1, pos = 0; idx >= 0; idx--, pos++) {
                HitMessage message = hitList.get(idx);
                if (message.hitDisplay != null && (message.hitDamage > 0 || message.hitDamageType == 2)) {
                    float yOffset = 0.45f + pos * 0.025f;
                    int y = (int) (j * yOffset);
                    float alpha = Math.max(0.0f, 1.0f - pos * 0.15f);
                    int a = ((int) (alpha * 255)) << 24;
                    int color = a | 0x00FFFFFF;
                    String display = message.hitDamageType == 2 ? message.hitDisplay : String.format("%.1f %s", -message.hitDamage, message.hitDisplay);
                    mc.fontRenderer.drawString(
                        display,
                        baseX,
                        y,
                        color,
                        true
                    );
                }
            }
        }
    }

    @SubscribeEvent
    public void onGuiScreenDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (event == null || this.mc == null) {
            return;
        }
        boolean isVanillaInventory = event.gui instanceof GuiInventory;
        boolean isCreativeInventory = event.gui instanceof GuiContainerCreative;
        if (!isVanillaInventory && !isCreativeInventory) {
            return;
        }
        ScaledResolution scaled = new ScaledResolution(this.mc, this.mc.displayWidth, this.mc.displayHeight);
        drawEconomyBar(scaled.getScaledWidth(), scaled.getScaledHeight());
    }

    private void drawEconomyBar(int screenW, int screenH) {
        int x = 8;
        int y = screenH - 48;
        int w = 170;
        int h = 40;
        Gui.drawRect(x, y, x + w, y + h, 0x90101010);
        Gui.drawRect(x, y, x + w, y + 1, 0xB0908050);
        Gui.drawRect(x, y + h - 1, x + w, y + h, 0xB0606060);
        this.mc.fontRenderer.drawStringWithShadow("Economy", x + 6, y + 4, 0xF0F0F0);
        drawEconomyIconAndText(x + 6, y + 16, TEX_ICON_SL, new ItemStack(Items.gold_ingot), "SL " + MCH_EconomyClientData.getSL(), 0xFFE07A);
        drawEconomyIconAndText(x + 62, y + 16, TEX_ICON_GE, new ItemStack(Items.emerald), "GE " + MCH_EconomyClientData.getGE(), 0xFFD050);
        drawEconomyIconAndText(x + 118, y + 16, TEX_ICON_RP, new ItemStack(Items.enchanted_book), "RP " + MCH_EconomyClientData.getRP(), 0xA0D0FF);
    }

    private void drawEconomyIconAndText(int x, int y, ResourceLocation iconTex, ItemStack fallback, String text, int color) {
        drawEconomyInlineIcon(x, y, iconTex, fallback, 12);
        this.mc.fontRenderer.drawString(text, x + 14, y + 1, color);
    }

    private void drawEconomyGainToast(int screenW, int screenH) {
        if (!MCH_EconomyClientData.hasGainToast()) {
            return;
        }
        byte type = MCH_EconomyClientData.getGainToastType();
        int sl = MCH_EconomyClientData.getGainToastSL();
        int ge = MCH_EconomyClientData.getGainToastGE();
        int rp = MCH_EconomyClientData.getGainToastRP();
        if (sl <= 0 && ge <= 0 && rp <= 0) {
            return;
        }

        String title = type == 2 ? "摧毁载具" : "击杀目标";
        int gap = 18;
        int iconSize = 10;
        int xCursor = 0;
        int titleWidth = this.mc.fontRenderer.getStringWidth(title);
        xCursor += titleWidth;

        int slWidth = rewardPieceWidth("+" + sl, sl > 0, iconSize, gap);
        int geWidth = rewardPieceWidth("+" + ge, ge > 0, iconSize, gap);
        int rpWidth = rewardPieceWidth("+" + rp, rp > 0, iconSize, gap);
        int totalWidth = xCursor + slWidth + geWidth + rpWidth;

        int x = (screenW - totalWidth) / 2;
        int y = Math.max(18, (int) (screenH * 0.12f));

        int drawX = x;
        this.mc.fontRenderer.drawStringWithShadow(title, drawX, y, 0xFF5050);
        drawX += titleWidth;
        if (sl > 0) {
            drawX += gap;
            drawGainPiece(drawX, y, TEX_ICON_SL, new ItemStack(Items.gold_ingot), "+" + sl, iconSize);
            drawX += iconSize + 4 + this.mc.fontRenderer.getStringWidth("+" + sl);
        }
        if (ge > 0) {
            drawX += gap;
            drawGainPiece(drawX, y, TEX_ICON_GE, new ItemStack(Items.emerald), "+" + ge, iconSize);
            drawX += iconSize + 4 + this.mc.fontRenderer.getStringWidth("+" + ge);
        }
        if (rp > 0) {
            drawX += gap;
            drawGainPiece(drawX, y, TEX_ICON_RP, new ItemStack(Items.enchanted_book), "+" + rp, iconSize);
        }
    }

    private int rewardPieceWidth(String text, boolean draw, int iconSize, int gap) {
        if (!draw) {
            return 0;
        }
        return gap + iconSize + 4 + this.mc.fontRenderer.getStringWidth(text);
    }

    private void drawGainPiece(int x, int y, ResourceLocation iconTex, ItemStack fallback, String text, int iconSize) {
        drawEconomyInlineIcon(x, y, iconTex, fallback, iconSize);
        this.mc.fontRenderer.drawStringWithShadow(text, x + iconSize + 4, y + 1, 0xFFFFFF);
    }

    private void drawEconomyInlineIcon(int x, int y, ResourceLocation iconTex, ItemStack fallback, int iconSize) {
        if (bindTextureSafely(iconTex)) {
            GL11.glColor4f(1, 1, 1, 1);
            Tessellator t = Tessellator.instance;
            t.startDrawingQuads();
            t.addVertexWithUV((double) x, (double) (y + iconSize), 0.0D, 0.0D, 1.0D);
            t.addVertexWithUV((double) (x + iconSize), (double) (y + iconSize), 0.0D, 1.0D, 1.0D);
            t.addVertexWithUV((double) (x + iconSize), (double) y, 0.0D, 1.0D, 0.0D);
            t.addVertexWithUV((double) x, (double) y, 0.0D, 0.0D, 0.0D);
            t.draw();
            return;
        }
        RenderHelper.enableGUIStandardItemLighting();
        this.economyHudItemRenderer.renderItemAndEffectIntoGUI(this.mc.fontRenderer, this.mc.getTextureManager(), fallback, x - 3, y - 3);
        RenderHelper.disableStandardItemLighting();
    }

    private boolean bindTextureSafely(ResourceLocation texture) {
        if (texture == null) {
            return false;
        }
        try {
            this.mc.getResourceManager().getResource(texture);
            this.mc.getTextureManager().bindTexture(texture);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    public void onRenderTickPost(float partialTicks) {
        if (this.mc.thePlayer != null) {
            MCH_ClientTickHandlerBase.applyRotLimit(this.mc.thePlayer);
            MCH_ViewEntityDummy mCH_ViewEntityDummy = MCH_ViewEntityDummy.getInstance(this.mc.thePlayer.worldObj);
            if (mCH_ViewEntityDummy != null) {
                ((Entity) mCH_ViewEntityDummy).rotationPitch = this.mc.thePlayer.rotationPitch;
                ((Entity) mCH_ViewEntityDummy).rotationYaw = this.mc.thePlayer.rotationYaw;
                ((Entity) mCH_ViewEntityDummy).prevRotationPitch = this.mc.thePlayer.prevRotationPitch;
                ((Entity) mCH_ViewEntityDummy).prevRotationYaw = this.mc.thePlayer.prevRotationYaw;
            }
        }
//      if (this.mc.currentScreen == null || this.mc.currentScreen instanceof GuiChat || this.mc.currentScreen.getClass().toString().indexOf("GuiDriveableController") >= 0) {
//         for (MCH_Gui gui : this.guis) {
//            if (drawGui(gui, partialTicks))
//               break;
//         }
//         drawGui((MCH_Gui)this.gui_Common, partialTicks);
//         drawGui(this.gui_Wrench, partialTicks);
//         drawGui(this.gui_EMarker, partialTicks);
//         if (isDrawScoreboard)
//            MCH_GuiScoreboard.drawList(this.mc, this.mc.fontRenderer, false);
//         drawGui(this.gui_Title, partialTicks);
//      }
    }

    public boolean drawGui(MCH_Gui gui, float partialTicks) {
        if (gui.isDrawGui(super.mc.thePlayer)) {
            gui.drawScreen(0, 0, partialTicks);
            return true;
        } else {
            return false;
        }
    }

    public static class HitMessage {
        public String hitDisplay;
        public float hitDamage;
        public byte hitDamageType;
    }

    public static void startNukeFlashEffect(double x, double y, double z, float explosion, float radiusFactor, int minDurationTick, int maxDurationTick) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) {
            return;
        }
        float flashRadius = Math.max(1.0F, explosion * radiusFactor);
        double dist = mc.thePlayer.getDistance(x, y, z);
        if ((float) dist > flashRadius) {
            return;
        }

        float dist01 = MathHelper.clamp_float((float) dist / flashRadius, 0.0F, 1.0F);
        float pow01 = MathHelper.clamp_float(explosion / 40.0F, 0.0F, 1.0F);
        int minTick = Math.max(1, minDurationTick);
        int maxTick = Math.max(minTick, maxDurationTick);
        int duration = Math.round(minTick + (maxTick - minTick) * (0.65F * pow01 + 0.35F * (1.0F - dist01) * (1.0F - dist01)));
        duration = MathHelper.clamp_int(duration, minTick, maxTick);

        float peakAlpha = MathHelper.clamp_float(0.55F + 0.40F * (0.70F * pow01 + 0.30F * (1.0F - dist01)), 0.55F, 0.95F);
        int currentRemain = nukeFlashDurationTick - nukeFlashElapsedTick;
        if (nukeFlashDurationTick <= 0 || duration > currentRemain || peakAlpha > nukeFlashPeakAlpha) {
            nukeFlashDurationTick = duration;
            nukeFlashElapsedTick = 0;
            nukeFlashPeakAlpha = peakAlpha;
            nukeFlashPow01 = pow01;
        }
    }

    private static void tickNukeFlashEffect() {
        if (nukeFlashDurationTick <= 0) {
            return;
        }
        ++nukeFlashElapsedTick;
        if (nukeFlashElapsedTick >= nukeFlashDurationTick) {
            nukeFlashElapsedTick = -1;
            nukeFlashDurationTick = 0;
            nukeFlashPeakAlpha = 0.0F;
            nukeFlashPow01 = 0.0F;
        }
    }

    private static void renderNukeFlashOverlay(int screenW, int screenH, float partialTicks) {
        if (nukeFlashDurationTick <= 0 || nukeFlashElapsedTick < 0) {
            return;
        }
        float t = ((float) nukeFlashElapsedTick + partialTicks) / (float) nukeFlashDurationTick;
        t = MathHelper.clamp_float(t, 0.0F, 1.0F);
        float pulse = MathHelper.clamp_float((float) Math.sin(Math.PI * t), 0.0F, 1.0F);

        float baseAlpha = 0.22F;
        float alpha = baseAlpha + (nukeFlashPeakAlpha - baseAlpha) * pulse;

        float startR = 1.00F;
        float startG = 0.98F;
        float startB = 0.86F;
        float peakR = 1.00F;
        float peakG = 1.00F;
        float peakB = 0.94F;
        float colorBoost = 0.08F * nukeFlashPow01;

        float r = MathHelper.clamp_float(startR + (peakR - startR) * pulse + colorBoost, 0.0F, 1.0F);
        float g = MathHelper.clamp_float(startG + (peakG - startG) * pulse + colorBoost, 0.0F, 1.0F);
        float b = MathHelper.clamp_float(startB + (peakB - startB) * pulse, 0.0F, 1.0F);

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.setColorRGBA((int) (r * 255.0F), (int) (g * 255.0F), (int) (b * 255.0F), (int) (MathHelper.clamp_float(alpha, 0.0F, 1.0F) * 255.0F));
        tess.addVertex(0, screenH, 0);
        tess.addVertex(screenW, screenH, 0);
        tess.addVertex(screenW, 0, 0);
        tess.addVertex(0, 0, 0);
        tess.draw();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glPopMatrix();
    }
}
