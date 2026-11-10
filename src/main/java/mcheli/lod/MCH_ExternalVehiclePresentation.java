package mcheli.lod;

import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.plane.MCP_EntityPlane;
import mcheli.tank.MCH_EntityTank;
import mcheli.vehicle.MCH_EntityVehicle;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;

/** Optional source adapter, with no DH or client dependency. Does not claim legacy ownership. */
public final class MCH_ExternalVehiclePresentation {
    public static final int API_VERSION=1;
    private MCH_ExternalVehiclePresentation() { }
    public static byte[] capture(Entity entity) {
        if(!(entity instanceof MCH_EntityAircraft) || entity.worldObj==null || entity.worldObj.isRemote) return null;
        MCH_EntityAircraft aircraft=(MCH_EntityAircraft)entity;
        if(aircraft.isDead || aircraft.isDestroyed() || aircraft.isInvisible() || aircraft.getAcInfo()==null) return null;
        int kind=aircraft.getClass()==MCH_EntityHeli.class?MCH_VehicleVisualSnapshot.HELICOPTER
            :aircraft.getClass()==MCP_EntityPlane.class?MCH_VehicleVisualSnapshot.PLANE
            :aircraft.getClass()==MCH_EntityTank.class?MCH_VehicleVisualSnapshot.TANK
            :aircraft.getClass()==MCH_EntityVehicle.class?MCH_VehicleVisualSnapshot.VEHICLE:0;
        if(kind==0) return null;
        float[] state=new float[MCH_VehicleVisualSnapshot.STATE_COUNT];
        state[0]=aircraft.getRotYaw();state[1]=aircraft.getRotPitch();state[2]=aircraft.getRotRoll();
        state[3]=aircraft.getLastRiderYaw();state[4]=aircraft.getLastRiderPitch();
        if(kind==MCH_VehicleVisualSnapshot.HELICOPTER) state[5]=(float)((MCH_EntityHeli)aircraft).rotationRotor;
        if(kind==MCH_VehicleVisualSnapshot.PLANE) {
            MCP_EntityPlane plane=(MCP_EntityPlane)aircraft;
            state[5]=plane.rotationRotor;state[9]=plane.getNozzleRotation();state[10]=plane.getWingRotation();
        }
        state[6]=aircraft.getHatchRotation();state[7]=aircraft.getCanopyRotation();state[8]=aircraft.getLandingGearRotation();
        state[11]=(float)aircraft.getCurrentThrottle();
        int flags=(aircraft.getRiddenByEntity()!=null?1:0)|(aircraft.moveLeft?2:0)|(aircraft.moveRight?4:0)|(aircraft.onGround?8:0);
        return new MCH_VehicleVisualSnapshot(aircraft.getUniqueID(),aircraft.getEntityId(),kind,aircraft.ticksExisted,flags,light(aircraft),
            aircraft.getAcInfo().name,aircraft.getTextureName(),aircraft.posX,aircraft.posY,aircraft.posZ,state).encode();
    }
    private static int light(MCH_EntityAircraft aircraft) {
        int x=MathHelper.floor_double(aircraft.posX),z=MathHelper.floor_double(aircraft.posZ);
        int y=Math.max(0,Math.min(255,MathHelper.floor_double(aircraft.posY)));
        // Saved light access is common-side; the guard prevents generating or loading a chunk.
        if(!aircraft.worldObj.getChunkProvider().chunkExists(x>>4,z>>4)) return 0;
        int block=aircraft.worldObj.getSavedLightValue(EnumSkyBlock.Block,x,y,z);
        int sky=aircraft.worldObj.provider.hasNoSky?0:aircraft.worldObj.getSavedLightValue(EnumSkyBlock.Sky,x,y,z);
        return (Math.max(0,Math.min(15,sky))<<20)|(Math.max(0,Math.min(15,block))<<4);
    }
}
