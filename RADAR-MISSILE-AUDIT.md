# Radar missile launch audit

## Findings and fixes

- **B1 — AA launch rejected a maintained BVR radar track without a local target.**
  `MCH_WeaponAAMissile.shouldBlockShotWithoutBvrRadarTrack` required either a
  Minecraft client entity or a snapshot younger than 1.5 seconds. Radar tracking
  uses its own validity checks and a 4-second stale threshold. A valid radar track
  could therefore coexist with a weapon rejection. Preserve its ID as launch
  intent, following the existing AT launch path; the server resolves and validates it.
- **B2 — AA datalink repeated local seeker checks after radar validation.**
  `shouldBlockShotByDataLink` required local target data and the pilot's look FOV
  even for a maintained integrated BVR radar track. Match the AT datalink behavior
  for this case. Other datalink paths keep their existing checks.
- **B3 — AA and AT lock maintenance discarded maintained track IDs.**
  The radar update methods cleared weapon target IDs when snapshots aged out or
  were unavailable. Keep maintained integrated radar track IDs independently of
  optional client guidance coordinates. Missing snapshots are still never used
  to invent target coordinates.
- **B4 — Candidate validation checked the previous target's countermeasures.**
  `MCH_WeaponGuidanceSystem.canLockEntity(entity)` checked `targetEntity` for chaff
  and ground-vehicle flare use. This could reject a new valid target because the
  previous target was deploying countermeasures, or admit a protected candidate.
  Check the supplied candidate instead.

## Review

Reviewed AA/AT launch gates, radar lock maintenance, datalink targeting, shared
seeker validation, radar snapshot and heartbeat handling, weapon request transport,
and server launch validation. This is a bounded launch/lock audit, not a complete
review of missile flight physics or every radar mode.

The server still resolves target IDs in the firing player's world and applies
existing target-type, range, ECM, radar-power and required-track checks. The patch
adds no chunk loading, client imports, packet fields, identifiers or save changes.
Pure IR snapshot acquisition and lock timing are unchanged; the shared candidate
countermeasure correction also affects other seekers.

Review verdict: **ACCEPTABLE**, with **T1** (multiplayer runtime verification) open.
The code establishes these rejection paths; the original incident has not been
reproduced in a running game, and attribution to an earlier IR change is unconfirmed.

## Validation

- `gradlew.bat compileJava`: passed; existing deprecation/unchecked warnings.
- `git diff --check`: passed.
- No existing `src/test` suite is present. No automated gameplay tests were run.
- No build number, configuration, content definition or protocol changes.

## T1 — Manual multiplayer regression checklist (not yet run)

Use two players in opposing-team aircraft on a dedicated server. Keep the target
server-loaded by its pilot. Choose a BVR/radar-equipped launcher and compatible
active and semi-active/passive radar missiles. Keep both aircraft within radar
and missile range and the radar's permitted tracking angles/altitudes.

1. Acquire a maintained radar track beyond the shooter's normal client entity
   tracking distance, then fire. Expect a server-spawned missile with the correct
   target and no false "Lock a target with radar first" rejection. Repeat with
   datalink enabled and disabled and with each supported radar missile type.
2. Fly the target into and out of client entity range while maintaining radar
   tracking; fire on both sides of the boundary. Expect the same target ID and
   launch eligibility. Check missile guidance after the transition as well.
3. For a maintained datalink radar track, turn the pilot's view away while keeping
   the aircraft radar on target. Expect no additional pilot-look seeker rejection.
4. Unlock or disable radar, then fire. Expect rejection. Move beyond missile
   range while remaining within radar range; expect server rejection without a
   missile or authoritative ammunition loss. Also test target death/despawn.
5. Lock aircraft A, have A deploy chaff, then acquire an otherwise valid aircraft B
   using the seeker. Expect A's chaff not to block B. B's own chaff/ECM should
   still invoke the existing countermeasure rejection behavior.
6. Repeat an integrated radar AT launch against a valid ground vehicle across
   the render boundary. Check normal local launches and supported non-BVR
   datalink modes for regressions.
7. Repeat pure IR acquisition and firing locally and via distant snapshots.
   Expect existing lock timing, range, seeker-angle and ground restrictions.

For B1's stale-snapshot edge case, a debugger can age the target snapshot beyond
1.5 seconds while the radar still maintains its track. The client should send the
track ID; server rejection remains expected if the actual target/track is invalid.
Targets unloaded on the server are outside this fix's supported scenario.
