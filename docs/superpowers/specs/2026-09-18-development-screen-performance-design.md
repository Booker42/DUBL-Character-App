# Development Screen Performance Design

## Goal

Remove the repeated multi-second pause when opening Android development skills, reduce martial-arts scroll jank, and apply the same lazy grouped presentation to Desktop.

## Behaviour

- Regular, special-branch, martial-art, and chi groups start collapsed.
- A user may expand any number of groups during the current screen session.
- Only expanded groups emit item rows into the lazy list.
- Search temporarily shows matching groups and matching rows without overwriting manual expansion state.
- Clearing search restores the user's manual expansion state.
- The owned tab remains ungrouped and immediately visible.
- Android and Desktop use the same pure group-visibility policy.

## Performance Architecture

- Precompute stable entry metadata and normalized search text once per effective catalog.
- Prepare availability and economy outside composition and retain the prepared result across Android tab navigation for the same character/catalog revision.
- Tab changes select from prepared lists instead of reparsing requirements.
- Compose list bodies perform no catalog-wide requirement evaluation.
- Collapsed groups do not compose their child cards.

## Verification

- Common tests cover collapsed defaults, independent expansion, search override, and restoration after search.
- Model tests cover preparation reuse keys and indexed filtering.
- Android and Desktop compilation validates platform integration.
- Existing shared and Android unit suites must remain green.
