package mcheli.render;

import mcheli.MCH_EntityInfo;
import mcheli.plane.MCP_PlaneInfo;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import org.junit.Test;

import static org.junit.Assert.*;

public class MCH_RadarSearchTest {
    @Test public void ordinaryAirframeHoldsItsContactThroughTheNextSweep() {
        MCP_PlaneInfo aircraft = new MCP_PlaneInfo("ordinary_test_plane");
        boolean fullSignature = MCH_RenderRWR.hasFullAirframeRadarSignature(aircraft);
        assertTrue(fullSignature);
        assertEquals(41, MCH_RenderRWR.computeSearchContactHoldTick(10, 1.0F, 40, fullSignature));
        assertEquals(60, MCH_RenderRWR.computeSearchContactHoldTick(60, 1.0F, 40, fullSignature));
    }

    @Test public void reducedSignaturesRetainTheirConfiguredHoldTime() {
        MCP_PlaneInfo aircraft = new MCP_PlaneInfo("stealth_test_plane");
        aircraft.radarRcsFrontFactor = 0.5F;
        assertFalse(MCH_RenderRWR.hasFullAirframeRadarSignature(aircraft));
        aircraft.radarRcsFrontFactor = 1.0F;
        aircraft.radarRcsTimeFactor = 0.5F;
        assertFalse(MCH_RenderRWR.hasFullAirframeRadarSignature(aircraft));
        assertEquals(5, MCH_RenderRWR.computeSearchContactHoldTick(10, 0.5F, 40, false));
        aircraft.radarRcsTimeFactor = 1.0F;
        aircraft.stealthFactor = 0.5F;
        assertFalse(MCH_RenderRWR.hasFullAirframeRadarSignature(aircraft));
        aircraft.stealthFactor = 1.0F;
        aircraft.stealth = 0.5F;
        assertFalse(MCH_RenderRWR.hasFullAirframeRadarSignature(aircraft));
    }

    @Test public void radarAltitudeUsesServerMeasurementWithoutClientTerrain() {
        MCH_EntityInfo contact = new MCH_EntityInfo(1, "world", "plane", "MCP_EntityPlane", 900, 150, 900, 899, 150, 900);
        assertTrue(Double.isNaN(MCH_RenderRWR.resolveTrackedAgl(null, contact, 900, 150, 900)));
        contact.altitudeAboveGround = 30.0D;
        assertEquals(30.0D, MCH_RenderRWR.resolveTrackedAgl(null, contact, 900, 150, 900), 0.0D);
    }

    @Test public void clientChunkPlaceholderDoesNotProvideAnAltitude() {
        ChunkProviderClient provider = new ChunkProviderClient(null);
        assertTrue(provider.chunkExists(56, 56));
        assertTrue(Double.isNaN(MCH_RenderRWR.resolveClientAgl(provider, 900, 150, 900)));
    }
}
