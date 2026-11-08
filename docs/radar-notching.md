# Radar missile notching

Radar AA missiles can opt into server-owned notch loss and reacquisition:

```ini
NotchDegrees = 85
NotchBreakTicks = 2
NotchReacquireTicks = 3
```

`NotchDegrees` is the lower edge of a symmetric window around perpendicular.
85 means 85 through 95 degrees, measured between the missile's and target
aircraft's actual 3D velocity vectors. Higher values make the window narrower.
Zero or omission disables the mechanic. Nonfinite values and angles outside
0 < degrees < 90 disable it; malformed numeric text follows normal definition
loader error handling. Tick settings default to 2 and 3 and clamp to 1..200.
Near-zero or invalid velocity does not qualify as a notch.

After launch rigidity, consecutive ticks inside the window break tracking.
The missile coasts with normal propulsion, gravity, collision, lifetime, and
proximity fusing. Target memory is cleared and datalink target updates cannot
override the temporary loss. The launching aircraft's own radar track is not
changed. This game rule also applies during datalink guidance.

The previous target is retained only as a recovery candidate. Recovery needs
consecutive ticks outside the notch window with the target loaded, alive,
airborne, inside acquisition range and the current seeker cone, and eligible
under existing team rules. PassiveRadar (the legacy SARH alias) and
SemiActiveRadar also require the server's maintained launching-radar track.
Any failed recovery condition resets the recovery timer; a target that later
re-enters the cone or range may qualify again. Death or unloading discards the
candidate. An active missile's prior capture eligibility is restored so its
existing autonomous search can resume; searches skip notching aircraft.
ECM jamming cancels pending notch recovery. Existing chaff diversion rules
remain in force. No new packets, NBT fields, client classes, or radar-detection
rules are introduced. Existing PDHDNMaxDegree behavior is preserved.

## Initial Wargames Event 2.0.14 balance

These are gameplay tiers, not claims about real seeker specifications. Identical
missiles share settings across aircraft. All 20 arhaam_/sarhaam_ definitions in
the instance receive the three keys; the 17 aircraft-launched definitions are
enabled. Ground systems remain explicitly disabled. Other weapons are unchanged.

| Missile | Definitions | Degrees | Window | Break ticks | Recover ticks |
|---|---|---:|---|---:|---:|
| AIM-7PE2 | F-14A, F-14D | 80 | 80..100 | 2 | 6 |
| R-27ER | Su-34 | 82 | 82..98 | 2 | 5 |
| AIM-54C | F-14A, F-14D | 82 | 82..98 | 2 | 5 |
| R-77 | Su-35 | 84 | 84..96 | 2 | 4 |
| AIM-120C-8 | F-16V | 85 | 85..95 | 2 | 4 |
| AIM-120D | EA-18G | 86 | 86..94 | 3 | 3 |
| PL-15 | J-16, J-16D | 86 | 86..94 | 3 | 3 |
| AIM-120D-3 | F-15EX | 87 | 87..93 | 3 | 3 |
| Meteor | EF2000, Rafale | 87 | 87..93 | 3 | 3 |
| R-77M | Su-57 | 87 | 87..93 | 3 | 3 |
| AIM-260 | F-22A, F-35A | 88 | 88..92 | 4 | 2 |
| PL-16 | J-20 | 88 | 88..92 | 4 | 2 |
| PAC-2, 48N6, 9M311 | Patriot, S-300, Tunguska | 0 | Disabled | 2 | 3 |

At 20 server ticks per second, 2 ticks is 0.10 seconds and 6 ticks is 0.30 seconds.
Balance may need adjustment for actual turn rates, closing speed, and server TPS.

## Validation and in-game regression

Run `gradlew.bat compileJava --offline` and `gradlew.bat test --offline`.
MCH_RadarNotchTest covers angles, 3D and invalid velocities, disabled defaults,
timing, blocked datalink/snapshot steering, recovery eligibility, active capture,
target death/unloading, jamming, autonomous scans, and SARH support requirements.

With a rebuilt mod on the dedicated server and clients, restart the instance to
load the updated definitions. Test:

1. Fire an AIM-120C-8 at a moving aircraft. Hold an 85..95 degree velocity
   crossing: tracking should break after two ticks and steering should stop.
2. Leave the window while still inside the seeker cone and range: tracking
   should return after four eligible ticks. Re-entering the window resets recovery.
3. Repeat the same 86 degree crossing against an AIM-260: it should retain track.
   Hold 88..92 degrees for four ticks to break it; recovery takes two eligible ticks.
4. Repeat with an AIM-7. Recovery must fail with the launching radar disabled or
   tracking another aircraft, and succeed only when support and geometry return.
5. While notched, leave the cone/range, unload or destroy the target, and exercise
   datalink updates, ECM, and chaff. No stale-target steering or resurrection.
6. Confirm vertical maneuvers work, and propulsion, collision and proximity
   fusing still work during coasting. A notch does not guarantee a miss.
7. Compare Patriot, S-300, Tunguska and an unmodified IR missile: their notch
   behavior should be unchanged. Check a second player's view and server logs.

The instance definitions are external to this source repository. Their original
bytes and a complete weapons-folder SHA-256 manifest are retained locally under
`build/notching/` for verification and rollback; that build folder is ignored by Git.
The code must be rebuilt and installed before these definition keys take effect.
