# Laser designation box and range in thermal view

Laser graphics previously rendered during RenderWorldLast, before FMUR's
thermal image replaced the scene at the HELMET overlay. A confirmed designation
could therefore guide normally while its box and range were covered. Handheld
laser range text also inherited disabled texturing from the outline drawing.

MCH_RenderGPSPosition now captures the world model-view and projection matrices
once per ordinary world pass. It draws the aircraft designation HUD and handheld
laser marker at RenderGameOverlayEvent.Post ALL, after thermal compositing.
The saved matrices retain the world camera's projection, including zoom and
roll, without replacing camera or guidance state. The overlay consumes the
captured view once, checks world identity, respects F1, ignores thermal heat and
particle replay, and restores matrix mode, matrices and GL attributes. Existing
GPS-only marker drawing stays in its original world pass.

MCH_RenderLockBox keeps its confirmed-target requirement, distance projection
and jamming indication. It no longer uses the old magenta thermal-world color
path. Both aircraft and handheld renderers enable font texturing before drawing
range. Aircraft range labels include meters and also appear below ten meters.

No common/server class, packet, targeting, weapon definition, terrain validation,
laser ownership or gameplay behavior changes. FMUR is still optional; the same
HUD pass works in ordinary view. Only MCHR source needs updating for this fix.

Changed files: render/MCH_RenderGPSPosition.java (post-thermal laser overlay),
weapon/MCH_RenderLockBox.java (readable range and color), and this note.

Validation: `gradlew.bat compileJava test --offline`; the focused existing laser,
marker projection, LOD-targeting and laser-intent tests also pass. These checks
do not render an OpenGL frame; live visual verification remains necessary.

With an updated MCHR client, test:

1. Select an aircraft laser-guided weapon, activate thermal view and right-click
   to designate known terrain. Confirm the box and meter range remain visible.
2. Move and zoom the camera, roll the aircraft and switch thermal palettes.
   The box should stay aligned with the designated point and range remain legible.
3. Repeat at close range and beyond normal terrain/entity render distance with
   trusted cached terrain. Unknown terrain must still withhold designation.
4. Repeat with a handheld laser weapon; check its box/cross and LZR range.
5. Release designation, change weapon or mode, and switch worlds. No stale
   marker should remain. Check F1 hiding, normal view, GPS markers, and jamming.

The source has not been packaged or installed into the supplied Prism instance.
