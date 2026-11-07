package mcheli.aircraft;

import mcheli.MCH_I18n;
import mcheli.MCH_MOD;
import mcheli.MCH_PacketIndOpenScreen;
import mcheli.weapon.MCH_WeaponDummy;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponSet;
import mcheli.wrapper.W_GuiContainer;
import mcheli.wrapper.W_McClient;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MCH_AircraftGui extends W_GuiContainer {

    public static final int BUTTON_RELOAD = 1;
    public static final int BUTTON_NEXT = 2;
    public static final int BUTTON_PREV = 3;
    public static final int BUTTON_CLOSE = 4;
    public static final int BUTTON_CONFIG = 5;
    public static final int BUTTON_INVENTORY = 6;
    private final EntityPlayer thePlayer;
    private final MCH_EntityAircraft aircraft;
    private GuiButton buttonReload;
    private GuiButton buttonNext;
    private GuiButton buttonPrev;
    private GuiButton buttonInventory;
    private int currentWeaponId;
    private int reloadWait;


    public MCH_AircraftGui(EntityPlayer player, MCH_EntityAircraft ac) {
        super(new MCH_AircraftGuiContainer(player, ac));
        this.aircraft = ac;
        this.thePlayer = player;
        super.xSize = 210;
        super.ySize = 236;
        this.buttonReload = null;
        this.currentWeaponId = 0;
    }

    public void initGui() {
        super.initGui();
        if (this.currentWeaponId < 0 || this.currentWeaponId >= this.getPageCount()) this.currentWeaponId = 0;
        super.buttonList.clear();
        this.buttonReload = new GuiButton(1, super.guiLeft + 85, super.guiTop + 40, 50, 20, MCH_I18n.format("gui.mcheli.reload"));
        this.buttonNext = new GuiButton(3, super.guiLeft + 140, super.guiTop + 40, 20, 20, "<<");
        this.buttonPrev = new GuiButton(2, super.guiLeft + 160, super.guiTop + 40, 20, 20, ">>");
        this.buttonReload.enabled = this.canReload(this.thePlayer);
        this.buttonNext.enabled = this.getPageCount() > 1;
        this.buttonPrev.enabled = this.getPageCount() > 1;
        this.buttonInventory = new GuiButton(6, super.guiLeft + 25, super.guiTop + 215, 80, 16,
            MCH_I18n.format("gui.mcheli.inventory"));
        super.buttonList.add(new GuiButton(5, super.guiLeft + 110, super.guiTop + 215, 80, 16,
            MCH_I18n.format("gui.mcheli.mod_options")));
        super.buttonList.add(new GuiButton(4, super.guiLeft + 210 - 30 - 20, super.guiTop + 10, 40, 20, MCH_I18n.format("gui.mcheli.close")));
        super.buttonList.add(this.buttonReload);
        super.buttonList.add(this.buttonNext);
        super.buttonList.add(this.buttonPrev);
        if (this.aircraft != null && this.aircraft.getSizeInventory() > 0) {
            super.buttonList.add(this.buttonInventory);
        }

        this.reloadWait = 10;
    }

    public void closeScreen() {
        super.mc.thePlayer.closeScreen();
    }

    public boolean canReload(EntityPlayer player) {
        return this.isCountermeasuresPage() ? this.aircraft.canPlayerRearmCountermeasures(player)
            : this.aircraft.getWeaponNum() > 0
            && this.aircraft.canPlayerSupplyAmmo(player, this.currentWeaponId);
    }

    int getPageCount() {
        return this.aircraft.getWeaponNum() + 1;
    }

    boolean isCountermeasuresPage() {
        return this.currentWeaponId == this.aircraft.getWeaponNum();
    }

    public void updateScreen() {
        super.updateScreen();
        if (this.isCountermeasuresPage()) this.buttonReload.enabled = this.canReload(this.thePlayer);
        if (this.reloadWait > 0) {
            --this.reloadWait;
            if (this.reloadWait == 0) {
                this.buttonReload.enabled = this.canReload(this.thePlayer);
                this.reloadWait = 20;
            }
        }
    }

    protected void mouseClicked(int p_73864_1_, int p_73864_2_, int p_73864_3_) {
        super.mouseClicked(p_73864_1_, p_73864_2_, p_73864_3_);
    }

    public void onGuiClosed() {
        super.onGuiClosed();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (this.isCountermeasuresPage()
            && mouseX >= this.buttonReload.xPosition && mouseX < this.buttonReload.xPosition + this.buttonReload.width
            && mouseY >= this.buttonReload.yPosition && mouseY < this.buttonReload.yPosition + this.buttonReload.height) {
            this.drawWrappedTooltip(java.util.Arrays.asList(
                MCH_I18n.format("gui.mcheli.countermeasure_reload_cost"),
                MCH_I18n.format("gui.mcheli.countermeasure_reload_effect")), mouseX, mouseY);
        } else if (!this.isCountermeasuresPage() && mouseX >= this.guiLeft + 79 && mouseX < this.guiLeft + 201
            && mouseY >= this.guiTop + 30 && mouseY < this.guiTop + 39) {
            MCH_WeaponSet weapon = this.aircraft.getWeapon(this.currentWeaponId);
            if (weapon != null && this.getStringWidth(this.getWeaponDisplayName(weapon)) > 122) {
                this.drawWrappedTooltip(java.util.Collections.singletonList(this.getWeaponDisplayName(weapon)), mouseX, mouseY);
            }
        }
    }

    private void drawWrappedTooltip(List<String> lines, int mouseX, int mouseY) {
        int maxWidth = Math.max(1, Math.min(200, Math.max(mouseX - 16, this.width - mouseX - 12) - 8));
        List<String> wrapped = new ArrayList<String>();
        for (String line : lines) wrapped.addAll(this.fontRendererObj.listFormattedStringToWidth(line, maxWidth));
        this.drawHoveringText(wrapped, mouseX, mouseY, this.fontRendererObj);
    }

    protected void actionPerformed(GuiButton button) {
        super.actionPerformed(button);
        if (button.enabled) {
            switch (button.id) {
                case 1:
                    this.buttonReload.enabled = this.canReload(this.thePlayer);
                    if (this.buttonReload.enabled) {
                        if (this.isCountermeasuresPage()) {
                            this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId,
                                MCH_AircraftGuiContainer.RELOAD_COUNTERMEASURES);
                        } else {
                            MCH_PacketIndReload.send(this.aircraft, this.currentWeaponId);
                            this.aircraft.supplyAmmo(this.currentWeaponId);
                        }
                        this.reloadWait = 3;
                        this.buttonReload.enabled = false;
                    }
                    break;
                case 2:
                    ++this.currentWeaponId;
                    if (this.currentWeaponId >= this.getPageCount()) {
                        this.currentWeaponId = 0;
                    }

                    this.buttonReload.enabled = this.canReload(this.thePlayer);
                    break;
                case 3:
                    --this.currentWeaponId;
                    if (this.currentWeaponId < 0) {
                        this.currentWeaponId = this.getPageCount() - 1;
                    }

                    this.buttonReload.enabled = this.canReload(this.thePlayer);
                    break;
                case 4:
                    this.closeScreen();
                    break;
                case 5:
                    MCH_PacketIndOpenScreen.send(2);
                    break;
                case 6:
                    MCH_PacketIndOpenScreen.send(3);
                    break;
            }

        }
    }

    protected void drawGuiContainerForegroundLayer(int par1, int par2) {
        super.drawGuiContainerForegroundLayer(par1, par2);
        MCH_EntityAircraft ac = this.aircraft;
        this.drawPanelLabel(ac.getGuiInventory().getInventoryName(), 10, 10, 144);
        if (this.aircraft.getNumEjectionSeat() > 0) {
            this.drawPanelLabel(MCH_I18n.format("gui.mcheli.parachute"), 9, 95, 66);
        }

        if (this.isCountermeasuresPage()) {
            this.drawCountermeasuresPage();
        } else if (this.aircraft.getWeaponNum() > 0) {
            MCH_WeaponSet ws = this.aircraft.getWeapon(this.currentWeaponId);
            if (ws != null && !(ws.getFirstWeapon() instanceof MCH_WeaponDummy)) {
                this.drawPanelLabel(this.getWeaponDisplayName(ws), 79, 30, 122);
                int rest = ws.getRestAllAmmoNum() + ws.getAmmoNum();
                int color = rest == 0 ? 16711680 : (rest == ws.getAllAmmoNum() ? 2675784 : 4210752);
                String s = String.format("%4d/%4d", Integer.valueOf(rest), Integer.valueOf(ws.getAllAmmoNum()));
                this.fontRendererObj.drawString(s, 200 - this.getStringWidth(s), 70, color);
                int itemPosX = 90;

                Iterator i$;
                MCH_WeaponInfo.RoundItem r;
                for (i$ = ws.getInfo().roundItems.iterator(); i$.hasNext(); itemPosX += 20) {
                    r = (MCH_WeaponInfo.RoundItem) i$.next();
                    this.drawPanelLabel("" + r.num, itemPosX, 80, 20);
                }

                itemPosX = 85;

                for (i$ = ws.getInfo().roundItems.iterator(); i$.hasNext(); itemPosX += 20) {
                    r = (MCH_WeaponInfo.RoundItem) i$.next();
                    this.drawReloadItem(r.itemStack, itemPosX, 62);
                }
            }
        } else {
            this.drawString("None", 79, 45, 16777215);
        }

    }

    private String getWeaponDisplayName(MCH_WeaponSet weapon) {
        String name = weapon.getName();
        if (weapon.getInfo() != null && weapon.getInfo().enableDataLink
            && (weapon.getInfo().onlyDataLink || weapon.isDataLinkMode())) {
            String lang = this.mc.gameSettings.language != null
                ? this.mc.gameSettings.language.toLowerCase(java.util.Locale.ROOT) : "en_us";
            name += lang.startsWith("zh") ? " [数据链]" : " [DL]";
        }
        return name;
    }

    private void drawCountermeasuresPage() {
        this.drawPanelLabel(MCH_I18n.format("gui.mcheli.countermeasures"), 79, 30, 122);
        MCH_EntityAircraft ac = this.aircraft;
        int capacity = ac.getFlareCapacity() + ac.getChaffCapacity();
        if (capacity <= 0) {
            this.fontRendererObj.drawSplitString(MCH_I18n.format("gui.mcheli.countermeasures_not_equipped"),
                79, 68, 122, 4210752);
            return;
        }
        if (MCH_MOD.itemCountermeasureReload != null) {
            this.drawReloadItem(new ItemStack(MCH_MOD.itemCountermeasureReload), 85, 62);
        }
        this.drawPanelLabel("1", 90, 80, 20);
        String stock = String.format("%d/%d", ac.getRemainingFlarePairs() + ac.getRemainingChaffPairs(), capacity);
        this.fontRendererObj.drawString(stock, 200 - this.getStringWidth(stock), 70, 4210752);
        this.drawPanelLabel(MCH_I18n.format("gui.mcheli.flare_stock",
            ac.getRemainingFlarePairs(), ac.getFlareCapacity()), 79, 94, 122);
        this.drawPanelLabel(MCH_I18n.format("gui.mcheli.chaff_stock",
            ac.getRemainingChaffPairs(), ac.getChaffCapacity()), 79, 106, 122);
    }

    private void drawPanelLabel(String text, int x, int y, int maxWidth) {
        if (this.getStringWidth(text) > maxWidth) {
            text = this.fontRendererObj.trimStringToWidth(text, maxWidth - this.getStringWidth("...")) + "...";
        }
        this.fontRendererObj.drawString(text, x, y, 4210752);
    }

    private void drawReloadItem(ItemStack stack, int x, int y) {
        // The legacy item helper enables lighting/depth and resets shared GUI Z levels.
        // Isolate it so subsequent labels and other pages keep vanilla GUI colours/depth.
        float guiZ = this.zLevel;
        float itemZ = itemRender.zLevel;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LIGHTING_BIT | GL11.GL_DEPTH_BUFFER_BIT
            | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT);
        try {
            RenderHelper.enableGUIStandardItemLighting();
            this.drawItemStack(stack, x, y);
        } finally {
            this.zLevel = guiZ;
            itemRender.zLevel = itemZ;
            GL11.glPopAttrib();
        }
    }

    protected void keyTyped(char c, int code) {
        if (code == 1) {
            this.closeScreen();
        } else if (code == 28) {
            // String s = this.editCommand.getText().trim();
            // if(s.startsWith("/")) {
            //    s = s.substring(1);
            // }

            //   if(!s.isEmpty()) {
            //      //MCH_PacketIndMultiplayCommand.send(768, s);
            //   }
        } else {
            //this.editCommand.textboxKeyTyped(c, code);
        }

    }

    protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) {
        W_McClient.MOD_bindTexture("textures/gui/gui.png");
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (super.width - super.xSize) / 2;
        int y = (super.height - super.ySize) / 2;
        this.drawTexturedModalRect(x, y, 0, 0, super.xSize, super.ySize);

        int ff;
        for (ff = 0; ff < this.aircraft.getNumEjectionSeat(); ++ff) {
            this.drawTexturedModalRect(x + 10 + 18 * ff - 1, y + 105 - 1, 215, 55, 18, 18);
        }

        ff = (int) (this.aircraft.getFuelP() * 50.0F);
        if (ff >= 99) {
            ff = 100;
        }

        this.drawTexturedModalRect(x + 57, y + 30 + 50 - ff, 215, 0, 12, ff);
        ff = (int) ((double) (this.aircraft.getFuelP() * 100.0F) + 0.5D);
        int color = ff > 20 ? -14101432 : 16711680;
        this.drawString(String.format("%3d", ff) + "%", x + 30, y + 65, color);
    }
}
