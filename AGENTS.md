# Wargames Forge 1.7.10 Coding Guide

This guide applies to Java 8 Minecraft Forge 1.7.10 mods developed for the
Wargames server.

It has two parts:

1. **AI Coding Rules** — the ground rules for every AI-assisted code change.
2. **Coding Workflow** — a Codex/ChatGPT-friendly process for implementation,
   review, fixes, testing, and handoff.

Project-specific architecture, conventions, and design documents take priority
when they are more restrictive than this general guide.

---

## Part 1 — AI Coding Rules

AI assistants must work in small, reviewable steps. Implement only the current
task or milestone. Do not add future systems, speculative abstractions, or
unrelated cleanup.

Before editing, inspect the existing code, package structure, naming, style,
build configuration, and relevant Forge 1.7.10 patterns. Follow established
project architecture instead of inventing a replacement.

Prefer boring, readable Java 8. Avoid unnecessary managers, factories,
interfaces, abstract classes, reflection, complex generics, or deep inheritance.
Create abstractions only when there is an immediate need or clear duplication.

Keep patches small by default:

- 3 to 5 files changed maximum unless the task reasonably requires more.
- 150 to 250 new lines maximum unless the task reasonably requires more.
- No broad rewrites.
- No formatting entire unrelated files.
- No behavior changes outside the requested task.

### Java and Forge compatibility

- Use Java 8 syntax and APIs only.
- Use Minecraft Forge/FML 1.7.10 and the mappings already configured by the
  project. Do not assume APIs from newer Minecraft or Forge versions exist.
- Inspect the codebase or the actual dependency sources before using an
  uncertain API, method name, event, field, or lifecycle hook.
- Preserve existing registration order and compatibility-sensitive identifiers
  unless the task explicitly requires a migration.

### Client and server ownership

The server is authoritative for gameplay. It owns persistent and shared state
such as movement, position, health, damage, inventories, weapons, ammunition,
reloads, cooldowns, hit detection, permissions, spawning, destruction, and any
result that affects other players or the world.

The client owns presentation and local interaction such as input polling, HUD,
camera behavior, rendering, sounds, particles, interpolation, and cosmetic
prediction. Client prediction must never become the only source of authoritative
gameplay state.

Client packets express player intent, not trusted results. Validate the sender,
permissions, distance, current state, limits, timing, and packet contents before
changing server state. Reject malformed or impossible requests safely.

Sync only the state the client needs to present the server-owned result. Avoid
sending or saving duplicate sources of truth that can drift apart.

### Side separation

- Client-only Minecraft classes, rendering code, OpenGL calls, input handling,
  HUD code, sounds, and particles must remain behind the project's established
  client-side boundary.
- Common and dedicated-server code must not directly load client-only classes.
- Use the project's existing proxy, side-specific entry point, event handler,
  or abstraction pattern instead of introducing a second side-separation system.
- A side check does not make an unsafe client-only import safe if the dedicated
  server can still load the containing class.

### Architecture and data ownership

- Keep one clear source of truth for each piece of gameplay state.
- Rendering, GUI, adapters, wrappers, and packet objects must not own gameplay
  state merely because they display or transport it.
- Keep persistent state on the server and serialize it through the project's
  established NBT, world data, capability-like, or definition system.
- Separate input, simulation, networking, persistence, rendering, definitions,
  and presentation when they have different responsibilities.
- Do not create one class per content item when shared behavior plus data-driven
  definitions is sufficient. Use unique classes only for genuinely unique
  behavior.
- Avoid forcing chunk loads, broad entity scans, or per-tick allocations unless
  the feature explicitly requires them and the cost is justified.

### Compatibility boundaries

Preserve existing behavior unless the task explicitly changes it. Do not rename,
renumber, reorder, or silently reinterpret compatibility-sensitive values such
as:

- packet IDs or packet field order,
- NBT keys,
- configuration keys,
- registry names,
- entity IDs,
- dimension IDs,
- saved-data formats,
- JSON definition fields,
- network protocol behavior.

When a compatibility change is required, make the migration explicit and report
its effect on existing worlds, clients, configs, or content packs.

### Code quality

Comments should explain non-obvious decisions, Forge 1.7.10 quirks,
multiplayer authority, compatibility constraints, or unusual math. Do not add
comments that merely restate the code.

Handle invalid data at boundaries. Prefer safe defaults, clear rejection, or an
explicit error over a delayed null pointer or corrupted persistent state.

When uncertain, inspect first. If uncertainty remains, choose the smallest
change that preserves current behavior. Do not guess APIs or architecture into
existence.

Default rule: when uncertain, choose less code.

### Validation

Use the lightest command that meaningfully validates the change.

- `./gradlew compileJava` is the default validation for production Java changes
  because it catches compilation errors without performing the entire packaging
  pipeline.
- Run `./gradlew test` when relevant tests exist or were added.
- `build`, `assemble`, `jar`, `reobf`, client/server launch tasks, or other
  project tasks may be run when they provide useful validation or the user asks
  for them.
- Do not avoid a useful validation task solely because it increments a build
  number or modifies generated build metadata. Report any such side effect.
- Do not claim a command passed unless it was actually run successfully.

### Completion report

After editing, report:

1. files changed,
2. why each file changed,
3. behavior added or changed,
4. how to test it in-game,
5. validation commands run and their results,
6. risks, assumptions, compatibility effects, or deferred work.

Do not make git commits, push branches, publish releases, or upload artifacts
unless the user explicitly asks.

If the requested task conflicts with these rules or with established project
architecture, explain the conflict and choose the safest bounded implementation.

---

## Part 2 — Coding Workflow

This workflow is designed for Codex, ChatGPT, and similar coding assistants. The
roles below are review passes, not tool-specific permanent agents. One session
may perform every role sequentially, or the lead assistant may delegate a role
when its environment supports agents or parallel work.

### Role roster

| Role | Purpose | Writes code? |
|---|---|---|
| **Lead** | Defines scope, inspects context, controls the workflow, and owns the final result | Yes, for direct or trivial changes |
| **Implementer** | Makes one bounded change using the existing architecture | Yes |
| **Reviewer** | Performs a read-only correctness, compatibility, and scope review | No |
| **Fixer** | Applies the smallest safe correction for confirmed findings | Yes, minimally |
| **Tester** | Adds focused tests or defines a precise manual regression checklist | Tests only |

To start a coordinated task, use a prompt such as:

> Follow AGENTS.md. Act as the Lead and implement `<task>` using the bounded
> implement → review → fix → test → verify workflow.

### Lead protocol

The Lead:

1. **Defines scope.** Restates the requested behavior, the files or subsystem
   likely involved, and what is out of scope. Resolve ambiguity from existing
   code and documentation first; ask the user only when a material product or
   compatibility decision cannot be inferred safely.
2. **Inspects before editing.** Read relevant source, build files, definitions,
   documentation, and nearby patterns before proposing architecture or code.
3. **Chooses workflow depth.** Use the full pipeline for meaningful behavior
   changes. Handle genuinely trivial edits directly while still validating and
   reporting them.
4. **Runs roles in order:** implement → review → fix if needed → test → verify →
   summarize.
5. **Serializes edits.** Only one writing pass modifies overlapping files at a
   time. Do not let implementer, fixer, or tester edits race each other.
6. **Keeps scope bounded.** Pause large rewrites, new dependencies, protocol
   changes, save-format migrations, or build-system changes unless they are
   required by the task. Surface optional large changes separately.
7. **Caps review loops.** Use at most two reviewer-to-fixer cycles for one task.
   If critical problems remain, stop expanding the patch and report the blocker.
8. **Verifies independently.** Inspect the final diff and run appropriate
   validation rather than relying only on a prior role's summary.
9. **Summarizes plainly.** Explain what changed, why, how to test it, validation
   status, compatibility impact, and remaining risks.

### Review findings

Use stable finding IDs so later passes can reference exact issues:

- `C#` — critical correctness, security, corruption, or dedicated-server issue.
- `B#` — likely bug or regression.
- `D#` — design or maintainability concern within the current scope.
- `S#` — style or clarity issue.
- `T#` — missing test or unverified behavior.

Reviewer verdicts:

- `NEEDS FIXES` — one or more `C#` or significant `B#` findings must be fixed.
- `ACCEPTABLE WITH MINOR ISSUES` — behavior is usable; remaining findings are
  non-critical and may be deferred.
- `ACCEPTABLE` — no meaningful issue found within the reviewed scope.

The Fixer must cite the finding IDs it addressed. The Tester should use `T#`
findings as its test backlog.

### Handoff format

Each role should finish with a compact structured report. When roles are
performed by separate agents or sessions, pass the full relevant report to the
next role rather than a vague paraphrase.

#### IMPLEMENTER REPORT

- Files changed
- Behavior implemented
- Important decisions and assumptions
- Validation run
- Known risks or deferred work

#### REVIEWER REPORT

- Scope reviewed
- Findings with stable IDs and file/line references
- Dedicated-server, networking, persistence, and compatibility checks
- Verdict

#### FIXER REPORT

- Finding IDs addressed
- Minimal changes made
- Validation run
- Remaining findings or risks

#### TEST REPORT

- Automated tests added or run
- Manual in-game scenarios
- Expected results
- Untested risks

A role that encounters a material design decision should list the available
options and their consequences rather than silently choosing a large new
direction.

### Global workflow boundaries

Every role must follow these boundaries:

- Preserve existing behavior outside the requested change.
- Use Java 8 and the project's actual Forge 1.7.10 APIs only.
- Keep client-only classes out of common and dedicated-server class-loading
  paths.
- Keep authoritative gameplay state server-owned; presentation remains
  client-owned.
- Treat incoming client data as untrusted intent and validate it server-side.
- Preserve packet, registry, NBT, config, save, and definition compatibility
  unless migration is explicitly part of the task.
- Inspect before guessing.
- Prefer the smallest correct patch.
- Use `./gradlew compileJava` as the default Java validation, while allowing
  fuller build or packaging tasks when useful.
- Do not commit, push, release, or publish unless explicitly requested.

### Sample workflow A — implementing a vehicle-mod feature

Example: *"Stage 1 of the projectile spec: add a data-driven
ProjectileDefinition."*

1. **Lead** limits scope to the definition and loading behavior only—no physics
   or guidance—and inspects the existing vehicle-definition and Gson patterns.
2. **Implementer** adds `ProjectileDefinition` in a small patch, follows the
   established loader conventions, runs `compileJava`, and returns an
   IMPLEMENTER REPORT.
3. **Reviewer** checks only the changed files against the specification,
   Java 8/Forge 1.7.10 constraints, malformed-input behavior, and multiplayer
   ownership. Example findings: `[B1]` unsafe null defaults and `[T1]` missing
   malformed-JSON coverage.
4. **Fixer** receives the full review and corrects `[B1]` with the smallest safe
   change.
5. **Reviewer** rechecks the fix diff. If acceptable, the task proceeds.
6. **Tester** adds a focused test when the project already supports it. If a new
   test framework or build dependency would be required, the Lead reports that
   separately rather than silently expanding the task. Otherwise the Tester
   supplies a precise manual loading checklist.
7. **Lead** inspects the final diff, runs the appropriate compile/test/build
   commands, and gives the completion report.

### Sample workflow B — debugging a vehicle-mod failure

Example: *"The tank falls through terrain after driving off a cliff."*

1. **Lead** gathers the available reproduction steps, logs, definitions, and
   relevant physics code. No speculative rewrite is started.
2. **Reviewer** performs a diagnosis-only pass and reports candidate causes with
   evidence, such as `[B1]` a terrain probe shorter than the maximum fall
   distance per tick.
3. **Lead** checks that the diagnosis explains the reported behavior and records
   any remaining uncertainty.
4. **Fixer** receives only the confirmed finding, applies the smallest safe fix,
   runs `compileJava`, and reports regression risks such as slope or stair
   behavior.
5. **Tester** adds a regression test if the logic is isolated enough; otherwise
   it supplies an in-game checklist covering the cliff case, flat terrain,
   slopes, stairs, multiplayer observation, and dedicated-server behavior when
   relevant.
6. **Lead** verifies the final diff and summarizes the cause, fix, unchanged
   behavior, validation, and residual risk.

### When not to use the full pipeline

The full workflow is unnecessary for a typo, comment correction, isolated JSON
value change, or obvious one-line guard. The Lead may make such changes directly,
then inspect the diff, run proportionate validation, and provide the normal
completion report.

Use the full pipeline when a change affects gameplay authority, networking,
persistence, registration, rendering architecture, performance-sensitive tick
logic, multiple systems, or more than a very small patch.
