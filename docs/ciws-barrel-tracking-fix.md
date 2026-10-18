# CIWS barrel feedback and pitch limits — 2026-10-16

## Evidence and bounded diagnosis

Gunner 97 acquired enemy aircraft 84 (`eligible=1`) and remained enabled, with full
ammo and no heat. The fire gate consistently reported substantial yaw/pitch error,
so it correctly withheld a shot for the barrel direction it measured. The final
sample found an interceptable missile but rejected both candidates by traverse.
Gunner 82's earlier `unmounted` messages describe a different entity and cannot
explain gunner 97's firing gate.

The logs do not prove why look and barrel disagree in that particular live session.
This patch fixes two reproducible controller weaknesses and adds the missing angle
diagnostics. It does not raise fire tolerance to fire away from the predicted aim.

## Implement, review and fixes

- **B1 — Feedback used entity look instead of native barrel direction.** When
  look and barrel differ, repeating a bounded step from look can leave the gun
  short of the target. The controller now computes its next bounded step from
  the actual weapon rotation, translates that into relative look input, and lets
  the existing mount solver perform the rotation. The full-circle `DefaultYaw`
  compensation remains. It does not directly assign the weapon's angles.
- **B2 — Vehicle body pitch limits incorrectly restricted the gun.** The supplied
  C-RAM has body limits -85..+10 degrees, while its gun has -80..+25. Both acquisition
  and steering previously intersected them, rejecting legal gun depression beyond
  +10. They now use the native gun mount limits. The AA model also uses the already
  constrained gun pitch, avoiding a second body-pitch clamp during rendering.
- **D1 — Traverse diagnostics did not distinguish yaw from pitch.** The last
  rejected candidate now has an ID and specific `yaw limits`/`pitch limits` reason.
  A `[CIWS aim]` companion line reports profile, weapon, look speed/response,
  current look, requested look, actual barrel, predicted aim, body rotation and
  native mount limits. It retains the existing 40-tick debug cadence.

Read-only review checked common/client boundaries, unchanged weapon firing and team
gates, native yaw mapping, pitch-limit ownership, null-safe diagnostics, and bounded
barrel movement. Verdict: acceptable within this CIWS scope. No networking,
registration, persistent data, pack definition or weapon damage/cooldown changes.

## Files changed

| File | Reason |
| --- | --- |
| `src/main/java/mcheli/mob/MCH_EntityGunner.java` | Barrel feedback, native gun pitch limits and angle/rejection diagnostics |
| `src/main/java/mcheli/vehicle/MCH_RenderVehicle.java` | Render AA gun elevation without clamping it to vehicle body limits; share the existing operator check |
| `src/test/java/mcheli/mob/MCH_GunnerEngagementTest.java` | Six new controller and native mount regressions |
| `docs/ciws-aa-gunner.md`, this document | Current validation, testing instructions and limits of the diagnosis |

## Verification

Both the forced look/barrel mismatch regression and the independent gun/body pitch
regression failed before the controller corrections and pass afterward. Additional
tests cover eight compass bearings on a rotated vehicle, sustained tracking over a
200-tick moving aircraft crossing (more than 190 aligned firing intents), and an
incoming missile inside C-RAM's actual +25-degree depression limit. The mismatch
test checks that native yaw/pitch steps remain at most 15 degrees per controller
update with the maximum-response preset.

After clarification that both CIWS failed against a hovering helicopter, an
additional regression uses both supplied gun mount geometries (C-RAM and HPJ 1130),
their body pitch limits, and the real server `updateWeapons()` before each AA
controller call. Each sustains aligned firing intent at a stationary enemy
helicopter from eight bearings, then gives priority and firing intent to an
incoming missile. This passes with the latest controller. It does not independently
reproduce the original live-server stall or execute full vanilla mounted living
entity updates; no additional production behavior was changed for this clarification.

Java 8: `gradlew.bat compileJava test --offline` passes **114 tests, zero
failures/errors/skips**. `git diff --check` passes. Build number remains 180.

These headless tests invoke the real controller and native weapon rotation solver;
they record firing intent instead of sending packets. An attempt to run the entire
vanilla mounted living-entity lifecycle in the fixture was blocked by Forge's
LaunchWrapper/registry bootstrap requirement; that attempted test was replaced by
explicit controller/native-mount coverage. No full live mounted-lifecycle or WGE/WGC
service verification is claimed. The original live yaw discrepancy is not conclusively
reproduced by these logs alone. No replacement binary was packaged or installed.

## In-game checks

1. Install matching rebuilt MCHR client/server binaries, restart, and use the existing
   maximum-response AA profile. No further asset edits are required by this patch.
2. Fly an opposing survival aircraft across the CIWS at a few hundred blocks, then
   repeat approaching and hovering from different compass bearings. Watch barrel
   motion, ammo consumption and target damage. Allow the configured turn time.
3. Launch an enabled enemy missile at the turret; also test an approach below it
   that needs +10..+25 degrees of depression. Compare visible elevation with the
   actual gun direction. Targets beyond -80..+25 remain outside C-RAM's mount limits.
4. Repeat friendly/unknown/creative exclusions, terrain cover and exhausted ammo.
   Confirm the original normal firing restrictions still apply.
5. If it stalls, capture both `[CIWS]` and `[CIWS aim]` for the same gunner over
   several samples. `requested` versus `barrel` exposes native rotation disagreement;
   `barrel` versus `aim` exposes the alignment gate. `rejectedTarget` describes the
   last rejected candidate in the scan, not necessarily the current track.
