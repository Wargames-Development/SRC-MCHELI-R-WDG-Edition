package mcheli;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.plane.MCP_EntityPlane;
import mcheli.tank.MCH_EntityTank;
import mcheli.vehicle.MCH_EntityVehicle;
import mcheli.vector.Quaternion;
import mcheli.wrapper.W_Reflection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

@SideOnly(Side.CLIENT)
public class MCH_3rdCamera extends EntityLivingBase {

    private static final float VANILLA_EYE_OFFSET = 1.62F;
    private static final double ROTATION_HALF_LIFE = 0.025D;
    private static final double HARD_SNAP_DISTANCE_SQ = 64.0D * 64.0D;
    public final MCH_EntityAircraft entity;

    private final Quaternion orientation = new Quaternion();
    private final Quaternion target = new Quaternion();
    private final Quaternion vehicleRotation = new Quaternion();
    private final double pivotHeight;
    private double pivotX;
    private double pivotY;
    private double pivotZ;
    private long lastFrameNanos;
    private boolean framePending;
    private boolean frontView;
    private EntityRenderer frameRenderer;
    private float previousDistance;
    private float currentDistance;
    private float originalRoll;

    public MCH_3rdCamera(World world, MCH_EntityAircraft aircraft, EntityPlayer player) {
        super(world);
        entity = aircraft;
        pivotHeight = getPivotHeight(aircraft);
        setSize(0.0F, 0.0F);
        // With this offset, vanilla places the eye at the entity origin.
        yOffset = VANILLA_EYE_OFFSET;
        updatePivot(1.0F);
        publishPosition(pivotX, pivotY + VANILLA_EYE_OFFSET, pivotZ);
        prevRotationYaw = rotationYaw = player.rotationYaw;
        prevRotationPitch = rotationPitch = cameraPitch(player);
        setOrientation(orientation, rotationYaw, rotationPitch, 0.0F);
    }

    public void beginRenderFrame() {
        framePending = true;
    }

    public void endRenderFrame() {
        framePending = false;
        if (frameRenderer != null) {
            W_Reflection.setRenderThirdPersonDistances(frameRenderer, previousDistance, currentDistance);
            W_Reflection.setCameraRoll(originalRoll);
            frameRenderer = null;
        }
    }

    @Override
    public MovingObjectPosition rayTrace(double reach, float partialTicks) {
        // World rendering picks after vanilla mouse-look and before it samples the view entity.
        if (framePending) {
            framePending = false;
            updateRenderPose(Minecraft.getMinecraft(), partialTicks);
        }
        return super.rayTrace(reach, partialTicks);
    }

    @Override
    public Vec3 getPosition(float partialTicks) {
        // Vanilla's block and entity picking must originate at the vehicle pivot.
        return Vec3.createVectorHelper(pivotX, pivotY, pivotZ);
    }

    @Override
    public Vec3 getLookVec() {
        return forward(orientation, frontView);
    }

    private void updateRenderPose(Minecraft mc, float partialTicks) {
        updatePivot(partialTicks);
        frontView = mc.gameSettings.thirdPersonView == 2;
        originalRoll = W_Reflection.getPrevCameraRoll();
        setOrientation(target, mc.thePlayer.rotationYaw, cameraPitch(mc.thePlayer), originalRoll);

        long now = System.nanoTime();
        double seconds = lastFrameNanos == 0L ? 0.0D : (now - lastFrameNanos) * 1.0e-9D;
        float blend = lastFrameNanos == 0L || seconds > 0.25D
            ? 1.0F : (float) (1.0D - Math.pow(0.5D, seconds / ROTATION_HALF_LIFE));
        interpolate(orientation, target, blend);
        lastFrameNanos = now;

        Vec3 look = forward(orientation, false);
        float yaw = (float) Math.toDegrees(Math.atan2(-look.xCoord, look.zCoord));
        rotationYaw += MathHelper.wrapAngleTo180_float(yaw - rotationYaw);
        prevRotationYaw = rotationYaw;
        prevRotationPitch = rotationPitch = (float) Math.toDegrees(Math.asin(
            Math.max(-1.0D, Math.min(1.0D, -look.yCoord))));
        float roll = (float) Math.toDegrees(Math.atan2(
            2.0D * (orientation.x * orientation.y + orientation.z * orientation.w),
            1.0D - 2.0D * (orientation.x * orientation.x + orientation.z * orientation.z)));

        EntityRenderer renderer = mc.entityRenderer;
        previousDistance = W_Reflection.getPreviousThirdPersonDistance(renderer);
        currentDistance = W_Reflection.getThirdPersonDistance();
        double distance = previousDistance + (currentDistance - previousDistance) * partialTicks;
        Vec3 eye = clipEye(forward(orientation, !frontView), distance);
        publishPosition(eye.xCoord, eye.yCoord, eye.zCoord);

        // DH and waypoint renderers get the same eye through the usual vanilla render origin.
        // The chase distance is already in that origin, so disable vanilla's second setback.
        frameRenderer = renderer;
        W_Reflection.setRenderThirdPersonDistances(renderer, 0.0F, 0.0F);
        W_Reflection.setCameraRoll(roll);
    }

    private void updatePivot(float partialTicks) {
        setOrientation(vehicleRotation, entity.prevRotationYaw, entity.prevRotationPitch, entity.prevRotationRoll);
        setOrientation(target, entity.getRotYaw(), entity.getRotPitch(), entity.getRotRoll());
        interpolate(vehicleRotation, target, partialTicks);

        double dx = entity.posX - entity.lastTickPosX;
        double dy = entity.posY - entity.lastTickPosY;
        double dz = entity.posZ - entity.lastTickPosZ;
        float positionFraction = dx * dx + dy * dy + dz * dz > HARD_SNAP_DISTANCE_SQ ? 1.0F : partialTicks;
        double x = vehicleRotation.x, y = vehicleRotation.y, z = vehicleRotation.z, w = vehicleRotation.w;
        pivotX = entity.lastTickPosX + dx * positionFraction + 2.0D * (x * y - z * w) * pivotHeight;
        pivotY = entity.lastTickPosY + dy * positionFraction + (1.0D - 2.0D * (x * x + z * z)) * pivotHeight;
        pivotZ = entity.lastTickPosZ + dz * positionFraction + 2.0D * (y * z + x * w) * pivotHeight;
    }

    private Vec3 clipEye(Vec3 backward, double distance) {
        double eyeY = pivotY + VANILLA_EYE_OFFSET;
        double clippedDistance = distance;
        for (int corner = 0; corner < 8; ++corner) {
            double x = pivotX + ((corner & 1) == 0 ? -0.1D : 0.1D);
            double y = eyeY + ((corner & 2) == 0 ? -0.1D : 0.1D);
            double z = pivotZ + ((corner & 4) == 0 ? -0.1D : 0.1D);
            MovingObjectPosition hit = worldObj.rayTraceBlocks(Vec3.createVectorHelper(x, y, z),
                Vec3.createVectorHelper(x + backward.xCoord * distance, y + backward.yCoord * distance,
                    z + backward.zCoord * distance));
            if (hit != null) {
                double hitDistance = (hit.hitVec.xCoord - x) * backward.xCoord
                    + (hit.hitVec.yCoord - y) * backward.yCoord + (hit.hitVec.zCoord - z) * backward.zCoord;
                clippedDistance = Math.min(clippedDistance, Math.max(0.0D, hitDistance - 0.05D));
            }
        }
        return Vec3.createVectorHelper(pivotX + backward.xCoord * clippedDistance,
            eyeY + backward.yCoord * clippedDistance, pivotZ + backward.zCoord * clippedDistance);
    }

    private void publishPosition(double x, double y, double z) {
        prevPosX = lastTickPosX = posX = x;
        prevPosY = lastTickPosY = posY = y;
        prevPosZ = lastTickPosZ = posZ = z;
        boundingBox.setBounds(pivotX, pivotY, pivotZ, pivotX, pivotY, pivotZ);
    }

    private static void setOrientation(Quaternion result, float yaw, float pitch, float roll) {
        double halfYaw = Math.toRadians(-yaw) * 0.5D;
        double halfPitch = Math.toRadians(pitch) * 0.5D;
        double halfRoll = Math.toRadians(roll) * 0.5D;
        double cy = Math.cos(halfYaw), sy = Math.sin(halfYaw);
        double cp = Math.cos(halfPitch), sp = Math.sin(halfPitch);
        double cr = Math.cos(halfRoll), sr = Math.sin(halfRoll);
        result.set((float) (cy * sp * cr + sy * cp * sr), (float) (sy * cp * cr - cy * sp * sr),
            (float) (cy * cp * sr - sy * sp * cr), (float) (cy * cp * cr + sy * sp * sr));
    }

    private static void interpolate(Quaternion from, Quaternion to, float fraction) {
        // q and -q encode the same orientation. Keep the interpolation on the shorter arc.
        float signedFraction = Quaternion.dot(from, to) < 0.0F ? -fraction : fraction;
        float remaining = 1.0F - fraction;
        from.set(from.x * remaining + to.x * signedFraction, from.y * remaining + to.y * signedFraction,
            from.z * remaining + to.z * signedFraction, from.w * remaining + to.w * signedFraction);
        Quaternion.normalise(from, from);
    }

    private static Vec3 forward(Quaternion rotation, boolean front) {
        double x = rotation.x, y = rotation.y, z = rotation.z, w = rotation.w;
        double sign = front ? -1.0D : 1.0D;
        return Vec3.createVectorHelper(sign * 2.0D * (x * z + y * w), sign * 2.0D * (y * z - x * w),
            sign * (1.0D - 2.0D * (x * x + y * y)));
    }

    private static float cameraPitch(EntityPlayer player) {
        return MathHelper.clamp_float(player.rotationPitch + 2.0F, -89.9F, 89.9F);
    }

    private static double getPivotHeight(MCH_EntityAircraft entity) {
        if (entity instanceof MCP_EntityPlane) {
            return 10.0D;
        } else if (entity instanceof MCH_EntityHeli) {
            return 5.0D;
        } else if (entity instanceof MCH_EntityTank) {
            return 4.0D;
        } else if (entity instanceof MCH_EntityVehicle) {
            return 2.0D;
        }
        return 0.0D;
    }

    @Override
    public ItemStack getHeldItem() {
        return null;
    }

    @Override
    public ItemStack getEquipmentInSlot(int p_71124_1_) {
        return null;
    }

    @Override
    public void setCurrentItemOrArmor(int slotIn, ItemStack itemStackIn) {
    }

    @Override
    public ItemStack[] getLastActiveItems() {
        return null;
    }
}
