package mcheli.render;

import java.lang.reflect.Method;
import java.util.function.DoubleConsumer;

/** Optional DH render-thread geometry API. Presence never grants server visibility. */
final class MCH_DistantVisuals {
    private static boolean resolved,supported,failed;
    private static Method register,ready,collecting,submit;
    private MCH_DistantVisuals() { }
    static boolean supported() { resolve();return supported; }
    static boolean ready() { return supported() && Boolean.TRUE.equals(call(ready)); }
    static boolean collecting() { return supported() && Boolean.TRUE.equals(call(collecting)); }
    static void register(DoubleConsumer collector) { if(supported()) call(register,2,collector); }
    static boolean submit(Object world,double x,double y,double z,double center,double radius,float partial,int light,Runnable geometry) {
        return supported() && Boolean.TRUE.equals(call(submit,world,2,x,y,z,center,radius,partial,light,geometry));
    }
    private static Object call(Method method,Object... args) {
        if(failed || method==null) return null;
        try { return method.invoke(null,args); }
        catch(ReflectiveOperationException | RuntimeException | LinkageError error) {
            failed=true;
            org.apache.logging.log4j.LogManager.getLogger("MCH_DistantVisuals").error("DH visual terrain integration failed; distant geometry withheld",error);
            return null;
        }
    }
    private static void resolve() {
        if(resolved) return;resolved=true;
        try {
            Class<?> api=Class.forName("com.seibel.distanthorizons.forge17.effects.VisualTerrainApi",false,MCH_DistantVisuals.class.getClassLoader());
            if(api.getField("API_VERSION").getInt(null)!=1) return;
            register=api.getMethod("register",int.class,DoubleConsumer.class);
            ready=api.getMethod("ready");collecting=api.getMethod("collecting");
            submit=api.getMethod("submit",Object.class,int.class,double.class,double.class,double.class,double.class,double.class,
                    float.class,int.class,Runnable.class);
            supported=true;
        } catch(ClassNotFoundException absent) { }
        catch(ReflectiveOperationException | LinkageError unsupported) {
            org.apache.logging.log4j.LogManager.getLogger("MCH_DistantVisuals").warn("DH visual terrain API unavailable; legacy compatibility path retained",unsupported);
        }
    }
}
