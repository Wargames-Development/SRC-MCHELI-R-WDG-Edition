# Shared opaque cover policy

Updated FMUR and MCHR clients use WGMap's gap-coverage API and
`TerrainDepthApi.DRAW_STATUS_VERSION=1` / `drawPreparedDepthChecked()`.
Distant/snapshot LODs require complete current terrain/mesh readiness and a
successful draw into the prepared view. Partial or unknown coverage, rejected
draws and unsupported APIs withhold the affected LOD. Near native rendering,
tracking, configured ranges, radar and projectile behavior remain unchanged.

Trees/leaves are opaque blocks for LOD masking. WGMap does not sample leaf-texture
alpha. Existing supported leaf snapshots already contain this geometry; old
baselines without it require authorized regeneration. Custom unsupported plants
are not silently declared transparent or converted into invented geometry.

WGMap server/client protocol 1.29 adds chunk freshness and selected chunk refreshes.
Unchanged verified slots remain usable while a neighboring edit is pending.
A genuinely changed chunk that unloads before capture stays unknown until it
can be repaired; no renderer treats this as permission to expose an LOD.

Update client WGMap and both LOD mods together. Update server WGMap for the new
freshness/delta protocol. No companion packet/config/save format changes are
introduced by these renderer changes. WGMap authority metadata migrates to schema
7; use its documented backup/recovery path for downgrades.

Automated validation includes actual FMUR/MCHR bridge agreement on checked draw
success/failure, WGMap native GPU geometry and stale-view rejection, FMUR native
GPU projection/cover scenarios and MCHR unit tests. This is not a 50-player modpack
acceptance run. Retest forests/mountains, partial cover, scope/thermal passes,
terrain edits, fast flight, reconnects and server restarts with two clients before
production. Full file/validation/compatibility notes are in WGMap's
`docs/HANDOFF_OPAQUE_COVER_AND_SHARED_LODS.md`.
