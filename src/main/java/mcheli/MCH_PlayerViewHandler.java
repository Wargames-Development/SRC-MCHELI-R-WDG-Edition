package mcheli;

import net.minecraft.client.Minecraft;

import java.util.Random;

public class MCH_PlayerViewHandler {

    /**
     * Recoil applied to the player's view when firing
     */
    public static float playerRecoilPitch;
    public static float playerRecoilYaw;
    /**
     * Compensation amount applied to counteract recoil for smoother recovery
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
     * Updates the visual recoil (view shake) effect each frame
     */
    public static void onUpdate() {

        if(minecraft.thePlayer == null) {
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
}
