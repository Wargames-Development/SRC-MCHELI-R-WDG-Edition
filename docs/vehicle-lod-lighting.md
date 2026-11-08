# Vehicle LOD lighting

The far renderer previously raised known sky-light samples to a world ambient
floor. That could brighten vehicles in shade. Its no-data fallback also reduced
raw sky coordinates using skylightSubtracted, even though Minecraft's lightmap
already accounts for time and weather. A client entity whose chunk unloaded
could overwrite a valid server light sample with zero during handoff.

MCH_RenderFarVehicle now selects valid native light, server snapshot light, then
the previous valid sample. Only a genuinely unknown sample defaults to raw
exposed-sky light (15) or zero in skyless dimensions. Zero is a valid dark sample
and is never promoted to daylight. Block light remains intact. The existing
client lightmap applies sun, weather, gamma, and vision effects once.

Loaded chunks continue to use their native aircraft brightness, including the
existing entity-specific lighting. Snapshot-only vehicles use the server sample.
No additional chunk load, broad entity scan, terrain trace, packet or persistent
state is introduced.

The unified near/far model pass uses vanilla directional lights and the matching
FRONT_AND_BACK / AMBIENT_AND_DIFFUSE material settings. Compressed model scaling
uses normalized normals without also applying rescale-normal state. Color,
lightmap coordinates, lighting state, and the previously active GL program are
restored. A late sky/world shader is not allowed to shade these model draws with
unrelated samplers, uniforms, or fragment outputs.

MCH_FarVehicleDepthLayer explicitly renders its single-color off-screen target
without an inherited shader-pack program. When compositing the already-lit image,
it writes only the first visible color output and restores the previous draw
buffer mapping. Shader-pack normal/material attachments are left untouched.
The host pipeline retains its surrounding compositing/post-processing; this is
consistent vanilla-style model lighting, not shader-pack-specific entity PBR,
shadow-map integration, or arbitrary pack-specific material output support.

Changed files:

- MCH_RenderFarVehicle.java: light selection and consistent model lighting/state.
- MCH_FarVehicleDepthLayer.java: shader isolation and color-only compositing.
- MCH_RenderFarVehicleLightingTest.java: shade, block-light, missing-data and
  skyless-dimension regression cases.
- MCH_FarVehicleDepthLayerLightingTest.java: optional real off-screen GL checks
  for inherited shaders, auxiliary color targets, restored state and aborted draws.
- This note: behavior, compatibility boundaries and manual verification.

Validation uses Java 8 and `gradlew.bat compileJava test --offline`. Set
MCHR_LWJGL_NATIVES to the cached LWJGL 2 native directory to run the real GL tests
rather than skipping them. The focused off-screen tests were run successfully
with actual native OpenGL, including unchanged auxiliary-buffer pixel values.
They do not load Angelica or the actual shader pack into a running game.

In-game checks still required after rebuilding/installing the MCHR client:

1. Compare the same plane, helicopter, tank and vehicle just inside and outside
   normal entity/chunk render distance. Check top, side and underside shading.
2. Repeat at noon, sunset and night, then rain; include a vehicle in shade and
   one beside block lighting. Cross the boundary in both directions.
3. Repeat with shaders disabled and with the supplied BountifulStars pack on
   Angelica. Check that water/terrain shading is unchanged after a vehicle draws.
4. Check gamma changes, skyless dimensions, thermal/night vision, loaded-to-
   snapshot handoff and a vehicle for which no server lighting is available.

The instance currently configures shaders disabled; this task does not change
that setting, install a rebuilt mod, or claim an in-game shader-pack comparison.
