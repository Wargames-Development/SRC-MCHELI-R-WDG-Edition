package mcheli.aircraft;

import mcheli.MCH_Config;
import mcheli.MCH_I18n;
import mcheli.MCH_MOD;
import mcheli.tank.MCH_EntityTank;
import mcheli.tank.MCH_TankInfo;
import mcheli.weapon.*;
import mcheli.wrapper.W_Item;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.*;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.*;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.*;
import net.minecraft.entity.DataWatcher;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.objectweb.asm.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.util.*;

/** Explicit offscreen audit of the real GUI, vanilla font/buttons and the user's pack texture. */
public final class CountermeasureGuiAudit {
    private static Minecraft mc;
    private static WorldClient world;
    private static SimpleReloadableResourceManager resources;
    private static File output;

    public static void main(String[] args) throws Exception {
        // Match the existing headless test boundary: suppress only MCHR mod startup.
        ClassLoader loader = new ClassLoader(CountermeasureGuiAudit.class.getClassLoader()) {
            @Override protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.startsWith("mcheli.")) return super.loadClass(name, resolve);
                Class<?> type = findLoadedClass(name);
                if (type == null) try (InputStream in = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                    if (in == null) throw new ClassNotFoundException(name);
                    ClassWriter writer = new ClassWriter(0);
                    new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM5, writer) {
                        @Override public MethodVisitor visitMethod(int access, String method, String descriptor, String signature, String[] exceptions) {
                            return name.equals("mcheli.MCH_MOD") && method.equals("<clinit>") ? null
                                : super.visitMethod(access, method, descriptor, signature, exceptions);
                        }
                    }, 0);
                    byte[] bytes = writer.toByteArray(); type = defineClass(name, bytes, 0, bytes.length);
                } catch (IOException failed) { throw new ClassNotFoundException(name, failed); }
                if (resolve) resolveClass(type);
                return type;
            }
        };
        try { loader.loadClass(CountermeasureGuiAudit.class.getName()).getMethod("run", String[].class).invoke(null, (Object)args); }
        catch (InvocationTargetException failed) { throw new RuntimeException(failed.getCause()); }
    }

    public static void run(String[] args) throws Exception {
        File pack = new File(args[0]), repo = new File(args[1]); output = new File(args[2]); output.mkdirs();
        Pbuffer buffer = new Pbuffer(2560, 1440, new PixelFormat(), null, null);
        try {
            buffer.makeCurrent();
            new MCH_Config(".", "unused.cfg");
            mc = allocate(Minecraft.class); set(Minecraft.class, null, "theMinecraft", mc);
            mc.gameSettings = new GameSettings();
            world = allocate(WorldClient.class); world.isRemote = true; mc.theWorld = world;
            mc.thePlayer = allocate(EntityClientPlayerMP.class); mc.thePlayer.setEntityId(1);
            mc.thePlayer.worldObj = world; mc.thePlayer.inventory = new InventoryPlayer(mc.thePlayer);
            mc.thePlayer.capabilities = new PlayerCapabilities();
            DataWatcher watcher = new DataWatcher(mc.thePlayer); watcher.addObject(6, 20.0F);
            set(Entity.class, mc.thePlayer, "dataWatcher", watcher);
            mc.playerController = new AuditController(mc);
            resources = new SimpleReloadableResourceManager(new IMetadataSerializer());
            File vanilla = new File(System.getProperty("user.home"), ".gradle/caches/minecraft/net/minecraft/minecraft/1.7.10/minecraft-1.7.10.jar");
            resources.reloadResourcePack(new FileResourcePack(vanilla));
            resources.reloadResourcePack(new FolderResourcePack(new File(pack, "mods/MCHELI-R-WDG-Edition-1.1.8_X194")));
            resources.reloadResourcePack(new FolderResourcePack(new File(repo, "src/main/resources")));
            resources.reloadResourcePack(new FolderResourcePack(new File(pack, "resourcepacks/MCHR Resourcepack")));
            set(Minecraft.class, mc, "mcResourceManager", resources);
            set(Minecraft.class, mc, "mcLanguageManager", new LanguageManager(new IMetadataSerializer(), "en_US"));
            mc.renderEngine = new TextureManager(resources);
            mc.fontRenderer = new FontRenderer(mc.gameSettings, new ResourceLocation("textures/font/ascii.png"), mc.renderEngine, false);
            mc.fontRenderer.onResourceManagerReload(resources);
            set(GuiScreen.class, null, "itemRender", new AuditItemRenderer());
            MCH_MOD.itemCountermeasureReload = new W_Item();
            MCH_I18n.register();
            language("en_US");

            render("weapon-standard", 1920, 1080, 4, true, true, 1, 36, 128, 5, 19, true, 0, false);
            render("countermeasures-partial", 1920, 1080, 4, true, true, 1, 36, 128, 5, 19, true, 1, false);
            render("countermeasures-full", 1920, 1080, 4, true, true, 1, 36, 128, 36, 128, true, 1, false);
            render("countermeasures-no-item", 1920, 1080, 4, true, true, 1, 36, 128, 5, 19, false, 1, true);
            render("countermeasures-flare-only", 1280, 720, 3, true, true, 0, 36, 0, 5, 0, true, 0, false);
            render("countermeasures-chaff-only", 1280, 720, 3, true, true, 0, 0, 128, 0, 19, true, 0, false);
            render("countermeasures-not-equipped", 1920, 1080, 4, false, true, 1, 0, 0, 0, 0, true, 1, false);
            render("weaponless-countermeasures", 1920, 1080, 4, true, false, 0, 36, 128, 5, 19, true, 0, false);
            render("long-names-large-counts", 2560, 1440, 4, true, true, 1, 10000, 10000, 9999, 1, true, 1, false);
            render("minimum-320x240", 320, 240, 1, true, true, 1, 36, 128, 5, 19, true, 1, false);
            render("minimum-tooltip", 320, 240, 1, true, true, 1, 36, 128, 5, 19, true, 1, true);
            render("weapon-name-tooltip", 1920, 1080, 4, true, true, 1, 36, 128, 5, 19, true, 0, true);
            render("weapon-without-countermeasures", 1920, 1080, 4, false, true, 1, 0, 0, 0, 0, true, 0, false);
            render("countermeasures-full-inventory", 1920, 1080, 4, true, true, 1, 36, 128, 5, 19, true, 1, false);
            render("weaponless-not-equipped", 1920, 1080, 4, false, false, 0, 0, 0, 0, 0, true, 0, false);
            render("auto-scale", 2560, 1440, 0, true, true, 1, 36, 128, 5, 19, true, 1, false);
            render("scale-1", 854, 480, 1, true, true, 1, 36, 128, 5, 19, true, 1, false);
            render("scale-2", 1280, 720, 2, true, true, 1, 36, 128, 5, 19, true, 1, false);
            language("zh_CN"); mc.fontRenderer.setUnicodeFlag(true);
            render("countermeasures-chinese", 1920, 1080, 4, true, true, 1, 36, 128, 5, 19, true, 1, false);
            contactSheet();
            System.out.println("Rendered 19 actual GUI cases; page navigation, resize retention, button bounds and reload routing verified.");
        } finally { buffer.destroy(); }
    }

    @SuppressWarnings("unchecked") private static void language(String lang) throws Exception {
        net.minecraft.client.resources.Locale locale = new net.minecraft.client.resources.Locale();
        locale.loadLocaleDataFiles(resources, Arrays.asList("en_US", lang));
        Map<String, String> translations = (Map<String, String>)get(net.minecraft.client.resources.Locale.class, locale, "field_135032_a");
        Map<String, ArrayList<String>> registered = (Map<String, ArrayList<String>>)get(mcheli.wrapper.W_LanguageRegistry.class, null, "map");
        for (String entry : registered.get(lang)) { int split = entry.indexOf('='); translations.put(entry.substring(0, split), entry.substring(split + 1)); }
        set(I18n.class, null, "i18nLocale", locale);
    }

    private static void render(String name, int width, int height, int scale, boolean cm, boolean inventory,
            int weapons, int flares, int chaff, int remainingFlares, int remainingChaff, boolean pack, int page, boolean tooltip) throws Exception {
        mc.displayWidth = width; mc.displayHeight = height; mc.gameSettings.guiScale = scale;
        ScaledResolution resolution = new ScaledResolution(mc, width, height);
        AuditAircraft ac = allocate(AuditAircraft.class); ac.worldObj = world; ac.setEntityId(10);
        ac.info = new MCH_TankInfo("audit"); ac.info.displayName = name.startsWith("long") ? "An unusually long vehicle display name" : "Wargames Vehicle";
        ac.info.flare.types = cm && flares > 0 ? new int[]{1} : new int[0];
        ac.info.chaff = cm && chaff > 0 ? ac.info.new Chaff() : null;
        ac.info.flareCapacity = flares; ac.info.chaffCapacity = chaff;
        ac.inv = new MCH_AircraftInventory(ac); ac.storage = inventory;
        ac.riddenByEntity = mc.thePlayer; mc.thePlayer.ridingEntity = ac;
        ac.setCountermeasureStateClient(remainingFlares, remainingChaff);
        Arrays.fill(mc.thePlayer.inventory.mainInventory, null);
        if (pack) mc.thePlayer.inventory.mainInventory[0] = new ItemStack(MCH_MOD.itemCountermeasureReload, 2);
        if (name.endsWith("full-inventory")) for (int i = 0; i < 36; i++)
            mc.thePlayer.inventory.mainInventory[i] = new ItemStack(MCH_MOD.itemCountermeasureReload, 64);
        ac.weapons = new MCH_WeaponSet[weapons];
        for (int i = 0; i < weapons; i++) {
            MCH_WeaponInfo info = new MCH_WeaponInfo("audit_weapon"); info.displayName = "An extremely long missile name [1100m]";
            info.round = 10; info.maxAmmo = 100;
            info.roundItems.add(info.new RoundItem(1, "countermeasure_reload", 0));
            ((MCH_WeaponInfo.RoundItem)info.roundItems.get(0)).itemStack = new ItemStack(MCH_MOD.itemCountermeasureReload);
            ac.weapons[i] = new MCH_WeaponSet(new MCH_WeaponRocket(world, Vec3.createVectorHelper(0, 0, 0), 0, 0, info.name, info));
        }
        AuditGui gui = new AuditGui(mc.thePlayer, ac);
        gui.setWorldAndResolution(mc, resolution.getScaledWidth(), resolution.getScaledHeight());
        set(MCH_AircraftGui.class, gui, "currentWeaponId", page);
        gui.setWorldAndResolution(mc, resolution.getScaledWidth(), resolution.getScaledHeight());
        if ((Integer)get(MCH_AircraftGui.class, gui, "currentWeaponId") != page) throw new AssertionError("resize lost selected page");
        gui.updateScreen();
        if (gui.getPageCount() != weapons + 1 || gui.isCountermeasuresPage() != (page == weapons)) throw new AssertionError(name);
        gui.verifyLayout();
        GL11.glViewport(0, 0, width, height);
        GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glLoadIdentity();
        GL11.glOrtho(0, resolution.getScaledWidth(), resolution.getScaledHeight(), 0, 1000, 3000);
        GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glLoadIdentity(); GL11.glTranslatef(0, 0, -2000);
        GL11.glClearColor(0.18F, 0.2F, 0.23F, 1); GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_ALPHA_TEST); GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
        int mouseX = tooltip ? gui.left() + 110 : -10;
        int mouseY = tooltip ? gui.top() + (name.equals("weapon-name-tooltip") ? 34 : 50) : -10;
        gui.drawScreen(mouseX, mouseY, 0);
        GL11.glFinish();
        ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 4);
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int offset = ((height - y - 1) * width + x) * 4;
            image.setRGB(x, y, 0xFF000000 | (pixels.get(offset) & 255) << 16 | (pixels.get(offset + 1) & 255) << 8 | pixels.get(offset + 2) & 255);
        }
        ImageIO.write(image, "png", new File(output, name + ".png"));
        double pixelScaleX = width / (double)resolution.getScaledWidth();
        double pixelScaleY = height / (double)resolution.getScaledHeight();
        ImageIO.write(image.getSubimage((int)(gui.left() * pixelScaleX), (int)(gui.top() * pixelScaleY),
            (int)(210 * pixelScaleX), (int)(236 * pixelScaleY)), "png", new File(output, name + ".panel.png"));
        int previous = page;
        for (int i = 0; i < gui.getPageCount(); i++) gui.press(MCH_AircraftGui.BUTTON_NEXT);
        if ((Integer)get(MCH_AircraftGui.class, gui, "currentWeaponId") != previous) throw new AssertionError("page wrap");
        gui.verifyLayout();
        if (page == weapons && ac.canPlayerRearmCountermeasures(mc.thePlayer)) {
            AuditController controller = (AuditController)mc.playerController;
            int before = controller.requests; gui.press(MCH_AircraftGui.BUTTON_RELOAD);
            if (controller.requests != before + 1 || controller.action != MCH_AircraftGuiContainer.RELOAD_COUNTERMEASURES)
                throw new AssertionError("countermeasure reload routed to weapon ammunition");
        }
    }

    private static void contactSheet() throws IOException {
        String[] names = {"countermeasures-partial", "countermeasures-full", "countermeasures-not-equipped",
            "weapon-standard", "weaponless-countermeasures", "countermeasures-full-inventory"};
        BufferedImage sheet = new BufferedImage(1260, 1000, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics = sheet.createGraphics();
        graphics.setColor(new java.awt.Color(32, 35, 40)); graphics.fillRect(0, 0, 1260, 1000);
        graphics.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 14));
        graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (int i = 0; i < names.length; i++) {
            int x = (i % 3) * 420, y = (i / 3) * 500;
            graphics.setColor(java.awt.Color.WHITE); graphics.drawString(names[i], x + 10, y + 19);
            graphics.drawImage(ImageIO.read(new File(output, names[i] + ".panel.png")), x, y + 26, 420, 472, null);
        }
        graphics.dispose(); ImageIO.write(sheet, "png", new File(output, "audit-contact-sheet.png"));
    }

    private static Object get(Class<?> type, Object object, String name) throws Exception { Field f = type.getDeclaredField(name); f.setAccessible(true); return f.get(object); }
    private static void set(Class<?> type, Object object, String name, Object value) throws Exception { Field f = type.getDeclaredField(name); f.setAccessible(true); f.set(object, value); }
    private static <T> T allocate(Class<T> type) throws Exception {
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true);
        return type.cast(((sun.misc.Unsafe)field.get(null)).allocateInstance(type));
    }

    private static final class AuditAircraft extends MCH_EntityTank {
        MCH_TankInfo info; MCH_AircraftInventory inv; boolean storage;
        AuditAircraft(World world) { super(world); }
        @Override public MCH_AircraftInfo getAcInfo() { return info; }
        @Override public MCH_AircraftInventory getGuiInventory() { return inv; }
        @Override public int getSizeInventory() { return storage ? 9 : 0; }
        @Override public int getNumEjectionSeat() { return 2; }
        @Override public float getFuelP() { return 0.6F; }
        @Override public boolean canSupply() { return true; }
        @Override public boolean canPlayerSupplyAmmo(EntityPlayer player, int id) { return false; }
    }
    private static final class AuditController extends PlayerControllerMP {
        int requests, action;
        AuditController(Minecraft mc) { super(mc, null); }
        @Override public void sendEnchantPacket(int window, int action) { requests++; this.action = action; }
    }
    private static final class AuditGui extends MCH_AircraftGui {
        AuditGui(EntityPlayer player, MCH_EntityAircraft aircraft) { super(player, aircraft); }
        int left() { return guiLeft; } int top() { return guiTop; }
        void press(int id) { for (Object object : buttonList) if (((GuiButton)object).id == id) { actionPerformed((GuiButton)object); return; } }
        void verifyLayout() {
            for (Object object : buttonList) {
                GuiButton b = (GuiButton)object;
                if (b.xPosition < guiLeft || b.yPosition < guiTop || b.xPosition + b.width > guiLeft + xSize
                    || b.yPosition + b.height > guiTop + ySize) throw new AssertionError("button outside panel: " + b.id);
                if (b.id == BUTTON_CONFIG && (b.xPosition != guiLeft + 110 || b.yPosition != guiTop + 215)) throw new AssertionError("moving footer");
            }
            for (Object object : inventorySlots.inventorySlots) {
                Slot s = (Slot)object;
                if (s.xDisplayPosition < 0 || s.yDisplayPosition < 0 || s.xDisplayPosition + 16 > xSize || s.yDisplayPosition + 16 > ySize)
                    throw new AssertionError("slot outside panel");
            }
        }
    }
    private static final class AuditItemRenderer extends RenderItem {
        @Override public void renderItemAndEffectIntoGUI(FontRenderer font, TextureManager textures, ItemStack stack, int x, int y) {
            if (stack == null) return;
            // Same source icon, rendered directly so no item-atlas/Forge startup is required.
            textures.bindTexture(new ResourceLocation("mcheli", "textures/items/countermeasure_reload.png"));
            GL11.glDisable(GL11.GL_LIGHTING); GL11.glColor4f(1, 1, 1, 1);
            Tessellator t = Tessellator.instance; t.startDrawingQuads();
            t.addVertexWithUV(x, y + 16, 0, 0, 1); t.addVertexWithUV(x + 16, y + 16, 0, 1, 1);
            t.addVertexWithUV(x + 16, y, 0, 1, 0); t.addVertexWithUV(x, y, 0, 0, 0); t.draw();
        }
    }
}
