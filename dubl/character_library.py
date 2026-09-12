"""Qt character-library launcher and switcher."""
from __future__ import annotations

import base64
from datetime import datetime
from pathlib import Path

from PySide6.QtCore import Qt, Signal, QSize
from PySide6.QtGui import QIcon, QPixmap
from PySide6.QtWidgets import (
    QDialog, QWidget, QFrame, QLabel, QPushButton, QToolButton, QLineEdit,
    QVBoxLayout, QHBoxLayout, QGridLayout, QScrollArea, QFileDialog,
    QMessageBox, QSizePolicy, QSpacerItem
)

from .library_store import CharacterStore, CharacterSummary


def _portrait(blob: str) -> QPixmap:
    if not blob:
        return QPixmap()
    try:
        pix = QPixmap()
        pix.loadFromData(base64.b64decode(blob.encode("ascii"), validate=True))
        return pix
    except Exception:
        return QPixmap()


def _updated_text(timestamp: float) -> str:
    dt = datetime.fromtimestamp(timestamp)
    now = datetime.now()
    if dt.date() == now.date():
        return "Изменён сегодня · " + dt.strftime("%H:%M")
    if (now.date() - dt.date()).days == 1:
        return "Изменён вчера · " + dt.strftime("%H:%M")
    return "Изменён · " + dt.strftime("%d.%m.%Y")


class CharacterTile(QFrame):
    opened = Signal(str)
    menu_requested = Signal(str, object)

    def __init__(self, summary: CharacterSummary, current=False, parent=None):
        super().__init__(parent)
        self.summary = summary
        self.setObjectName("libraryCard")
        self.setProperty("current", bool(current))
        self.setCursor(Qt.PointingHandCursor)
        self.setMinimumWidth(260)
        self.setMaximumWidth(380)
        self.setSizePolicy(QSizePolicy.Expanding, QSizePolicy.Fixed)
        # Library cards are intentionally uniform.  The grid should never
        # stretch the last incomplete row to consume the viewport height.
        self.setFixedHeight(150)
        outer = QVBoxLayout(self)
        outer.setContentsMargins(14, 14, 14, 13)
        outer.setSpacing(11)

        top = QHBoxLayout(); top.setSpacing(12)
        portrait = QLabel(); portrait.setObjectName("libraryPortrait"); portrait.setFixedSize(74, 74); portrait.setAlignment(Qt.AlignCenter)
        pix = _portrait(summary.portrait)
        if pix.isNull():
            portrait.setText((summary.name[:1] or "Д").upper())
        else:
            portrait.setPixmap(pix.scaled(72, 72, Qt.KeepAspectRatioByExpanding, Qt.SmoothTransformation))
        top.addWidget(portrait)

        text = QVBoxLayout(); text.setSpacing(4)
        name = QLabel(summary.name); name.setObjectName("libraryName"); name.setWordWrap(True); name.setMaximumHeight(42); text.addWidget(name)
        concept = QLabel(summary.concept or "Без концепции"); concept.setObjectName("libraryConcept"); concept.setWordWrap(True); concept.setMaximumHeight(34); text.addWidget(concept)
        meta = QLabel(_updated_text(summary.updated)); meta.setObjectName("libraryMeta"); text.addWidget(meta)
        top.addLayout(text, 1)
        more = QToolButton(); more.setObjectName("libraryMore"); more.setText("⋯"); more.setFixedSize(30, 30)
        more.clicked.connect(lambda: self.menu_requested.emit(summary.id, more))
        top.addWidget(more, 0, Qt.AlignTop)
        outer.addLayout(top)

        bottom = QHBoxLayout(); bottom.setSpacing(8)
        if current:
            badge = QLabel("ОТКРЫТ"); badge.setObjectName("libraryCurrent"); bottom.addWidget(badge)
        bottom.addStretch(1)
        open_btn = QPushButton("Открыть"); open_btn.setObjectName("libraryOpen"); open_btn.clicked.connect(lambda: self.opened.emit(summary.id)); bottom.addWidget(open_btn)
        outer.addLayout(bottom)

    def mouseDoubleClickEvent(self, event):
        if event.button() == Qt.LeftButton:
            self.opened.emit(self.summary.id)
            event.accept()
            return
        super().mouseDoubleClickEvent(event)


class CharacterLibraryDialog(QDialog):
    """Card-based library shown at startup and from the character sheet."""

    def __init__(self, store: CharacterStore, current_id: str | None = None, parent=None):
        super().__init__(parent)
        self.store = store
        self.current_id = current_id
        self.selected_id: str | None = None
        self._cards: list[CharacterTile] = []
        self._summaries: list[CharacterSummary] = []
        self.setWindowTitle("Персонажи — Дубль")
        self.resize(1040, 720)
        self.setMinimumSize(760, 520)
        self.setModal(True)

        root = QVBoxLayout(self); root.setContentsMargins(24, 22, 24, 20); root.setSpacing(16)
        header = QHBoxLayout(); header.setSpacing(12)
        brand = QLabel("Д"); brand.setObjectName("libraryBrand"); brand.setAlignment(Qt.AlignCenter); header.addWidget(brand)
        titles = QVBoxLayout(); titles.setSpacing(2)
        title = QLabel("Персонажи"); title.setObjectName("libraryTitle"); titles.addWidget(title)
        subtitle = QLabel("Выберите лист — приложение сохранит изменения автоматически."); subtitle.setObjectName("librarySubtitle"); titles.addWidget(subtitle)
        header.addLayout(titles, 1)
        create = QPushButton("+ Новый персонаж"); create.setObjectName("libraryPrimary"); create.clicked.connect(self.create_character); header.addWidget(create)
        imp = QPushButton("Импортировать"); imp.clicked.connect(self.import_character); header.addWidget(imp)
        root.addLayout(header)

        search_row = QHBoxLayout(); search_row.setSpacing(8)
        self.search = QLineEdit(); self.search.setPlaceholderText("Найти персонажа…"); self.search.setClearButtonEnabled(True); self.search.textChanged.connect(self.refresh); search_row.addWidget(self.search, 1)
        root.addLayout(search_row)

        self.scroll = QScrollArea(); self.scroll.setWidgetResizable(True); self.scroll.setFrameShape(QFrame.NoFrame); self.scroll.setHorizontalScrollBarPolicy(Qt.ScrollBarAlwaysOff)
        self.host = QWidget(); self.host.setObjectName("libraryHost"); self.grid = QGridLayout(self.host); self.grid.setContentsMargins(0, 0, 0, 0); self.grid.setHorizontalSpacing(12); self.grid.setVerticalSpacing(12); self.grid.setAlignment(Qt.AlignTop | Qt.AlignLeft)
        self.scroll.setWidget(self.host); root.addWidget(self.scroll, 1)

        foot = QHBoxLayout(); self.count_label = QLabel(""); self.count_label.setObjectName("libraryMeta"); foot.addWidget(self.count_label); foot.addStretch(1)
        close = QPushButton("Закрыть"); close.clicked.connect(self.reject); foot.addWidget(close); root.addLayout(foot)

        self.setStyleSheet(self._style())
        self.refresh()

    def _style(self) -> str:
        return """
QDialog { background:#0e1014; color:#eee9e1; }
QWidget#libraryHost, QScrollArea { background:#0e1014; border:none; }
QLabel { color:#eee9e1; }
QLabel#libraryBrand { background:#a83948; color:white; border:1px solid #bd5360; border-radius:8px; min-width:34px; max-width:34px; min-height:34px; max-height:34px; font-size:17px; font-weight:700; }
QLabel#libraryTitle { font-size:24px; font-weight:700; }
QLabel#librarySubtitle, QLabel#libraryMeta, QLabel#libraryConcept { color:#989eaa; }
QLabel#libraryName { font-size:16px; font-weight:650; }
QLabel#libraryConcept { font-size:12px; }
QLabel#libraryMeta { font-size:11px; }
QLabel#libraryPortrait { background:#12151a; border:1px solid #343945; border-radius:9px; font-size:25px; font-weight:700; color:#d9b7bb; }
QLabel#libraryCurrent { color:#d6a8ae; background:#39252e; border:1px solid #653640; border-radius:5px; padding:3px 7px; font-size:9px; font-weight:700; }
QFrame#libraryCard { background:#191c23; border:1px solid #30343e; border-radius:10px; }
QFrame#libraryCard:hover { border:1px solid #525967; background:#1d2028; }
QFrame#libraryCard[current="true"] { border:1px solid #8b4550; }
QLineEdit { background:#13161b; border:1px solid #30343e; border-radius:7px; padding:8px 10px; min-height:22px; color:#eee9e1; }
QLineEdit:focus { border:1px solid #d4868f; }
QPushButton { background:#191c23; color:#eee9e1; border:1px solid #363b46; border-radius:7px; padding:8px 13px; min-height:20px; }
QPushButton:hover { background:#242832; border-color:#565d6b; }
QPushButton#libraryPrimary, QPushButton#libraryOpen { background:#a83948; border-color:#b7515e; color:white; font-weight:600; }
QPushButton#libraryPrimary:hover, QPushButton#libraryOpen:hover { background:#b84453; }
QToolButton#libraryMore { background:transparent; border:none; border-radius:6px; color:#a1a5b0; font-size:18px; }
QToolButton#libraryMore:hover { background:#30232a; color:#eee9e1; }
QMenu { background:#191c23; color:#eee9e1; border:1px solid #3a404b; padding:5px; }
QMenu::item { padding:7px 26px 7px 10px; border-radius:5px; }
QMenu::item:selected { background:#39252e; }
QScrollBar:vertical { width:7px; background:transparent; }
QScrollBar::handle:vertical { background:#343945; border-radius:3px; min-height:28px; }
QScrollBar::add-line:vertical, QScrollBar::sub-line:vertical, QScrollBar::add-page:vertical, QScrollBar::sub-page:vertical { height:0; background:transparent; }
"""

    def _clear_grid(self) -> None:
        while self.grid.count():
            item = self.grid.takeAt(0)
            widget = item.widget()
            if widget:
                widget.deleteLater()
        self._cards.clear()

    def _columns(self) -> int:
        width = max(1, self.scroll.viewport().width())
        return max(1, min(4, width // 300))

    def refresh(self, *_):
        self._summaries = self.store.list()
        query = self.search.text().casefold().strip() if hasattr(self, "search") else ""
        shown = [s for s in self._summaries if not query or query in (s.name + " " + s.concept).casefold()]
        self._clear_grid()
        cols = self._columns()
        if not shown:
            empty = QFrame(); empty.setObjectName("libraryCard"); lay = QVBoxLayout(empty); lay.setContentsMargins(20, 30, 20, 30)
            title = QLabel("Персонажей пока нет" if not query else "Ничего не найдено"); title.setObjectName("libraryName"); title.setAlignment(Qt.AlignCenter); lay.addWidget(title)
            desc = QLabel("Создайте новый лист или импортируйте существующий файл." if not query else "Попробуйте другой запрос."); desc.setObjectName("librarySubtitle"); desc.setAlignment(Qt.AlignCenter); lay.addWidget(desc)
            self.grid.addWidget(empty, 0, 0, 1, cols)
        else:
            rows = (len(shown) + cols - 1) // cols
            # Clear any stretch left by a previous reflow.
            for row_index in range(max(rows + 2, self.grid.rowCount() + 1)):
                self.grid.setRowStretch(row_index, 0)
            for i, summary in enumerate(shown):
                tile = CharacterTile(summary, summary.id == self.current_id)
                tile.opened.connect(self.choose)
                tile.menu_requested.connect(self.card_menu)
                self.grid.addWidget(tile, i // cols, i % cols, Qt.AlignTop)
                self._cards.append(tile)
            for col in range(cols):
                self.grid.setColumnStretch(col, 1)
            # A dedicated expanding spacer absorbs all unused vertical room.
            # This keeps an incomplete final row identical in height to every
            # other row instead of turning its cards into tall empty panels.
            self.grid.addItem(QSpacerItem(1, 1, QSizePolicy.Minimum, QSizePolicy.Expanding), rows, 0, 1, cols)
            self.grid.setRowStretch(rows, 1)
        self.count_label.setText(f"Персонажей: {len(self._summaries)}")

    def resizeEvent(self, event):
        old_cols = getattr(self, "_last_cols", 0)
        super().resizeEvent(event)
        cols = self._columns()
        if cols != old_cols:
            self._last_cols = cols
            # Reflow only when crossing a card-width breakpoint.
            if hasattr(self, "search"):
                self.refresh()

    def choose(self, character_id: str):
        self.selected_id = character_id
        self.accept()

    def create_character(self):
        self.choose(self.store.create())

    def import_character(self):
        path, _ = QFileDialog.getOpenFileName(self, "Импортировать персонажа", str(Path.home()), "Персонаж Дубль (*.dubl *.json *.bak);;Все файлы (*)")
        if not path:
            return
        try:
            self.choose(self.store.import_file(path))
        except Exception as exc:
            QMessageBox.critical(self, "Не удалось импортировать персонажа", str(exc))

    def export_character(self, character_id: str):
        summary = next((s for s in self._summaries if s.id == character_id), None)
        base = (summary.name if summary else "character").replace("/", "-").replace("\\", "-")
        path, selected = QFileDialog.getSaveFileName(self, "Экспортировать персонажа", str(Path.home() / f"{base}.dubl"), "Персонаж Дубль (*.dubl);;JSON (*.json)")
        if not path:
            return
        suffix = ".json" if "JSON" in selected else ".dubl"
        dest = Path(path)
        if not dest.suffix:
            dest = dest.with_suffix(suffix)
        try:
            self.store.export(character_id, dest)
        except Exception as exc:
            QMessageBox.critical(self, "Не удалось экспортировать персонажа", str(exc))

    def card_menu(self, character_id: str, anchor):
        from PySide6.QtWidgets import QMenu
        menu = QMenu(self)
        menu.addAction("Открыть", lambda: self.choose(character_id))
        menu.addAction("Дублировать", lambda: self.duplicate_character(character_id))
        menu.addAction("Экспортировать…", lambda: self.export_character(character_id))
        menu.addSeparator()
        delete = menu.addAction("Удалить", lambda: self.delete_character(character_id))
        if character_id == self.current_id:
            delete.setEnabled(False)
            delete.setToolTip("Сначала переключитесь на другого персонажа")
        menu.exec(anchor.mapToGlobal(anchor.rect().bottomLeft()))

    def duplicate_character(self, character_id: str):
        try:
            self.store.duplicate(character_id)
            self.refresh()
        except Exception as exc:
            QMessageBox.critical(self, "Не удалось дублировать персонажа", str(exc))

    def delete_character(self, character_id: str):
        summary = next((s for s in self._summaries if s.id == character_id), None)
        name = summary.name if summary else "этого персонажа"
        if QMessageBox.question(self, "Удалить персонажа?", f"Удалить «{name}» из локальной библиотеки?\n\nЭто действие нельзя отменить.", QMessageBox.Yes | QMessageBox.No, QMessageBox.No) != QMessageBox.Yes:
            return
        try:
            self.store.delete(character_id)
            self.refresh()
        except Exception as exc:
            QMessageBox.critical(self, "Не удалось удалить персонажа", str(exc))
