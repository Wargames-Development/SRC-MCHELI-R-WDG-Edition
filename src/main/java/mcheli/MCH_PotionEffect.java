package mcheli;

import net.minecraft.potion.PotionEffect;

public class MCH_PotionEffect {
    public PotionEffect potionEffect;
    public int startDist;
    public int endDist;

    public MCH_PotionEffect(PotionEffect potionEffect, int startDist, int endDist) {
        this.potionEffect = potionEffect;
        this.startDist = startDist;
        this.endDist = endDist;
    }
}
