# Survival vehicle pickup

The server's existing `config/mcheli.cfg` now includes:

```ini
EnableSurvivalVehiclePickup = false
```

With this default, vehicles without an override (including aircraft, tanks,
helicopters, and static weapons) cannot be removed as items by survival attacks.
Set it to `true` to enable pickup by default instead.

Any vehicle `.txt` definition can override that default in either direction:

```ini
SurvivalPickup = true
```

Add this line to the tripod TOW launcher's definition to allow survival players
to pick it up and place it again. Leave the line out of the A-10, Abrams, and
other large vehicles to inherit the disabled default. `SurvivalPickup = false`
explicitly denies pickup even when the global default is enabled. These content
pack definitions are not included in this source checkout, so the TOW definition
must be updated in the installed content pack.

Existing restrictions still apply: `PreventingBroken = true` disables all
survival pickup, and `BreakableOnlyPickaxe = true` requires a pickaxe. The normal
rider and multiplayer attack checks remain in place. A denied pickup attack
follows ordinary combat damage rules; this setting does not make vehicles
invulnerable.

Creative removal bypasses the survival settings. Existing
`DropItemInCreativeMode` and sneaking behavior still determine whether creative
removal drops a vehicle item.

Pickup no longer adds damage or forces an engine shutdown. With the existing
`ItemDamage = true` default, placing the dropped item restores its previous HP.
`ItemDamage = false` retains its usual behavior of not saving damage on new
drops. Previously damaged pickup items retain their saved damage for backward
compatibility; they are not automatically repaired. Fuel and ammunition use the
existing item serialization.

Restart after editing server config/content definitions; distribute updated
content definitions to clients as usual. Definition reload also resets a removed
`SurvivalPickup` override to inheritance.

## Manual regression checklist

1. With default config and no definition overrides, attack empty A-10 and Abrams
   vehicles in survival. Neither becomes an item; ordinary damage still applies.
2. Give the TOW `SurvivalPickup = true`. Pick up and place it in survival at full
   health, then repeat with a damaged launcher. With `ItemDamage = true`, HP must
   remain unchanged across each pickup/placement. Verify fuel/ammunition remain
   consistent with their existing persistence settings.
3. Enable the global default. Confirm a vehicle without an override can be
   picked up, while one with `SurvivalPickup = false` cannot.
4. Test `PreventingBroken = true` and `BreakableOnlyPickaxe = true` separately.
   Confirm the former blocks survival pickup and the latter requires a pickaxe.
5. In creative, remove vehicles with both overrides and both global defaults.
   Confirm removal still works; check sneaking and `DropItemInCreativeMode` for
   the expected item drops.
6. Repeat pickup/placement on a dedicated server with another player watching.
   Confirm exactly one dropped item and no leftover vehicle entity. Check an
   occupied vehicle against the existing rider/pickaxe rules.
7. Reload a definition after removing its override, and restart with an older
   config missing the new key. Confirm both inherit the disabled default.
