package mcheli.gui;

import mcheli.MCH_I18n;
import mcheli.weapon.MCH_GPSPosition;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.input.Keyboard;

public class MCH_GuiGPSInput extends GuiScreen {

    private final EntityPlayer player;
    private GuiTextField xField;
    private GuiTextField yField;
    private GuiTextField zField;
    private String message = "";


    public MCH_GuiGPSInput(EntityPlayer player) {
        this.player = player;
    }

    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(3, centerX - 102, centerY + 44, 204, 20, MCH_I18n.format("gui.mcheli.gps_input.fill_player_pos")));
        this.buttonList.add(new GuiButton(1, centerX - 102, centerY + 68, 100, 20, MCH_I18n.format("gui.done")));
        this.buttonList.add(new GuiButton(2, centerX + 2, centerY + 68, 100, 20, MCH_I18n.format("gui.cancel")));
        this.xField = new GuiTextField(this.fontRendererObj, centerX - 100, centerY - 34, 200, 20);
        this.yField = new GuiTextField(this.fontRendererObj, centerX - 100, centerY - 8, 200, 20);
        this.zField = new GuiTextField(this.fontRendererObj, centerX - 100, centerY + 18, 200, 20);
        this.xField.setMaxStringLength(24);
        this.yField.setMaxStringLength(24);
        this.zField.setMaxStringLength(24);
        this.xField.setFocused(true);
    }

    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    public void updateScreen() {
        this.xField.updateCursorCounter();
        this.yField.updateCursorCounter();
        this.zField.updateCursorCounter();
    }

    protected void actionPerformed(GuiButton button) {
        if (!button.enabled) {
            return;
        }
        if (button.id == 2) {
            this.mc.thePlayer.closeScreen();
            return;
        }
        if (button.id == 1) {
            this.applyGPS();
            return;
        }
        if (button.id == 3) {
            this.fillFromPlayerPos();
        }
    }

    protected void keyTyped(char c, int code) {
        if (code == 1) {
            this.mc.thePlayer.closeScreen();
            return;
        }
        if (code == 15) {
            if (this.xField.isFocused()) {
                this.xField.setFocused(false);
                this.yField.setFocused(true);
            } else if (this.yField.isFocused()) {
                this.yField.setFocused(false);
                this.zField.setFocused(true);
            } else {
                this.zField.setFocused(false);
                this.xField.setFocused(true);
            }
            return;
        }
        if (code == 28 || code == 156) {
            this.applyGPS();
            return;
        }
        this.xField.textboxKeyTyped(c, code);
        this.yField.textboxKeyTyped(c, code);
        this.zField.textboxKeyTyped(c, code);
    }

    protected void mouseClicked(int x, int y, int button) {
        super.mouseClicked(x, y, button);
        this.xField.mouseClicked(x, y, button);
        this.yField.mouseClicked(x, y, button);
        this.zField.mouseClicked(x, y, button);
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        this.drawCenteredString(this.fontRendererObj, MCH_I18n.format("gui.mcheli.gps_input.title"), centerX, centerY - 62, 16777215);
        this.drawString(this.fontRendererObj, "X", centerX - 112, centerY - 28, 16777215);
        this.drawString(this.fontRendererObj, "Y", centerX - 112, centerY - 2, 16777215);
        this.drawString(this.fontRendererObj, "Z", centerX - 112, centerY + 24, 16777215);
        this.xField.drawTextBox();
        this.yField.drawTextBox();
        this.zField.drawTextBox();
        if (this.message != null && this.message.length() > 0) {
            this.drawCenteredString(this.fontRendererObj, this.message, centerX, centerY + 94, 16733525);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void applyGPS() {
        try {
            double x = Double.parseDouble(this.xField.getText().trim());
            double y = Double.parseDouble(this.yField.getText().trim());
            double z = Double.parseDouble(this.zField.getText().trim());
            MCH_GPSPosition.set(x, y, z, true, this.player);
            this.mc.thePlayer.closeScreen();
        } catch (NumberFormatException e) {
            this.message = MCH_I18n.format("gui.mcheli.gps_input.invalid");
        }
    }

    private void fillFromPlayerPos() {
        this.xField.setText(String.valueOf(this.player.posX));
        this.yField.setText(String.valueOf(this.player.posY));
        this.zField.setText(String.valueOf(this.player.posZ));
        this.message = "";
    }
}
