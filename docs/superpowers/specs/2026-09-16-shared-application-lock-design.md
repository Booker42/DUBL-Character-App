# DUBL Shared Application Lock Design

## Goal

Make shared/commonMain the only state-changing application boundary for Android and Compose Desktop while preserving the current DUBL 3.69 behavior. Add typed golden scenarios that prove deterministic application behavior without claiming that every current behavior is already rulebook-correct.

## Scope

This slice does not redesign either UI, re-audit DUBL 3.69, repair known rulebook migration gaps, or build a generic ruleset/module engine. It establishes the application boundary those later changes will use.

## Architecture

`DublApplication` owns the internal `CharacterSession` and `CharacterExtrasSession` and exposes focused capability objects: character/resources, skills, development/Chi, magic, equipment, and sheet extras/grouping. Platform code may read application state and invoke capability methods, but may not obtain a raw session, arbitrary character transform, or arbitrary extras transform.

`CharacterSession`, `CharacterExtrasSession`, and their raw transform methods are implementation details of shared commonMain. Android `CharacterController` and Desktop `DesktopAppState` are observable platform adapters over `DublApplication`; they synchronize Compose state after each application call.

## Hard-lock contract

- Android/Desktop presentation code must not import or instantiate `CharacterSession` or `CharacterExtrasSession`.
- Android/Desktop must not expose `updateActive { ... }`, `mutate { ... }`, `updateExtras { ... }`, or repository-level extras writes as mutation escape hatches.
- Every state-changing operation reachable from current UI must have a typed shared application method.
- Pure shared rules/queries may remain callable directly; the lock applies to application state changes.
- Portrait byte/file selection remains platform-specific, but storing/removing the resulting portrait reference goes through sheet application actions.
- One-step Undo semantics for the currently undoable sheet operations are owned by `DublApplication`; Android/Desktop may decide how to present the undo affordance but may not implement the reverse mutation themselves.

## Deferred application concerns

- Dice RNG injection belongs in shared application when roll execution itself becomes an application command. Current roll choice/effect calculations remain deterministic shared rules, so this slice does not add an unused RNG abstraction.
- A richer `ApplicationResult` error taxonomy is deferred until current boolean/null mutation APIs expose enough domain-specific rejection reasons to avoid inventing meaningless generic errors. The hard lock already ensures both platforms receive those results from one shared implementation.

## Golden scenarios

Golden scenarios are typed Kotlin programs against `DublApplication`. They use deterministic ID factories and in-memory stores and assert canonical snapshot/extras state after a sequence of user-level operations. They establish behavior parity and regression protection, not rulebook authority. If a later rulebook audit corrects behavior, the shared implementation and corresponding golden expectation change together.

Initial scenarios cover:

1. profile/creation/resources and persistence normalization;
2. skills and preferred roll attribute;
3. development/Chi mutation;
4. magic and equipment mutation;
5. sheet conditions/grouping/custom conditions;
6. roster switching and per-character extras isolation.

## Future compatibility

The application boundary consumes the existing persisted ruleset identity and must not assume that presentation code owns DUBL-specific mutation semantics. Future ruleset composition may introduce module/capability resolution behind this boundary, but this slice introduces no generic module loader or scripting language.
