package mcheli.render;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MCH_RenderFarVehicleProjectionTest {
    @Test
    public void incompleteGpuCoverageKeepsNormalDepthThroughChunkOverlap() {
        assertEquals(176.0D, MCH_RenderFarVehicle.cpuProjectedDistance(176.0D, 176.0D, 192.0D), 0.0D);
        assertEquals(183.0D, MCH_RenderFarVehicle.cpuProjectedDistance(183.0D, 176.0D, 192.0D), 0.0D);
        assertEquals(192.0D, MCH_RenderFarVehicle.cpuProjectedDistance(192.0D, 176.0D, 192.0D), 0.0D);
        assertEquals(176.0D, MCH_RenderFarVehicle.cpuProjectedDistance(193.0D, 176.0D, 192.0D), 0.0D);
    }
}
