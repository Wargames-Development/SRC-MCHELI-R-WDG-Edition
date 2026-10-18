package mcheli.mob;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;

import static org.junit.Assert.*;

public class MCH_GunnerServerSafetyTest {
    @Test public void commonGunnerDoesNotCallClientLookMethodOrClientClasses() throws Exception {
        try (InputStream source = MCH_EntityGunner.class.getResourceAsStream("MCH_EntityGunner.class")) {
            assertNotNull(source);
            new ClassReader(source).accept(new ClassVisitor(Opcodes.ASM5) {
                @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    return new MethodVisitor(Opcodes.ASM5) {
                        @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                            assertFalse("Client class on gunner server path: " + owner, owner.startsWith("net/minecraft/client/"));
                            assertNotEquals("setAngles", name);
                            assertNotEquals("func_70082_c", name);
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
    }

    @Test public void optionalBoundaryCanBeLinkedAndGunnerTeamUsesPersistedIdentity() throws Exception {
        assertNotNull(Class.forName("mcheli.wgc.Integrations", false, getClass().getClassLoader()));
        MCH_EntityGunner gunner = new MCH_EntityGunner(null);
        gunner.setTeamName("wge_t1");
        assertEquals("wge_t1", MCH_GunnerTeams.teamOf(null, gunner));
        assertEquals("", MCH_GunnerTeams.teamOf(null, null));
    }
}
