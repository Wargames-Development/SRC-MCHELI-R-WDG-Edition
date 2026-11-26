package mcheli.wrapper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.TextureMetadataSection;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.opengl.GL11;

/** Resolves pack resources on the client and hands each opened stream to one decoder. */
@SideOnly(Side.CLIENT)
public final class ClientTextureWarmup {
    private static final int UPLOAD_PIXELS_PER_FRAME = 1024 * 1024;
    private final Set<ResourceLocation> queued = new LinkedHashSet<>();
    private final Set<ResourceLocation> priority = new LinkedHashSet<>();
    private final Set<ResourceLocation> failed = new LinkedHashSet<>();
    private final Executor decoder;
    private CompletableFuture<PreparedTexture> pending;
    private PreparedTexture uploading;
    private ResourceLocation decoding;
    private boolean discarded;

    public ClientTextureWarmup(String threadName) {
        decoder = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, threadName);
            thread.setDaemon(true);
            return thread;
        });
    }

    public void reset() {
        queued.clear();
        priority.clear();
        failed.clear();
        discarded = pending != null;
        if (uploading != null) {
            uploading.deleteGlTexture();
            uploading = null;
            decoding = null;
        }
    }

    public boolean request(TextureManager textures, ResourceLocation location, boolean urgent) {
        ITextureObject texture = textures.getTexture(location);
        if (texture != null && texture != TextureUtil.missingTexture) return true;
        if (failed.contains(location)) return texture != null;
        if (discarded || !location.equals(decoding)) {
            if (urgent) {
                if (priority.size() < 256) {
                    queued.remove(location);
                    priority.add(location);
                }
            } else if (!priority.contains(location)) {
                queued.add(location);
            }
        }
        return false;
    }

    public void uploadReady(Minecraft mc) {
        TextureManager textures = mc.getTextureManager();
        if (textures == null) return;
        if (pending != null) {
            if (!pending.isDone()) return;
            uploading = pending.getNow(null);
            pending = null;
        }
        if (decoding != null) {
            if (!discarded && !isResident(textures, decoding)) {
                if (uploading == null) markMissing(textures, decoding);
                else {
                    int binding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
                    try {
                        if (!uploading.uploadNextSlice()) return;
                        textures.loadTexture(decoding, uploading);
                    } finally {
                        GL11.glBindTexture(GL11.GL_TEXTURE_2D, binding);
                    }
                }
            } else if (uploading != null) {
                uploading.deleteGlTexture();
            }
            uploading = null;
            discarded = false;
            decoding = null;
        }
        Set<ResourceLocation> next = priority.isEmpty() ? queued : priority;
        if (next.isEmpty()) return;
        Iterator<ResourceLocation> iterator = next.iterator();
        ResourceLocation location = iterator.next();
        iterator.remove();
        if (isResident(textures, location)) return;
        try {
            IResource resource = mc.getResourceManager().getResource(location);
            decoding = location;
            pending = CompletableFuture.supplyAsync(() -> decode(location, resource), decoder);
        } catch (IOException | RuntimeException failure) {
            markMissing(textures, location);
        }
    }

    private void markMissing(TextureManager textures, ResourceLocation location) {
        failed.add(location);
        textures.loadTexture(location, TextureUtil.missingTexture);
    }

    private static boolean isResident(TextureManager manager, ResourceLocation location) {
        ITextureObject texture = manager.getTexture(location);
        return texture != null && texture != TextureUtil.missingTexture;
    }

    private static PreparedTexture decode(ResourceLocation location, IResource resource) {
        try (InputStream input = resource.getInputStream()) {
            TextureMetadataSection metadata = null;
            if (resource.hasMetadata()) {
                try {
                    metadata = (TextureMetadataSection) resource.getMetadata("texture");
                } catch (RuntimeException invalid) {
                    LogManager.getLogger().warn("Failed reading metadata of " + location, invalid);
                }
            }
            BufferedImage image = ImageIO.read(input);
            if (image == null) return null;
            return new PreparedTexture(location, image, metadata);
        } catch (IOException | RuntimeException failure) {
            return null;
        }
    }

    private static final class PreparedTexture extends SimpleTexture {
        private final int width, height, rowsPerSlice;
        private final boolean blur, clamp;
        private int[][] slices;
        private int nextSlice;

        PreparedTexture(ResourceLocation location, BufferedImage image, TextureMetadataSection metadata) {
            super(location);
            width = image.getWidth();
            height = image.getHeight();
            rowsPerSlice = Math.max(1, UPLOAD_PIXELS_PER_FRAME / width);
            slices = new int[(height + rowsPerSlice - 1) / rowsPerSlice][];
            for (int slice = 0; slice < slices.length; slice++) {
                int y = slice * rowsPerSlice;
                slices[slice] = image.getRGB(0, y, width, Math.min(rowsPerSlice, height - y), null, 0, width);
            }
            blur = metadata != null && metadata.getTextureBlur();
            clamp = metadata != null && metadata.getTextureClamp();
        }

        boolean uploadNextSlice() {
            int texture = getGlTextureId();
            if (nextSlice == 0) TextureUtil.allocateTexture(texture, width, height);
            else GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            int[] pixels = slices[nextSlice];
            TextureUtil.uploadTextureMipmap(new int[][] { pixels }, width, pixels.length / width,
                    0, nextSlice * rowsPerSlice, blur, clamp);
            slices[nextSlice++] = null;
            return nextSlice == slices.length;
        }

        @Override public void loadTexture(IResourceManager resources) throws IOException {
            // The first TextureManager registration follows the final slice.
            // Subsequent calls are explicit resource reloads.
            if (slices == null) super.loadTexture(resources);
            else slices = null;
        }
    }
}

