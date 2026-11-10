package mcheli.lod;

/** Four source samples on a bounded, non-predicting presentation timeline. No entity or world simulation. */
public final class MCH_VehicleVisualTrack {
    private final MCH_VehicleVisualSnapshot[] poses=new MCH_VehicleVisualSnapshot[4];
    private final long[] sequences=new long[4];
    private long received;
    private int count,interval=2;
    public void observe(MCH_VehicleVisualSnapshot pose,long sequence,long receipt,int interval) {
        if(pose==null || sequence<0 || interval<1 || interval>20) throw new IllegalArgumentException("Invalid vehicle timeline sample");
        if(count>0 && sequence<=sequences[count-1]) return;
        if(count>0) {
            MCH_VehicleVisualSnapshot previous=poses[count-1];double dx=pose.x-previous.x,dy=pose.y-previous.y,dz=pose.z-previous.z;
            if(pose.age<previous.age || !previous.uuid.equals(pose.uuid) || previous.entityId!=pose.entityId || previous.kind!=pose.kind || !previous.type.equals(pose.type)
                    || dx*dx+dy*dy+dz*dz>4096 || sequence-sequences[count-1]>20) count=0;
        }
        if(count==4) {
            System.arraycopy(poses,1,poses,0,3);System.arraycopy(sequences,1,sequences,0,3);
            count=3;
        }
        poses[count]=pose;sequences[count]=sequence;count++;
        this.interval=interval;received=receipt;
    }
    public boolean sample(long now,Sample out) {
        if(count==0 || now-received>1000000000L) return false;
        int delay=Math.max(2,interval);
        double tick=sequences[count-1]+Math.max(0,Math.min(delay,(now-received)/50000000.0))-delay;
        int first=0,last=0;
        if(tick>=sequences[count-1]) first=last=count-1;
        else for(int i=1;i<count;i++) if(tick>=sequences[i-1] && tick<sequences[i]) { first=i-1;last=i;break; }
        double fraction=first==last?0:Math.max(0,Math.min(1,(tick-sequences[first])/(sequences[last]-sequences[first])));
        MCH_VehicleVisualSnapshot a=poses[first],b=poses[last];
        out.first=a;out.last=b;out.fraction=(float)fraction;out.appearance=fraction>=1?b:a;
        out.x=mix(a.x,b.x,fraction);out.y=mix(a.y,b.y,fraction);out.z=mix(a.z,b.z,fraction);out.age=(float)mix(a.age,b.age,fraction);
        for(int i=0;i<out.state.length;i++) {
            boolean wraps=i==MCH_VehicleVisualSnapshot.YAW || i==MCH_VehicleVisualSnapshot.ROLL || i==MCH_VehicleVisualSnapshot.TURRET_YAW;
            out.state[i]=wraps?angle(a.state(i),b.state(i),fraction):(float)mix(a.state(i),b.state(i),fraction);
        }
        out.rotorPhase=mix(a.rotorPhase,b.rotorPhase,fraction);
        out.state[MCH_VehicleVisualSnapshot.ROTOR]=(float)(out.rotorPhase%360);
        return true;
    }
    private static double mix(double a,double b,double t) { return a+(b-a)*t; }
    /** Anchor to the actual source blade, using the unwrapped phase only to recover whole turns. */
    public static float bladeRotation(Sample sample,float first,float last,boolean unfolded) {
        double delta=last-first;delta-=Math.floor((delta+180)/360)*360;
        if(unfolded) delta+=360*Math.rint(((sample.last.rotorPhase-sample.first.rotorPhase)-delta)/360);
        return (float)(first+delta*sample.fraction);
    }
    private static float angle(float a,float b,double t) { double d=b-a;d-=Math.floor((d+180)/360)*360;return (float)(a+d*t); }
    public static final class Sample {
        public MCH_VehicleVisualSnapshot first,last,appearance;
        public double x,y,z,rotorPhase;
        public float age,fraction;
        public final float[] state=new float[MCH_VehicleVisualSnapshot.STATE_COUNT];
    }
}
