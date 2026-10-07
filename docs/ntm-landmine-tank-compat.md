# NTM landmines and MCHR tanks

NTM owns mine detection. The compatibility change is in the neighboring
`HBM-Space-WDG-Edition` repository, in
`src/main/java/com/hbm/tileentity/bomb/TileEntityLandmine.java`.
Rebuilding MCHR alone does not include this change; deploy matching updated
NTM builds to the server and clients.

The existing server-side mine query now accepts non-dead `MCH_EntityTank`
instances and subclasses as well as living entities. This includes tracked
and wheeled definitions using that entity type, with or without a driver.
The type check uses class names so NTM can still run without MCHR installed.
No new world scans, chunk loads, packets, NBT fields, or dependencies are added.

Tanks inside an unprimed mine's clearance radius prevent it from arming, just
like living entities. Once primed, a tank in its normal trigger volume causes
the mine's existing `Landmine.explode` path to run. The world-generation
`waitingForPlayer` rule and the clear-block-above-mine requirement remain intact.

Damage needs no second implementation. NTM's mine processor already attacks
non-living entities with explosion-tagged Sedna damage sources. MCHR recognizes
these as NTM explosions and applies its existing
`NTMExplosionDamageMultiplier`, vehicle explosion multiplier, permissions,
and damage protections. Mine damage values remain controlled by NTM.

## Validation

NTM `gradlew.bat compileJava --offline` passed on 2026-10-06. The source diff
passed `git diff --check`. NTM has no existing unit-test source set or test
framework for this tile entity; none was added for the compatibility guard.
Live client/server checks have not been run.

## In-game regression checks

1. On flat ground, place an HE mine, leave its clearance radius, and wait for
   its arming sound. Drive a tracked tank through its trigger volume. Confirm
   the mine is consumed once and the tank's HP decreases with damage enabled.
2. Repeat with a wheeled tank definition and in reverse, then compare the
   dedicated server's HP with the driver's and another player's display.
3. Repeat with AP, shrapnel, and nuclear mines. Verify each keeps its configured
   explosion effects and damage; do not expect identical damage across variants.
4. Place a mine near a stationary tank. Confirm it does not arm until the tank
   and living entities leave the clearance radius. Then drive back through it.
5. Confirm ordinary player/mob triggers, defusing, and world-generated mines'
   waiting-for-player behavior still work. Items/projectiles must not acquire
   tank-trigger behavior.
6. Test a parked/unoccupied tank within the trigger volume of an already armed
   mine. Detection must not require a rider.
7. Launch both client and dedicated server with NTM alone, then with NTM and
   MCHR. Confirm the optional type check causes no missing-class failures.
