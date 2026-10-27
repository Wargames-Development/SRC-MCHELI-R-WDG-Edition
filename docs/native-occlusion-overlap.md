# Native terrain / LOD overlap

WGMap retains cached occlusion through a one-chunk band before the visual
transition. A loaded chunk is skipped there only when its whole horizontal bounds
fit inside the safe native interior; chunk-center membership alone is insufficient.
Native foreground depth is preserved. The precise camera Z=7544 mask-drop case
and its mixed native/cached GPU counterpart now pass regression checks.

MCHR and FMUR acquire their mask one chunk earlier. Native-band models remain
at their original physical positions; visual compression starts at its original
distance. Unknown/partial masks still withhold affected LODs. Tracking, radar,
weapons, configured ranges and fast terrain update behavior remain unchanged.

Update WGMap, MCHR and FMUR clients together. No server update, new configuration,
cache deletion, terrain regeneration or save migration is needed. Existing work
and memory limits remain; live dense-forest, transition and graphics-pack testing
is still required. Full review and validation are in WGMap's
`docs/HANDOFF_NATIVE_LOD_OCCLUSION_OVERLAP.md`.
