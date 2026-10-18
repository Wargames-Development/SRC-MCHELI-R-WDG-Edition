# CIWS AA gunner replacement

The first replacement operator controls turret-mounted MachineGun1 and MachineGun2
weapons on generic vehicles, including C-RAM and HPJ 1130. It does not pilot tanks,
helicopters, or planes. The old combat and navigation implementation has been removed.

## Installation and use

Java changes require a rebuilt MCHR mod installed on both client and server. Copying
the definitions alone into the existing X180 installation does not install the new AI.
No replacement mod binary is deployed by this change.
The latest [ray-trace origin fix](ciws-raytrace-origin-fix.md) also provides a
compiled code-only ZIP for updating the existing extracted installation while
preserving its assets. Stop both sides before replacing loaded code.
Use [the balanced update with gunner removal](../build/ciws-balance-audit/ciws-balanced-X180-removable-gunner-update.zip),
containing the compiled code and seven tuned assets. CIWS AA remains the sole egg in the gunner tab.
After that combined update, apply the [earlier-engagement profile patch](../build/ciws-earlier-engagement/ciws-earlier-engagement-profiles.zip)
to both sides and restart. It replaces only the two model profiles; no new Java
classes are needed for this range/timing adjustment.

The definitions are `assets/mcheli/gunners/gunner_aa_default.txt` and
`assets/mcheli/blocks/ciws_cram.txt`. They use the existing gunner icon, block textures,
and `vehicles/cram.txt` / `weapons/ac_cram_m61a1.txt` content. Restart after installing
definitions: items and blocks register during startup.

Join a WGEvents team or a WGCore faction, then right-click a compatible empty turret
seat with **Gunner [CIWS AA]**. Alternatively, place **Team CIWS [C-RAM]**. That block
captures the placing player's team and deploys one C-RAM above it when the area is
clear. The saved block and gunner keep their deployment team if the placer later
changes team. A player without a team must join one and replace the block.

**Shift-right-click the occupied turret, gunner, or seat to remove the operator.**
The owner, a member of the deployment team, or a creative player may remove it;
creative-placed gunners still require a creative player. A survival-placed gunner
drops one CIWS AA egg. The turret and its current ammunition remain intact. Enter
the freed turret and reload normally, then use the egg to put the gunner back.
Ordinary clicks on the gunner no longer remove it. Empty turrets retain their
existing shift-right-click inventory interaction, and occupied turret removal
takes precedence over inventory opening or wrench texture switching.

The removal patch changes `MCH_EntityAircraft.java` and `MCH_EntitySeat.java` to
forward sneak interactions to their gunner, and `MCH_EntityGunner.java` to require
sneaking, acknowledge the client click, and enforce existing permissions on the
server. `MCH_GunnerEngagementTest.java` covers owner/team/creative access, enemy
rejection, creative-placed restrictions, seat removal, one egg per removal,
preserved ammunition, and the freed turret's inventory interaction. No packet,
registry, NBT, definition, or balance changes are required.

Validation: Java 8 `gradlew.bat compileJava test jar reobf --offline` passed with
125 tests and no failures. Headless player tests use Java 8's internal `Unsafe`
allocator to avoid Forge runtime registry bootstrap; this produces two test-only
compiler warnings. In-game verification remains necessary: on both C-RAM and HPJ
1130, remove a survival-placed gunner as owner and teammate, collect its egg,
enter and reload the turret, and replace the gunner. Check that an enemy cannot
remove it and that ordinary right-clicks leave it mounted. Install the new code on
both client and server with the game stopped; no binaries were deployed automatically.

Operators can also use `/addgunner aa <x> <y> <z> <radius>` or
`/addgunner profile gunner_aa_default <x> <y> <z> <radius>`. Commands require permission
level 2, affect compatible empty seats only, and limit radius to 128 blocks.

For a fixed team profile, set `TeamMode = fixed` and `FixedTeamId` to a WGEvents
internal ID such as `t1` (or its scoreboard name `wge_t1`), or the actual WGCore
faction UUID. Fixed profiles work from command blocks. Block definitions may instead
set `GunnerFactionId` when the profile uses player mode. External teams are never
created by MCHR. Without either team mod, existing vanilla scoreboard teams work.
WGCore faction lookup requires `EnableWGCoreIntegration = true`.

## Behavior and tuning

The server selects enemy MCHR missiles/rockets and opted-in bomb parent bodies with
`CanBeIntercepted = true`, and
helicopters/planes. The flag belongs to the target missile weapon definition, not
the CIWS ammunition. Missiles have
priority; distance and target-switch hysteresis decide between threats. Ordinary
bullets, bomblets, ground vehicles, dead/destroyed entities, the gunner's own launcher,
and aircraft with any friendly crew are excluded. Creative player aircraft are
excluded. Unknown-team missiles and empty aircraft are ignored. Ownership is resolved
from missile shooters, with launcher crew as a fallback, and current aircraft crew.
Third-party mod projectiles are not recognized in this first operator.

Line of sight and the existing mount's traverse limits gate target acquisition.
Prediction uses target velocity and the weapon's actual bullet speed scaling and
gravity. The operator applies relative look inputs over time; aircraft weapon updates
own the barrel rotation. It fires only when the actual barrel is aligned. Existing
weapon ammo, reload, heat, cooldown, damage, and spread apply, including normal global
infinite-ammo/creative settings. Block-spawned gunners use ordinary finite ammo unless
the server enables infinite ammo. There is no AI-only refill or turret-stat override.

| Profile parameter | Default | Effect |
| --- | --- | --- |
| DetectionRange | 180 | Spherical acquisition/firing distance in blocks, 1–1000 |
| ScanInterval | 4 | Ticks between nearby searches, 1–200 |
| ReactionTicks | 10 | Delay after acquiring/switching a target, 0–200 |
| LookSpeed | 4 | Maximum nominal barrel look step in degrees/tick, 0.1–15 |
| TrackingResponse | 0.35 | Proportion of remaining angular error corrected, 0.01–1 |
| AimError | 0.15 | Maximum persistent aim bias per axis/target in degrees, 0–30 |
| FireTolerance | 0.9 | Allowed barrel error per axis in degrees, 0.1–10 |
| LeadFactor | 1 | Lead scaling, 0–2; 0 removes velocity lead |
| TargetMissiles | true | Enable missile/rocket targeting |
| TargetAircraft | true | Enable helicopter/plane targeting |
| EnableShortBurst | true | Enable timed bursts starting at the first successful shot |
| ShortBurstFireTick | 18 | Burst duration in ticks |
| ShortBurstRestTick | 6 | Pause between bursts in ticks |
| DebugGunner | false | Server `[CIWS]` state summary every 40 ticks when enabled |
| WeaponProfile | repeatable mapping | `weapon_name, profile_name` selects timing/range for that weapon |

These are the conservative fallback settings. The default profile maps C-RAM to
`gunner_aa_cram` (320 blocks) and HPJ 1130 to `gunner_aa_hpj1130` (420 blocks).
Both scan every 2 ticks; their reaction delays are 4 and 3 ticks respectively.
Model profiles supply their complete timing/aim settings; edit their `DebugGunner`
field to enable diagnostics for those weapons. Placement/team ownership stays on
the base profile, and custom profiles without mappings retain their own tuning.
See [CIWS balancing](ciws-balancing.md) for radar, gun and probability analysis.
The [earlier interception test preset](ciws-interception-fix.md) is historical.
See [the engagement audit](ciws-engagement-audit.md) for the constructor/save bug,
acquisition and lead fixes, and GPS/variant flag audit.
The [barrel tracking follow-up](ciws-barrel-tracking-fix.md) corrects look/barrel
feedback and independently rotating gun pitch limits, with detailed angle logs.
The subsequent [ray-trace origin fix](ciws-raytrace-origin-fix.md) identifies and
corrects the native visibility method mutating the shared aiming origin.

Pitch input also uses the weapon's `CameraRotationSpeedPitch`. Lower look speed,
slower response, longer reaction time, or greater aim error makes interception harder.
An aim error greater than fire tolerance can deliberately prevent an accurate shot.
Detection is an operator scan, independent of the radar's probabilistic contacts.

## Compatibility

The gunner entity ID, CIWS egg registry name `mcheli:gunner_aa`, existing packet
formats, and legacy NBT identity keys remain. The retired `StupidGunner` flag is
ignored and omitted on save.
`GunnerProfile`, `PlacedGunnerTeam`, and `GunnerOwnerUUID` are additive
NBT keys. The deployment block has a new registry name `ciws_cram` and definition
BlockID 34750. Old non-AA gunners remain loadable and removable but do not fight or
drive. Their eggs have been removed from registration: `spawn_gunner_vs_monster`,
`spawn_gunner_vs_player`, `gunner_enemy`, `gunner_friendly_stupid`, and
`gunner_enemy_stupid`. Forge explicitly discards those five missing item mappings
when loading an old world, so existing stacks disappear. Additional gunner profiles
no longer register extra eggs; `gunner_aa_default` configures the sole CIWS egg.
The NPC egg `spawn_npc` is also removed; its missing item mapping is explicitly
discarded on old-world load. The NPC entity remains registered for existing worlds
and the separate tech-tree NPC system. Only the CIWS AA egg is registered in the
gunner tab. Old `/addgunner` friendly/player/enemy/pvp
forms are retired. Old spawners requesting those operators no longer spawn them.
Their block definitions and unrelated waypoint systems are retained for saved-world
registry compatibility. Legacy AI-only profile fields have no effect on this AA AI.

Existing AA gunners without `GunnerProfile` use built-in tuning. An old gunner with
no compatible deployment team remains inactive until replaced. A removed profile
falls back to built-in tuning. Team switches by target crews are checked before firing.

## Manual regression checklist

1. Run a dedicated server with WGEvents only, then repeat with WGCore only. Confirm
   startup and gunner placement work without the other mod. Use two actual teams.
2. Deploy the C-RAM with the item and block. Observe gradual yaw/pitch movement on
   both a controlling client and another observer; the barrel and muzzle direction
   should agree. Repeat on HPJ 1130 and at yaw ±180 degrees.
3. Fly an enemy helicopter/plane across range. Fire hostile missiles toward the gun.
   The gun should lead targets, prefer missiles, and intercept through ordinary
   projectile collisions. Receding targets faster than the bullets should not fire.
4. Repeat with friendly aircraft/missiles, mixed friendly/enemy crew, empty aircraft,
   unknown shooters, creative crews, terrain cover, and targets outside pitch limits.
   Friendly/unknown and unreachable targets must not receive target-directed fire.
5. Change an enemy crew's team to friendly while tracked. Firing should stop that
   tick. Switch the placing player's team: the deployed gun should retain its team.
6. Exhaust ammo and overheat the gun. Confirm normal reload/cooldown, ammo consumption,
   and observer visuals. With infinite ammo disabled, supplies must be finite.
7. Save/restart with a deployed gunner and block. Confirm team, owner, and profile
   survive; transient targets are reacquired. Removing/replacing a gunner must never
   eject a player. Non-operators must not run `/addgunner`.
8. Increase reaction time and aim error; lower look speed and lead factor. Verify
   interception worsens. Test bursts and disabled aircraft/missile targeting.
9. After installing matching rebuilt client/server mods, check the creative gunner
   tab and item search: only **Gunner [CIWS AA]** remains as a gunner egg. Confirm
   `/give @p mcheli:gunner_aa` works and the five retired gunner names plus
   `mcheli:spawn_npc` are unavailable.
   Load a test world containing old eggs: its stacks should disappear without a
   missing-mapping blocker for those eggs. Remove an old inactive gunner and confirm
   it does not drop an egg. A noncreative AA gunner still drops the CIWS egg.

Automated checks cover interception math, look-step bounds/yaw wrapping, friendly
and unknown team comparison, parameter rejection, NBT identity/profile persistence,
and absence of client look calls in the common gunner bytecode. Live multiplayer,
actual WGCore/WGEvents service behavior, and interception rates require this checklist.

## Changed files and verification

Paths below are relative to the repository root.

| File | Reason |
| --- | --- |
| src/main/java/mcheli/MCH_MOD.java | Register only the CIWS egg and explicitly discard retired gunner/NPC egg mappings on old-world load |
| src/main/java/mcheli/mob/MCH_ItemSpawnNPC.java (deleted) | Remove the unused NPC egg implementation |
| src/main/java/mcheli/mob/MCH_EntityGunner.java | Replace the monolithic AI with a mounted AA controller and retain saved identity |
| src/main/java/mcheli/mob/MCH_AAGunnerMath.java | Bounded look inputs, enemy identity comparison, and intercept solver |
| src/main/java/mcheli/mob/MCH_GunnerTeams.java | Select WGE scoreboard or WGC faction identities without loading WGE classes |
| src/main/java/mcheli/mob/MCH_GunnerInfo.java | Add validated AA performance parameters |
| src/main/java/mcheli/mob/MCH_GunnerInfoManager.java | Reject malformed profiles and clear stale definitions when the folder is emptied |
| src/main/java/mcheli/mob/MCH_ItemSpawnGunner.java | Validate compatible seats and teams; preserve occupants and consume items server-side; remove unused old-mode item flag |
| src/main/java/mcheli/command/MCH_CommandAddGunner.java | Bound operator-only AA spawning and retire old AI command modes |
| src/main/java/mcheli/block/MCH_ConfigSpawnerBlock.java | Capture player team at block placement through the existing Forge hook |
| src/main/java/mcheli/block/MCH_ConfigSpawnerTileEntity.java | Save deployment ownership and attach the new AA profile without old faction creation/FMUR AI |
| src/main/java/mcheli/vehicle/MCH_RenderVehicle.java | Render AA barrel orientation consistently, including operators on secondary seats |
| src/main/java/mcheli/wgc/Integrations.java | Expose optional faction lookup through the established boundary |
| src/main/java/mcheli/wgc/WGCoreCompat.java | Delegate faction lookup to the actual WGCore API |
| src/main/resources/assets/mcheli/gunners/gunner_aa_default.txt | First configurable CIWS AA operator |
| src/main/resources/assets/mcheli/gunners/gunner_aa_cram.txt | C-RAM point-defense range and response settings |
| src/main/resources/assets/mcheli/gunners/gunner_aa_hpj1130.txt | HPJ 1130 point-defense range and response settings |
| src/main/resources/assets/mcheli/blocks/ciws_cram.txt | First team-owned C-RAM deployment block |
| src/test/java/mcheli/mob/MCH_AAGunnerMathTest.java | Intercept, look limits, yaw seam, and enemy comparison regression tests |
| src/test/java/mcheli/mob/MCH_GunnerInfoTest.java | Parameter loading and invalid-number regression tests |
| src/test/java/mcheli/mob/MCH_GunnerPersistenceTest.java | Save/load identity and legacy mode regression tests |
| src/test/java/mcheli/mob/MCH_GunnerServerSafetyTest.java | Common-bytecode client-call checks and optional boundary linking |
| src/main/java/mcheli/weapon/MCH_EntityBaseBullet.java | Center missile boxes, repair spawn positions, sweep moving collisions, and check target interception flags |
| src/main/java/mcheli/weapon/MCH_EntityBullet.java | Use the same moving-projectile collision solver for CIWS rounds |
| src/main/java/mcheli/weapon/MCH_ProjectileCollision.java | Continuous relative-motion box collision during one server tick |
| src/main/java/mcheli/MCH_EventHook.java | Repair projectile boxes before the existing spawn hook starts world tracking |
| src/test/java/mcheli/weapon/MCH_ProjectileCollisionTest.java | Real projectile eligibility/box checks plus swept collision, damage, and update-order regression tests |
| src/main/java/mcheli/aircraft/MCH_EntityAircraft.java | Preserve constructor gunner/status bits, save without enabling gunner mode, and track movement tick for lead |
| src/test/java/mcheli/mob/MCH_GunnerEngagementTest.java | Real constructor, save/load, approach, target-selection and native turret rotation regressions |
| docs/ciws-aa-gunner.md | Configuration, compatibility changes, validation limits, and manual checks |
| docs/ciws-interception-fix.md | Follow-up diagnosis, temporary pack tuning, migration, and regression checklist |
| docs/ciws-engagement-audit.md | Lifecycle diagnosis, engagement review, GPS/variant audit and current verification |
| docs/ciws-barrel-tracking-fix.md | Barrel feedback, gun/body pitch separation, live log interpretation and test limitations |
| docs/ciws-raytrace-origin-fix.md | Confirmed visibility-vector corruption, accurate native-ray regressions and compiled code update |
| docs/ciws-npc-egg-removal.md | NPC egg removal, existing-world handling and latest compiled code update |
| docs/ciws-balancing.md | Pack-relative balancing, sources, physical interception estimates and deployment |

The two definition files are also copied to the supplied PrismLauncher instance's
`assets/mcheli/gunners` and `assets/mcheli/blocks` directories, with matching SHA-256
hashes. The follow-up also applies a temporary weapon preset to the supplied instance,
with originals backed up as described in `ciws-interception-fix.md`.

Validation uses Java 8 and cached dependencies: `gradlew.bat compileJava test --offline`.
The complete suite passes: 122 tests, zero failures/errors/skips, including an
old-world mapping test and native barrel feedback, limits, moving-target and
stationary-helicopter checks for both supplied CIWS gun mounts.
Engagement checks now use the real vanilla visibility tracer over test blocks;
earlier clear-sight stubs missed its mutation of the ray start vector. Packaging
validation `gradlew.bat jar reobf --offline` also passes; the code-only update ZIP
is superseded by the combined code/assets ZIP in `ciws-balancing.md`.
`git diff --check` passes. No build-number change, commit, push, release,
or binary deployment was performed.

Review corrections: C1 removed the server call to the client-only `Entity.setAngles`;
B1 aligned prediction with native bullet speed scaling; B2 restricted placement to
usable empty CIWS seats; B3 made AA seat rendering follow barrel state; B4 prevented
unreachable targets from blocking acquisition; B5 kept block-spawned ammo finite.
The final read-only review found no additional required fix. Live-game checks remain
unverified; the automated suite cannot measure live hit rate, observer interpolation,
or loaded WGE/WGC services. The follow-up regression uses real projectile instances
in a minimal World to check boxes, collision eligibility, and damage dispatch.
