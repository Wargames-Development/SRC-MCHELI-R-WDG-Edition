# Team marker positioning and visibility

Evidence inspected: `C:\Users\nateb\Downloads\Event Logging\ScreenshotEvidence\mchr triangle wrong spot.png`
and `no team triangle.png`. The screenshots show displaced/missing cyan triangles;
they do not identify every target or record the render state at capture time.

## Diagnosis and fix

- **B1:** Entity/model callbacks projected markers using whichever OpenGL model,
  camera and viewport transforms were active. Nested vehicle riders and FMUR's
  scaled distant-player renderer can change those transforms. The first projection
  claimed the entity ID, preventing the later world-last fallback from correcting it.
- **B2:** Aircraft and gunner registration also depended on their model renderers
  running. Renderer culling, replacement or early returns could remove their markers.
- Entity markers now register only during a dedicated collection pass with the
  active world camera. It scans already loaded living entities and aircraft, then
  uses the existing fresh, same-team snapshots for unloaded players/aircraft.
  Loaded entities use interpolated positions, current team/mount/death state and
  configured aircraft marker heights. Player anchors include vanilla crouch/yOffset.
- The FMUR camera geometry callback runs the same pass because isolated scopes do
  not dispatch Forge's world-last event. Existing per-camera marker collection
  swapping is preserved. Late model/heat callbacks cannot claim displaced markers.

## Validation and review

`gradlew.bat compileJava test --tests mcheli.multiplay.MCH_GuiTargetMarkerTest --tests "mcheli.render.MCH_*ProjectionTest" --tests mcheli.network.packets.PacketEntityInfoSyncTest --tests mcheli.aircraft.MCH_ThermalProfileTest --offline -PwgmapDevelopmentClasses=C:/Users/nateb/OneDrive/Documents/GitHub/WGMap/build/classes/java/main`

Passed: 26 tests, including six marker regressions for registration timing,
collection identity, camera isolation, movement/head projection, scope FOV/viewport
and far/behind/invalid positions. The WGMap override supplies an unrelated API
missing from the existing development jar. No packaging/build-number increment.
Sequential review verdict: **ACCEPTABLE**; the isolated-camera regression found
during review was corrected through the existing FMUR callback. No packet, save,
registry, definition or server gameplay changes. No new FMUR dependency/API required.

## In-game acceptance (not run here)

1. On the same scoreboard team, watch a teammate walk, crouch, jump and teleport.
   Move/turn the camera in first and third person. Expect one triangle above the
   head, following the rendered position without sideways drift or stale ghosts.
2. Cross native entity/chunk render-distance boundaries and use short view distances.
   Expect loaded eligible contacts to keep markers even when their models are culled;
   unloaded teammates use fresh snapshots within the existing 4,096-block sync range.
3. Repeat through FMUR color scopes, thermal scopes, binoculars and vehicle cameras,
   including scope entry/exit and resizing. Expect independent marker positions in
   each view; the magnified image must not borrow the primary-camera triangle.
4. Mount/dismount friendly vehicles, including gunners. Existing rules replace player
   markers with the vehicle marker while mounted and suppress the viewer's own vehicle.
   Check death, respawn, team changes and disconnect: no stale friendly markers.
5. Look behind/off-screen and disable markers with the existing settings/server rule.
   Expect no mirrored, edge-clamped or disabled entity triangles. Point/GPS markers
   retain their existing behavior. Repeat with the event's shader/mod configuration.

**T1 remains manual:** multiplayer/rendering acceptance above. Automated math and
registration tests cannot establish an unconditional visual guarantee in a live mod pack.
