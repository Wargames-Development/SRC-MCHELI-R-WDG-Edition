package mcheli.particles;

import net.minecraft.client.renderer.Tessellator;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MCH_SmokeBatchTest {
    @Test
    public void batchesTheProfiledSmokeLoadInOrderWithPerParticleAttributes() {
        int smokeCount = 3155;
        List<MCH_EntityParticleSmoke> smoke = new ArrayList<MCH_EntityParticleSmoke>(smokeCount + 1);
        for (int i = 0; i < smokeCount; i++) {
            smoke.add(new TestSmoke(i));
        }
        TestSmoke expired = new TestSmoke(-1);
        expired.setDead();
        smoke.add(smokeCount / 2, expired);

        RecordingTessellator tessellator = new RecordingTessellator();
        MCH_EntityParticleSmoke.drawSmokeBatch(tessellator, smoke, null, 0.5F, false);

        assertEquals(1, tessellator.starts);
        assertEquals(1, tessellator.draws);
        assertEquals(smokeCount * 4, tessellator.vertices);
        assertEquals(smokeCount, tessellator.firstVertices.size());
        for (int i = 0; i < smokeCount; i++) {
            Quad quad = tessellator.firstVertices.get(i);
            assertEquals(i, quad.x - tessellator.firstVertices.get(0).x, 0.0D);
            assertEquals((i % 8 + 1) / 8.0D, quad.u, 0.0D);
            assertEquals((i % 16) / 16.0F, quad.red, 0.0F);
            assertEquals(0.4F, quad.alpha, 0.0F);
            assertEquals(0xA00000 | ((i % 16) << 4), quad.brightness);
        }
    }

    @Test
    public void preservesLegacyThermalTintAndSkipsEmptyBatches() {
        TestSmoke smoke = new TestSmoke(0);
        RecordingTessellator tessellator = new RecordingTessellator();
        MCH_EntityParticleSmoke.drawSmokeBatch(tessellator, Collections.<MCH_EntityParticleSmoke>singletonList(smoke),
            null, 0.5F, true);
        Quad quad = tessellator.firstVertices.get(0);
        assertEquals(1, tessellator.draws);
        assertEquals(4, tessellator.vertices);
        assertEquals(1.0F, quad.red, 0.0F);
        assertEquals(0.0F, quad.green, 0.0F);
        assertEquals(1.0F, quad.blue, 0.0F);
        assertEquals(1.0F, quad.alpha, 0.0F);
        assertEquals(0xF000F0, quad.brightness);

        smoke.setDead();
        MCH_EntityParticleSmoke.drawSmokeBatch(tessellator, Collections.<MCH_EntityParticleSmoke>singletonList(smoke),
            null, 0.5F, false);
        assertEquals(1, tessellator.starts);
        assertEquals(1, tessellator.draws);
    }

    private static class TestSmoke extends MCH_EntityParticleSmoke {
        private final int brightness;

        TestSmoke(int index) {
            super(null, index, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
            prevPosX = posX;
            setParticleScale(5.0F);
            setParticleTextureIndex(index % 8);
            setRBGColorF((index % 16) / 16.0F, 0.5F, 0.7F);
            setAlphaF(0.4F);
            brightness = 0xA00000 | ((index % 16) << 4);
            thermalHot = true;
        }

        @Override
        public int getBrightnessForRender(float partialTicks) {
            return brightness;
        }
    }

    private static class RecordingTessellator extends Tessellator {
        final List<Quad> firstVertices = new ArrayList<Quad>();
        int starts, draws, vertices, brightness;
        float red, green, blue, alpha;

        @Override public void startDrawingQuads() { starts++; }
        @Override public int draw() { return ++draws; }
        @Override public void setBrightness(int brightness) { this.brightness = brightness; }

        @Override
        public void setColorRGBA_F(float red, float green, float blue, float alpha) {
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
        }

        @Override
        public void addVertexWithUV(double x, double y, double z, double u, double v) {
            if (vertices++ % 4 == 0) firstVertices.add(new Quad(x, u, red, green, blue, alpha, brightness));
        }
    }

    private static class Quad {
        final double x, u;
        final float red, green, blue, alpha;
        final int brightness;

        Quad(double x, double u, float red, float green, float blue, float alpha, int brightness) {
            this.x = x;
            this.u = u;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
            this.brightness = brightness;
        }
    }
}
