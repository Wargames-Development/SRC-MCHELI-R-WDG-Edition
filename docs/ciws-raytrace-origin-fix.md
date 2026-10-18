# CIWS ray-trace origin corruption — 2026-10-16

## Confirmed cause

The 22:27 server log shows requested look, entity look and actual barrel agreeing,
while the reported aim differs by as much as 178 degrees. This rules out a native
mount failing to follow the requested direction in those samples. It instead
points to inconsistent geometry before and after the look input.

Minecraft/Forge 1.7.10's `World.rayTraceBlocks` delegates to the vanilla voxel
walker, which changes its **starting Vec3 in place** as it advances through blocks.
The AA code passed its shared world-space muzzle directly to visibility checks.
The modified muzzle was then reused for acquisition, prediction and steering.
The final visibility check could also modify the muzzle before the firing-angle
calculation. That created contradictory aim directions for the same target.

The installed `mcheli/mob/MCH_EntityGunner.class` was inspected with `javap` and
contains the preceding controller and diagnostics code; this is not simply an
installation missing those earlier changes. Its visibility calls still have the
shared-vector defect addressed here.

## Changes and review

- **B1 — Shared muzzle corruption:** all three AA visibility checks now pass
  disposable endpoints through a shared helper. Acquisition and fire geometry
  retain the actual muzzle coordinates. No weapon rotation, firing tolerance,
  damage, ammunition, team identity or profile setting was bypassed or increased.
- **B2 — Single-ray traversal cap:** vanilla stops after 200 voxel steps, even
  when it has not reached a distant endpoint. Visibility is now checked in
  100-block segments, requiring at most approximately 174 voxel steps each.
  Terrain beyond the original limit can no longer be treated as clear by the AI.
  Each segment checks existing chunks before querying blocks; uncertain paths
  across missing terrain are rejected without forcing chunk loads.
- **T1 — Inaccurate test double:** clear visibility previously returned null
  without tracing or modifying vectors, hiding B1 and B2. The test World now uses
  the real vanilla ray tracer over supplied air/solid test blocks. Its separate
  explicit blocked-ray switch remains for earlier controller tests.

Review verdict: acceptable. Dedicated-server APIs and SRG names were checked
against the configured Forge dependency. The geometry is local to the server AA
controller; no packet, registry, NBT, save format or definition changed. Loaded
chunk checks are conservative over each segment's bounding rectangle, so terrain
with missing nearby chunks can temporarily prevent a shot until those chunks are
loaded normally.

## Files changed

| File | Reason |
| --- | --- |
| `src/main/java/mcheli/mob/MCH_EntityGunner.java` | Preserve muzzle coordinates at all visibility boundaries; cover the complete sight path without chunk loads |
| `src/test/java/mcheli/mob/MCH_GunnerEngagementTest.java` | Exercise real vanilla tracing, hover/missile acquisition, distant walls and missing chunks |
| `docs/ciws-aa-gunner.md`, this document | Updated diagnosis, validation and concrete installation instructions |

The both-CIWS hovering/missile regression failed after switching the fixture to
the native ray tracer, before the production fix. It passes afterward. The distant
wall regression also failed before segmented tracing and passes afterward.
Earlier passing tests were insufficient because their visibility stub omitted the
native method's mutating behavior; that gap is now corrected for all engagement tests.

## Validation and compiled update

Java 8: `gradlew.bat compileJava test --offline` passes **118 tests, zero
failures/errors/skips**. `gradlew.bat jar reobf --offline` also passes, producing
`build/libs/MCHELI-R-WDG-Edition-1.9.01_X180-dev.jar`. `git diff --check` passes.
Build number remains 180. No commit, push, release upload or live deployment occurred.

`build/ciws-raytrace-fix/ciws-code-update-X180-raytrace.zip` contains all reobfuscated
`mcheli/**/*.class` files and `mcmod.info` from that jar: **836 entries**, with no
assets. Every entry was SHA-256 compared against the rebuilt jar. The archive hash
and source jar path are recorded in the adjacent `manifest.json`.

## Installation and game checks

1. Stop the client and server. Back up their current `mcheli/` code folder and
   `mcmod.info` outside the active mods directory. Replace those two entries with
   the ZIP contents in the existing extracted MCHR installation. Keep the tuned
   `assets/` folder. Do not add the rebuilt jar alongside the extracted mod; use
   one MCHR installation per side. Use this same code update on both sides.
2. Restart and deploy opposing-team C-RAM and HPJ 1130. Hover a survival helicopter
   within their gun elevation limits at several bearings and ranges. The muzzle
   must turn smoothly toward the target, alignment errors must settle, and ordinary
   firing must consume ammunition. Confirm observer barrel direction matches.
3. Fire enabled hostile missiles toward each gun, including while it tracks the
   helicopter. Confirm missile priority, barrel alignment and physical interception.
4. Test a real wall more than 200 blocks along a longer sight path. It must reject
   the target for sight. Repeat friendly/unknown/creative exclusions and targets
   outside native traverse. Missing terrain must not cause chunk loading.

Actual multiplayer hit rate, observers and live WGE/WGC services still require
these game checks. The tests use the real visibility and mount solvers but record
firing intent instead of running a dedicated server's packet transport.
