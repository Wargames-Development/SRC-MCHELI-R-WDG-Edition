package mcheli.plane;

import com.google.common.io.ByteArrayDataInput;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.uav.MCH_EntityUavStation;
import net.minecraft.entity.player.EntityPlayer;

public class MCP_PlanePacketHandler {

    public static void onPacket_PlayerControl(EntityPlayer player, ByteArrayDataInput data) {
        if (!player.worldObj.isRemote) {
            MCP_PlanePacketPlayerControl pc = new MCP_PlanePacketPlayerControl();
            pc.readData(data);
            MCP_EntityPlane plane = null;
            if (player.ridingEntity instanceof MCP_EntityPlane) {
                plane = (MCP_EntityPlane) player.ridingEntity;
            } else if (player.ridingEntity instanceof MCH_EntitySeat) {
                if (((MCH_EntitySeat) player.ridingEntity).getParent() instanceof MCP_EntityPlane) {
                    plane = (MCP_EntityPlane) ((MCH_EntitySeat) player.ridingEntity).getParent();
                }
            } else if (player.ridingEntity instanceof MCH_EntityUavStation) {
                MCH_EntityUavStation ac = (MCH_EntityUavStation) player.ridingEntity;
                if (ac.getControlAircract() instanceof MCP_EntityPlane) {
                    plane = (MCP_EntityPlane) ac.getControlAircract();
                }
            }

            if (plane != null) {
                if (pc.isUnmount == 1) {
                    plane.unmountEntity();
                } else if (pc.isUnmount == 2) {
                    plane.unmountCrew();
                } else if (pc.ejectSeat) {
                    plane.ejectSeat(player);
                } else {
                    if (pc.switchVtol == 0) {
                        plane.switchVtolMode(false);
                    }

                    if (pc.switchVtol == 1) {
                        plane.switchVtolMode(true);
                    }

                    if (pc.switchMode == 0) {
                        plane.switchGunnerMode(false);
                    }

                    if (pc.switchMode == 1) {
                        plane.switchGunnerMode(true);
                    }

                    if (pc.switchMode == 2) {
                        plane.switchHoveringMode(false);
                    }

                    if (pc.switchMode == 3) {
                        plane.switchHoveringMode(true);
                    }

                    if (pc.switchSearchLight) {
                        plane.setSearchLight(!plane.isSearchLightON());
                    }

                    if (pc.switchCameraMode > 0) {
                        plane.switchCameraMode(player, pc.switchCameraMode - 1);
                    }

                    if (pc.switchWeapon >= 0) {
                        plane.switchWeapon(player, pc.switchWeapon);
                    }

                    if (plane.isPilot(player)) {
                        plane.throttleUp = pc.throttleUp;
                        plane.throttleDown = pc.throttleDown;
                        plane.moveLeft = pc.moveLeft;
                        plane.moveRight = pc.moveRight;
                    }

                    if (pc.useFlareType > 0) {
                        plane.useFlare(pc.useFlareType);
                    }

                    if (pc.useChaff) {
                        plane.useChaff();
                    }

                    if (pc.useMaintenance) {
                        plane.useMaintenance();
                    }

                    if (pc.useAPS) {
                        plane.useAPS(player);
                    }

                    if (pc.useECMJammer) {
                        plane.useECMJammer(player);
                    }

                    if (pc.openGui) {
                        plane.openGui(player);
                    }

                    if (pc.switchHatch > 0) {
                        if (plane.getAcInfo().haveHatch()) {
                            plane.foldHatch(pc.switchHatch == 2);
                        } else {
                            plane.foldWing(pc.switchHatch == 2);
                        }
                    }

                    if (pc.switchFreeLook > 0) {
                        plane.switchFreeLookMode(pc.switchFreeLook == 1);
                    }

                    if (pc.switchGear == 1) {
                        plane.foldLandingGear();
                    }

                    if (pc.switchGear == 2) {
                        plane.unfoldLandingGear();
                    }

                    if (pc.putDownRack == 1) {
                        plane.mountEntityToRack();
                    }

                    if (pc.putDownRack == 2) {
                        plane.unmountEntityFromRack();
                    }

                    if (pc.putDownRack == 3) {
                        plane.rideRack();
                    }

                    if (pc.isUnmount == 3) {
                        plane.unmountAircraft();
                    }
                }

            }
        }
    }
}
