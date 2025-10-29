package mcheli;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MCH_ExplosionParam {

    /**
     * The entity that caused the explosion.  This may be {@code null} if the
     * explosion was not triggered by an entity.
     */
    public Entity exploder;

    /**
     * The player credited with the explosion.  This may be {@code null}
     * if the explosion was not created on behalf of a player.  For example,
     * this allows damage attribution for server side logging or scoring.
     */
    public EntityPlayer player;

    /**
     * X coordinate of the explosion centre.
     */
    public double x;

    /**
     * Y coordinate of the explosion centre.
     */
    public double y;

    /**
     * Z coordinate of the explosion centre.
     */
    public double z;

    /**
     * The damage radius for entities.  This value is passed to the parent
     * {@link net.minecraft.world.Explosion} constructor and also used in
     * various calculations to determine knockback and fire propagation.
     */
    public float size;

    /**
     * The radius used when destroying or altering blocks.  Separating
     * {@link #size} and {@link #sizeBlock} allows explosions which damage
     * entities over a larger area than they destroy terrain, for example
     * high‐explosive munitions.
     */
    public float sizeBlock;

    /**
     * 直击的实体，用于应用固定伤害
     */
    public Entity directAttackEntity;

    /**
     * Whether to play the explosion sound.  When set to {@code false} the
     * explosion will occur silently.  This can be useful for custom
     * explosion effects or debugging.
     */
    @Builder.Default
    public boolean isPlaySound = true;

    /**
     * Whether to spawn smoke and debris particles as part of the explosion.
     */
    @Builder.Default
    public boolean isSmoking = true;

    /**
     * Whether the explosion should light nearby blocks on fire.  See
     * {@link MCH_Explosion#doExplosionB(boolean)} for details.
     */
    @Builder.Default
    public boolean isFlaming = false;

    /**
     * Whether the explosion should destroy blocks.  This flag should honour
     * the server game rule {@code mobGriefing} to avoid griefing when
     * disabled.
     */
    @Builder.Default
    public boolean isDestroyBlock = true;

    /**
     * When greater than zero, nearby entities will be set on fire for a
     * duration proportional to their proximity to the explosion centre.  A
     * value of zero disables this behaviour.
     */
    @Builder.Default
    public int countSetFireEntity = 0;

    /**
     * Whether the explosion is occurring underwater.  Some visual effects
     * behave differently when underwater and this flag allows those
     * effects to be selected appropriately.
     */
    @Builder.Default
    public boolean isInWater = false;

    /**
     * Damage multiplier applied when the target is a player.  Defaults to 1.0.
     */
    @Builder.Default
    public float damageVsPlayer = 1.0f;

    /**
     * Damage multiplier applied when the target is an {@link net.minecraft.entity.EntityLivingBase}
     * but not a player.  Defaults to 1.0.
     */
    @Builder.Default
    public float damageVsLiving = 1.0f;

    /**
     * Damage multiplier applied when the target is a plane.  Defaults to 1.0.
     */
    @Builder.Default
    public float damageVsPlane = 1.0f;

    /**
     * Damage multiplier applied when the target is a helicopter.  Defaults to 1.0.
     */
    @Builder.Default
    public float damageVsHeli = 1.0f;

    /**
     * Damage multiplier applied when the target is a tank.  Defaults to 1.0.
     */
    @Builder.Default
    public float damageVsTank = 1.0f;

    /**
     * Damage multiplier applied when the target is a vehicle.  Defaults to 1.0.
     */
    @Builder.Default
    public float damageVsVehicle = 1.0f;

    /**
     * Damage multiplier applied when the target is a ship (a floating plane).  Defaults to 1.0.
     */
    @Builder.Default
    public float damageVsShip = 1.0f;
}
