# Airburst ranging and vehicle blast damage

## Changes and diagnosis

- **B1 — terrain-only ranging:** `resetAirburstDistance` now uses
  `MCH_RayTracer.rayTraceAirburst` to select the nearest loaded MCHR vehicle hull
  or rotated component hitbox, stopping at intervening terrain and excluding the
  firing vehicle. A miss clears programming to zero. The original camera origin
  is preserved even though the legacy segmented terrain tracer mutates its input.
- **B2 — reversed component rays:** `MCH_AircraftBoundingBox.calculateIntercept`
  used `Vec3.subtract` as receiver minus argument. Minecraft 1.7.10 implements
  the reverse. Explicit coordinate differences now produce forward, correctly
  positioned intersections, also benefiting other users of this shared hitbox ray.
- **B3 — origin-only blast range:** `MCH_Explosion` now measures MCHR vehicle
  blast falloff from the closest hull/oriented component surface. A burst beside
  an extended wing can damage its parent even when the vehicle origin is outside
  the blast and vanilla's chunk query misses it. Loaded aircraft supplement that
  query once per explosion; no chunk loading or damage per component is added.
  Existing hull cover sampling remains, with an exposed nearest surface also
  counted. A zero knockback direction no longer discards vehicle damage.
- **B4 — wrong explosion gate:** timed and proximity airbursts check
  `ExplosionAirburst`, rather than requiring the normal `Explosion` to be positive.
- `PacketAirburstDistReset` retains its two-integer format and now validates
  the sender's controlled vehicle, current weapon capability and accepted range.
  Zero still disables programming; active distances remain 6..2999 metres.

Damage remains server-owned. Existing vehicle armour/explosion multipliers,
weapon target-type multipliers, friendly-fire and WGCore protection paths remain
in effect. The existing programmed-distance **+3 metre** detonation offset and
AHEAD behavior are retained. No registry, packet layout, config or save migration.
The shared native explosion geometry correction applies to other MCHR blasts
against MCHR vehicles too; HBM-owned explosion handling is unchanged.

## Validation

`gradlew.bat compileJava test --offline -PwgmapDevelopmentClasses=C:/Users/nateb/OneDrive/Documents/GitHub/WGMap/build/classes/java/main`

Passed: 209 tests; two existing optional tests skipped. The new isolated-registry
test runs nine scenarios covering nearest/occluded range hits, offset and rotated
hitboxes, damage metadata preservation, missed-query wing blasts without duplicate
damage, cover/multipliers/radius/inactive ERA, zero-direction blasts, airburst-only
explosives, server authority and packet validation including AHEAD/reset.
`git diff --check` passed. Compilation/tests did not increment the build number.
Sequential review verdict: **ACCEPTABLE**. No client-only dependencies were added
to the common ray, collision, packet or explosion paths.

## In-game acceptance (not run here)

1. Equip a `CanAirburst` weapon with positive `ExplosionAirburst`, such as the
   local pack's Puma DM21. Aim at an enemy plane's body, wing and tail and press
   **Reset Airburst Dist**. Expect the HUD to show the camera-to-hitbox distance.
2. Put terrain in front of the target, then aim into empty sky. Expect the terrain
   distance and then an unprogrammed fuse respectively. The firing vehicle must
   not be selected. Check a rotated vehicle as well as an axis-aligned one.
3. Fire so the burst is next to a wing while the plane origin lies outside the
   blast radius. Confirm server vehicle HP drops once, subject to configured
   armour/protections. Repeat with hull, helicopter, tank and ground vehicle.
4. Check terrain cover, outside-radius misses, friendly-fire protection, AHEAD,
   and ordinary direct hits. Verify matching results for multiplayer observers.

Manual ranging requires a loaded client collision entity; snapshot-only distant
visuals do not provide live hitboxes. Native vehicle blast fixes use the server's
loaded aircraft. Zero damage multipliers/protections intentionally still prevent
damage. **T1:** live multiplayer/content-pack acceptance remains unverified.
