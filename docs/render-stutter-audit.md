# Vehicle rendering stutter audit

## Scope and evidence

Audited the shared aircraft display-list cache, near-plane rendering, distant
vehicle rendering, model registration/loading, texture binding, and render-tick
ownership against the supplied WGProfiler X25 captures. MCHR is the only edited
repository. Existing uncommitted work, including the far-vehicle renderer change,
was preserved.

The twelve-minute capture records two frames at 240.685 and 438.636 seconds with
160.658 and 157.909 ms inside `MCH_RenderFarVehicle`. The corresponding intervals
contain 16 and 15 new display-list compilations and texture uploads. The earlier
capture also has compilation batches of 29, 17, 22 and 23 lists in affected
vehicle/plane render intervals. These interval-level observations identify
preparation during rendering, but do not separate compilation time from texture
decoding, upload time, or driver waits.

## Findings and implementation

- **B1, corrected:** Cache misses could compile every newly encountered vehicle
  part in one frame. The existing cache now allows at most one compilation
  attempt per outer Minecraft render frame, shared across all its callers.
  Once spent, the existing direct-render fallback draws remaining parts normally.
  They become eligible for caching on following frames; cached hits consume no
  allowance. Clearing the cache does not reset the allowance within a frame.
- **D1, remaining limitation:** A single large model/part compilation or first
  texture decode/upload can still stall. The count limit is not a millisecond
  guarantee. Direct rendering can temporarily increase CPU work while the cache
  fills. Full-pack measurements are required to assess the tradeoff.
- **T1, pending:** The synthetic native OpenGL checks verify visibility and cache
  lifecycle, but do not establish a frame-time improvement with the actual
  content pack, Angelica shader pipeline, thermal replay or WGMap depth rendering.

No global texture preloading, new workers, queues, dependencies, configuration,
network packets, formats or gameplay policies were added. This patch does not
address the unattributed recurring 77.3-second stalls or window-size-associated
low-FPS period from the captures. Texture preparation remains a separate follow-up
requiring per-operation measurements and a bounded resource-loading design.

## Files changed

- `MCH_ModelDisplayListCache.java`: enforce the shared compilation allowance.
- `MCH_RenderAircraft.java`: expose its internal renderer frame-reset hook.
- `MCH_ClientCommonTickHandler.java`: reset once at the existing client render
  start, before world/player early returns. Nested rendering does not reset it.
- `MCH_ModelDisplayListCacheTest.java`: optional native OpenGL regression tests
  for body/part fallback, visibility, convergence, hits and invalidation.
- This document: evidence, review, validation and remaining runtime checks.

## Review

Read-only review verdict: **ACCEPTABLE WITH MINOR ISSUES**, with D1 and T1 above.
No additional correctness finding required a production fix. The added call is
inside the existing `@SideOnly(CLIENT)` tick handler and reaches only renderer
classes; common/server entry points are unchanged. Forge 1.7.10 sources confirm
one render-start event around each outer `updateCameraAndRender` call. All OpenGL
work stays on the existing render thread. Java 8 compatibility, existing GL-list
deletion, animated part selection, thermal transforms, visibility and server
authority remain intact by source review. No migration is required.

## Validation

- Default `compileJava test --tests mcheli.aircraft.MCH_ModelDisplayListCacheTest
  --offline`: failed because the existing default development dependency lacks
  `WGMap.integration.mchr.RadarTerrainAccess`, required by prior radar work.
- Retried with the existing `-PwgmapDevelopmentClasses=<WGMap>/build/classes/java/main`
  override: compile and all three focused native OpenGL tests passed, zero skips.
- Full `compileJava test` with that override, Java 8 and `--offline`: passed;
  214 tests, zero failures/errors, two existing skips. The three added native
  OpenGL checks all ran successfully.
- `git diff --check`: passed. Build number stayed X199. No jar, archive, commit
  or release was created.

The native checks use a hidden LWJGL 2 Pbuffer. Set `MCHR_LWJGL_NATIVES` to the
existing Minecraft 1.7.10 native-library directory before running Gradle. Without
that environment variable, this GPU test class is deliberately skipped.

## Manual verification still required

1. Start with a cold cache and encounter several new helicopters, planes, tanks
   and vehicles together. Confirm all bodies and moving parts appear immediately
   in normal and far rendering. Repeat the route to compare warm-cache behavior.
2. Capture detailed frame times and GL intervals before/after. The MCHR shared
   cache may create only one list per rendered frame, so one-second CSV rows can
   still contain many creations. Other renderers also create their own lists.
3. Exercise animated weapons, rotors, wings, selected skins, thermal/scoped
   replay, WGMap depth occlusion, and the existing model reload operation. Confirm
   no invisible parts, GL errors, stale geometry or cache-reset compilation burst.
4. Repeat with multithreaded model loading enabled and disabled. No new GL work
   should move to a model-loader worker.
5. Start a dedicated server and connect with the updated client to verify normal
   startup, matching version requirements and unchanged vehicle gameplay. No
   Minecraft client or dedicated-server launch was performed in this audit.
