package mcheli.render;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MCH_CompressedDepthProjectionTest {
    @Test public void sharedVersionOneVectors() {
        double[] physical = {112.0D, 250.0D, 500.0D, 1000.0D, 2000.0D, 4096.0D};
        double[] projected = {112.0D, 116.28081646410587D, 116.95484674105806D,
                117.62887701801027D, 118.30290729496247D, 119.0D};
        double[] depthFraction = {0.0D, 0.22847233441537074D, 0.38253639795612887D,
                0.5366004612594893D, 0.6906645245628498D, 0.85D};
        for (int i = 0; i < physical.length; i++)
            assertEquals(projected[i], MCH_CompressedDepthProjection.projected(physical[i], 112.0D), 1.0E-10D);
        for (int i = 0; i < physical.length; i++)
            assertEquals(depthFraction[i], MCH_CompressedDepthProjection.depthFraction(physical[i], 112.0D), 1.0E-10D);
    }

    @Test public void orderingAndTransition() {
        for (double start : new double[] {48.0D, 112.0D, 176.0D}) {
            assertEquals(start, MCH_CompressedDepthProjection.projected(start, start), 0.0D);
            double previous = start;
            double previousDepth = 0.0D;
            for (int distance = (int)start + 1; distance <= 4096; distance++) {
                double current = MCH_CompressedDepthProjection.projected(distance, start);
                double currentDepth = MCH_CompressedDepthProjection.depthFraction(distance, start);
                assertTrue(current > previous);
                assertTrue(current <= start + Math.min(8.0D, start / 16.0D));
                assertTrue(currentDepth > previousDepth);
                assertTrue(currentDepth <= 0.85D + 1.0E-12D);
                previous = current;
                previousDepth = currentDepth;
            }
        }
    }

    @Test public void specifiedPhysicalPairsKeepTheirOrder() {
        double[][] pairs = {{450.0D, 500.0D}, {495.0D, 500.0D},
                {500.0D, 505.0D}, {1000.0D, 2000.0D}, {3000.0D, 4000.0D}};
        for (double[] pair : pairs) {
            assertTrue(MCH_CompressedDepthProjection.projected(pair[0], 112.0D)
                    < MCH_CompressedDepthProjection.projected(pair[1], 112.0D));
        }
    }
}
