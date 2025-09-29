package mcheli.gui;

import mcheli.MCH_I18n;
import mcheli.wrapper.W_GuiButton;
import net.minecraft.client.Minecraft;

public class MCH_GuiOnOffButton extends W_GuiButton {

    private final String dispOnOffString;
    private boolean statOnOff;


    public MCH_GuiOnOffButton(int par1, int par2, int par3, int par4, int par5, String par6Str) {
        super(par1, par2, par3, par4, par5, "");
        this.dispOnOffString = par6Str;
        this.setOnOff(false);
    }

    public boolean getOnOff() {
        return this.statOnOff;
    }

    public void setOnOff(boolean b) {
        this.statOnOff = b;
        super.displayString = this.dispOnOffString + (this.getOnOff() ? MCH_I18n.format("gui.mcheli.key.key_on") : MCH_I18n.format("gui.mcheli.key.key_off"));
    }

    public void switchOnOff() {
        this.setOnOff(!this.getOnOff());
    }

    public boolean mousePressed(Minecraft mc, int x, int y) {
        if (super.mousePressed(mc, x, y)) {
            this.switchOnOff();
            return true;
        } else {
            return false;
        }
    }
}
