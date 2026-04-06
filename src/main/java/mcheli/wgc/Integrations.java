package mcheli.wgc;

import com.wdg.wgcore.integration.api.WGCoreIntegrationAccess;
import com.wdg.wgcore.integration.model.ActionAttribution;
import com.wdg.wgcore.integration.model.ActionSourceType;
import com.wdg.wgcore.integration.model.ExplosionActionContext;
import com.wdg.wgcore.integration.model.ExplosionDecision;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class Integrations {

    private static final String SOURCE_MOD_ID = "mcheli";

    private Integrations() {
    }

    public static boolean canHarmPlayerWGC(Entity actingEntity, Entity targetEntity, World world) {
        if (world == null) {
            return false;
        }

        if (!(targetEntity instanceof EntityPlayer)) {
            return true;
        }

        UUID targetPlayerId = targetEntity.getUniqueID();
        UUID actingPlayerId = resolveActingPlayerId(actingEntity);

        if (actingPlayerId == null || targetPlayerId == null) {
            return false;
        }

        return WGCoreIntegrationAccess.canHarmPlayer(actingPlayerId, targetPlayerId, world);
    }

    public static boolean canHarmPlayerWGC(UUID actingPlayerId, UUID targetPlayerId, World world) {
        if (world == null || actingPlayerId == null || targetPlayerId == null) {
            return false;
        }

        return WGCoreIntegrationAccess.canHarmPlayer(actingPlayerId, targetPlayerId, world);
    }

    public static boolean canVehicleDamageBlockWGC(World world,
                                                   Entity actingEntity,
                                                   int x,
                                                   int y,
                                                   int z,
                                                   String impactTypeId) {
        if (world == null) {
            return false;
        }

        ChunkPosition blockPos = new ChunkPosition(x, y, z);
        ExplosionDecision decision = evaluateExplosionWGC(
                world,
                actingEntity,
                null,
                x + 0.5D,
                y + 0.5D,
                z + 0.5D,
                impactTypeId != null ? impactTypeId : "mcheli:vehicle_collision",
                Collections.singletonList(blockPos)
        );

        if (decision == null || !decision.isExplosionAllowed() || !decision.isBlockDamageAllowed()) {
            return false;
        }

        if (!decision.isFiltered()) {
            return true;
        }

        List<ChunkPosition> filtered = decision.getFilteredAffectedBlocks();
        if (filtered == null || filtered.isEmpty()) {
            return false;
        }

        for (ChunkPosition pos : filtered) {
            if (pos != null && pos.chunkPosX == x && pos.chunkPosY == y && pos.chunkPosZ == z) {
                return true;
            }
        }

        return false;
    }

    public static ExplosionDecision evaluateExplosionWGC(World world,
                                                         Entity actingEntity,
                                                         Explosion explosion,
                                                         double originX,
                                                         double originY,
                                                         double originZ,
                                                         String explosionTypeId,
                                                         List<ChunkPosition> affectedBlocks) {
        if (world == null) {
            return ExplosionDecision.deny(null);
        }

        UUID actingPlayerId = resolveActingPlayerId(actingEntity);
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

        return WGCoreIntegrationAccess.evaluateExplosion(context);
    }

    public static Entity resolveActingEntity(Entity preferredPlayerEntity, Entity fallbackEntity) {
        return preferredPlayerEntity != null ? preferredPlayerEntity : fallbackEntity;
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

    private static UUID resolveActingPlayerId(Entity actingEntity) {
        if (actingEntity instanceof EntityPlayer) {
            return actingEntity.getUniqueID();
        }

        return null;
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