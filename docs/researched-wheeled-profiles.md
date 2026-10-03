# Wheeled vehicle movement profiles

This supersedes the earlier role-based pack tuning. The active local pack was
found in the same Prism instance under `MCHELI-R-WDG-Edition-1.1.8_X173`, replacing
the previous X172 folder. AllTankSpeed is 1.00. Only the 11 enabled wheeled
definitions are edited; the other 16 tracked definitions retain their bytes.

## Units and supported controls

One block is treated as approximately one metre at 20 simulation ticks/second.
`Speed = road km/h / 72` remains the existing content key. Model dimensions and
Minecraft terrain do not make these exact vehicle performance measurements.

Two optional keys allow different vehicle profiles without moving their terrain
probes or adding a vehicle-specific class:

- `WheeledAcceleration`: peak low-speed acceleration in metres/second squared,
  0.1–10.0. Converted to blocks/tick squared by dividing by 400. For configured
  profiles, acceleration tapers with speed using `1 - 0.75*(speed/topSpeed)^2`.
  This is an approximation of diminishing engine pull, not a measured engine,
  transmission or aerodynamic simulation. Pull remains positive near top speed.
- `WheeledTurnRadius`: minimum nominal chassis-path radius in blocks/metres,
  2–50, at low speed and full steering lock. It calibrates the equivalent bicycle
  geometry at the existing 35-degree steering range. Physical `SetWheelPos`
  coordinates remain unchanged. Actual real-world outer-wheel, bumper and
  centerline circles are different; the selected radii are approximate equivalents.

Missing keys preserve the preceding default acceleration calculation and
axle-derived steering geometry. Reloading resets overrides if keys are removed.
Malformed, non-finite or out-of-range values fail at definition loading.

The shared high-speed lateral budget is now 5 m/s² (about 0.5g), replacing
32 m/s² (about 3.3g). It is a conservative usable road-handling assumption, not a
measured tire-grip rating. Slow down for tight turns; faster turns widen naturally.
Throttle still takes two seconds to build and one second to release. Braking,
coasting, reverse limits, intent validation and server movement authority are
preserved. New profile settings do not change turret or camera interpolation.

## Selected profiles

The user-requested gameplay follow-up multiplies every listed peak acceleration
by 1.5. Road caps, turning radii and the taper curve retain the researched tuning.
The table below shows the current boosted values, rather than the original
research estimates. ITV and VDV still share exactly the same movement settings.

Every acceleration and the resulting times below is a tuning estimate, not a
published acceleration test. Times are simulated from rest with continuous
ground contact/full forward input at 20 TPS, including acceleration taper and
excluding hills, steering, collisions, cargo variation and surface resistance.
Top speed is a road target; a separate road/off-road or amphibious speed model
is outside this tuning pass.

| Vehicle | Road target km/h | Speed key | Peak accel. m/s² | Low-speed radius m | Estimated 0–60 km/h s | Estimated 0–top s |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| M1161ITV | 137 | 1.902778 | 3.750 | 3.50 | 4.70 | 15.40 |
| VDV BUGGY | 137 | 1.902778 | 3.750 | 3.50 | 4.70 | 15.40 |
| MRAP | 105 | 1.458333 | 2.100 | 8.00 | 8.70 | 21.10 |
| SPM-3 | 90 | 1.250000 | 1.650 | 8.50 | 11.55 | 23.05 |
| LAV-25 | 100 | 1.388889 | 1.950 | 7.75 | 9.50 | 21.65 |
| LAV-AD | 100 | 1.388889 | 1.800 | 7.75 | 10.30 | 23.45 |
| CS/SA5 | 100 | 1.388889 | 1.650 | 9.00 | 11.20 | 25.60 |
| FMTV | 96 | 1.333333 | 1.350 | 9.50 | 13.85 | 30.05 |
| Ural-4320 | 85 | 1.180556 | 1.050 | 9.50 | 18.45 | 34.20 |
| M142 HIMARS | 85 | 1.180556 | 1.200 | 10.00 | 16.15 | 29.90 |
| S-300PM | 60 | 0.833333 | 0.825 | 12.00 | 30.70 | 30.70 |

The VDV intentionally copies the ITV's movement profile at the user's request;
it is not a claim about a particular Russian buggy. MRAP names a vehicle family,
so its target is a representative armored 4×4 estimate, using M-ATV-class road
performance as a reference rather than claiming an identified exact variant.

## Research and uncertainties

- M1161: the [Growler manufacturer specification, PDF pages 8–9](https://www.growlerme.com/_files/ugd/9e4f62_b4f99c060cbe4c17a81c39f5613519f4.pdf)
  lists 85 mph. The [earlier General Dynamics brochure](https://warwheels.net/images/M1161GrowlerBrochureGDOTS.pdf)
  states 65+ mph and documents four-wheel steering. The selected 137 km/h uses
  the later manufacturer figure; the tight 3.5 m radius is an estimate informed
  by four-wheel steering, not an exact turning-radius specification.
- LAV-25: [Marine Corps vehicle information](https://www.marines.mil/News/News-Display/article/646113/1st-lar-trains-for-real-world-combat/)
  gives approximately 62 mph. A 100 km/h road target is a rounding of that value.
  The 7.75 m nominal radius is a tuning estimate, not a verified centerline
  turning-radius measurement for this exact variant.
  LAV-AD retains the same road cap and radius as its shared chassis, with slower
  acceleration as a variant assumption. No arbitrary lower road cap is imposed
  solely because it carries an air-defense turret.
- SPM-3: [Rosgvardiya's vehicle specification](https://rosguard.gov.ru/ru/page/index/btrvv-spm3-medved)
  gives 12,000 kg and at least 90 km/h on highways. The chosen 90 km/h is a
  conservative road target; acceleration and radius are estimates.
- FMTV: the [Oshkosh FMTV cargo specification](https://oshkoshdefense.com/wp-content/uploads/2018/12/17305_FMTV_Cargo_SS-A4size_LowRes_4.15.2015.pdf)
  lists 96 km/h for both 4×4 and 6×6 cargo trucks. The pack's modeled three-axle
  truck is treated as the 6×6 family; radius and acceleration are estimates.
- Ural: the [URAL manufacturer catalogue, PDF page 4, mirrored by a distributor](https://www.batseer.mn/files/ural/Catalogue_URAL_eng_2010.pdf)
  lists 85 km/h and 11.4 m external turning radius at the bumper for the 4320-31.
  The selected 9.5 m chassis-path radius is an approximate inward conversion,
  not another published measurement. The file does not identify a precise suffix.
- HIMARS: [Marine Corps vehicle appendix, section E-3](https://www.29palms.marines.mil/Portals/56/Docs/LAS-SUA-Archive/EIS/29Palms-Draft-EIS-Vol-II-Appendices.pdf?ver=dSgaGDuraaE_2fEfWocHWw%3D%3D)
  lists 53 mph, approximately 85 km/h. The [Lockheed Martin product description](https://www.lockheedmartin.com/en-us/products/himars.html)
  identifies the FMTV 5-ton truck platform. Radius and acceleration remain
  estimates for the loaded launcher, not measured vehicle tests.
- CS/SA5: the [U.S. Army vehicle entry](https://odin.t2com.army.mil/WEG/Asset/007e482d9ba53f0e2829dd1d01f1f0a7)
  identifies a four-axle Type-08-family chassis. Its 100 km/h cap, 9 m nominal
  radius and acceleration here are chassis-class estimates; no exact CS/SA5
  mobility test was verified.
- S-300PM: different launchers use different chassis. This model's four-axle
  transporter is provisionally treated as an older MAZ-derived heavy TEL. Its
  60 km/h cap, 12 m radius and low acceleration are conservative class estimates.
  A precise chassis variant and exact mobility specification were not verified.
- MRAP: [Oshkosh's M-ATV family description](https://oshkoshdefense.com/vehicles/mine-resistant-ambush-protected-mrap/)
  provides class context only. Its selected numerical profile is an estimate
  because the pack/user identifies the family rather than a specific chassis.

## Implementation, review and verification

Production files: `MCH_TankInfo.java` loads and resets the two optional fields;
`MCH_WheeledControlMath.java` applies explicit acceleration/taper and the corrected
lateral budget; `MCH_EntityTank.java` reads these server-side definition values.
`MCH_TankInfoWheeledTest.java` and `MCH_WheeledControlMathTest.java` cover units,
rate separation, reachable top speeds, definition reload/removal, invalid inputs,
braking, reverse permissions and grip. `wheeled-handling.md` links this follow-up;
this report documents the 11 definition files named in the profile snapshot.

Sequential review: ACCEPTABLE. No client-only dependency was added to the math
or definition loader. Movement and profile choice remain server-owned; packets,
NBT and existing keys retain their format. Tracked handling retains its path.
Wheeled definitions without new fields retain their previous acceleration and
low-speed radius; all wheeled vehicles receive the revised high-speed grip limit.

The updated code must be rebuilt and installed on server and clients. Adding the
new fields to an older compiled mod does not enable those fields. This pass edits
source and definitions; it does not package/install binaries or increment builds.

Validation: Java 8 `gradlew.bat compileJava test --offline` succeeded. All 42 tests
passed with zero failures or errors; `git diff --check` passed. The build number
remains 170. External updates checked every original hash before writing and
whitelisted the 11 files, with rollback on failure. Independent read-back checks
confirmed all planned values, exactly 11 changed files, all other bytes/16 tracked
files unchanged, and no redundant legacy driving keys in the wheeled files.

All 27 original definitions, SHA-256 hashes, the applied verification and the
11-file tuning plan are saved in
`build/researched-wheeled-profiles-378bdc9dcf0e4d47b076e153d95cba04/`.

The subsequent acceleration-only backup and applied hash verification are in
`build/wheeled-acceleration-buff-c28af4e2d6cb43bca44e99fdbf629aff/`. All 27 files
were backed up again before this follow-up; only the 11 `WheeledAcceleration`
values changed. See [moving cannon alignment](wheeled-cannon-alignment.md) for
the corresponding presentation fix and its live-game regression checklist.

Manual checks: use a flat dry straight road at stable 20 TPS; race ITV/VDV and
LAV25 from rest, record position distance/time and eventual speed rather than
throttle percentage, then compare their slow full-lock circles. Check gentle
high-speed turns, braking before sharp bends, slopes, walls, reverse, and turret
aim hold while steering. Compare driver/passenger/observer views and test a
tracked vehicle. These live-game results remain unverified. The original LAV
speed impression is not confirmed as a bug: current definitions already made
the ITV faster; terrain, cornering, presentation, loaded definitions or TPS may
affect a casual comparison.
