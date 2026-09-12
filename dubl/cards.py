"""Character-sheet cards for the free-form DUBL 0.14 workspace.

Each card owns a bounded user geometry (x/y/width/height).  The surrounding
workspace never reflows unrelated cards.  Drag/resize is enabled only while
explicit layout-customize mode is active.  Content may temporarily grow a card
vertically when room is available, without overwriting the user's saved size.
"""
from __future__ import annotations
import math
from dataclasses import dataclass
from PySide6.QtCore import Qt,QSize,QTimer,QRect,QEvent
from PySide6.QtGui import QAction
from PySide6.QtWidgets import (
    QWidget,QFrame,QVBoxLayout,QHBoxLayout,QGridLayout,QLabel,QToolButton,QMenu,
    QScrollArea,QDialog,QSizePolicy
)

EXPANDED='expanded'
STANDARD='standard'
COMPACT='compact'
MINIMAL='minimal'
DENSITIES=(EXPANDED,STANDARD,COMPACT,MINIMAL)


@dataclass(frozen=True)
class CardSpec:
    min_h:int
    preferred_h:int
    max_h:int
    compact_h:int
    expanded_h:int
    standard_w:int
    expanded_w:int
    min_w:int=280
    preferred_w:int=420
    max_w:int=980


CARD_SPECS={
    'profile':CardSpec(175,285,420,225,350,320,470,300,390,660),
    'resources':CardSpec(190,320,520,250,410,310,440,280,360,620),
    'derived':CardSpec(165,215,285,190,245,320,520,300,430,700),
    'attributes':CardSpec(220,300,460,250,365,380,560,360,650,1100),
    'skills':CardSpec(210,390,780,285,520,400,560,380,700,1250),
    'feats':CardSpec(165,320,800,235,470,380,520,350,620,1120),
    'magic':CardSpec(190,365,860,270,500,380,520,360,620,1150),
    'gear':CardSpec(165,310,760,235,450,360,500,340,580,1050),
    'cyber':CardSpec(125,255,620,190,400,340,460,320,520,900),
    'modifiers':CardSpec(125,245,560,180,380,340,460,320,520,900),
    'notes':CardSpec(145,275,720,205,450,340,460,320,540,920),
    'checks':CardSpec(130,250,560,185,375,340,460,320,520,900),
}
DEFAULT_SPEC=CardSpec(140,275,700,205,430,350,480,300,500,950)


def spec_for(key):
    if key.startswith('custom_'):return CardSpec(145,275,740,205,450,340,470,320,520,920)
    return CARD_SPECS.get(key,DEFAULT_SPEC)


def density_for_budget(key,width,height):
    spec=spec_for(key);width=max(0,int(width));height=max(0,int(height))
    if width < spec.min_w + 40 or height <= spec.min_h + 14:return MINIMAL
    if width < spec.standard_w or height < spec.compact_h:return COMPACT
    if width >= spec.expanded_w and height >= spec.expanded_h:return EXPANDED
    return STANDARD


class GeometryHandle(QWidget):
    """Workspace resize handle; visible only in explicit customize mode."""
    def __init__(self,card,direction):
        super().__init__(card);self.card=card;self.direction=direction;self.start=None
        self.setObjectName('cardGeometryHandle')
        cursor={'right':Qt.SizeHorCursor,'bottom':Qt.SizeVerCursor,'corner':Qt.SizeFDiagCursor}[direction]
        self.setCursor(cursor)
        self.setToolTip('Изменить размер карточки')
        self.hide()
    def mousePressEvent(self,e):
        if e.button()==Qt.LeftButton and self.card.can_customize():
            self.start=e.globalPosition().toPoint();self.card.begin_workspace_resize(self.start,self.direction);e.accept()
    def mouseMoveEvent(self,e):
        if self.start is not None:
            self.card.preview_workspace_resize(e.globalPosition().toPoint());e.accept()
    def mouseReleaseEvent(self,e):
        if self.start is not None:
            self.start=None;self.card.finish_workspace_resize();e.accept()


class Card(QFrame):
    """Independent card placed directly on the free-form workspace canvas."""
    def __init__(self,owner,key,title,body):
        super().__init__(getattr(owner,'workspace_canvas',None));self.owner=owner;self.key=key;self.title=title;self.body=body
        self.collapsed=False;self.user_height=0;self.user_width=0;self.user_x=0;self.user_y=0;self.popup=None;self.density=None
        self._dragging=False;self._resizing=False;self._locked=False;self._customizing=False;self._syncing=False;self._width_preview=False
        self._interaction_origin=None;self._interaction_start=None;self._resize_direction=None
        self._fit_timer=QTimer(self);self._fit_timer.setSingleShot(True);self._fit_timer.setInterval(0);self._fit_timer.timeout.connect(self.sync_height)
        self.setObjectName('sheetCard');self.setProperty('density',STANDARD);self.setProperty('customizing',False);self.setProperty('layoutInvalid',False)
        self.setSizePolicy(QSizePolicy.Fixed,QSizePolicy.Fixed)
        self.outer=QVBoxLayout(self);self.outer.setContentsMargins(0,0,0,0);self.outer.setSpacing(0)
        top=QWidget();top.setObjectName('cardHeader');self.header=top;head=QHBoxLayout(top);self.header_layout=head;head.setContentsMargins(16,10,10,9);head.setSpacing(5)
        self.title_label=QLabel(title);self.title_label.setObjectName('cardTitle');self.title_label.setToolTip(title);self.title_label.setSizePolicy(QSizePolicy.Ignored,QSizePolicy.Preferred);head.addWidget(self.title_label,1)
        self.fold=self.control('−','Свернуть / развернуть',self.toggle_fold);head.addWidget(self.fold)
        self.float_button=self.control('↗','Открыть отдельным окном',self.detach);head.addWidget(self.float_button)
        self.more=self.control('⋯','Настройки карточки');menu=QMenu(self.more);self.more.setMenu(menu);self.more.setPopupMode(QToolButton.InstantPopup)
        menu.addAction('Свернуть / развернуть',self.toggle_fold);menu.addAction('Рекомендуемый размер',self.reset_user_size);menu.addAction('Открыть отдельным окном',self.detach);menu.addSeparator();menu.addAction('Скрыть карточку',self.hide_card);head.addWidget(self.more)
        self.outer.addWidget(top);self.outer.addWidget(body,1)
        # Header/title are the drag surface. Buttons remain ordinary controls.
        self.header.installEventFilter(self);self.title_label.installEventFilter(self)
        self.resize_right=GeometryHandle(self,'right');self.resize_bottom=GeometryHandle(self,'bottom');self.resize_corner=GeometryHandle(self,'corner');self.grip=self.resize_bottom
        self.toggle=QAction(title,owner);self.toggle.setCheckable(True);self.toggle.setChecked(True);self.toggle.triggered.connect(lambda on:self.show_card() if on else self.hide_card())
        QTimer.singleShot(0,self.update_density)
    def spec(self):return spec_for(self.key)
    def preferred_height(self):return self.spec().preferred_h
    def preferred_width(self):return self.spec().preferred_w
    def min_user_height(self):return self.spec().min_h
    def max_user_height(self):return self.spec().max_h
    def min_user_width(self):return self.spec().min_w
    def max_user_width(self):return self.spec().max_w
    def clamp_user_height(self,h):return max(self.min_user_height(),min(self.max_user_height(),int(h)))
    def clamp_user_width(self,w):return max(self.min_user_width(),min(self.max_user_width(),int(w)))
    def can_customize(self):return self._customizing and not self._locked and self.popup is None
    def control(self,text,tip,fn=None):
        b=QToolButton();b.setText(text);b.setObjectName('cardControl');b.setToolTip(tip);b.setAccessibleName(tip);b.setFixedSize(27,27)
        if fn:b.clicked.connect(fn)
        return b
    def sizeHint(self):return QSize(self.user_width or self.preferred_width(),48 if self.collapsed or self.popup else self.effective_target_height())
    def minimumSizeHint(self):return QSize(self.min_user_width(),48 if self.collapsed else self.min_user_height())
    def eventFilter(self,obj,event):
        if obj in (self.header,self.title_label) and self.can_customize():
            if event.type()==QEvent.MouseButtonPress and event.button()==Qt.LeftButton:
                self.begin_workspace_move(event.globalPosition().toPoint());return True
            if event.type()==QEvent.MouseMove and self._dragging:
                self.preview_workspace_move(event.globalPosition().toPoint());return True
            if event.type()==QEvent.MouseButtonRelease and event.button()==Qt.LeftButton and self._dragging:
                self.finish_workspace_move();return True
        return super().eventFilter(obj,event)
    def resizeEvent(self,event):
        super().resizeEvent(event);self._position_handles()
        if not (self._resizing or self._dragging) and event.oldSize().width()!=event.size().width():
            self.update_density(preview=True);QTimer.singleShot(120,lambda:self.update_density() if not self._resizing else None)
    def _position_handles(self):
        w,h=self.width(),self.height();edge=9;corner=16
        self.resize_right.setGeometry(max(0,w-edge),34,edge,max(1,h-34-corner))
        self.resize_bottom.setGeometry(18,max(0,h-edge),max(1,w-18-corner),edge)
        self.resize_corner.setGeometry(max(0,w-corner),max(0,h-corner),corner,corner)
        self.resize_right.raise_();self.resize_bottom.raise_();self.resize_corner.raise_()
    def _polish(self,obj):
        try:obj.style().unpolish(obj);obj.style().polish(obj);obj.update()
        except RuntimeError:pass
    def chrome_height(self):return self.header.sizeHint().height()+2
    def density_budget(self):return max(self.min_user_width(),self.user_width or self.width()),self.user_height or self.preferred_height()
    def needed_height(self):
        try:
            if self.body.layout():self.body.layout().activate()
            hint=max(self.body.sizeHint().height(),self.body.minimumSizeHint().height())
        except RuntimeError:hint=max(70,self.min_user_height()-self.chrome_height())
        return max(self.min_user_height(),int(hint)+self.chrome_height()+2)
    def effective_target_height(self):
        if self.collapsed or self.popup:return 48
        requested=self.user_height or self.preferred_height()
        return self.clamp_user_height(max(requested,self.needed_height()))
    def request_content_fit(self):
        if not self.collapsed and not self.popup and not self._dragging and not self._resizing:self._fit_timer.start()
    def sync_height(self):
        if self._syncing or self._dragging or self._resizing or self.collapsed or self.popup:return
        self._syncing=True
        try:
            target=self.effective_target_height()
            if hasattr(self.owner,'apply_card_content_height'):
                self.owner.apply_card_content_height(self,target)
            elif self.height()!=target:self.resize(self.width(),target)
        finally:self._syncing=False
    def update_density(self,force=None,preview=False):
        if self.collapsed:state=MINIMAL;width=max(self.min_user_width(),self.width());height=48
        elif self.popup:state=EXPANDED;width=max(500,self.width());height=max(600,self.height())
        else:
            width,height=self.density_budget();state=force or density_for_budget(self.key,width,height)
        changed=state!=self.density;self.density=state;self.setProperty('density',state);self.body.setProperty('density',state)
        if state==EXPANDED:margins=(16,10,10,9);control=27
        elif state==STANDARD:margins=(15,9,9,8);control=26
        elif state==COMPACT:margins=(12,7,8,6);control=24
        else:margins=(10,6,7,5);control=22
        self.header_layout.setContentsMargins(*margins);self.fold.setFixedSize(control,control);self.float_button.setFixedSize(control,control);self.more.setFixedSize(control,control)
        self.float_button.setVisible(state==EXPANDED and self.popup is None);self.fold.setVisible(not self.popup)
        if changed:self._polish(self);self._polish(self.body)
        fn=getattr(self.body,'set_density',None)
        if callable(fn):fn(state,width,max(0,height-self.chrome_height()))
        if not preview:QTimer.singleShot(0,self.sync_height)
    def set_workspace_geometry(self,rect:QRect,save=True):
        w=self.clamp_user_width(rect.width());h=self.clamp_user_height(rect.height()) if not self.collapsed else 48
        x=max(0,int(rect.x()));y=max(0,int(rect.y()))
        if save:self.user_x,self.user_y,self.user_width=x,y,w;self.user_height=self.clamp_user_height(rect.height())
        self.setGeometry(x,y,w,h);self._position_handles();self.update_density(preview=True)
    def begin_workspace_move(self,global_pos):
        if not self.can_customize():return
        self._dragging=True;self._fit_timer.stop();self._interaction_start=global_pos;self._interaction_origin=QRect(self.geometry());self.raise_();self.header.setCursor(Qt.ClosedHandCursor)
    def preview_workspace_move(self,global_pos):
        if not self._dragging:return
        delta=global_pos-self._interaction_start;rect=QRect(self._interaction_origin);rect.translate(delta)
        canvas=getattr(self.owner,'workspace_canvas',None)
        if canvas:rect=canvas.snap_move(self,rect)
        self.setGeometry(rect);self.setProperty('layoutInvalid',bool(canvas and canvas.conflicts(self,rect)));self._polish(self)
        if canvas:canvas.refresh_extent()
    def finish_workspace_move(self):
        if not self._dragging:return
        canvas=getattr(self.owner,'workspace_canvas',None);rect=QRect(self.geometry())
        if canvas and canvas.conflicts(self,rect):rect=QRect(self._interaction_origin);self.setGeometry(rect)
        self.user_x,self.user_y=rect.x(),rect.y();self._dragging=False;self._interaction_origin=None;self._interaction_start=None;self.header.setCursor(Qt.OpenHandCursor if self.can_customize() else Qt.ArrowCursor)
        self.setProperty('layoutInvalid',False);self._polish(self)
        if canvas:canvas.clear_guides();canvas.refresh_extent()
        self.owner.layout_changed()
    def begin_workspace_resize(self,global_pos,direction):
        if not self.can_customize() or self.collapsed:return
        self._resizing=True;self._fit_timer.stop();self._interaction_start=global_pos;self._interaction_origin=QRect(self.geometry());self._resize_direction=direction;self.raise_()
    def preview_workspace_resize(self,global_pos):
        if not self._resizing:return
        delta=global_pos-self._interaction_start;origin=self._interaction_origin
        w=origin.width()+delta.x() if self._resize_direction in ('right','corner') else origin.width()
        h=origin.height()+delta.y() if self._resize_direction in ('bottom','corner') else origin.height()
        w=self.clamp_user_width(w);h=self.clamp_user_height(h);rect=QRect(origin.x(),origin.y(),w,h)
        canvas=getattr(self.owner,'workspace_canvas',None)
        if canvas:rect=canvas.snap_resize(self,rect,self._resize_direction in ('right','corner'),self._resize_direction in ('bottom','corner'))
        rect.setWidth(self.clamp_user_width(rect.width()));rect.setHeight(self.clamp_user_height(rect.height()))
        self.user_width,self.user_height=rect.width(),rect.height()  # live presentation budget; committed/reverted on release
        self.setGeometry(rect);self.update_density(preview=True)
        self.setProperty('layoutInvalid',bool(canvas and canvas.conflicts(self,rect)));self._polish(self)
        if canvas:canvas.refresh_extent()
    def finish_workspace_resize(self):
        if not self._resizing:return
        canvas=getattr(self.owner,'workspace_canvas',None);rect=QRect(self.geometry())
        if canvas and canvas.conflicts(self,rect):
            rect=QRect(self._interaction_origin);self.setGeometry(rect)
        self.user_x,self.user_y,self.user_width,self.user_height=rect.x(),rect.y(),rect.width(),self.clamp_user_height(rect.height())
        self._resizing=False;self._interaction_origin=None;self._interaction_start=None;self._resize_direction=None
        self.setProperty('layoutInvalid',False);self._polish(self);self.update_density()
        if canvas:canvas.clear_guides();canvas.refresh_extent()
        self.owner.layout_changed()
    def reset_user_size(self):
        self.user_width=self.preferred_width();self.user_height=self.preferred_height();rect=QRect(self.x(),self.y(),self.user_width,self.user_height)
        canvas=getattr(self.owner,'workspace_canvas',None)
        if canvas and canvas.conflicts(self,rect):
            rect=self.owner.find_free_geometry(self,rect)
        self.setGeometry(rect);self.user_x,self.user_y=rect.x(),rect.y();self.update_density();self.owner.layout_changed()
        if canvas:canvas.refresh_extent()
    def reset_user_height(self):self.reset_user_size()
    def set_user_height(self,height):
        self.user_height=self.clamp_user_height(height or self.preferred_height());self.resize(self.width(),self.user_height);self.update_density()
    def resize_to(self,height):self.set_user_height(height or self.preferred_height());self.owner.layout_changed()
    @property
    def manual_height(self):return self.user_height
    @manual_height.setter
    def manual_height(self,v):self.user_height=int(v or 0)
    def set_collapsed(self,on):
        self.collapsed=bool(on);self.body.setVisible(not on and self.popup is None);self.fold.setText('+' if on else '−')
        if on:self.resize(self.width(),48);self.density=MINIMAL
        else:
            QTimer.singleShot(0,self.update_density)
            if hasattr(self.owner,'ensure_card_nonoverlap'):QTimer.singleShot(0,lambda:self.owner.ensure_card_nonoverlap(self))
        self.set_customize(self._customizing);self.updateGeometry()
        if hasattr(self.owner,'workspace_canvas'):self.owner.workspace_canvas.refresh_extent()
    def toggle_fold(self):self.set_collapsed(not self.collapsed);self.owner.layout_changed()
    def apply_height(self):QTimer.singleShot(0,self.update_density)
    def hide_card(self):
        if self.popup:self.popup.close()
        self.hide();self.toggle.setChecked(False);self.owner.layout_changed()
        if hasattr(self.owner,'workspace_canvas'):self.owner.workspace_canvas.refresh_extent()
    def show_card(self):
        self.show();self.toggle.setChecked(True);QTimer.singleShot(0,self.update_density)
        if hasattr(self.owner,'ensure_card_nonoverlap'):QTimer.singleShot(0,lambda:self.owner.ensure_card_nonoverlap(self))
        self.owner.layout_changed()
        if hasattr(self.owner,'workspace_canvas'):self.owner.workspace_canvas.refresh_extent()
    def raise_(self):
        self.show_card();self.set_collapsed(False);super().raise_()
    def detach(self):
        if self.popup:self.popup.close();return
        d=QDialog(self.owner);d.setWindowTitle(self.title);d.resize(max(560,self.width()),720);d.setAttribute(Qt.WA_DeleteOnClose);v=QVBoxLayout(d);v.setContentsMargins(12,12,12,12)
        self.outer.removeWidget(self.body);outside=QScrollArea();outside.setWidgetResizable(True);outside.setFrameShape(QFrame.NoFrame);outside.setHorizontalScrollBarPolicy(Qt.ScrollBarAlwaysOff);outside.setWidget(self.body);v.addWidget(outside);self.popup=d;self.resize(self.width(),48)
        fn=getattr(self.body,'set_density',None)
        if callable(fn):fn(EXPANDED,max(500,d.width()-40),max(600,d.height()-40))
        def returned(*_):
            outside.takeWidget();self.outer.insertWidget(1,self.body,1);self.popup=None;self.body.setVisible(not self.collapsed);self.set_customize(self._customizing);self.owner.layout_changed();QTimer.singleShot(0,self.update_density)
        d.finished.connect(returned);d.show();self.owner.layout_changed()
    def set_customize(self,on):
        self._customizing=bool(on);enabled=self.can_customize();self.setProperty('customizing',enabled)
        self.header.setCursor(Qt.OpenHandCursor if enabled else Qt.ArrowCursor)
        self.resize_right.setVisible(enabled and not self.collapsed);self.resize_bottom.setVisible(enabled and not self.collapsed);self.resize_corner.setVisible(enabled and not self.collapsed)
        self._polish(self)
    def lock(self,on):self._locked=bool(on);self.set_customize(self._customizing)

class AttributeTile(QFrame):
    """One attribute that recomposes instead of merely shrinking."""
    def __init__(self,name,number,total):
        super().__init__();self.setObjectName('attributeTile');self.name=QLabel(name);self.name.setObjectName('attributeName');self.name.setWordWrap(False);self.number=number;self.total=QLabel('');self.total.setObjectName('attributeTotal');self.total_value=0;self.density=None
        self.grid=QGridLayout(self);self.grid.setContentsMargins(7,6,7,6);self.grid.setHorizontalSpacing(5);self.grid.setVerticalSpacing(2);self.set_density(STANDARD)
    def set_total(self,value):
        self.total_value=value;self._update_total_text()
    def _update_total_text(self):
        self.total.setText((f'Итог {self.total_value:g}' if self.density in (EXPANDED,STANDARD) else f'{self.total_value:g}'))
    def set_density(self,state):
        if state==self.density:return
        self.density=state;self.setProperty('density',state)
        for w in (self.name,self.number,self.total):self.grid.removeWidget(w);w.show()
        self.number.set_density(state)
        if state==EXPANDED:
            self.grid.setContentsMargins(6,4,6,4);self.grid.setHorizontalSpacing(4);self.grid.setVerticalSpacing(0)
            self.name.setAlignment(Qt.AlignCenter);self.total.setAlignment(Qt.AlignCenter)
            self.grid.addWidget(self.name,0,0);self.grid.addWidget(self.number,1,0);self.grid.addWidget(self.total,2,0)
        elif state==STANDARD:
            self.grid.setContentsMargins(6,3,6,3);self.grid.setHorizontalSpacing(4);self.grid.setVerticalSpacing(0)
            self.name.setAlignment(Qt.AlignLeft|Qt.AlignVCenter);self.total.setAlignment(Qt.AlignRight|Qt.AlignVCenter)
            self.grid.addWidget(self.name,0,0);self.grid.addWidget(self.total,0,1);self.grid.addWidget(self.number,1,0,1,2)
        else:
            self.grid.setContentsMargins(6,3,6,3);self.grid.setHorizontalSpacing(4);self.grid.setVerticalSpacing(0)
            self.name.setAlignment(Qt.AlignLeft|Qt.AlignVCenter);self.total.setAlignment(Qt.AlignRight|Qt.AlignVCenter)
            self.grid.addWidget(self.name,0,0);self.grid.addWidget(self.number,0,1);self.grid.addWidget(self.total,0,2)
            self.grid.setColumnStretch(0,1)
        self._update_total_text();self.updateGeometry()
    def sizeHint(self):
        return QSize(108,{EXPANDED:68,STANDARD:56,COMPACT:36,MINIMAL:32}.get(self.density,56))
    def minimumSizeHint(self):return QSize(82,32)


class AttributeGrid(QWidget):
    """Responsive attribute collection: cards when roomy, dense rows when not."""
    def __init__(self):
        super().__init__();self.tiles=[];self.grid=QGridLayout(self);self.grid.setContentsMargins(0,0,0,0);self.grid.setSpacing(5);self.count=4;self.density=STANDARD;self.setObjectName('panelBody')
    def add_tile(self,tile):self.tiles.append(tile);self._relayout(max(300,self.width()))
    def cols(self,width):
        if self.density==MINIMAL:return 4 if width>=400 else 2 if width>=245 else 1
        if self.density==COMPACT:return 4 if width>=430 else 2 if width>=245 else 1
        return 4 if width>=440 else 2
    def set_density(self,state,width=None):
        self.density=state
        for tile in self.tiles:tile.set_density(state)
        self._relayout(width or self.width());self.updateGeometry()
    def _relayout(self,width):
        cols=self.cols(max(1,width));self.count=cols
        for t in self.tiles:self.grid.removeWidget(t)
        for i,t in enumerate(self.tiles):self.grid.addWidget(t,i//cols,i%cols)
        for i in range(4):self.grid.setColumnStretch(i,1 if i<cols else 0)
    def hasHeightForWidth(self):return True
    def heightForWidth(self,width):
        if not self.tiles:return 0
        cols=self.cols(width);rows=math.ceil(len(self.tiles)/cols);tile_h=max(t.sizeHint().height() for t in self.tiles)
        return rows*tile_h+max(0,rows-1)*self.grid.verticalSpacing()
    def sizeHint(self):return QSize(490,self.heightForWidth(max(240,self.width())))
    def minimumSizeHint(self):return QSize(220,72)
    def resizeEvent(self,event):
        super().resizeEvent(event);cols=self.cols(event.size().width())
        if cols!=self.count:self._relayout(event.size().width());self.updateGeometry()
