package mcheli.economy;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

public class MCH_ContainerEconomyTechTree extends Container {

    public MCH_ContainerEconomyTechTree(InventoryPlayer playerInventory) {
        // Tech tree UI does not use inventory slots; keep container slotless to
        // avoid hidden slot-click transactions interfering with purchases.
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }
}
