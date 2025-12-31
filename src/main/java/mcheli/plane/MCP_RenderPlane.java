package mcheli.plane;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_ModelManager;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_RenderAircraft;
import mcheli.wrapper.W_Render;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

@SideOnly(Side.CLIENT)
public class MCP_RenderPlane extends MCH_RenderAircraft {

    private static final String TEX_EXHAUST_DIR = "textures/exhaustflames/";

    public MCP_RenderPlane() {
        super.shadowSize = 2.0F;
    }

    @Override
    public void renderCommonPart(MCH_EntityAircraft ac, MCH_AircraftInfo info, double x, double y, double z, float tickTime) {
        super.renderCommonPart(ac, info, x, y, z, tickTime);
        MCP_PlaneInfo planeInfo;
        if (ac instanceof MCP_EntityPlane) {
            MCP_EntityPlane plane = (MCP_EntityPlane) ac;
            planeInfo = plane.getPlaneInfo();
            if (!planeInfo.exhaustFlames.isEmpty()) {
                this.renderExhaustFlame(plane, planeInfo, tickTime);
            }
        }
    }

    public void renderAircraft(MCH_EntityAircraft entity, double posX, double posY, double posZ, float yaw, float pitch, float roll, float tickTime) {
        MCP_PlaneInfo planeInfo;
        if (entity instanceof MCP_EntityPlane) {
            MCP_EntityPlane plane = (MCP_EntityPlane) entity;
            planeInfo = plane.getPlaneInfo();
            if (planeInfo != null) {
                this.renderDebugHitBox(plane, posX, posY, posZ, yaw, pitch);
                this.renderDebugPilotSeat(plane, posX, posY, posZ, yaw, pitch, roll);
                GL11.glTranslated(posX, posY, posZ);
                GL11.glRotatef(yaw, 0.0F, -1.0F, 0.0F);
                GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(roll, 0.0F, 0.0F, 1.0F);
                this.bindTexture("textures/planes/" + plane.getTextureName() + ".png", plane);
                if (planeInfo.haveNozzle() && plane.partNozzle != null) {
                    this.renderNozzle(plane, planeInfo, tickTime);
                }

                if (planeInfo.haveWing() && plane.partWing != null) {
                    this.renderWing(plane, planeInfo, tickTime);
                }

                if (planeInfo.haveRotor() && plane.partNozzle != null) {
                    this.renderRotor(plane, planeInfo, tickTime);
                }

                renderBody(planeInfo.model);
            }
        }
    }

    public void renderRotor(MCP_EntityPlane plane, MCP_PlaneInfo planeInfo, float tickTime) {
        float rot = plane.getNozzleRotation();
        float prevRot = plane.getPrevNozzleRotation();

        for (Object object : planeInfo.rotorList) {
            MCP_PlaneInfo.Rotor r = (MCP_PlaneInfo.Rotor) object;
            GL11.glPushMatrix();
            GL11.glTranslated(r.pos.xCoord, r.pos.yCoord, r.pos.zCoord);
            GL11.glRotatef((prevRot + (rot - prevRot) * tickTime) * r.maxRotFactor, (float) r.rot.xCoord, (float) r.rot.yCoord, (float) r.rot.zCoord);
            GL11.glTranslated(-r.pos.xCoord, -r.pos.yCoord, -r.pos.zCoord);
            renderPart(r.model, planeInfo.model, r.modelName);

            for (Object o : r.blades) {
                MCP_PlaneInfo.Blade b = (MCP_PlaneInfo.Blade) o;
                float br = plane.prevRotationRotor;
                br += (plane.rotationRotor - plane.prevRotationRotor) * tickTime;
                GL11.glPushMatrix();
                GL11.glTranslated(b.pos.xCoord, b.pos.yCoord, b.pos.zCoord);
                GL11.glRotatef(br, (float) b.rot.xCoord, (float) b.rot.yCoord, (float) b.rot.zCoord);
                GL11.glTranslated(-b.pos.xCoord, -b.pos.yCoord, -b.pos.zCoord);

                for (int i = 0; i < b.numBlade; ++i) {
                    GL11.glTranslated(b.pos.xCoord, b.pos.yCoord, b.pos.zCoord);
                    GL11.glRotatef((float) b.rotBlade, (float) b.rot.xCoord, (float) b.rot.yCoord, (float) b.rot.zCoord);
                    GL11.glTranslated(-b.pos.xCoord, -b.pos.yCoord, -b.pos.zCoord);
                    renderPart(b.model, planeInfo.model, b.modelName);
                }

                GL11.glPopMatrix();
            }

            GL11.glPopMatrix();
        }

    }

    public void renderWing(MCP_EntityPlane plane, MCP_PlaneInfo planeInfo, float tickTime) {
        float rot = plane.getWingRotation();
        float prevRot = plane.getPrevWingRotation();

        for (Iterator i$ = planeInfo.wingList.iterator(); i$.hasNext(); GL11.glPopMatrix()) {
            MCP_PlaneInfo.Wing w = (MCP_PlaneInfo.Wing) i$.next();
            GL11.glPushMatrix();
            GL11.glTranslated(w.pos.xCoord, w.pos.yCoord, w.pos.zCoord);
            GL11.glRotatef((prevRot + (rot - prevRot) * tickTime) * w.maxRotFactor, (float) w.rot.xCoord, (float) w.rot.yCoord, (float) w.rot.zCoord);
            GL11.glTranslated(-w.pos.xCoord, -w.pos.yCoord, -w.pos.zCoord);
            renderPart(w.model, planeInfo.model, w.modelName);
            if (w.pylonList != null) {

                for (Object o : w.pylonList) {
                    MCP_PlaneInfo.Pylon p = (MCP_PlaneInfo.Pylon) o;
                    GL11.glPushMatrix();
                    GL11.glTranslated(p.pos.xCoord, p.pos.yCoord, p.pos.zCoord);
                    GL11.glRotatef((prevRot + (rot - prevRot) * tickTime) * p.maxRotFactor, (float) p.rot.xCoord, (float) p.rot.yCoord, (float) p.rot.zCoord);
                    GL11.glTranslated(-p.pos.xCoord, -p.pos.yCoord, -p.pos.zCoord);
                    renderPart(p.model, planeInfo.model, p.modelName);
                    GL11.glPopMatrix();
                }
            }
        }

    }

    public void renderNozzle(MCP_EntityPlane plane, MCP_PlaneInfo planeInfo, float tickTime) {
        float rot = plane.getNozzleRotation();
        float prevRot = plane.getPrevNozzleRotation();
        for (Object o : planeInfo.nozzles) {
            MCH_AircraftInfo.DrawnPart n = (MCH_AircraftInfo.DrawnPart) o;
            GL11.glPushMatrix();
            GL11.glTranslated(n.pos.xCoord, n.pos.yCoord, n.pos.zCoord);
            GL11.glRotatef(prevRot + (rot - prevRot) * tickTime, (float) n.rot.xCoord, (float) n.rot.yCoord, (float) n.rot.zCoord);
            GL11.glTranslated(-n.pos.xCoord, -n.pos.yCoord, -n.pos.zCoord);
            renderPart(n.model, planeInfo.model, n.modelName);
            GL11.glPopMatrix();
        }

    }

    public void renderExhaustFlame(MCP_EntityPlane plane, MCP_PlaneInfo planeInfo, float tickTime) {
        final int n = planeInfo.exhaustFlames.size();
        if (n == 0) return;

        MCP_EntityPlane.ExhaustAnimState st = plane.exhaustAnimState;
        if (st == null || st.frame.length != n) {
            st = new MCP_EntityPlane.ExhaustAnimState(n);
            plane.exhaustAnimState = st;
        }

        float throttle = (float) plane.getCurrentThrottle();
        throttle = clamp(throttle, 0.0F, 1.0F);
        if(throttle == 0) {
            return;
        }

        if(plane.getVtolMode() != 0) {
            return;
        }

        float yawFactor = lerp(plane.prevRotationExhaustFlameY, plane.rotationExhaustFlameY, tickTime);
        float pitchFactor = lerp(plane.prevRotationExhaustFlameX, plane.rotationExhaustFlameX, tickTime);
        yawFactor = clamp(yawFactor, -1.0F, 1.0F);
        pitchFactor = clamp(pitchFactor, -1.0F, 1.0F);

        float scaleZ = throttle;

        for (int i = 0; i < n; i++) {
            MCP_PlaneInfo.ExhaustFlame ef = planeInfo.exhaustFlames.get(i);
            int delay = ef.delay <= 0 ? 1 : ef.delay;
            int frame = st.frame[i];
            int t = st.tick[i] + 1;

            if (t >= delay) {
                t = 0;
                int next = frame + 1;
                if (next >= MCP_PlaneInfo.exhaustFlameTextureMap.getOrDefault(ef.texturePrefix, 0)) {
                    next = 0;
                }
                frame = next;
            }

            st.frame[i] = frame;
            st.tick[i] = t;

            this.bindTexture(TEX_EXHAUST_DIR + ef.texturePrefix + frame + ".png");

            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            RenderHelper.disableStandardItemLighting();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);

            // 计算旋转角度
            float yawDeg = clamp(yawFactor * ef.degreeYaw, -ef.degreeYaw, ef.degreeYaw);

            GL11.glPushMatrix();

            // 将火焰平移到飞机上的相对位置
            GL11.glTranslated(ef.pos.xCoord, ef.pos.yCoord, ef.pos.zCoord);

            GL11.glRotated(-yawDeg, ef.rot.xCoord, ef.rot.yCoord, ef.rot.zCoord);

            GL11.glScalef(1.0F, 1.0F, scaleZ);

            // 渲染火焰模型
            String flameModelName = (ef.modelName != null && !ef.modelName.isEmpty()) ? ef.modelName : "Exhaustflame";
            MCH_ModelManager.render("exhaustflames", flameModelName);

            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private static float lerp(float prev, float now, float t) {
        return prev + (now - prev) * t;
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : (Math.min(v, max));
    }

    protected ResourceLocation getEntityTexture(Entity entity) {
        return W_Render.TEX_DEFAULT;
    }
}
