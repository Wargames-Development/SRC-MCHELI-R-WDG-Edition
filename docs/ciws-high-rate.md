# All-round tracers and high-rate CIWS

Target: `C:/Users/nateb/AppData/Roaming/PrismLauncher/instances/Trailer Mod1/minecraft`.
MCHR X197 is installed in this instance, using the existing FMUR X190 renderer
and ammunition definitions. No FMUR source or content changes are required.

## Behavior

All sixteen converted mounted 7.62/.50 weapon definitions now specify
`FMURTracerInterval = 1`: every successful shot spawns the existing red FMUR
tracer ammunition. The full definition and vehicle inventory is in
[fmur-mounted-guns.md](fmur-mounted-guns.md).

Only the two requested CIWS definitions opt into multiple real rounds per tick:

| Definition | Rounds/tick | RPM at 20 TPS | Heat/round | Maximum heat |
|---|---:|---:|---:|---:|
| `ac_cram_m61a1.txt` | 4 | 4,800 | 2 | 4,000 |
| `ac_hpj1130_30mm.txt` | 8 | 9,600 | 2 | 8,000 |

Both use `Delay = 1`. Each native projectile uses the existing red FMUR tracer
renderer, scale 1.0 for 20 mm and 1.25 for 30 mm. Each round retains its damage,
spread, fuse and missile-interception behavior. Server ammunition and heat count
actual successful projectile spawns. A partial belt fires only the rounds left;
rejected spawns consume no ammunition. Heat limits allow sustained firing at the
new rate without immediately overheating. Normal belt sizes and reloads remain.

The first collision-checked movement is distributed over fractions of a tick
using the synchronized entity ID. Later movement retains normal speed. This
spaces real bullets into a stream without moving them ahead through cover or
adding cosmetic projectile entities. Client prediction uses the same one-tick
trigger interval; server cooldown and existing intent validation remain active.

## Files changed for this update

| File | Reason |
|---|---|
| `MCH_WeaponInfo.java` | Parse `RoundsPerShot`, default 1; bound it to 1..8 and require native MachineGun2 with Delay=1 for multiple rounds |
| `MCH_WeaponCreator.java` | Retain the one-tick player interval for opted-in high-rate guns |
| `MCH_WeaponParam.java` | Local shot result carries actual round count; no packet field added |
| `MCH_WeaponSet.java` | Cap requested rounds by ammunition, account for successful rounds and avoid duplicate spread |
| `MCH_WeaponMachineGun2.java` | Spawn independently spread native rounds, retaining normal per-round parameters |
| `MCH_EntityBullet.java` | Phase the initial collision-checked movement to space the stream |
| `MCH_FmurMountedGunTest.java` | Validate bounded high-rate configuration and incompatible settings |
| `MCH_GunnerEngagementTest.java` | Check player interval, real rates, heat/ammo, movement phases, partial belts, rejected spawns and live definitions |
| `tools/Set-FmurMountedBullets.ps1` | Apply tracer interval 1 to the sixteen audited definitions |
| `tools/Set-CiwsTracers.ps1` | Apply the two CIWS rate, tracer and heat configurations |
| Documentation | Record current behavior and supersede the previous cadence settings |

Production Java paths above are under `src/main/java/mcheli/weapon`; tests are
under `src/test/java/mcheli/weapon` and `src/test/java/mcheli/mob` respectively.
External content changes are exactly the sixteen inventoried small-arms weapon
files and these two CIWS weapon files under the existing expanded MCHR pack.

## Verification and limits

- `gradlew.bat compileJava test --offline -I tools/fmur_mounted_gun_checks.gradle`
  with local FMUR classes and Trailer Mod1 paths: **195 tests passed**, no skips.
- Existing GPU harness: **236 assertions passed**, covering original FMUR
  appearance, fixed-function/GLSL rendering, both calibers, occlusion and state restoration.
- `gradlew.bat jar --offline`: successful production reobfuscation and packaging;
  build metadata advanced to X197. Initial sandboxed packaging could not access
  Gradle's cache lock; the authorized retry succeeded.
- Read-only review: server owns spawning/ammo/heat; optional rendering remains
  client-only. No packet, registry, NBT or saved-world formats changed. Both
  sides need matching code and weapon definitions. Other weapons default to one round.
- Deployment: all 853 installed class/metadata entries match the verified
  production jar. The expanded folder keeps its X194 name and assets; actual
  code and metadata are X197. Exactly one FMUR jar remains active, X190.

Restart Trailer Mod1. Fire a mounted 7.62 and .50 gun and verify every round has
the familiar red FMUR tracer. Hold fire in C-RAM, then HPJ1130; expect a dense
stream, ammunition consumption near 80/160 rounds per second at 20 TPS, and
normal reloads. Repeat against vehicles and incoming missiles with player and
AA gunners. Check shaders off/on, day/night and observers in multiplayer.

No Minecraft gameplay session was launched for this update. Full installed
shader-pack appearance and multiplayer load remain manual checks. These are
real additional projectiles: damage output and ammunition use increase with
rate, and many simultaneously firing CIWS can increase server/client load.
