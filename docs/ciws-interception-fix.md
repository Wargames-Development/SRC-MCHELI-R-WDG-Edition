# CIWS interception fixes and temporary test preset

## Confirmed findings and fixes

- **B1 — Wrong interception flag owner.** Physical impacts read the attacking
  round's `CanBeIntercepted`, whereas lasers read the target missile's flag. Physical
  collision eligibility, impact, and thrown projectile damage now consistently read
  the target's flag. AA acquisition also requires it. Earlier guidance to enable the
  flag on C-RAM ammunition described the broken implementation; enable it on the
  missile/rocket weapon instead. The existing special guided-missile collision pairs
  remain compatible, but AA-fired rounds still reject friendly/unknown missiles.
- **B2 — Fast targets missed between ticks.** Both collision loops tested a stationary
  target box. They now use a common relative-motion sweep for projectile pairs, with
  the actual movement if the target already updated, or its upcoming step otherwise.
  A crossing missile can be hit even if neither endpoint box intersects the round's
  static ray. Terrain clipping also scales the target movement to the clipped ray's
  fraction of a tick, preventing a collision behind a wall from being moved ahead of it.
- **B3 — Offset and stale missile boxes.** Missile models are centered at their position
  but vanilla boxes extended upwards from it. Missile boxes now center on the actual
  position. The spawn hook and projectile update repair raw position changes before
  world tracking/collision; interception damage also uses the real position without
  adding another half-block offset. Box size remains one block with the existing 0.3
  collision margin. Other projectile types retain their existing vertical anchoring.
- **B4 — Previous-tick barrel gating.** The AA operator issued look input and checked
  the preceding barrel state, often refusing fast targets. It now feeds the bounded
  input through `updateWeaponsRotation` before checking actual barrel alignment.
  It does not assign target angles directly to the weapon set.
- **B5 — Prediction offsets.** Aim uses the real box center and weapon shot origin.
  It includes the MG's unscaled half-launch-speed spawn advance, 3.9 cap, fast-bullet
  multiplier, and scaled discrete gravity. Missile positions normalize to tick start
  when they already updated, avoiding a one-tick lead discrepancy. Aircraft velocity
  comes from server motion, rather than an update-order-sensitive position difference.

The aircraft observation during creative testing is explained by the existing rule:
aircraft with a creative player onboard are excluded. That rule remains. Test enemy
aircraft with survival-mode pilots. Missile shooters may be creative; their missiles
are filtered by team and interception eligibility rather than player game mode.

## Installed temporary tuning

The supplied PrismLauncher instance has these asset changes:

- `gunners/gunner_aa_default.txt`: range 1000, scans every tick, zero reaction delay,
  look speed 15 degrees/tick, full tracking response, zero aim bias, continuous fire,
  and `DebugGunner = true`.
- 117 native AA/AT/AS/TV missile and rocket weapon definitions:
  `CanBeIntercepted = true`. This temporarily overrides the explicit false setting
  on `iragm_puma_spikelr.txt` too; no weapon registration names or IDs change.
- `weapons/ac_cram_m61a1.txt` and `weapons/ac_hpj1130_30mm.txt`: `Accuracy = 0`,
  `HeatCount = 0`, `MaxHeatCount = 0`. Damage, projectile speed, ammo, reloads, firing
  interval, and mount sectors remain their existing values.

Before this preset, C-RAM accuracy was 1.2, HPJ 1130 accuracy was 1.1, and both guns
had HeatCount 3 and MaxHeatCount 400. The original AA profile used range 500, scan
interval 5, reaction 6, look speed 6, response 0.5, and aim error 0.25.

Originals and a per-file SHA-256 manifest are saved under the supplied asset root:
`.ciws-interception-backup-20261013/weapons`, `.../gunners`, and `.../manifest.json`.
The backup is outside the definition loader folders. To restore balanced tuning,
copy originals back to the corresponding folders and restart. Keep missile flags
enabled where interception is desired with the corrected code. The staging copies
and manifest are also in the repository's `build/ciws-interception-tuning` directory.

**Install a rebuilt mod on client and server, copy the same updated definitions to
the server, and restart both.** This change edits source and loose client assets;
it does not replace either running mod binary or upload files to the server.

This is a high-cost diagnostic scan preset for a few test turrets, not production
balancing. It removes aim randomness and weapon overheating, not geometry or physics:
friendly/unknown targets, walls, creative-piloted aircraft, mount limits, finite ammo,
reloads, targets outside loaded chunks, and impossible intercepts still apply. No
guaranteed-hit deletion or target snapping was introduced. Maneuvering/accelerating
targets can deviate from constant-velocity prediction; a live hit-rate claim needs
server testing.

## Diagnostic output and regression checks

With `DebugGunner = true`, search the server log for `[CIWS]`. Every 40 ticks it records
gunner ID/team, state, target ID, observed missile count, enabled missile count,
eligible target count, ammo, and heat. States distinguish no weapon, inactive mount/
team/status, reaction delay, impossible intercept, turning, cooldown/reload, firing,
and weapon refusal. Set the parameter false after diagnosis.

1. Place opposing-team C-RAMs, fire enabled hostile missiles across their range, then
   repeat head-on and with crossing rockets. Missile collision boxes (debug hitboxes)
   should follow their rendered positions and rounds should physically intercept.
2. Fire a missile with `CanBeIntercepted = false`: it must not be acquired by AA or
   destroyed by ordinary thrown-round impacts. Its weapon flag does not change which
   team it belongs to. Special legacy guided-missile pairs remain an exception.
3. Repeat with friendly, mixed friendly/enemy launcher crews, unknown shooters, and
   terrain cover. AA must not target them. Validate survival-piloted enemy planes and
   helicopters; creative-piloted aircraft should remain excluded.
4. Observe `[CIWS]` summaries during a failure. `missiles>0, interceptable=0` points to
   missing server definition flags; enabled but `eligible=0` points to ownership,
   visibility, or mount sectors. Repeated weapon refusal points to ammo/heat; repeated
   turning points to the rate/geometry limits. Check both the server and observer view.
5. Repeat after a save/restart and test a target switching to the gunner's team.

Validation: `gradlew.bat compileJava test --offline` passes under Java 8: **98 tests**,
zero failures/errors/skips. New checks cover continuous crossing/head-on hits,
already-updated versus not-yet-updated target order, terrain truncation, centered and repaired boxes,
target-side flags, friendly/unknown rejection, actual-position damage dispatch,
muzzle advance, and discrete gravity. The 119 weapon copies and AA profile were
verified against staging hashes. Live WGEvents server hit rate remains unverified.

Changed production files in this follow-up: `MCH_EntityGunner.java` (acquisition, aim,
fire timing, diagnostics), `MCH_AAGunnerMath.java` (launch/gravity math),
`MCH_GunnerInfo.java` (test defaults and diagnostic parameter),
`MCH_EntityBaseBullet.java` (missile boxes, target flags, moving impacts),
`MCH_EntityBullet.java` (use common sweep), `MCH_ProjectileCollision.java` (sweep),
`MCH_EventHook.java` (repair at spawn), and `gunner_aa_default.txt` (test preset).
Tests: `MCH_AAGunnerMathTest.java` and `MCH_ProjectileCollisionTest.java`. Both CIWS
documents are updated. No commits, pushes, build-number change, or deployment occurred.
