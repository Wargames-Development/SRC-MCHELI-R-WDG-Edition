package mcheli.mob;

import mcheli.MCH_MOD;
import mcheli.MCH_PacketIndOpenScreen;
import mcheli.MCH_Lib;
import mcheli.gui.MCH_GuiCommonHandler;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

public class MCH_EntityNPC extends EntityVillager {

    public MCH_EntityNPC(World world) {
        super(world, 0);
        this.setCustomNameTag("科技官");
        this.setAlwaysRenderNameTag(true);
    }

    private boolean openTechTree(EntityPlayer player) {
        if (player == null) {
            return false;
        }
        if (this.worldObj.isRemote) {
            MCH_Lib.DbgTrace(
                this.worldObj,
                "event=npc_interact entityId=%d player=%s guiId=%d pos=%.2f,%.2f,%.2f",
                this.getEntityId(),
                player.getCommandSenderName(),
                MCH_GuiCommonHandler.GUIID_ECONOMY_TECH_TREE,
                this.posX,
                this.posY,
                this.posZ
            );
            MCH_PacketIndOpenScreen.send(MCH_GuiCommonHandler.GUIID_ECONOMY_TECH_TREE);
            return true;
        }
        if (player instanceof EntityPlayerMP) {
            MCH_Lib.DbgTrace(
                this.worldObj,
                "event=npc_interact entityId=%d player=%s guiId=%d pos=%.2f,%.2f,%.2f",
                this.getEntityId(),
                player.getCommandSenderName(),
                MCH_GuiCommonHandler.GUIID_ECONOMY_TECH_TREE,
                this.posX,
                this.posY,
                this.posZ
            );
            player.openGui(MCH_MOD.instance, MCH_GuiCommonHandler.GUIID_ECONOMY_TECH_TREE, this.worldObj,
                (int) this.posX, (int) this.posY, (int) this.posZ);
        }
        return true;
    }

    @Override
    public boolean interact(EntityPlayer player) {
        return this.openTechTree(player);
    }

    @Override
    public EntityVillager createChild(EntityAgeable parent) {
        return new MCH_EntityNPC(this.worldObj);
    }
}
