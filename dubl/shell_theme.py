"""Workspace design tokens and overrides shared by the native card shell."""
from .theme import fonts

def shell_style(size=11,light=False):
    body,display,mono=fonts()
    bg='#0e1014';surface='#191c23';inset='#13161b';border='#30343e';text='#eee9e1';muted='#a1a5b0';gold='#d0ae7e';selected='#39252e'
    if light:bg='#eeece8';surface='#faf9f6';inset='#f0eeea';border='#d5d1ca';text='#2b2c31';muted='#686975';gold='#895f32';selected='#efdbdf'
    return f'''
QWidget {{font-family:"{body}";font-size:{size}pt;}}
QWidget#workspace, QWidget#columnHost, QScrollArea#columnScroll {{background:{bg};border:none;}}
QWidget#shellHeader {{background:{bg};border-bottom:1px solid {border};}}
QLabel#shellBrand {{font-family:"{display}";font-size:{size+3}pt;font-weight:700;color:{text};}}
QLabel#heroName {{font-family:"{display}";font-size:{size+8}pt;font-weight:600;color:{text};}}
QLabel#heroMeta {{font-size:{max(9,size-1)}pt;color:{muted};}}
QLabel#eyebrow {{font-size:{max(8,size-3)}pt;font-weight:600;color:{muted};padding:2px 2px;}}
QPushButton#activeNav {{border:none;border-bottom:2px solid #bb5863;border-radius:0;background:transparent;padding:8px 4px;color:{text};}}
QToolButton#shellNav {{background:transparent;border:none;color:{muted};padding:8px 7px;}}
QToolButton#shellNav:hover {{background:{surface};color:{text};border-radius:6px;}}
QToolButton#primaryNav {{background:#a83948;color:white;border:1px solid #b7515e;border-radius:7px;padding:8px 14px;font-weight:600;}}
QPushButton#saveAction {{background:{surface};border:1px solid {border};padding:8px 14px;color:{text};}}
QLabel#saveState {{color:{muted};font-size:{max(8,size-2)}pt;padding:0 2px;}}
QLabel#saveState[state="saving"] {{color:{gold};}}
QLabel#saveState[state="error"] {{color:#d77b86;font-weight:600;}}
QLabel#saveState[state="saved"] {{color:{muted};}}
QFrame#sheetCard {{background:{surface};border:1px solid {border};border-radius:10px;}}
QWidget#cardHeader {{background:transparent;border:none;}}
QLabel#cardTitle {{font-weight:650;color:{text};font-size:{size}pt;}}
QToolButton#cardControl {{background:transparent;color:{muted};border:none;border-radius:5px;font-size:{size+2}pt;padding:0;}}
QToolButton#cardControl:hover {{background:{selected};color:{text};}}
QWidget#cardResize {{background:transparent;border-bottom:2px solid {border};margin:0 18px 3px 18px;}}
QWidget#cardResize:hover {{background:#a83948;border-radius:3px;}}
QSplitter::handle {{background:{bg};}}
QSplitter::handle:hover {{background:#6b3540;border-radius:4px;}}
QScrollArea, QScrollArea > QWidget > QWidget {{background:transparent;border:none;}}
QWidget#panelBody {{background:transparent;}}
QWidget#profileTop, QWidget#profileFields, QWidget#attributeMetaRow {{background:transparent;border:none;}}
QToolButton#portraitButton {{background:{inset};border:1px solid {border};border-radius:10px;color:{muted};font-size:{max(8,size-2)}pt;padding:4px;}}
QToolButton#portraitButton:hover {{border:1px solid #a95863;background:{selected};color:{text};}}
QToolButton#portraitButton:pressed {{background:{bg};}}
QWidget#attributeMetaRow QAbstractSpinBox {{max-width:92px;}}
QLabel {{color:{text};}}
QLabel#mutedLabel, QLabel#sidebarMeta {{color:{muted};font-size:{max(9,size-1)}pt;}}
QLineEdit, QAbstractSpinBox, QComboBox {{background:{inset};border:1px solid {border};border-radius:6px;padding:5px 7px;min-height:20px;}}
QLineEdit:focus, QAbstractSpinBox:focus, QComboBox:focus {{border:1px solid #d4868f;background:{inset};}}
QPushButton {{background:{inset};border:1px solid {border};border-radius:6px;padding:5px 9px;min-height:20px;font-weight:500;}}
QPushButton:focus, QToolButton:focus {{border:1px solid #d4868f;}}
QPushButton#quietButton {{background:transparent;color:{muted};border:1px solid {border};}}
QPushButton#quietButton:hover {{background:{selected};color:{text};}}
QToolButton#inlineMenu {{background:{inset};border:1px solid {border};border-radius:6px;padding:6px 8px;color:{text};}}
QToolButton#inlineMenu:hover {{background:{selected};}}
QPushButton#miniButton {{background:transparent;border:none;padding:0;min-width:27px;max-width:27px;min-height:27px;color:{muted};}}
QPushButton#numberStep {{background:transparent;border:none;color:{muted};padding:0;min-height:24px;}}
QPushButton#numberStep:hover {{background:{selected};color:{text};border:1px solid {border};}}
QFrame#attributeTile {{background:{inset};border:1px solid {border};border-radius:7px;}}
QLabel#attributeName {{font-size:{max(9,size-1)}pt;color:{muted};}}
QFrame#attributeTile QAbstractSpinBox {{background:transparent;border:none;font-family:"{mono}";font-size:{size+3}pt;font-weight:600;padding:0;}}
QLabel#attributeTotal {{color:{gold};font-size:{max(8,size-2)}pt;}}
QWidget#panelBody[density="compact"] QFrame#attributeTile QAbstractSpinBox, QWidget#panelBody[density="minimal"] QFrame#attributeTile QAbstractSpinBox {{font-size:{size+1}pt;}}
QWidget#panelBody[density="compact"] QLabel#attributeName, QWidget#panelBody[density="minimal"] QLabel#attributeName {{font-size:{max(8,size-2)}pt;}}
QFrame#metricTile {{background:{inset};border:1px solid {border};border-radius:7px;}}
QLabel#metricValue {{font-family:"{mono}";font-size:{size+4}pt;font-weight:600;color:{text};}}
QWidget#panelBody[density="compact"] QLabel#metricValue, QWidget#panelBody[density="minimal"] QLabel#metricValue {{font-size:{size+2}pt;}}
QLabel#metricLabel {{font-size:{max(8,size-2)}pt;color:{muted};}}
QFrame#resourceTile {{background:{inset};border:1px solid {border};border-radius:7px;}}
QLabel#resourceName {{font-size:{max(9,size-1)}pt;font-weight:600;color:{text};}}
QFrame#resourceTile QAbstractSpinBox {{padding:0;min-height:20px;border:none;background:transparent;font-family:"{mono}";font-weight:600;font-size:{size+2}pt;}}
QWidget#panelBody[density="compact"] QFrame#resourceTile QAbstractSpinBox, QWidget#panelBody[density="minimal"] QFrame#resourceTile QAbstractSpinBox {{font-size:{size+1}pt;}}
QProgressBar {{border:none;border-radius:2px;background:{border};}}
QProgressBar::chunk {{background:#b65866;border-radius:2px;}}
QProgressBar#staminaBar::chunk {{background:#b49b68;}}
QProgressBar#manaBar::chunk {{background:#8095c7;}}
QProgressBar#customBar::chunk {{background:#71a492;}}
QTableWidget#contentTable {{background:transparent;border:none;border-radius:0;padding:0;selection-background-color:{selected};}}
QTableWidget#contentTable::item {{padding:3px 6px;border:none;border-bottom:1px solid {border};}}
QWidget#panelBody[density="compact"] QTableWidget#contentTable::item {{padding:2px 5px;}}
QWidget#panelBody[density="minimal"] QTableWidget#contentTable::item {{padding:1px 4px;}}
QTableWidget#contentTable::item:selected {{background:{selected};color:{text};border-left:2px solid #b95865;}}
QHeaderView::section {{background:transparent;color:{muted};font-size:{max(8,size-2)}pt;font-weight:500;border:none;border-bottom:1px solid {border};padding:5px 6px;}}
QTextBrowser#structuredDetails {{background:{inset};border:1px solid {border};border-radius:8px;padding:9px;}}
QLabel#selectionPreview {{background:{inset};border-left:2px solid #8d4c57;border-radius:5px;padding:7px 9px;color:{muted};font-size:{max(9,size-1)}pt;}}
QWidget#panelBody[density="compact"] QLabel#selectionPreview, QWidget#panelBody[density="minimal"] QLabel#selectionPreview {{padding:5px 7px;font-size:{max(8,size-2)}pt;}}
QFrame#sheetCard[density="compact"] QLabel#cardTitle, QFrame#sheetCard[density="minimal"] QLabel#cardTitle {{font-size:{max(9,size-1)}pt;}}
QFrame#sheetCard[density="compact"] QToolButton#cardControl, QFrame#sheetCard[density="minimal"] QToolButton#cardControl {{font-size:{size+1}pt;}}
QWidget#panelBody[density="compact"] QPushButton, QWidget#panelBody[density="compact"] QToolButton#inlineMenu {{padding:4px 7px;min-height:18px;}}
QWidget#panelBody[density="minimal"] QPushButton, QWidget#panelBody[density="minimal"] QToolButton#inlineMenu {{padding:3px 6px;min-height:17px;}}


QPushButton#tableOverflow {{background:transparent;border:none;border-top:1px solid {border};border-radius:0;color:{muted};padding:5px 7px;text-align:left;min-height:18px;}}
QPushButton#tableOverflow:hover {{color:{text};background:{inset};}}
QScrollArea#columnScroll QScrollBar:vertical {{width:6px;background:transparent;margin:2px 0;}}
QScrollArea#columnScroll QScrollBar::handle:vertical {{background:{border};border-radius:3px;min-height:34px;}}
QScrollArea#columnScroll QScrollBar::add-line:vertical, QScrollArea#columnScroll QScrollBar::sub-line:vertical, QScrollArea#columnScroll QScrollBar::add-page:vertical, QScrollArea#columnScroll QScrollBar::sub-page:vertical {{height:0;background:transparent;}}
QFrame#emptyState {{background:{inset};border:1px dashed {border};border-radius:8px;}}
QLabel#emptyTitle {{font-weight:600;color:{text};}}
QScrollBar:vertical {{width:8px;background:transparent;}}
QScrollBar::handle:vertical {{background:{border};border-radius:3px;min-height:25px;}}

QScrollArea#workspaceScroll {{background:{bg};border:none;}}
QWidget#workspaceCanvas {{background:{bg};border:none;}}
QWidget#workspaceCanvas[customizing="true"] {{background:{bg};}}
QPushButton#layoutModeButton {{background:transparent;border:1px solid {border};border-radius:7px;padding:8px 12px;color:{muted};font-weight:600;}}
QPushButton#layoutModeButton:hover {{background:{surface};color:{text};}}
QPushButton#layoutModeButton[active="true"] {{background:#a83948;border-color:#b7515e;color:white;}}
QFrame#sheetCard[customizing="true"] {{border:1px solid #6f5960;}}
QFrame#sheetCard[layoutInvalid="true"] {{border:2px solid #d55d68;}}
QWidget#cardGeometryHandle {{background:transparent;}}
QFrame#sheetCard[customizing="true"] QWidget#cardGeometryHandle {{background:rgba(185,88,101,42);}}
QFrame#sheetCard[customizing="true"] QWidget#cardGeometryHandle:hover {{background:#b95865;}}
QScrollArea#workspaceScroll QScrollBar:vertical, QScrollArea#workspaceScroll QScrollBar:horizontal {{background:transparent;}}
QScrollArea#workspaceScroll QScrollBar::handle:vertical, QScrollArea#workspaceScroll QScrollBar::handle:horizontal {{background:{border};border-radius:3px;min-height:34px;min-width:34px;}}
'''
