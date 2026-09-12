# 0.13.5 — stable layout pass

This patch intentionally trades aggressive automatic resizing for predictable desktop behavior.

## Contract

1. Width may change presentation (`standard` / `compact`).
2. Height never changes presentation.
3. Open card height follows visible content.
4. A user may collapse a card, but may not squash its open content with a vertical handle.
5. Nested card/table/detail scrollbars are avoided; the column is the normal vertical scroll owner.
6. Large lists use explicit “Показать все”; full descriptions use the details dialog or detached-card view.

This removes the circular dependency that previously existed between card height, density mode and `sizeHint()`.
