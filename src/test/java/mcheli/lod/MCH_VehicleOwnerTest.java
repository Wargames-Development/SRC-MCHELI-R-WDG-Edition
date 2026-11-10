package mcheli.lod;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;
import static org.junit.Assert.*;

public class MCH_VehicleOwnerTest {
    private static final class Actor extends Entity {
        Actor() { super(null); }protected void entityInit() { }protected void readEntityFromNBT(NBTTagCompound tag) { }protected void writeEntityToNBT(NBTTagCompound tag) { }
    }
    @Test public void legacyStaysDefaultAndExplicitOwnershipCanBeUnregistered() {
        try {
            MCH_ExternalVehicleRenderer.setOwner(null,null,null);assertFalse(MCH_ExternalVehicleRenderer.ownsLegacy());
            Actor actor=new Actor();MCH_ExternalVehicleRenderer.setOwner(()->false,value->true,value->true);
            assertFalse(MCH_ExternalVehicleRenderer.replaceNative(actor));
            MCH_ExternalVehicleRenderer.setOwner(()->true,value->value==actor,value->false);
            assertTrue(MCH_ExternalVehicleRenderer.ownsLegacy());assertTrue(MCH_ExternalVehicleRenderer.replaceNative(actor));assertFalse(MCH_ExternalVehicleRenderer.replaceNative(null));
            MCH_ExternalVehicleRenderer.setOwner(null,null,null);assertFalse(MCH_ExternalVehicleRenderer.ownsLegacy());
        } finally { MCH_ExternalVehicleRenderer.setOwner(null,null,null); }
    }
    @Test public void modelAdapterDoesNotInvokeWorldSimulationOrNetworkMethods() throws Exception {
        try(java.io.InputStream source=MCH_ExternalVehicleRenderer.class.getResourceAsStream("MCH_ExternalVehicleRenderer$Context.class")) {
            assertNotNull(source);
            new org.objectweb.asm.ClassReader(source).accept(new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM5) {
                public org.objectweb.asm.MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions) {
                    return new org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM5) {
                        public void visitMethodInsn(int opcode,String owner,String method,String descriptor,boolean iface) {
                            for(String forbidden:new String[]{"onUpdate","onUpdateAircraft","updateWeapons","useCurrentWeapon","spawnEntityInWorld","sendToServer","getChunkFromChunkCoords","provideChunk","setDead"}) assertNotEquals(forbidden,method);
                        }
                    };
                }
            },org.objectweb.asm.ClassReader.SKIP_DEBUG|org.objectweb.asm.ClassReader.SKIP_FRAMES);
        }
    }
}
