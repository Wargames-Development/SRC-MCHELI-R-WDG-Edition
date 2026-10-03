# Wheeled ground vehicle handling

Ground vehicles in `tanks/` with `WeightType=car` now use server-owned wheeled
handling by default. `WeightType=tank` and unspecified weight types keep legacy
handling. This does not change the separate `vehicles/` entity implementation.

The base mode field is `WheeledHandling=true|false`. Use `false` to
retain legacy handling for a car, or `true` to enable wheeled handling for a
definition with another collision weight type. WeightType's collision and block
destruction meanings are unchanged.

The later [researched movement profiles](researched-wheeled-profiles.md) add
optional `WheeledAcceleration` and `WheeledTurnRadius` fields and revise the
high-speed lateral budget to approximately 0.5g. That document contains current
pack values, units, sources and validation for the tuning follow-up.

```text
WeightType=car
Speed=4.0
```

`Speed` remains blocks per tick, with the existing `AllTankSpeed` multiplier.
Wheeled speed accepts 0 through 8, including 2.0 and 4.0; the final multiplied
limit is also bounded to 8. Legacy speed still clamps to 1.8 before applying the
multiplier. Definition field order does not affect which limit is selected.
This intentionally raises the old car limit, rather than changing the unit.
Both client and server must load matching definitions.

## Driving

- Forward/reverse keys request acceleration. Releasing them coasts; holding the
  opposite direction brakes to a stop before accelerating in that direction.
  Both direction keys together brake. Reverse is capped at the smaller of 0.6
  and 30% of the forward limit, and still requires EnableBack.
- Steering builds smoothly and returns to center. Wheelbase comes from the
  outermost existing SetWheelPos axles, with a four-block fallback. No wheelbase,
  tire, gear, or acceleration tuning fields are required.
- Steering uses signed incoming motion, so reverse turns naturally and a stopped
  hull cannot pivot. A fixed lateral acceleration budget reduces the permitted
  wheel angle and widens fast turns; the displayed wheels use that same angle.
- Grounded lateral grip makes velocity follow the steered hull. Forward speed
  has its own acceleration/coasting model, rather than the legacy double drag.
- The brake key decelerates motion directly. Steering and braking work while
  coasting after fuel loss or engine shutdown; propulsion does not.
- No tire acceleration or steering yaw is applied in the air. Floating vehicles
  retain propulsion/steering but use weaker lateral damping.

For the new mode, MotionFactor, PivotTurnThrottle, MobilityYawOnGround,
ThrottleUpDown, ThrottleDownFactor, and AutoThrottleDownTank no longer tune the
driving model. They retain their original meanings in legacy mode. CanMoveOnGround,
CanRotOnGround, EnableBack, fuel, canopy, damage, gravity, and collision checks
remain applicable. Existing wheel drawing parts display the server steering;
wheel spin follows signed movement rather than engine throttle.

## Authority and compatibility

The existing seven-byte tank control payload and packet ID are unchanged.
Left/right intent is now consumed on the server for wheeled vehicles. Valid pilot
input is published atomically from Netty and rechecked on the simulation tick.
Input refreshes every five client ticks and expires after twenty server ticks;
driver changes, death, destruction, and closed control state clear active input.
Opening a GUI sends neutral driving input with brakes applied.

Client rotation packets cannot overwrite a wheeled hull's heading, pitch, or
roll. The server simulates driving at 20 TPS. Tank-only data watcher slots 17
(heading) and 18 (steering) carry authoritative presentation state; existing
position and roll synchronization remains in use. Clients interpolate heading,
including the driver's vehicle, and do not integrate hull steering per render
frame. This deliberately uses server interpolation; steering response under
network latency still needs in-game assessment.

Wheeled translation advances from server velocity and blends the remaining
position error, with normal previous/current frame interpolation. Prediction
bridges at most two missing updates before holding. Late positions cannot reverse
the current motion; authoritative stops, reversals and large teleports still
correct it. Regular per-tick updates avoid the extra positional lag that made
rounds appear ahead of the barrel. Heading retains its smoothing. See
[moving cannon alignment](wheeled-cannon-alignment.md) for the multiplayer follow-up.

Upgrade server and clients together because of the new tank data watcher fields.
No packet IDs/field layouts, NBT keys, registry IDs, dependencies, or build
configuration were changed. Steering/input is transient, not saved world data.

## Review and validation

Files changed for this task:

| File | Purpose |
| --- | --- |
| MCH_EntityTank.java | Server driving, input ownership/expiry, terrain contact, heading synchronization, wheel animation |
| MCH_WheeledControlMath.java | Testable speed limits, acceleration/braking, steering geometry and speed-dependent wheel angle |
| MCH_TankInfo.java | Mode selection, definition-order-independent speed limits and derived wheelbase |
| MCH_ClientTankTickHandler.java | Intent refresh, key releases, steering while braking and neutral GUI input |
| MCH_TankPacketHandler.java | Reject incomplete packets and publish validated pilot driving intent |
| MCH_TankPacketPlayerControl.java | Validate the unchanged seven-byte payload before accepting it |
| MCH_AircraftPacketHandler.java | Reject client-supplied wheeled hull rotation |
| MCH_WheeledControlMathTest.java | Eight speed, steering, braking and invalid-data regression tests |
| MCH_TankInfoWheeledTest.java | Two definition-order/mode and wheelbase regression tests |
| MCH_TankPacketPlayerControlTest.java | Golden wire-layout, packet ID, round-trip and truncation regression test |
| docs/wheeled-handling.md | Configuration, compatibility, review findings and manual scenarios |

Sequential implement → review → fix → test → verify workflow:

- B1 fixed: engine permission originally also disabled coasting brakes/steering.
- B2 fixed: legacy throttle-dependent gravity could lose ground contact at full
  throttle. Wheeled gravity now follows the configured gravity directly.
- C1 fixed: driving intent must not mutate simulation state from Netty. An atomic
  pending intent is consumed and pilot ownership rechecked on the server tick.
- T1 open: actual terrain handling, multiplayer interpolation/latency, and a
  dedicated-server launch require the manual scenarios below.

Java 8 `gradlew.bat compileJava test --offline` passes: 33 tests, including 11 new
ground handling/definition/packet tests. `git diff --check` passes. No packaging
task, Minecraft launch, or build-number increment was performed.

## Manual regression checklist

1. On a long flat road, compare otherwise identical cars with Speed=2.0 and 4.0.
   Full forward input should reach distinct limits; steering should not acquire
   the old throttle-dependent pivot behavior. Verify Speed before/after
   WeightType and the AllTankSpeed multiplier.
2. At rest, steer both directions: wheels turn but the hull does not pivot.
   Repeat while pushing against a wall. Test slow parking turns and fast road
   turns; faster full-lock turns should be wider. Release steering to recenter.
3. Coast, brake, hold reverse from forward travel, then hold forward from reverse.
   Each direction change should stop first; EnableBack=false prevents reverse.
4. Test slopes, stairs, a cliff/drop, water entry/exit, walls, and vehicle impacts.
   Full throttle must retain ground contact. Airborne input must not steer the
   hull or accelerate it horizontally. Existing collision behavior still applies.
5. In multiplayer, compare driver and passenger/observer views, including high
   and low FPS, yaw wraparound, and latency. Verify steering animation and wheel
   spin while reversing/coasting. Check starting/stopping for visible correction.
6. Release keys, open a GUI, dismount/change drivers, lose fuel, damage the engine,
   and disconnect. Throttle must not stick; valid coasting brakes should remain
   available. Passenger controls and forged rotation packets must not drive the
   hull; truncated control packets must not change intent.
7. Launch a dedicated server. Compare a tracked vehicle and a car with
   WheeledHandling=false against their prior handling. Reload definitions and
   save/reload the world; no input/steering state should persist across respawn.

## Follow-up: speed-dependent pitch and stepped turns

- Wheeled terrain probes now run after the server chassis moves and resolves
  collisions, with no additional horizontal extrapolation on either side.
  Previously wheels moved ahead of the center used for the terrain normal,
  producing artificial pitch at speed, especially on short-wheelbase vehicles.
  Tracked vehicles retain their original wheel-update order and offsets.
- Render control preserves the wheeled hull's previous tick heading and the
  server heading instead of overwriting them each frame. The driver view and
  camera offsets use the existing shortest-path heading interpolation with the
  actual render fraction; mouse input still uses elapsed frame time. No packet,
  definition, or speed/steering limit changes are required.
- Files changed for this follow-up: `MCH_EntityTank.java` (probe timing and heading
  snapshots), `MCH_ClientCommonTickHandler.java` (render fraction and camera
  alignment), and this document (regression scenarios).
- Read-only review: server movement remains authoritative, legacy/tracked and
  flight controls retain their previous paths, and temporary render heading is
  restored in `finally`. Live terrain and camera behavior require in-game checks.
- Verification: Java 8 `gradlew.bat compileJava test --offline` succeeded with
  33 tests, zero failures/errors; `git diff --check` passed. No new test framework,
  packaging task, installation, or build-number increment was performed.

Focused manual checks after installing the rebuilt mod on server and clients:

1. Drive M1161ITV at full speed along flat terrain, coast and brake, then reverse.
   Sustained speed should not introduce a nose-down pitch. Repeat with slopes,
   stair steps and a wall: terrain tilt and collision response should still work.
2. Hold a slow tight turn and a fast sweeping turn at 30, 60 and 144 FPS. Compare
   first-person, third-person, free look, passenger and observer views. Heading
   and camera offset should advance smoothly between ticks, including crossing
   the -180/180-degree boundary. No double interpolation or one-frame heading
   snap should occur when changing view or releasing steering.
3. Repeat the previous checks with a tracked vehicle and `WheeledHandling=false`
   to confirm legacy handling is preserved. Assess multiplayer latency separately.

## Follow-up: gentler acceleration and stable turret aim

- Wheeled acceleration is now `0.012 + Speed / 240` blocks/tick per tick,
  replacing `0.04 + Speed / 80`. The tuned M1161ITV (Speed=1.65) reaches full speed
  in approximately 4.4 seconds from rest rather than 1.4 seconds. Top speed,
  braking, coasting and reverse limits are preserved; reverse acceleration is
  also gentler. These timings assume uninterrupted ground contact at 20 TPS.
- Wheeled throttle builds over two seconds and releases over one second.
  Throttle remains server-owned; it drives the existing sound/display state.
  Propulsion continues to use the independently slowed acceleration calculation.
  No new content parameters are needed.
- Local wheeled weapon poses now apply the existing aim/traverse limit logic
  against the hull's interpolated render heading. Previously those per-frame
  poses counter-rotated against its tick heading, visibly stepping on the smooth
  hull. Turret rotating parts and bays share shortest-path world-aim interpolation
  relative to the rendered hull; local pilot aim avoids a second interpolation.
  Temporary client render heading is restored in `finally`.
- Files: `MCH_WheeledControlMath.java` (rates and relative render angle),
  `MCH_EntityTank.java` (server throttle ramp), `MCH_ClientCommonTickHandler.java`
  (local weapon pose reference), `MCH_RenderAircraft.java` (turret parts/bays),
  `MCH_WheeledControlMathTest.java` (three regressions), and this document.
- Sequential review verdict: ACCEPTABLE. The render correction remains client
  presentation; server movement/input authority, fixed-gun behavior, existing
  traverse logic, packets and content definitions retain their existing paths.
  Java 8 `gradlew.bat compileJava test --offline` passed with 36 tests, zero
  failures/errors. No packaging, installation or build-number change occurred.
- T2: Live turret/camera behavior still requires validation. Drive a wheeled
  turret vehicle while holding aim on a distant landmark without mouse movement;
  test left/right steering, yaw wrap, and 30/60/144 FPS in both camera views.
  Repeat while moving the mouse, at traverse limits, with fixed guns, and as
  passenger/observer. Check gun/barrel, turret attachments and firing alignment.
  Compare tracked vehicles and `WheeledHandling=false`. On flat ground verify
  the ITV's longer acceleration run, unchanged eventual top speed, progressive
  throttle sound/display, braking and forward/reverse transitions.
