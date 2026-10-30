# Shared CIWS tracer rendering

C-RAM retains its native MCHR 20 mm projectile and HPJ1130 retains its native
MCHR 30 mm projectile. Each round receives a red tracer representation; there
are no extra cosmetic projectile/particle entities. Existing per-round damage,
fuses, collision, gunner aiming and missile interception are
retained. The later [high-rate update](ciws-high-rate.md) changes CIWS cadence,
ammunition consumption and heat limits.

Only the two installed weapon definitions opt in:

| Definition | Caliber | Settings |
|---|---|---|
| `ac_cram_m61a1.txt` | 20 mm | `FMURTracerGlow = true`, `FMURTracerScale = 1.0` |
| `ac_hpj1130_30mm.txt` | 30 mm | `FMURTracerGlow = true`, `FMURTracerScale = 1.25` |

The pack is `mods/MCHELI-R-WDG-Edition-1.1.8_X194/assets/mcheli/weapons` under the
Trailer Mod1 instance. `tools/Set-CiwsTracers.ps1 -MinecraftRoot <minecraft>`
reviews these two entries; `-Apply` updates them in place without backups.
Other weapon definitions do not enable this feature. Mounted 7.62/.50 weapons
now use tracer ammunition every round.

## Implementation and files changed

| Repository / file | Purpose |
|---|---|
| FMUR: `src/main/java/com/flansmod/client/TracerRenderApi.java` | Shared, versioned, client-only original FMUR line/point glow |
| FMUR: `src/main/java/com/flansmod/client/model/RenderBullet.java` | Delegate existing FMUR tracers to the shared renderer; keep its existing 20-block visibility gate |
| MCHR: `src/main/java/mcheli/render/MCH_FMURTracer.java` | Cache the optional client API and supply effective motion, including MCHR's acceleration multiplier |
| MCHR: `src/main/java/mcheli/weapon/MCH_RenderBullet.java` | Draw opted-in native bullets using the shared API, with native-model fallback |
| MCHR: `src/main/java/mcheli/weapon/MCH_WeaponInfo.java` | Default-off visual settings and validation |
| MCHR: `src/test/java/mcheli/weapon/MCH_FmurMountedGunTest.java` | Validate opt-in, scale limits and incompatible projectile settings |
| MCHR: `src/test/java/mcheli/mob/MCH_GunnerEngagementTest.java` | Load real installed definitions and assert exactly two CIWS opt-ins with original fuses |
| MCHR: `tools/Set-CiwsTracers.ps1` | Bounded, repeatable two-file content update |
| MCHR: `tools/CiwsTracerGpuChecks.java`, `tools/ciws_tracer_checks.gradle` | Native OpenGL/GLSL visual and state regression harness |
| MCHR: `docs/ciws-tracers.md`, `docs/fmur-mounted-guns.md` | Current behavior, inventory and validation record |

The earlier quad implementation changed the familiar FMUR gun appearance and was
replaced. The API now uses FMUR's original three line layers (11/6/2.25 pixels),
two point layers (9/3.5 pixels), colors, alpha and length formula. Existing regular
FMUR guns and opted-in CIWS call the same implementation. HPJ1130 scales it by
1.25 for 30 mm; C-RAM uses scale 1.0 for 20 mm.

The renderer supplies the same textured vertices, full-bright lightmap and normals
as the original FMUR renderer under the active entity shader. It does not switch
shader programs, framebuffer or projection. Depth testing prevents glow through
terrain, with depth writes temporarily disabled. OpenGL attributes, active texture
and Minecraft's cached lightmap coordinates are restored after rendering.

Both bridge and API are client-only. Common definitions hold primitives only.
The optional bridge follows the established FMUR thermal API pattern and falls
back to the native bullet model when an older/missing FMUR API cannot be used.
No entity registration, network packet or saved-vehicle format changed.

## Validation

Both mods passed `gradlew.bat compileJava --offline`. FMUR was compiled with the
installed Angelica API through `-PangelicaCompileJar=<mods>/angelica-2.2.14.jar`.

MCHR's full suite passed **193 tests**, zero failures/errors/skips, using:

```text
gradlew.bat compileJava test --offline -I tools/fmur_mounted_gun_checks.gradle -PfmurClasses=<FMUR>/build/classes/java/main -PminecraftRoot=<instance>/minecraft
```

The native GPU harness passed **236 assertions** with actual LWJGL 2 OpenGL
rendering. It checks the fixed-function pipeline and a GLSL entity program
requiring textures, colors, full-bright attributes and normals. Both sizes are
checked at 30, 150 and 500 blocks, sideways and head-on, plus cover occlusion,
GL/shader/lightmap restoration, the actual MCHR bridge with native bullet motion,
and invalid-motion fallback. Each scenario also compares RGB output against a
frozen copy of the original FMUR gun renderer, within one intensity level.
That independent reference is `tools/tracer-reference/OriginalGunTracerReference.java`.

Run from the FMUR checkout:

```text
gradlew.bat ciwsTracerGpuChecks --offline -I <MCHR>/tools/ciws_tracer_checks.gradle -PmchrRoot=<MCHR> -PlwjglNatives=<LWJGL2 natives> -PangelicaCompileJar=<Angelica jar>
```

PNG captures are saved under MCHR's `build/ciws-tracer-checks`. These are native
offscreen captures, not screenshots from Minecraft or the full installed
Angelica/BountifulStars pipeline. The installed shader's entity-input contract
was inspected; a complete in-game shader-pack visual check remains manual.

## In-game verification and deployment

MCHR X197 and FMUR X190 are installed in Trailer Mod1. The expanded MCHR folder
keeps its existing `MCHELI-R-WDG-Edition-1.1.8_X194` directory name to preserve the
asset layout; its actual code and metadata are X197. See
[trailer-mod1-tracer-fix.md](trailer-mod1-tracer-fix.md) for the deployment and
player-cadence correction. Restart the instance before testing.

1. With shaders off, fire C-RAM and HPJ1130 at stationary and moving targets.
   Expect thin red streaks and visible heads; HPJ1130 should look slightly larger.
2. Repeat with the installed shader pack enabled, by day and night, while moving
   the camera and viewing from the front/side. Inspect glow, near-camera clipping
   and distant visibility. Bloom intensity remains shader-pack dependent.
3. Put terrain between the observer and rounds; the tracer must not show through.
4. Use AA gunners against aircraft and incoming missiles. Verify original damage,
   fuse timing, muzzle positions, ammunition and interception behavior.
5. Fire other cannons and inspect their native visuals. Small-arms FMUR bullets
   retain their earlier tracer cadence; unrelated MCHR cannons remain unmodified.
