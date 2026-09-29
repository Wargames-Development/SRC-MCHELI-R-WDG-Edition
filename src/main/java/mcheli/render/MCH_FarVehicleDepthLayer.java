package mcheli.render;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_AircraftInfo;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/** Keeps a distant model's self-depth separate from the compressed world depth. Render thread only. */
@SideOnly(Side.CLIENT)
final class MCH_FarVehicleDepthLayer {
    private static final double LOCAL_NEAR = 16.0D;
    private static final double LOCAL_FAR = 256.0D;
    private static final int DEPTH_COMPONENT24 = 0x81A6;
    private static final String VERTEX = "#version 120\n"
            + "void main() { gl_Position = ftransform(); gl_TexCoord[0] = gl_MultiTexCoord0; }\n";
    private static final String FRAGMENT = "#version 120\n"
            + "uniform sampler2D colorTex, depthTex;\n"
            + "uniform float localZ, localOffset, worldZ, worldOffset, rangeNear, rangeFar;\n"
            + "void main() {\n"
            + "  vec2 uv = gl_TexCoord[0].xy;\n"
            + "  vec4 color = texture2D(colorTex, uv);\n"
            + "  float d = texture2D(depthTex, uv).r;\n"
            + "  if (d >= 1.0 || color.a <= 0.001) discard;\n"
            + "  float eyeZ = localOffset / (1.0 - 2.0 * d - localZ);\n"
            + "  float normalDepth = 0.5 * ((worldZ * eyeZ + worldOffset) / -eyeZ + 1.0);\n"
            + "  gl_FragDepth = clamp(rangeNear + (rangeFar - rangeNear) * normalDepth, 0.0, 1.0);\n"
            + "  gl_FragColor = color;\n"
            + "}\n";

    private final IntBuffer viewport = BufferUtils.createIntBuffer(4);
    private final FloatBuffer localProjection = BufferUtils.createFloatBuffer(16);
    private int framebuffer, colorTexture, depthTexture, program;
    private int colorSampler, depthSampler, localZUniform, localOffsetUniform;
    private int worldZUniform, worldOffsetUniform, rangeNearUniform, rangeFarUniform;
    private int width, height, previousReadFramebuffer, previousDrawFramebuffer;
    private int previousProgram, previousMatrixMode, previousActiveTexture;
    private int maxTextureSize;
    private int left, bottom, right, top;
    private boolean active, unavailable;

    boolean begin(double worldX, double worldY, double worldZ, MCH_AircraftInfo info,
            double cameraX, double cameraY, double cameraZ, double transitionStart,
            FloatBuffer modelview, FloatBuffer projection) {
        if (unavailable || !GLContext.getCapabilities().OpenGL30
                || !GLContext.getCapabilities().OpenGL20
                || Math.abs(projection.get(11) + 1.0F) > 0.0001F
                || Math.abs(projection.get(15)) > 0.0001F) return false;
        viewport.clear();
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
        int vx = viewport.get(0), vy = viewport.get(1);
        int vw = viewport.get(2), vh = viewport.get(3);
        if (maxTextureSize == 0) maxTextureSize = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
        if (vx != 0 || vy != 0 || vw <= 0 || vh <= 0
                || vw > maxTextureSize || vh > maxTextureSize) return false;
        double dx = worldX - cameraX, dy = worldY - cameraY, dz = worldZ - cameraZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double scale = MCH_CompressedDepthProjection.projected(distance, transitionStart) / distance;
        if (!Double.isFinite(scale) || scale <= 0.0D) return false;
        // Include long barrels, rotors and animated parts. If the box cannot be projected
        // conservatively, the caller retains the existing direct draw.
        double radius = Math.max(64.0D, 2.0D * Math.max(info.bodyWidth,
                Math.max(info.bodyHeight, Math.max(Math.abs(info.bbZmin), Math.abs(info.bbZmax))))) * scale;
        if (radius >= 80.0D || !bounds(dx * scale, dy * scale, dz * scale,
                radius, modelview, projection, vw, vh)) return false;
        previousReadFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        previousDrawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        previousMatrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        previousActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        boolean projectionPushed = false;
        try {
            if (!ensureResources(vw, vh)) {
                restoreFramebuffers();
                GL13.glActiveTexture(previousActiveTexture);
                GL11.glPopAttrib();
                unavailable = true;
                MCH_Lib.Log("[FarVehicle] high-precision depth layer unavailable; using direct depth");
                return false;
            }
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            GL11.glViewport(0, 0, vw, vh);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(left, bottom, right - left, top - bottom);
            GL11.glColorMask(true, true, true, true);
            GL11.glDepthMask(true);
            GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            GL11.glClearDepth(1.0D);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glDepthRange(0.0D, 1.0D);
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            localProjection.clear();
            for (int i = 0; i < 16; i++) localProjection.put(i, projection.get(i));
            localProjection.put(10, (float)(-(LOCAL_FAR + LOCAL_NEAR) / (LOCAL_FAR - LOCAL_NEAR)));
            localProjection.put(14, (float)(-2.0D * LOCAL_FAR * LOCAL_NEAR / (LOCAL_FAR - LOCAL_NEAR)));
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            projectionPushed = true;
            GL11.glLoadMatrix(localProjection);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            active = true;
            return true;
        } catch (RuntimeException failure) {
            if (projectionPushed) {
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glPopMatrix();
            }
            restoreFramebuffers();
            GL11.glMatrixMode(previousMatrixMode);
            GL13.glActiveTexture(previousActiveTexture);
            GL11.glPopAttrib();
            unavailable = true;
            MCH_Lib.Log("[FarVehicle] high-precision depth layer failed: %s", failure.toString());
            return false;
        }
    }

    void finish(boolean completed, double depthNear, double depthFar, float worldZ, float worldOffset) {
        if (!active) return;
        try {
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            restoreFramebuffers();
            GL11.glViewport(0, 0, width, height);
            if (completed) {
                try {
                    composite(depthNear, depthFar, worldZ, worldOffset);
                } catch (RuntimeException failure) {
                    unavailable = true;
                    MCH_Lib.Log("[FarVehicle] depth layer composite failed: %s", failure.toString());
                }
            }
        } finally {
            GL20.glUseProgram(previousProgram);
            restoreFramebuffers();
            GL11.glMatrixMode(previousMatrixMode);
            GL13.glActiveTexture(previousActiveTexture);
            GL11.glPopAttrib();
            active = false;
        }
    }

    private void restoreFramebuffers() {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousReadFramebuffer);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDrawFramebuffer);
    }

    private boolean bounds(double x, double y, double z, double r,
            FloatBuffer m, FloatBuffer p, int w, int h) {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 8; i++) {
            double px = x + ((i & 1) == 0 ? -r : r);
            double py = y + ((i & 2) == 0 ? -r : r);
            double pz = z + ((i & 4) == 0 ? -r : r);
            double ex = m.get(0) * px + m.get(4) * py + m.get(8) * pz + m.get(12);
            double ey = m.get(1) * px + m.get(5) * py + m.get(9) * pz + m.get(13);
            double ez = m.get(2) * px + m.get(6) * py + m.get(10) * pz + m.get(14);
            double cw = p.get(3) * ex + p.get(7) * ey + p.get(11) * ez + p.get(15);
            if (cw <= 0.001D) return false;
            double sx = (1.0D + (p.get(0) * ex + p.get(4) * ey + p.get(8) * ez + p.get(12)) / cw) * w * 0.5D;
            double sy = (1.0D + (p.get(1) * ex + p.get(5) * ey + p.get(9) * ez + p.get(13)) / cw) * h * 0.5D;
            minX = Math.min(minX, sx); maxX = Math.max(maxX, sx);
            minY = Math.min(minY, sy); maxY = Math.max(maxY, sy);
        }
        left = Math.max(0, (int)Math.floor(minX) - 2);
        bottom = Math.max(0, (int)Math.floor(minY) - 2);
        right = Math.min(w, (int)Math.ceil(maxX) + 2);
        top = Math.min(h, (int)Math.ceil(maxY) + 2);
        return right > left && top > bottom;
    }

    private boolean ensureResources(int w, int h) {
        if (program == 0) {
            int vertex = compile(GL20.GL_VERTEX_SHADER, VERTEX);
            int fragment = compile(GL20.GL_FRAGMENT_SHADER, FRAGMENT);
            if (vertex == 0 || fragment == 0) {
                if (vertex != 0) GL20.glDeleteShader(vertex);
                if (fragment != 0) GL20.glDeleteShader(fragment);
                return false;
            }
            program = GL20.glCreateProgram();
            GL20.glAttachShader(program, vertex);
            GL20.glAttachShader(program, fragment);
            GL20.glLinkProgram(program);
            GL20.glDeleteShader(vertex);
            GL20.glDeleteShader(fragment);
            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                MCH_Lib.Log("[FarVehicle] depth layer link failed: %s",
                        GL20.glGetProgramInfoLog(program, 1024));
                GL20.glDeleteProgram(program);
                program = 0;
                return false;
            }
            colorSampler = GL20.glGetUniformLocation(program, "colorTex");
            depthSampler = GL20.glGetUniformLocation(program, "depthTex");
            localZUniform = GL20.glGetUniformLocation(program, "localZ");
            localOffsetUniform = GL20.glGetUniformLocation(program, "localOffset");
            worldZUniform = GL20.glGetUniformLocation(program, "worldZ");
            worldOffsetUniform = GL20.glGetUniformLocation(program, "worldOffset");
            rangeNearUniform = GL20.glGetUniformLocation(program, "rangeNear");
            rangeFarUniform = GL20.glGetUniformLocation(program, "rangeFar");
        }
        if (framebuffer != 0 && width == w && height == h) return true;
        if (framebuffer != 0) GL30.glDeleteFramebuffers(framebuffer);
        if (colorTexture != 0) GL11.glDeleteTextures(colorTexture);
        if (depthTexture != 0) GL11.glDeleteTextures(depthTexture);
        framebuffer = GL30.glGenFramebuffers();
        colorTexture = texture(w, h, GL11.GL_RGBA8, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE);
        depthTexture = texture(w, h, DEPTH_COMPONENT24, GL11.GL_DEPTH_COMPONENT, GL11.GL_UNSIGNED_INT);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, colorTexture, 0);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
                GL11.GL_TEXTURE_2D, depthTexture, 0);
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
        width = w; height = h;
        return GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) == GL30.GL_FRAMEBUFFER_COMPLETE;
    }

    private static int texture(int w, int h, int internal, int format, int type) {
        int id = GL11.glGenTextures();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internal, w, h, 0, format, type, (java.nio.ByteBuffer)null);
        return id;
    }

    private static int compile(int kind, String source) {
        int shader = GL20.glCreateShader(kind);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) != GL11.GL_FALSE) return shader;
        MCH_Lib.Log("[FarVehicle] depth layer shader failed: %s",
                GL20.glGetShaderInfoLog(shader, 1024));
        GL20.glDeleteShader(shader);
        return 0;
    }

    private void composite(double near, double far, float worldZ, float worldOffset) {
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LESS);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColorMask(true, true, true, true);
        GL20.glUseProgram(program);
        GL20.glUniform1i(colorSampler, 0);
        GL20.glUniform1i(depthSampler, 1);
        GL20.glUniform1f(localZUniform, localProjection.get(10));
        GL20.glUniform1f(localOffsetUniform, localProjection.get(14));
        GL20.glUniform1f(worldZUniform, worldZ);
        GL20.glUniform1f(worldOffsetUniform, worldOffset);
        GL20.glUniform1f(rangeNearUniform, (float)near);
        GL20.glUniform1f(rangeFarUniform, (float)far);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorTexture);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTexture);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0D, width, 0.0D, height, -1.0D, 1.0D);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2d((double)left / width, (double)bottom / height); GL11.glVertex2i(left, bottom);
        GL11.glTexCoord2d((double)right / width, (double)bottom / height); GL11.glVertex2i(right, bottom);
        GL11.glTexCoord2d((double)right / width, (double)top / height); GL11.glVertex2i(right, top);
        GL11.glTexCoord2d((double)left / width, (double)top / height); GL11.glVertex2i(left, top);
        GL11.glEnd();
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
    }
}
