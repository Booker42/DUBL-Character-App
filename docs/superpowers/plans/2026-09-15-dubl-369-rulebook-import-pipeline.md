# DUBL 3.69 Rulebook Import Pipeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first deterministic DUBL 3.69 DOCX import pipeline with Raw Source IR, provenance, diagnostics/resolutions, and source-coverage validation for the canonical shared catalogs.

**Architecture:** Each registered rulebook source is parsed independently into loss-minimized Raw Source IR and combined only through source-qualified references. All semantic import work consumes IR rather than reparsing Word layout independently. During migration the existing shared catalogs are copied into a versioned ruleset bundle only as explicitly marked `bootstrap_mirror` domains, enriched with rulebook provenance and validated for coverage; later tasks replace each mirror with a true source-generated importer.

**Tech Stack:** Python 3, python-docx, JSON, existing Kotlin/Compose shared runtime catalogs, Python unittest parity guards.

**Spec:** `docs/superpowers/specs/2026-09-15-dubl-369-rulebook-import-pipeline-design.md`

## Global Constraints

- Stock DUBL 3.69 DOCX is the core source of truth; approved supplement books are authoritative for their imported modules.
- Ambiguous or contradictory material must not become executable behavior without an explicit resolution.
- DOCX is a development input, not a runtime dependency.
- Existing Android/Desktop runtime behavior is regression evidence, not authority over the rulebook.
- No UI redesign, server work, or universal rules DSL in this milestone.

---

### Task 1: Deterministic Raw Source IR extractor

**Files:**
- Create: `tools/rulebook/__init__.py`
- Create: `tools/rulebook/extract_docx.py`
- Create: `tools/tests/test_rulebook_raw_ir.py`

**Interfaces:**
- Produces: `extract_docx(path: Path) -> dict`
- Produces CLI: `python3 tools/rulebook/extract_docx.py --source <docx> --output <json>`

- [ ] Write a failing test that extracts a synthetic DOCX with headings, paragraphs, and a table and asserts block order, heading paths, stable block IDs, original text, and source SHA-256.
- [ ] Run the test and confirm it fails because the extractor does not exist.
- [ ] Implement deterministic block extraction using `python-docx` and document-body order.
- [ ] Re-run the test and confirm it passes.

### Task 2: Source index and candidate lookup

**Files:**
- Create: `tools/rulebook/source_index.py`
- Create: `tools/tests/test_rulebook_source_index.py`

**Interfaces:**
- Consumes: Raw IR from Task 1.
- Produces: `build_source_index(raw_ir: dict) -> dict`
- Produces: normalized lookup entries keyed by heading text and exact/normalized block text.

- [ ] Write failing tests for deterministic heading candidates, duplicate headings, and lookup by normalized Russian text (`ё/е`, whitespace, dash normalization).
- [ ] Run tests and confirm RED.
- [ ] Implement index construction without discarding duplicate occurrences.
- [ ] Re-run and confirm GREEN.

### Task 3: Diagnostics and explicit resolutions model

**Files:**
- Create: `tools/rulebook/diagnostics.py`
- Create: `rulesets/dubl-3.69/resolutions.json`
- Create: `tools/tests/test_rulebook_diagnostics.py`

**Interfaces:**
- Produces: stable diagnostic IDs derived from type + source refs + normalized subject.
- Produces: `apply_resolutions(diagnostics, resolutions) -> (remaining, applied)`.

- [ ] Write failing tests proving unresolved errors remain errors and explicit resolution IDs remove only their named conflict.
- [ ] Implement diagnostic normalization and resolution matching.
- [ ] Re-run and confirm GREEN.

### Task 4: Bootstrap ruleset bundle with provenance coverage

**Files:**
- Create: `tools/rulebook/build_ruleset.py`
- Create: `tools/rulebook/provenance.py`
- Create: `rulesets/dubl-3.69/manifest.json`
- Create: `tools/tests/test_rulebook_ruleset_build.py`

**Interfaces:**
- CLI: `python3 tools/rulebook/build_ruleset.py --source <docx> --repo-root <repo> --output-dir rulesets/dubl-3.69`
- Reads canonical shared catalogs.
- Writes Raw IR, source index, mirrored content catalogs, diagnostics, and manifest.

- [ ] Write failing tests using a synthetic rulebook + synthetic catalog to assert exact source match, ambiguous source match, missing source match, and `bootstrap_mirror` status.
- [ ] Implement provenance matching using exact normalized names first; never silently choose among multiple equally valid matches.
- [ ] Build the bundle deterministically with sorted JSON keys where schema permits and stable entity order inherited from catalogs.
- [ ] Re-run and confirm GREEN.

### Task 5: Ruleset validator

**Files:**
- Create: `tools/rulebook/validate_ruleset.py`
- Create: `tools/tests/test_rulebook_validator.py`

**Interfaces:**
- Produces CLI exit code 0 only for structurally valid bundles with no unresolved `error` diagnostics.
- Validates manifest identity, content files, duplicate entity IDs, provenance refs, resolution refs, and source hash consistency.

- [ ] Write failing tests for duplicate IDs, nonexistent source refs, stale source hash, invalid resolution target, and unresolved error.
- [ ] Implement validator with human-readable diagnostics.
- [ ] Re-run and confirm GREEN.

### Task 6: Real DUBL 3.69 baseline build and coverage report

**Files:**
- Generate: `rulesets/dubl-3.69/source/raw_ir.json`
- Generate: `rulesets/dubl-3.69/source/source_index.json`
- Generate: `rulesets/dubl-3.69/content/*.json`
- Generate: `rulesets/dubl-3.69/diagnostics.json`
- Modify: `rulesets/dubl-3.69/manifest.json`
- Create: `tools/tests/test_dubl_369_rulebook_pipeline.py`

**Interfaces:**
- Uses the stock rulebook as external build input.
- Regression test checks counts/hashes/coverage summary from generated bundle, but does not require the DOCX to ship in the application.

- [ ] Run the builder against the stock DOCX.
- [ ] Inspect diagnostics and classify extractor/provenance failures separately from genuine rulebook ambiguity.
- [ ] Add regression guards for real-source extraction counts and manifest domain status.
- [ ] Run the complete rulebook test suite and existing ruleset parity guards.

### Task 7: CI/release gate integration

**Files:**
- Modify: `.github/workflows/linux-appimage.yml`
- Modify: `README.md`
- Modify: `HANDOFF.md`
- Modify: `CHANGELOG.md`
- Create or modify: `tools/tests/test_rulebook_release_gate.py`

**Interfaces:**
- CI must run rulebook pipeline structural tests without needing the private/local DOCX fixture.
- Real DOCX regeneration remains an explicit developer command until the source file is intentionally distributed with CI fixtures.

- [ ] Add failing source guard asserting rulebook tests are included in the Linux release gate.
- [ ] Wire the tests into CI without adding the DOCX as an app/runtime dependency.
- [ ] Document regeneration, validation, unresolved diagnostics, and domain-promotion workflow.
- [ ] Run all fast parity/release tests and `git diff --check`.
