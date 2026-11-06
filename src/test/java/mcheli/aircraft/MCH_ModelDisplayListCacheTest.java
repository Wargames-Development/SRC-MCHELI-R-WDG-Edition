package mcheli.aircraft;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import mcheli.wrapper.modelloader.W_WavefrontObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assume.assumeNotNull;

/** Optional real-GL regression checks; set MCHR_LWJGL_NATIVES to LWJGL 2's native directory. */
public class MCH_ModelDisplayListCacheTest {
    private static Pbuffer context;

    @BeforeClass public static void createContext() throws Exception {
        String natives = System.getenv("MCHR_LWJGL_NATIVES");
        assumeNotNull(natives);
        System.setProperty("org.lwjgl.librarypath", natives);
        context = new Pbuffer(32, 32, new PixelFormat(), null, null);
        context.makeCurrent();
    }

    @AfterClass public static void destroyContext() {
        if (context != null) context.destroy();
    }

    @Before public void beginFrame() {
        MCH_RenderAircraft.clearModelDisplayListCache();
        MCH_RenderAircraft.beginRenderFrame();
        GL11.glViewport(0, 0, 32, 32);
        GL11.glClearColor(0, 0, 0, 1);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    }

    @After public void clearCache() {
        MCH_RenderAircraft.clearModelDisplayListCache();
        assertEquals(GL11.GL_NO_ERROR, GL11.glGetError());
    }

    @Test public void deferredBodiesRemainVisibleAndCacheOnFollowingFrames() {
        Model first = new Model(), second = new Model();
        MCH_RenderAircraft.renderBody(first);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        MCH_RenderAircraft.renderBody(second);
        MCH_RenderAircraft.renderBody(second);
        assertEquals(1, first.compilations);
        assertEquals(0, second.compilations);
        assertEquals(2, second.directDraws);
        assertVisible();

        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        MCH_RenderAircraft.beginRenderFrame();
        MCH_RenderAircraft.renderBody(first); // A hit must not spend the next frame's allowance.
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        MCH_RenderAircraft.renderBody(second);
        MCH_RenderAircraft.renderBody(second);
        assertEquals(1, first.compilations);
        assertEquals(1, second.compilations);
        assertEquals(2, second.directDraws);
        assertVisible();
    }

    @Test public void embeddedPartsAndSeparateModelsShareOneAllowance() {
        Model body = new Model(), separate = new Model();
        MCH_RenderAircraft.renderPart(null, body, "first");
        MCH_RenderAircraft.renderPart(null, body, "second");
        MCH_RenderAircraft.renderPart(separate, body, "third");
        assertEquals(1, body.compilations);
        assertEquals(1, body.directDraws);
        assertEquals(0, separate.compilations);
        assertEquals(1, separate.directDraws);

        MCH_RenderAircraft.beginRenderFrame();
        MCH_RenderAircraft.renderPart(null, body, "first");
        MCH_RenderAircraft.renderPart(null, body, "second");
        assertEquals(2, body.compilations);
        assertVisible();
    }

    @Test public void invalidationDeletesListsWithoutOpeningAnotherBatchInSameFrame() {
        Model model = new Model();
        MCH_RenderAircraft.renderBody(model);
        int oldList = model.lastList;
        MCH_RenderAircraft.clearModelDisplayListCache();
        assertFalse(GL11.glIsList(oldList));
        MCH_RenderAircraft.renderBody(model);
        assertEquals(1, model.compilations);
        assertEquals(1, model.directDraws);
        MCH_RenderAircraft.beginRenderFrame();
        MCH_RenderAircraft.renderBody(model);
        assertEquals(2, model.compilations);
        assertVisible();
    }

    private static void assertVisible() {
        ByteBuffer pixel = BufferUtils.createByteBuffer(4);
        GL11.glReadPixels(16, 16, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
        assertEquals(255, pixel.get(0) & 255);
        assertEquals(0, pixel.get(1) & 255);
    }

    private static final class Model extends W_WavefrontObject {
        int compilations, directDraws, lastList;
        Model() { super("test.obj", new ByteArrayInputStream(new byte[0])); }
        @Override public boolean containsPart(String name) { return !"$body".equals(name); }
        @Override public void renderAll() { draw(); }
        @Override public void renderPart(String name) { draw(); }
        private void draw() {
            int list = GL11.glGetInteger(GL11.GL_LIST_INDEX);
            if (list != 0) { ++compilations; lastList = list; }
            else ++directDraws;
            GL11.glColor3f(1, 0, 0);
            GL11.glBegin(GL11.GL_TRIANGLES);
            GL11.glVertex2f(-1, -1);
            GL11.glVertex2f(1, -1);
            GL11.glVertex2f(0, 1);
            GL11.glEnd();
        }
    }
}
