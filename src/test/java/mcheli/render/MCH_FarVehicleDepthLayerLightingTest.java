package mcheli.render;

import mcheli.plane.MCP_PlaneInfo;
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
