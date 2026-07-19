package mcheli.wgc;

import com.wdg.wgcore.integration.api.WGCoreIntegrationAccess;
import com.wdg.wgcore.integration.model.ActionAttribution;
import com.wdg.wgcore.integration.model.ActionSourceType;
import com.wdg.wgcore.integration.model.ExplosionActionContext;
import com.wdg.wgcore.integration.model.ExplosionDecision;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

final class WGCoreCompat {

    private static final String SOURCE_MOD_ID = "mcheli";

    private WGCoreCompat() {
    }

    static boolean canHarmPlayer(UUID actingPlayerId, UUID targetPlayerId, World world) {
        return WGCoreIntegrationAccess.canHarmPlayer(actingPlayerId, targetPlayerId, world);
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
                                                          List<ChunkPosition> affectedBlocks) {
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
        ExplosionDecision decision = WGCoreIntegrationAccess.evaluateExplosion(context);

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
