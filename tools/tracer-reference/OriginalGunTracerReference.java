package com.flansmod.client.model;

import com.flansmod.client.FlansModResourceHandler;
import com.flansmod.common.guns.EntityBullet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.Map;
import java.util.WeakHashMap;

public class OriginalGunTracerReference extends Render
{
	private static final float NIGHT_VISION_BRIGHTNESS_MULTIPLIER = 1.35F;
	private static final double MAX_HANDHELD_VISUAL_CORRECTION = 3D;
	private static final ResourceLocation RED_TRACER_TEXTURE =
			new ResourceLocation("flansmod", "particle/FMTracerRed.png");
	private static final ResourceLocation GREEN_TRACER_TEXTURE =
			new ResourceLocation("flansmod", "particle/FMTracerGreen.png");
	private static final ResourceLocation BLUE_TRACER_TEXTURE =
			new ResourceLocation("flansmod", "particle/FMTracerBlue.png");
	private final Map<EntityBullet, VisualOriginCorrection> visualOriginCorrections =
			new WeakHashMap<EntityBullet, VisualOriginCorrection>();

	public OriginalGunTracerReference()
	{
		shadowSize = 0;
	}

	public void render(EntityBullet bullet, double d, double d1, double d2, float f, float f1)
	{
		if(bullet.owner == Minecraft.getMinecraft().thePlayer && bullet.ticksExisted < 1)
			return;
		VisualOriginCorrection correction = getVisualOriginCorrection(bullet, f1);
		bindEntityTexture(bullet);
		GL11.glPushMatrix();
		GL11.glTranslated(d + correction.x, d1 + correction.y, d2 + correction.z);
		renderTracerGlow(bullet, f1);
		GL11.glRotatef(f, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(90F -bullet.prevRotationPitch - (bullet.rotationPitch - bullet.prevRotationPitch) * f1, 1.0F, 0.0F, 0.0F);
		ModelBase model = bullet.type.model;
		if(model != null)
			model.render(bullet, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F);
		GL11.glPopMatrix();
	}

	/**
	 * Rebase only the rendered trajectory of handheld player bullets onto the
	 * shooter's visible firing line. The authoritative entity can arrive at the
	 * client from an older server-side player position, which makes the whole
	 * projectile path appear on the opposite side of a strafe. Projecting the
	 * eye/barrel anchor onto the horizontal path gives one constant lateral
	 * offset. Y is deliberately preserved so looking up or down cannot lift the
	 * synchronized tracer away from the model-anchored muzzle tracer.
	 */
	private VisualOriginCorrection getVisualOriginCorrection(EntityBullet bullet, float partialTicks)
	{
		VisualOriginCorrection existing = visualOriginCorrections.get(bullet);
		if(existing != null)
			return existing;

		if(!(bullet.owner instanceof EntityPlayer) || bullet.type == null
				|| bullet.owner.ridingEntity != null)
			return VisualOriginCorrection.NONE;

		double horizontalSpeedSquared = bullet.motionX * bullet.motionX
				+ bullet.motionZ * bullet.motionZ;
		if(horizontalSpeedSquared < 0.000001D)
			return VisualOriginCorrection.NONE;

		EntityPlayer shooter = (EntityPlayer)bullet.owner;
		double bulletX = interpolate(bullet.lastTickPosX, bullet.posX, partialTicks);
		double bulletZ = interpolate(bullet.lastTickPosZ, bullet.posZ, partialTicks);
		double muzzleX = interpolate(shooter.lastTickPosX, shooter.posX, partialTicks);
		double muzzleZ = interpolate(shooter.lastTickPosZ, shooter.posZ, partialTicks);

		double toMuzzleX = muzzleX - bulletX;
		double toMuzzleZ = muzzleZ - bulletZ;
		double alongTrajectory = (toMuzzleX * bullet.motionX
				+ toMuzzleZ * bullet.motionZ) / horizontalSpeedSquared;
		double correctionX = toMuzzleX - bullet.motionX * alongTrajectory;
		double correctionZ = toMuzzleZ - bullet.motionZ * alongTrajectory;
		double correctionSquared = correctionX * correctionX
				+ correctionZ * correctionZ;

		// Do not pull unrelated or discontinuity-sized projectiles onto a player.
		if(correctionSquared > MAX_HANDHELD_VISUAL_CORRECTION * MAX_HANDHELD_VISUAL_CORRECTION)
			return VisualOriginCorrection.NONE;

		VisualOriginCorrection correction = new VisualOriginCorrection(
				correctionX, 0D, correctionZ);
		visualOriginCorrections.put(bullet, correction);
		return correction;
	}

	private double interpolate(double previous, double current, float partialTicks)
	{
		return previous + (current - previous) * partialTicks;
	}

	private static final class VisualOriginCorrection
	{
		private static final VisualOriginCorrection NONE = new VisualOriginCorrection(0D, 0D, 0D);
		private final double x;
		private final double y;
		private final double z;

		private VisualOriginCorrection(double x, double y, double z)
		{
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

	/**
	 * Render the gameplay tracer directly on the synchronized bullet entity.
	 */
	private void renderTracerGlow(EntityBullet bullet, float partialTicks)
	{
		if(!bullet.type.tracer || !bullet.hasTracerReachedVisibleDistance())
			return;

		double speed = Math.sqrt(bullet.motionX * bullet.motionX
				+ bullet.motionY * bullet.motionY
				+ bullet.motionZ * bullet.motionZ);
		if(speed < 0.001D)
			return;

		double directionX = bullet.motionX / speed;
		double directionY = bullet.motionY / speed;
		double directionZ = bullet.motionZ / speed;
		float tracerScale = bullet.type.tracerScale;
		double length = Math.max(0.45D, Math.min(3.25D, speed * 1.35D)) * tracerScale;
		Minecraft minecraft = Minecraft.getMinecraft();
		float nightVisionBrightness = minecraft.thePlayer != null
				&& minecraft.thePlayer.isPotionActive(Potion.nightVision)
				? NIGHT_VISION_BRIGHTNESS_MULTIPLIER : 1F;
		if(minecraft.gameSettings.thirdPersonView == 0 && minecraft.thePlayer != null)
		{
			double bulletX = bullet.lastTickPosX + (bullet.posX - bullet.lastTickPosX) * partialTicks;
			double bulletY = bullet.lastTickPosY + (bullet.posY - bullet.lastTickPosY) * partialTicks;
			double bulletZ = bullet.lastTickPosZ + (bullet.posZ - bullet.lastTickPosZ) * partialTicks;
			double eyeX = minecraft.thePlayer.lastTickPosX
					+ (minecraft.thePlayer.posX - minecraft.thePlayer.lastTickPosX) * partialTicks;
			double eyeY = minecraft.thePlayer.lastTickPosY
					+ (minecraft.thePlayer.posY - minecraft.thePlayer.lastTickPosY) * partialTicks
					+ minecraft.thePlayer.getEyeHeight();
			double eyeZ = minecraft.thePlayer.lastTickPosZ
					+ (minecraft.thePlayer.posZ - minecraft.thePlayer.lastTickPosZ) * partialTicks;
			double distanceToEye = Math.sqrt((bulletX - eyeX) * (bulletX - eyeX)
					+ (bulletY - eyeY) * (bulletY - eyeY)
					+ (bulletZ - eyeZ) * (bulletZ - eyeZ));
			length = Math.min(length, Math.max(0D, distanceToEye - 0.35D));
		}

		double tailX = -directionX * length;
		double tailY = -directionY * length;
		double tailZ = -directionZ * length;
		boolean greenTracer = bullet.type.greenTracer;
		boolean blueTracer = bullet.type.blueTracer;
		float glowRed = blueTracer || greenTracer ? 0F : 1F;
		float glowGreen = blueTracer ? 0.35F : (greenTracer ? 1F : 0F);
		float glowBlue = blueTracer ? 1F : 0F;
		float accentRed = blueTracer ? 0.08F : (greenTracer ? 0.04F : 1F);
		float coreGreen = blueTracer ? 0.72F : (greenTracer ? 1F : 0.04F);
		float coreBlue = blueTracer ? 1F : 0.01F;

		GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
		try
		{
			// Shader packs still run their entity program here. Keep texturing enabled and
			// submit complete vertex data instead of raw untextured immediate-mode lines.
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glDisable(GL11.GL_ALPHA_TEST);
			GL11.glDisable(GL11.GL_CULL_FACE);
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glDepthMask(false);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
			GL11.glEnable(GL11.GL_LINE_SMOOTH);
			GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
			minecraft.renderEngine.bindTexture(blueTracer ? BLUE_TRACER_TEXTURE
					: (greenTracer ? GREEN_TRACER_TEXTURE : RED_TRACER_TEXTURE));

			if(length > 0.05D)
			{
				drawTracerLine(tailX, tailY, tailZ, 11F * tracerScale,
						glowRed, glowGreen, glowBlue, scaledAlpha(0.10F, nightVisionBrightness));
				drawTracerLine(tailX, tailY, tailZ, 6F * tracerScale,
						blueTracer ? 0.02F : (greenTracer ? 0.015F : 1F),
						blueTracer ? 0.55F : (greenTracer ? 1F : 0.015F), glowBlue,
						scaledAlpha(0.24F, nightVisionBrightness));
				drawTracerLine(tailX, tailY, tailZ, 2.25F * tracerScale,
						accentRed, coreGreen, coreBlue, 1F);
			}

			GL11.glEnable(GL11.GL_POINT_SMOOTH);
			GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_NICEST);
			drawTracerPoint(9F * tracerScale, glowRed, glowGreen, glowBlue,
					scaledAlpha(0.16F, nightVisionBrightness));
			drawTracerPoint(3.5F * tracerScale, accentRed, coreGreen, coreBlue, 1F);
		}
		finally
		{
			GL11.glPopAttrib();
			GL11.glColor4f(1F, 1F, 1F, 1F);
			bindEntityTexture(bullet);
		}
	}

	private float scaledAlpha(float alpha, float multiplier)
	{
		return Math.min(1F, alpha * multiplier);
	}

	private void drawTracerLine(double tailX, double tailY, double tailZ,
			float width, float red, float green, float blue, float alpha)
	{
		Tessellator tessellator = Tessellator.instance;
		GL11.glLineWidth(width);
		tessellator.startDrawing(GL11.GL_LINES);
		tessellator.setBrightness(15728880);
		tessellator.setColorRGBA_F(red, green, blue, alpha);
		tessellator.setNormal(0F, 1F, 0F);
		tessellator.addVertexWithUV(tailX, tailY, tailZ, 0.5D, 0.5D);
		tessellator.addVertexWithUV(0D, 0D, 0D, 0.5D, 0.5D);
		tessellator.draw();
	}

	private void drawTracerPoint(float size, float red, float green, float blue, float alpha)
	{
		Tessellator tessellator = Tessellator.instance;
		GL11.glPointSize(size);
		tessellator.startDrawing(GL11.GL_POINTS);
		tessellator.setBrightness(15728880);
		tessellator.setColorRGBA_F(red, green, blue, alpha);
		tessellator.setNormal(0F, 1F, 0F);
		tessellator.addVertexWithUV(0D, 0D, 0D, 0.5D, 0.5D);
		tessellator.draw();
	}

	@Override
	public void doRender(Entity entity, double d, double d1, double d2, float f, float f1)
	{
		render((EntityBullet) entity, d, d1, d2, f, f1);
	}
	
	@Override
	protected ResourceLocation getEntityTexture(Entity entity) 
	{
		return FlansModResourceHandler.getTexture(((EntityBullet)entity).type);
	}
}