"""Central UI theme for Dubl.

Phase 1 v3 moves away from the native 2000-era menu-bar look toward a
compact application header and card-like dock chrome.  Dock title buttons stay
native QDockWidget sub-controls so dragging/docking behavior remains reliable,
but they get explicit, high-contrast icons and much larger hit targets.
"""
from __future__ import annotations

from pathlib import Path
from PySide6.QtGui import QFontDatabase

BODY_CANDIDATES = (
    "Inter",
    "Noto Sans",
    "IBM Plex Sans",
    "Segoe UI",
    "Liberation Sans",
    "DejaVu Sans",
)
DISPLAY_CANDIDATES = (
    "Marcellus",
    "Alegreya SC",
    "Noto Serif",
    "Cormorant Garamond",
    "Libre Baskerville",
    "Georgia",
    "Liberation Serif",
    "DejaVu Serif",
)
MONO_CANDIDATES = (
    "JetBrains Mono",
    "IBM Plex Mono",
    "Noto Sans Mono",
    "DejaVu Sans Mono",
)


def _pick(candidates: tuple[str, ...], fallback: str) -> str:
    installed = set(QFontDatabase.families())
    return next((name for name in candidates if name in installed), fallback)


def fonts() -> tuple[str, str, str]:
    return (
        _pick(BODY_CANDIDATES, "Sans Serif"),
        _pick(DISPLAY_CANDIDATES, "Serif"),
        _pick(MONO_CANDIDATES, "Monospace"),
    )


def _asset(name: str) -> str:
    return (Path(__file__).resolve().parent / "assets" / name).as_posix()


def midnight(font_size: int = 11) -> str:
    """Dark graphite + restrained crimson theme inspired by modern RPG tools."""
    body, display, mono = fonts()
    close_icon = _asset("dock_close.svg")
    float_icon = _asset("dock_float.svg")
    return f"""
/* ---------- global ---------- */
QWidget {{
    font-family: "{body}";
    font-size: {font_size}pt;
    color: #ECE9E5;
    background-color: #101216;
}}
QMainWindow, QDialog {{ background-color: #0C0E11; }}
QWidget#panelBody, QWidget#rowContainer, QWidget#numberControl {{ background: transparent; }}
QScrollArea, QScrollArea > QWidget > QWidget {{ background: transparent; border: none; }}

/* ---------- modern top header ---------- */
QWidget#appHeader {{
    background-color: #0A0C0F;
    border-bottom: 1px solid #292D33;
}}
QLabel#brandMark {{
    background-color: #A52B35;
    color: #FFFFFF;
    border: 1px solid #C34A54;
    border-radius: 8px;
    min-width: 30px;
    min-height: 30px;
    max-width: 30px;
    max-height: 30px;
    font-family: "{display}";
    font-size: {font_size + 4}pt;
    font-weight: 700;
    qproperty-alignment: AlignCenter;
}}
QLabel#brandText {{
    color: #F5F1EC;
    font-family: "{display}";
    font-size: {font_size + 3}pt;
    font-weight: 700;
    padding-right: 10px;
}}
QLabel#headerCharacter {{
    color: #F2EEE9;
    font-weight: 600;
    padding: 0 7px;
}}
QLabel#headerMeta {{
    color: #858C96;
    font-size: {max(9, font_size - 1)}pt;
    padding-right: 4px;
}}
QToolButton#navButton, QToolButton#viewButton {{
    background: transparent;
    color: #BFC4CB;
    border: 1px solid transparent;
    border-radius: 7px;
    padding: 7px 11px;
    min-height: 24px;
    font-weight: 600;
}}
QToolButton#navButton:hover, QToolButton#viewButton:hover {{
    background-color: #1A1E23;
    color: #FFFFFF;
    border-color: #30353C;
}}
QToolButton#addButton {{
    background-color: #8F2730;
    color: #FFFFFF;
    border: 1px solid #B13A44;
    border-radius: 7px;
    padding: 7px 12px;
    min-height: 24px;
    font-weight: 700;
}}
QToolButton#addButton:hover {{ background-color: #A9333D; border-color: #CF5660; }}
QToolButton#saveButton {{
    background-color: #20242A;
    color: #EDE9E4;
    border: 1px solid #373C44;
    border-radius: 7px;
    padding: 7px 12px;
    min-height: 24px;
    font-weight: 600;
}}
QToolButton#saveButton:hover {{ background-color: #292E35; border-color: #555C67; color: #FFFFFF; }}
QToolButton::menu-indicator {{ image: none; width: 0px; }}

/* ---------- dock cards ---------- */
QDockWidget {{
    background-color: #15181D;
    border: 1px solid #2A2F36;
    color: #F0ECE7;
}}
QDockWidget::title {{
    background-color: #1B1F25;
    color: #F1EDE8;
    font-family: "{body}";
    font-size: {font_size}pt;
    font-weight: 700;
    text-align: left;
    padding: 9px 74px 9px 11px;
    border-bottom: 1px solid #2D3239;
}}
QDockWidget::close-button, QDockWidget::float-button {{
    background-color: #252A31;
    border: 1px solid #383E47;
    border-radius: 6px;
    width: 25px;
    height: 25px;
    margin: 3px 3px 3px 0px;
    padding: 3px;
}}
QDockWidget::float-button {{ image: url("{float_icon}"); }}
QDockWidget::close-button {{ image: url("{close_icon}"); }}
QDockWidget::float-button:hover {{ background-color: #343A43; border-color: #59616D; }}
QDockWidget::close-button:hover {{ background-color: #7C262F; border-color: #B4444F; }}
QDockWidget::float-button:pressed, QDockWidget::close-button:pressed {{ background-color: #16191D; }}
QMainWindow::separator {{
    background: #090B0D;
    width: 7px;
    height: 7px;
}}
QMainWindow::separator:hover {{ background: #7E2B34; }}

/* ---------- text ---------- */
QLabel {{ color: #D6D2CD; background: transparent; }}
QLabel#fieldLabel, QLabel#statName {{ color: #9A9FA7; }}
QLabel#sectionLabel {{ color: #F4F0EB; font-weight: 700; padding-top: 3px; }}
QLabel#statResult, QLabel#statValue {{
    color: #F4DADD;
    background-color: #291A1D;
    border: 1px solid #553038;
    border-radius: 7px;
    padding: 4px 8px;
    font-weight: 700;
}}
QLabel#mutedLabel {{ color: #858B94; }}

/* ---------- inputs ---------- */
QLineEdit, QPlainTextEdit, QTextEdit, QTextBrowser, QAbstractSpinBox,
QComboBox, QListWidget, QTableWidget {{
    background-color: #111419;
    color: #EEEAE5;
    border: 1px solid #343A42;
    border-radius: 7px;
    padding: 6px 8px;
    selection-background-color: #6A3037;
    selection-color: #FFFFFF;
}}
QLineEdit:hover, QPlainTextEdit:hover, QTextEdit:hover, QTextBrowser:hover,
QAbstractSpinBox:hover, QComboBox:hover, QListWidget:hover, QTableWidget:hover {{ border-color: #505761; }}
QLineEdit:focus, QPlainTextEdit:focus, QTextEdit:focus, QTextBrowser:focus,
QAbstractSpinBox:focus, QComboBox:focus, QListWidget:focus, QTableWidget:focus {{
    border: 1px solid #C04A55;
    background-color: #14171C;
}}
QLineEdit::placeholder {{ color: #6E747C; }}
QTextBrowser#detailsPane {{
    background-color: #12151A;
    border: 1px solid #30363E;
    border-radius: 8px;
    padding: 9px 10px;
}}

/* ---------- buttons ---------- */
QPushButton {{
    background-color: #23272D;
    color: #EAE6E1;
    border: 1px solid #3A4048;
    border-radius: 7px;
    padding: 7px 10px;
    min-height: 18px;
    font-weight: 600;
}}
QPushButton:hover {{ background-color: #2D3239; border-color: #5B626D; color: #FFFFFF; }}
QPushButton:pressed {{ background-color: #191C21; border-color: #A13A44; }}
QPushButton:disabled {{ background-color: #191C20; color: #666B72; border-color: #2A2E34; }}
QPushButton#primaryButton {{ background-color: #932B34; border-color: #B6404A; color: #FFFFFF; font-weight: 700; }}
QPushButton#primaryButton:hover {{ background-color: #AA3540; border-color: #D15B65; }}
QPushButton#quietButton {{ background-color: #1A1E23; border-color: #30353C; color: #B5BAC1; }}
QPushButton#numberStep {{
    background-color: #20242A;
    color: #E5C2C5;
    border: 1px solid #3A4048;
    border-radius: 7px;
    padding: 0px;
    min-height: 26px;
    font-size: {font_size + 1}pt;
    font-weight: 700;
}}
QPushButton#numberStep:hover {{ background-color: #312226; border-color: #8D3B43; color: #FFFFFF; }}

/* ---------- combos ---------- */
QComboBox {{ padding-right: 24px; }}
QComboBox::drop-down {{
    subcontrol-origin: padding;
    subcontrol-position: top right;
    width: 24px;
    border: none;
    border-left: 1px solid #2E333A;
    background: #1A1E23;
}}
QComboBox QAbstractItemView {{
    background-color: #181B20;
    border: 1px solid #3B4149;
    padding: 4px;
    outline: 0;
    selection-background-color: #643038;
    selection-color: #FFFFFF;
}}

/* ---------- tables / lists ---------- */
QTableWidget {{ gridline-color: #262B31; alternate-background-color: #14171B; }}
QHeaderView::section {{
    background-color: #1D2127;
    color: #AEB3BA;
    border: none;
    border-right: 1px solid #2E333A;
    border-bottom: 1px solid #343941;
    padding: 7px 8px;
    font-weight: 700;
}}
QTableWidget::item, QListWidget::item {{ padding: 5px 7px; border: none; }}
QTableWidget::item:selected, QListWidget::item:selected {{ background-color: #5C2B32; color: #FFFFFF; }}
QTableWidget::item:hover, QListWidget::item:hover {{ background-color: #22262C; }}
QTableCornerButton::section {{ background-color: #1D2127; border: none; }}

/* ---------- dock tabs ---------- */
QTabBar::tab {{
    background-color: #111419;
    color: #8F959D;
    border: none;
    border-top: 1px solid #2B3037;
    padding: 8px 13px;
    margin-right: 1px;
}}
QTabBar::tab:hover {{ background-color: #1C2026; color: #E0DDD8; }}
QTabBar::tab:selected {{
    background-color: #1C2026;
    color: #F5F1EC;
    border-top: 2px solid #B83E49;
}}

/* ---------- checks ---------- */
QCheckBox {{ spacing: 7px; color: #D0CCC7; background: transparent; }}
QCheckBox:hover {{ color: #FFFFFF; }}

/* ---------- popup menus ---------- */
QMenu {{
    background-color: #171A1F;
    color: #E5E1DC;
    border: 1px solid #3A4048;
    padding: 7px;
}}
QMenu::item {{ padding: 8px 34px 8px 12px; border-radius: 5px; }}
QMenu::item:selected {{ background-color: #552A31; color: #FFFFFF; }}
QMenu::item:disabled {{ color: #666B72; }}
QMenu::separator {{ height: 1px; background: #30353C; margin: 6px 8px; }}
QMenu::indicator {{ width: 14px; height: 14px; }}

/* ---------- scrollbars ---------- */
QScrollBar:vertical {{ background: #0F1114; width: 10px; margin: 0; }}
QScrollBar::handle:vertical {{ background: #3A3F46; min-height: 32px; border-radius: 5px; margin: 2px; }}
QScrollBar::handle:vertical:hover {{ background: #595F68; }}
QScrollBar::add-line:vertical, QScrollBar::sub-line:vertical,
QScrollBar::add-page:vertical, QScrollBar::sub-page:vertical {{ height: 0; background: transparent; }}
QScrollBar:horizontal {{ background: #0F1114; height: 10px; margin: 0; }}
QScrollBar::handle:horizontal {{ background: #3A3F46; min-width: 32px; border-radius: 5px; margin: 2px; }}
QScrollBar::handle:horizontal:hover {{ background: #595F68; }}
QScrollBar::add-line:horizontal, QScrollBar::sub-line:horizontal,
QScrollBar::add-page:horizontal, QScrollBar::sub-page:horizontal {{ width: 0; background: transparent; }}

/* ---------- footer / dialogs ---------- */
QStatusBar {{
    background-color: #0C0E11;
    color: #8C929A;
    border-top: 1px solid #25292F;
    padding: 3px 7px;
}}
QStatusBar::item {{ border: none; }}
QToolTip {{ color: #F4F0EB; background-color: #20242A; border: 1px solid #555C65; padding: 7px 9px; }}
QDialogButtonBox QPushButton {{ min-width: 92px; }}
"""


def warm_light(font_size: int = 11) -> str:
    """Light companion theme compatible with the same modern header/layout."""
    body, display, mono = fonts()
    close_icon = _asset("dock_close_light.svg")
    float_icon = _asset("dock_float_light.svg")
    return f"""
QWidget {{ font-family: "{body}"; font-size: {font_size}pt; color: #2B2E33; background-color: #F2F1EF; }}
QMainWindow, QDialog {{ background-color: #E8E7E4; }}
QWidget#panelBody, QWidget#rowContainer, QWidget#numberControl {{ background: transparent; }}
QScrollArea, QScrollArea > QWidget > QWidget {{ background: transparent; border: none; }}
QWidget#appHeader {{ background: #FCFBF9; border-bottom: 1px solid #CBC9C5; }}
QLabel#brandMark {{ background: #A52B35; color: white; border: 1px solid #8F202A; border-radius: 8px; min-width: 30px; min-height: 30px; max-width: 30px; max-height: 30px; font-family: "{display}"; font-size: {font_size + 4}pt; font-weight: 700; qproperty-alignment: AlignCenter; }}
QLabel#brandText {{ color: #25272B; font-family: "{display}"; font-size: {font_size + 3}pt; font-weight: 700; padding-right: 10px; }}
QLabel#headerCharacter {{ color: #272A2F; font-weight: 700; padding: 0 7px; }}
QLabel#headerMeta {{ color: #777B82; font-size: {max(9, font_size - 1)}pt; padding-right: 4px; }}
QToolButton#navButton, QToolButton#viewButton {{ background: transparent; color: #52565D; border: 1px solid transparent; border-radius: 7px; padding: 7px 11px; min-height: 24px; font-weight: 600; }}
QToolButton#navButton:hover, QToolButton#viewButton:hover {{ background: #EBE9E5; border-color: #D1CEC8; color: #22252A; }}
QToolButton#addButton {{ background: #A52B35; color: white; border: 1px solid #8F202A; border-radius: 7px; padding: 7px 12px; min-height: 24px; font-weight: 700; }}
QToolButton#saveButton {{ background: #ECEAE6; color: #2B2E33; border: 1px solid #CAC7C1; border-radius: 7px; padding: 7px 12px; min-height: 24px; font-weight: 600; }}
QToolButton::menu-indicator {{ image: none; width: 0px; }}
QDockWidget {{ background: #FBFAF8; border: 1px solid #CAC7C1; }}
QDockWidget::title {{ background: #ECEAE6; color: #2B2E33; font-family: "{body}"; font-size: {font_size}pt; font-weight: 700; padding: 9px 74px 9px 11px; border-bottom: 1px solid #CCC9C3; }}
QDockWidget::close-button, QDockWidget::float-button {{ background: #F8F7F4; border: 1px solid #C9C6C0; border-radius: 6px; width: 25px; height: 25px; margin: 3px 3px 3px 0; padding: 3px; }}
QDockWidget::float-button {{ image: url("{float_icon}"); }}
QDockWidget::close-button {{ image: url("{close_icon}"); }}
QDockWidget::float-button:hover {{ background: #E6E3DE; border-color: #AAA69F; }}
QDockWidget::close-button:hover {{ background: #F0D7DA; border-color: #BB6068; }}
QMainWindow::separator {{ background: #D8D5D0; width: 7px; height: 7px; }}
QMainWindow::separator:hover {{ background: #B43B46; }}
QLabel {{ background: transparent; color: #3F4349; }}
QLabel#fieldLabel, QLabel#statName {{ color: #73777E; }}
QLabel#sectionLabel {{ color: #292C31; font-weight: 700; }}
QLabel#statResult, QLabel#statValue {{ color: #8E2932; background: #FAEDEF; border: 1px solid #E0B9BD; border-radius: 7px; padding: 4px 8px; font-weight: 700; }}
QLineEdit, QPlainTextEdit, QTextEdit, QTextBrowser, QAbstractSpinBox, QComboBox, QListWidget, QTableWidget {{ background: #FFFFFF; color: #2A2D32; border: 1px solid #C4C1BB; border-radius: 7px; padding: 6px 8px; selection-background-color: #E7BFC3; selection-color: #202226; }}
QLineEdit:focus, QPlainTextEdit:focus, QTextEdit:focus, QTextBrowser:focus, QAbstractSpinBox:focus, QComboBox:focus, QListWidget:focus, QTableWidget:focus {{ border: 1px solid #B6404A; }}
QPushButton {{ background: #E9E7E3; color: #2C2F34; border: 1px solid #C4C1BB; border-radius: 7px; padding: 7px 10px; min-height: 18px; font-weight: 600; }}
QPushButton:hover {{ background: #DFDCD7; border-color: #AAA69F; }}
QPushButton#primaryButton {{ background: #A52B35; border-color: #8F202A; color: #FFFFFF; font-weight: 700; }}
QPushButton#quietButton {{ background: #F2F0EC; color: #666A71; }}
QPushButton#numberStep {{ color: #8F2932; padding: 0; min-height: 26px; font-weight: 700; }}
QHeaderView::section {{ background: #EAE8E4; color: #5E6269; border: none; border-right: 1px solid #D0CDC7; border-bottom: 1px solid #C8C5BF; padding: 7px 8px; font-weight: 700; }}
QTableWidget {{ gridline-color: #D6D3CE; alternate-background-color: #F8F7F5; }}
QTableWidget::item:selected, QListWidget::item:selected, QMenu::item:selected {{ background: #E8C2C6; color: #26292E; }}
QTabBar::tab {{ background: #E7E5E1; color: #777B82; border: none; border-top: 1px solid #CBC8C2; padding: 8px 13px; }}
QTabBar::tab:selected {{ background: #FBFAF8; color: #292C31; border-top: 2px solid #B53A45; }}
QMenu {{ background: #FBFAF8; color: #303338; border: 1px solid #C8C5BF; padding: 7px; }}
QMenu::item {{ padding: 8px 34px 8px 12px; border-radius: 5px; }}
QMenu::item:selected {{ background: #F0DADD; color: #24272B; }}
QMenu::separator {{ height: 1px; background: #D5D2CC; margin: 6px 8px; }}
QStatusBar {{ background: #E9E7E3; color: #777B82; border-top: 1px solid #CECBC5; }}
QToolTip {{ color: #2A2D32; background: #FFFDF7; border: 1px solid #AAA69F; padding: 7px 9px; }}
"""


# Backwards-compatible name used by the application.
elegant_dark = midnight
