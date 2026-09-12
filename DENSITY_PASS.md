# DUBL 0.12.1 — no nested scrolling pass

The 0.12 responsive card system is retained, but the layout contract is stricter:

- cards themselves never scroll;
- table widgets inside cards never scroll;
- a card cannot be resized below a readable floor for its content type;
- when space gets tight, lower-priority content disappears before controls become unreadable;
- long lists expose a useful subset plus an explicit **Показать все** action;
- **Показать все** grows the card and leaves scrolling to the containing column;
- full structured descriptions grow to their document height instead of creating another scrollbar;
- the shell layout state version was bumped to 2 so cramped 0.12 saved heights are discarded once.

The intended rule is: one scroll owner per column, zero nested scrollbars in the normal character-sheet cards.


## 0.13.4 — tighter default density
- Portrait enlarged to 216 px Expanded / 184 px Standard.
- Finite cards now have sensible maximum manual heights instead of stretching into empty rectangles.
- Attribute tiles are shorter and denser.
- Skills show 5 rows by default in Standard mode; more entries remain behind `Показать все`.
- Feats, Magic and Gear use smaller default list windows.
- Table rows and selection previews were tightened.

## 0.13.5 — stable content-sized cards

The experimental height-driven responsive system was removed from the main sheet.

- Card density is now selected from **width only**: standard or narrow/compact.
- Card height is calculated from visible content and cannot be manually squashed below it.
- The vertical resize grip was removed.
- The only vertical compaction control is explicit collapse/expand in the card header.
- Tables and detail previews still do not own scrollbars; if a column becomes taller than the window, the **column** is the single vertical scroll owner.
- Full descriptions remain available by double-click / “Полное описание” or in a detached card; they no longer appear/disappear because a card crossed a height breakpoint.
- Existing saved 0.12/0.13 manual card heights are discarded once while column placement, widths, visibility and collapsed state are preserved.
