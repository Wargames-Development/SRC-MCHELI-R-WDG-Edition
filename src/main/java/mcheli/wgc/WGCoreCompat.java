package mcheli.wgc;

import com.wdg.wgcore.integration.api.WGCoreIntegrationAccess;
import com.wdg.wgcore.integration.model.ActionAttribution;
import com.wdg.wgcore.integration.model.ActionSourceType;
import com.wdg.wgcore.integration.model.ExplosionActionContext;
import com.wdg.wgcore.integration.model.ExplosionDecision;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class WGCoreCompat {

    private static final String SOURCE_MOD_ID = "mcheli";
    private static final Method PREPARE_EXPLOSION = findPrepareExplosion();
    private static final Method TERRAIN_IMPACT = findMethod("evaluateTerrainImpact", ExplosionActionContext.class, Map.class);
    private static final Method RESISTANCE = findMethod("getBlockResistanceOverride",
            World.class, int.class, int.class, int.class, double.class);

    private WGCoreCompat() {
    }

    private static Method findPrepareExplosion() {
        return findMethod("prepareExplosion", Explosion.class, double.class, boolean.class, boolean.class);
    }

    private static Method findMethod(String name, Class<?>... parameters) {
        try {
            return WGCoreIntegrationAccess.class.getMethod(name, parameters);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    static boolean supportsTerrainImpact() {
        return TERRAIN_IMPACT != null && RESISTANCE != null;
    }

    static double terrainResistance(World world, int x, int y, int z, double nativeResistance) {
        try {
            return (Double) RESISTANCE.invoke(null, world, x, y, z, nativeResistance);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not query WGCore terrain resistance", failure);
        }
    }

    static boolean prepareExplosion(Explosion explosion, double strength, boolean terrainEnabled) {
        if (PREPARE_EXPLOSION == null) {
            return false;
        }
        try {
            PREPARE_EXPLOSION.invoke(null, explosion, strength, terrainEnabled, true);
            return true;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to prepare WGCore explosion", e);
        }
    }

    static boolean canHarmPlayer(UUID actingPlayerId, UUID targetPlayerId, World world) {
        return WGCoreIntegrationAccess.canHarmPlayer(actingPlayerId, targetPlayerId, world);
    }

    static UUID getPlayerFaction(World world, UUID playerId) {
        return WGCoreIntegrationAccess.getPlayerFaction(world, playerId);
    }

    static boolean arePlayersInSameFaction(World world, UUID firstPlayerId, UUID secondPlayerId) {
        UUID firstFactionId = WGCoreIntegrationAccess.getPlayerFaction(world, firstPlayerId);
        UUID secondFactionId = WGCoreIntegrationAccess.getPlayerFaction(world, secondPlayerId);
        return firstFactionId != null && firstFactionId.equals(secondFactionId);
    }

    static Integrations.ExplosionResult evaluateExplosion(World world,
                                                          UUID actingPlayerId,
                                                          Explosion explosion,
                                                          double originX,
                                                          double originY,
                                                          double originZ,
                                                          String explosionTypeId,
                                                          List<ChunkPosition> affectedBlocks,
                                                          Map<ChunkPosition, Double> terrainImpact) {
        ActionAttribution attribution = buildAttribution(world, actingPlayerId, ActionSourceType.EXPLOSIVE);
        ExplosionActionContext context = new ExplosionActionContext(
                world,
                floorToInt(originX),
                floorToInt(originY),
                floorToInt(originZ),
                explosion,
                attribution,
                normaliseExplosionTypeId(explosionTypeId),
                affectedBlocks != null ? affectedBlocks : Collections.<ChunkPosition>emptyList()
        );
        ExplosionDecision decision;
        try {
            decision = terrainImpact == null ? WGCoreIntegrationAccess.evaluateExplosion(context)
                    : (ExplosionDecision) TERRAIN_IMPACT.invoke(null, context, terrainImpact);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not evaluate WGCore terrain impact", failure);
        }

        if (decision == null) {
            return null;
        }

        return new Integrations.ExplosionResult(
                decision.isExplosionAllowed(),
                decision.isEntityDamageAllowed(),
                decision.isBlockDamageAllowed(),
                decision.isFiltered(),
                decision.getFilteredAffectedBlocks()
        );
    }

    private static ActionAttribution buildAttribution(World world,
                                                       UUID actingPlayerId,
                                                       ActionSourceType sourceType) {
        if (actingPlayerId == null) {
            return new ActionAttribution(
                    null,
                    null,
                    null,
                    null,
                    SOURCE_MOD_ID,
                    sourceType,
                    true,
                    null
            );
        }

        return ActionAttribution.directPlayer(
                actingPlayerId,
                WGCoreIntegrationAccess.getPlayerFaction(world, actingPlayerId),
                SOURCE_MOD_ID,
                sourceType
        );
    }

    private static String normaliseExplosionTypeId(String explosionTypeId) {
        if (explosionTypeId == null) {
            return "mcheli:explosion";
        }

        String trimmed = explosionTypeId.trim();
        return trimmed.isEmpty() ? "mcheli:explosion" : trimmed;
    }

    private static int floorToInt(double value) {
        return (int) Math.floor(value);
    }
}
