# Aircraft and tank control audit

## Scope and findings

Reviewed the render input path, shared angle integration, helicopter, plane and tank
keyboard controls, mobility/rotation definition loading, camera/rider smoothing,
and rotation synchronization. Translational flight physics were inspected and
left unchanged; this patch corrects the attitude controls feeding those physics.

- **B1, fixed:** Shared control timing replaced steps below 0.03 ticks with 0.4
  ticks. Helicopters still used that path. Filtering elapsed time could also
  exceed the current frame's mobility budget after an FPS change. Both aircraft
  now use actual monotonic elapsed time, capped at one tick, without filtering or
  inflating it. Starting, switching aircraft, and resuming from pause reset timing.
- **B2, fixed:** Helicopter keyboard roll directly added 1.2 degrees per step,
  ignoring MobilityRoll. Mouse and keyboard now share the existing roll clamp.
  Hover keyboard strafing remains available, with the same mobility restriction.
- **B3, fixed:** Plane keyboard roll had a threefold reference-frame multiplier
  and bypassed the mouse roll budget. Its original 0.5-degree coefficient is now
  applied per tick, and keyboard demand joins mouse demand before the roll clamp.
- **B4, fixed:** Folded helicopter ground yaw ignored MobilityYawOnGround and
  CanRotOnGround; plane airborne fallback keyboard yaw ignored MobilityYaw.
  These paths now use those parameters; VTOL fallback yaw uses its factor too.
- **B5, fixed:** Mouse centering and plane gunner stabilization included effects
  applied once per render frame. Decay now uses elapsed time; client gunner yaw
  increments use elapsed time too. Other angle stabilization uses exponential
  retention so subdivision into more frames does not strengthen it.
- **B6, fixed in follow-up:** After helicopter controls moved to elapsed per-frame
  timing, chase-camera rotation synchronization still applied only to planes.
  The model used the current mouse-controlled attitude while the camera
  interpolated older 20 TPS angles, creating relative sideways rotation that
  reset at tick boundaries. Both aircraft now use the existing per-frame camera
  synchronization and final-yaw snapshot. Camera position smoothing is unchanged.
  This correction did not resolve the reported cockpit jitter; B7 addresses the
  separate input conflict found after the user's cockpit/third-person comparison.
- **B7, corrected; in-game verification pending:** The render-start handler reads
  mouse movement and applies vehicle controls, but vanilla EntityRenderer then
  polls MouseHelper again and applies that second sample directly to the player.
  The cockpit renders that extra turn; render-end rider restoration removes it.
  Commit e511e10 introduced that restoration in the same change as throttle/spawn
  and mount-lifecycle fixes. The second poll also steals input from the following
  vehicle frame. MCHeli now temporarily supplies a no-poll MouseHelper after its
  own input sample. Late movement stays queued for the next vehicle frame.
  Cursor grab/release delegates to the original helper; render end, next render
  start, next client tick, and client cleanup restore it. Vanilla cinematic
  filtering is suspended only during that frame because it can add camera motion
  even with zero input; its setting is restored afterward. Generic vehicles and
  walking retain vanilla mouse input. Dummy camera pitch is now synchronized
  before rendering, as yaw already was, rather than waiting until render end.
- **B8, corrected; in-game verification pending:** Tanks retained their own
  small-step inflation and elapsed-time filter. At 1000 FPS, the 0.02-tick step
  became 0.4 ticks (20 times as much steering time). Tanks now receive the same
  monotonic frame timing and normalization as aircraft, including turret limits.
  Tank gunner stabilization and autopilot yaw also use elapsed time. Ground
  mobility, pivot-throttle, reverse-steering and terrain checks remain in place.
- **C1, pre-existing and deferred:** MCH_AircraftPacketHandler.onPacketIndRotation
  accepts client-supplied yaw/pitch/roll without checking pilot ownership, finite
  values, or turn limits. Honest-client controls are corrected here, but a modified
  client can still bypass them. Server enforcement needs a separate bounded change
  accounting for local-axis rotation, Euler wrap/inversion, packet timing, UAVs,
  and server-side physics. This patch does not establish server-owned attitude.
- **T1, open:** In-game flight feel, multiplayer observation, and dedicated-server
  launch have not been exercised in this environment. Use the checklist below.

## Files and compatibility

- MCH_ClientCommonTickHandler: elapsed timing for both aircraft and time-based
  mouse centering. Existing final plane yaw and chase-camera synchronization,
  rider render snapshots, and generic-vehicle mouse-look fixes remain in place.
  Follow-up adds tank timing, scoped mouse ownership, and pre-render dummy pitch.
- MCH_RenderMouseHelper: client-only adapter preventing the second vanilla poll
  while forwarding cursor focus operations to the original helper.
- MCH_EntityTank: remove inflated/averaged frame timing and scale gunner effects
  by elapsed time.
- MCH_EntityAircraft: unfiltered timing and shared mobility-limited angle steps.
  Existing local-axis matrix rotation and pitch/roll angle clamps remain in place.
- MCH_3rdCamera: documentation now reflects that its existing render-rotation
  synchronization is used for both planes and helicopters.
- MCH_EntityHeli: mobility-limited keyboard roll, ground yaw parameters, and
  time-based bank/ground stabilization.
- MCP_EntityPlane: combined roll demand, removal of the 3x boost, mobility-aware
  fallback yaw, and time-based stabilization.
- MCH_AircraftControlMath: small client-independent calculations shared by these
  paths, enabling focused tests without loading a Minecraft world.
- MCH_AircraftControlMathTest: seven regression tests for elapsed time, mobility
  budgets, combined input, axis factors, invalid input, and stabilization.
- MCH_RenderMouseHelperTest: two tests for late mouse samples remaining queued,
  vanilla receiving zero movement, and cursor grab/release forwarding.

No packet fields/IDs, NBT, registries, definition fields, dependencies, or build
configuration changed. Mobility remains a limit on local control input, using
the existing 40 * Mobility * 0.06 degrees/tick conversion and aircraft factors.
It is not a global Euler-axis limit when a banked aircraft couples yaw/pitch/roll.
Configured rotation-angle limits still clamp both before and after stabilization.
Tank controls retain their separate angle implementation. Existing public filter
state is retained for compatibility with MCH_API and other callers.

## Validation and manual regression checklist

Automated: `gradlew.bat compileJava`, `gradlew.bat test`, and `git diff --check`.
Compilation passed and all nine tests passed. Tests cover 20, 60, 144, 240, and
1000 FPS, mobility values from zero to two, changing frame times, zero/invalid
steps, equal-time stabilization, and the second-poll input conflict. The latest
follow-up preserved the pre-existing build-number edit from 149 to 155.

1. Fly a helicopter and plane at 60, 144, and uncapped FPS. Hold A and D separately
   for equal durations, then release. Banking should progress gradually and have
   similar timing at each FPS; opposite keys together should cancel keyboard input.
2. Compare otherwise identical test definitions with MobilityYaw/Pitch/Roll of
   0.25 and 1.0. Apply sustained large mouse/stick input separately per axis from
   level flight. The lower-mobility aircraft should saturate at one quarter of the
   control rate. A zero value should disable intentional rotation on that local axis.
3. Test keyboard roll with MobilityRoll = 0, then 0.25, including helicopter hover.
   Zero should prevent keyboard banking; low values should bank more slowly.
   Combine large mouse and keyboard demand; they should share the roll limit.
4. Use narrow Min/MaxRotationPitch and Min/MaxRotationRoll limits. Hold mouse and
   keys against each limit. The aircraft should remain within the configured angles.
5. Test helicopter hover, plane VTOL, flight-sim mouse mode, free look, and gunner
   mode. Verify mode-specific control behavior, stabilizing behavior, and VTOL factors.
6. On the ground, compare MobilityYawOnGround = 0 and 1, and CanRotOnGround = false.
   Include a helicopter with foldable blades folded and a plane on solid terrain.
   Disallowed steering should remain disabled; permitted steering should be gradual.
7. Pause/resume, dismount/remount, and switch aircraft. Verify no extra control step
   is accumulated across the transition. At below 20 FPS, the one-tick safety cap
   deliberately drops excess control time rather than allowing a large sudden turn.
8. Check cockpit and chase cameras through tick boundaries while moving the mouse.
   Smooth presentation should remain intact. Repeat with a remote observer/UAV pilot,
   and smoke-test a tank and generic vehicle plus dedicated-server startup.
   Specifically alternate left/right mouse input in a helicopter chase camera;
   the model should no longer rotate ahead of the view and snap back each tick.
9. In a helicopter and plane cockpit, move the mouse rapidly left/right and
   up/down, then stop. Compare with chase view. The cockpit should not briefly
   turn beyond the controlled vehicle attitude and snap back. Repeat with
   cinematic camera enabled, free look, and a passenger/gunner seat. After
   dismounting, opening a menu, or changing worlds, ordinary mouse-look and
   cursor capture/release must still work.
10. Hold tank A/D for equal durations at 60, 144, and uncapped/high FPS. Compare
    hull steering and turret turning, with ground mobility 0 and 1, forward and
    reverse movement, pivot-turn throttle requirements, and slopes. High FPS
    should no longer multiply the allowed steering time.

Review verdict for the corrective patch: acceptable, with T1 runtime validation
outstanding. C1 remains an explicit limitation of the existing movement authority.
