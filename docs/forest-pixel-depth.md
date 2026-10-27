# Forest pixel-depth correction

WGMap terrain and FMUR/MCHR LOD composites now evaluate the same existing v2
physical-depth curve per fragment. Separate affine depth fits could place a
foreground tree behind a nearby vehicle, even with complete fresh snapshots.
The actual GPU regression reproduced this with a vehicle two blocks behind a
crown; it now checks both hidden-behind and visible-in-front controls at normal
and scoped FOVs and three transition distances.

MCHR uses the existing precision layer throughout projected rendering and
withholds if the layer cannot begin. No approximate direct fallback is used.
Its local far plane also supports larger client render distances. FMUR retains
the same strict mask readiness policy. Native near rendering is unchanged.

Update all three client mods together. No server update, terrain regeneration,
cache deletion, config or save migration is required for this correction.
Tracking/radar/gameplay and fast terrain-change synchronization are unchanged.
GPU frame-time and in-game testing at the original location remain required.

Full diagnosis, files, commands, review and runtime checklist are in WGMap's
`docs/HANDOFF_FOREST_PIXEL_DEPTH.md`.
