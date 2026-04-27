package mcheli.mob;

import mcheli.MCH_Config;
import mcheli.MCH_MOD;
import mcheli.MCH_PacketIndOpenScreen;
import mcheli.MCH_Lib;
import mcheli.economy.MCH_EconomyClientData;
import mcheli.economy.MCH_EconomyService;
import mcheli.economy.MCH_EconomyTechService;
import mcheli.gui.MCH_GuiCommonHandler;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

public class MCH_EntityNPC extends EntityVillager {

    public MCH_EntityNPC(World world) {
        super(world, 0);
        this.setCustomNameTag(MCH_TechNpcConfig.getDisplayName());
        this.setAlwaysRenderNameTag(true);
    }

    private boolean openTechTree(EntityPlayer player) {
        if (player == null) {
            return false;
        }
        if (MCH_MOD.config == null || !MCH_Config.EnableTechTreeGameplay.prmBool) {
            if (!this.worldObj.isRemote) {
                player.addChatMessage(new net.minecraft.util.ChatComponentText("科技树玩法当前未启用。"));
            }
            return true;
        }
        if (this.worldObj.isRemote) {
            String techTreeId = MCH_TechNpcConfig.getTechTreeId();
            String techTreeIdsRaw = MCH_TechNpcConfig.getTechTreeIdsRaw();
            MCH_EconomyClientData.updateActiveTechTreeId(techTreeId);
            MCH_EconomyClientData.updateAllowedTechTreeIds(techTreeIdsRaw);
            MCH_Lib.DbgTrace(
                this.worldObj,
                "event=npc_interact entityId=%d player=%s guiId=%d treeId=%s treeIds=%s pos=%.2f,%.2f,%.2f",
                this.getEntityId(),
                player.getCommandSenderName(),
                MCH_GuiCommonHandler.GUIID_ECONOMY_TECH_TREE,
                techTreeId,
                techTreeIdsRaw,
                this.posX,
                this.posY,
                this.posZ
            );
            MCH_PacketIndOpenScreen.send(MCH_GuiCommonHandler.GUIID_ECONOMY_TECH_TREE);
            return true;
        }
        if (player instanceof EntityPlayerMP) {
            String techTreeId = MCH_TechNpcConfig.getTechTreeId();
            String techTreeIdsRaw = MCH_TechNpcConfig.getTechTreeIdsRaw();
            MCH_EconomyTechService.setActiveTechTreeId((EntityPlayerMP) player, techTreeId);
            MCH_EconomyTechService.setAllowedTechTreeIdsRaw((EntityPlayerMP) player, techTreeIdsRaw);
            MCH_EconomyService.syncToClient((EntityPlayerMP) player);
            MCH_Lib.DbgTrace(
                this.worldObj,
                "event=npc_interact entityId=%d player=%s guiId=%d treeId=%s treeIds=%s pos=%.2f,%.2f,%.2f",
                this.getEntityId(),
                player.getCommandSenderName(),
                MCH_GuiCommonHandler.GUIID_ECONOMY_TECH_TREE,
                techTreeId,
                techTreeIdsRaw,
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
