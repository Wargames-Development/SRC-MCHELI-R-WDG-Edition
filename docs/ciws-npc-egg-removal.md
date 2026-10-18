# NPC spawn egg removal — 2026-10-16

Only `mcheli:gunner_aa` remains as a spawn egg in the gunner tab. The NPC egg
`mcheli:spawn_npc` is removed from registration, commands and creative item lists.
Its Forge missing-item mapping is explicitly ignored on old-world load, discarding
old stacks instead of blocking that world's startup for the removed item.

## Changes and review

| File | Reason |
| --- | --- |
| `src/main/java/mcheli/MCH_MOD.java` | Remove NPC egg registration, field, import and names; handle its removed item mapping |
| `src/main/java/mcheli/mob/MCH_ItemSpawnNPC.java` (deleted) | Remove the now-unused egg class |
| `src/test/java/mcheli/mob/MCH_GunnerEngagementTest.java` | Extend the existing old-world regression to the NPC egg and same-named unrelated blocks/other-mod items |
| `docs/ciws-aa-gunner.md`, this document | Current egg availability, compatibility, validation and installation |

Review verdict: acceptable. NPC entities are still used by the tech-tree village
system and existing saves, so their entity ID, renderer and behavior are retained.
This change only removes the requested item. No packets, NBT fields or remaining
registry names changed. The CIWS ray-trace fix is included unchanged.

## Validation

Java 8 `gradlew.bat compileJava test jar reobf --offline` passes. All **118 tests**
pass, with zero failures/errors/skips. The mapping test confirms only the six
removed egg mappings are discarded; CIWS AA, unrelated items, blocks and other
mods retain normal missing-mapping handling. `git diff --check` passes.

Two stale generated NPC-egg files were removed from `build/sources/java` and
`build/classes/java/main` before rebuilding. The resulting jar and update ZIP were
checked to contain no `mcheli/mob/MCH_ItemSpawnNPC.class`. The ZIP's 835 entries
were each hash-checked against the reobfuscated jar. Build number remains 180.
No commit, push, release upload or live installation was performed.

## Compiled update and in-game checks

Use `build/ciws-npc-egg-removal/ciws-code-update-X180-no-npc.zip`. This supersedes
the earlier ray-trace-only ZIP and includes that fix plus the NPC egg removal.
Archive hashes and source jar location are in the adjacent `manifest.json`.

1. Stop client/server, back up their current `mcheli/` code folder and `mcmod.info`
   outside the active mods directory, and replace those entries in the extracted
   MCHR installations with this ZIP. Preserve the existing `assets/` folder.
2. Restart both sides. Confirm the gunner tab contains only **Gunner [CIWS AA]**,
   `/give @p mcheli:spawn_npc` fails as an unknown item, and the CIWS egg still mounts
   a compatible gun. A test world containing old NPC egg stacks should load and
   discard them. Live saved-world and client inventory checks remain manual.
