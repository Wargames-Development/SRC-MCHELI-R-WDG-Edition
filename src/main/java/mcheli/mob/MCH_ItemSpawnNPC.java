package mcheli.mob;

import mcheli.wrapper.W_Item;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class MCH_ItemSpawnNPC extends W_Item {

    public MCH_ItemSpawnNPC() {
        this.maxStackSize = 64;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (player == null) {
            return stack;
        }
        if (!world.isRemote) {
            double lx = player.getLookVec().xCoord;
            double lz = player.getLookVec().zCoord;
            int x = MathHelper.floor_double(player.posX + lx * 2.0D);
            int z = MathHelper.floor_double(player.posZ + lz * 2.0D);
            int y = MathHelper.floor_double(player.posY);
            while (y < 255 && !world.isAirBlock(x, y, z)) {
                y++;
            }
            MCH_EntityNPC npc = new MCH_EntityNPC(world);
            npc.setLocationAndAngles(x + 0.5D, y, z + 0.5D, player.rotationYaw, 0.0F);
            world.spawnEntityInWorld(npc);
            W_WorldFunc.MOD_playSoundAtEntity(npc, "random.pop", 0.8F, 1.0F);
        }
        if (!player.capabilities.isCreativeMode && stack != null) {
            stack.stackSize--;
        }
        return stack;
    }
}
