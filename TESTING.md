# DUBL Testing Guide

This document is for people testing development releases of DUBL Character App.

The goal is not to exhaustively click every control. The most useful testing is a short, reproducible path that proves whether character data, rules calculations, persistence, and the workspace behave correctly.

## Before you start

Record these details before filing an issue:

- DUBL release/version.
- Operating system.
- On Linux: distribution, desktop environment/window manager, and whether you use X11 or Wayland if known.
- Whether the character is new, imported, or migrated from an older DUBL version.

For bugs involving existing character data, first reproduce on a new character if practical. That tells us whether the problem is general or save-specific.

## Core test pass

### 1. Launch and character library

- Start DUBL from a release package.
- Create a new character.
- Rename/edit it if the UI allows it.
- Close DUBL and open it again.
- Confirm the character is still present and opens correctly.

Report any startup error, blank window, missing controls, extreme scaling, unreadable text, or lost character immediately.

### 2. Attributes and calculated values

Change several attributes one at a time and verify that visibly dependent/derived values update immediately.

Useful checks:

- Increase and decrease values, including back to zero/default.
- Change Size separately from normal attributes.
- Save, reopen, and verify the same values remain.
- Watch for a displayed total that does not update until another action occurs.

When reporting a formula problem, write the exact input values and the displayed result. If you know the expected result from the rulebook, include it too.

### 3. Skills and abilities

- Add/use ordinary skills.
- Test a skill with prerequisites.
- Test a branch/sub-skill that should require another skill.
- Try an invalid combination and check whether DUBL blocks it or clearly explains the requirement.
- Hide/remove an unused skill if that feature is available, then restore it and verify its data is not corrupted.

For prerequisite bugs, include the exact skill/ability names and ranks involved.

### 4. Resources

- Change current resource values.
- Test optional Mana behavior.
- Add a custom resource.
- Save/reopen and confirm custom resources and values persist.

Watch for duplicated resources, reset values, negative/invalid states, or custom blocks disappearing after UI changes.

### 5. Magic, equipment, and secondary systems

Open the Magic and Equipment areas and test editing/selecting actual content rather than only opening the panels.

Check for:

- Missing catalog entries.
- Wrong descriptions or metadata.
- A selection changing the wrong character field.
- Text clipping or fields that become unusable at normal window sizes.
- Data disappearing after save/reload.

### 6. Workspace and cards

This is a high-priority UI area.

Test:

- Move cards.
- Resize from every supported edge/corner.
- Collapse and expand cards.
- Hide and restore cards.
- Detach/pop out and return a card if available.
- Close and reopen DUBL and check whether the layout is restored.
- Try a smaller application window and a maximized window.

For visual/layout bugs, screenshots or a short video are much more useful than a long description.

### 7. Import/export and persistence

- Export/save a character.
- Re-import/reopen it.
- Confirm important values survived the round trip.
- If you have an older DUBL save, test migration on a COPY of that file.

Never use your only copy of an important character for migration testing.

## High-value bug reports

A strong report answers these questions:

1. Which version and OS?
2. What exact character/save state did you start from?
3. What exact actions reproduce it?
4. What happened?
5. What should have happened?
6. Does it reproduce after restarting DUBL?
7. Does it reproduce on a newly created character?

If the bug is intermittent, say approximately how often you can reproduce it.

## UX feedback is also useful

Please report things such as:

- A control whose purpose is unclear.
- Important information hidden in an unexpected place.
- A workflow that takes too many clicks.
- Text that is hard to read or gets clipped.
- A panel that wastes space or becomes unusable at smaller sizes.
- A button/menu item that appears obsolete or duplicated.

For UX feedback, explain what you were trying to accomplish before you got confused or slowed down. That context is usually more valuable than simply saying that a screen looks bad.
