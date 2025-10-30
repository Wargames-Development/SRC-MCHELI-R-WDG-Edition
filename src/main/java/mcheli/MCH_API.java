package mcheli;

import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.aircraft.MCH_SeatRackInfo;
import net.minecraft.entity.player.EntityPlayer;

public class MCH_API {
    public static boolean mountPilot(Object p, Object ac) {
        EntityPlayer player = (EntityPlayer) p;
        MCH_EntityAircraft aircraft = (MCH_EntityAircraft) ac;
        if (player == null || aircraft == null) {
            return false;
        }
        if (aircraft.isDestroyed() || aircraft.getAcInfo() == null) {
            return false;
        }
        if (!aircraft.checkTeam(player)) {
            return false;
        }
        if (!aircraft.getAcInfo().canRide || aircraft.isUAV()) {
            return false;
        }
        if (aircraft.getRiddenByEntity() != null || player.ridingEntity instanceof MCH_EntitySeat) {
            return false;
        }
        if (!aircraft.canRideSeatOrRack(0, player)) {
            return false;
        }
//        if (aircraft.getAcInfo().haveCanopy() && aircraft.isCanopyClose()) {
//            aircraft.openCanopy();
//            return false;
//        }
        if (aircraft.getModeSwitchCooldown() > 0) {
            return false;
        }
//        aircraft.closeCanopy();
        aircraft.riddenByEntity = null;
        aircraft.lastRiddenByEntity = null;
        aircraft.initRadar();

        if (!aircraft.worldObj.isRemote) {
            player.mountEntity(aircraft);
            if (!aircraft.keepOnRideRotation) {
                aircraft.mountMobToSeats();
            }
        } else {
            aircraft.updateClientSettings(0);
        }

        aircraft.setCameraId(0);
        aircraft.initPilotWeapon();
        if (aircraft.lowPassPartialTicks != null) {
            aircraft.lowPassPartialTicks.clear();
        }
        aircraft.onInteractFirst(player);
        return true;
    }

    public static boolean mountFirstEmptySeat(Object p, Object ac) {
        EntityPlayer player = (EntityPlayer) p;
        MCH_EntityAircraft aircraft = (MCH_EntityAircraft) ac;
        if (player == null || aircraft == null) {
            return false;
        }
        MCH_EntitySeat[] seats = aircraft.getSeats();
        if (seats == null || seats.length == 0) {
            return false;
        }
        int seatId = 1;
        for (MCH_EntitySeat seat : seats) {
            if (seat != null) {
                if (seat.riddenByEntity == null
                    && !aircraft.isMountedEntity(player)
                    && aircraft.canRideSeatOrRack(seatId, player)) {
                    if (!(aircraft.getSeatInfo(seatId) instanceof MCH_SeatRackInfo)) {
                        if (!aircraft.worldObj.isRemote) {
                            player.mountEntity(seat);
                        } else {
                            aircraft.updateClientSettings(seatId);
                        }
                        return true;
                    } else {
                        break;
                    }
                }
                seatId++;
            }
        }
        return false;
    }
}
