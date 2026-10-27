# Vehicle LOD visibility range

In the client's `config/mcheli.cfg`, `[General]`:

```ini
VehicleLodRenderDistance = 750
```

Default 750 blocks; consumed values clamp to 1..4096. The unified vehicle model
pass checks true 3D view-to-vehicle distance before model/depth work. Regular and
thermal replay obey the same limit. Tracking/contact sync remains 4096 blocks;
radar, targeting, missiles, markers and server logic are unchanged. This is a
client presentation setting, without new server enforcement. Other specialised
renderers outside this unified pass are unchanged.

Updated WGMap clients accept an optional `VIEW_DISTANCE_VERSION = 1` hint through
the existing optional client adapter. WGMap uses configured range + 250 blocks
for a centre-out local-first preload. Base depth API/projection versions and old
WGMap compatibility remain intact. The nearest 256 regions and 192 MiB payload
cap bound the working set; high settings may not receive complete RAM coverage.
Explicit laser/tool terrain queries retain a separate bounded on-demand corridor.

Changed: `MCH_Config`, `MCH_RenderFarVehicle`, `MCH_WGMapDepth`, and
`MCH_VehicleRenderDistanceTest`. Java 8 wrapper `compileJava` and `test` passed
(162 tests). No tracker, radar, weapon, missile or network source was edited by
this task. Existing working changes were preserved.

In-game validation still required: test 749/750/751-block models, thermal,
native/snapshot transitions, a configured override, radar/locks/missiles beyond
750, optional WGMap absence/legacy versions, mountain hiding, high-speed travel,
and dedicated-server startup. No GPU or 50-player performance result is claimed.
See WGMap `docs/HANDOFF_VEHICLE_VISIBILITY_RANGE.md` for full bounds and review.
