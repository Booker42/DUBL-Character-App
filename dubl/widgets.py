"""Reusable native Qt controls."""
from PySide6.QtCore import Qt, Signal, QEvent, QTimer
from PySide6.QtWidgets import (
    QWidget,QHBoxLayout,QVBoxLayout,QLabel,QPushButton,QDoubleSpinBox,
    QAbstractSpinBox,QDialog,QDialogButtonBox,QFormLayout,QLineEdit,
    QPlainTextEdit,QComboBox,QCheckBox,QScrollArea,QListWidget,QListWidgetItem
)


class CompactSpinBox(QDoubleSpinBox):
    def textFromValue(self,value):
        text=super().textFromValue(value);decimal=self.locale().decimalPoint()
        if decimal in text:text=text.rstrip('0').rstrip(decimal)
        return text

class Number(QWidget):
    """Compact numeric control with explicit horizontal minus/plus buttons."""
    changed=Signal(float)
    def __init__(self,value=0,lo=-999999,hi=999999,decimals=0,parent=None):
        super().__init__(parent);self.setObjectName('numberControl')
        row=QHBoxLayout(self);row.setContentsMargins(0,0,0,0);row.setSpacing(5)
        self.spin=CompactSpinBox();self.spin.setDecimals(decimals);self.spin.setRange(lo,hi);self.spin.setButtonSymbols(QAbstractSpinBox.NoButtons);self.spin.setKeyboardTracking(False);self.spin.setAlignment(Qt.AlignCenter);self.spin.setMinimumWidth(58);self.spin.setMinimumHeight(30)
        self.spin.setValue(value or 0)
        minus=QPushButton('−');minus.setObjectName('numberStep');minus.setFixedSize(28,30);minus.setAutoRepeat(True);minus.setAccessibleName('Уменьшить');minus.clicked.connect(lambda:self.spin.setValue(self.spin.value()-1))
        plus=QPushButton('+');plus.setObjectName('numberStep');plus.setFixedSize(28,30);plus.setAutoRepeat(True);plus.setAccessibleName('Увеличить');plus.clicked.connect(lambda:self.spin.setValue(self.spin.value()+1))
        self.minus=minus;self.plus=plus;self._density='standard';self._hover_steps=False
        row.addWidget(minus);row.addWidget(self.spin,1);row.addWidget(plus)
        self.spin.valueChanged.connect(self.changed)
    def set_hover_steps(self,on=True):
        """Show step buttons only while the control is actively inspected.

        Dense tables are primarily read surfaces. Keeping +/- permanently
        visible on every row wastes a large amount of horizontal space, so
        skill-rank editors can opt into hover/focus chrome instead.
        """
        self._hover_steps=bool(on)
        for widget in (self,self.spin,self.minus,self.plus):
            widget.installEventFilter(self)
            widget.setMouseTracking(True)
        self._refresh_step_visibility()
    def eventFilter(self,obj,event):
        if self._hover_steps and event.type() in (QEvent.Enter,QEvent.Leave,QEvent.FocusIn,QEvent.FocusOut):
            QTimer.singleShot(0,self._refresh_step_visibility)
        return super().eventFilter(obj,event)
    def _refresh_step_visibility(self):
        if not self._hover_steps:return
        density=self._density or 'standard'
        show=density in ('expanded','standard') and (self.underMouse() or self.spin.hasFocus() or self.minus.underMouse() or self.plus.underMouse())
        self.minus.setVisible(show);self.plus.setVisible(show)
        self.updateGeometry()
    def set_density(self,state):
        """Adjust the editor chrome without changing its value or semantics.

        Compact/minimal states are deliberately read-first: the spin box remains
        directly editable, while the permanent +/- buttons disappear.
        """
        state=state or 'standard';self._density=state;self.setProperty('density',state)
        show_steps=state in ('expanded','standard')
        if self._hover_steps:
            show_steps=show_steps and (self.underMouse() or self.spin.hasFocus() or self.minus.underMouse() or self.plus.underMouse())
        self.minus.setVisible(show_steps);self.plus.setVisible(show_steps)
        if state=='expanded':
            self.minus.setFixedSize(26,30);self.plus.setFixedSize(26,30);self.spin.setMinimumWidth(48);self.spin.setMaximumWidth(16777215);self.spin.setMinimumHeight(30)
            self.layout().setSpacing(4)
        elif state=='standard':
            self.minus.setFixedSize(24,28);self.plus.setFixedSize(24,28);self.spin.setMinimumWidth(42);self.spin.setMaximumWidth(16777215);self.spin.setMinimumHeight(28)
            self.layout().setSpacing(3)
        elif state=='compact':
            self.spin.setMinimumWidth(42);self.spin.setMaximumWidth(68);self.spin.setMinimumHeight(26)
            self.layout().setSpacing(0)
        else:
            self.spin.setMinimumWidth(36);self.spin.setMaximumWidth(56);self.spin.setMinimumHeight(24)
            self.layout().setSpacing(0)
        self.updateGeometry()
    def value(self):return self.spin.value()
    def setValue(self,v):
        self.spin.blockSignals(True);self.spin.setValue(v or 0);self.spin.blockSignals(False)


def label(text,role=None):
    w=QLabel(text);w.setWordWrap(True);w.setTextFormat(Qt.PlainText);w.setTextInteractionFlags(Qt.TextSelectableByMouse)
    if role:w.setObjectName(role)
    return w


def button(text,fn,role=None):
    b=QPushButton(text)
    if role:b.setObjectName(role)
    b.clicked.connect(lambda _=False:fn());return b


def row(*widgets):
    w=QWidget();w.setObjectName('rowContainer');lay=QHBoxLayout(w);lay.setContentsMargins(0,0,0,0);lay.setSpacing(7)
    for x in widgets:lay.addWidget(x)
    return w


class Editor(QDialog):
    """Fields: (key, label, kind, default[, options/min/max])."""
    def __init__(self,parent,title,fields):
        super().__init__(parent);self.setWindowTitle(title);self.resize(600,600);self.inputs={}
        outer=QVBoxLayout(self);outer.setContentsMargins(14,14,14,14);outer.setSpacing(10)
        scroll=QScrollArea();scroll.setWidgetResizable(True);body=QWidget();body.setObjectName('panelBody');form=QFormLayout(body);form.setRowWrapPolicy(QFormLayout.WrapLongRows);form.setHorizontalSpacing(14);form.setVerticalSpacing(10);scroll.setWidget(body);outer.addWidget(scroll)
        for f in fields:
            key,title,kind,value,*opts=f
            if kind in ('int','float'):
                w=Number(value,*(opts or [-999999,999999]),decimals=2 if kind=='float' else 0)
            elif kind=='bool':w=QCheckBox();w.setChecked(bool(value))
            elif kind=='choice':
                w=QComboBox();w.addItems(opts[0]);w.setCurrentText(str(value))
            elif kind=='multi':
                w=QListWidget();w.setObjectName('multiChoice');w.setMaximumHeight(190);selected=set(value if isinstance(value,(list,tuple,set)) else [value] if value else [])
                for option in opts[0]:
                    item=QListWidgetItem(str(option));item.setFlags(item.flags()|Qt.ItemIsUserCheckable);item.setCheckState(Qt.Checked if option in selected else Qt.Unchecked);w.addItem(item)
            elif kind=='text':w=QPlainTextEdit(str(value or ''));w.setMinimumHeight(120)
            else:w=QLineEdit(str(value or ''))
            self.inputs[key]=(kind,w);form.addRow(label(title,'fieldLabel'),w)
        buttons=QDialogButtonBox(QDialogButtonBox.Save|QDialogButtonBox.Cancel);buttons.button(QDialogButtonBox.Save).setText('Сохранить');buttons.button(QDialogButtonBox.Save).setObjectName('primaryButton');buttons.button(QDialogButtonBox.Cancel).setText('Отмена');buttons.accepted.connect(self.accept);buttons.rejected.connect(self.reject);outer.addWidget(buttons)
    def values(self):
        out={}
        for k,(kind,w) in self.inputs.items():
            if kind=='int':out[k]=int(w.value())
            elif kind=='float':out[k]=w.value()
            elif kind=='bool':out[k]=w.isChecked()
            elif kind=='choice':out[k]=w.currentText()
            elif kind=='multi':out[k]=[w.item(i).text() for i in range(w.count()) if w.item(i).checkState()==Qt.Checked]
            elif kind=='text':out[k]=w.toPlainText()
            else:out[k]=w.text().strip()
        return out
