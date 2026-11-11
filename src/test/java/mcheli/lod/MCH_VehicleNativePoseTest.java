package mcheli.lod;

import java.lang.reflect.Field;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.helicopter.*;
import mcheli.plane.*;
import mcheli.tank.*;
import mcheli.vehicle.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Exercise the real preparation branch without a world, model loader or GL context. */
public class MCH_VehicleNativePoseTest {
    private static void set(Object target,String name,Object value) throws Exception {
        Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);field.set(target,value);
    }
    @Test public void allTrackedVehicleKindsKeepTheirNativePoseWithoutDetachedSimulation() throws Exception {
        Field access=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");access.setAccessible(true);
        sun.misc.Unsafe allocator=(sun.misc.Unsafe)access.get(null);
        Class<?>[][] kinds={{MCH_EntityHeli.class,MCH_HeliInfo.class},{MCP_EntityPlane.class,MCP_PlaneInfo.class},
            {MCH_EntityTank.class,MCH_TankInfo.class},{MCH_EntityVehicle.class,MCH_VehicleInfo.class}};
        for(Class<?>[] kind:kinds) {
            MCH_EntityAircraft nativeModel=(MCH_EntityAircraft)allocator.allocateInstance(kind[0]);
            nativeModel.posY=65;nativeModel.rotationYaw=35;nativeModel.prevRotationYaw=12;
            MCH_ExternalVehicleRenderer.Context context=(MCH_ExternalVehicleRenderer.Context)allocator.allocateInstance(MCH_ExternalVehicleRenderer.Context.class);
            set(context,"sampled",true);set(context,"definition",allocator.allocateInstance(kind[1]));set(context,"tracked",nativeModel);
            assertTrue(kind[0].getName(),context.prepare());
            assertEquals(65,nativeModel.posY,0);assertEquals(35,nativeModel.rotationYaw,0);assertEquals(12,nativeModel.prevRotationYaw,0);
            Field actor=context.getClass().getDeclaredField("actor");actor.setAccessible(true);assertNull(actor.get(context));
        }
    }
}
