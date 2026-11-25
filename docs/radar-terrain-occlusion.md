# Radar terrain occlusion

## Behavior

Search radar hides a vehicle when loaded opaque blocks form an uninterrupted
cross-section across the line of sight to its collision bounds. Checks start
just above the emitter's main bounding box. The server projects the target box
onto candidate planes along the longest axis of the sightline. A plane counts
as cover only if every cell in the projected rectangle is opaque. The server
checks extra hitboxes as well, so an exposed turret or body section can keep
the contact visible.

Covered contacts are removed from retained search contacts and excluded from
ACM capture, selection, tracked-contact rendering, and BVR contact visibility.
Existing radar tracks drop when terrain coverage is reported. Server validation
also rejects those tracks for radar-dependent firing/relay and stops refreshing
radar search/lock RWR events through cover. Re-exposed full-signature aircraft
and helicopters acquire on the next eligible air-search sweep and hold through
the following sweep. Range, scan sector, speed, altitude, team, and
countermeasure checks still apply. Reduced signatures use probabilistic search
detection. Supported snapshots supply the target's server-measured altitude
above ground. If the snapshot and client chunk both lack it, the altitude gate
waits for a measurement.

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

The terrain check reads already-loaded server chunks. Distant Horizons and
WGMap currently supply historical terrain, so an unloaded ridge can leave a
contact visible until its blocks load on the server. A loaded solid slice can
still prove cover across an unloaded gap. Opaque full cubes count as cover.
Water, glass, foliage, and stairs leave gaps in the proof. Complex terrain that
offers cover across several partial slices can also leave a contact visible.

The server recomputes terrain results every five ticks and sends them in the
existing two-tick active snapshot stream. Each box check stops at a confirmed
slice or 8,192 cell checks. Large multiplayer radar workloads still need live
performance validation.

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
3. Expose the turret or wing around cover, or open a one-block sightline through
   the wall. Confirm that the contact reappears on the next eligible sweep.
4. Fly a non-stealth aircraft near the configured range limit in the scan sector
   for several sweeps. Check the HUD and cockpit display beyond the client's
   terrain render distance, then repeat with an unloaded DH-only ridge and a
   loaded server ridge. Confirm the latter hides the contact while fully covered.
5. Change seats, switch vehicles, toggle radar power, reconnect, and change
   dimensions. Terrain results must belong to the current radar vehicle only.
6. Repeat with multiple radar vehicles/crew and observe server tick performance.
   Check water/glass/foliage and unknown chunks do not create false terrain cover.
