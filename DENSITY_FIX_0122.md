# DUBL 0.12.2 — stable responsive sizing

This pass fixes the resize feedback loop introduced by 0.12/0.12.1.

- Density is selected from the user-requested height budget, not the final height produced by content.
- Content-driven growth never changes the density decision, so threshold oscillation cannot feed itself.
- Horizontal resize density changes are debounced.
- Long spell/feat/item descriptions grow the card and leave scrolling to the column.
- Selecting a shorter description can shrink the card back to the requested height because the requested height is no longer overwritten.
- Inner tables and detail panes remain non-scrolling.
- 0.12.1 saved card heights are discarded once while preserving column order, widths, visibility and collapse state.
- Minimum desktop/column widths were raised to avoid unusable ultra-narrow cards.
