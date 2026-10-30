# Mounted FMUR small-arms ammunition

MCHR can opt a `MachineGun1` or `MachineGun2` definition into a real FMUR bullet:

```text
FMURBullet = 50calammo
FMURBulletDamage = 1
FMURTracerBullet = 50calammoTracer
FMURTracerInterval = 1
```

Files changed for this integration:

| File | Purpose |
|---|---|
| `src/main/java/mcheli/MCH_FMURUtil.java` | Spawn real FMUR bullets and protect their source mount/crew |
| `src/main/java/mcheli/MCH_MOD.java` | Register the guarded FMUR collision handler |
| `src/main/java/mcheli/weapon/MCH_WeaponInfo.java` | Parse and validate the opt-in ammo and base-damage settings |
| `src/main/java/mcheli/weapon/MCH_WeaponBase.java` | Hold the server-only successful-shot tracer counter per barrel |
| `src/main/java/mcheli/weapon/MCH_WeaponMachineGun1.java` | Route configured server shots to FMUR |
| `src/main/java/mcheli/weapon/MCH_WeaponMachineGun2.java` | Route configured server shots to FMUR |
| `src/test/java/mcheli/weapon/MCH_FmurMountedGunTest.java` | Test configuration defaults and malformed values |
| `src/test/java/mcheli/mob/MCH_GunnerEngagementTest.java` | Exercise real FMUR spawning, server/client source collisions and installed content |
| `tools/Set-FmurMountedBullets.ps1` | Audit all vehicles and apply the selected content mappings |
| `tools/fmur_mounted_gun_checks.gradle` | Supply optional local integration-test paths |
| `docs/fmur-mounted-guns.md` | Record behavior, content inventory and manual verification |

The sixteen external weapon files changed are enumerated below; only
`FMURBullet`, `FMURBulletDamage`, `FMURTracerBullet` and `FMURTracerInterval` settings were added.

`FMURBullet` is the case-sensitive FMUR ammo **ShortName**, not its file name or
item registry name. Omit it to retain native MCHR projectiles.
`FMURBulletDamage` defaults to 1 and supplies base gun damage before FMUR ammo
multipliers. `Acceleration` supplies nominal launch speed; FMUR then controls
flight, drag, gravity, penetration, damage falloff, hit effects and rendering.
MCHR retains its firing interval, spread, mount transforms, sounds, muzzle flash,
heat, reloads, supply items and ammunition counts. No FMUR magazine item is consumed.

This FMUR fork provides ordinary and tracer ammo definitions, without a built-in
mixed-magazine interval. MCHR selects `FMURTracerBullet` on every Nth successfully
spawned round, where N is `FMURTracerInterval`. The converted guns now use N=1:
every round is a red tracer. Other intervals remain supported per physical barrel.
Failed shots do not advance the counter. Switching operators, pausing fire and
reloading retain the cadence; reconstructing the weapon instance resets it.
Omit both tracer settings to disable mixing. The shared FMUR definitions are never
mutated; FMUR's existing spawn packet already identifies the chosen ammo variant.

The server creates exactly one FMUR entity at the computed barrel position.
The owner remains the player or gunner. A server-only source UUID marks these
bullets so FMUR's cancellable hit event can ignore the source vehicle and mounted
crew, plus auxiliary MCHR seats/hitboxes as native MCHR bullets do. FMUR's existing
entity registration and spawn packet are reused. Clients infer cosmetic source immunity from the shooter's
current MCHR mount because FMUR's spawn packet does not carry Forge entity data;
the server continues to use the recorded UUID exclusively. Existing vehicle NBT and packet
formats are unchanged. Missing FMUR/ammo prevents firing and logs a diagnostic
once per ammo name; it does not silently substitute an MCHR round.

## Trailer Mod1 content inventory

The reviewed pack is `mods/MCHELI-R-WDG-Edition-1.1.8_X194/assets/mcheli`.
All 64 definitions in `tanks`, `vehicles`, `helicopters` and `planes` were inspected.
Sixteen weapon definitions cover 28 mounts on 17 vehicles: 21 tank mounts and
7 helicopter mounts. No matching mounts occur in `planes` or `vehicles`.

| Ammo | Weapon definitions |
|---|---|
| `762x51100rdbox` | `mg_762`, `mg_mi8_762_b`, `mg_mi8_762_l`, `mg_mi8_762_r`, `ac_ah6j_2xminigun`, `ac_vt4a1_minigun` |
| `50calammo` | `gunner_mg_127`, `mg_lav25_m2hb`, `mg_m1161_m2hb`, `mg_m1a1_m2hb`, `mg_m1a2sepv3_m2hb`, `mg_m2a3_m2hb`, `mg_t90m_kord`, `mg_t90m_m2hb`, `mg_vdv_kord`, `ac_ec635_2x30mm` |

Affected vehicle definitions: `2a4`, `ah6j`, `bmp2m`, `ec635`, `lav25`,
`m1161itv`, `m1a1`, `m1a2`, `m2a3`, `mi8amtsh-2`, `mrap`, `spm3`, `t90m`,
`uav_m1a2`, `vdv`, `vt4a1`, `zbd04a`.
Shared and currently unmounted matching weapon definitions are mapped too.
The EC635 weapon's identifier says `2x30mm`, but its configured display name
is `.50 Cal MG` and it is non-explosive; it is treated as the configured .50-cal gun.
Both 12.7 mm Kord definitions use the requested .50-cal ammo. All 7.62 mm mounts,
including the Mi-8 guns and miniguns, use the requested 7.62x51 ammo.

The .50-cal base damage is 1, matching FMUR's installed M2HB gun, before the
ammo's 14x living/20x vehicle multipliers. The 7.62 base damage remains the
weapon's MCHR Power (5, or 7 for the AH-6 miniguns), before FMUR multipliers.
The base ammo definitions have `Tracer False`. Every fifth round instead uses
`50calammoTracer` or `762x51100rdboxTracer`, respectively. Both existing variants
have `Tracer True` and `TracerColor Red`, with matching ballistic/damage settings.
CIWS, other cannons and missiles retained their native projectiles; FMUR content
files were not changed. A later visual-only CIWS opt-in is documented in
[ciws-tracers.md](ciws-tracers.md).

`tools/Set-FmurMountedBullets.ps1 -MinecraftRoot <instance>/minecraft` performs
a read-only inventory; `-Apply` changes only the selected weapon definitions.
`-ReportPath <path>` writes a complete JSON inventory including existing unresolved
weapon references outside this migration. No backups are created.

## Validation and manual checks

`gradlew.bat compileJava test --offline` runs ordinary tests. To also exercise
real FMUR bullets and installed content, use:

```text
gradlew.bat compileJava test --offline -I tools/fmur_mounted_gun_checks.gradle -PfmurClasses=<FMUR repo>/build/classes/java/main -PminecraftRoot=<instance>/minecraft
```

The integration fixture uses compiled FMUR classes and replaces mod startup only;
it supplies Forge's normal cancellable-event transformation for headless execution.
Those two environment-specific tests skip when their paths are not supplied.
All 193 tests passed with both paths supplied, including actual MCHR content loading,
MG1/MG2 bullet type/count, muzzle coordinates, owner, damage, direction, source
vehicle/crew protection, missing-ammo rejection, tracer cadence on both gun types,
and retrying the fifth round after a missing tracer or rejected spawn.

After rebuilding and installing matching MCHR code on client and server, restart
the game/server so definitions reload. Check:

1. M1A1/M1A2 coaxial and commander guns: infantry hits, close-range fire, traverse,
   ammo decrement, reload, heat, sound, and no impact against the firing hull.
2. Mi-8 left/right/rear doors, AH-6 miniguns, and EC635 pods: each barrel fires
   from its own mount, including while moving and banking.
3. Kord, MRAP, VDV and VT4 mounts: both human operators and applicable AI operators
   fire correctly; a second player sees the same FMUR projectiles and damage.
4. Passengers and source seats do not consume outgoing shots. Enemy infantry and
   vehicles remain hittable, subject to FMUR damage and MCHR armor/team rules.
5. C-RAM/HPJ1130 still use native rounds and intercept missiles as before.
6. Fire at least ten rounds from each caliber: shots 5 and 10 from each barrel
   should be red tracers. FMUR hides tracer glow for the first 20 blocks, so observe
   farther downrange. Pausing or changing operators should not restart the sequence.

In-game visual, multiplayer and damage-balancing checks remain manual. The
subsequent Trailer Mod1 correction installed MCHR X195 and FMUR X190, verified
the live code against the built archives, and applied all mappings there. See
[trailer-mod1-tracer-fix.md](trailer-mod1-tracer-fix.md).
