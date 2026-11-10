package mcheli.lod;

import java.nio.ByteBuffer;

/** Bounded immutable part state; only values needed to draw source-confirmed vehicle geometry. */
public final class MCH_VehicleVisualParts {
    public static final int MAX_BYTES=3072,MAX_WEAPONS=32,MAX_BARRELS=128,MAX_PARTS=64,MAX_ROTORS=16;
    public final float throttleBack,lightHatch,steering,searchYaw,searchPitch;
    public final int foldState,eraCount;
    public final long eraMask;
    private final float[] bays,turretBays;
    private final float[][] blades;
    private final Weapon[] weapons;
    public MCH_VehicleVisualParts(float throttleBack,float lightHatch,float steering,float searchYaw,float searchPitch,int foldState,
            float[] bays,float[] turretBays,float[][] blades,Weapon[] weapons,int eraCount,long eraMask) {
        if(!finite(throttleBack,lightHatch,steering,searchYaw,searchPitch) || throttleBack<0 || throttleBack>10 || lightHatch<0 || lightHatch>1
                || Math.abs(steering)>1 || foldState<0 || foldState>3 || eraCount<0 || eraCount>64
                || (eraCount<64 && (eraMask>>>eraCount)!=0) || bays==null || turretBays==null || bays.length+turretBays.length>MAX_PARTS
                || blades==null || blades.length>MAX_ROTORS || weapons==null || weapons.length>MAX_WEAPONS) throw new IllegalArgumentException("Invalid vehicle parts");
        this.bays=angles(bays,180);this.turretBays=angles(turretBays,180);this.blades=new float[blades.length][];
        int bladeCount=0;for(int i=0;i<blades.length;i++) { this.blades[i]=angles(blades[i],360);bladeCount+=blades[i].length; }
        if(bladeCount>MAX_BARRELS) throw new IllegalArgumentException("Too many rotor blades");
        this.weapons=weapons.clone();int barrels=0;
        for(Weapon weapon:weapons) { if(weapon==null) throw new IllegalArgumentException("Missing weapon state");barrels+=weapon.barrels(); }
        if(barrels>MAX_BARRELS) throw new IllegalArgumentException("Too many weapon barrels");
        this.throttleBack=throttleBack;this.lightHatch=lightHatch;this.steering=steering;this.searchYaw=searchYaw;this.searchPitch=searchPitch;
        this.foldState=foldState;this.eraCount=eraCount;this.eraMask=eraMask;
    }
    private static float[] angles(float[] values,float maximum) {
        if(values==null || values.length>MAX_BARRELS) throw new IllegalArgumentException("Invalid part count");
        float[] copy=values.clone();for(float value:copy) if(!Float.isFinite(value) || value<0 || value>maximum) throw new IllegalArgumentException("Invalid part angle");return copy;
    }
    private static boolean finite(float... values) { for(float value:values) if(!Float.isFinite(value) || Math.abs(value)>10000000) return false;return true; }
    public int bayCount(boolean turret) { return (turret?turretBays:bays).length; }
    public float bay(boolean turret,int index) { return (turret?turretBays:bays)[index]; }
    public int rotorCount() { return blades.length; }
    public int bladeCount(int rotor) { return blades[rotor].length; }
    public float blade(int rotor,int index) { return blades[rotor][index]; }
    public int weaponCount() { return weapons.length; }
    public Weapon weapon(int index) { return weapons[index]; }
    public byte[] encode() {
        ByteBuffer out=ByteBuffer.allocate(MAX_BYTES);
        out.put((byte)1).putFloat(throttleBack).putFloat(lightHatch).putFloat(steering).putFloat(searchYaw).putFloat(searchPitch).put((byte)foldState);
        write(out,bays);write(out,turretBays);out.put((byte)blades.length);for(float[] rotor:blades) write(out,rotor);
        out.put((byte)weapons.length);for(Weapon weapon:weapons) weapon.write(out);
        out.put((byte)eraCount).putLong(eraMask);byte[] bytes=new byte[out.position()];out.flip();out.get(bytes);return bytes;
    }
    public static MCH_VehicleVisualParts decode(byte[] bytes) {
        if(bytes==null || bytes.length<35 || bytes.length>MAX_BYTES) throw new IllegalArgumentException("Invalid parts envelope");
        try {
            ByteBuffer in=ByteBuffer.wrap(bytes);if((in.get()&255)!=1) throw new IllegalArgumentException("Unsupported parts version");
            float back=in.getFloat(),hatch=in.getFloat(),steering=in.getFloat(),yaw=in.getFloat(),pitch=in.getFloat();int fold=in.get()&255;
            float[] bays=read(in,MAX_PARTS),turret=read(in,MAX_PARTS);
            int count=in.get()&255;if(count>MAX_ROTORS) throw new IllegalArgumentException("Too many rotors");
            float[][] blades=new float[count][];int total=0;
            for(int i=0;i<count;i++) { blades[i]=read(in,MAX_BARRELS-total);total+=blades[i].length; }
            count=in.get()&255;if(count>MAX_WEAPONS) throw new IllegalArgumentException("Too many weapon sets");
            Weapon[] weapons=new Weapon[count];total=0;
            for(int i=0;i<count;i++) {
                float wy=in.getFloat(),wp=in.getFloat(),wt=in.getFloat(),barrel=in.getFloat();int ammo=in.getInt(),mask=in.getInt();
                float[] recoil=read(in,MAX_BARRELS-total);total+=recoil.length;weapons[i]=new Weapon(wy,wp,wt,barrel,ammo,mask,recoil);
            }
            int era=in.get()&255;long mask=in.getLong();
            if(in.hasRemaining()) throw new IllegalArgumentException("Trailing part state");
            return new MCH_VehicleVisualParts(back,hatch,steering,yaw,pitch,fold,bays,turret,blades,weapons,era,mask);
        } catch(java.nio.BufferUnderflowException invalid) { throw new IllegalArgumentException("Truncated vehicle parts",invalid); }
    }
    private static void write(ByteBuffer out,float[] values) { out.put((byte)values.length);for(float value:values) out.putFloat(value); }
    private static float[] read(ByteBuffer in,int maximum) {
        int count=in.get()&255;if(count>maximum || count*4>in.remaining()) throw new IllegalArgumentException("Invalid part array length");
        float[] values=new float[count];for(int i=0;i<count;i++) values[i]=in.getFloat();return values;
    }
    public static final class Weapon {
        public final float yaw,pitch,turretYaw,barrel;
        public final int ammo,usedMask;
        private final float[] recoil;
        public Weapon(float yaw,float pitch,float turretYaw,float barrel,int ammo,int usedMask,float[] recoil) {
            if(!finite(yaw,pitch,turretYaw,barrel) || ammo<0 || ammo>1000000 || recoil==null || recoil.length>32
                    || (recoil.length<32 && (usedMask>>>recoil.length)!=0)) throw new IllegalArgumentException("Invalid visual weapon state");
            this.recoil=angles(recoil,1);this.yaw=yaw;this.pitch=pitch;this.turretYaw=turretYaw;this.barrel=barrel;this.ammo=ammo;this.usedMask=usedMask;
        }
        public int barrels() { return recoil.length; }
        public float recoil(int index) { return recoil[index]; }
        private void write(ByteBuffer out) { out.putFloat(yaw).putFloat(pitch).putFloat(turretYaw).putFloat(barrel).putInt(ammo).putInt(usedMask);MCH_VehicleVisualParts.write(out,recoil); }
    }
}
