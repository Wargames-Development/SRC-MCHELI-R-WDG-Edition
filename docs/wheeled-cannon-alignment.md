# Wheeled acceleration buff and moving cannon alignment

## Scope and implementation

The user requested 50% more acceleration on all enabled wheeled definitions and
a fix for cannon rounds appearing ahead of a moving vehicle when firing sideways.

- The 11 X173 tank definition files have their `WheeledAcceleration` multiplied
  by exactly 1.5. No road speed, steering, throttle ramp, weapon or other keys
  change. Three decimal places preserve S-300's exact `0.55 -> 0.825` increase.
  ITV/VDV change `2.50 -> 3.750`; LAV25 changes `1.30 -> 1.950`.
- The initial `MCH_EntityTank.java` fix used the received position directly.
  This has been superseded by the multiplayer correction described below, after
  the user reported repeated forward movement followed by backward corrections.
- `researched-wheeled-profiles.md` updates the current values and estimated times;
  `wheeled-handling.md` documents the translation behavior. This report records
  diagnosis, sequential review and regression checks.

## Diagnosis and evidence

The actual configured Forge 1.7.10 sources confirm that `NetHandlerPlayClient`
passes three interpolation ticks for entity movement. The mod registers tanks
with a one-tick tracking frequency. The preceding wheeled client code moved only
one third of the remaining distance toward every fresh packet's position.
At constant velocity, repeatedly replacing the target each tick leaves the drawn
chassis approximately two velocity ticks behind the received authoritative pose.
At 100 km/h that is approximately 2.78 blocks of extra lag; the offset is zero at
rest and grows linearly with speed.

`PacketUseWeapon` builds the shot origin from the server aircraft position,
not client-supplied coordinates. `MCH_WeaponBase.use` adds the transformed local
weapon offset. Machine-gun/cannon launchers advance rounds along their firing
direction; that forward barrel advance cannot explain a movement-direction
offset when firing perpendicular to vehicle motion. Reducing the chassis's extra
positional delay addresses the confirmed mismatch without moving authoritative
projectile origins backward or changing ballistics/hit detection.

This diagnosis explains the reported speed-dependent presentation symptom.
Actual in-game firing and packet-arrival timing remain to be tested. It does not
promise zero network latency or eliminate the physical separation between a
vehicle and a round after that round has left the barrel.

## Sequential review

Verdict: ACCEPTABLE WITH MINOR ISSUES.

- Production scope is one client presentation branch in an existing common
  entity, plus 11 definition values. No new client-only dependency, manager or
  per-tick allocation is introduced.
- The server simulation, firing origin, collisions, damage, packet IDs/layouts,
  registrations, NBT and tracked behavior retain their existing paths.
- Heading smoothing and turret world-aim compensation remain intact. Changes
  do not reset previous tick position or angle snapshots used for rendering.
- T1: live driver/passenger/observer firing tests are unverified. The regular
  JUnit runner does not bootstrap Forge's `LaunchClassLoader`, which the full
  tank constructor requires. A trial whole-entity test was removed after its
  initialization failed; no new test dependency or launch framework was added.
- T2: using received positions immediately can expose packet-arrival jitter
  under poor network conditions. Frame interpolation remains active, but this
  needs the packet-loss and observer checks below.

## Validation and manual checklist

Java 8 `gradlew.bat compileJava test --offline` succeeded with all 42 supported
existing tests passing, zero failures/errors. `git diff --check` passed. An
independent read-back check verified all 11 acceleration values are exactly 1.5
times their backups and every other byte/all 16 tracked files are unchanged.
Build number remains 170. No Forge client/server was launched.

1. With matching rebuilt code and definitions, time ITV/VDV and LAV25 launches
   from rest on a flat road at 20 TPS. Expect approximately 4.7 versus 9.5 seconds
   to 60 km/h with full input; eventual top speeds retain the preceding values.
2. Aim a LAV cannon exactly sideways on a straight road. Fire while stopped,
   moving slowly, at full speed and while reversing. Compare both left and right
   sides. Expect the extra movement-direction gap at launch to disappear.
3. Repeat with an independent passenger gunner, an external observer and the
   driver's first/third-person view. Check muzzle flash, tracer and barrel origin,
   not only the projectile position several ticks after launch.
4. Steer with a stationary mouse, including crossing the +/-180-degree heading
   boundary. Expect smooth hull/turret motion and maintained world aim.
5. Repeat sideways firing while accelerating, braking, on slopes and after
   hitting a wall. A received stopped position must not be extrapolated forward.
6. Repeat with artificial latency/packet gaps. Look for translation jitter,
   freezes, corrections or a remaining barrel/projectile timing mismatch.
7. Confirm a tracked tank's movement, cannon firing, ammunition and impacts retain
   their behavior. Verify dedicated-server firing and hits with another player.

The external originals, plan and SHA-256 verification are in
`build/wheeled-acceleration-buff-c28af4e2d6cb43bca44e99fdbf629aff/`.
The new position fix requires rebuilding and installing the mod; the definition
buff applies when the updated pack is loaded/reloaded. No binaries were installed.

## Multiplayer position reconciliation follow-up

The preceding snap-to-packet approach exposed network delivery jitter. The
legacy client fallback could also continue integrating velocity after its three
interpolation ticks expired, then snap backward when a later position arrived.
The lack of a single consistent position path was a confirmed code defect (B1).

Changes:

- `MCH_EntityTank.java` resets a client-only age counter when a position packet
  arrives. Both its interpolation and no-new-packet branches use the same helper.
  It no longer enters legacy free-running movement/damping for wheeled vehicles.
- `MCH_WheeledControlMath.java`, `clientPosition`, advances by server velocity
  before correcting one third of the residual position error. Under regular
  per-tick constant-speed updates, prediction reaches the received target, so
  the correction is zero rather than adding the previous two-tick barrel lag.
- Targets advance with packet age for at most two missing updates (100 ms at
  20 client ticks/second). Further missing updates hold moving axes until fresh
  state arrives. A zero-velocity axis continues settling toward its received pose.
- While velocity is nonzero, correction is bounded to its magnitude, so a stale
  target cannot move the axis opposite that velocity. Fresh server stop/reverse
  velocity permits correction in the appropriate direction. Errors over 32
  blocks on an axis accept the new target as a teleport instead of chasing it.
- `MCH_WheeledPositionTest.java` tests steady-speed barrel translation, two-tick
  packet gaps and expiry, late packets, stops/reversals, delayed three-tick
  cadence, slower server cadence, initial/invalid data and real teleports.
- This document and `wheeled-handling.md` describe the current behavior.

Sequential review: ACCEPTABLE WITH MINOR ISSUES. B1 is addressed. No gameplay
prediction is sent to the server or used for hits. No packet layout, registry,
NBT, definition value or tracked movement path changed. The age counter is
presentation-only and does not need persistence. No allocation/client-only import
was added to the pure math. Yaw/turret/frame interpolation is preserved.

Java 8 `gradlew.bat compileJava test --offline` passed with all 48 tests, zero
failures/errors. Tests run the production math without requiring a Forge entity
constructor or a new test dependency. No client/server session was launched.

Remaining T1: on the real server, drive ITV/LAV straight and watch from driver,
passenger and observer views; repeat with short packet gaps and low server TPS.
Expect smooth forward/reverse progress instead of repeated small backward snaps.
Test acceleration, braking into a wall, reversal, slopes, a large teleport, and
a longer disconnection: brief gaps bridge, long gaps hold, fresh stopped state
settles back to the server pose. Repeat sideways firing at several speeds and
stationary-mouse steering across yaw wrap to check barrel alignment and aim hold.
Check a tracked tank as a regression control. Severe latency or low server TPS
still limits responsiveness; the math tests do not establish live network quality.
