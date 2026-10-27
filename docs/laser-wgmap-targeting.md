# Laser targeting on distant terrain

Aircraft and handheld designators can target WGMap terrain outside loaded client
chunks. The client range remains 1,500 blocks. DH renders the terrain; WGMap
supplies obstruction data. Missile range, lifetime, damage, and chunk loading
remain unchanged.

The client shares the bounded voxel traversal with server validation: live block
collision is used in resident native chunks, and optional WGMap first-hit queries
are used only across unloaded spans. Forge 1.7.10 `ChunkProviderClient.chunkExists`
always returns true, so native residency uses its verified in-memory `provideChunk`
lookup and the `EmptyChunk` sentinel instead. The old check discarded every cached
hit and caused the 1,500 m fallback even when a nearer cached surface was ready.

WGMap exposes optional `TerrainOcclusionApi.FIRST_HIT_VERSION = 1` and
`traceFirstHit(..., double[] hit)` without changing base API version 1. This
returns the first trusted surface directly, including solid chunk-entry cases,
and requires every preceding cached cell to be clear. Older WGMap versions keep
the conservative prefix-query fallback. A single whole-corridor demand query
helps missing regions arrive together; it never supplies a hit position.
Missing/uncertain prefixes now suppress the designation marker and publish inactive
guidance while data is requested. The rangefinder uses its existing unavailable
readout. Thus 1,500 m remains valid only for a known-clear maximum-range sky aim;
unknown terrain cannot produce a confirmed maximum-range designation.
Live blocks and nearer entity hits take precedence. Handheld and
aircraft markers are projected inside the vanilla far plane; aircraft markers
draw once in world-last without the old 1,000-block cutoff or entity culling.

## Server validation

Both laser packets queue intent for the server tick. The queue coalesces three
channels per player, holds at most 128 players, and processes at most 16 players
per tick. It rejects stale/disconnected/dead senders, queued world changes, and
repeated/out-of-order state sequences. Packet owner IDs never establish ownership.

Validation checks the held weapon or controlled aircraft, finite world coordinates,
server-known aim, and range. The server allows 1,536 blocks to account for camera
and seat offsets around the 1,500-block client limit. Aim allows five degrees of
rotation lag or eight blocks of close-range parallax with a forward component.

The server reconstructs hits using actual collision in loaded chunks and WGMap's
new versioned LaserTerrainAccess API across unloaded spans. Missing or untrusted
data suspends guidance. A nearer known surface replaces the requested point.
Traversal is capped at 8,192 steps and never loads chunks. Eligible local entity
targets are resolved from server entities; known-clear full-range sky aim remains.

WGMap requires the owning server thread/current world, ready policy permitting
generated terrain import, unsuspended masking, and no pending edit in queried
chunks. Cache misses use its existing bounded asynchronous .wgor read queue.

Server laser states belong to their owning world through weak references, expire
against that world's clock, and cannot guide missiles in another world. Existing
server missile launch mode still determines TV versus laser guidance; client-local
weapon mode fields are not treated as server mode evidence.

## LOD acquisition and follow

Entity-lock weapons also query the existing far-vehicle renderer's current
smoothed poses. This includes snapshot-only vehicles without native client
entities. Acquisition uses a vehicle-sized box and nearest intersection inside
the configured visual distance (default 750 blocks). Follow retains UUID identity
across numeric entity-ID handoffs and uses the existing camera follow strength.
Every candidate still needs a trusted live/cached terrain prefix to its centre.
Unknown terrain pauses designation; nearer terrain breaks vehicle follow.
Destroyed, jammed/smoking, own, stale-frame and stale-snapshot candidates are
excluded. Candidate processing is capped at 256 existing poses per query.

The server resolves snapshot latency against current aircraft within 64 blocks
of the requested point, processing at most 128 candidates from a local loaded
entity query. It retains existing weapon, world, aim-cone and range checks and
validates actual terrain LOS to the resolved point. It accepts no client entity
identity and adds no packet fields. Update MCHR on the server as well as clients.
Tracking range, radar contacts, missile physics and terrain loading are unchanged.

Automated tests exercise snapshot-only acquisition, moving UUID follow and
identity handoff, stale/smoke/destruction rejection, mixed terrain traversal and
server aim tolerance. Actual GPU masking, aircraft camera follow and multiplayer
latency still require in-game validation. WGMap's companion handoff is
`docs/HANDOFF_OCCLUSION_COVERAGE_AND_LASER_FOLLOW.md`.

## Changed files and purpose

MCHeli production paths below are relative to src/main/java/mcheli/.

| File | Purpose |
| --- | --- |
| integration/wgmap/client/WGMapLaserClientBridge.java | Cached client hit prediction. |
| integration/wgmap/WGMapLaserServerBridge.java | Optional, versioned server API access. |
| weapon/MCH_LaserTargeting.java | Weapon/aim/range validation and authoritative mixed terrain tracing. |
| network/packets/PacketLaserStateSync.java | Bounded server-thread intent processing. |
| network/packets/PacketLaserGuidanceTargeting.java | Validates legacy guidance through the same path. |
| MCH_ServerTickHandler.java | Drains the queue on the server thread. |
| weapon/MCH_LaserStateStore.java | World-scoped state and per-world expiry. |
| weapon/MCH_EntityTvMissile.java | Consumes laser state only in the owning world. |
| weapon/MCH_LaserGuidanceSystem.java | Uses cached targeting in a client-only method. |
| lweapon/MCH_ClientLightWeaponTickHandler.java | Adds cached targeting to handheld fallback. |
| render/MCH_WGMapOcclusion.java | Exposes the existing client query to the helper. |
| render/MCH_RenderGPSPosition.java | Projects handheld markers and invokes aircraft overlay. |
| weapon/MCH_RenderLockBox.java | Removes marker cutoff and preserves overlay render state. |
| dev-deps/WGMap-dev-local.jar | Compile-only dependency containing the new API signatures. |

WGMap production paths below are relative to src/main/java/com/wdg/wgmap/.

| File | Purpose |
| --- | --- |
| integration/mchr/LaserTerrainAccess.java | Version 1 server first-hit API. |
| proxy/CommonProxy.java | Dedicated-server-safe integration entry. |
| server/ServerLifecycleManager.java | World/thread/policy/revision validation and cache access. |
| terrain/occlusion/TerrainSegmentQuery.java | First-hit mode that rejects unknown prefixes, preserving existing diagnostic traces. |

New/updated tests cover prediction precision, loaded/unloaded/mixed traversal,
bounded work, malformed aim, packet layouts, absent WGMap, world identity/clocks,
slab surfaces, unknown prefixes, negative coordinates, and extended height.

No packet IDs/layouts, config keys, NBT keys, definition fields, save formats, or
registrations change. Update WGMap on the server as well as MCHeli for unloaded
server terrain. Older/absent WGMap fails closed for unloaded spans; fully loaded
terrain continues to work. The client marker is prediction, not server acceptance.

## Review and verification

- C1: direct trust of client coordinates replaced with bounded server-thread hit reconstruction.
- C2: cross-world state/clock reuse prevented through world scoping and regression tests.
- B1/B2: marker culling, cutoff, fog, and overlay state corrected.
- B3: API version uses a runtime getter to prevent Java constant inlining.
- B4: server missile launch mode retains mode ownership because weapon mode switching is client-local.
- Build correction: interrupted validation left the development jar without
  apiVersion(). Refreshed the API classes from compiled WGMap output and confirmed
  the method using javap.

Java 8 validation on 2026-10-25:

- MCHeli: gradlew.bat build --offline passed, including reobfuscation and
  160 tests with zero failures/errors.
- WGMap: gradlew.bat compileJava test verifySideSeparation verifyOptionalModIsolation --offline
  passed, including 1,090 tests with zero failures/errors.
- Build numbers remain MCHeli 180 and WGMap 125. Existing unrelated wrapper and
  WGMap changes were preserved. No commit, push, or installation was made.

## Manual in-game checks still required

1. With updated mods and trusted WGMap data, reduce vanilla view distance and
   designate DH ground 500-1,400 blocks away. Check marker/range and missile steering.
2. Repeat on foot, in aircraft seats, and through a UAV, including beyond 1,000
   blocks. Cross the loaded-chunk boundary and check marker stability.
3. Add a nearer loaded wall/entity. Check nearer hits, entity lock, smoke, and jamming.
4. Test absent/old WGMap, missing data, and normal WGMap invalidation/suspension.
   Guidance across unknown spans must stop; loaded targets should still work.
5. Change weapons, release/re-aim, switch TV/laser mode, reconnect, and change
   dimensions. Check fresh acquisition and ownership on a dedicated server.
6. Test concurrent users and rapid camera motion for tick cost and aim rejection
   caused by latency/parallax. No live client/server or performance test was run.
   WGMap collision classifications can differ from DH's visible mesh.

