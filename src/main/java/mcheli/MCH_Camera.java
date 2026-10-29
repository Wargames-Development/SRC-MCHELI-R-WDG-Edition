package mcheli;

import java.util.Arrays;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_Lib;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_AircraftInfo;
import net.minecraft.entity.Entity;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

public class MCH_Camera {

    public static final int MODE_NORMAL = 0;
    public static final int MODE_NIGHTVISION = 1;
    public static final int MODE_THERMALVISION = 2;
    public static int currentCameraMode = 0;
    private final World worldObj;
    private final Entity owner;
    public double posX;
    public double posY;
    public double posZ;
    public float rotationYaw;
    public float rotationPitch;
    public float prevRotationYaw;
    public float prevRotationPitch;
    public float partRotationYaw;
    public float partRotationPitch;
    public float prevPartRotationYaw;
    public float prevPartRotationPitch;
    private float zoom;
    private int[] mode;
    private boolean[] canUseShader;
    private int[] lastMode;
    private int lastZoomDir;


    public MCH_Camera(World w, Entity p) {
        this.worldObj = w;
        this.owner = p;
        this.mode = new int[]{0, 0};
        this.zoom = 1.0F;
        this.lastMode = new int[this.mode.length];
        this.lastZoomDir = 0;
        this.canUseShader = new boolean[this.mode.length];
    }

    public MCH_Camera(World w, Entity p, double x, double y, double z) {
        this(w, p);
        this.setPosition(x, y, z);
        this.setCameraZoom(1.0F);
    }

    public int getUserMax() {
        if (this.owner instanceof MCH_EntityAircraft) {
            MCH_AircraftInfo info = ((MCH_EntityAircraft)this.owner).getAcInfo();
            if (info != null && info.getNumSeatAndRack() > this.mode.length) {
                int users = info.getNumSeatAndRack();
                this.mode = Arrays.copyOf(this.mode, users);
                this.lastMode = Arrays.copyOf(this.lastMode, users);
                this.canUseShader = Arrays.copyOf(this.canUseShader, users);
            }
        }
        return this.mode.length;
    }

    public void initCamera(int uid, Entity viewer) {
        this.setCameraZoom(1.0F);
        this.setMode(uid, 0);
        this.updateViewer(uid, viewer);
    }

    /*
    public void setMode(int uid, int m) {
        if (this.isValidUid(uid)) {
            if (this.worldObj.isRemote) {
                this.mode[uid] = m < 0 ? 0 : m % this.getModeNum(uid);
            }
        }
    }
     */
    public void setMode(int uid, int m) {
        if (this.isValidUid(uid)) {
            int next = m < 0 ? 0 : m % this.getModeNum(uid);
            // Keep wire mode IDs stable while cycling vehicle optics directly to thermal.
            if (this.owner instanceof MCH_EntityAircraft && next == MODE_NIGHTVISION)
                next = this.getModeNum(uid) == 3 ? MODE_THERMALVISION : MODE_NORMAL;
            this.mode[uid] = next;
        }
    }

    public void setShaderSupport(int uid, Boolean b) {
        if (this.isValidUid(uid)) {
            this.canUseShader[uid] = b != null && b;
            int modeNum = this.getModeNum(uid);
            if (this.mode[uid] >= modeNum) {
                this.mode[uid] = 0;
            }
        }

    }

    public boolean isValidUid(int uid) {
        // Extra crew slots need an equipped sensor; ordinary passengers remain excluded.
        return uid >= 0 && uid < this.getUserMax() && (uid < 2 || thermalAllowed(uid));
    }

    public int getModeNum(int uid) {
        int normalModes = this.owner instanceof MCH_EntityAircraft ? 1 : 2;
        return !this.isValidUid(uid) ? normalModes : (this.canUseShader[uid] && thermalAllowed(uid) ? 3 : normalModes);
    }

    private boolean thermalAllowed(int uid) {
        if (!(owner instanceof MCH_EntityAircraft)) return true;
        MCH_EntityAircraft aircraft = (MCH_EntityAircraft)owner;
        MCH_AircraftInfo info = aircraft.getAcInfo();
        return info != null && info.isThermalEnabled(aircraft.getCameraPosInfo(uid), uid);
    }

    public int getMode(int uid) {
        if (!this.isValidUid(uid)) return MODE_NORMAL;
        if (this.owner instanceof MCH_EntityAircraft && this.mode[uid] == MODE_NIGHTVISION)
            this.mode[uid] = MODE_NORMAL;
        if (this.mode[uid] >= this.getModeNum(uid)) this.mode[uid] = MODE_NORMAL;
        return this.mode[uid];
    }

    public String getModeName(int uid) {
        return this.getMode(uid) == 1 ? "NIGHT VISION" : (this.getMode(uid) == 2 ? "THERMAL VISION" : "");
    }

    public void updateViewer(int uid, Entity viewer) {
        // Aircraft optics must neither grant night vision nor remove wearable NVG effects.
        if (this.owner instanceof MCH_EntityAircraft) return;
        if (this.isValidUid(uid) && viewer != null) {
            if (W_Lib.isEntityLivingBase(viewer) && !viewer.isDead) {
                PotionEffect pe;
                if ((this.getMode(uid) == 0 || this.getMode(uid) == 2) && this.lastMode[uid] != 0) {
                    pe = W_Entity.getActivePotionEffect(viewer, Potion.nightVision);
                    if (pe != null && pe.getDuration() > 0 && pe.getDuration() < 500) {
                        if (viewer.worldObj.isRemote) {
                            W_Entity.removePotionEffectClient(viewer, Potion.nightVision.id);
                        } else {
                            W_Entity.removePotionEffect(viewer, Potion.nightVision.id);
                        }
                    }
                }

                if (this.getMode(uid) == 1) {
                    pe = W_Entity.getActivePotionEffect(viewer, Potion.nightVision);
                    if ((pe == null || pe.getDuration() < 500) && !viewer.worldObj.isRemote) {
                        W_Entity.addPotionEffect(viewer, new PotionEffect(Potion.nightVision.id, 250, 0, true));
                    }
                }
            }

            this.lastMode[uid] = this.getMode(uid);
        }
    }

    public void setPosition(double x, double y, double z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
    }

    public float getCameraZoom() {
        return this.zoom;
    }

    public void setCameraZoom(float z) {
        float prevZoom = this.zoom;
        this.zoom = Math.max(z, 1.0F);
        if (this.zoom > prevZoom) {
            this.lastZoomDir = 1;
        } else if (this.zoom < prevZoom) {
            this.lastZoomDir = -1;
        } else {
            this.lastZoomDir = 0;
        }

    }
}
