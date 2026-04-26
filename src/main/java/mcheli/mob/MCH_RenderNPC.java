package mcheli.mob;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

@SideOnly(Side.CLIENT)
public class MCH_RenderNPC extends RenderBiped {

    private static final ResourceLocation STEVE_TEXTURE = new ResourceLocation("minecraft", "textures/entity/steve.png");

    public MCH_RenderNPC() {
        super(new ModelBiped(0.0F), 0.5F);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return STEVE_TEXTURE;
    }
}
