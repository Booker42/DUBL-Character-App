# DUBL 3.69 Domain Promotion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Promote Conditions and Skills from bootstrap/manual runtime data to deterministic DUBL 3.69 rulebook-generated runtime content without changing UI design.

**Architecture:** The existing multi-source Rulebook IR remains the only upstream parser. Each promoted domain gets a dedicated importer that emits deterministic JSON with provenance, a committed promoted artifact used by shared runtime, a baseline hash, and parity tests against stable runtime IDs. Android/Desktop consume the same shared artifact and never interpret DOCX directly.

**Tech Stack:** Python 3 + python-docx import pipeline, Kotlin Multiplatform shared models/parsers, Android asset adapter, Compose Desktop classpath resources, Python/Kotlin parity harnesses.

**Spec:** `docs/superpowers/specs/2026-09-15-dubl-369-rulebook-import-pipeline-design.md`

## Global Constraints

- Rulebook sources are authoritative; current app content is regression evidence only.
- Ambiguous source material stays unresolved unless an explicit tracked resolution selects an interpretation.
- Stable persistence IDs must not change merely because content is regenerated.
- Promoted runtime artifacts must be deterministic and shared by Android/Desktop.
- No UI redesign, server work, or universal rules DSL.
- Every promoted domain must expose a local add/override/reset escape hatch; user overrides never modify canonical generated artifacts.

---

### Task 1: Harden source policy and tracked resolutions

**Files:**
- Modify: `tools/rulebook/provenance.py`
- Modify: `tools/rulebook/build_ruleset.py`
- Modify: `tools/tests/test_rulebook_ruleset_build.py`
- Modify: `tools/tests/test_rulebook_validator.py`

**Interfaces:**
- Archmage source selection prefers explicit source metadata and uses IDs only as legacy fallback.
- Bundle `resolutions.json` is copied from tracked `rulesets/dubl-3.69/resolutions.json` and validated against emitted diagnostics.

- [ ] Add RED tests for explicit Archmage source metadata and tracked resolution propagation.
- [ ] Implement minimal policy/resolution changes.
- [ ] Run rulebook build/validator tests GREEN.

### Task 2: Promote Conditions into the shared runtime resource

**Files:**
- Create: `shared/src/commonMain/resources/conditions_catalog.json`
- Modify: `tools/rulebook/check_baseline.py`
- Modify: `tools/rulebook/baseline.py`
- Modify: `tools/tests/test_rulebook_conditions_runtime_parity.py`
- Modify: `shared/src/commonMain/kotlin/com/dubl/character/android/data/CatalogData.kt`
- Modify: `shared/src/commonMain/kotlin/com/dubl/character/android/model/CharacterSheetExtras.kt`
- Create: `app/src/main/java/com/dubl/character/android/data/ConditionCatalogRepository.kt`
- Modify: `shared/src/desktopMain/kotlin/com/dubl/character/desktop/data/DesktopCatalogLoader.kt`
- Modify: Android/Desktop condition-info call sites only as needed to consume shared generated summaries.

**Interfaces:**
- `parseConditionCatalog(raw: String) -> ConditionCatalog`
- Runtime stable enum IDs remain `CharacterConditionId.name`.
- Rulebook generated condition content maps by exact canonical title to those IDs.

- [ ] Add RED parity tests requiring committed runtime JSON to equal the generated rulebook artifact and all enum titles to resolve.
- [ ] Promote generated JSON into shared resources through explicit baseline/update tooling.
- [ ] Add shared parser and platform loaders.
- [ ] Replace handwritten condition summaries with catalog lookup while preserving UI behavior.
- [ ] Run Android/Desktop condition parity and persistence tests GREEN.

### Task 3: Source-generate the base Skills catalog

**Files:**
- Create: `tools/rulebook/import_skills.py`
- Create: `tools/tests/test_rulebook_skills_import.py`
- Create: `shared/src/commonMain/resources/skills_catalog.json`
- Modify: `shared/src/commonMain/kotlin/com/dubl/character/android/model/SkillModels.kt`
- Modify: baseline/update tooling and ruleset config.

**Interfaces:**
- `import_skills(raw_ir: dict, source_key: str = "core") -> dict`
- Emits stable skill IDs, names, default attributes, untrained policy, description/source refs, and explicit unresolved diagnostics for incomplete skill definitions.

- [ ] Inspect the stock skill section and encode its structural grammar in synthetic RED fixtures.
- [ ] Implement importer from Raw IR only.
- [ ] Compare generated skills against current runtime skill definitions; app differences become diagnostics, not silent overrides.
- [ ] Promote only deterministic fields; leave unresolved mechanics explicit.
- [ ] Make shared runtime use the promoted catalog while preserving stable skill IDs/state persistence.
- [ ] Run SkillRules + Android/Desktop skill parity GREEN.

### Task 4: End-to-end promotion gate

**Files:**
- Modify: `tools/tests/test_rulebook_release_gate.py`
- Modify: `.github/workflows/linux-appimage.yml`
- Modify: `README.md`, `HANDOFF.md`, `CHANGELOG.md`

**Interfaces:**
- CI validates importer synthetic tests, committed generated-artifact hashes, Conditions runtime parity, Skills runtime parity, and existing Android/Desktop ruleset guards.

- [ ] Add RED release-gate assertions.
- [ ] Wire promoted-domain checks into CI.
- [ ] Run full offline parity suite and heavy Kotlin harnesses.
- [ ] Verify cumulative patch applies cleanly to the previous parity-lock baseline.

### Task 5: Add local override escape hatch for promoted domains

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/dubl/character/android/model/SkillModels.kt`
- Modify: `shared/src/commonMain/kotlin/com/dubl/character/android/state/CharacterSession.kt`
- Modify: `shared/src/commonMain/kotlin/com/dubl/character/android/data/SnapshotCodec.kt`
- Modify: Android/Desktop skill settings dialogs without redesigning layout.
- Test: shared/offline skill override persistence and reset harnesses.

**Interfaces:**
- Built-in skills can locally override user-editable fields without mutating `GeneratedSkillCatalog`.
- Custom skills remain addable.
- `resetSkillOverrides(id)` restores canonical imported fields while preserving rank/progression state.

- [ ] Add RED tests for canonical skill override, unresolved `Компьютеры` local untrained override, reset-to-canonical, and persistence round-trip.
- [ ] Implement minimal model/session/codec changes.
- [ ] Expose the same edit/reset actions from Android and Desktop skill settings.
- [ ] Run rulebook baseline, generated-skill parity, CharacterSession, persistence, and Desktop/Android source guards GREEN.
