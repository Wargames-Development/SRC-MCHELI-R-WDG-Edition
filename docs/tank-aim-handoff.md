# Tank aim at the entity tracking boundary

The X216 snapshot extension supplies server weapon-set angles. The loaded model
still rendered its own client weapon-set angles, which can differ after dismounting
or respawning the client entity. Crossing the entity tracking boundary switched
between those sources. The earlier regression compared matching source values and
missed this case.

## X218 changes and review

- B9 fixed in MCH_RenderFarVehicle.java. Observed loaded tanks and detached tanks
  now sample the same server aim timeline. The loaded model temporarily receives
  that sampled pose during common-part rendering. A finally block restores its
  local weapon angles and previous/current rider angles.
- The local mounted crew and locally controlled aircraft keep their existing aim
  path. The render override requires matching UUID and weapon-set count.
- MCH_FarVehicleDepthLayerLightingTest now deliberately gives the client different
  angles, compares both rendering paths across eight crossings, checks exceptional
  cleanup, and rejects missing snapshots and mismatched identities.
- gradle.properties advanced to X218 through the normal build task.

Review verdict is ACCEPTABLE WITH MINOR ISSUES. T4 is live multiplayer acceptance.
The runtime edit is confined to the existing client renderer. Snapshot capture,
packet layout, server authority, terrain occlusion and saved data retain X216
behavior. The temporary mutation is render-thread scoped and uses a reusable
bounded array, five floats per weapon set plus four rider angles.

## Validation

Using Java 8 and the repository wrapper with the cached Windows LWJGL natives,
the focused test command passed for MCH_FarVehicleDepthLayerLightingTest and
PacketEntityInfoSyncTest. The full command was
`gradlew.bat --offline --no-daemon build --console=plain`.

The build passed with 256 tests passed, two optional FMUR gun tests skipped, and
zero failures or errors. Production packaging verification and git diff whitespace
checks passed. Logs are build/tank-aim-handoff-tests.log and
build/tank-aim-handoff-release.log. The jar is
build/libs/MCHELI-R-WDG-Edition-1.9.01_X218.jar.

The jar has platform-independent Java rendering changes. Windows OpenGL tests ran
locally. Linux/macOS gameplay and a dedicated-server boot were not executed.

## Runtime acceptance

Use MCHR X218 on the client with an X216-or-newer server, which supplies weapon
aim snapshots. In the reported area, aim the tank turret sideways and elevate its
barrel, dismount, then cross 142–153 blocks repeatedly in both directions. Check
normal view, PiP, thermal, and shaders on/off. Repeat after reconnecting, with an
observer present while another player rotates the turret, and with the local player
controlling each crew position. Confirm aim continuity and responsive local controls.
Also check a commander weapon with a different aim from the main gun.

The artifact is in the repository build/libs directory. Installation and live
acceptance are pending. No additional roadmap work was introduced.
