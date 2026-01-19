package mcheli;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;

public class MCH_PlayerViewHandler {

    /**
     * 通过射击作用于玩家视野的后坐力
     */
    public static float playerRecoilPitch;
    public static float playerRecoilYaw;
    /**
     * 为使后坐力恢复正常，对后坐力施加的补偿量
     */
    public static float antiRecoilPitch;
    public static float antiRecoilYaw;

    public static Minecraft minecraft = Minecraft.getMinecraft();

    public static float recoilControl = 0.8f;

    public static void applyRecoil(float pitch, float yaw, float control) {
        playerRecoilPitch += pitch;
        playerRecoilYaw += yaw;
        recoilControl = control;
    }

    /**
     * 每帧更新视角抖动效果
     */
    public static void onUpdate() {

        if (minecraft.thePlayer == null) {
            return;
        }

        if (playerRecoilPitch > 0) {
            playerRecoilPitch *= recoilControl;
        }

        minecraft.thePlayer.rotationPitch -= playerRecoilPitch;
        minecraft.thePlayer.rotationYaw -= playerRecoilYaw;
        antiRecoilPitch += playerRecoilPitch;
        antiRecoilYaw += playerRecoilYaw;

        minecraft.thePlayer.rotationPitch += antiRecoilPitch * 0.2F;
        minecraft.thePlayer.rotationYaw += antiRecoilYaw * 0.2F;

        antiRecoilPitch *= 0.8F;
        antiRecoilYaw *= 0.8F;

        playerRecoilYaw *= 0.8F;
    }


    public static void updatePlayerViewDirection(Entity user, double targetX, double targetY, double targetZ, float smoothFactor) {
        double dx = targetX - user.posX;
        double dy = targetY - user.posY;
        double dz = targetZ - user.posZ;
        double distance = MathHelper.sqrt_double(dx * dx + dz * dz);
        float newPitch = (float) (Math.atan2(dy, distance) * 180.0D / Math.PI);
        float newYaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        float maxDelta = 10.0f;
        float pitchDiff = newPitch + user.rotationPitch;
        if (Math.abs(pitchDiff) > maxDelta) {
            newPitch = user.rotationPitch - Math.signum(pitchDiff) * maxDelta;
        } else {
            newPitch = -newPitch;
        }
        float yawDiff = lerpAngle(0, newYaw - user.rotationYaw, 1.0f);
        if (Math.abs(yawDiff) > maxDelta) {
            newYaw = user.rotationYaw + Math.signum(yawDiff) * maxDelta;
        }
        user.rotationPitch = lerpAngle(user.rotationPitch, newPitch, smoothFactor);
        user.rotationYaw = lerpAngle(user.rotationYaw, newYaw, smoothFactor);
    }

    private static float lerpAngle(float current, float target, float factor) {
        float diff = target - current;
        while (diff > 180.0F) diff -= 360.0F;
        while (diff < -180.0F) diff += 360.0F;
        return current + diff * factor;
    }
}
