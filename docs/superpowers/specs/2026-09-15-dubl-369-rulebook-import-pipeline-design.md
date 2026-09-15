# DUBL 3.69 Rulebook Import Pipeline Design

## Goal

Make `Dубль All Stars 3.69 REWORK` the auditable upstream source for DUBL 3.69 ruleset content without silently inventing behavior where the rulebook is incomplete or contradictory.

## Source-of-truth policy

1. The stock DUBL 3.69 DOCX is the authoritative core source. Approved supplement rulebooks (currently `Dубль, Мастера ближнего боя` and `Книга Архимага`) are authoritative only for content intentionally imported from those modules.
2. Android and Desktop are implementations, never authorities over the rulebook.
3. Extraction preserves source text and structure before any interpretation.
4. Ambiguous, incomplete, or contradictory material is imported as source content and diagnostic metadata, but is not silently promoted to executable mechanics.
5. Any executable interpretation not uniquely determined by the rulebook must have an explicit resolution record.
6. Generated runtime catalogs are derived artifacts. The DOCX itself is not a runtime dependency.
7. Canonical imported content is immutable at runtime, but it must never trap the user: every promoted domain needs a local custom/override layer that can add entities, override incomplete or disputed fields, and reset overrides back to canonical content.
8. Local overrides are user data, never source resolutions. They must not mutate the canonical ruleset, rewrite importer baselines, or masquerade as rulebook authority.

## Pipeline

```text
DOCX
  -> Raw Source IR
  -> Source index / entity candidates
  -> Domain importers
  -> DUBL 3.69 ruleset bundle
  -> Validator + diagnostics + explicit resolutions
  -> shared runtime resources
  -> CharacterSession
  -> Android / Desktop
```

## Raw Source IR

Each registered source is extracted independently. The extractor emits deterministic JSON containing:

- source filename, SHA-256, extraction schema version;
- ordered paragraphs and tables;
- paragraph style and heading level;
- heading path active at each block;
- table rows/cells without semantic reinterpretation;
- stable block IDs based on document order;
- normalized text only as a parallel search key; original text remains intact.

The Raw IR must be reproducible byte-for-byte for the same source DOCX and importer version.

## Provenance

Every imported semantic entity may contain one or more source references:

```json
{
  "source": "core",
  "blockId": "p-005103",
  "headingPath": ["Магия", "Описание заклинаний", "Агония"],
  "excerpt": "..."
}
```

Source references are development/audit metadata. Runtime Kotlin models do not need to carry them unless a future UI deliberately exposes citations.

## Diagnostics and resolutions

Diagnostics have stable IDs and severity:

- `error`: structural failure or contradiction that prevents a deterministic executable interpretation;
- `warning`: incomplete, suspicious, duplicate, or ambiguous content preserved without automation;
- `info`: drift, duplicate, or source-coverage note.

Explicit resolutions are separate from extraction. A resolution must name the diagnostic/conflict it resolves, state the selected interpretation, and carry a human-readable rationale. Importers never infer a resolution from Android/Desktop behavior.

## Ruleset layout

Full extraction/build output is deliberately ignored and reproducible:

```text
build/rulesets/dubl-3.69/
  manifest.json
  source/
    core_raw_ir.json
    melee_raw_ir.json
    archmage_raw_ir.json
    source_index.json
  content/
    development.json
    chi.json
    magic_equipment.json
    skill_effects.json
    conditions.json
  diagnostics.json
  resolutions.json
```

The tracked control plane is compact and reviewable:

```text
rulesets/dubl-3.69/
  config.json
  baseline.json
  resolutions.json
  generated/
    conditions.json
```

`baseline.json` locks source hashes, extraction counts, provenance coverage, and diagnostic totals. `check_baseline --update` is an explicit promotion step and also refreshes tracked artifacts for domains marked `source_generated`. Large mirrored catalogs and Raw IR are never a second committed source of truth.

During migration, existing shared catalogs may be represented in the build bundle only as explicitly marked `bootstrap_mirror` domains, enriched with source provenance and validated for coverage. Each domain is then promoted to source-generated status individually.

## Migration policy

Milestone A establishes multi-source extraction, source-qualified provenance, validation, diagnostics, resolutions, deterministic build tooling, and source coverage of the current canonical catalogs. Domain source policy is explicit: core content resolves against `core`; martial/Chi against `melee`; approved Archmage additions against `archmage`.

Milestone B promotes domains one at a time to true source-generated state. A domain is considered promoted only when:

1. its catalog is generated from Raw IR + explicit resolutions;
2. generated output is deterministic;
3. current runtime parity tests pass;
4. all imported entities have source provenance or an explicit synthetic/resolution marker;
5. unresolved source contradictions are surfaced instead of silently normalized.

Recommended promotion order: conditions/basic character rules -> skills -> development/special branches -> martial arts/Chi -> magic -> equipment -> skill-effect automation.

## Runtime integration

The current shared resources remain the runtime API while migration is underway. Once a domain is promoted, the generated ruleset artifact becomes the only producer of its shared resource. Android/Desktop continue reading the same shared resources and do not parse DOCX.

## Local override / custom-content contract

Promotion is not allowed to make incomplete source material unusable. Runtime resolution follows this precedence:

```text
local entity/field override
  -> canonical imported field
  -> explicit unresolved/unavailable state
```

A user override may choose a playable local interpretation for unresolved content, but this remains character/local user data and never resolves the upstream diagnostic. Every editable canonical entity must support reset-to-canonical semantics. Domains that support user-created entities keep those entities distinct from canonical IDs so future ruleset rebuilds cannot overwrite them.

## Non-goals

- No universal RPG DSL in this phase.
- No server, HTTP API, authentication, or remote ruleset distribution.
- No automatic AI interpretation of contradictory prose.
- No rewriting/correcting the rulebook source.
- No UI redesign; minimal controls needed to expose local overrides are allowed.
