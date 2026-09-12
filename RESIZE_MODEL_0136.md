# 0.13.6 — bounded resizable cards

## Contract

Each card has a per-card minimum, preferred and maximum height. The user owns a persistent requested height inside those bounds. The rendered height may temporarily grow when visible content requires more room, but temporary growth never overwrites the requested height and never participates in density selection.

Density is recalculated only after a vertical drag ends or after a debounced column-width change. During the drag only the outer geometry changes. This prevents the resize → density → sizeHint → resize feedback loop from earlier versions.

Long details are capped inside the sheet and remain available in the separate full-description dialog. Tables/details do not own vertical scrollbars; the column scroll area remains the only scroll owner.

Shell state version 6 stores the requested height for each card. Heights from earlier experimental resize models are intentionally discarded once during migration.
