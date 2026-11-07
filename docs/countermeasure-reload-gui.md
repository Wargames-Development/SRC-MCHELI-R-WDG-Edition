# Countermeasure reload page and visual audit

## Result

The vehicle R menu has one extra **Countermeasures** page after its existing
weapons. It uses the same Reload button, page arrows, item requirement position
and stock-count area. Every vehicle has the page; unsupported vehicles show
**Not equipped** with Reload disabled. Weaponless vehicles open directly on it.
Inventory and Mod Options stay at fixed footer positions across every page and
vehicle. The selected page survives resizing.

The existing `mcheli:countermeasure_reload` item is named **Countermeasures**.
Its existing recipe uses an iron ingot, paper and gunpowder. One survival item
fully refills both enabled flare and chaff stores, including when one is already
full, independent of capacity or remaining stock. Full stores consume nothing;
creative mode consumes nothing. Ground/supply and pilot authorization rules remain.
This keeps the existing server-owned stores, saved data and container action.
Flare/chaff deployment still retains its distinct heat/radar functionality.

## Files

- `MCH_AircraftGui.java`: the extra page, shared reload action, fixed footer,
  bounded names with full-name hover text, wrapped tooltips and consistent labels.
  Item rendering now restores lighting, depth, colour and GUI Z state; the old
  helper could otherwise leave those settings changed for subsequent GUI content.
- `MCH_I18n.java`: English/Chinese page and unsupported-equipment labels.
- `MCH_CountermeasureReloadTest.java`: adds the one-store-already-full cases to
  the existing server refill, cost, authorization and container-action scenarios.
- `tools/CountermeasureGuiAudit.java` and `countermeasure_gui_audit.gradle`:
  reproducible offscreen audit of the actual GUI with vanilla buttons/font and
  the supplied Event 2.0.14 pack's `gui.png`. Only item icons bypass the Forge
  atlas, drawing the same source PNG directly; normal vanilla count overlays run.

## Verification

From this repository, Java 8:

```powershell
.\gradlew.bat -I tools/countermeasure_gui_audit.gradle compileJava test auditCountermeasureGui --offline '-PguiAuditPack=C:/Users/nateb/AppData/Roaming/PrismLauncher/instances/Wargames Event 2.0.14/minecraft' '-PwgmapDevelopmentClasses=C:/Users/nateb/OneDrive/Documents/GitHub/WGMap/build/classes/java/main'
```

Compilation/full tests passed: **209 passed, 2 optional gunner tests skipped**.
The WGMap override supplies the existing unrelated development API dependency.
All **19 offscreen renders** were visually reviewed: standard weapons, combined
countermeasures, full stores, missing item, flare-only, chaff-only, unsupported,
weaponless, large capacities/long names, 320x240, small-window tooltip wrapping,
full weapon-name tooltip, weapons without countermeasures, full inventory,
weaponless/unsupported, GUI scales 1/2/3/4/Auto, English and Chinese.
Assertions also check page wrapping, resize retention, button/slot bounds, fixed
footer coordinates and routing countermeasure reloads to the container action.

Screenshots: `build/countermeasure-gui-audit/*.png`; overview:
`build/countermeasure-gui-audit/audit-contact-sheet.png`. Full screenshots retain
tooltips outside the panel; `.panel.png` crops intentionally contain only the GUI.

Sequential review verdict: **ACCEPTABLE**. No item registration, packet layout,
container slot, recipe, NBT or save migration is required. No build-number change.

## Installation and live check

The supplied pack has an unpacked MCHR installation. Its Countermeasures item
texture is absent, while this repository already includes
`assets/mcheli/textures/items/countermeasure_reload.png`. Install updated code
**and resources** together when updating the pack; preserve its vehicle/model
content. This task did not modify the installed pack or a live server.

No game window was open. The audit exercised actual GUI drawing offscreen and
server reload logic through headless tests, not a running multiplayer pack.
After installing the updated mod on the matching client/server, mount a vehicle,
open R, cycle to Countermeasures, and reload with one item while both stores are
partially depleted. Verify both become full and the stack loses exactly one.
Repeat with one store full, no item, full stores, an unsupported vehicle and a
weaponless vehicle. Check ordinary weapon reloads and GUI resizing still work.
