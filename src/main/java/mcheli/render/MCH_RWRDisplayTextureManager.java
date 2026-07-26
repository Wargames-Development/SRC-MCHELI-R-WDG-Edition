package mcheli.render;

import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.plane.MCP_EntityPlane;
import mcheli.tank.MCH_EntityTank;
import mcheli.vehicle.MCH_EntityVehicle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.IResource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class MCH_RWRDisplayTextureManager {

    private static final double RWR_UV_DIAMETER_RATIO = 0.75D;
    private static final double RWR_INNER_RING_RATIO = 0.22D;
    private static final double RWR_OUTER_RING_RATIO = 0.88D;
    private static final int WARMUP_CHUNK_PIXELS = 8192;
    private static final int RING_BG_COLOR = 0xE0000000;

    private static final ResourceLocation RWR_TEX = new ResourceLocation("mcheli", "textures/rwr.png");
    private static final ResourceLocation RWR_HELI_TEX = new ResourceLocation("mcheli", "textures/rwr_heli.png");
    private static final ResourceLocation RWR_TANK_TEX = new ResourceLocation("mcheli", "textures/rwr_tank.png");
    private static final ResourceLocation RWR_FAC_TEX = new ResourceLocation("mcheli", "textures/rwr_fac.png");

    private static final Map<Integer, RwrTexState> CACHE = new HashMap<Integer, RwrTexState>();
    private static final int[] PIXEL_COMPARISON = new int[MCH_TextureRenderUtil.TEX_SIZE * MCH_TextureRenderUtil.TEX_SIZE];

    private MCH_RWRDisplayTextureManager() {
    }

    public static ResourceLocation getTexture(MCH_EntityAircraft ac, EntityPlayer player, float partialTicks) {
        if (ac == null || ac.getAcInfo() == null || !ac.getAcInfo().hasRWR) {
            return null;
        }
        RwrTexState state = getOrCreate(ac);
        long worldTick = ac.worldObj != null ? ac.worldObj.getTotalWorldTime() : 0L;

        if (!state.ready) {
            warmupTexture(state);
            if (!state.ready) {
                return null;
            }
            if (!state.clearUploaded) {
                state.texture.updateDynamicTexture();
                state.clearUploaded = true;
                state.lastUpdateTick = worldTick;
                return null;
            }
        }

        if (!state.backgroundLoaded) {
            loadRWRBackground(state, ac);
        }

        if (ac.jammingTick > 0) {
            return MCH_TextureRenderUtil.getSharedJamTexture(worldTick);
        }

        MCH_RenderRWR.RWRDisplayFrame frame = MCH_RenderRWR.buildRWRDisplayFrame(ac, player, partialTicks);
        boolean hasThreats = frame != null && frame.valid && !frame.points.isEmpty();

        if (!hasThreats && state.hasRenderedFrame && !state.lastFrameHadThreats) {
            return state.location;
        }

        System.arraycopy(state.pixels, 0, PIXEL_COMPARISON, 0, state.pixels.length);
        renderGraphicsFrame(state.pixels, frame, state);
        state.hasRenderedFrame = true;
        state.lastFrameHadThreats = hasThreats;

        uploadIfChanged(state, PIXEL_COMPARISON);
        state.lastUpdateTick = worldTick;
        return state.location;
    }

    private static void warmupTexture(RwrTexState state) {
        if (state == null || state.pixels == null || state.ready) {
            return;
        }
        int start = state.warmupCursor;
        int end = Math.min(state.pixels.length, start + WARMUP_CHUNK_PIXELS);
        for (int i = start; i < end; i++) {
            state.pixels[i] = MCH_TextureRenderUtil.CLEAR_COLOR;
        }
        state.warmupCursor = end;
        if (end >= state.pixels.length) {
            state.ready = true;
        }
    }

    private static RwrTexState getOrCreate(MCH_EntityAircraft ac) {
        int aircraftId = ac.getEntityId();
        RwrTexState state = CACHE.get(aircraftId);
        UUID aircraftUuid = ac.getUniqueID();
        if (state != null && state.matches(aircraftUuid, ac.worldObj)) {
            return state;
        }
        if (state != null) {
            deleteTexture(state);
            CACHE.remove(aircraftId);
        }
        Minecraft mc = Minecraft.getMinecraft();
        DynamicTexture tex = new DynamicTexture(MCH_TextureRenderUtil.TEX_SIZE, MCH_TextureRenderUtil.TEX_SIZE);
        ResourceLocation location = mc.getTextureManager().getDynamicTextureLocation("mcheli_rwr_display_" + aircraftId, tex);
        state = new RwrTexState();
        state.texture = tex;
        state.location = location;
        state.pixels = tex.getTextureData();
        state.aircraftUuid = aircraftUuid;
        state.world = ac.worldObj;
        CACHE.put(aircraftId, state);
        return state;
    }

    public static void cleanup(World world) {
        if (world == null || CACHE.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Integer, RwrTexState>> iterator = CACHE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, RwrTexState> entry = iterator.next();
            RwrTexState state = entry.getValue();
            Entity entity = world.getEntityByID(entry.getKey());
            if (!(entity instanceof MCH_EntityAircraft)
                || entity.isDead
                || !state.matches(entity.getUniqueID(), world)) {
                deleteTexture(state);
                iterator.remove();
            }
        }
    }

    public static void clear() {
        for (RwrTexState state : CACHE.values()) {
            deleteTexture(state);
        }
        CACHE.clear();
    }

    private static void deleteTexture(RwrTexState state) {
        if (state != null && state.location != null) {
            Minecraft.getMinecraft().getTextureManager().deleteTexture(state.location);
        }
    }

    private static void uploadIfChanged(RwrTexState state, int[] previousPixels) {
        if (!Arrays.equals(state.pixels, previousPixels)) {
            state.texture.updateDynamicTexture();
        }
    }

    private static ResourceLocation selectRWRTexture(MCH_EntityAircraft ac) {
        if (ac instanceof MCP_EntityPlane) {
            if (ac.getAcInfo() != null && ac.getAcInfo().isFloat) {
                return RWR_FAC_TEX;
            }
            return RWR_TEX;
        } else if (ac instanceof MCH_EntityHeli) {
            return RWR_HELI_TEX;
        } else if (ac instanceof MCH_EntityTank || ac instanceof MCH_EntityVehicle) {
            return RWR_TANK_TEX;
        }
        return RWR_TEX;
    }

    private static void loadRWRBackground(RwrTexState state, MCH_EntityAircraft ac) {
        if (state.backgroundLoaded) return;
        ResourceLocation rwrRes = selectRWRTexture(ac);
        try {
            IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(rwrRes);
            BufferedImage image = ImageIO.read(resource.getInputStream());
            int texSize = MCH_TextureRenderUtil.TEX_SIZE;
            BufferedImage scaled = new BufferedImage(texSize, texSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scaled.createGraphics();
            int drawSize = (int) (texSize * RWR_UV_DIAMETER_RATIO);
            int offset = (texSize - drawSize) / 2;
            g.drawImage(image, offset, offset, drawSize, drawSize, null);
            g.dispose();
            state.backgroundPixels = new int[texSize * texSize];
            scaled.getRGB(0, 0, texSize, texSize, state.backgroundPixels, 0, texSize);
            state.backgroundLoaded = true;
            state.hasArtisticBg = true;
        } catch (IOException e) {
            state.backgroundPixels = null;
            state.backgroundLoaded = true;
            state.hasArtisticBg = false;
        }
    }

    private static void renderGraphicsFrame(int[] pixels, MCH_RenderRWR.RWRDisplayFrame frame, RwrTexState state) {
        int cx = MCH_TextureRenderUtil.TEX_SIZE / 2;
        int cy = MCH_TextureRenderUtil.TEX_SIZE / 2;

        if (state.hasArtisticBg && state.backgroundPixels != null) {
            System.arraycopy(state.backgroundPixels, 0, pixels, 0, pixels.length);
        } else {
            MCH_TextureRenderUtil.fill(pixels, MCH_TextureRenderUtil.CLEAR_COLOR);
            int radius = (int) (MCH_TextureRenderUtil.TEX_SIZE * (RWR_UV_DIAMETER_RATIO * 0.5D));
            MCH_TextureRenderUtil.fillCircle(pixels, cx, cy, (int)(radius * 0.88D), RING_BG_COLOR);
        }

        if (frame != null && frame.valid) {
            int baseR = (int) (MCH_TextureRenderUtil.TEX_SIZE * (RWR_UV_DIAMETER_RATIO * 0.5D));
            double innerR = baseR * RWR_INNER_RING_RATIO;
            double outerR = baseR * RWR_OUTER_RING_RATIO;

            for (MCH_RenderRWR.RWRDisplayPoint point : frame.points) {
                int opaqueColor = point.color | 0xFF000000;
                double rangeNorm = MCH_TextureRenderUtil.clamp(point.rangeNorm, 0.0D, 1.0D);
                double r = innerR + (outerR - innerR) * rangeNorm;
                int px = cx + (int) Math.round(Math.cos(point.angleRad) * r);
                int py = cy + (int) Math.round(Math.sin(point.angleRad) * r);

                int textX = px - MCH_TextureRenderUtil.textWidth(point.label, 1) / 2;
                int textY = py - 8;
                MCH_TextureRenderUtil.drawText(pixels, point.label, textX, textY, opaqueColor, 1);
            }
        }
    }

    private static class RwrTexState {
        public DynamicTexture texture;
        public ResourceLocation location;
        public int[] pixels;
        public int[] backgroundPixels;
        public UUID aircraftUuid;
        public World world;
        public long lastUpdateTick = -1L;
        public int warmupCursor = 0;
        public boolean ready = false;
        public boolean clearUploaded = false;
        public boolean backgroundLoaded = false;
        public boolean hasArtisticBg = false;
        public boolean hasRenderedFrame = false;
        public boolean lastFrameHadThreats = false;

        public boolean matches(UUID uuid, World world) {
            return this.world == world && this.aircraftUuid != null && this.aircraftUuid.equals(uuid);
        }
    }
}
