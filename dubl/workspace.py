"""Free-form character-sheet workspace used by DUBL 0.14.

The workspace owns card position/size. Cards never participate in a column
layout, so moving or resizing one card does not force unrelated cards to
reflow.  Editing geometry is available only in explicit layout-customize mode.
"""
from __future__ import annotations

from PySide6.QtCore import Qt, QRect, QSize, QPoint
from PySide6.QtGui import QPainter, QPen, QColor
from PySide6.QtWidgets import QWidget, QScrollArea, QFrame

GRID = 8
GAP = 10
SNAP = 11
MARGIN = 18


class WorkspaceCanvas(QWidget):
    """Absolute-position canvas with grid/edge snapping and alignment guides."""
    def __init__(self, owner, parent=None):
        super().__init__(parent)
        self.owner = owner
        self.guides_x: list[int] = []
        self.guides_y: list[int] = []
        self.customizing = False
        self.setObjectName('workspaceCanvas')
        self.setMinimumSize(1180, 760)
        self.setAttribute(Qt.WA_StyledBackground, True)

    def set_customize(self, on: bool):
        self.customizing = bool(on)
        self.setProperty('customizing', self.customizing)
        self.update()

    def visible_cards(self, exclude=None):
        return [c for c in self.owner.docks.values() if c is not exclude and c.isVisible() and not getattr(c, 'popup', None)]

    def _grid(self, value: int) -> int:
        return int(round(value / GRID) * GRID)

    def _nearest(self, value: int, candidates: list[tuple[int, int]]):
        """Return (snapped value, guide) when candidate is close enough."""
        best = None
        for target, guide in candidates:
            delta = abs(value - target)
            if delta <= SNAP and (best is None or delta < best[0]):
                best = (delta, target, guide)
        return (best[1], best[2]) if best else (value, None)

    def snap_move(self, card, rect: QRect) -> QRect:
        w, h = rect.width(), rect.height()
        x = self._grid(max(MARGIN, rect.x()))
        y = self._grid(max(MARGIN, rect.y()))
        x_candidates = [(MARGIN, MARGIN)]
        y_candidates = [(MARGIN, MARGIN)]
        for other in self.visible_cards(card):
            r = other.geometry()
            # Match left/right/center, or sit next to the other card with GAP.
            x_candidates += [
                (r.left(), r.left()),
                (r.right() - w + 1, r.right()),
                (r.center().x() - w // 2, r.center().x()),
                (r.right() + 1 + GAP, r.right() + 1 + GAP),
                (r.left() - GAP - w, r.left() - GAP),
            ]
            y_candidates += [
                (r.top(), r.top()),
                (r.bottom() - h + 1, r.bottom()),
                (r.center().y() - h // 2, r.center().y()),
                (r.bottom() + 1 + GAP, r.bottom() + 1 + GAP),
                (r.top() - GAP - h, r.top() - GAP),
            ]
        x, gx = self._nearest(x, x_candidates)
        y, gy = self._nearest(y, y_candidates)
        self.set_guides([gx] if gx is not None else [], [gy] if gy is not None else [])
        return QRect(max(MARGIN, x), max(MARGIN, y), w, h)

    def snap_resize(self, card, rect: QRect, horizontal=True, vertical=True) -> QRect:
        x, y = rect.x(), rect.y()
        right = self._grid(rect.right() + 1)
        bottom = self._grid(rect.bottom() + 1)
        rx_candidates: list[tuple[int, int]] = []
        by_candidates: list[tuple[int, int]] = []
        for other in self.visible_cards(card):
            r = other.geometry()
            rx_candidates += [(r.left() - GAP, r.left() - GAP), (r.right() + 1, r.right() + 1)]
            by_candidates += [(r.top() - GAP, r.top() - GAP), (r.bottom() + 1, r.bottom() + 1)]
        gx = gy = None
        if horizontal:
            right, gx = self._nearest(right, rx_candidates)
        else:
            right = rect.right() + 1
        if vertical:
            bottom, gy = self._nearest(bottom, by_candidates)
        else:
            bottom = rect.bottom() + 1
        self.set_guides([gx] if gx is not None else [], [gy] if gy is not None else [])
        return QRect(x, y, max(1, right - x), max(1, bottom - y))

    def conflicts(self, card, rect: QRect) -> bool:
        # A visible GAP between cards is intentional. Exact edge contact is still
        # legal but the default snapping targets GAP for a cleaner sheet.
        for other in self.visible_cards(card):
            if rect.intersects(other.geometry()):
                return True
        return False

    def available_growth_height(self, card, target_height: int) -> int:
        """Grow downward only until another card would be hit."""
        current = card.geometry()
        allowed = target_height
        left, right = current.left(), current.right()
        for other in self.visible_cards(card):
            r = other.geometry()
            horizontal_overlap = not (right < r.left() or left > r.right())
            if horizontal_overlap and r.top() > current.top():
                allowed = min(allowed, max(current.height(), r.top() - GAP - current.top()))
        return max(current.height(), allowed)

    def set_guides(self, xs=None, ys=None):
        xs = [int(x) for x in (xs or []) if x is not None]
        ys = [int(y) for y in (ys or []) if y is not None]
        if xs == self.guides_x and ys == self.guides_y:
            return
        self.guides_x, self.guides_y = xs, ys
        self.update()

    def clear_guides(self):
        self.set_guides([], [])

    def refresh_extent(self):
        max_right = max([c.geometry().right() for c in self.visible_cards()] + [0])
        max_bottom = max([c.geometry().bottom() for c in self.visible_cards()] + [0])
        viewport = getattr(self.owner, 'workspace_area', None)
        viewport_size = viewport.viewport().size() if viewport else QSize(1180, 760)
        self.setMinimumSize(
            max(viewport_size.width() - 2, max_right + MARGIN + 1),
            max(viewport_size.height() - 2, max_bottom + MARGIN + 1),
        )
        self.resize(self.minimumSize())

    def paintEvent(self, event):
        super().paintEvent(event)
        if not self.customizing or (not self.guides_x and not self.guides_y):
            return
        painter = QPainter(self)
        pen = QPen(QColor('#bb5863'))
        pen.setWidth(1)
        pen.setStyle(Qt.DashLine)
        painter.setPen(pen)
        for x in self.guides_x:
            painter.drawLine(x, 0, x, self.height())
        for y in self.guides_y:
            painter.drawLine(0, y, self.width(), y)


class WorkspaceScrollArea(QScrollArea):
    """Single scroll owner for the whole character-sheet workspace."""
    def __init__(self, owner, canvas, parent=None):
        super().__init__(parent)
        self.owner = owner
        self.canvas = canvas
        self.setObjectName('workspaceScroll')
        self.setFrameShape(QFrame.NoFrame)
        self.setWidgetResizable(False)
        self.setWidget(canvas)
        self.setHorizontalScrollBarPolicy(Qt.ScrollBarAsNeeded)
        self.setVerticalScrollBarPolicy(Qt.ScrollBarAsNeeded)

    def resizeEvent(self, event):
        super().resizeEvent(event)
        self.canvas.refresh_extent()
