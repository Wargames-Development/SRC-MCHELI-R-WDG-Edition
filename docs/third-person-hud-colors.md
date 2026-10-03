# Third-person HUD colors

## Cause and change

The vehicle overlay inherited fixed-function OpenGL lighting from world rendering.
World-space marker renderers can leave lighting enabled. That lighting overrides
the intended colors of untextured HUD geometry, making yellow durability and
throttle bars gray or dark and removing contrast from the exit-progress bar.

`MCH_ClientCommonTickHandler.onRenderGameOverlayEvent` now renders the existing
HELMET overlay pass with lighting disabled and the initial color set to white.
The pass includes vehicle HUDs, the exit-progress indicator, and the third-person
crosshair. A saved OpenGL attribute scope restores incoming enable, lighting,
color, blend, and depth state in a `finally` block.

This is a client presentation change. It changes no packets, definitions, saved
data, or server gameplay. The build number remains 170. No packaged or installed
mod was updated by this change.

## Review and validation

- Reviewed the overlay scope, existing HUD primitive drawing, exit-indicator
  drawing, and world-marker lighting behavior. The change stays in the existing
  client-only class and preserves the rendering order.
- Java 8 `gradlew.bat compileJava test --offline`: passed; 48 tests, zero failures
  or errors.
- `git diff --check`: passed.
- An offscreen LWJGL OpenGL probe called the production HUD rectangle drawing
  method with incoming lighting enabled. It reproduced gray `(10, 10, 10)`, then
  produced the requested yellow `(255, 224, 0)` inside the new attribute scope.
- The same probe checked the exit indicator's black-background/white-fill quad
  drawing sequence: fill `(220, 220, 220)`, background `(0, 0, 0)`. Incoming
  lighting was restored afterward, with no OpenGL error.

The probe is a rendering check, not an in-game camera test. Live Minecraft and
compatibility with other mods' overlay handlers remain unverified.

## In-game verification

Rebuild and install the updated client, then:

1. Enter a vehicle and compare first person, rear third person, and front third
   person with F5. Durability and throttle geometry should retain its configured
   color in every view, matching the HUD text.
2. Hold the exit key in each view. The bright fill should advance visibly over
   the dark background. Release early and retry to check cancellation/reset.
3. Repeat while moving, aiming, and displaying world-space targeting markers.
4. Check scopes, night vision, vehicle menus, and the normal world view for
   unintended lighting or color changes.
