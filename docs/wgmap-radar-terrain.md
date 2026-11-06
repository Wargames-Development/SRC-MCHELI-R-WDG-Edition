# WGMap stored terrain for radar — 2026-11-02

MCHR radar now supplements its existing loaded-terrain evidence with WGMap's
trusted server-side stored occlusion cells for unloaded chunks. The server keeps
the existing multiple rays toward each target's bounds, blocked-contact packet,
tracking validation and RWR behavior. The client depth buffer is not authoritative.

`WGMapRadarServerBridge` is loaded only behind `Loader.isModLoaded("wgmap")`.
It uses public `RadarTerrainAccess` API 1 and returns no extra evidence if WGMap
or that API is absent. No reflection, new dependency, packet change or save-format
change is introduced. The existing compile-only dev jar must contain the new API;
alternatively build WGMap first, then compile/test this repository with
`-PwgmapDevelopmentClasses=<absolute WGMap build/classes/java/main>`.

Live loaded chunks take precedence over historical snapshots. Loaded lookup uses
the server's loaded table and owning physical world height, never a chunk-loading
method. Missing or pending stored data is not an obstruction. Cold queries can
queue bounded asynchronous reads; they need not preload the full radar circle.
The existing five-tick radar evidence cache can delay reevaluation after adoption.

Stored full occluding cells use WGMap's existing opaque classification, including
opaque vegetation. Loaded-block MCHR leaf exclusion remains unchanged. Partial
slab states do not count as full-cell obstruction in this sampler.

WGMap's new `config/wgmap/memory.cfg`, category `memory`, sets
`occlusionHeapPercent` (default 8%, range 1..20%, restart required). It controls
local cache allowances/headroom from Java Xmx, shares the integrated-server
budget, and reduces retention under sustained pressure. It does not expand the
default client 750+250-block visual window or change `RadarMaxTargetRange`.
See WGMap `docs/HANDOFF_RADAR_OCCLUSION_AND_MEMORY.md` for formulas and limits.

Validation: Java 8 wrapper compilation against WGMap development classes passed;
29 focused radar/optional-integration/track-packet/laser tests passed. WGMap's full
1,198-test suite and repository boundary checks passed. No full-pack or actual
dedicated-server launch is claimed.

Runtime acceptance: put emitter and target 2,500 blocks apart with an unloaded
stored mountain between them; confirm eventual contact/track suppression without
loading mountain chunks. Expose part of the target or fly above the mountain to
restore detection. Repeat with pending/missing terrain, multiple radar emitters,
extended height, and WGMap absent/older. Measure cold-query delay, tick time and
disk/cache churn. No published artifacts or commits were created.
