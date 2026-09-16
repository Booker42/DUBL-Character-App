# DUBL KMP / Desktop 0.2 handoff

## Current target

The **DUBL 3.69 rulebook set is the source of truth for rules/content**. Android **0.6.2** remains the mature implementation/UX reference, while Desktop **0.2.0** is a real **Compose Desktop** frontend over the shared KMP application/domain layer. Do not resume feature work in the legacy portable/Swing shell; keep it only as a parity oracle/fallback until a Compose AppImage has been built and smoke-tested in an environment with Gradle/Maven network access.

Web/Wasm, server/accounts, and sync remain out of scope. The old PySide/Electron desktop project is not a source of rules, architecture, or layout.

## Compose Desktop status

Implemented and wired to shared state/persistence:

- `DesktopAppState` owns persistent desktop stores plus a shared `DublApplication`; raw `CharacterSession` / `CharacterExtrasSession` are internal shared implementation details. The catalog payloads physically live once in shared resources and Android reads those same files;
- six top-level workflows: Character Sheet, Skills, Development/Martial Arts/Chi, Magic, Equipment, Characters;
- persistent roster/active character and schema-8 character data with `dubl` / `3.69` ruleset identity plus schema-7 migration;
- persistent character-sheet extras/grouping;
- rule-aware rolls and skill effects;
- custom skills/resources/spells/schools/gear where Android supports them;
- destructive-action confirmation and custom-skill validation;
- native Compose portrait rendering with a desktop file chooser;
- no app-level horizontal scrolling; compact/normal/wide responsive policy remains shared.

## Shared Application Lock

Android and Compose Desktop now use the same public state-changing boundary: `shared/commonMain/.../application/DublApplication`. It exposes focused capabilities for character/resources, skills, development/Chi, magic, equipment, and sheet extras/grouping. Platform adapters may observe state and invoke typed application operations, but must not import raw sessions, expose arbitrary `DublCharacter` / extras transforms, or write extras repositories directly.

Typed Kotlin golden scenarios use deterministic IDs plus in-memory stores and assert canonical snapshot/extras outcomes. One-step recent-change Undo is also shared: platform UIs keep only presentation metadata while `DublApplication.undoLast()` owns the reverse mutation. Treat these as **behavior-parity locks**, not proof that every migrated DUBL 3.69 mechanic is rulebook-correct. A later rulebook audit may intentionally change a shared behavior and its golden expectation once, after which both platforms inherit the correction.

This is deliberately not a generic ruleset/module engine. The boundary is compatible with future ruleset/module composition, but current work remains DUBL 3.69 core behavior first.


## Semantic rulebook audit

The first semantic audit slice is now tracked in `docs/rulebook-audit/2026-09-16-core-skills-rolls.md`. Character Core / Derived Stats / Skills / Rolls were compared directly against the real DUBL 3.69 core DOCX. Confirmed corrections are made in shared rules only: roll load penalties, complete Fortitude/Initiative presets, two-skill synergy/assistance, negative current Health, and target-aware `1–1` critical-failure resolution. Ambiguous or context-dependent mechanics are explicitly deferred rather than guessed. This audit is separate from golden behavior parity.

Verification for this slice: 41/41 focused core/parity/release checks passed; the 60-file non-subprocess source/parity/import sweep passed 226 tests with 4 expected skips; the pure `SkillCheckRules` and roll-target harnesses executed successfully. Full `:shared:desktopTest :desktopApp:compileKotlin` remains a networked CI gate in this sandbox because `services.gradle.org` cannot resolve.

## Rulebook-first import status

The next architecture phase has started. Do not add another direct DOCX parser to Android/Desktop or silently copy a rule from the app back into the ruleset.

Implemented pipeline:

- deterministic DOCX -> Raw Source IR extraction preserving ordered paragraphs/tables, heading paths, original text, source SHA-256, and stable block IDs;
- multi-source qualified provenance (`core:...`, `melee:...`, `archmage:...`) with duplicate occurrences preserved rather than collapsed;
- source diagnostics for draft markers and identical/variant duplicate headings;
- explicit diagnostics/resolutions model where unresolved `error` blocks validation;
- bootstrap provenance coverage over the existing canonical runtime catalogs;
- compact reproducibility baseline with source hashes, structure counts, domain coverage, and diagnostic counts;
- first source-generated domain: 23 runtime conditions + 2 condition-related mechanics extracted from the core book, with a parity guard against `CharacterConditionId`;
- Linux release gate coverage for all synthetic importer/validator contracts without requiring the DOCX fixtures.

`validate_ruleset` currently guarantees structural integrity and that promoted executable domains have no unresolved `error`; it does **not** erase or supersede the full semantic rulebook audit. The known blocker/critical prose ambiguities must be converted into domain diagnostics/resolutions as those domains are promoted.

The full generated bundle belongs under ignored `build/rulesets/dubl-3.69`. The tracked `rulesets/dubl-3.69` directory contains only `config.json`, `resolutions.json`, `baseline.json`, and promoted small generated artifacts. The DOCX books are development inputs and are not app/runtime assets.

Current real-source baseline: 32,233 ordered blocks across three books (31,910 paragraphs, 323 tables, 3,018 headings). Bootstrap provenance currently links Development 713/796 uniquely (83 ambiguous), Chi 72/77 (5 ambiguous), Magic/Equipment 306/525 (219 ambiguous), and skill effects 259/283 (23 ambiguous, 1 missing). The remaining ambiguity is intentionally surfaced instead of auto-resolved.

Normal rebuild/check sequence is documented in `README.md`. Only run `check_baseline --update` after reviewing the source/import diff.

## Release architecture

Canonical Linux packaging is `packaging/linux/build-appimage.sh` -> `:desktopApp:createDistributable` -> AppImage. `.github/workflows/linux-appimage.yml` now builds Compose, not portable/Swing, and requires:

```text
:shared:desktopTest
:desktopApp:compileKotlin
```

before packaging. `packaging/linux/build-portable-appimage.sh` and `packaging/linux/portable-src` are legacy fallback/oracle code only.

## Verification already completed in this sandbox

- Cross-platform character transfer verification: `tools/tests/test_character_transfer.py` 5/5 passed, including an executable local Kotlin round-trip/rejection/future-schema harness; the current fast non-subprocess source/parity sweep is 234 passed / 4 skipped, and the focused Shared Application lock/golden/Desktop persistence/sheet group is 12/12 passed.
- Shared common model/data/state/application sources compile successfully with the available local Kotlin compiler; the only warning is the pre-existing deprecated legacy Magic power adapter.
- Project Gradle verification was attempted with `./gradlew :shared:desktopTest :desktopApp:compileKotlin --offline` and remains unavailable because the wrapper/bootstrap cannot resolve `services.gradle.org` in this sandbox (`curl: (6) Could not resolve host: services.gradle.org`); networked CI remains the authoritative project-toolchain compile gate.
- Shared Application hard-lock/source regression sweep: 229 passed, 4 skipped across the fast non-compiler test set after platform migration;
- hard-lock + typed-golden source contracts: 6/6 passed;
- typed Shared Application golden coverage is now 17 application-level scenarios in `shared/commonTest`; the original local golden harness executed the first 15 successfully, and `CharacterTransferHarness` separately executes the new transfer round-trip/rejection/forward-schema behavior; the authoritative project-toolchain execution remains `:shared:desktopTest` in CI;
- current golden expansion verification: 16/16 focused hard-lock/golden/release source tests and 212 passed / 4 skipped in the fast non-subprocess parity suite;
- Gradle/Compose compile remains unavailable in this sandbox because `services.gradle.org` DNS resolution is blocked; the networked CI compile/AppImage gate remains authoritative.
- Shared Application patch verification: `git apply --check` and real apply both succeed against the untouched rulebook-compile-hotfix source ZIP; the applied tree matches the generated source snapshot byte-for-byte aside from pre-existing cache directories. Focused lock/golden/release tests on the applied copy: 26/26 passed.
- Exact offline Linux parity-release test list: **102/102 passed** when run in bounded groups (87 source/parity checks plus Development/Chi 2/2, Magic 2/2, Equipment 4/4, rules-boundary 6/6, and desktop persistence 1/1);
- additional Kotlin harnesses independently confirmed CharacterSession 1/1, desktop catalog parsing 1/1, and Character Sheet workflow 3/3;
- a cumulative patch was applied to a clean copy of the last compile-hotfixed baseline with `git apply --check`, `git apply`, and `git diff --check`, then the 87 fast guards and all newly introduced backend/rules harnesses were rerun successfully on that applied copy;
- static rules-boundary scans confirm Android/Desktop UI no longer owns direct `DevelopmentEffectIds` rank formulas, Chi/Magic XP arithmetic, run size/legs multiplier tables, or selected roll-effect summation;
- release workflow source tests require the Compose packager, full current parity guards, compile/test gate, and a clean-HOME Xvfb AppImage startup smoke;
- Gradle bootstrap and wrapper are synchronized to 9.7.0; Compose 1.12.0 dependencies use its aligned Material3 1.12.0-alpha03 set; Linux CI uses the project JDK 17 toolchain.

## Environment limitation

The sandbox does not currently have a Gradle distribution or Compose/Maven dependency cache and shell DNS is blocked. Therefore an actual `:desktopApp:compileKotlin`/Compose AppImage build cannot be honestly claimed from this environment yet. CI/local Linux with normal network access is the intended final binary verification path. Do not relabel any older portable 0.2 artifact as the new Compose build.

## Next execution step

1. Run the networked Gradle/Compose compile and AppImage smoke gate for the completed parity-lock slice.
2. Treat shared rules/catalogs/application behavior plus the executable parity contracts as the baseline for all subsequent desktop work.
3. Continue future rulebook-import work by extending the ruleset boundary rather than reintroducing Android/Desktop catalog or formula copies.
4. Defer server/remote repository and generic rule scripting until a real second ruleset/server milestone exists; the schema-8 ruleset identity and repository boundary are the compatibility hooks for that future work.

See `docs/DESKTOP_0_2_PARITY.md` and `docs/superpowers/plans/2026-09-15-desktop-0.2-android-parity.md`.
