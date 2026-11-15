package mcheli.render;

import mcheli.MCH_EntityInfo;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_RenderAircraft;
import mcheli.plane.MCP_PlaneInfo;
import mcheli.tank.MCH_EntityTank;
import mcheli.tank.MCH_TankInfo;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponMachineGun2;
import mcheli.weapon.MCH_WeaponSet;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.model.IModelCustom;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

/** Real off-screen GL checks; uses the same optional native directory as model-cache tests. */
public class MCH_FarVehicleDepthLayerLightingTest {
    private static Pbuffer context;
    private final FloatBuffer modelview = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer projection = BufferUtils.createFloatBuffer(16);
    private int framebuffer, color, auxiliary, inheritedProgram;

    @BeforeClass public static void createContext() throws Exception {
        String natives = System.getenv("MCHR_LWJGL_NATIVES");
        assumeNotNull(natives);
        System.setProperty("org.lwjgl.librarypath", natives);
        context = new Pbuffer(64, 64, new PixelFormat(), null, null);
        context.makeCurrent();
        assumeTrue(GLContext.getCapabilities().OpenGL30);
    }

    @AfterClass public static void destroyContext() {
        if (context != null) context.destroy();
    }

    @Before public void prepareScene() {
        framebuffer = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        color = texture();
        auxiliary = texture();
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, color, 0);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT1, GL11.GL_TEXTURE_2D, auxiliary, 0);
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
        GL11.glClearColor(0, 0, 0, 1);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT1);
        GL11.glClearColor(0, 0, 1, 1);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        IntBuffer outputs = BufferUtils.createIntBuffer(2);
        outputs.put(GL30.GL_COLOR_ATTACHMENT0).put(GL30.GL_COLOR_ATTACHMENT1).flip();
        GL20.glDrawBuffers(outputs);
        assertEquals(GL30.GL_FRAMEBUFFER_COMPLETE, GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER));
        GL11.glViewport(0, 0, 64, 64);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glFrustum(-0.1, 0.1, -0.1, 0.1, 0.1, 2000);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelview);
        inheritedProgram = GL20.glCreateProgram();
        int vertex = shader(GL20.GL_VERTEX_SHADER, "#version 120\nvoid main(){gl_Position=ftransform();}");
        int fragment = shader(GL20.GL_FRAGMENT_SHADER, "#version 120\nvoid main(){gl_FragColor=vec4(1,0,0,1);}");
        GL20.glAttachShader(inheritedProgram, vertex);
        GL20.glAttachShader(inheritedProgram, fragment);
        GL20.glLinkProgram(inheritedProgram);
        GL20.glDeleteShader(vertex);
        GL20.glDeleteShader(fragment);
        assertEquals(GL11.GL_TRUE, GL20.glGetProgrami(inheritedProgram, GL20.GL_LINK_STATUS));
    }

    @After public void clearScene() {
        GL20.glUseProgram(0);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL30.glDeleteFramebuffers(framebuffer);
        GL11.glDeleteTextures(color);
        GL11.glDeleteTextures(auxiliary);
        GL20.glDeleteProgram(inheritedProgram);
        assertEquals(GL11.GL_NO_ERROR, GL11.glGetError());
    }

    @Test public void inheritedShaderCannotTintLodOrOverwriteShaderPackAuxiliaryTargets() {
        GL20.glUseProgram(inheritedProgram);
        drawLayer(true);
        assertEquals(inheritedProgram, GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM));
        assertEquals(framebuffer, GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING));
        assertEquals(GL30.GL_COLOR_ATTACHMENT0, GL11.glGetInteger(GL20.GL_DRAW_BUFFER0));
        assertEquals(GL30.GL_COLOR_ATTACHMENT1, GL11.glGetInteger(GL20.GL_DRAW_BUFFER1));
        assertPixel(GL30.GL_COLOR_ATTACHMENT0, 0, 255, 0);
        assertPixel(GL30.GL_COLOR_ATTACHMENT1, 0, 0, 255);
    }

    @Test public void sameLitColorSurvivesCompositingWithoutShaderPack() {
        GL20.glUseProgram(0);
        drawLayer(true);
        assertEquals(0, GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM));
        assertPixel(GL30.GL_COLOR_ATTACHMENT0, 0, 255, 0);
        assertPixel(GL30.GL_COLOR_ATTACHMENT1, 0, 0, 255);
    }

    @Test public void abortedLayerRestoresProgramAndDoesNotTouchSceneColors() {
        GL20.glUseProgram(inheritedProgram);
        drawLayer(false);
        assertEquals(inheritedProgram, GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM));
        assertPixel(GL30.GL_COLOR_ATTACHMENT0, 0, 0, 0);
        assertPixel(GL30.GL_COLOR_ATTACHMENT1, 0, 0, 255);
    }

    private void drawLayer(boolean completed) {
        MCH_FarVehicleDepthLayer layer = new MCH_FarVehicleDepthLayer();
        assertTrue(layer.begin(0, 0, -500, new MCP_PlaneInfo("lod_lighting"),
                0, 0, 0, 112, modelview, projection));
        assertEquals("The model must not inherit a shader-pack program", 0, GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM));
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glColor4f(0, 1, 0, 1);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3f(-20, -20, -120);
        GL11.glVertex3f(20, -20, -120);
        GL11.glVertex3f(20, 20, -120);
        GL11.glVertex3f(-20, 20, -120);
        GL11.glEnd();
        layer.finish(completed, 0, 1, projection.get(10), projection.get(14));
    }

    @Test public void bomberCaptureContainsTailAndNoseAcrossHandoff() {
        MCP_PlaneInfo bomber = new MCP_PlaneInfo("b1b_capture");
        bomber.bbZmin = -53; bomber.bbZmax = 12;
        for (double distance : new double[] {112.01, 128, 150, 160, 750}) {
            MCH_FarVehicleDepthLayer layer = new MCH_FarVehicleDepthLayer();
            assertTrue("B1b distance " + distance, layer.begin(0, 0, -distance, bomber,
                    0, 0, 0, 112, modelview, projection));
            try {
                FloatBuffer local = BufferUtils.createFloatBuffer(16);
                GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, local);
                double scale = MCH_CompressedDepthProjection.projected(distance, 112) / distance;
                for (double offset : new double[] {-53, 12, -106, 106}) {
                    double eye = (-distance + offset) * scale;
                    double clip = (local.get(10) * eye + local.get(14)) / -eye;
                    assertTrue("Animated envelope clipped: " + distance + "/" + offset,
                            clip >= -1.00001 && clip <= 1.00001);
                }
            } finally { layer.finish(false, 0, 1, projection.get(10), projection.get(14)); }
        }
    }

    @Test public void detachedTankKeepsTrackedTurretAndBarrelTransforms() throws Exception {
        MCH_TankInfo info = new MCH_TankInfo("tank_handoff");
        info.turretPosition = Vec3.createVectorHelper(0, 2, 1);
        MCH_AircraftInfo.WeaponSet definition = info.new WeaponSet("test_gun");
        info.weaponSetList.add(definition);
        String[] names = { "test_gun" };
        MCH_AircraftInfo.PartWeapon part = info.new PartWeapon(names, false, false, false, true, false,
            0, 2, 2, "weapon0", 0, 0, 0, 0, true);
        MCH_AircraftInfo.PartWeaponChild child = info.new PartWeaponChild(names, false, true,
            0, 2, 3, "weapon0_0", 0, 0, 0, 0);
        info.partWeapon.add(part);
        part.child.add(child);
        MatrixProbe turret = new MatrixProbe();
        MatrixProbe barrel = new MatrixProbe();
        part.model = turret;
        child.model = barrel;

        Field singleton = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        singleton.setAccessible(true);
        sun.misc.Unsafe allocator = (sun.misc.Unsafe)singleton.get(null);
        MCH_EntityTank tank = (MCH_EntityTank)allocator.allocateInstance(MCH_EntityTank.class);
        WorldClient world = (WorldClient)allocator.allocateInstance(WorldClient.class);
        MCH_WeaponInfo weaponInfo = new MCH_WeaponInfo("test_gun");
        MCH_WeaponSet weapon = new MCH_WeaponSet(new MCH_WeaponMachineGun2(world,
            Vec3.createVectorHelper(0, 0, 0), 0, 0, "test_gun", weaponInfo));
        weapon.prevRotationYaw = weapon.rotationYaw = 92;
        weapon.prevRotationPitch = weapon.rotationPitch = -14;
        weapon.defaultRotationYaw = 90;
        weapon.rotationTurretYaw = 35;
        tank.weapons = new MCH_WeaponSet[] { weapon };
        MCH_RenderAircraft.renderWeapon(tank, info, 1.0F);
        float[] nativeTurret = turret.values;
        float[] nativeBarrel = barrel.values;

        MCH_EntityInfo contact = new MCH_EntityInfo(10, "world", info.name, tank.getClass().getName(), 0, 0, 0, 0, 0, 0);
        Method capture = MCH_EntityInfo.class.getDeclaredMethod("captureTankWeaponAim", MCH_WeaponSet[].class, int.class);
        capture.setAccessible(true);
        contact.tankWeaponAim = (MCH_EntityInfo.WeaponAim[])capture.invoke(null, tank.weapons, info.getWeaponNum());

        Class<?> poseType = Class.forName(MCH_RenderFarVehicle.class.getName() + "$SmoothedPose");
        Constructor<?> constructor = poseType.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object pose = constructor.newInstance();
        Method observe = poseType.getDeclaredMethod("observe", MCH_EntityInfo.class, long.class);
        observe.setAccessible(true);
        observe.invoke(pose, contact, contact.lastUpdateTime);
        Method update = poseType.getDeclaredMethod("update", MCH_EntityInfo.class, long.class, int.class);
        update.setAccessible(true);
        update.invoke(pose, contact, contact.lastUpdateTime, 1);
        Method render = MCH_RenderFarVehicle.class.getDeclaredMethod("renderWeaponParts", MCH_AircraftInfo.class, poseType);
        render.setAccessible(true);
        // The fixture calls the model draw directly, without registering an event/collection callback.
        MCH_RenderFarVehicle renderer = (MCH_RenderFarVehicle)allocator.allocateInstance(MCH_RenderFarVehicle.class);
        render.invoke(renderer, info, pose);
        assertArrayEquals(nativeTurret, turret.values, 0.00001F);
        assertArrayEquals(nativeBarrel, barrel.values, 0.00001F);

        // A loaded client copy can retain a different aim after dismounting.
        // Both representations must sample the server pose, including on repeated crossings.
        java.util.UUID identity = java.util.UUID.randomUUID();
        Field uuid = net.minecraft.entity.Entity.class.getDeclaredField("entityUniqueID");
        uuid.setAccessible(true);
        uuid.set(tank, identity);
        contact.aircraftUuid = identity;
        weapon.rotationYaw = 31;
        weapon.prevRotationYaw = 29;
        weapon.rotationPitch = 3;
        weapon.prevRotationPitch = 2;
        weapon.rotationTurretYaw = 12;
        tank.lastRiderYaw = 80;
        tank.prevLastRiderYaw = 79;
        tank.lastRiderPitch = 7;
        tank.prevLastRiderPitch = 6;
        Method begin = poseType.getDeclaredMethod("beginTankAim", mcheli.aircraft.MCH_EntityAircraft.class);
        Method end = poseType.getDeclaredMethod("endTankAim", mcheli.aircraft.MCH_EntityAircraft.class);
        begin.setAccessible(true);
        end.setAccessible(true);
        for (int crossing = 0; crossing < 8; ++crossing) {
            assertEquals(Boolean.TRUE, begin.invoke(pose, tank));
            try {
                MCH_RenderAircraft.renderWeapon(tank, info, 0.5F);
                assertArrayEquals(nativeTurret, turret.values, 0.00001F);
                assertArrayEquals(nativeBarrel, barrel.values, 0.00001F);
                if (crossing % 2 == 0) throw new IllegalStateException("part failure");
            } catch (IllegalStateException expected) {
                assertEquals("part failure", expected.getMessage());
            } finally {
                end.invoke(pose, tank);
            }
            assertEquals(31, weapon.rotationYaw, 0);
            assertEquals(29, weapon.prevRotationYaw, 0);
            assertEquals(3, weapon.rotationPitch, 0);
            assertEquals(2, weapon.prevRotationPitch, 0);
            assertEquals(12, weapon.rotationTurretYaw, 0);
            assertEquals(80, tank.lastRiderYaw, 0);
            assertEquals(79, tank.prevLastRiderYaw, 0);
            assertEquals(7, tank.lastRiderPitch, 0);
            assertEquals(6, tank.prevLastRiderPitch, 0);
            render.invoke(renderer, info, pose);
            assertArrayEquals(nativeTurret, turret.values, 0.00001F);
            assertArrayEquals(nativeBarrel, barrel.values, 0.00001F);
        }
        contact.aircraftUuid = java.util.UUID.randomUUID();
        assertEquals(Boolean.FALSE, begin.invoke(pose, tank));
        contact.tankWeaponAim = null; // A server with the old snapshot sends only the neutral pilot aim.
        assertEquals(Boolean.FALSE, begin.invoke(pose, tank));
        render.invoke(renderer, info, pose);
        assertNotEquals(nativeTurret[2], turret.values[2], 0.00001F);
        assertNotEquals(nativeBarrel[4], barrel.values[4], 0.00001F);
    }

    private static final class MatrixProbe implements IModelCustom {
        private float[] values;
        @Override public String getType() { return "matrix-probe"; }
        @Override public void renderAll() {
            FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, matrix);
            values = new float[16];
            matrix.get(values);
        }
        @Override public void renderOnly(String... parts) { renderAll(); }
        @Override public void renderPart(String part) { renderAll(); }
        @Override public void renderAllExcept(String... parts) { renderAll(); }
    }

    private static void assertPixel(int attachment, int red, int green, int blue) {
        GL11.glReadBuffer(attachment);
        ByteBuffer pixel = BufferUtils.createByteBuffer(4);
        GL11.glReadPixels(32, 32, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
        assertEquals(red, pixel.get(0) & 255);
        assertEquals(green, pixel.get(1) & 255);
        assertEquals(blue, pixel.get(2) & 255);
    }

    private static int texture() {
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, 64, 64, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer)null);
        return texture;
    }

    private static int shader(int kind, String source) {
        int shader = GL20.glCreateShader(kind);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        assertEquals(GL20.glGetShaderInfoLog(shader, 1024), GL11.GL_TRUE,
                GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS));
        return shader;
    }
}
