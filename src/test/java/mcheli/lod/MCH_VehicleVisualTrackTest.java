package mcheli.lod;

import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class MCH_VehicleVisualTrackTest {
    private static final long RECEIPT=1000000000L;
    private MCH_VehicleVisualSnapshot pose(int id,int age,double x,double phase,float yaw,float gear,String texture) {
        float[] states=new float[MCH_VehicleVisualSnapshot.STATE_COUNT];
        states[MCH_VehicleVisualSnapshot.YAW]=yaw;states[MCH_VehicleVisualSnapshot.GEAR]=gear;states[MCH_VehicleVisualSnapshot.ROTOR]=(float)(phase%360);
        return new MCH_VehicleVisualSnapshot(new UUID(1,2),id,1,age,0,0,"type",texture,x,100,200,states,phase);
    }
    @Test public void fastAndReverseRotorsRetainEverySourceTurn() {
        for(double delta:new double[]{60,180,300,720,1440,-60,-180,-300,-720,-1440}) {
            MCH_VehicleVisualTrack track=new MCH_VehicleVisualTrack();MCH_VehicleVisualTrack.Sample out=new MCH_VehicleVisualTrack.Sample();
            track.observe(pose(7,0,0,10,359,0,"first"),0,RECEIPT-100000000,2);
            track.observe(pose(7,2,2,10+delta,1,90,"second"),2,RECEIPT,2);
            assertTrue(track.sample(RECEIPT+50000000,out));
            assertEquals(1,out.x,0);assertEquals(10+delta*.5,out.rotorPhase,.0001);
            assertEquals(360,out.state[MCH_VehicleVisualSnapshot.YAW],.0001);assertEquals(45,out.state[MCH_VehicleVisualSnapshot.GEAR],.0001);
            assertEquals("first",out.appearance.texture);
            assertTrue(track.sample(RECEIPT+100000000,out));assertEquals(10+delta,out.rotorPhase,.0001);assertEquals("second",out.appearance.texture);
        }
    }
    @Test public void sourcePhaseKeepsDoublePrecisionAfterLongServerUptime() {
        double phase=1000000000000.125;
        MCH_VehicleVisualSnapshot decoded=MCH_VehicleVisualSnapshot.decode(pose(7,1000,0,phase,0,0,"texture").encode());
        assertEquals(phase,decoded.rotorPhase,0);
    }
    @Test public void timelineNeverPredictsPositionOrActionsAndExpiresFromReceipt() {
        MCH_VehicleVisualTrack track=new MCH_VehicleVisualTrack();MCH_VehicleVisualTrack.Sample out=new MCH_VehicleVisualTrack.Sample();
        track.observe(pose(7,0,0,0,0,0,"texture"),0,RECEIPT-100000000,2);
        track.observe(pose(7,2,20,300,20,90,"texture"),2,RECEIPT,2);
        assertTrue(track.sample(RECEIPT+900000000,out));assertEquals(20,out.x,0);assertEquals(300,out.rotorPhase,0);
        assertFalse(track.sample(RECEIPT+1000000001L,out));
    }
    @Test public void teleportAndReusedEntityIdentityResetTheBuffer() {
        MCH_VehicleVisualTrack track=new MCH_VehicleVisualTrack();MCH_VehicleVisualTrack.Sample out=new MCH_VehicleVisualTrack.Sample();
        track.observe(pose(7,0,0,0,0,0,"texture"),0,RECEIPT,2);
        track.observe(pose(7,2,1000,300,0,0,"texture"),2,RECEIPT,2);
        assertTrue(track.sample(RECEIPT,out));assertEquals(1000,out.x,0);
        track.observe(pose(8,4,1001,400,0,0,"texture"),4,RECEIPT,2);
        assertTrue(track.sample(RECEIPT,out));assertEquals(8,out.appearance.entityId);assertEquals(1001,out.x,0);
    }
    @Test public void latePacketsCannotRewindTheTimelineOrRenewExpiry() {
        MCH_VehicleVisualTrack track=new MCH_VehicleVisualTrack();MCH_VehicleVisualTrack.Sample out=new MCH_VehicleVisualTrack.Sample();
        track.observe(pose(7,2,2,300,0,0,"texture"),2,RECEIPT,2);
        track.observe(pose(7,1,1,150,0,0,"old"),1,RECEIPT+900000000,2);
        assertTrue(track.sample(RECEIPT+900000000,out));assertEquals(2,out.x,0);assertEquals("texture",out.appearance.texture);
        assertFalse(track.sample(RECEIPT+1000000001L,out));
    }
    @Test public void everySupportedUpdateIntervalReachesItsLastAuthoritativeSample() {
        for(int interval=1;interval<=20;interval++) {
            MCH_VehicleVisualTrack track=new MCH_VehicleVisualTrack();MCH_VehicleVisualTrack.Sample out=new MCH_VehicleVisualTrack.Sample();
            track.observe(pose(7,0,0,0,0,0,"first"),0,RECEIPT-interval*50000000L,interval);
            track.observe(pose(7,interval,10,1000,0,0,"last"),interval,RECEIPT,interval);
            assertTrue(track.sample(RECEIPT+Math.max(2,interval)*50000000L,out));
            assertEquals(10,out.x,0);assertEquals(1000,out.rotorPhase,0);assertEquals("last",out.appearance.texture);
        }
    }
}
