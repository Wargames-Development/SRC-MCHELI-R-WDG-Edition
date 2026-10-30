# Trailer Mod1 tracer and player-fire correction

This records the X195 correction. The subsequent all-round tracers and X197
CIWS firing-rate update are documented in [ciws-high-rate.md](ciws-high-rate.md).

Target instance only:
`C:/Users/nateb/AppData/Roaming/PrismLauncher/instances/Trailer Mod1/minecraft`.
This correction did not modify the Wargames Release instance.

## Confirmed findings

- **B1: Wrong live instance / missing integration.** Trailer Mod1's expanded
  MCHR classes had no `fmurBullet` fields or tracer bridge. Its FMUR X189 jar did
  not contain `TracerRenderApi`. Its weapon files lacked the earlier FMUR and
  CIWS settings. Source changes and changes in another instance could not make
  this instance use the new behavior.
- **B2: Altered familiar tracer appearance.** The initial shared API changed
  FMUR's original line/point glow into quads. The API now uses the original FMUR
  gun geometry, widths, colors, alpha and length formula, with state cleanup.
- **B3: Player cooldown restarted by stale acknowledgements.**
  `MCH_WeaponSet.applyServerUseState` reapplied a server cooldown measured at the
  earlier shot when its reply arrived later. With a two-tick round trip, a
  120-tick regression scenario produced 40 player firing requests instead of
  the expected 60. Positive acknowledgements now cannot extend an already
  elapsed local cooldown. Negative switch delays and reload state still
  reconcile exactly. Every projectile remains subject to the original server
  cooldown, ammo, heat and pending-intent checks.

The user confirmed the slowdown occurred under player control. Gunner burst
profiles were not changed. C-RAM still has `Delay = 2`, HPJ1130 `Delay = 1`.

## Files / behavior

| File | Change |
|---|---|
| MCHR `src/main/java/mcheli/weapon/MCH_WeaponSet.java` | Reconcile positive cooldowns without restarting elapsed client prediction |
| MCHR `src/test/java/mcheli/weapon/MCH_WeaponCadenceTest.java` | Reproduce delayed-ACK cadence, plus reload/switch/server gating regressions |
| FMUR `src/main/java/com/flansmod/client/TracerRenderApi.java` | Restore exact original FMUR gun tracer appearance in the shared implementation |
| MCHR `tools/CiwsTracerGpuChecks.java`, `tools/ciws_tracer_checks.gradle` | Compare shared pixels against the original FMUR renderer under fixed-function and GLSL rendering |
| MCHR `tools/tracer-reference/OriginalGunTracerReference.java` | Frozen independent renderer reference, not shipped in either mod |
| MCHR `tools/Install-TrailerTracerFix.ps1` | Install verified runtime-mapped code while preserving expanded custom content |
| Both `gradle.properties` | Normal successful packaging increments: MCHR X194 to X195, FMUR X189 to X190 |

Reapplied the earlier content migrations to this instance: 16 small-arms weapon
definitions (28 mounts on 17 of 64 audited vehicles), plus only the two CIWS
visual opt-ins. The small guns use the existing FMUR ordinary/red-tracer types
on a 4:1 cadence. C-RAM/HPJ1130 still fire native MCHR projectiles; their vehicle
damage, fuse timing and missile interception are unchanged. No Flan ammo file,
packet layout, registration identifier or vehicle save format was changed.

## Validation and installation

- The timing tests failed before the fix and passed afterwards.
- `gradlew.bat compileJava test --offline` with the optional FMUR/content test
  paths pointing to Trailer Mod1: **193 tests, zero failures/errors/skips**.
- The native GPU harness: **236 assertions passed**, including 28 original-vs-
  shared render comparisons within one RGB intensity level, both calibers,
  shader/no-shader, near/far, side/head-on, terrain cover and state cleanup.
- Both mods' `gradlew.bat jar --offline` tasks passed their runtime/SRG mapping
  verification. FMUR used Trailer Mod1's installed Angelica compile API.
- Installed MCHR X195 code and metadata into the existing expanded X194-named
  directory, preserving its custom assets. All **853** installed class/metadata
  entries were independently SHA-256 checked against the verified runtime jar.
- Installed `FMUR-WDG-Edition-1.8.06_X190.jar`; its SHA-256 matches the built jar.
  The exact old X189 jar was removed after verification, leaving one active FMUR.
  FMUR SHA-256: `7564857CFAE5A79D3355FA58D1B359A63294CA1CAE0DFDA0237AAC5589836128`.
- Confirmed the installed MCHR fields and shared FMUR API using `javap` against
  the live instance. Verified CIWS opt-ins are restricted to C-RAM and HPJ1130.
- `git diff --check` passed in both repositories.

No game was running during installation. A full Minecraft/BountifulStars visual
or multiplayer session was not launched; GPU comparisons use the real renderer
under native OpenGL and a representative GLSL entity program.

Restart **Trailer Mod1**. Fire a regular FMUR red-tracer gun and the converted
7.62/.50 mounts downrange, then hold fire on both CIWS under player control.
Expect the familiar FMUR glow, small-arms tracers on shots 5/10/etc., continuous
CIWS timing without extra pauses from server replies, original reload/heat
limits, and normal gunner missile interception. Repeat with shaders on/off.
