package mcheli.wgc;

import cpw.mods.fml.common.Loader;
import mcheli.MCH_Config;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class Integrations {

    private static final String WGCORE_MOD_ID = "wgcore";
    private static final boolean WGCORE_LOADED = Loader.isModLoaded(WGCORE_MOD_ID);

    private Integrations() {
    }

    public static boolean isWGCoreIntegrationEnabled() {
        return WGCORE_LOADED
                && MCH_Config.EnableWGCoreIntegration != null
                && MCH_Config.EnableWGCoreIntegration.prmBool;
    }

    public static boolean canHarmPlayerWGC(Entity actingEntity, Entity targetEntity, World world) {
        if (!isWGCoreIntegrationEnabled()) {
            return true;
        }

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

        return WGCoreCompat.canHarmPlayer(actingPlayerId, targetPlayerId, world);
    }

    public static boolean canHarmPlayerWGC(UUID actingPlayerId, UUID targetPlayerId, World world) {
        if (!isWGCoreIntegrationEnabled()) {
            return true;
        }

        if (world == null || actingPlayerId == null || targetPlayerId == null) {
            return false;
        }

        return WGCoreCompat.canHarmPlayer(actingPlayerId, targetPlayerId, world);
    }

    public static boolean canVehicleDamageBlockWGC(World world,
                                                   Entity actingEntity,
                                                   int x,
                                                   int y,
                                                   int z,
                                                   String impactTypeId) {
        if (!isWGCoreIntegrationEnabled()) {
            return true;
        }

        if (world == null) {
            return false;
        }

        ChunkPosition blockPos = new ChunkPosition(x, y, z);
        ExplosionResult decision = evaluateExplosionWGC(
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
        if (filtered.isEmpty()) {
            return false;
        }

        for (ChunkPosition pos : filtered) {
            if (pos != null && pos.chunkPosX == x && pos.chunkPosY == y && pos.chunkPosZ == z) {
                return true;
            }
        }

        return false;
    }

    public static ExplosionResult evaluateExplosionWGC(World world,
                                                       Entity actingEntity,
                                                       Explosion explosion,
                                                       double originX,
                                                       double originY,
                                                       double originZ,
                                                       String explosionTypeId,
                                                       List<ChunkPosition> affectedBlocks) {
        if (!isWGCoreIntegrationEnabled()) {
            return ExplosionResult.allowAll();
        }

        if (world == null) {
            return null;
        }

        return WGCoreCompat.evaluateExplosion(
                world,
                resolveActingPlayerId(actingEntity),
                explosion,
                originX,
                originY,
                originZ,
                explosionTypeId,
                affectedBlocks
        );
    }

    public static boolean arePlayersInSameFactionWGC(World world, UUID firstPlayerId, UUID secondPlayerId) {
        if (!isWGCoreIntegrationEnabled()
                || world == null
                || firstPlayerId == null
                || secondPlayerId == null) {
            return false;
        }

        return WGCoreCompat.arePlayersInSameFaction(world, firstPlayerId, secondPlayerId);
    }

    public static Entity resolveActingEntity(Entity preferredPlayerEntity, Entity fallbackEntity) {
        return preferredPlayerEntity != null ? preferredPlayerEntity : fallbackEntity;
    }

    private static UUID resolveActingPlayerId(Entity actingEntity) {
        if (actingEntity instanceof EntityPlayer) {
            return actingEntity.getUniqueID();
        }

        return null;
    }

    public static final class ExplosionResult {

        private static final ExplosionResult ALLOW_ALL = new ExplosionResult(
                true,
                true,
                true,
                false,
                Collections.<ChunkPosition>emptyList()
        );

        private final boolean explosionAllowed;
        private final boolean entityDamageAllowed;
        private final boolean blockDamageAllowed;
        private final boolean filtered;
        private final List<ChunkPosition> filteredAffectedBlocks;

        ExplosionResult(boolean explosionAllowed,
                        boolean entityDamageAllowed,
                        boolean blockDamageAllowed,
                        boolean filtered,
                        List<ChunkPosition> filteredAffectedBlocks) {
            this.explosionAllowed = explosionAllowed;
            this.entityDamageAllowed = entityDamageAllowed;
            this.blockDamageAllowed = blockDamageAllowed;
            this.filtered = filtered;
            this.filteredAffectedBlocks = filteredAffectedBlocks != null
                    ? filteredAffectedBlocks
                    : Collections.<ChunkPosition>emptyList();
        }

        static ExplosionResult allowAll() {
            return ALLOW_ALL;
        }

        public boolean isExplosionAllowed() {
            return this.explosionAllowed;
        }

        public boolean isEntityDamageAllowed() {
            return this.entityDamageAllowed;
        }

        public boolean isBlockDamageAllowed() {
            return this.blockDamageAllowed;
        }

        public boolean isFiltered() {
            return this.filtered;
        }

        public List<ChunkPosition> getFilteredAffectedBlocks() {
            return this.filteredAffectedBlocks;
        }
    }
}
