package mcheli.render;

import java.lang.reflect.Method;
import mcheli.MCH_EntityInfo;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_VisualModelBounds;
import net.minecraft.client.Minecraft;

/** Optional visual-only DH handoff; radar/lock state stays in the existing renderer. */
final class MCH_DhMarkerOverlay {
    private static boolean resolved;
    private static Method enqueue;
    private MCH_DhMarkerOverlay() { }
    static boolean enqueue(double x,double y,double z,double radius,int left,int bottom,int right,int top,Runnable draw) {
        if(!resolved) {
            resolved=true;
            try {
                Class<?> api=Class.forName("com.seibel.distanthorizons.forge17.effects.MarkerOverlayApi",false,MCH_DhMarkerOverlay.class.getClassLoader());
                if(api.getField("API_VERSION").getInt(null)==1)
                    enqueue=api.getMethod("enqueue",Object.class,double.class,double.class,double.class,double.class,int.class,int.class,int.class,int.class,Runnable.class);
            } catch(ClassNotFoundException absent) { }
            catch(ReflectiveOperationException | LinkageError unsupported) { mcheli.MCH_Lib.Log("[BVR] DH marker API unavailable: %s",unsupported.toString()); }
        }
        if(enqueue==null) return false;
        try { return Boolean.TRUE.equals(enqueue.invoke(null,Minecraft.getMinecraft().theWorld,x,y,z,radius,left,bottom,right,top,draw)); }
        catch(ReflectiveOperationException | RuntimeException | LinkageError failure) {
            enqueue=null;mcheli.MCH_Lib.Log("[BVR] DH marker handoff failed: %s",failure.toString());return false;
        }
    }
    static double radius(MCH_EntityInfo target) {
        MCH_AircraftInfo info=null;String type=target.entityClassName,name=target.entityName;
        if(type==null || name==null) return 1;
        if(type.contains("MCH_EntityHeli")) info=mcheli.helicopter.MCH_HeliInfoManager.get(name);
        else if(type.contains("MCP_EntityPlane")) info=mcheli.plane.MCP_PlaneInfoManager.get(name);
        else if(type.contains("MCH_EntityTank")) info=mcheli.tank.MCH_TankInfoManager.get(name);
        else if(type.contains("MCH_EntityVehicle")) info=mcheli.vehicle.MCH_VehicleInfoManager.get(name);
        return info==null?1:MCH_VisualModelBounds.radius(info);
    }
}
