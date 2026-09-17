# Development Cockpit and Acquisition Planner Design

## Goal

Turn the DUBL development screen into a self-contained character-building cockpit on both Desktop and Android. A player should be able to understand what an entry does, why it is or is not available, navigate prerequisites and downstream unlocks, plan a build, and optionally acquire every resolvable missing prerequisite in one reviewed atomic action without opening the rulebook.

## Shared behavior

All requirement analysis, acquisition planning, cost calculation, OR-path selection, and atomic application live in `shared`. Android and Desktop only render the shared plan and invoke the same `DevelopmentApplication` operation.

A plan may contain attribute increases, skill-rank increases, development-rank purchases, unresolved/manual requirements, and explicit OR choices. The planner simulates the resulting character while walking prerequisites recursively so duplicate prerequisites are only purchased once. By default an OR requirement chooses the cheapest fully resolvable branch; the UI may override a choice and recompute the plan.

Unsupported/manual/story requirements are never guessed. They are surfaced as unresolved and prevent automatic application. Normal manual/GM override purchase remains available through the existing workflow.

`applyAcquisitionPlan` is atomic: either the complete currently validated plan is committed in one character mutation or the character is unchanged. The operation re-plans against current state before applying to avoid stale previews.

## Development cockpit

The browser exposes `Все`, `Можно взять`, `Почти доступно`, `Взято`, and `План`. Desktop uses a responsive two-column card browser with a persistent inspector at wide widths; Android keeps a single-column browser and a bottom-sheet inspector. Both show the same information and actions.

Cards emphasize name, current/max rank, cost, availability state, and a compact benefit preview. Details move to the inspector.

The inspector shows full benefit/rule text, current rank and price, parsed requirement checks, what is still missing, downstream `Открывает` links, local override controls, build-plan toggle, and auto-acquisition actions.

## Build plan

A per-screen build plan can contain multiple target development entries. The shared planner computes the merged prerequisite chain and cost against the current character. Planned targets are visually distinct and can be filtered. The plan is intentionally presentation state in this iteration; canonical character state changes only when the player confirms a purchase.

## Acquire everything

For a selected target the UI offers:

- `Добрать требования` — acquire every resolvable prerequisite but not the target.
- `Добрать и взять` — acquire prerequisites plus the next target rank.

Before mutation, a confirmation surface lists every change, XP/AP totals, remaining budgets, selected OR paths, and unresolved requirements. If an OR branch has alternatives, the user can pick a different branch and the preview is recalculated. The confirm action is enabled only for a fully resolved, affordable plan. Manual purchase/GM override remains separate.

## Non-goals

This iteration does not create a general natural-language rules engine, auto-learn spells, choose custom specializations on the user's behalf, or silently satisfy story/GM requirements.
