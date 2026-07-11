package mcheli;

import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.aircraft.MCH_ItemAircraft;
import mcheli.aircraft.MCH_SeatRackInfo;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.wrapper.W_Item;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class MCH_API {

    public static boolean spawnAircraftAndMountPlayer(Object world, Object p, String itemName, double x, double y, double z, float rotationYaw, boolean detectIfExists) {
        Object ac;
        if((ac = spawnAircraft(world, itemName, x, y, z, rotationYaw, detectIfExists)) != null) {
            return mountPilot(p, ac);
        }
        return false;
    }

    public static String getAcName(Object p) {
        if(p instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) p;
            MCH_EntityAircraft ac = null;
            if (player.ridingEntity instanceof MCH_EntityAircraft) {
                ac = (MCH_EntityAircraft) player.ridingEntity;
            } else if (player.ridingEntity instanceof MCH_EntitySeat) {
                ac = ((MCH_EntitySeat) player.ridingEntity).getParent();
            } else if (player.ridingEntity instanceof MCH_EntityUavStation) {
                ac = ((MCH_EntityUavStation) player.ridingEntity).getControlAircract();
            }
            if(ac != null && ac.getAcInfo() != null) {
                Object o = ac.getAcInfo().displayNameLang.get("en_US");
                return o == null ? "AIR" : o.toString();
            }
        }
        return "AIR";
    }

    public static Object spawnAircraft(Object world, String itemName, double x, double y, double z, float rotationYaw, boolean detectIfExists) {
        Item item = W_Item.getItemByName(itemName);
        if(item instanceof MCH_ItemAircraft) {
            boolean result = true;
            if(detectIfExists) {
                result = false;
            }
            if(result) {
                ItemStack itemStack = new ItemStack(item);
                MCH_ItemAircraft itemAircraft = (MCH_ItemAircraft) item;
                MCH_EntityAircraft ac = itemAircraft.createAircraft((World) world, (float) x + 0.5F, (float) y + 1.0F, (float) z + 0.5F, itemStack);
                if (ac == null) {
                    return null;
                } else {
                    ac.initRotationYaw((float) (((MathHelper.floor_double((double) (rotationYaw * 4.0F / 360.0F) + 0.5D) & 3) - 1) * 90));
                    if (!((World) world).isRemote) {
                        ac.getAcDataFromItem(itemStack);
                        ((World) world).spawnEntityInWorld(ac);
                    }
                    return ac;
                }
            }
        }
        return null;
    }

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
