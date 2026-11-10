package mcheli.lod;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Immutable source visual state. DH owns recipient policy, timing and transport envelopes. */
public final class MCH_VehicleVisualSnapshot {
    public static final int VERSION=1,MAX_BYTES=512,STATE_COUNT=12;
    public static final int HELICOPTER=1,PLANE=2,TANK=3,VEHICLE=4;
    public static final int YAW=0,PITCH=1,ROLL=2,TURRET_YAW=3,TURRET_PITCH=4,ROTOR=5,
        HATCH=6,CANOPY=7,GEAR=8,NOZZLE=9,WING=10,THROTTLE=11;
    public final UUID uuid;
    public final int entityId,kind,age,flags,light;
    public final String type,texture;
    public final double x,y,z;
    private final float[] state;
    public MCH_VehicleVisualSnapshot(UUID uuid,int entityId,int kind,int age,int flags,int light,String type,String texture,
            double x,double y,double z,float[] state) {
        if(uuid==null || entityId<0 || kind<HELICOPTER || kind>VEHICLE || age<0 || (flags & ~15)!=0
                || (light & ~0x00F000F0)!=0 || !Double.isFinite(x+y+z) || Math.abs(x)>30000000 || Math.abs(z)>30000000
                || Math.abs(y)>65536 || state==null || state.length!=STATE_COUNT) throw new IllegalArgumentException("Invalid vehicle presentation");
        name(type);name(texture);
        for(float value:state) if(!Float.isFinite(value) || Math.abs(value)>10000000) throw new IllegalArgumentException("Invalid vehicle animation");
        for(int i=HATCH;i<=WING;i++) if(state[i]<0 || state[i]>180) throw new IllegalArgumentException("Invalid vehicle part rotation");
        if(state[THROTTLE]<0 || state[THROTTLE]>10) throw new IllegalArgumentException("Invalid vehicle throttle");
        this.uuid=uuid;this.entityId=entityId;this.kind=kind;this.age=age;this.flags=flags;this.light=light;this.type=type;this.texture=texture;
        this.x=x;this.y=y;this.z=z;this.state=state.clone();
    }
    public float state(int index) { return state[index]; }
    public float[] states() { return state.clone(); }
    private static byte[] name(String name) {
        if(name==null || name.isEmpty() || name.indexOf('/')>=0 || name.indexOf('\\')>=0 || name.contains("..")) throw new IllegalArgumentException("Invalid vehicle asset name");
        for(int i=0;i<name.length();i++) if(Character.isISOControl(name.charAt(i))) throw new IllegalArgumentException("Invalid vehicle asset name");
        byte[] encoded=name.getBytes(StandardCharsets.UTF_8);
        if(encoded.length>96) throw new IllegalArgumentException("Vehicle asset name exceeds limit");return encoded;
    }
    public byte[] encode() {
        ByteBuffer out=ByteBuffer.allocate(MAX_BYTES);
        out.put((byte)VERSION).put((byte)kind).putInt(entityId).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits());
        out.putInt(age).put((byte)flags).putInt(light).putDouble(x).putDouble(y).putDouble(z);
        for(float value:state) out.putFloat(value);
        for(String value:new String[]{type,texture}) { byte[] text=name(value);out.put((byte)text.length).put(text); }
        byte[] bytes=new byte[out.position()];out.flip();out.get(bytes);return bytes;
    }
    public static MCH_VehicleVisualSnapshot decode(byte[] bytes) {
        if(bytes==null || bytes.length<107 || bytes.length>MAX_BYTES) throw new IllegalArgumentException("Invalid vehicle envelope");
        try {
            ByteBuffer in=ByteBuffer.wrap(bytes);
            if((in.get()&255)!=VERSION) throw new IllegalArgumentException("Unsupported vehicle envelope");
            int kind=in.get()&255,id=in.getInt();UUID uuid=new UUID(in.getLong(),in.getLong());
            int age=in.getInt(),flags=in.get()&255,light=in.getInt();double x=in.getDouble(),y=in.getDouble(),z=in.getDouble();
            float[] state=new float[STATE_COUNT];for(int i=0;i<state.length;i++) state[i]=in.getFloat();
            String type=readName(in),texture=readName(in);
            if(in.hasRemaining()) throw new IllegalArgumentException("Trailing vehicle data");
            return new MCH_VehicleVisualSnapshot(uuid,id,kind,age,flags,light,type,texture,x,y,z,state);
        } catch(java.nio.BufferUnderflowException | CharacterCodingException invalid) {
            throw new IllegalArgumentException("Truncated or malformed vehicle envelope",invalid);
        }
    }
    private static String readName(ByteBuffer in) throws CharacterCodingException {
        int length=in.get()&255;if(length<1 || length>96 || length>in.remaining()) throw new IllegalArgumentException("Invalid vehicle name envelope");
        ByteBuffer text=in.slice();text.limit(length);in.position(in.position()+length);
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(text).toString();
    }
}
