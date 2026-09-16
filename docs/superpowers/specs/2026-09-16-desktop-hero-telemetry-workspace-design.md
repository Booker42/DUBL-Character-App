# Desktop Hero Telemetry + Skills/Development Workspace Design

## Scope

This pass changes only the DUBL Desktop character sheet presentation plus the shared roll preset needed for the explicitly requested Run quick roll. It preserves the existing Shared Application boundary, skill/development grouping data, hidden-skill semantics, persistence, catalogs, and ruleset identity.

## Hero

The hero becomes the compact character telemetry surface. It keeps portrait, identity, XP/AP economy, conditions, and adaptive resources. Below those, it contains two dense strips:

- **Characteristics**: all eight attributes. Each item shows icon, full name, value, dice action, minus, plus. No XP helper text.
- **Indicators**: Defense, Reflexes, Initiative, Fortitude, Run, Size. Each shows icon, name, value. Reflexes, Initiative, Fortitude, and Run have an explicit dice action. Defense and Size do not.

Wide desktop uses horizontal compact cells; normal/compact widths wrap into more rows without horizontal scrolling.

## Run roll

Add `RollContext.RUN` to shared roll presets so Desktop uses the same roll engine as other quick checks. The current roll engine requires an integer check bonus, while `runFull` is a distance value represented as `Double`; the quick-roll preset uses the integral part of `runFull` as the check bonus and exposes a formula label identifying it as Run. This is a convenience roll requested by the user, not a rewrite of the Run distance formula.

## Main workspace

On wide/normal desktop, the primary row becomes:

- **Skills ~35%**
- **Development ~65%**

Skills retain all visible skills including rank 0, explicit rank + total bonus, user groups, hidden-skill behavior, and quick rolls.

Development retains user groups, shared requirement/parent hierarchy, parent/child tree rendering, and existing detail navigation. The development panel moves out of the old summary row and sits directly beside Skills.

Notes remain below as a compact full-width panel. On compact width, Skills, Development, and Notes stack vertically.

## Non-goals

- No changes to skill/development ownership semantics.
- No changes to hidden-skill persistence or grouping schema.
- No Android UI redesign in this pass.
- No new ruleset/catalog content.
- No horizontal scrolling.
