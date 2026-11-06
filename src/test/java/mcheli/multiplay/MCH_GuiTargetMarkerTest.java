package mcheli.multiplay;

import mcheli.MCH_MarkEntityPos;
import org.junit.After;
import org.junit.Test;
import org.lwjgl.BufferUtils;

import java.lang.reflect.Field;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashSet;

import static org.junit.Assert.*;

public class MCH_GuiTargetMarkerTest {
    @After
    public void clearMarkers() {
        MCH_GuiTargetMarker.clearMarkEntityPos();
    }

    @Test
    public void modelCallbacksCannotClaimAnEntityBeforeOrAfterWorldPass() throws Exception {
        MCH_GuiTargetMarker.addMarkEntityPos(2, null, 900, 900, 900);
        assertTrue(positions().isEmpty());
        assertTrue(markedIds().isEmpty());

        MCH_GuiTargetMarker.beginEntityMarkerPass();
        MCH_GuiTargetMarker.endEntityMarkerPass();
        MCH_GuiTargetMarker.addMarkEntityPos(2, null, -900, -900, -900);
        assertTrue(positions().isEmpty());
        assertTrue(markedIds().isEmpty());
    }

    @Test
    public void worldPassRebuildsEntitiesWithoutDiscardingPointsOrReplacingCameraCollections() throws Exception {
        ArrayList<MCH_MarkEntityPos> positions = positions();
        HashSet<Integer> ids = markedIds();
        MCH_MarkEntityPos point = new MCH_MarkEntityPos(MCH_TargetType.POINT.ordinal());
        positions.add(new MCH_MarkEntityPos(MCH_TargetType.SAME_TEAM_PLAYER.ordinal()));
        positions.add(point);
        ids.add(42);

        MCH_GuiTargetMarker.beginEntityMarkerPass();

        assertSame(positions, positions());
        assertSame(ids, markedIds());
        assertEquals(1, positions.size());
        assertSame(point, positions.get(0));
        assertTrue(ids.isEmpty());
    }

    @Test
    public void secondaryCameraRebuildDoesNotClearPrimaryMarkers() throws Exception {
        ArrayList<MCH_MarkEntityPos> primary = positions();
        HashSet<Integer> primaryIds = markedIds();
        primary.add(new MCH_MarkEntityPos(MCH_TargetType.SAME_TEAM_PLAYER.ordinal()));
        primaryIds.add(42);
        Field positionsField = MCH_GuiTargetMarker.class.getDeclaredField("entityPos");
        Field idsField = MCH_GuiTargetMarker.class.getDeclaredField("markedEntityIds");
        positionsField.setAccessible(true);
        idsField.setAccessible(true);
        try {
            positionsField.set(null, new ArrayList<MCH_MarkEntityPos>());
            idsField.set(null, new HashSet<Integer>());
            MCH_GuiTargetMarker.beginEntityMarkerPass();
            assertTrue(positions().isEmpty());
            assertTrue(markedIds().isEmpty());
            assertEquals(1, primary.size());
            assertTrue(primaryIds.contains(42));
        } finally {
            MCH_GuiTargetMarker.endEntityMarkerPass();
            positionsField.set(null, primary);
            idsField.set(null, primaryIds);
        }
    }

    @Test
    public void headAnchorTracksPositionInTheWorldCamera() {
        MCH_MarkEntityPos marker = project(10, 2.3, -40, 2560, 1440, 1);
        assertEquals(1460, marker.pos.get(0), 0.001);
        assertEquals(761.4, marker.pos.get(1), 0.001);

        MCH_MarkEntityPos moved = project(12, 3.3, -40, 2560, 1440, 1);
        assertEquals(36, moved.pos.get(0) - marker.pos.get(0), 0.001);
        assertEquals(18, moved.pos.get(1) - marker.pos.get(1), 0.001);
    }

    @Test
    public void scopeUsesItsOwnViewportAndFieldOfView() {
        MCH_MarkEntityPos primary = project(10, 2, -40, 2560, 1440, 1);
        MCH_MarkEntityPos scope = project(10, 2, -40, 1024, 768, 2);
        assertEquals(1460, primary.pos.get(0), 0.001);
        assertEquals(704, scope.pos.get(0), 0.001);
        assertEquals(422.4, scope.pos.get(1), 0.001);
        assertEquals(1460, primary.pos.get(0), 0.001);
    }

    @Test
    public void farPlaneDoesNotHideForwardMarkersButBehindCameraAndInvalidPointsStayRejected() {
        MCH_MarkEntityPos distant = project(10, 2, -4096, 2560, 1440, 1);
        assertTrue(distant.pos.get(2) >= 0 && distant.pos.get(2) < 1);
        MCH_MarkEntityPos behind = project(10, 2, 40, 2560, 1440, 1);
        assertTrue(behind.pos.get(2) >= 1);
        assertFalse(projectInto(Double.NaN, 2, -40, 2560, 1440, 1, distant));
        assertFalse(projectInto(10, 2, 0, 2560, 1440, 1, distant));
    }

    private MCH_MarkEntityPos project(double x, double y, double z, int width, int height, float zoom) {
        MCH_MarkEntityPos marker = new MCH_MarkEntityPos(MCH_TargetType.SAME_TEAM_PLAYER.ordinal());
        assertTrue(projectInto(x, y, z, width, height, zoom, marker));
        return marker;
    }

    private boolean projectInto(double x, double y, double z, int width, int height, float zoom,
                                MCH_MarkEntityPos marker) {
        FloatBuffer model = BufferUtils.createFloatBuffer(16);
        model.put(0, 1).put(5, 1).put(10, 1).put(15, 1);
        FloatBuffer projection = BufferUtils.createFloatBuffer(16);
        projection.put(0, zoom * height / width).put(5, zoom);
        projection.put(10, -129.0F / 127.0F).put(11, -1).put(14, -256.0F / 127.0F);
        IntBuffer viewport = BufferUtils.createIntBuffer(4);
        viewport.put(2, width).put(3, height);
        return MCH_GuiTargetMarker.projectMarker(x, y, z, marker, false, model, projection, viewport);
    }

    @SuppressWarnings("unchecked")
    private ArrayList<MCH_MarkEntityPos> positions() throws Exception {
        Field field = MCH_GuiTargetMarker.class.getDeclaredField("entityPos");
        field.setAccessible(true);
        return (ArrayList<MCH_MarkEntityPos>) field.get(null);
    }

    @SuppressWarnings("unchecked")
    private HashSet<Integer> markedIds() throws Exception {
        Field field = MCH_GuiTargetMarker.class.getDeclaredField("markedEntityIds");
        field.setAccessible(true);
        return (HashSet<Integer>) field.get(null);
    }
}
