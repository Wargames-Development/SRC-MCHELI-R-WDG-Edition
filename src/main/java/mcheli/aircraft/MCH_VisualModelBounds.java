package mcheli.aircraft;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.*;
import mcheli.helicopter.MCH_HeliInfo;
import mcheli.plane.MCP_PlaneInfo;
import mcheli.vehicle.MCH_VehicleInfo;
import mcheli.wrapper.modelloader.W_ModelCustom;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.model.IModelCustom;

/** Conservative physical-space envelope of native model transforms; missing bounds fail closed. */
@SideOnly(Side.CLIENT)
public final class MCH_VisualModelBounds {
    private int remaining=8192;
    private final double body;
    private MCH_VisualModelBounds(double body) { this.body=body; }
    public static double radius(MCH_AircraftInfo info) {
        if(info==null) return Double.NaN;
        double body=model(info.model);MCH_VisualModelBounds bounds=new MCH_VisualModelBounds(body);double radius=body;
        for(List parts:Arrays.asList(info.hatchList,info.cameraList,info.canopyList,info.landingGear,info.partThrottle,info.partRotPart,
                info.partTrackRoller,info.partCrawlerTrack,info.partWheel,info.partSteeringWheel,info.lightHatchList)) radius=Math.max(radius,bounds.parts(parts,0));
        double turret=2*length(info.turretPosition);
        for(List parts:Arrays.asList(info.partWeapon,info.partWeaponBay,info.partTurretWeaponBay,info.partTurretRotPart))
            radius=Math.max(radius,bounds.parts(parts,0)+turret);
        if(info instanceof MCH_HeliInfo) radius=Math.max(radius,bounds.parts(((MCH_HeliInfo)info).rotorList,0));
        if(info instanceof MCP_PlaneInfo) {
            MCP_PlaneInfo plane=(MCP_PlaneInfo)info;
            for(List parts:Arrays.asList(plane.nozzles,plane.wingList,plane.rotorList)) radius=Math.max(radius,bounds.parts(parts,0));
        }
        if(info instanceof MCH_VehicleInfo) radius=Math.max(radius,bounds.parts(((MCH_VehicleInfo)info).partList,0));
        return radius+1;
    }
    private double parts(List parts,int depth) {
        if(parts==null) return 0;
        if(depth>16) return Double.NaN;
        double result=0;
        for(Object value:parts) {
            if(--remaining<0 || !(value instanceof MCH_AircraftInfo.DrawnPart)) return Double.NaN;
            MCH_AircraftInfo.DrawnPart part=(MCH_AircraftInfo.DrawnPart)value;
            // Named groups without a separate part model inherit the body's complete geometry bounds.
            double radius=part.model==null?body:model(part.model),pivot=2*length(part.pos);
            if(part instanceof MCH_AircraftInfo.PartWheel) pivot+=2*length(((MCH_AircraftInfo.PartWheel)part).pos2);
            if(part instanceof MCH_AircraftInfo.Throttle) radius+=length(((MCH_AircraftInfo.Throttle)part).slide);
            if(part instanceof MCH_AircraftInfo.LandingGear) radius+=2*length(((MCH_AircraftInfo.LandingGear)part).slide);
            if(part instanceof MCH_AircraftInfo.PartWeapon) {
                MCH_AircraftInfo.PartWeapon weapon=(MCH_AircraftInfo.PartWeapon)part;
                radius+=Math.abs(weapon.recoilBuf);radius=Math.max(radius,parts(weapon.child,depth+1));
            }
            if(part instanceof MCH_AircraftInfo.PartWeaponChild) radius+=Math.abs(((MCH_AircraftInfo.PartWeaponChild)part).recoilBuf);
            if(part instanceof MCH_VehicleInfo.VPart) {
                MCH_VehicleInfo.VPart vehicle=(MCH_VehicleInfo.VPart)part;
                radius+=Math.abs(vehicle.recoilBuf);radius=Math.max(radius,parts(vehicle.child,depth+1));
            }
            if(part instanceof MCP_PlaneInfo.Rotor) radius=Math.max(radius,parts(((MCP_PlaneInfo.Rotor)part).blades,depth+1));
            if(part instanceof MCP_PlaneInfo.Wing) radius=Math.max(radius,parts(((MCP_PlaneInfo.Wing)part).pylonList,depth+1));
            if(part instanceof MCH_AircraftInfo.CrawlerTrack) {
                MCH_AircraftInfo.CrawlerTrack track=(MCH_AircraftInfo.CrawlerTrack)part;
                if(track.lp==null || track.lp.size()>8192) return Double.NaN;
                double travel=0;
                for(Object point:track.lp) {
                    MCH_AircraftInfo.CrawlerTrackPrm p=(MCH_AircraftInfo.CrawlerTrackPrm)point;
                    travel=Math.max(travel,Math.hypot(p.x,p.y));
                }
                radius+=travel;
            }
            result=Math.max(result,radius+pivot);
        }
        return result;
    }
    private static double length(Vec3 vector) { return vector==null?0:Math.sqrt(vector.xCoord*vector.xCoord+vector.yCoord*vector.yCoord+vector.zCoord*vector.zCoord); }
    private static double model(IModelCustom model) {
        if(!(model instanceof W_ModelCustom)) return Double.NaN;
        W_ModelCustom box=(W_ModelCustom)model;
        if(box.minX>box.maxX || box.minY>box.maxY || box.minZ>box.maxZ) return Double.NaN;
        double x=Math.max(Math.abs(box.minX),Math.abs(box.maxX)),y=Math.max(Math.abs(box.minY),Math.abs(box.maxY)),z=Math.max(Math.abs(box.minZ),Math.abs(box.maxZ));
        return Math.sqrt(x*x+y*y+z*z);
    }
}
