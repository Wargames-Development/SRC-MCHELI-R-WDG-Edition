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
    public static final int API_VERSION=3;
    private MCH_ExternalVehiclePresentation() { }
    public static boolean eligible(Entity entity) {
        if(!(entity instanceof MCH_EntityAircraft) || entity.worldObj==null || entity.worldObj.isRemote) return false;
        MCH_EntityAircraft aircraft=(MCH_EntityAircraft)entity;
        if(aircraft.isDead || aircraft.isDestroyed() || aircraft.isInvisible() || aircraft.getAcInfo()==null) return false;
        Class<?> type=aircraft.getClass();
        return type==MCH_EntityHeli.class || type==MCP_EntityPlane.class || type==MCH_EntityTank.class || type==MCH_EntityVehicle.class;
    }
    public static byte[] capture(Entity entity) {
        if(!eligible(entity)) return null;
        MCH_EntityAircraft aircraft=(MCH_EntityAircraft)entity;
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
        double rotorPhase=kind==MCH_VehicleVisualSnapshot.HELICOPTER?((MCH_EntityHeli)aircraft).presentationRotorPhase
            :kind==MCH_VehicleVisualSnapshot.PLANE?((MCP_EntityPlane)aircraft).presentationRotorPhase:0;
        int flags=(aircraft.getRiddenByEntity()!=null?1:0)|(aircraft.moveLeft?2:0)|(aircraft.moveRight?4:0)|(aircraft.onGround?8:0)|(aircraft.isSearchLightON()?16:0)|(aircraft.isRadarEnabledRuntime()?32:0);
        return new MCH_VehicleVisualSnapshot(aircraft.getUniqueID(),aircraft.getEntityId(),kind,aircraft.ticksExisted,flags,light(aircraft),
            aircraft.getAcInfo().name,aircraft.getTextureName(),aircraft.posX,aircraft.posY,aircraft.posZ,state,rotorPhase,parts(aircraft,kind).encode()).encode();
    }
    private static MCH_VehicleVisualParts parts(MCH_EntityAircraft aircraft,int kind) {
        if(aircraft.weaponBays.length+aircraft.turretWeaponBays.length>MCH_VehicleVisualParts.MAX_PARTS) throw new IllegalArgumentException("Too many source bays");
        float[] bays=new float[aircraft.weaponBays.length],turret=new float[aircraft.turretWeaponBays.length];
        for(int i=0;i<bays.length;i++) bays[i]=aircraft.weaponBays[i].rot;
        for(int i=0;i<turret.length;i++) turret[i]=aircraft.turretWeaponBays[i].rot;
        float[][] blades=new float[0][];int fold=2;
        if(kind==MCH_VehicleVisualSnapshot.HELICOPTER) {
            MCH_EntityHeli heli=(MCH_EntityHeli)aircraft;fold=heli.getDataWatcher().getWatchableObjectByte(30);
            if(heli.rotors.length>MCH_VehicleVisualParts.MAX_ROTORS) throw new IllegalArgumentException("Too many source rotors");
            blades=new float[heli.rotors.length][];
            for(int i=0;i<blades.length;i++) {
                if(heli.rotors[i].blades.length>MCH_VehicleVisualParts.MAX_BARRELS) throw new IllegalArgumentException("Too many source blades");
                blades[i]=new float[heli.rotors[i].blades.length];
                for(int j=0;j<blades[i].length;j++) blades[i][j]=heli.rotors[i].blades[j].getRotation();
            }
        }
        int count=aircraft.getAcInfo().weaponSetList.isEmpty()?0:aircraft.weapons.length;
        if(count>MCH_VehicleVisualParts.MAX_WEAPONS) throw new IllegalArgumentException("Too many source weapon sets");
        MCH_VehicleVisualParts.Weapon[] weapons=new MCH_VehicleVisualParts.Weapon[count];
        for(int i=0;i<count;i++) {
            mcheli.weapon.MCH_WeaponSet weapon=aircraft.weapons[i];
            if(weapon.getWeaponNum()>32) throw new IllegalArgumentException("Too many source weapon barrels");
            float[] recoil=new float[weapon.getWeaponNum()];int mask=0;
            for(int j=0;j<recoil.length;j++) { recoil[j]=weapon.recoilBuf[j].recoilBuf;if(aircraft.isWeaponNotCooldown(weapon,j)) mask|=1<<j; }
            weapons[i]=new MCH_VehicleVisualParts.Weapon(weapon.rotationYaw,weapon.rotationPitch,weapon.rotationTurretYaw,weapon.rotBarrel,weapon.getAmmoNum(),mask,recoil);
        }
        int eraCount=0;long eraMask=0;
        for(mcheli.aircraft.MCH_BoundingBox box:aircraft.extraBoundingBox) if(box.isERA) {
            if(eraCount==64) throw new IllegalArgumentException("Too many source ERA panels");
            if(box.eraActive) eraMask|=1L<<eraCount;eraCount++;
        }
        net.minecraft.entity.Entity lightUser=aircraft.getEntityBySeatId(1);if(lightUser==null) lightUser=aircraft.getEntityBySeatId(0);
        float searchYaw=lightUser==null?aircraft.getLastRiderYaw():lightUser.rotationYaw;
        float searchPitch=lightUser==null?aircraft.getLastRiderPitch():lightUser.rotationPitch;
        float steering=kind==MCH_VehicleVisualSnapshot.TANK && ((MCH_EntityTank)aircraft).isWheeledHandling()
            ?-aircraft.getDataWatcher().getWatchableObjectFloat(18):0;
        return new MCH_VehicleVisualParts(aircraft.throttleBack,aircraft.rotLightHatch,steering,searchYaw,searchPitch,fold,bays,turret,blades,weapons,eraCount,eraMask);
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
