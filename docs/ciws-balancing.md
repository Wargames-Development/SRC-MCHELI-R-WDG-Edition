# C-RAM and HPJ 1130 balancing — 2026-10-16

The supplied PrismLauncher assets have been changed from the temporary maximum
response preset to short-range point defense. One CIWS AA egg automatically selects
model-specific gunner tuning from its current weapon. No NPC or legacy eggs return.

## Earlier engagement adjustment — 2026-10-17

Playtesting found the original 180/240-block envelopes and 10/6-tick reaction
delays left too little time for fast incoming missiles, even with a ready barrel.
The two model profiles now use **320 blocks / 4 reaction ticks for C-RAM** and
**420 blocks / 3 reaction ticks for HPJ 1130**, both scanning every **2 ticks**.
At 20 TPS this reduces reaction delay from 0.50/0.30 s to 0.20/0.15 s. The extra
140/180 blocks provide roughly 0.58/0.75 s more warning against a straight incoming
missile moving 12 blocks/tick. This is additional tracking time, not a promised kill.

Only `gunner_aa_cram.txt` and `gunner_aa_hpj1130.txt` change gameplay. Radar ranges,
round lifetime, traverse, spread, damage, rate of fire, bursts, heat, ammo and team
rules remain as below. The new spheres stay below their 450/600-block radars and
approximately 388/493-block native straight-line round travel limits. Existing
intercept feasibility, terrain and loaded-chunk checks still apply. Acquisition
and firing share this range setting, so aircraft also become eligible farther out.
Other guns retain the base profile's 180-block fallback.

The profile binding/range tests were updated, and a regression advances a hostile
missile at 12 blocks/tick through each real model profile with its normal round
lifetime. Both prealigned guns must fire before the missile crosses the former
180/240-block boundary. Java 8 `gradlew.bat compileJava test --offline` passed:
**126 tests, zero failures/errors**. Two existing test-only Unsafe warnings remain.
Review: acceptable; this changes definition values without Java gameplay, protocol,
registration or save-format changes. Live missile interception still needs testing.

Install `build/ciws-earlier-engagement/ciws-earlier-engagement-profiles.zip` over
the existing mod folder on client and server, after the combined removable-gunner
update, then restart. It contains just:

* `assets/mcheli/gunners/gunner_aa_cram.txt`
* `assets/mcheli/gunners/gunner_aa_hpj1130.txt`

The supplied client asset folder is updated with originals backed up under
`assets/mcheli/.ciws-earlier-engagement-backup-20261017/`. The server is not deployed.
In-game: launch an enemy survival missile toward an already-facing gun from beyond
320/420 blocks, confirm tracking and firing begin before the old boundary, then
repeat from the side and as a salvo. Check a friendly missile remains ignored and
terrain still blocks engagement. Test at a loaded location; this does not force
chunks to load. The probability model later in this document describes the original
180/240 tuning and has not been recalibrated for this adjustment.

## Pack baseline and real-world role

Audited **195 weapon, 27 tank, 21 plane, 11 helicopter and 5 vehicle definitions**.
The complete read-only snapshot is `build/ciws-balance-audit/assets.json`.

| Pack reference | Radar range (blocks) | Relevant weapon reference |
| --- | --- | --- |
| Gepard G1A4 | 900 | Stinger 275; 35mm power 25, accuracy 1.1 |
| Tunguska 2K22 | 1200 | 9M311 500; 30mm power 22, accuracy 1.1 |
| CSSA5 | 1500 | FB10A 350; 30mm power 15, accuracy 1.1 |
| S-300 / Patriot | 2800 / 2600 | Long-range missile defense |
| Fighters | Mostly 1850–2500 | Short-range AAM 350–500, longer-range AAM 900–1350 |
| Attack helicopters | 850–1200 where explicitly configured | ATGM examples 400–1500 |
| GPS attack weapons | — | GBU-39 550, AGM-84 dual 1000, AGM-158 1900, Kh-101 2800 |

Aircraft generally have 350–550 HP; attack helicopters generally 300–400 HP.
The previous gunner's 1000-block engagement radius overlapped missile defenses.
The initial reduction was to 180/240; playtesting revised this to 320/420, retaining
a short-range defense role relative to the pack's longer missile systems. Radar display/track range
remains longer than autonomous gun engagement range.

The real-world basis is role and relative capability, not literal meters or rounds
per minute copied into Minecraft. [RTX describes Phalanx as terminal defense and
the Army's land counter-RAM system](https://www.rtx.com/en/raytheon/what-we-do/sea/phalanx-close-in-weapon-system).
The [Royal Australian Navy lists Phalanx as a 20mm, 4500-round/minute weapon](https://www.navy.gov.au/capabilities/weapons/phalanx).
China's [Ministry of National Defense identifies Type 1130 as an 11-barrel 30mm system
firing more than 10,000 rounds/minute](https://eng.mod.gov.cn/2025xb/D/S/16443188.html).
Public real-world interception claims are not used as a guaranteed in-game success
percentage. The game uses slower, compressed rates and distances; HPJ's larger
volume, heavier round and somewhat greater reach are retained within that scale.

## Applied values

| Parameter | C-RAM | HPJ 1130 |
| --- | --- | --- |
| AI acquisition/engagement sphere | 320 blocks | 420 blocks |
| Radar maximum target range | 450 blocks | 600 blocks |
| Radar scan / base detection chance | 10 ticks / 0.90 | 8 ticks / 0.93 |
| Gunner scan interval | 2 ticks | 2 ticks |
| Acquisition/switch reaction | 4 ticks (0.20 s) | 3 ticks (0.15 s) |
| Look limit / tracking response | 4 degrees/tick / 0.35 | 5 degrees/tick / 0.45 |
| Persistent aim bias cap / fire tolerance | 0.15 / 0.90 degrees | 0.10 / 0.80 degrees |
| Correct lead factor | 1 | 1 |
| Burst / pause | 18 / 6 ticks (0.90 / 0.30 s) | 24 / 4 ticks (1.20 / 0.20 s) |
| Delay / nominal server shot rate | 2 ticks / 10 rounds/s | 1 tick / 20 rounds/s |
| Sustained controller duty limit | About 7.5 rounds/s | About 17.1 rounds/s |
| Native weapon accuracy width | 1.8 degrees | 1.4 degrees |
| Direct-hit power | 12 | 18 |
| Plane / heli damage factor | 1.25 / 1.0 | 1.35 / 1.1 |
| Heat per shot / limit | 3 / 300 | 4 / 400 |
| Magazine / maximum ammo | 1550 / 1550 | 2560 / 2560 |
| Supplied rounds | 1550 | 2560 |
| Reload when supplied | 240 ticks (12 s) | 300 ticks (15 s) |
| Round lifetime | 22 ticks | 28 ticks |
| Initial native round speed | About 17.55 blocks/tick | About 17.55 blocks/tick |
| Explosive size | Existing 1 | Existing 2 |
| Debug logging | Off | Off |

RadarType's existing C-RAM `MODREN_AA` typo was normalized to `MODERN_AA`.
Mount limits, global damage settings, team rules, interception flags, collision
boxes, speed and the working line-of-sight/lead fixes are preserved. Nominal speed
uses the existing 3.9 constructor cap and 18/4 bullet scaling. The new lifetime also
limits stray rounds; it is longer than the normal engagement envelope. Acquisition
rejects solutions that the projectile cannot reach before its native fuse expires.
Magazine/reload choices are game-balance values, not claims of an exact real-world
reloading procedure. Previously saved HPJ ammo stays as saved; this grants no refill.

## Interception probability

There is no fixed per-missile invulnerability or post-hit failure roll. A real
physical hit still intercepts enabled ammunition. Probability changes through
normal spread, a shorter time window, slower tracking, reaction delay, burst pauses,
round lifetime, weapon cooldown and heat. Lead remains correct; aim bias stays below
fire tolerance so difficulty does not recreate a permanently aligned-but-not-firing
state. One gun still handles one track at a time and pays reaction delay on switching,
making salvos, fast attacks, terrain and unfavorable bearing meaningful.

For the original 180/240 tuning, `build/ciws-balance-audit/interception-model.json` records a seeded, simplified
relative-motion box model with 2000 trials per speed/facing case. It uses the native
1m projectile body plus 0.3m collision expansion, angular spread, turn/response,
scan/reaction, burst, cooldown and round lifetime. Examples for a straight incoming
projectile from a random bearing, with an initially forward-facing gun:

| Constant speed (blocks/tick) | C-RAM modeled interception | HPJ modeled interception |
| --- | --- | --- |
| 1.5 | 100% | 100% |
| 3.9 | 67.8% | 100% |
| 8 | 18.6% | 57.4% |
| 12 | 1.0% | 27.5% |

These are **model estimates, not measured live-server rates or guarantees**.
All prealigned single-projectile cases in that model were intercepted; conditional
mean interception times were approximately 0.65–1.47 seconds. A ready, correctly
pointed gun is intentionally effective against a single straight incoming threat.
The model omits guided maneuvers, terrain, warhead splash, salvos, networking, heat
over repeated engagements and loaded team services. Constant speeds are scenarios,
not an assertion that every named missile maintains that speed. The function and
seeded results are archived beside the asset audit for later comparison.

## Implementation, validation and review

| Files changed in this follow-up | Purpose |
| --- | --- |
| `src/main/java/mcheli/mob/MCH_GunnerInfo.java` | Conservative built-in defaults and validated repeatable `WeaponProfile = weapon,profile` bindings |
| `src/main/java/mcheli/mob/MCH_EntityGunner.java` | Resolve one weapon-specific timing profile, retain base placement/team identity, and respect native round lifetime |
| `src/main/resources/assets/mcheli/gunners/gunner_aa_default.txt` | Base egg profile with C-RAM/HPJ bindings and conservative fallback |
| `src/main/resources/assets/mcheli/gunners/gunner_aa_cram.txt`, `gunner_aa_hpj1130.txt` | Independent model range/response/burst definitions; both use the existing AA item name |
| `src/test/java/mcheli/mob/MCH_GunnerInfoTest.java` | Mapping validation and case normalization |
| `src/test/java/mcheli/mob/MCH_GunnerEngagementTest.java` | Actual profile binding, range/reaction/burst, legacy/fallback/custom behavior and fuse-limited failover; earlier diagnostic tests explicitly select maximum-response test tuning |
| Supplied assets: `vehicles/cram.txt`, `vehicles/hpj1130.txt` | Radar ranges, scan and detection tuning |
| Supplied assets: `weapons/ac_cram_m61a1.txt`, `weapons/ac_hpj1130_30mm.txt` | Spread, shot rate, damage, heat, ammo, reload and fuse tuning |
| Supplied assets: three `gunners/gunner_aa_*.txt` files | Install the same source profiles in the actual pack |
| `docs/ciws-aa-gunner.md`, this document | Defaults, rationale, compatibility, model limitations and testing |

Review verdict: acceptable. This is an additive definition field with no new item,
registry, packet or NBT identifier. Existing default/unnamed gunners pick up the model
binding on restart. Explicit custom profiles without bindings retain their own
settings; missing or non-AA mapped profiles fall back safely to the base. Profile
resolution is one level and cannot recurse. Java changes stay in the existing server
controller and definition classes. All other vehicle/aircraft/weapon definitions
were read for comparison and were not modified.

Java 8 `gradlew.bat compileJava test jar reobf --offline` passes: **122 tests, zero
failures/errors/skips**. `git diff --check` passes. Build number remains 180.

## Installed assets, backup and combined update

Seven asset files were applied and hash-verified in the supplied PrismLauncher
folder. Five originals are preserved under
`assets/mcheli/.ciws-balanced-backup-20261016/`, with a manifest identifying the two
new profile files. Earlier interception/flag backups remain separate.

`build/ciws-balance-audit/ciws-balanced-X180-update.zip` contains the matching
reobfuscated `mcheli/` classes, `mcmod.info`, and **only those seven changed assets**.
Every entry was verified against the rebuilt jar or staged assets. This supersedes
the earlier code-only ZIPs and includes the aiming and egg-removal fixes. The local
assets are installed; Java classes were not hot-replaced in the running game. No
commit, push, release upload or server deployment was performed.

Stop client/server, back up their current code and affected assets, and extract the
combined update into each existing extracted MCHR installation. Preserve the rest
of the asset pack. Restart both sides; install one MCHR copy per side. A code-only
update would miss the weapon/radar edits, while assets alone would miss automatic
HPJ profile selection and fuse-aware acquisition.

## In-game acceptance checks

1. Hover enemy survival aircraft just outside and inside 320 blocks for C-RAM and
   420 for HPJ, measured from the muzzle. Outside: no autonomous acquisition/firing.
   Inside: acquisition, visible turn/reaction delay, ordinary burst fire and damage.
2. At 100–150 blocks, fire single enabled missiles with each gun initially facing
   toward, sideways to and away from the launch. Repeat many trials rather than
   treating one success/failure as the probability. HPJ should recover more quickly.
3. Repeat fast rockets and simultaneous salvos from two bearings. Some should break
   through; a prealigned single slow missile can still be consistently intercepted.
4. Test a moving aircraft, terrain cover and missile-to-aircraft failover. Confirm
   finite magazines, resupply/reload and sustained heat behavior. Change target teams
   and repeat friendly, unknown-owner and creative exclusions on WGE and WGC separately.
5. Confirm only the CIWS AA egg appears. To collect tuning logs, enable `DebugGunner`
   in the selected model profile, then restart. The base profile's field does not
   override a mapped model's complete settings.

This is the first balanced preset after correctness testing. Actual interception
rates and aircraft time-to-kill still need multiplayer playtesting under the server's
typical approach geometries, ammunition supply and global damage settings.
