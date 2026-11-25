package mcheli.particles;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Camera;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.render.MCH_FMURThermal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class MCH_EntityParticleSmoke extends MCH_EntityParticleBase {

    public boolean thermalHot;
    private static final ResourceLocation SMOKE_TEXTURE = new ResourceLocation(MCH_MOD.DOMAIN, "textures/particles/smoke.png");
    private static final List<MCH_EntityParticleSmoke> queued = new ArrayList<MCH_EntityParticleSmoke>();
    private static float rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ;
    private static double viewX, viewY, viewZ;

    public static void renderQueued(float partialTicks) {
        if (queued.isEmpty()) return;
        Minecraft mc = Minecraft.getMinecraft();
        float oldBrightnessX = OpenGlHelper.lastBrightnessX;
        float oldBrightnessY = OpenGlHelper.lastBrightnessY;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT
            | GL11.GL_LIGHTING_BIT | GL11.GL_TEXTURE_BIT | GL11.GL_CURRENT_BIT);
        try {
            mc.entityRenderer.enableLightmap(partialTicks);
            boolean legacyThermal = useLegacyThermalTint();
            prepareSmokeDraw(mc, legacyThermal);
            drawSmokeBatch(Tessellator.instance, queued, mc.theWorld, partialTicks, legacyThermal);
        } finally {
            queued.clear();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
                oldBrightnessX, oldBrightnessY);
            mc.entityRenderer.disableLightmap(partialTicks);
            GL11.glPopAttrib();
        }
    }

    static void drawSmokeBatch(Tessellator tessellator, List<MCH_EntityParticleSmoke> particles,
                               World world, float partialTicks, boolean legacyThermal) {
        boolean started = false;
        // Keep submission order so translucent smoke blends in the same order as before.
        for (MCH_EntityParticleSmoke smoke : particles) {
            if (smoke.isDead || smoke.worldObj != world) continue;
            if (!started) {
                tessellator.startDrawingQuads();
                tessellator.setNormal(0.0F, 1.0F, 0.0F);
                started = true;
            }
            smoke.appendBillboard(tessellator, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ,
                viewX, viewY, viewZ, false, legacyThermal);
        }
        if (started) tessellator.draw();
    }

    public MCH_EntityParticleSmoke(World par1World, double x, double y, double z, double mx, double my, double mz) {
        super(par1World, x, y, z, mx, my, mz);
        super.particleRed = super.particleGreen = super.particleBlue = super.rand.nextFloat() * 0.3F + 0.7F;
        this.setParticleScale(super.rand.nextFloat() * 0.5F + 5.0F);
        this.setParticleMaxAge((int) (16.0D / ((double) super.rand.nextFloat() * 0.8D + 0.2D)) + 2);
    }

    public void onUpdate() {
        super.prevPosX = super.posX;
        super.prevPosY = super.posY;
        super.prevPosZ = super.posZ;
        if (super.particleAge < super.particleMaxAge) {
            this.setParticleTextureIndex((int) (8.0D * (double) super.particleAge / (double) super.particleMaxAge));
            ++super.particleAge;
            if (super.diffusible && super.particleScale < super.particleMaxScale) {
                super.particleScale += 0.8F;
            }

            if (super.toWhite) {
                float mn = this.getMinColor();
                float mx = this.getMaxColor();
                float dist = mx - mn;
                if ((double) dist > 0.2D) {
                    super.particleRed += (mx - super.particleRed) * 0.016F;
                    super.particleGreen += (mx - super.particleGreen) * 0.016F;
                    super.particleBlue += (mx - super.particleBlue) * 0.016F;
                }
            }

            this.effectWind();
            if ((double) super.particleAge / (double) super.particleMaxAge > (double) super.moutionYUpAge) {
                super.motionY += 0.02D;
            } else {
                super.motionY += (double) super.gravity;
            }

            this.moveEntity(super.motionX, super.motionY, super.motionZ);
            if (super.diffusible) {
                super.motionX *= 0.96D;
                super.motionZ *= 0.96D;
                super.motionY *= 0.96D;
            } else {
                super.motionX *= 0.9D;
                super.motionZ *= 0.9D;
            }

        } else {
            this.setDead();
        }
    }

    public float getMinColor() {
        return this.min(this.min(super.particleBlue, super.particleGreen), super.particleRed);
    }

    public float getMaxColor() {
        return this.max(this.max(super.particleBlue, super.particleGreen), super.particleRed);
    }

    public float min(float a, float b) {
        return Math.min(a, b);
    }

    public float max(float a, float b) {
        return Math.max(a, b);
    }

    public void effectWind() {
        if (super.isEffectedWind) {
            boolean range = true;
            List list = super.worldObj.getEntitiesWithinAABB(MCH_EntityAircraft.class, this.getBoundingBox().expand(15.0D, 15.0D, 15.0D));

            for (Object o : list) {
                MCH_EntityAircraft ac = (MCH_EntityAircraft) o;
                if (ac.getThrottle() > 0.10000000149011612D) {
                    float dist = this.getDistanceToEntity(ac);
                    double vel = (23.0D - (double) dist) * 0.009999999776482582D * ac.getThrottle();
                    double mx = ac.posX - super.posX;
                    double mz = ac.posZ - super.posZ;
                    super.motionX -= mx * vel;
                    super.motionZ -= mz * vel;
                }
            }
        }

    }

    public int getFXLayer() {
        return 3;
    }

    @SideOnly(Side.CLIENT)
    public int getBrightnessForRender(float p_70070_1_) {
        double y = super.posY;
        super.posY += 3000.0D;
        int i = super.getBrightnessForRender(p_70070_1_);
        super.posY = y;
        return i;
    }

    @Override
    public void renderParticle(Tessellator tessellator, float partialTicks, float rotX, float rotZ, float rotYZ, float rotXY, float rotXZ) {
        if (!MCH_FMURThermal.isParticlePass()) {
            // Vanilla renders layer-3 particles before the far-vehicle LOD pass.
            if (queued.isEmpty()) {
                rotationX = rotX;
                rotationZ = rotZ;
                rotationYZ = rotYZ;
                rotationXY = rotXY;
                rotationXZ = rotXZ;
                viewX = EntityFX.interpPosX;
                viewY = EntityFX.interpPosY;
                viewZ = EntityFX.interpPosZ;
            }
            queued.add(this);
            return;
        }

        // FMUR replays particles directly into its active sensor target.
        boolean legacyThermal = useLegacyThermalTint();
        int srcBlend = GL11.glGetInteger(GL11.GL_BLEND_SRC);
        int dstBlend = GL11.glGetInteger(GL11.GL_BLEND_DST);
        prepareSmokeDraw(Minecraft.getMinecraft(), legacyThermal);
        try {
            tessellator.startDrawingQuads();
            tessellator.setNormal(0.0F, 1.0F, 0.0F);
            appendBillboard(tessellator, partialTicks, rotX, rotZ, rotYZ, rotXY, rotXZ,
                EntityFX.interpPosX, EntityFX.interpPosY, EntityFX.interpPosZ, true, legacyThermal);
            tessellator.draw();
        } finally {
            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glBlendFunc(srcBlend, dstBlend);
            GL11.glDisable(GL11.GL_BLEND);
        }
    }

    private static boolean useLegacyThermalTint() {
        return MCH_Camera.currentCameraMode == MCH_Camera.MODE_THERMALVISION && !MCH_FMURThermal.isAvailable();
    }

    private static void prepareSmokeDraw(Minecraft mc, boolean legacyThermal) {
        mc.renderEngine.bindTexture(SMOKE_TEXTURE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        // Layer-3 particles render outside vanilla's normal transparent-particle state setup.
        // Keep depth testing, but never let a translucent smoke billboard write scene depth.
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        if (legacyThermal) {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            GL11.glColor4f(1.0F, 0.0F, 1.0F, 1.0F);
        } else {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private void appendBillboard(Tessellator tessellator, float partialTicks, float rotX, float rotZ, float rotYZ,
                                 float rotXY, float rotXZ, double cameraX, double cameraY, double cameraZ,
                                 boolean thermalPass, boolean legacyThermal) {
        if (legacyThermal) {
            tessellator.setColorRGBA_F(1.0F, 0.0F, 1.0F, 1.0F);
            tessellator.setBrightness(0xF000F0);
        } else {
            tessellator.setColorRGBA_F(super.particleRed, super.particleGreen, super.particleBlue, super.particleAlpha);
            if (thermalPass && this.thermalHot) {
                // FMUR's particle pass reserves block-light 241..255 for cooling smoke heat.
                float life = Math.min(1F, (super.particleAge + partialTicks) / Math.max(1F, super.particleMaxAge));
                tessellator.setBrightness(0xF00000 | (241 + Math.round(14F * (0.9F - 0.5F * life))));
            } else {
                tessellator.setBrightness(this.getBrightnessForRender(partialTicks));
            }
        }
        float leftU = super.particleTextureIndexX / 8.0F;
        float rightU = leftU + 0.125F;
        float size = 0.1F * super.particleScale;
        float x = (float) (super.prevPosX + (super.posX - super.prevPosX) * partialTicks - cameraX);
        float y = (float) (super.prevPosY + (super.posY - super.prevPosY) * partialTicks - cameraY);
        float z = (float) (super.prevPosZ + (super.posZ - super.prevPosZ) * partialTicks - cameraZ);
        tessellator.addVertexWithUV(x - rotX * size - rotXY * size, y - rotZ * size, z - rotYZ * size - rotXZ * size, rightU, 1.0D);
        tessellator.addVertexWithUV(x - rotX * size + rotXY * size, y + rotZ * size, z - rotYZ * size + rotXZ * size, rightU, 0.0D);
        tessellator.addVertexWithUV(x + rotX * size + rotXY * size, y + rotZ * size, z + rotYZ * size + rotXZ * size, leftU, 0.0D);
        tessellator.addVertexWithUV(x + rotX * size - rotXY * size, y - rotZ * size, z + rotYZ * size - rotXZ * size, leftU, 1.0D);
    }
}
