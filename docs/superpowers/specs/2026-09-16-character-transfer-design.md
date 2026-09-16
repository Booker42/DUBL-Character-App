# DUBL Cross-Platform Character Transfer Design

## Goal

Add a portable single-character `.dubl` interchange format so a character can be exported from Android and imported on Compose Desktop, and vice versa, without bypassing the Shared Application Lock.

## Scope

This slice transfers one character at a time. It preserves the canonical `DublCharacter` payload and portable `CharacterSheetExtras`: conditions, hidden resources, preferred skill attributes, skill/development groups, local condition overrides, custom conditions, and notes.

`portraitUri` is deliberately not transferred in v1 because it is a platform-local Android content URI or desktop filesystem path and is not portable across devices. The transfer document omits it and imports with `portraitUri = null`. Portable portrait attachments can be added in a later transfer-format version without changing the character schema.

The current implementation accepts only the canonical DUBL ruleset reference `dubl / 3.69`. Other rulesets are rejected instead of being interpreted through DUBL UI/rules accidentally.

## Architecture

A new shared `CharacterTransferCodec` owns the file format. The document is JSON with an explicit transfer format identifier and version. It embeds the existing `SnapshotCodec` representation for exactly one character, so character schema migrations remain centralized in `SnapshotCodec` rather than duplicated in transfer code. Portable extras use a small shared JSON representation based on the existing common `MiniJson` utilities.

A new `CharacterTransferApplication` capability is exposed as `DublApplication.transfer`. Export is read-only. Import is a state-changing operation routed through the shared application boundary: it decodes and validates the transfer document, creates a new local character identity, persists the imported character, persists its extras, and selects the imported character.

Imported top-level character IDs are always regenerated locally. This makes repeated imports safe and avoids accidental overwrite when the same character already exists on the target device. IDs inside the character payload (custom skills, resources, spells, gear, development entries, condition/group references) are preserved so internal references remain valid.

Android and Desktop remain thin platform adapters. They only choose/read/write files and call the shared transfer capability.

## File Format

Transfer v1 root fields:

- `format`: `"dubl.character"`
- `version`: `1`
- `characterSnapshot`: the JSON object produced by `SnapshotCodec` for an `AppSnapshot` containing exactly one character
- `extras`: portable character-sheet extras, excluding `portraitUri`

The file extension is `.dubl`. The payload is UTF-8 JSON. Android uses the system document picker; Desktop uses the existing AWT `FileDialog` pattern already used for portrait selection.

## Import Result and Validation

Shared import returns a typed result:

- `Imported(characterId, name)` on success;
- `Rejected(reason)` for invalid JSON/shape, unsupported transfer-format version, or unsupported ruleset.

A rejected import must not mutate the roster or extras stores.

## UI

Android `CharactersScreen` adds `Импорт` and `Экспорт` actions next to character creation. Export targets the active character and suggests `<character-name>.dubl`. Import opens any file type because Android file providers may not know the custom `.dubl` extension. A short status message reports success/failure.

Desktop `CharactersScreen` adds the same actions and uses native file dialogs. Export defaults to `<character-name>.dubl`; import reads a selected file and reports a short status message.

## Testing

1. Common codec tests prove transfer round-trip and v1 validation.
2. Shared application golden coverage proves import/export go through `DublApplication`, preserve portable extras, regenerate the top-level ID, make the imported character active, and do not mutate state on rejection.
3. Source-contract tests prove Android/Desktop surfaces call the shared transfer capability and the Linux release gate includes the new test.
4. Existing Shared Application Lock, persistence, character-sheet, and parity tests remain green.
