# Radar terrain occlusion

## Behavior

Search radar hides vehicle contacts when opaque terrain blocks every sampled
point on their collision bounds. Checks start just above the emitting vehicle's
main bounding box, independently of camera position. Fifteen rays sample the
center and four corners at low, middle, and upper heights of each box. Extra
vehicle hitboxes are included, so an exposed turret or another body section can
keep the contact detectable. This is a bounded approximation of full coverage,
not an exact test against every model triangle.

Covered contacts are removed from retained search contacts and excluded from
ACM capture, selection, tracked-contact rendering, and BVR contact visibility.
Existing radar tracks drop when terrain coverage is reported. Server validation
also rejects those tracks for radar-dependent firing/relay and stops refreshing
radar search/lock RWR events through cover. Existing RWR event lifetimes remain
unchanged. Re-exposed vehicles use the existing scan cadence and detection chance.

This applies to MCHeli vehicles, including aircraft, tanks, and generic vehicles.
Missiles, infantry, GPS waypoints, legacy living-entity radar, mortar radar, and
the separate anti-radiation emitter display retain their existing behavior.

## Files changed

| File | Purpose |
| --- | --- |
| `src/main/java/mcheli/MCH_RadarTerrain.java` | Common/server terrain traversal, multi-point bounds checks, and a five-tick cache shared by each emitter's crew and server tracking checks. |
| `src/main/java/mcheli/MCH_EntityInfoManager.java` | Adds recipient-specific blocked vehicle IDs for the player's current radar vehicle to existing server snapshots. |
| `src/main/java/mcheli/network/packets/PacketEntityInfoSync.java` | Encodes/decodes an optional radar terrain trailer without changing existing fields or packet IDs. |
| `src/main/java/mcheli/MCH_EntityInfoClientTracker.java` | Publishes immutable per-emitter terrain results with the accepted snapshot sequence; clears them on reset. |
| `src/main/java/mcheli/render/MCH_RenderRWR.java` | Applies terrain results to contact detection, retention, display, selection, ACM acquisition, and radar tracking. |
| `src/main/java/mcheli/MCH_RWRThreatManager.java` | Applies the same server evidence to RWR emission and authoritative track validation. |
| `src/test/java/mcheli/MCH_RadarTerrainTest.java` | Covers full/partial cover, long rays, negative coordinates, diagonal/vertical traversal, missing data, and bounded work. |
| `src/test/java/mcheli/network/packets/PacketEntityInfoSyncTest.java` | Covers terrain trailer round trips, absent trailers, preserved vehicle identity, and invalid counts. |

Six production files are needed to carry server-owned terrain evidence through
the existing snapshot and client display while preserving authoritative tracking.
No new settings, dependencies, save formats, registrations, or packet IDs.

## Compatibility and limits

The optional `RAD1` trailer follows existing snapshot extensions. It contains
the emitter ID, a bounded count, and blocked target IDs. Older clients ignore the
trailer; newer clients accept its absence. Update both client and server to get
the complete feature. Old clients may still display covered contacts, while an
updated server rejects covered radar tracks. Old servers provide no terrain mask.

Only already-loaded server chunks are queried. Radar never loads terrain chunks.
Unknown terrain alone does not hide a vehicle; a known opaque obstruction still
counts even if another part of the ray crosses unloaded terrain. Consequently,
a hill entirely in unloaded server chunks cannot provide blocking evidence.
Opaque full-cube terrain/buildings block radar; water, glass, foliage, stairs,
and other non-opaque blocks do not. Bounds/model mismatches and tiny exposed
gaps between sample rays can affect edge cases.

Cache results last at most five server ticks (250 ms at 20 TPS), then travel in
the existing two-tick active snapshot stream. Network latency adds to this.
Traversal stops at a confirmed blocker or 8192 visited voxels per ray, allowing
the existing 4096-block radar range without vanilla's 200-step ray limit. A clear
sample ends further bounds checks immediately. Large multiplayer radar workloads
still need live performance validation.

## Implement, review, fix, test, verify

- Implementer: added server terrain evidence and integrated it into the existing
  snapshot/contact/tracking paths. No client terrain or WGMap dependency is used
  for authoritative checks, and visual vehicle snapshots remain available.
- Reviewer: checked packet-prefix compatibility, snapshot ordering/reset,
  client/server separation, retained-contact/forced-track bypasses, chunk-loading
  avoidance, and bounded traversal. B1: an unknown chunk before known blocking
  terrain must not discard that later obstruction evidence. C2: network-thread
  weapon validation must consume published server evidence instead of tracing
  world terrain. B3: leaf opacity must not make foliage count as solid cover.
- Fixer: addressed B1 by continuing traversal through unknown cells; added a
  regression check for a known wall after an unknown gap. Addressed C2 with a
  concurrent publication of the server tick's cached pair results; weapon checks
  read only those results. Addressed B3 with an explicit leaf-material exclusion.
- Tester/verifier: Java 8 `gradlew.bat compileJava test --offline` passed with
  58 tests, zero failures/errors. `git diff --check` passed. Final review found
  no remaining code issue within scope. T1: live dedicated-server driving,
  acquisition/lock behavior, and performance remain untested.

No Minecraft client/server was launched, no jar was packaged or installed, and
the build number remains 170. Existing unrelated working changes were preserved.

## In-game checks

1. On an updated server/client, use an enabled ground-search or multi-mode radar
   and place a detectable enemy vehicle in clear view. Confirm normal acquisition.
2. Move the target fully behind a broad hill or solid wall. Check it disappears
   from the radar panel and BVR contact boxes after the short update delay,
   including if it was already selected or tracked. A radar-dependent weapon
   should no longer accept the covered radar track.
3. Expose the turret/roof above cover, then expose one side around cover. Confirm
   those positions remain eligible for detection with the usual scan probability.
4. Move back into clear view and confirm normal reacquisition. Repeat with air
   radar, ACM acquisition, and more than 200 blocks between radar and cover.
5. Change seats, switch vehicles, toggle radar power, reconnect, and change
   dimensions. Terrain results must belong to the current radar vehicle only.
6. Repeat with multiple radar vehicles/crew and observe server tick performance.
   Check water/glass/foliage and unknown chunks do not create false terrain cover.
