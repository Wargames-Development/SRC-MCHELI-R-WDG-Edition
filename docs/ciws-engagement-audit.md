# CIWS engagement audit — 2026-10-13

The reported survival approach began with `inactive: check mount, team and
GunnerStatus`, then started firing later. A confirmed lifecycle bug explains this
sequence: `Entity` calls `entityInit()` during construction, setting gunner mode and
configured infinite-ammo/fuel bits. The aircraft constructor then replaced that
bitmask with radar alone. Saving to a fresh NBT compound enabled gunner mode as a
side effect, making engagement appear to start randomly after a world save.

## Findings and corrections

| Finding | Correction |
| --- | --- |
| B1: Constructor erased gunner status; saving silently enabled it | Preserve initialized status bits, make saving side-effect free, and default only a missing legacy `AcGunnerStatus` key to enabled. Explicit saved false stays false. |
| B2: Unreachable priority missiles could hold acquisition while aircraft passed | Validate intercept feasibility and predicted mount limits during acquisition; select another eligible target. Apply switching hysteresis once to the current track, after choosing the nearest candidate. |
| B3: Initial muzzle and full-circle default yaw could disagree with the final barrel | Compensate the native full-circle `DefaultYaw` branch, apply bounded relative look input through the existing mount solver, then recompute the actual muzzle, lead, sight and fire error. |
| B4: Falling `_dual`/`_term` bomb bodies were excluded even with a flag | Include explicitly interceptable bomb parent bodies. Account for target gravity and whether it has already moved during this world tick. Bomblets remain excluded. |
| B5: Generic inactive logs concealed the failing gate | Report the specific inactive reason, enabled status, seat/weapon, team/sight/traverse/intercept rejection counts, and actual barrel yaw/pitch error. Clear stale tracking when unmounted or without a supported selected weapon. |

Radar contact is not required. Acquisition scans loaded server entities, checks
the deployment team against current target ownership, and respects configured
range, line of sight, native traverse, weapon availability and alignment.
Friendly/unknown ownership and aircraft carrying a creative player remain excluded.
WGE and WGC retain their separate existing integration boundaries.

## Files changed in this follow-up

| File | Purpose |
| --- | --- |
| `src/main/java/mcheli/aircraft/MCH_EntityAircraft.java` | B1 lifecycle fix and transient physics-tick marker for lead normalization |
| `src/main/java/mcheli/mob/MCH_EntityGunner.java` | B2/B3/B5 acquisition, controller and diagnostics fixes; shared acquisition/fire prediction |
| `src/main/java/mcheli/mob/MCH_AAGunnerMath.java` | Predict falling targets using native discrete gravity and muzzle advance |
| `src/main/java/mcheli/weapon/MCH_EntityBaseBullet.java` | B4 opted-in bomb eligibility through the existing interception/hitbox path |
| `src/test/java/mcheli/mob/MCH_GunnerEngagementTest.java` | Eight constructor, NBT, acquisition and real mount-solver regressions |
| `src/test/java/mcheli/mob/MCH_AAGunnerMathTest.java` | Falling-target lead and tick-order regression |
| `docs/ciws-aa-gunner.md`, this document | Current behavior, validation, compatibility and test instructions |

## Supplied asset folder

Audited weapon filenames containing `gps`, variant suffixes `_dual`/`_term`,
`IsGPSMissile = true`, GPS display labels and matching Modifier values. There were
38 matching definitions: 34 were already enabled. Added `CanBeIntercepted = true`
to these four existing Bomb definitions:

- `cb_a10_cbu97_dual.txt`
- `cb_a10_cbu97_term.txt`
- `cb_su25_rkb500u_dual.txt`
- `cb_su25_rkb500u_term.txt`

All 38 were rechecked successfully. Other definition values were preserved.
The four originals and SHA-256 audit manifest are in
`assets/mcheli/.ciws-gps-variants-backup-20261013/` within the supplied PrismLauncher
instance. A manifest copy and application script are under the repository's ignored
`build/ciws-gps-variant-audit/`. The previous missile/rocket and C-RAM test-preset
backup remains separate, as documented in [the earlier interception fix](ciws-interception-fix.md).

## Validation and review

`gradlew.bat compileJava test --offline` under Java 8 passes: **107 tests, zero
failures/errors/skips**. The eight new engagement tests use real aircraft/gunner
constructors, NBT and the native turret rotation solver. The headless fixture
replaces Forge bootstrap, supplies a small test World and WGE-shaped team IDs, and
records weapon use instead of sending transport packets. It therefore verifies
acquisition and firing intent, not live projectile hit rate or the WGE/WGC services.
Existing projectile tests exercise real interception flags, centered boxes, swept
moving collisions, update order and damage dispatch. Final read-only review found
no remaining significant correctness issue within this CIWS scope.

No packet, registry or NBT key was renamed. No new persistent tick marker is saved.
The constructor fix also restores the configured infinite-ammo/fuel bits that it
previously erased. Existing turrets saved with `AcGunnerStatus = false` retain that
value; place a fresh turret or explicitly enable gunner mode when testing those.
No build-number change, commit, push, binary packaging or deployment was performed.

## In-game verification

Install a rebuilt matching MCHR on client and server, copy the four changed weapon
definitions to the server pack, and restart. Loose asset changes do not install Java
fixes into the existing X180 binary.

1. Place a fresh CIWS and fly an enemy survival helicopter/plane toward it before
   any world save, from each compass direction and across the yaw seam. It should
   acquire during approach once inside range, sight and traverse, turn at the
   configured input rate, and fire when the real barrel aligns.
2. Repeat after save/restart. Confirm deliberately disabled gunner mode remains
   disabled through saving and logs `inactive: GunnerStatus disabled`.
3. Launch enemy GPS, `_dual` and `_term` projectiles. Test the four cluster-bomb
   parent bodies before they split. Parent bodies should be acquired and lead
   should follow falling motion; spawned bomblets remain excluded.
4. Present an unreachable receding missile or out-of-sector missile alongside a
   reachable enemy aircraft. The missile must not monopolize targeting. Check
   rejection counts and `aimError(yaw/pitch)` if a target is not engaged.
5. Repeat with friendly/unknown shooters, creative aircraft, mixed crew, terrain
   cover, empty ammo, reload and team changes. Friendly targets must not receive
   target-directed fire. Repeat WGE and WGC on separate packs.

The maximum-response preset remains enabled for diagnosis. Ordinary weapon speed,
traverse, ammo and cooldown still apply. Live hit rate, observer interpolation,
guided-target maneuver prediction and actual team-service behavior remain manual
verification items; this patch does not promise interception beyond those limits.
