package mcheli;

import mcheli.wrapper.W_Session;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.world.World;

public class MCH_ViewEntityDummy extends EntityPlayerSP {

    private static MCH_ViewEntityDummy instance = null;
    private float zoom;


    private MCH_ViewEntityDummy(World world) {
        super(Minecraft.getMinecraft(), world, W_Session.newSession(), 1);
        super.hurtTime = 0;
        super.maxHurtTime = 1;
        this.setSize(0.0F, 0.0F);
        this.noClip = true;
    }


    public static MCH_ViewEntityDummy getInstance(World w) {
        if (w == null || !w.isRemote) {
            return null;
        }

        MCH_ViewEntityDummy current = instance;
        if (current != null && current.worldObj == w && !current.isDead) {
            return current;
        }

        // Never pass the mutable static reference to WorldClient: an integrated-server
        // unload callback must not be able to turn the spawn argument into null.
        MCH_ViewEntityDummy created = new MCH_ViewEntityDummy(w);
        if (Minecraft.getMinecraft().thePlayer != null) {
            created.movementInput = Minecraft.getMinecraft().thePlayer.movementInput;
        }
        created.setPosition(0.0D, -4.0D, 0.0D);
        instance = created;
        w.spawnEntityInWorld(created);
        return created;
    }

    /** Clear only the camera dummy belonging to the unloading client world. */
    public static void onUnloadWorld(World world) {
        if (world == null || !world.isRemote) {
            return;
        }
        MCH_ViewEntityDummy current = instance;
        if (current != null && current.worldObj == world) {
            instance = null;
            current.setDead();
        }
    }

    /** Retained for callers compiled against the original no-argument API. */
    public static void onUnloadWorld() {
        MCH_ViewEntityDummy current = instance;
        if (current != null) {
            onUnloadWorld(current.worldObj);
        }
    }

    public static void setCameraPosition(double x, double y, double z) {
        if (instance != null) {
            instance.prevPosX = x;
            instance.prevPosY = y;
            instance.prevPosZ = z;
            instance.lastTickPosX = x;
            instance.lastTickPosY = y;
            instance.lastTickPosZ = z;
            instance.posX = x;
            instance.posY = y;
            instance.posZ = z;
        }
    }

    @Override
    public void onUpdate() {
    }

    public void update(MCH_Camera camera) {
        if (camera != null) {
            this.zoom = camera.getCameraZoom();
            super.prevRotationYaw = super.rotationYaw;
            super.prevRotationPitch = super.rotationPitch;
            super.rotationYaw = camera.rotationYaw;
            super.rotationPitch = camera.rotationPitch;
            super.prevPosX = camera.posX;
            super.prevPosY = camera.posY;
            super.prevPosZ = camera.posZ;
            super.posX = camera.posX;
            super.posY = camera.posY;
            super.posZ = camera.posZ;
        }
    }

    @Override
    public boolean canRiderInteract() {
        return false;
    }

    @Override
    public float getFOVMultiplier() {
        return super.getFOVMultiplier() * (1.0F / this.zoom);
    }

}
