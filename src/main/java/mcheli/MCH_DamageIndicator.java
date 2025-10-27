package mcheli;

import net.minecraft.util.Vec3;

public class MCH_DamageIndicator {
    public Vec3 relativeHitPos;
    public Vec3 relativeDir;
    public String weaponName;
    public String hitBoxName;
    public double damage;

    public MCH_DamageIndicator(Vec3 relativeHitPos, Vec3 relativeDir, String weaponName, String hitBoxName, double damage) {
        this.relativeHitPos = relativeHitPos;
        this.relativeDir = relativeDir;
        this.weaponName = weaponName;
        this.hitBoxName = hitBoxName;
        this.damage = damage;
    }

    @Override
    public String toString() {
        return relativeHitPos + "," + weaponName + "," + hitBoxName + "," + damage;
    }
}
