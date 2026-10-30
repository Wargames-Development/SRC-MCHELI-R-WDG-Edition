import com.flansmod.client.TracerRenderApi;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.util.glu.GLU;

/** Real GPU checks of shared tracer geometry; not a full Iris shader-pack or gameplay launch. */
public final class CiwsTracerGpuChecks {
    private static final int W = 640, H = 360;
    private static int checks;
    private static sun.misc.Unsafe allocator;
    private static Minecraft mc;
    private static Entity projectile;
    private static File output;
    private static double distance = 30;
    private static com.flansmod.common.guns.EntityBullet regularBullet;
    private static com.flansmod.client.model.OriginalGunTracerReference original;
    private static java.lang.reflect.Method originalGlow;

    public static void main(String[] args) throws Exception {
        output = new File(args[0]); output.mkdirs();
        Pbuffer context = new Pbuffer(W, H, new PixelFormat().withDepthBits(24), null, null);
        try {
            context.makeCurrent();
            allocator = (sun.misc.Unsafe) field(sun.misc.Unsafe.class, "theUnsafe").get(null);
            mc = (Minecraft) allocator.allocateInstance(Minecraft.class);
            field(Minecraft.class, "theMinecraft").set(null, mc);
            mc.renderViewEntity = (net.minecraft.entity.EntityLivingBase) allocator.allocateInstance(net.minecraft.entity.passive.EntityPig.class);
            mc.renderEngine = new TextureManager(null) {
                private final Map<ResourceLocation, Integer> textures = new HashMap<ResourceLocation, Integer>();
                @Override public void bindTexture(ResourceLocation location) {
                    Integer id = textures.get(location);
                    if (id == null) {
                        try {
                            BufferedImage image = ImageIO.read(getClass().getResourceAsStream("/assets/" + location.getResourceDomain() + "/" + location.getResourcePath()));
                            ByteBuffer rgba = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4);
                            for (int y = 0; y < image.getHeight(); ++y) for (int x = 0; x < image.getWidth(); ++x) {
                                int pixel = image.getRGB(x, y);
                                rgba.put((byte)(pixel >> 16)).put((byte)(pixel >> 8)).put((byte)pixel).put((byte)(pixel >> 24));
                            }
                            rgba.flip(); id = GL11.glGenTextures(); GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
                            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
                            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, image.getWidth(), image.getHeight(), 0,
                                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, rgba);
                            textures.put(location, id);
                        } catch (Exception failure) { throw new RuntimeException(failure); }
                    }
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
                }
            };
            mc.gameSettings = new net.minecraft.client.settings.GameSettings();
            regularBullet = (com.flansmod.common.guns.EntityBullet) allocator.allocateInstance(com.flansmod.common.guns.EntityBullet.class);
            regularBullet.type = (com.flansmod.common.guns.type.BulletType) allocator.allocateInstance(com.flansmod.common.guns.type.BulletType.class);
            regularBullet.type.tracer = true;
            regularBullet.type.texture = "defaultBullet";
            field(regularBullet.getClass(), "tracerOriginSet").setBoolean(regularBullet, true);
            original = new com.flansmod.client.model.OriginalGunTracerReference();
            original.setRenderManager(net.minecraft.client.renderer.entity.RenderManager.instance);
            net.minecraft.client.renderer.entity.RenderManager.instance.renderEngine = mc.renderEngine;
            originalGlow = original.getClass().getDeclaredMethod("renderTracerGlow", regularBullet.getClass(), float.class);
            originalGlow.setAccessible(true);
            OpenGlHelper.defaultTexUnit = GL13.GL_TEXTURE0;
            OpenGlHelper.lightmapTexUnit = GL13.GL_TEXTURE1;
            projectile = (Entity) allocator.allocateInstance(net.minecraft.entity.projectile.EntitySnowball.class);
            int program = shader();
            for (boolean shaded : new boolean[]{false, true}) {
                for (float scale : new float[]{1F, 1.25F}) {
                    verify(shaded ? program : 0, scale, false, false);
                    verify(shaded ? program : 0, scale, false, true);
                    verify(shaded ? program : 0, scale, true, false);
                    for (double far : new double[]{150, 500}) {
                        distance = far;
                        verify(shaded ? program : 0, scale, false, false);
                        verify(shaded ? program : 0, scale, false, true);
                    }
                    distance = 30;
                }
            }
            nativeBridge(program);
            GL20.glUseProgram(0); GL20.glDeleteProgram(program);
            check(GL11.glGetError() == GL11.GL_NO_ERROR, "No OpenGL errors");
            System.out.println("PASS: " + checks + " GPU assertions: fixed-function/GLSL, both calibers, head-on, cover, GL restoration and real MCHR bridge.");
        } finally { context.destroy(); }
    }

    private static void setup(int program, boolean wall) {
        GL20.glUseProgram(0);
        for (int i = 0; i < 3; ++i) { GL13.glActiveTexture(GL13.GL_TEXTURE0 + i); GL11.glDisable(GL11.GL_TEXTURE_2D); }
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glViewport(0, 0, W, H); GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_FOG);
        GL11.glDisable(GL11.GL_BLEND); GL11.glDisable(GL11.GL_ALPHA_TEST); GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_DEPTH_TEST); GL11.glDepthMask(true); GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glClearColor(0.035F, 0.045F, 0.055F, 1); GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glLoadIdentity(); GLU.gluPerspective(65, (float)W/H, 0.1F, 1000);
        GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glLoadIdentity();
        if (wall) {
            GL11.glColor4f(0.2F, 0.2F, 0.2F, 1); GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex3d(-20, -20, -15); GL11.glVertex3d(20, -20, -15);
            GL11.glVertex3d(20, 20, -15); GL11.glVertex3d(-20, 20, -15); GL11.glEnd();
        }
        projectile.posZ = projectile.lastTickPosZ = -distance;
        GL11.glTranslated(0, 0, -distance);
        GL20.glUseProgram(program);
        if (program != 0) GL20.glUniform1i(GL20.glGetUniformLocation(program, "tex"), 0);
        OpenGlHelper.setLightmapTextureCoords(GL13.GL_TEXTURE1, 12, 34);
        GL11.glColor4f(0.3F, 0.4F, 0.5F, 0.6F);
        GL13.glActiveTexture(GL13.GL_TEXTURE2);
    }

    private static void verify(int program, float scale, boolean wall, boolean headOn) throws Exception {
        setup(program, wall);
        check(TracerRenderApi.render(projectile, 0.5F, headOn ? 0 : 15, headOn ? 0 : 3, -18, scale, false, false), "Tracer renders");
        state(program);
        int bright = redPixels();
        if (!wall) save((program == 0 ? "fixed" : "shader") + "-" + (scale == 1 ? "20mm" : "30mm") + (headOn ? "-head" : "-side")
            + (distance == 30 ? "" : "-" + (int)distance + "m") + ".png");
        check(wall ? bright == 0 : bright > 2, "Visible red tracer, or completely hidden behind cover: " + bright);
        ByteBuffer shared = pixels();
        setup(program, wall);
        regularBullet.posZ = regularBullet.lastTickPosZ = -distance;
        regularBullet.motionX = headOn ? 0 : 15;
        regularBullet.motionY = headOn ? 0 : 3;
        regularBullet.motionZ = -18;
        regularBullet.type.tracerScale = scale;
        GL13.glActiveTexture(GL13.GL_TEXTURE0); // Normal FMUR entity rendering enters on the diffuse unit.
        originalGlow.invoke(original, regularBullet, 0.5F);
        ByteBuffer baseline = pixels();
        for (int i = 0; i < W * H * 4; ++i) {
            if (i % 4 != 3 && Math.abs((shared.get(i) & 255) - (baseline.get(i) & 255)) > 1) {
                throw new AssertionError("Shared tracer differs from original FMUR gun pixels at byte " + i);
            }
        }
        ++checks;
    }

    private static void nativeBridge(int program) throws Exception {
        Class<?> type = Class.forName("mcheli.weapon.MCH_EntityBullet");
        Object bullet = allocator.allocateInstance(type);
        Class<?> infoType = Class.forName("mcheli.weapon.MCH_WeaponInfo");
        Object info = infoType.getConstructor(String.class).newInstance("ac_cram_m61a1");
        field(infoType, "fmurTracerGlow").setBoolean(info, true);
        field(type, "weaponInfo").set(bullet, info);
        Entity entity = (Entity) bullet; entity.posZ = entity.lastTickPosZ = -30; entity.motionX = 3.9;
        field(type, "accelerationFactor").setDouble(bullet, 4.5);
        java.lang.reflect.Method render = Class.forName("mcheli.render.MCH_FMURTracer").getMethod("render", type.getSuperclass(), float.class);
        setup(program, false);
        check((Boolean) render.invoke(null, bullet, 0.5F), "Actual MCHR native bullet reaches FMUR API");
        check(redPixels() > 2, "Actual MCHR bridge draws pixels"); state(program);
        save("mchr-ciws-bridge.png");
        entity.motionX = Double.NaN;
        check(!(Boolean) render.invoke(null, bullet, 0.5F), "Invalid motion safely falls back");
    }

    private static void state(int program) {
        check(GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) == program, "Shader program preserved");
        check(GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE) == GL13.GL_TEXTURE2, "Active texture preserved");
        check(GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK), "Depth write mask restored");
        check(!GL11.glIsEnabled(GL11.GL_BLEND) && GL11.glIsEnabled(GL11.GL_DEPTH_TEST), "Blend/depth state restored");
        check(OpenGlHelper.lastBrightnessX == 12 && OpenGlHelper.lastBrightnessY == 34, "Lightmap cache restored");
    }

    private static int shader() {
        int vertex = GL20.glCreateShader(GL20.GL_VERTEX_SHADER), fragment = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(vertex, "#version 120\nvarying vec2 uv; varying vec4 tint; varying float fullbright; void main(){gl_Position=ftransform();uv=gl_MultiTexCoord0.xy;tint=gl_Color;fullbright=step(239.0,gl_MultiTexCoord1.x)*step(239.0,gl_MultiTexCoord1.y)*step(0.5,length(gl_Normal));}");
        GL20.glShaderSource(fragment, "#version 120\nuniform sampler2D tex;varying vec2 uv;varying vec4 tint;varying float fullbright;void main(){gl_FragColor=texture2D(tex,uv)*tint*fullbright;}");
        GL20.glCompileShader(vertex); GL20.glCompileShader(fragment);
        check(GL20.glGetShaderi(vertex, GL20.GL_COMPILE_STATUS) != 0, GL20.glGetShaderInfoLog(vertex, 4096));
        check(GL20.glGetShaderi(fragment, GL20.GL_COMPILE_STATUS) != 0, GL20.glGetShaderInfoLog(fragment, 4096));
        int program = GL20.glCreateProgram(); GL20.glAttachShader(program, vertex); GL20.glAttachShader(program, fragment); GL20.glLinkProgram(program);
        check(GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) != 0, GL20.glGetProgramInfoLog(program, 4096));
        GL20.glDeleteShader(vertex); GL20.glDeleteShader(fragment); return program;
    }

    private static ByteBuffer pixels() {
        ByteBuffer buffer = BufferUtils.createByteBuffer(W * H * 4);
        GL11.glReadPixels(0, 0, W, H, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer); return buffer;
    }
    private static int redPixels() {
        ByteBuffer pixels = pixels(); int result = 0;
        for (int i = 0; i < W * H * 4; i += 4) if ((pixels.get(i) & 255) > (pixels.get(i + 1) & 255) + 25) ++result;
        return result;
    }
    private static void save(String name) throws Exception {
        ByteBuffer pixels = pixels(); BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < H; ++y) for (int x = 0; x < W; ++x) {
            int i = (y * W + x) * 4;
            image.setRGB(x, H - y - 1, 0xff000000 | (pixels.get(i)&255)<<16 | (pixels.get(i+1)&255)<<8 | pixels.get(i+2)&255);
        }
        ImageIO.write(image, "png", new File(output, name));
    }
    private static Field field(Class<?> type, String name) throws Exception {
        for (; type != null; type = type.getSuperclass()) try { Field result = type.getDeclaredField(name); result.setAccessible(true); return result; }
        catch (NoSuchFieldException absent) { }
        throw new NoSuchFieldException(name);
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); ++checks; }
}
