package mcheli.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_WaypointNavDebug;
import mcheli.wrapper.W_BlockContainer;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Locale;

public class MCH_ConfigSpawnerBlock extends W_BlockContainer implements ITileEntityProvider {

    private final MCH_BlockInfo info;
    @SideOnly(Side.CLIENT)
    private IIcon activeIcon;
    @SideOnly(Side.CLIENT)
    private IIcon sleepIcon;
    @SideOnly(Side.CLIENT)
    private IIcon errorIcon;

    public MCH_ConfigSpawnerBlock(MCH_BlockInfo info, Material material) {
        super(info.blockID, material);
        this.info = info;
    }

    public TileEntity createNewTileEntity(World world, int metadata) {
        return new MCH_ConfigSpawnerTileEntity(this.info.name);
    }

    public TileEntity createNewTileEntity(World world) {
        return new MCH_ConfigSpawnerTileEntity(this.info.name);
    }

    public boolean hasTileEntity(int metadata) {
        return true;
    }

    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float px, float py, float pz) {
        if (world == null || player == null) {
            return false;
        }
        if (world.isRemote) {
            // Let server side handle the actual info output.
            return false;
        }
        TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof MCH_ConfigSpawnerTileEntity)) {
            return false;
        }
        MCH_ConfigSpawnerTileEntity tile = (MCH_ConfigSpawnerTileEntity)te;
        if (!tile.isWaypointMarker()) {
            MCH_WaypointNavDebug.trace(world, player, "RightClick ignored: block=%s pos=%d,%d,%d waypoint=false",
                this.info != null ? this.info.name : "<null>", x, y, z);
            return false;
        }
        String cur = tile.getWaypointId();
        String next = tile.getNextWaypointId();
        String curPos = "(" + x + "," + y + "," + z + ")";
        MCH_WaypointNavDebug.trace(world, player, "RightClick waypoint: block=%s current=%s next=%s pos=%s",
            this.info != null ? this.info.name : "<null>", cur, next, curPos);
        player.addChatMessage(new ChatComponentText("Current: " + cur + " " + curPos));
        if (next == null || next.isEmpty()) {
            player.addChatMessage(new ChatComponentText("Next: <NONE>"));
        } else {
            MCH_ConfigSpawnerTileEntity nextTile = MCH_ConfigSpawnerTileEntity.resolveNearestWaypoint(world, next, x + 0.5D, y + 0.5D, z + 0.5D);
            if (nextTile != null) {
                String nextPos = "(" + nextTile.xCoord + "," + nextTile.yCoord + "," + nextTile.zCoord + ")";
                player.addChatMessage(new ChatComponentText("Next: " + next + " " + nextPos));
            } else {
                player.addChatMessage(new ChatComponentText("Next: " + next + " <UNRESOLVED>"));
            }
        }
        return true;
    }

    public void onBlockAdded(World world, int x, int y, int z) {
        super.onBlockAdded(world, x, y, z);
        if (!world.isRemote) {
            world.setBlockMetadataWithNotify(x, y, z, MCH_ConfigSpawnerTileEntity.STATE_ACTIVE, 2);
        }
    }

    public int damageDropped(int meta) {
        return 0;
    }

    public int getMobilityFlag() {
        return 1;
    }

    public boolean getUseNeighborBrightness() {
        return true;
    }

    public boolean isOpaqueCube() {
        return true;
    }

    public boolean renderAsNormalBlock() {
        return true;
    }

    public IIcon getIcon(int side, int meta) {
        if (meta == MCH_ConfigSpawnerTileEntity.STATE_SLEEP) {
            return this.sleepIcon != null ? this.sleepIcon : this.blockIcon;
        }
        if (meta == MCH_ConfigSpawnerTileEntity.STATE_ERROR) {
            return this.errorIcon != null ? this.errorIcon : this.blockIcon;
        }
        return this.activeIcon != null ? this.activeIcon : this.blockIcon;
    }

    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        return this.getIcon(side, world.getBlockMetadata(x, y, z));
    }

    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister) {
        if (this.info.enableWaypoint && !this.info.enableSpawner) {
            String icon = this.normalizeIcon(this.info.textureName);
            this.activeIcon = iconRegister.registerIcon(icon);
            this.sleepIcon = this.activeIcon;
            this.errorIcon = this.activeIcon;
        } else {
            this.activeIcon = iconRegister.registerIcon(this.normalizeIcon(this.info.textureActive));
            this.sleepIcon = iconRegister.registerIcon(this.normalizeIcon(this.info.textureSleep));
            this.errorIcon = iconRegister.registerIcon(this.normalizeIcon(this.info.textureError));
        }
        this.blockIcon = this.activeIcon;
    }

    private String normalizeIcon(String iconName) {
        if (iconName == null || iconName.trim().isEmpty()) {
            return "mcheli:" + this.info.name;
        }
        String icon = iconName.trim();
        if (icon.indexOf(':') < 0) {
            icon = "mcheli:" + icon;
        }
        icon = icon.toLowerCase(Locale.ROOT);
        return icon;
    }
}
