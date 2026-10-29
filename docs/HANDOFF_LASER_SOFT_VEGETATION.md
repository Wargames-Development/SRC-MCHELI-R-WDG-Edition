# Laser soft-vegetation exclusions

The user authorized MCHR as WGMap's primary companion for this fix. Laser aim
must ignore soft plants and hit the solid block behind them; trees/leaves stay
solid. Unrelated camera/thermal and other working-tree edits were preserved.

## Implemented files

- `MCH_LaserTargeting`: exact vanilla plant exclusions and one shared native
  block-hit helper, used by authoritative server validation and client prediction.
- `WGMapLaserClientBridge`: skip those plants in loaded terrain, and discard
  an initial vanilla plant hit before it can shorten the complete laser ray.
- `MCH_LaserGuidanceSystem`: allow entities behind the ignored initial plant to
  participate in laser selection. Final tracing still checks solid terrain in
  front of them. Mark this client selection method explicitly `@SideOnly(CLIENT)`.
- `MCH_LaserVegetationTest`: isolated real Forge/vanilla registry tests for all
  14 plants and metadata values, plants in front of solid grass, double-height
  vegetation, initial aim cutoff, an entity behind vegetation and leaf cover.
  Also checks common laser classloading without client/optional WGMap types.

The list matches WGMap capture: tall grass/ferns, dead bush, yellow/red flowers,
double plants, saplings, small mushrooms, wheat, carrots, potatoes, nether wart,
reeds and vines. Grass/dirt blocks, wood, leaves, giant mushrooms, glass, fences
and slabs are not ignored. Unknown modded plants are not broadly exempted by
material or inheritance. Actual bullets and generic `MCH_RayTracer` are unchanged.

## Review and compatibility

Server authority, range/aim/weapon validation and optional WGMap handling remain
in place. Client-only chunk-provider access stays in the existing client bridge;
the new common helper uses only common block/world types on the owning thread.
Loaded-state checks still precede native reads. No new chunk load, disk read,
thread, packet, per-tick queue, entity scan or save format is introduced. The
existing bounded trace is reused. No new WGMap dev/API jar is needed.

Review verdict: ACCEPTABLE. The initial-plant-cutoff finding is fixed, including
entity selection. Test development first exposed incomplete fixture setup (FML
mod discovery had not run), then an entity whose expanded hit box extended in
front of the test leaves. Only the isolated fixtures were corrected for these
failures. No live-client or dedicated-server gameplay scenario was executed.

- PASS: `./gradlew.bat --offline --no-daemon compileJava test` with the existing
  `MCH_LaserTargetingTest` and `WGMapLaserClientBridgeTest` filters.
- PASS: focused real-vanilla vegetation/targeting/bridge tests after correcting
  the isolated mod-list fixture. Expanded entity-cover verification exposed the
  enlarged test hit box described above; the final full suite covers its fix.
- PASS: final `./gradlew.bat --offline --no-daemon check`: 181 tests, zero
  failures/errors/skips. Existing 14 Unsafe warnings remain. This includes
  projectile collision, LOD targeting and common dedicated-classloading checks.
- PASS: focused `git diff --check` for the three production files; source
  comparison with WGMap confirms identical 14-block exclusion lists.
- WGMap counterpart: `./gradlew.bat --offline --no-daemon check -Pci=true`
  passed 1,179 tests and its repository boundary/hygiene guards.

## Manual verification and WGMap cache update

Update MCHR on client/server. Aim aircraft and handheld lasers through grass,
flowers, tall plants and crops at a dirt/grass block. Repeat with an eligible
entity behind plants and with a tree between the laser and entity; solid tree
cover must win. Check native terrain with WGMap absent and cached terrain with
WGMap present, including missing/pending terrain rejection.

WGMap's companion capture change uses the same exclusions for remote snapshots.
Old UNKNOWN cells cannot be identified as plants from cached data alone. On the
updated WGMap server run Map Data Only over the affected area, then create and
distribute a fresh bundle. Ordinary Download/Full Verify can reuse complete old
snapshots. New terrain generation and automatic whole-map retransmission are
not required or introduced. No automatic migration runs. Existing formats and
versions remain readable on rollback; older recaptures can restore UNKNOWN plants.

No commits, branches, published artifacts or source archives were created.
Modded-plant adapters and precise unsupported-shape support remain deferred.
