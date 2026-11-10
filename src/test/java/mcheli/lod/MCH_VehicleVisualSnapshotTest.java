package mcheli.lod;

import java.util.Arrays;
import java.util.UUID;
import org.junit.Test;
import org.objectweb.asm.*;
import java.io.InputStream;
import static org.junit.Assert.*;

public class MCH_VehicleVisualSnapshotTest {
    private MCH_VehicleVisualSnapshot sample(String type,String texture,float[] state) {
        return new MCH_VehicleVisualSnapshot(new UUID(9,10),11,1,200,7,0xF000F0,type,texture,100,200,-300,state);
    }
    @Test public void animationAndIdentityRoundTripWithoutAliasing() {
        float[] state=new float[MCH_VehicleVisualSnapshot.STATE_COUNT];state[0]=359;state[5]=210;state[8]=45;state[11]=.75F;
        MCH_VehicleVisualSnapshot source=sample("x","y",state);state[0]=0;
        byte[] bytes=source.encode();assertTrue(bytes.length<=MCH_VehicleVisualSnapshot.MAX_BYTES);
        MCH_VehicleVisualSnapshot copy=MCH_VehicleVisualSnapshot.decode(bytes);
        assertEquals(source.uuid,copy.uuid);assertEquals(source.entityId,copy.entityId);assertEquals(200,copy.age);
        assertEquals(359,copy.state(0),0);assertEquals(210,copy.state(5),0);assertEquals(45,copy.state(8),0);assertEquals(.75,copy.state(11),.00001);
        copy.states()[0]=180;assertEquals(359,copy.state(0),0);
    }
    private void invalid(Runnable operation) { try { operation.run();fail("invalid snapshot accepted"); }catch(IllegalArgumentException expected) { } }
    @Test public void malformedLengthsVersionsFlagsAndUtf8AreRejected() {
        byte[] good=sample("x","y",new float[12]).encode();
        for(int length=0;length<good.length;length++) { final byte[] truncated=Arrays.copyOf(good,length);invalid(()->MCH_VehicleVisualSnapshot.decode(truncated)); }
        invalid(()->MCH_VehicleVisualSnapshot.decode(Arrays.copyOf(good,good.length+1)));
        byte[] version=good.clone();version[0]=2;invalid(()->MCH_VehicleVisualSnapshot.decode(version));
        byte[] flags=good.clone();flags[26]=(byte)128;invalid(()->MCH_VehicleVisualSnapshot.decode(flags));
        byte[] utf8=good.clone();utf8[104]=(byte)255;invalid(()->MCH_VehicleVisualSnapshot.decode(utf8));
        byte[] excessive=good.clone();excessive[103]=(byte)255;invalid(()->MCH_VehicleVisualSnapshot.decode(excessive));
    }
    @Test public void invalidAssetsAndNonfiniteAnimationFailAtBoundary() {
        for(String name:new String[]{"", "../tank", "a/b", "a\\b", "a\nb",new String(new char[97]).replace('\0','a')})
            invalid(()->sample(name,"texture",new float[12]));
        float[] state=new float[12];state[5]=Float.NaN;invalid(()->sample("type","texture",state));
        state[5]=0;state[8]=-1;invalid(()->sample("type","texture",state));
        state[8]=0;state[11]=Float.POSITIVE_INFINITY;invalid(()->sample("type","texture",state));
    }
    @Test public void serverCaptureAvoidsClientMethodsWorldSimulationAndChunkGeneration() throws Exception {
        assertNull(MCH_ExternalVehiclePresentation.capture(null));
        try(InputStream source=MCH_ExternalVehiclePresentation.class.getResourceAsStream("MCH_ExternalVehiclePresentation.class")) {
            assertNotNull(source);
            new ClassReader(source).accept(new ClassVisitor(Opcodes.ASM5) {
                public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions) {
                    return new MethodVisitor(Opcodes.ASM5) {
                        public void visitMethodInsn(int opcode,String owner,String method,String descriptor,boolean iface) {
                            assertFalse(owner.startsWith("net/minecraft/client/"));
                            assertNotEquals("getBrightnessForRender",method);assertNotEquals("getLightBrightnessForSkyBlocks",method);
                            assertNotEquals("getChunkFromChunkCoords",method);assertNotEquals("provideChunk",method);
                            assertNotEquals("createInfo",method);assertNotEquals("onUpdate",method);assertNotEquals("spawnEntityInWorld",method);
                        }
                    };
                }
            },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
        }
    }
}
