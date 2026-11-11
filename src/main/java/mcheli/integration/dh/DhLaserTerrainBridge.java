package mcheli.integration.dh;

import java.lang.reflect.Method;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/** Optional DH finest-data terrain. Server callers retain all weapon/aim/permission validation. */
public final class DhLaserTerrainBridge {
    private static boolean resolved,present;
    private static Method trace;
    private DhLaserTerrainBridge() { }
    public static boolean isPresent() {
        if(!resolved) {
            resolved=true;
            try {
                Class<?> api=Class.forName("com.seibel.distanthorizons.forge17.terrain.DhTerrainRayApi",false,DhLaserTerrainBridge.class.getClassLoader());present=true;
                if(api.getField("API_VERSION").getInt(null)==1)
                    trace=api.getMethod("trace",World.class,double.class,double.class,double.class,double.class,double.class,double.class,double[].class);
            } catch(ClassNotFoundException absent) { }
            catch(ReflectiveOperationException | LinkageError unsupported) { present=true; }
        }
        return present;
    }
    public static String query(World world,Vec3 start,Vec3 end,double[] hit) {
        if(!isPresent() || trace==null) return "UNKNOWN";
        try {
            Object result=trace.invoke(null,world,start.xCoord,start.yCoord,start.zCoord,end.xCoord,end.yCoord,end.zCoord,hit);
            return result instanceof Enum?((Enum<?>)result).name():"UNKNOWN";
        } catch(ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return "UNKNOWN"; }
    }
    public static Vec3 trace(World world,Vec3 start,Vec3 end) {
        double[] hit=new double[3];String result=query(world,start,end,hit);
        if("CLEAR".equals(result)) return end;
        if("BLOCKED".equals(result) && Double.isFinite(hit[0]+hit[1]+hit[2])) return Vec3.createVectorHelper(hit[0],hit[1],hit[2]);
        return null;
    }
}
