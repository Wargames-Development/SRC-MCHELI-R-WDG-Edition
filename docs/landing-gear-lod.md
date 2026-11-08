# Landing gear on distant aircraft

Snapshot-only aircraft previously drew landing-gear model parts at their resting
positions. The server snapshot did not carry landing-gear rotation, so a distant
observer could see extended gear even while the pilot had retracted it.

MCH_EntityInfo now captures the server's existing part rotation: 0 degrees is
extended and 90 degrees is retracted. PacketEntityInfoSync appends an optional
GEAR trailer after ARM1 with one float per snapshot record. Existing records,
trailer order, packet IDs, definitions, and saved-world data are unchanged.
The decoder accepts older packets without GEAR and rejects invalid counts,
truncated payloads, nonfinite rotations, and values outside 0..90 degrees.
Older clients can ignore the appended trailer; both server and observing client
need the update to display the corrected distant gear state.

MCH_RenderAircraft exposes its existing gear transforms through an overload that
accepts rotation directly. MCH_RenderFarVehicle uses that method for snapshot
models instead of drawing resting gear. This preserves rotating, reversed,
two-axis, sliding, scaling, and gear-door behavior. Loaded-aircraft rendering
continues to interpolate its existing entity part rotation. Snapshot-only gear
follows received server rotations at the existing snapshot cadence; no guessed
flight/altitude-based state or client gameplay changes are introduced.

The far pose retains the loaded aircraft's gear rotation during handoff. If an
older server supplies no gear data, it keeps the last known rotation; a new
unknown contact retains the legacy extended default. Identity changes reset
that fallback so one aircraft cannot inherit another aircraft's gear pose.

Changed files: MCH_EntityInfo (capture), PacketEntityInfoSync (optional transport),
MCH_RenderAircraft (shared transforms), MCH_RenderFarVehicle (snapshot pose and
drawing), PacketEntityInfoSyncTest and MCH_AircraftSnapshotRotationTest
(round trips, legacy packets, malformed payloads, and fallback handling).

Validation: `gradlew.bat compileJava test --offline` and `git diff --check`.

In-game verification still required with two clients and an updated server:

1. Retract and extend the gear while the observer is beyond normal entity
   tracking range but within VehicleLodRenderDistance. Both endpoints should
   match the pilot's actual gear state after snapshot delivery.
2. Toggle gear while distant. Check intermediate poses and gear doors using
   aircraft with rotating and scaling/sliding gear definitions.
3. Move the observer through the loaded/snapshot boundary with gear up, then
   down. Verify the gear does not return to its resting pose on handoff.
4. Repeat in thermal view, reload the aircraft's chunk, and observe another
   aircraft of the same type to check stale-pose handling.

This source change is not installed into the supplied Prism instance by these
validation commands. Rebuild and install the mod on the server and clients
before checking the visual result. No OpenGL or multiplayer visual test is claimed.
