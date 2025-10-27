package mcheli;

import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.Vec3;

public class MCH_IndicatedDamageSource extends EntityDamageSourceIndirect {

    //命中的绝对坐标
    public Vec3 hitPos;
    //子弹的绝对运动方向
    public Vec3 dir;

    public MCH_IndicatedDamageSource(String p_i1568_1_, Entity p_i1568_2_, Entity p_i1568_3_) {
        super(p_i1568_1_, p_i1568_2_, p_i1568_3_);
    }

    public static MCH_IndicatedDamageSource build(DamageSource damageSource, Vec3 hitPos, Vec3 dir) {
        MCH_IndicatedDamageSource indicatedDamageSource = new MCH_IndicatedDamageSource(damageSource.damageType, damageSource.getSourceOfDamage(), damageSource.getEntity());
        indicatedDamageSource.hitPos = hitPos;
        indicatedDamageSource.dir = dir;
        return indicatedDamageSource;
    }


}
