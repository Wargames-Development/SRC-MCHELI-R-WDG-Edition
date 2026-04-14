package mcheli.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.wrapper.W_Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;

public class MCH_ConfigBlock extends W_Block {

    private final String textureName;

    public MCH_ConfigBlock(Material material, String textureName) {
        super(material);
        this.textureName = textureName;
    }

    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister) {
        String icon = this.textureName;
        if (icon.indexOf(':') < 0) {
            icon = "mcheli:" + icon;
        }
        this.blockIcon = iconRegister.registerIcon(icon);
    }
}
