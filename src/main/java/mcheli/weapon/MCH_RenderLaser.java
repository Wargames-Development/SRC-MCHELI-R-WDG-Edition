package mcheli.weapon;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class MCH_RenderLaser {

    private static final List<Beam> beams = new LinkedList<>();

    public MCH_RenderLaser() {
    }

    public static void addBeam(Vec3 start, Vec3 end, int argb, float width, int lifeTicks, boolean pulsate, double renderStartDist) {
        float a = ((argb >>> 24) & 0xFF) / 255.0f;
        float r = ((argb >>> 16) & 0xFF) / 255.0f;
        float g = ((argb >>> 8) & 0xFF) / 255.0f;
        float b = ((argb) & 0xFF) / 255.0f;
        beams.add(new Beam(
            Vec3.createVectorHelper(start.xCoord, start.yCoord, start.zCoord),
            Vec3.createVectorHelper(end.xCoord, end.yCoord, end.zCoord),
            r, g, b, a, width, pulsate, lifeTicks, renderStartDist
        ));
    }

    /**
     * tick
     */
    public static void tickBeams() {
        Iterator<Beam> it = beams.iterator();
        while (it.hasNext()) {
            Beam b = it.next();
            b.lifeTicks--;
            if (b.lifeTicks <= 0) it.remove();
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent evt) {
        if (beams.isEmpty()) return;

        Minecraft mc = Minecraft.getMinecraft();
        Entity viewerEnt = mc.renderViewEntity != null ? mc.renderViewEntity : mc.thePlayer;
        if (viewerEnt == null) return;

        final float pt = evt.partialTicks;

        final double camX = viewerEnt.lastTickPosX + (viewerEnt.posX - viewerEnt.lastTickPosX) * pt;
        final double camY = viewerEnt.lastTickPosY + (viewerEnt.posY - viewerEnt.lastTickPosY) * pt;
        final double camZ = viewerEnt.lastTickPosZ + (viewerEnt.posZ - viewerEnt.lastTickPosZ) * pt;

        final Tessellator tes = Tessellator.instance;
        final long wt = viewerEnt.worldObj.getTotalWorldTime();

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        GL11.glTranslated(-camX, -camY, -camZ);

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glDepthMask(false);

        GL11.glDisable(GL11.GL_DEPTH_TEST);

        mc.entityRenderer.disableLightmap(pt);

        for (Beam b : beams) {
            renderBeam(b, tes, wt, pt);
        }

        mc.entityRenderer.enableLightmap(pt);

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LIGHTING);

        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    private void renderBeam(Beam b, Tessellator tes, long worldTime, float partialTicks) {
        final Vec3 s = b.start;
        final Vec3 e = b.end;

        double dx = e.xCoord - s.xCoord;
        double dy = e.yCoord - s.yCoord;
        double dz = e.zCoord - s.zCoord;

        double lengthSq = dx * dx + dy * dy + dz * dz;
        if (lengthSq < 1.0E-8) return;
        double length = Math.sqrt(lengthSq);
        Vec3 adjustedStart = s;
        if (b.renderStartDist > 0) {
            double startDist = Math.min(b.renderStartDist, length);
            if (startDist > 0) {
                double ratio = startDist / length;
                adjustedStart = Vec3.createVectorHelper(
                    s.xCoord + dx * ratio,
                    s.yCoord + dy * ratio,
                    s.zCoord + dz * ratio
                );
                dx = e.xCoord - adjustedStart.xCoord;
                dy = e.yCoord - adjustedStart.yCoord;
                dz = e.zCoord - adjustedStart.zCoord;
                lengthSq = dx * dx + dy * dy + dz * dz;
                length = Math.sqrt(lengthSq);
                if (length < 1.0E-8) return;
            }
        }
        float width = b.width;
        if (b.pulsate) {
            double pulse = 0.7 + 0.3 * Math.sin((worldTime + partialTicks) * 0.4);
            width *= (float) pulse;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityLivingBase viewer = mc.renderViewEntity;
        if (viewer == null) return;
        Vec3 lookVec = viewer.getLook(partialTicks);
        double dirX = dx / length;
        double dirY = dy / length;
        double dirZ = dz / length;
        double crossX = dirY * lookVec.zCoord - dirZ * lookVec.yCoord;
        double crossY = dirZ * lookVec.xCoord - dirX * lookVec.zCoord;
        double crossZ = dirX * lookVec.yCoord - dirY * lookVec.xCoord;
        double crossLength = Math.sqrt(crossX * crossX + crossY * crossY + crossZ * crossZ);
        if (crossLength < 1.0E-8) {
            crossX = -dirZ;
            crossY = 0;
            crossZ = dirX;
            crossLength = Math.sqrt(crossX * crossX + crossY * crossY + crossZ * crossZ);
            if (crossLength < 1.0E-8) return;
        }
        crossX /= crossLength;
        crossY /= crossLength;
        crossZ /= crossLength;
        double halfWidth = width * 0.5;
        double hwX = crossX * halfWidth;
        double hwY = crossY * halfWidth;
        double hwZ = crossZ * halfWidth;
        double x1 = adjustedStart.xCoord, y1 = adjustedStart.yCoord, z1 = adjustedStart.zCoord;
        double x2 = e.xCoord, y2 = e.yCoord, z2 = e.zCoord;
        tes.startDrawingQuads();
        tes.setColorRGBA_F(
            Math.min(b.r * 1.5f, 1.0f),
            Math.min(b.g * 1.5f, 1.0f),
            Math.min(b.b * 1.5f, 1.0f),
            b.a * 0.4f
        );
        double glowHwX = hwX * 2.0;
        double glowHwY = hwY * 2.0;
        double glowHwZ = hwZ * 2.0;
        tes.addVertex(x1 - glowHwX, y1 - glowHwY, z1 - glowHwZ);
        tes.addVertex(x1 + glowHwX, y1 + glowHwY, z1 + glowHwZ);
        tes.addVertex(x2 + glowHwX, y2 + glowHwY, z2 + glowHwZ);
        tes.addVertex(x2 - glowHwX, y2 - glowHwY, z2 - glowHwZ);
        tes.draw();
        tes.startDrawingQuads();
        tes.setColorRGBA_F(b.r, b.g, b.b, b.a);
        tes.addVertex(x1 - hwX, y1 - hwY, z1 - hwZ);
        tes.addVertex(x1 + hwX, y1 + hwY, z1 + hwZ);
        tes.addVertex(x2 + hwX, y2 + hwY, z2 + hwZ);
        tes.addVertex(x2 - hwX, y2 - hwY, z2 - hwZ);
        tes.draw();
        if (adjustedStart != s) {
            adjustedStart.xCoord = adjustedStart.yCoord = adjustedStart.zCoord = 0;
        }
    }

    public static class Beam {
        public final Vec3 start;     // 激光起始坐标
        public final Vec3 end;       // 激光结束坐标
        public final float r, g, b;  // 0..1
        public final float a;        // 0..1
        public final float width;    // 基础宽度（方块单位）
        public final boolean pulsate; // 是否脉动
        public int lifeTicks;        // 剩余生存 tick
        public double renderStartDist; // 从起点开始渲染的距离

        public Beam(Vec3 start, Vec3 end, float r, float g, float b, float a, float width, boolean pulsate, int lifeTicks, double renderStartDist) {
            this.start = start;
            this.end = end;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.width = width;
            this.pulsate = pulsate;
            this.lifeTicks = lifeTicks;
            this.renderStartDist = renderStartDist;
        }
    }
}
