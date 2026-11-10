package mcheli.lod;

import java.util.Arrays;
import java.nio.ByteBuffer;
import org.junit.Test;
import static org.junit.Assert.*;

public class MCH_VehicleVisualPartsTest {
    private MCH_VehicleVisualParts fixture() {
        return new MCH_VehicleVisualParts(.2F,.5F,-.7F,120,-30,1,new float[]{0,45,90},new float[]{20},new float[][]{{5,185}},
            new MCH_VehicleVisualParts.Weapon[]{new MCH_VehicleVisualParts.Weapon(35,-10,20,270,2,1,new float[]{.6F,0})},2,1);
    }
    private void invalid(Runnable action) { try { action.run();fail("invalid parts accepted"); }catch(IllegalArgumentException expected) { } }
    @Test public void sourceWeaponsAmmoBladesBaysAndEraRoundTrip() {
        MCH_VehicleVisualParts decoded=MCH_VehicleVisualParts.decode(fixture().encode());
        assertEquals(.2,decoded.throttleBack,.00001);assertEquals(-.7,decoded.steering,.00001);assertEquals(120,decoded.searchYaw,0);
        assertEquals(3,decoded.bayCount(false));assertEquals(45,decoded.bay(false,1),0);assertEquals(20,decoded.bay(true,0),0);
        assertEquals(2,decoded.bladeCount(0));assertEquals(185,decoded.blade(0,1),0);assertEquals(1,decoded.foldState);
        assertEquals(2,decoded.weapon(0).ammo);assertEquals(1,decoded.weapon(0).usedMask);assertEquals(.6,decoded.weapon(0).recoil(0),.00001);
        assertEquals(2,decoded.eraCount);assertEquals(1,decoded.eraMask);
    }
    @Test public void maximumSupportedContentRemainsUnderSourcePayloadBudget() {
        MCH_VehicleVisualParts.Weapon[] weapons=new MCH_VehicleVisualParts.Weapon[32];
        for(int i=0;i<weapons.length;i++) weapons[i]=new MCH_VehicleVisualParts.Weapon(0,0,0,0,1,15,new float[4]);
        float[][] blades=new float[16][8];
        MCH_VehicleVisualParts source=new MCH_VehicleVisualParts(0,0,0,0,0,2,new float[32],new float[32],blades,weapons,64,-1);
        byte[] bytes=source.encode();assertTrue(bytes.length<=MCH_VehicleVisualParts.MAX_BYTES);
        MCH_VehicleVisualParts decoded=MCH_VehicleVisualParts.decode(bytes);
        assertEquals(32,decoded.weaponCount());assertEquals(16,decoded.rotorCount());assertEquals(-1,decoded.eraMask);
    }
    @Test public void allTruncatedLengthsAndTrailingDataAreRejected() {
        byte[] bytes=fixture().encode();
        for(int i=0;i<bytes.length;i++) { final byte[] truncated=Arrays.copyOf(bytes,i);invalid(()->MCH_VehicleVisualParts.decode(truncated)); }
        invalid(()->MCH_VehicleVisualParts.decode(Arrays.copyOf(bytes,bytes.length+1)));
        byte[] version=bytes.clone();version[0]=99;invalid(()->MCH_VehicleVisualParts.decode(version));
        byte[] count=bytes.clone();count[22]=(byte)255;invalid(()->MCH_VehicleVisualParts.decode(count));
        byte[] nan=bytes.clone();ByteBuffer.wrap(nan).putFloat(1,Float.NaN);invalid(()->MCH_VehicleVisualParts.decode(nan));
    }
    @Test public void arraysCannotBeAliasedAcrossSourceAndPresentation() {
        float[] recoil={.5F},bays={45};float[][] blades={{90}};
        MCH_VehicleVisualParts.Weapon weapon=new MCH_VehicleVisualParts.Weapon(0,0,0,0,1,0,recoil);
        MCH_VehicleVisualParts.Weapon[] weapons={weapon};
        MCH_VehicleVisualParts source=new MCH_VehicleVisualParts(0,0,0,0,0,2,bays,new float[0],blades,weapons,0,0);
        recoil[0]=0;bays[0]=0;blades[0][0]=0;weapons[0]=null;
        assertEquals(.5,source.weapon(0).recoil(0),0);assertEquals(45,source.bay(false,0),0);assertEquals(90,source.blade(0,0),0);
    }
    @Test public void impossibleCountsMasksAndRotationsAreRejected() {
        invalid(()->new MCH_VehicleVisualParts.Weapon(0,0,0,0,-1,0,new float[0]));
        invalid(()->new MCH_VehicleVisualParts.Weapon(0,0,0,0,0,2,new float[1]));
        invalid(()->new MCH_VehicleVisualParts.Weapon(0,0,0,0,0,0,new float[]{Float.NaN}));
        invalid(()->new MCH_VehicleVisualParts(0,0,2,0,0,0,new float[0],new float[0],new float[0][],new MCH_VehicleVisualParts.Weapon[0],0,0));
        invalid(()->new MCH_VehicleVisualParts(0,0,0,0,0,0,new float[]{181},new float[0],new float[0][],new MCH_VehicleVisualParts.Weapon[0],0,0));
        invalid(()->new MCH_VehicleVisualParts(0,0,0,0,0,0,new float[0],new float[0],new float[0][],new MCH_VehicleVisualParts.Weapon[0],1,2));
    }
}
