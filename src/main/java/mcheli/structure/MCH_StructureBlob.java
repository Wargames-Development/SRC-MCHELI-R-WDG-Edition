package mcheli.structure;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.List;

public class MCH_StructureBlob {
    public int version = MCH_StructureMeta.CURRENT_VERSION;
    public int sizeX = 1;
    public int sizeY = 1;
    public int sizeZ = 1;
    public final List<BlockEntry> blocks = new ArrayList<BlockEntry>();

    public static class BlockEntry {
        public int x;
        public int y;
        public int z;
        public String blockName = "minecraft:air";
        public int meta = 0;
        public NBTTagCompound tileEntity;
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger("Version", this.version);
        root.setInteger("SizeX", this.sizeX);
        root.setInteger("SizeY", this.sizeY);
        root.setInteger("SizeZ", this.sizeZ);

        NBTTagList list = new NBTTagList();
        for (BlockEntry e : this.blocks) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("X", e.x);
            tag.setInteger("Y", e.y);
            tag.setInteger("Z", e.z);
            tag.setString("Block", e.blockName != null ? e.blockName : "minecraft:air");
            tag.setInteger("Meta", e.meta);
            if (e.tileEntity != null) {
                tag.setTag("TileEntity", e.tileEntity.copy());
            }
            list.appendTag(tag);
        }
        root.setTag("Blocks", list);
        return root;
    }

    public static MCH_StructureBlob fromNBT(NBTTagCompound root) {
        MCH_StructureBlob blob = new MCH_StructureBlob();
        if (root == null) {
            return blob;
        }
        blob.version = root.getInteger("Version");
        if (blob.version <= 0) {
            blob.version = MCH_StructureMeta.CURRENT_VERSION;
        }
        blob.sizeX = Math.max(1, root.getInteger("SizeX"));
        blob.sizeY = Math.max(1, root.getInteger("SizeY"));
        blob.sizeZ = Math.max(1, root.getInteger("SizeZ"));

        NBTTagList list = root.getTagList("Blocks", 10);
        for (int i = 0; i < list.tagCount(); ++i) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            BlockEntry e = new BlockEntry();
            e.x = tag.getInteger("X");
            e.y = tag.getInteger("Y");
            e.z = tag.getInteger("Z");
            e.blockName = tag.getString("Block");
            e.meta = tag.getInteger("Meta");
            if (tag.hasKey("TileEntity")) {
                e.tileEntity = tag.getCompoundTag("TileEntity");
            }
            blob.blocks.add(e);
        }
        return blob;
    }
}
