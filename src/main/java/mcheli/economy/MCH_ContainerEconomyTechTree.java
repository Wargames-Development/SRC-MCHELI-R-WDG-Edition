package mcheli.economy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

public class MCH_ContainerEconomyTechTree extends Container {

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }
}
