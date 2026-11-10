package mcheli.aircraft;

import mcheli.helicopter.MCH_HeliInfo;
import mcheli.wrapper.modelloader.W_ModelCustom;
import org.junit.Test;
import static org.junit.Assert.*;

public class MCH_VisualModelBoundsTest {
    private static final class Model extends W_ModelCustom {
        Model(float radius) { minX=minY=minZ=-radius;maxX=maxY=maxZ=radius; }
        public String getType() { return "fixture"; }public void renderAll() { }public void renderOnly(String... names) { }
        public void renderPart(String name) { }public void renderAllExcept(String... names) { }public boolean containsPart(String name) { return true; }
        public void renderAll(int start,int count) { }public void renderAllLine(int start,int count) { }public int getVertexNum() { return 8; }public int getFaceNum() { return 12; }
    }
    @Test public void namedBodyPartsRemainInsideBoundsDuringPivotRotation() {
        MCH_HeliInfo info=new MCH_HeliInfo("bounds");info.model=new Model(1);
        info.partRotPart.add(info.new RotPart(10,0,0,0,1,0,2,true,"part"));
        double radius=MCH_VisualModelBounds.radius(info);
        for(int degrees=0;degrees<360;degrees++) {
            double angle=Math.toRadians(degrees);
            for(int vertex=0;vertex<8;vertex++) {
                double x=(vertex&1)==0?-1:1,y=(vertex&2)==0?-1:1,z=(vertex&4)==0?-1:1;
                double rx=10+(x-10)*Math.cos(angle)+z*Math.sin(angle),rz=-(x-10)*Math.sin(angle)+z*Math.cos(angle);
                assertTrue(rx*rx+y*y+rz*rz<=radius*radius);
            }
        }
    }
    @Test public void childEnvelopesIncludeTheirParentPivot() {
        MCH_HeliInfo info=new MCH_HeliInfo("bounds");info.model=new Model(1);
        MCH_AircraftInfo.PartWeapon parent=info.new PartWeapon(new String[]{"gun"},false,false,false,true,true,20,0,0,"parent",0,1,0,0,false);
        parent.child.add(info.new PartWeaponChild(new String[]{"gun"},true,true,10,0,0,"child",0,1,0,0));info.partWeapon.add(parent);
        assertTrue(MCH_VisualModelBounds.radius(info)>=61);
    }
    @Test public void cyclicOrUnboundedAssetsAreWithheld() {
        MCH_HeliInfo info=new MCH_HeliInfo("bounds");Model model=new Model(1);info.model=model;
        model.maxX=Float.NaN;assertTrue(Double.isNaN(MCH_VisualModelBounds.radius(info)));model.maxX=1;
        MCH_AircraftInfo.PartWeapon parent=info.new PartWeapon(new String[]{"gun"},false,false,false,true,true,0,0,0,"parent",0,1,0,0,false);
        parent.child.add(parent);info.partWeapon.add(parent);assertTrue(Double.isNaN(MCH_VisualModelBounds.radius(info)));
    }
}
