# Desktop UI Structural Redesign v1

## Goal

Replace the current parity/prototype presentation of Compose Desktop with a desktop-native shell and a dense Character Sheet dashboard, while preserving every shared DUBL rule, application mutation, persistence behavior, and existing Android/Desktop parity contract.

## Scope

This slice implements exactly three approved steps:

1. a full-width desktop shell with a purposeful navigation rail;
2. reusable desktop presentation primitives;
3. a structural redesign of `CharacterSheetScreen`.

Skills, Development, Magic, Equipment, roll dialogs, and detail dialogs keep their current workflows in this slice. They may consume the new shell spacing automatically, but are not structurally redesigned yet.

## Architecture

`shared/commonMain` rules, formulas, catalogs, and character mutation semantics remain unchanged. One presentation-metadata extension is allowed: `CharacterSheetExtras.notes` plus a typed `SheetApplication.setNotes` operation so notes persist on Android/Desktop through the same application boundary. This is not executable ruleset data.

`Main.kt` owns responsive shell composition and navigation. `UiPrimitives.kt` owns reusable desktop surfaces, section headers, custom vector icons, dense stat/metric/skill/resource rows and compact actions. `CharacterSheetScreen.kt` composes those primitives and invokes only typed `DesktopAppState` methods backed by `DublApplication`.

## Desktop shell

For NORMAL and WIDE layouts the navigation rail is fixed at 220 dp and the workspace fills the remaining width. The previous `widthIn(max = ...)` page cap is removed. The workspace uses 24 dp normal / 28 dp wide outer padding and 18 dp compact padding.

The rail is visually part of the application background rather than a stack of cards. It contains DUBL branding, a compact active-character switcher, five character-workflow navigation entries, a separated Characters entry at the bottom, and no prototype/version footer.

COMPACT keeps top navigation and a single-column workspace.

## Presentation primitives

Three hierarchy levels are used:

- application background;
- standard panel (`DublSurface`);
- raised/inset content (`DublSurfaceRaised` / `DublSurfaceInset`).

The accent palette is informational rather than decorative. Large filled crimson controls are not used for ordinary increment actions.

Required primitives:

- `DesktopPanel`
- `DesktopSectionHeader`
- `DesktopHeroPanel`
- `DesktopStatCell`
- `DesktopMetricCell`
- `DesktopResourceRow`
- `DesktopConditionChip`
- `DesktopSmallAction`

Existing `SectionCard`, `RankStepper`, `KeyValue`, and other primitives remain available for screens not redesigned in this slice.

## Character Sheet layout

The Character Sheet uses `BoxWithConstraints` to choose presentation density from available content width without changing domain behavior.

### Hero

The hero is a single high-priority panel. On wide/normal layouts it contains a compact portrait area beside identity and economy information. On compact layouts it stacks. It shows name, concept, XP, remaining XP, OS budget, size/legs/creation state, active condition chips, and compact edit/economy/portrait actions.

Conditions no longer consume a dedicated always-visible full-width card. Active conditions render as chips in the hero. When none are active only a compact `+ Состояние` action remains.

### Resources, stats, skills, and notes

Resources form one compact horizontal panel beneath the hero on NORMAL/WIDE and wrap/stack at compact widths. Every resource uses the same icon, track, current/maximum value, decrement/increment actions, and optional secondary action.

Below resources, NORMAL/WIDE uses one dense three-column row:

- **Характеристики** — smallest column (about 28% wide): eight compact rows containing only icon, name, effective value, dice action, minus, and plus. XP helper text is removed from the always-visible row.
- **Показатели** — compact middle column (about 21%): Defense, Reflexes, Initiative, Fortitude, Run, and Size as icon/name/value rows with a subtle detail/quick-check affordance where applicable.
- **Умения** — largest column (about 51%): trained skills in two dense columns with category icon, name, total bonus, and quick-roll dice action. Saved grouping order is preserved and group management remains reachable from the header.

COMPACT stacks the same three panels vertically.

The final row contains **Навыки и развитие** plus **Заметки**. Development entries retain grouped ordering, group management, navigation, and click-through details. Notes are editable and persisted as sheet metadata. Conditions remain in the hero and are not duplicated below.

## Behavior preservation

No existing `DesktopAppState` state-changing method is replaced or bypassed. Undo, resource control, custom resources, conditions, portrait import, attribute progression, quick rolls, grouping, skill rolls, development details, and navigation keep their existing callbacks.

## Responsive and accessibility constraints

- No app-level horizontal scroll.
- NORMAL/WIDE prioritizes information density: characteristics consume the least space, derived stats remain compact, and skills receive the largest share.
- Long names and concepts wrap rather than clip critical information.
- Character Sheet remains usable at COMPACT widths.
- Ordinary text/action controls keep practical click targets without letting controls dominate the data hierarchy.
- Empty states collapse rather than reserving large blank areas.

## Verification

Add a source contract test that fails on the old UI and protects the structural design:

- no desktop page `widthIn(max = ...)` cap;
- no `Desktop 0.2 · Compose parity` footer;
- rail separates Characters from primary workflow navigation;
- required primitives exist;
- Character Sheet uses `BoxWithConstraints`, `DesktopHeroPanel`, `DesktopResourceRow`, `DesktopStatCell`, `DesktopMetricCell`, and `DesktopConditionChip`;
- Character Sheet no longer builds a standalone `SectionCard("Состояния")`;
- existing parity/source suites remain green.
