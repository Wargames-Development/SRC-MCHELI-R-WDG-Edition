# Building and updating MCHR

Use Java 8 and run `gradlew.bat build` on Windows or `./gradlew build` on Linux.
Add `--offline` when all dependencies are already cached. The verified code jar
is written to `build/libs/MCHELI-R-WDG-Edition-<version>.jar`.

`jar`, `reobf`, `assemble`, and `build` all reach the same production packaging
step. It verifies the remapped Minecraft references and the mod metadata before
copying the jar into `build/libs`. Development intermediates stay in
`build/intermediates/reobf`.

## Build numbers

- A successful production packaging invocation increments `modBuildNumber` once.
  The jar filename, `@Mod` version, and `mcmod.info` use that same new number.
- Compilation, tests, configuration/help, dry runs, and development launches do
  not increment the counter. Failed packaging does not persist its candidate
  number or replace a previously verified output jar.
- Requesting several packaging tasks together still increments only once.
- `-PmodBuildNumber=<number>` pins a deliberate build version without changing
  the saved counter.
- Run development launches and production packaging in separate invocations.

## Reported startup crash

The supplied server report fails in `MCH_Lib.init` with `NoSuchFieldError: air`.
The Java source correctly uses `Material.air`; ForgeGradle must convert that to
`Material.field_151579_a` for production Forge 1.7.10. Deploying compiled
development classes skips that conversion.

The packaging check examines actual field instructions in `MCH_Lib.class`,
requires the expected SRG material names, and rejects the development names.
It also parses `mcmod.info` and checks its mod ID and version. JSON escape
sequences are preserved during resource processing.

The crash report identifies an unpacked MCHR distribution. When updating that
distribution, take the `mcheli/` classes, `mcmod.info`, and updated resource files
from the verified jar, retaining the existing content-pack assets. Do not copy
from `build/classes` or install a second MCHR copy alongside the unpacked mod.
This source repository does not contain the full vehicle/model content pack.

## Verification performed

- Full `build --offline`: 182 tests passed with no failures, errors, or skips.
- Successful `build` and direct `jar` runs each advanced the counter once.
- `compileJava test --offline` and a dry-run build preserved the counter.
- Excluding `reobf` was rejected without changing the counter or verified jar.
- A controlled no-op replacement for reobfuscation left MCP names in the
  intermediate jar; verification rejected it and preserved the counter/output.
- Retrying the normal build succeeded with an up-to-date intermediate jar.

Validation produced X187, X188, and X189 from the initial X186 counter; X189 is
the latest verified output. No world, packet, or content-definition migration
is required. Clients and servers must use matching updated MCHR builds, as
already enforced by the mod's version check.

The target Crucible server was not launched here. After updating its existing
MCHR installation, confirm that startup passes `MCH_Lib.init`, the reported
version matches the jar, and vehicles load and can be mounted normally.
