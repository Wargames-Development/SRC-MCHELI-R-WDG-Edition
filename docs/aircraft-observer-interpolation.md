# Smooth aircraft turns for observer cameras

Minecraft 1.7.10's bundled `EntityTrackerEntry` quantizes yaw/pitch to 256
values per revolution and normally waits for a four-unit change (5.625 degrees)
before sending a look update. Frame interpolation cannot reconstruct the small
turns omitted by that stream. The mod's existing entity-info snapshots already
carry full-precision yaw, pitch and roll every two server ticks.

## Implementation and review

- `MCH_RenderFarVehicle.java`: loaded planes and helicopters viewed by another
  player now interpolate those precise snapshots. Snapshot intervals are bounded
  to 50–200 ms, use a monotonic render clock, and hold at the last received target
  during gaps. Normal delivery adds approximately one 100 ms snapshot interval
  of presentation delay. Position interpolation remains as before.
- Only fresh (at most 500 ms old), finite, UUID-matched snapshots are accepted.
  Missing/old snapshots use the existing entity rendering path. Initialization,
  teleports over 16 blocks, identity changes and local rider transitions discard
  the prior interpolation. Local pilots, passengers and UAV operators keep the
  existing camera/model behavior. Tanks and generic vehicles are unchanged.
- **B1, fixed during review:** ordinary Euler interpolation produced a false
  half-turn when pitch crossed vertical. Choosing the nearest equivalent Euler
  representation preserves the received attitude and avoids that inversion.
- **B2, fixed in pitch follow-up:** Forge 1.7.10's mod-entity spawn message
  creates a new local UUID rather than transmitting the server UUID. The first
  patch's UUID guard therefore discarded precise snapshots for loaded aircraft.
  `MCH_EntityAircraft.java` now appends an optional 20-byte `AID1` UUID trailer to
  spawn data and reads it before the client entity enters the world. The server
  UUID and all existing spawn fields remain unchanged. No NBT migration is needed.
- `MCH_GunnerEngagementTest.java`: reuse the existing headless aircraft fixture
  for two spawn regressions. A fully typed plane round-trips its server UUID;
  the actual loaded render pose advances to 0.5 degrees of pitch while vanilla
  entity pitch remains zero. Absent/truncated trailers retain the local UUID and
  still load the existing size fields. The UUID test failed before B2's fix.
- **Review verdict: ACCEPTABLE.** No entity attitude, controls, physics, server
  authority, existing packet fields/IDs, NBT, definitions, registries,
  dependencies or build configuration change. The optional spawn trailer is
  ignored by older clients; newer clients accept older servers without it.
  Smooth precise observer pitch requires both server and filming client updated,
  followed by reconnecting or re-tracking the aircraft to receive new spawn data.
- `MCH_AircraftSnapshotRotationTest.java`: five regressions cover slow turns at
  20/60/144/240/1000 FPS, angle wrapping, loop inversion, irregular delivery,
  packet gaps, clearing state and invalid angles. The inversion test failed
  before B1's correction and passes afterward, including a matrix-equivalence check.

## Test and verify

`gradlew.bat compileJava test` passed: 135 tests, zero failures/errors/skips.
`git diff --check` passed. No tracked build metadata changed.

**T1, pending in-game verification:** update the server and filming client,
reconnect, and use a second player as the filming camera. First hold roll/yaw
steady and slowly pitch up/down through angles below the 5.625-degree threshold.
Expect continuous pitch changes rather than waiting for a vanilla look packet.
Film slow banking turns, turn reversal/release, heading across 0/360, full rolls
and vertical loops at 60 and 144 FPS. Expect continuous attitude between frames
and no wrap/inversion spins. Check pilot/seat, weapon and effect alignment in
close shots, then repeat across loaded/unloaded aircraft handoff. Check cockpit,
third-person pilot, passenger and UAV views for unchanged responsiveness. Test
packet gaps/recovery and teleport/remount; prolonged gaps should fall back safely.
A dedicated-server launch and actual trailer footage have not been exercised.
