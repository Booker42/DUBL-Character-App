"""The default character workspace. Rules and editors remain in the existing modules."""
from __future__ import annotations
import copy,base64
from pathlib import Path
from PySide6.QtCore import Qt,QTimer,QByteArray,QRect,QBuffer,QIODevice,QSize
from PySide6.QtGui import QFont,QColor,QPixmap,QIcon
from PySide6.QtWidgets import (QApplication,QWidget,QFrame,QVBoxLayout,QHBoxLayout,QGridLayout,QLabel,QLineEdit,QToolButton,QPushButton,QMenu,QScrollArea,QSplitter,QSizePolicy,QProgressBar,QCheckBox,QPlainTextEdit,QDialog,QTextBrowser,QMessageBox,QTableWidget,QFileDialog)
from .app import Window
from .model import ATTRS,CATALOG,number,fresh
from .widgets import Number,label,button,row
from .cards import Card,AttributeGrid,AttributeTile,EXPANDED,STANDARD,COMPACT,MINIMAL
from .details import Details
from .catalog import description
from . import engine
from .theme import elegant_dark,warm_light,fonts
from .shell_theme import shell_style
from .workspace import WorkspaceCanvas,WorkspaceScrollArea,GAP,MARGIN

SECONDARY={'cyber','modifiers','notes','checks'}

def default_workspace_geometry(window_width=1500):
    usable=max(1260,int(window_width)-42)
    gap=12;left=300;right=390;center=max(520,usable-left-right-gap*2)
    x1=MARGIN;x2=x1+left+gap;x3=x2+center+gap
    return {
        'profile':QRect(x1,18,left,285),'resources':QRect(x1,315,left,320),'derived':QRect(x1,647,left,215),
        'attributes':QRect(x2,18,center,300),'skills':QRect(x2,330,center,390),'feats':QRect(x2,732,center,320),
        'magic':QRect(x3,18,right,365),'gear':QRect(x3,395,right,310),
        'cyber':QRect(x3,717,right,255),'modifiers':QRect(x3,984,right,245),'notes':QRect(x3,1241,right,275),'checks':QRect(x3,1528,right,250),
    }

class CharacterWindow(Window):
    def __init__(self,state=None,autosave=True,store=None,character_id=None):
        self.store=store
        self.library_id=character_id or ((state or {}).get('id') if isinstance(state,dict) else None)
        self.columns=[];self.scrolls=[];self.tables={};self.details={};self.selections={};self._pending_scrolls=[];self.workspace_canvas=None;self.workspace_area=None;self.customize_mode=False
        if state is None and self.store and self.library_id:
            state=self.store.load(self.library_id)
        super().__init__(state,autosave)
        if self.store:
            self.library_id=str(self.s.get('id'))
            self.path=self.store.character_path(self.library_id)
            self.auto_path=self.path
        self.statusBar().hide()
        self.setMinimumSize(1120,680)
        if not self.s['ui'].get('shell'):self.resize(1500,980)
    def action(self,*args,**kwargs):
        a=super().action(*args,**kwargs);self.addAction(a);return a
    def make_menus(self):
        super().make_menus()
        self.lock_action.setText('Закрепить размеры и расположение')
        header=QWidget();header.setObjectName('shellHeader');outer=QVBoxLayout(header);outer.setContentsMargins(24,0,24,0);outer.setSpacing(0)
        top=QHBoxLayout();top.setContentsMargins(0,12,0,12);top.setSpacing(14)
        mark=label('Д','brandMark');top.addWidget(mark);self.brand_word=label('ДУБЛЬ','shellBrand');top.addWidget(self.brand_word)
        selected=button('Лист персонажа',self.go_home,'activeNav');top.addWidget(selected)
        catalog=self.menu_button('Каталог',self.reference_menu);top.addWidget(catalog)
        self.characters_menu=QMenu('Персонажи',self);self.characters_menu.aboutToShow.connect(self.populate_characters_menu)
        top.addStretch();top.addWidget(self.menu_button('Персонажи',self.characters_menu));top.addWidget(self.menu_button('+ Добавить',self.add_menu,'primaryNav'))
        setup=QMenu(self);setup.addMenu(self.view).setText('Карточки');setup.addMenu(self.settings).setText('Оформление')
        self.layout_button=QPushButton('Настроить лист');self.layout_button.setObjectName('layoutModeButton');self.layout_button.setCheckable(True);self.layout_button.toggled.connect(self.set_customize_mode);top.addWidget(self.layout_button)
        self.config_button=self.menu_button('Настройки',setup);top.addWidget(self.config_button)
        self.lock_action.setVisible(False)
        self.save_state_label=label('Сохранено','saveState');self.save_state_label.setToolTip('Изменения сохраняются автоматически');top.addWidget(self.save_state_label)
        top.addWidget(button('Сохранить',lambda:self.save(),'saveAction'));outer.addLayout(top)
        hero=QHBoxLayout();hero.setContentsMargins(0,8,0,15);hero.setSpacing(18)
        text=QVBoxLayout();text.setSpacing(4);text.addWidget(label('ЛИСТ ПЕРСОНАЖА  /  3.69','eyebrow'));self.header_name=label('','heroName');self.header_name.setMaximumWidth(530);text.addWidget(self.header_name);hero.addLayout(text,1)
        self.header_meta=label('','heroMeta');hero.addWidget(self.header_meta)
        self.check_button=button('Проверка',lambda:self.focus_card('checks'),'quietButton');self.check_button.setMaximumWidth(170);self.header_meta.setAlignment(Qt.AlignRight|Qt.AlignVCenter);hero.addWidget(self.check_button);outer.addLayout(hero)
        self.setMenuWidget(header)
    def go_home(self):
        if self.workspace_area:
            self.workspace_area.horizontalScrollBar().setValue(0);self.workspace_area.verticalScrollBar().setValue(0)
    def help(self):
        QMessageBox.information(self,'Дубль 0.14','Обычный режим листа защищён от случайного перемещения карточек.\n\nНажмите «Настроить лист», чтобы свободно перетаскивать карточки за заголовок и менять их ширину/высоту за правый, нижний или угловой край. Карточки магнитятся к сетке и соседям; красная рамка означает конфликт — при отпускании карточка вернётся на предыдущее место.\n\n«Готово» фиксирует рабочую область. Позиции и размеры сохраняются для персонажа автоматически. «Настройки → Оформление → Сбросить расположение» возвращает стандартный макет.\n\nВнутри карточек нет обычной прокрутки: длинные описания могут временно увеличить карточку, а полный текст всегда можно открыть отдельно.')
    def menu_button(self,title,menu,role='shellNav'):
        b=QToolButton();b.setObjectName(role);b.setText(title+'  ▾');b.setMenu(menu);b.setPopupMode(QToolButton.InstantPopup);return b
    def populate_characters_menu(self):
        if not hasattr(self,'characters_menu'):return
        menu=self.characters_menu;menu.clear()
        if self.store:
            menu.addAction('Все персонажи…',self.show_character_library)
            menu.addSeparator()
            summaries=self.store.list()[:5]
            for summary in summaries:
                pix=self._portrait_from_blob(summary.portrait)
                icon=QIcon(pix.scaled(28,28,Qt.KeepAspectRatioByExpanding,Qt.SmoothTransformation)) if not pix.isNull() else QIcon()
                action=menu.addAction(icon,summary.name)
                action.setCheckable(True);action.setChecked(summary.id==self.library_id)
                action.setToolTip(summary.concept or 'Открыть персонажа')
                action.triggered.connect(lambda _=False,cid=summary.id:self.switch_character(cid))
            if summaries:menu.addSeparator()
            menu.addAction('Новый персонаж',self.new)
            menu.addAction('Импортировать…',self.open)
            menu.addAction('Экспортировать текущего…',lambda:self.save(True))
        else:
            for action in self.file_menu.actions():menu.addAction(action)
    def show_character_library(self):
        if not self.store:return self.open()
        if not self.autosave():return
        from .character_library import CharacterLibraryDialog
        dialog=CharacterLibraryDialog(self.store,self.library_id,self)
        if dialog.exec()==QDialog.Accepted and dialog.selected_id:
            self.switch_character(dialog.selected_id,save_current=False)
    def switch_character(self,character_id,save_current=True):
        if not self.store or not character_id or character_id==self.library_id:return
        if save_current and not self.autosave():return
        try:state=self.store.load(character_id)
        except Exception as exc:
            QMessageBox.critical(self,'Не удалось открыть персонажа',str(exc));return
        self.timer.stop();self.s=state;self.library_id=str(state['id']);self.path=self.store.character_path(self.library_id);self.auto_path=self.path;self.dirty=False
        self.rebuild(restore=False);self.update_summary();self._set_save_state('saved');self.store.set_last_character(self.library_id)
    def resizeEvent(self,event):
        super().resizeEvent(event)
        if hasattr(self,'brand_word'):self.brand_word.setVisible(self.width()>=1180)
    def column(self):
        w=QWidget();w.setObjectName('panelBody');v=QVBoxLayout(w);v.setContentsMargins(14,2,14,9);v.setSpacing(10);return w,v
    def responsive(self,w,handler=None):
        """Attach the bounded card-density contract to a card body.

        The Card chooses presentation from the user's requested size budget,
        never from its temporary auto-grown rendered height. Child widgets only
        receive the already-decided state and never resize their parent directly.
        """
        def set_density(state,width,height):
            w._density_state=state;w._density_size=(width,height);w.setProperty('density',state)
            lay=w.layout()
            if isinstance(lay,QVBoxLayout):
                if state==EXPANDED:lay.setContentsMargins(14,2,14,10);lay.setSpacing(10)
                elif state==STANDARD:lay.setContentsMargins(12,2,12,8);lay.setSpacing(8)
                elif state==COMPACT:lay.setContentsMargins(9,1,9,6);lay.setSpacing(6)
                else:lay.setContentsMargins(7,1,7,5);lay.setSpacing(4)
            for n in w.findChildren(Number):n.set_density(state)
            if handler:handler(state,width,height)
            w.updateGeometry()
        w.set_density=set_density;set_density(STANDARD,420,320);return w
    def dock(self,key,title,body):
        if not callable(getattr(body,'set_density',None)):self.responsive(body)
        card=Card(self,key,title,body);self.docks[key]=card;self.view.addAction(card.toggle);return card
    def rebuild(self,restore=False):
        if self.docks and not restore:self.capture_layout()
        self.building=True
        for card in self.docks.values():
            if card.popup:card.popup.close()
        old=self.takeCentralWidget()
        if old:old.deleteLater()
        self.docks={};self.refreshers=[];self.tables={};self.details={};self.view.clear();self.columns=[];self.scrolls=[]
        ui=self.s['ui'];raw=ui.get('shell',{})
        saved=raw if isinstance(raw,dict) and raw.get('version')==7 else {}
        legacy_states=raw.get('cards',{}) if isinstance(raw,dict) else {}
        self.selections=copy.deepcopy((saved or raw if isinstance(raw,dict) else {}).get('selections',{}))
        self.workspace_canvas=WorkspaceCanvas(self)
        self.workspace_area=WorkspaceScrollArea(self,self.workspace_canvas)
        self.setCentralWidget(self.workspace_area)
        makers=[('profile','Персонаж',self.profile_panel),('resources','Ресурсы',self.resources_panel),('derived','Показатели',self.derived_panel),('attributes','Характеристики',self.attributes_panel),('skills','Умения',self.skills_panel),('feats','Навыки и способности',self.feats_panel),('magic','Магия',self.magic_panel),('gear','Снаряжение',self.gear_panel),('cyber','Киберпанк',self.cyber_panel),('modifiers','Поправки',self.modifiers_panel),('notes','Заметки',self.notes_panel),('checks','Проверка персонажа',self.checks_panel)]
        for key,title,fn in makers:self.dock(key,title,fn())
        for block in self.s['customBlocks']:
            w,v=self.column();setup=button('Настроить',lambda b=block:self.edit_block(b),'quietButton');v.addWidget(setup);edit=QPlainTextEdit(block['text']);edit.setVerticalScrollBarPolicy(Qt.ScrollBarAlwaysOff);edit.setHorizontalScrollBarPolicy(Qt.ScrollBarAlwaysOff);edit.textChanged.connect(lambda b=block,e=edit:self.assign(b,'text',e.toPlainText()));v.addWidget(edit)
            def custom_density(state,width,height,edit=edit,setup=setup):
                setup.setVisible(state!=MINIMAL);base=150 if state==EXPANDED else 105 if state==STANDARD else 70 if state==COMPACT else 48;self.fit_text_widget(edit,base)
            edit.document().contentsChanged.connect(lambda edit=edit:QTimer.singleShot(0,lambda:self.fit_text_widget(edit,72)))
            self.dock('custom_'+block['id'],block['name'],self.responsive(w,custom_density))
        defaults=default_workspace_geometry(self.width())
        states=saved.get('cards',{}) if saved else legacy_states
        custom_y=max([defaults[k].bottom() for k in defaults if k not in SECONDARY]+[900])+24
        for key,card in self.docks.items():
            config=states.get(key,{}) if isinstance(states,dict) else {}
            if saved:
                base=defaults.get(key,QRect(MARGIN,custom_y,card.preferred_width(),card.preferred_height()))
                rect=QRect(int(number(config.get('x'),base.x())),int(number(config.get('y'),base.y())),int(number(config.get('width'),base.width())),int(number(config.get('height'),base.height())))
            else:
                rect=defaults.get(key,QRect(MARGIN,custom_y,card.preferred_width(),card.preferred_height()))
                if key.startswith('custom_'):custom_y+=card.preferred_height()+GAP
            card.user_x=max(MARGIN,rect.x());card.user_y=max(MARGIN,rect.y());card.user_width=card.clamp_user_width(rect.width());card.user_height=card.clamp_user_height(rect.height())
            card.setGeometry(card.user_x,card.user_y,card.user_width,48 if config.get('collapsed',False) else card.user_height)
            card.set_collapsed(config.get('collapsed',False))
            hidden=config.get('hidden',key in SECONDARY)
            card.setVisible(not hidden);card.toggle.setChecked(not hidden);card.set_customize(False)
            if not card.collapsed:QTimer.singleShot(0,card.update_density)
        self.customize_mode=False
        if hasattr(self,'layout_button'):
            self.layout_button.blockSignals(True);self.layout_button.setChecked(False);self.layout_button.setText('Настроить лист');self.layout_button.blockSignals(False)
        self.workspace_canvas.set_customize(False);self.workspace_canvas.refresh_extent()
        self.apply_style()
        if restore and ui.get('geometry'):self.restoreGeometry(QByteArray.fromBase64(ui['geometry'].encode()))
        scroll=saved.get('scroll',[0,0]) if saved else [0,0]
        QTimer.singleShot(0,lambda s=scroll:(self.workspace_area.horizontalScrollBar().setValue(int(number(s[0]))),self.workspace_area.verticalScrollBar().setValue(int(number(s[1]))),self.workspace_canvas.refresh_extent()))
        self.building=False;self.update_summary()
    def set_customize_mode(self,on):
        self.customize_mode=bool(on)
        if hasattr(self,'layout_button'):
            self.layout_button.setText('Готово' if on else 'Настроить лист');self.layout_button.setProperty('active',bool(on))
            try:self.layout_button.style().unpolish(self.layout_button);self.layout_button.style().polish(self.layout_button)
            except RuntimeError:pass
        if self.workspace_canvas:self.workspace_canvas.set_customize(on)
        for card in self.docks.values():card.set_customize(on)
        if not on:self.layout_changed()
    def find_free_geometry(self,card,rect):
        if not self.workspace_canvas:return rect
        candidate=QRect(rect);candidate.moveLeft(max(MARGIN,candidate.x()));candidate.moveTop(max(MARGIN,candidate.y()))
        for _ in range(200):
            conflicts=[c for c in self.workspace_canvas.visible_cards(card) if candidate.intersects(c.geometry())]
            if not conflicts:return candidate
            candidate.moveTop(max(c.geometry().bottom()+1+GAP for c in conflicts))
        return candidate
    def ensure_card_nonoverlap(self,card):
        if not self.workspace_canvas or card.isHidden() or card.popup:return
        rect=QRect(card.geometry())
        if self.workspace_canvas.conflicts(card,rect):
            rect=self.find_free_geometry(card,rect);card.setGeometry(rect);card.user_x,card.user_y=rect.x(),rect.y();self.workspace_canvas.refresh_extent()
    def apply_card_content_height(self,card,target):
        if not self.workspace_canvas or card.collapsed or card.popup:return
        target=card.clamp_user_height(target);current=card.geometry()
        if target>current.height():target=self.workspace_canvas.available_growth_height(card,target)
        # If a previous temporary grow is no longer needed, return toward the
        # user's stored height. This never changes user_height itself.
        target=max(card.user_height or card.preferred_height(),target)
        if current.height()!=target:
            card.setGeometry(current.x(),current.y(),current.width(),target);card._position_handles();self.workspace_canvas.refresh_extent()
    def focus_card(self,key):
        card=self.docks.get(key)
        if not card:return
        card.show_card();card.set_collapsed(False);card.raise_()
        if self.workspace_area:self.workspace_area.ensureWidgetVisible(card,30,30)
        if card.popup:card.popup.raise_();card.popup.activateWindow()
    def capture_layout(self):
        if not self.workspace_canvas:return
        cards={}
        for k,c in self.docks.items():
            cards[k]={'hidden':c.isHidden(),'collapsed':c.collapsed,'x':c.x(),'y':c.y(),'width':c.user_width or c.width(),'height':c.user_height or c.preferred_height()}
        self.s['ui']['shell']={'version':7,'selections':dict(self.selections),'scroll':[self.workspace_area.horizontalScrollBar().value(),self.workspace_area.verticalScrollBar().value()],'cards':cards}
        self.s['ui']['geometry']=bytes(self.saveGeometry().toBase64()).decode()
    def reset_layout(self):
        self.s['ui'].pop('shell',None);self.rebuild(restore=True);self.changed()
    def lock(self,checked=False):
        # Kept for old saved settings/API compatibility. Explicit customize mode
        # now controls whether geometry can change.
        self.s['ui']['locked']=bool(checked)
        for c in self.docks.values():c.lock(bool(checked))
        if checked and self.customize_mode and hasattr(self,'layout_button'):self.layout_button.setChecked(False)
        self.layout_changed()
    def apply_style(self):
        app=QApplication.instance();app.setStyle('Fusion');body,_,_=fonts();app.setFont(QFont(body,self.s['ui']['fontSize']))
        light=self.s['ui'].get('theme')=='light';base=warm_light if light else elegant_dark
        self.setStyleSheet(base(self.s['ui']['fontSize'])+shell_style(self.s['ui']['fontSize'],light))
    def update_summary(self):
        super().update_summary();name=self.s['profile']['name'] or 'Новый персонаж';self.setWindowTitle(name+' — Дубль 0.14'+(' •' if self.dirty else ''))
        if hasattr(self,'header_name'):
            self.header_name.setToolTip(name)
        if hasattr(self,'check_button'):
            warnings=engine.warnings(self.s);self.check_button.setText(f'Проверка · {len(warnings)}' if warnings else '✓ Проверка');self.check_button.setToolTip('Открыть проверку требований и расходов опыта')
    def _set_save_state(self,state,tip=''):
        if not hasattr(self,'save_state_label'):return
        texts={'saved':'Сохранено','saving':'Сохраняется…','error':'Ошибка сохранения'}
        self.save_state_label.setText(texts.get(state,state));self.save_state_label.setProperty('state',state)
        self.save_state_label.setToolTip(tip or ('Изменения сохраняются автоматически' if state!='error' else 'Автосохранение не удалось'))
        try:self.save_state_label.style().unpolish(self.save_state_label);self.save_state_label.style().polish(self.save_state_label)
        except RuntimeError:pass
    def changed(self):
        super().changed();self._set_save_state('saving')
    def compact(self,value,lo=-999999,hi=999999,decimals=0):
        n=Number(value,lo,hi,decimals);n.spin.setMinimumWidth(40);n.layout().setSpacing(3)
        for b in n.findChildren(QPushButton):b.setFixedSize(25,30)
        return n
    def _portrait_from_blob(self,blob):
        if not blob:return QPixmap()
        try:
            raw=base64.b64decode(str(blob).encode('ascii'),validate=True);pix=QPixmap();pix.loadFromData(raw);return pix
        except Exception:return QPixmap()
    def portrait_pixmap(self):
        return self._portrait_from_blob(self.s.get('profile',{}).get('portrait',''))
    def choose_portrait(self):
        path,_=QFileDialog.getOpenFileName(self,'Портрет персонажа','', 'Изображения (*.png *.jpg *.jpeg *.webp *.bmp)')
        if not path:return
        pix=QPixmap(path)
        if pix.isNull():
            QMessageBox.warning(self,'Не удалось открыть изображение','Выберите PNG, JPG, WEBP или BMP изображение.');return
        pix=pix.scaled(512,512,Qt.KeepAspectRatio,Qt.SmoothTransformation)
        data=QByteArray();buffer=QBuffer(data);buffer.open(QIODevice.WriteOnly)
        if not pix.save(buffer,'PNG'):
            QMessageBox.warning(self,'Не удалось сохранить портрет','Qt не смог подготовить изображение для листа персонажа.');return
        self.s['profile']['portrait']=base64.b64encode(bytes(data)).decode('ascii');self.rebuild();self.changed()
    def clear_portrait(self):
        if not self.s['profile'].get('portrait'):return
        self.s['profile']['portrait']='';self.rebuild();self.changed()
    def portrait_context(self,portrait,pos):
        menu=QMenu(portrait);menu.addAction('Выбрать другое изображение',self.choose_portrait)
        if self.s['profile'].get('portrait'):menu.addAction('Удалить портрет',self.clear_portrait)
        menu.exec(portrait.mapToGlobal(pos))
    def profile_panel(self):
        w,v=self.column();p=self.s['profile'];fields={}
        top=QWidget();top.setObjectName('profileTop');top_lay=QHBoxLayout(top);top_lay.setContentsMargins(0,0,0,0);top_lay.setSpacing(12);top_lay.setAlignment(Qt.AlignTop)
        portrait=QToolButton();portrait.setObjectName('portraitButton');portrait.setToolTip('Нажмите, чтобы добавить или заменить портрет');portrait.setAccessibleName('Портрет персонажа');portrait.clicked.connect(self.choose_portrait);portrait.setContextMenuPolicy(Qt.CustomContextMenu);portrait.customContextMenuRequested.connect(lambda pos:self.portrait_context(portrait,pos));top_lay.addWidget(portrait,0,Qt.AlignTop)
        fields_box=QWidget();fields_box.setObjectName('profileFields');fields_box.setSizePolicy(QSizePolicy.Expanding,QSizePolicy.Maximum);fields_lay=QVBoxLayout(fields_box);fields_lay.setContentsMargins(0,0,0,0);fields_lay.setSpacing(7);fields_lay.setAlignment(Qt.AlignTop)
        for key,placeholder in [('name','Имя персонажа'),('concept','Концепция, роль, происхождение')]:
            field=QLineEdit(p[key]);field.setPlaceholderText(placeholder);field.setAccessibleName(placeholder);field.setToolTip(p[key] or placeholder);field.textChanged.connect(lambda text,k=key,field=field:(field.setToolTip(text or field.placeholderText()),self.assign(p,k,text)));fields_lay.addWidget(field);fields[key]=field
        top_lay.addWidget(fields_box,1,Qt.AlignTop);v.addWidget(top)
        portrait_menu=QMenu(self);portrait_menu.addAction('Добавить / заменить портрет…',self.choose_portrait);remove_portrait=portrait_menu.addAction('Удалить портрет',self.clear_portrait);remove_portrait.setEnabled(bool(p.get('portrait')))
        portrait_actions=self.menu_button('Портрет',portrait_menu,'inlineMenu')
        configure=button('Настроить персонажа',self.configure_profile,'quietButton');actions=row(portrait_actions,configure);v.addWidget(actions)
        pix=self.portrait_pixmap()
        def portrait_size(px):
            portrait.setFixedSize(px,px)
            if not pix.isNull():
                portrait.setIcon(QIcon(pix));portrait.setIconSize(QSize(max(36,px-6),max(36,px-6)));portrait.setText('');portrait.setToolButtonStyle(Qt.ToolButtonIconOnly)
            else:
                portrait.setIcon(QIcon());portrait.setText('＋\nПортрет' if px>=78 else '＋');portrait.setToolButtonStyle(Qt.ToolButtonTextOnly)
        def density(state,width,height):
            fields['concept'].setVisible(state!=MINIMAL)
            configure.setVisible(state not in (COMPACT,MINIMAL));portrait_actions.setVisible(state!=MINIMAL)
            # Portrait is intentionally a strong visual anchor of the character card.
            portrait_size(216 if state==EXPANDED else 184 if state==STANDARD else 128 if state==COMPACT else 108)
            top_lay.setSpacing(12 if state in (EXPANDED,STANDARD) else 8)
            fields_lay.setSpacing(7 if state in (EXPANDED,STANDARD) else 4)
            for field in fields.values():field.setMinimumHeight(26 if state in (COMPACT,MINIMAL) else 30)
        return self.responsive(w,density)
    def configure_profile(self):
        p=self.s['profile'];r=self.edit('Настройки персонажа',[('xpTotal','Всего опыта','int',p['xpTotal'],0,999999),('creationXp','Опыт при создании','int',p['creationXp'],0,999999),('sizeModifiers','Поправки от размера','bool',self.s['options']['sizeModifiers'])])
        if r:self.s['options']['sizeModifiers']=r.pop('sizeModifiers');p.update(r);self.changed()
        # Ability budget remains a separate action in the resource settings menu.
    def resources_panel(self):
        w,v=self.column();settings=QMenu(self);settings.addAction('Добавить ресурс',self.add_resource);settings.addAction('Очки способностей',self.ability_budget)
        mana=settings.addAction('Показывать ману');mana.setCheckable(True);mana.setChecked(self.s['resources']['manaVisible']);mana.triggered.connect(self.mana_visible);tiles=[]
        def resource(title,obj,key,maxfn,kind='health',custom=None):
            box=QFrame();box.setObjectName('resourceTile');lay=QVBoxLayout(box);lay.setContentsMargins(9,5,9,5);lay.setSpacing(3)
            head=QHBoxLayout();name=label(title,'resourceName');head.addWidget(name);head.addStretch();maximum=label('','mutedLabel');head.addWidget(maximum)
            custom_button=None
            if custom:custom_button=button('⋯',lambda:self.edit_resource(custom),'miniButton');head.addWidget(custom_button)
            lay.addLayout(head);n=self.compact(obj.get(key) if obj.get(key) is not None else maxfn(),decimals=2);n.spin.setMinimumHeight(24)
            n.changed.connect(lambda value:self.assign(obj,key,value));restore=button('↺',lambda:self.assign(obj,key,maxfn()),'miniButton');restore.setToolTip('Восстановить до максимума');restore.setAccessibleName('Восстановить '+title);value_row=row(n,restore);lay.addWidget(value_row)
            bar=QProgressBar();bar.setRange(0,1000);bar.setTextVisible(False);bar.setFixedHeight(4);bar.setObjectName(kind+'Bar');lay.addWidget(bar);v.addWidget(box);tiles.append((box,n,restore,bar,custom_button))
            def update():
                mx=maxfn();cur=obj.get(key) if obj.get(key) is not None else mx;maximum.setText('/ '+f'{mx:g}');n.setValue(cur);bar.setValue(max(0,min(1000,int(cur/max(1,mx)*1000))))
            self.refreshers.append(update)
        resource('Здоровье',self.s['resources'],'healthCurrent',lambda:engine.derived(self.s)['Здоровье'])
        resource('Выносливость',self.s['resources'],'staminaCurrent',lambda:engine.derived(self.s)['Выносливость'],'stamina')
        if self.s['resources']['manaVisible']:resource('Мана',self.s['magic'],'currentMana',lambda:engine.mana(self.s),'mana')
        for r in self.s['resources']['custom']:resource(r['name'],r,'current',lambda r=r:r['max'],'custom',r)
        settings_button=self.menu_button('Настроить ресурсы',settings,'inlineMenu');v.addWidget(settings_button)
        def density(state,width,height):
            for box,n,restore,bar,custom_button in tiles:
                n.set_density(state);restore.setVisible(state in (EXPANDED,STANDARD));bar.setVisible(state!=MINIMAL)
                if custom_button:custom_button.setVisible(state!=MINIMAL)
                lay=box.layout();lay.setContentsMargins(8,4,8,4 if state in (EXPANDED,STANDARD) else 3);lay.setSpacing(3 if state in (EXPANDED,STANDARD) else 1)
            settings_button.setVisible(state!=MINIMAL)
        return self.responsive(w,density)
    def derived_panel(self):
        w,v=self.column();grid=QGridLayout();grid.setSpacing(6);v.addLayout(grid);out={};tiles=[]
        keys=['Защита','Инициатива','Рефлексы','Стойкость','Бег','Рывок']
        for i,k in enumerate(keys):
            tile=QFrame();tile.setObjectName('metricTile');lay=QVBoxLayout(tile);lay.setContentsMargins(6,6,6,6);lay.setSpacing(1);value=label('','metricValue');value.setAlignment(Qt.AlignCenter);name=label(k,'metricLabel');name.setSizePolicy(QSizePolicy.Ignored,QSizePolicy.Preferred);name.setAlignment(Qt.AlignCenter);lay.addWidget(value);lay.addWidget(name);tiles.append(tile);out[k]=value
        all_button=button('Все показатели',self.all_stats,'quietButton');v.addWidget(all_button)
        def relayout(cols):
            for tile in tiles:grid.removeWidget(tile)
            for i,tile in enumerate(tiles):grid.addWidget(tile,i//cols,i%cols)
            for i in range(6):grid.setColumnStretch(i,1 if i<cols else 0)
        relayout(3)
        def update():
            d=engine.derived(self.s)
            for k,widget in out.items():widget.setText(f'{d[k]:g}')
        self.refreshers.append(update)
        def density(state,width,height):
            cols=6 if width>=520 else 3 if width>=245 else 2
            relayout(cols);all_button.setVisible(state!=MINIMAL)
            for tile in tiles:
                tile.setProperty('density',state);lay=tile.layout();lay.setContentsMargins(4,3,4,3 if state in (EXPANDED,STANDARD) else 2);lay.setSpacing(1)
        return self.responsive(w,density)
    def all_stats(self):
        d=QDialog(self);d.setWindowTitle('Все показатели');d.resize(460,580);v=QVBoxLayout(d);panel=Window.derived_panel(self);v.addWidget(panel);self.update_summary();d.exec()
        # Remove callbacks bound to the temporary panel before Qt can destroy it.
        d.deleteLater();self.rebuild()
    def attributes_panel(self):
        w,v=self.column();p=self.s['profile']
        size_line=QWidget();size_line.setObjectName('attributeMetaRow');size_lay=QHBoxLayout(size_line);size_lay.setContentsMargins(2,0,2,1);size_lay.setSpacing(7);size_label=label('Размер','mutedLabel');size=self.compact(p['size'],1,10);size.spin.setMinimumWidth(46);size.changed.connect(lambda x:self.assign(p,'size',int(x)));size_lay.addWidget(size_label);size_lay.addWidget(size);size_lay.addStretch(1);v.addWidget(size_line)
        grid=AttributeGrid();v.addWidget(grid);tiles={}
        for a in ATTRS:
            n=self.compact(self.s['attributes'][a],-5,10);n.changed.connect(lambda x,a=a:self.assign(self.s['attributes'],a,int(x)));tile=AttributeTile(a,n,0);tiles[a]=tile;grid.add_tile(tile)
        def update():
            values=engine.attrs(self.s)
            for a,tile in tiles.items():tile.set_total(values[a])
        self.refreshers.append(update)
        def density(state,width,height):
            size.set_density(state);grid.set_density(state,width)
            size_label.setText('Размер' if state!=MINIMAL else 'Разм.')
            size_line.setMaximumHeight(36 if state in (EXPANDED,STANDARD) else 31)
        return self.responsive(w,density)
    def table(self,headers,widths=None):
        t=super().table(headers,widths);t.setObjectName('contentTable');t.setMinimumHeight(0);t.setAlternatingRowColors(False);t.setTextElideMode(Qt.ElideRight);t.horizontalHeader().setDefaultAlignment(Qt.AlignLeft|Qt.AlignVCenter);t.setVerticalScrollBarPolicy(Qt.ScrollBarAlwaysOff);t.setHorizontalScrollBarPolicy(Qt.ScrollBarAlwaysOff);return t
    def fit_table(self,t,count,cap=5,rowheight=38):
        """Fit the table to the rows we deliberately expose.

        Tables inside cards never own a scrollbar.  Dense cards expose a useful
        subset and an explicit "show all" action; expanded lists make the card
        taller and leave scrolling to the column.
        """
        t._fit_count=count;t._fit_base_cap=cap;t._fit_rowheight=rowheight;t._show_all=False
        def adjust(*_):
            row_now=max(26,int(getattr(t,'_fit_rowheight',rowheight)));total=0;visible=0
            wrap=t.wordWrap()
            for i in range(count):
                if t.isRowHidden(i):continue
                visible+=1;text=t.item(i,0).text() if t.item(i,0) else ''
                if wrap:
                    bound=t.fontMetrics().boundingRect(QRect(0,0,max(50,t.columnWidth(0)-22),1000),Qt.TextWordWrap,text);h=max(row_now,min(72,bound.height()+10))
                else:h=row_now
                t.setRowHeight(i,h);total+=h
            header=0 if t.horizontalHeader().isHidden() else t.horizontalHeader().sizeHint().height()
            t.setFixedHeight(header+(total if visible else row_now)+3)
        t._fit_rows=adjust;t.horizontalHeader().sectionResized.connect(adjust);QTimer.singleShot(0,adjust);adjust()
    def table_density(self,t,state,cap=6):
        t.horizontalHeader().setVisible(state!=MINIMAL);t.setWordWrap(state in (EXPANDED,STANDARD))
        t._fit_rowheight=31 if state==EXPANDED else 29 if state==STANDARD else 27 if state==COMPACT else 25
        if not hasattr(t,'_fit_rows'):
            self.fit_table(t,t.rowCount(),max(1,t.rowCount()),t._fit_rowheight)
        if hasattr(t,'_fit_rows'):t._fit_rows()
    def table_window(self,t,state,base_cap,overflow=None,candidates=None,priority=None):
        count=t.rowCount();candidates=list(range(count)) if candidates is None else list(candidates)
        priority=[i for i in (priority or []) if i in candidates]
        ordered=priority+[i for i in candidates if i not in priority]
        cap={EXPANDED:base_cap+2,STANDARD:base_cap,COMPACT:max(3,base_cap-2),MINIMAL:max(2,base_cap-3)}.get(state,base_cap)
        if getattr(t,'_show_all',False):visible=ordered
        else:
            visible=ordered[:cap];selected=t.currentRow()
            if selected in candidates and selected not in visible:
                if visible:visible[-1]=selected
                else:visible=[selected]
        visible=set(visible)
        for i in range(count):t.setRowHidden(i,i not in visible)
        self.table_density(t,state,base_cap)
        remaining=max(0,len(candidates)-len(visible))
        if overflow is not None:
            showing_all=bool(getattr(t,'_show_all',False))
            overflow.setVisible(len(candidates)>cap or showing_all)
            overflow.setText('Свернуть список' if showing_all else f'Показать все · ещё {remaining}')
        QTimer.singleShot(0,lambda t=t:self.request_card_fit(t))
        return remaining
    def toggle_table_all(self,key,t):
        t._show_all=not bool(getattr(t,'_show_all',False));card=self.docks.get(key)
        if card:
            # Keep the current density. Showing all rows makes the card grow
            # and delegates scrolling to the column instead of changing mode.
            fn=getattr(card.body,'set_density',None)
            if callable(fn):
                width,height=card.density_budget();fn(card.density or STANDARD,width,height)
            card.request_content_fit();card.owner.layout_changed()
    def request_card_fit(self,widget):
        parent=widget
        while parent is not None:
            if isinstance(parent,Card):parent.request_content_fit();return
            parent=parent.parentWidget()
    def fit_text_widget(self,widget,minheight=72,maxheight=480):
        if not widget:return
        widget.setVerticalScrollBarPolicy(Qt.ScrollBarAlwaysOff);widget.setHorizontalScrollBarPolicy(Qt.ScrollBarAlwaysOff)
        try:
            widget.document().setTextWidth(max(160,widget.viewport().width()))
            h=max(minheight,min(maxheight,int(widget.document().size().height())+18))
            if widget.height()!=h:widget.setFixedHeight(h)
            widget.updateGeometry();self.request_card_fit(widget)
        except RuntimeError:pass
    def detail_visibility(self,details,on,minheight=None,maxheight=440):
        if on:
            details.fit_cap=max(minheight or getattr(details,'base_height',120),int(maxheight or 440));details.setMaximumHeight(details.fit_cap);details.setMinimumHeight(minheight or getattr(details,'base_height',120));details.setVisible(True);QTimer.singleShot(0,details.fit_content)
        else:
            details.setVisible(False);details.setMinimumHeight(0);details.setMaximumHeight(0)
        QTimer.singleShot(0,lambda details=details:self.request_card_fit(details))
    def empty(self,v,title,subtitle,fn,action):
        box=QFrame();box.setObjectName('emptyState');lay=QVBoxLayout(box);lay.setContentsMargins(12,10,12,10);lay.setSpacing(5);lay.addWidget(label(title,'emptyTitle'));lay.addWidget(label(subtitle,'mutedLabel'));lay.addWidget(button(action,fn,'quietButton'));v.addWidget(box);return box
    def preview_text(self,e,kind='',rank=None):
        if not e:return ''
        name=str(e.get('name',''));meta=[]
        if kind=='skill':meta=[' + '.join(engine.skill_attrs(e)),'ранг '+str(e.get('rank',rank or 0))]
        elif kind=='feat':
            if e.get('category'):meta.append(str(e.get('category')))
            if rank is not None:meta.append('ранг '+str(rank))
        elif kind=='spell':meta=[str(e.get('school','—')),'мана '+str(e.get('manaText',e.get('cost','—')))]
        elif kind=='gear' and e.get('fields'):meta=[str(k)+': '+str(v) for k,v in list(e.get('fields',{}).items())[:2]]
        body=next((str(e.get(k,'')).strip() for k in ['benefit','description','effect','notes'] if e.get(k)), '')
        body=' '.join(body.split());body=(body[:150].rstrip()+'…') if len(body)>150 else body
        top=name+('  ·  '+' · '.join(meta) if meta else '')
        return top+('\n'+body if body else '')
    def selection(self,key,t,items,refresh):
        self.tables[key]=t
        def selected():
            entry=self.selected(t,items)
            if entry:self.selections[key]=entry.get('uid') or entry.get('id') or entry.get('name')
            refresh(entry)
        t.itemSelectionChanged.connect(selected);t.itemDoubleClicked.connect(lambda *_:self.full_details(key))
        identity=self.selections.get(key);idx=next((i for i,x in enumerate(items) if (x.get('uid') or x.get('id') or x.get('name'))==identity),0)
        if items:t.setCurrentCell(idx,0)
    def full_details(self,key):
        panel=self.details.get(key)
        if not panel or not panel.entry:return
        d=QDialog(self);d.setWindowTitle(panel.entry.get('name','Описание'));d.resize(680,620);v=QVBoxLayout(d);content=QTextBrowser();content.setHtml(panel.toHtml());v.addWidget(content);v.addWidget(button('Закрыть',d.accept,'quietButton'));d.exec()
    def actions(self,v,key,t,items,add,custom,edit,remove=True):
        if not items:
            widget=button('Создать свою запись',custom,'quietButton');v.addWidget(widget);return widget
        menu=QMenu(self);menu.addAction('Добавить из книги',add);menu.addAction('Добавить своё',custom)
        edits=button('Изменить',lambda:edit(self.selected(t,items)),'quietButton');more=QMenu(self)
        more.addAction('Полное описание',lambda:self.full_details(key))
        if remove:more.addAction('Удалить запись',lambda:self.remove_selected(t,items))
        widget=row(self.menu_button('+ Добавить',menu,'inlineMenu'),edits,self.menu_button('Ещё',more,'inlineMenu'));v.addWidget(widget);edits.setEnabled(bool(items));return widget
    def skills_panel(self):
        w,v=self.column();search=QLineEdit();search.setPlaceholderText('Найти умение…');search.setAccessibleName('Поиск умения');v.addWidget(search)
        items=[x for x in engine.skills(self.s) if x['name'] not in self.s['ui']['hiddenSkills'] and not x.get('hidden')];t=self.table(['Умение','Ранг','Бонус'],[0,112,72]);v.addWidget(t);rank_editors=[];bonus_items=[]
        for i,sk in enumerate(items):
            t.insertRow(i);self.textitem(t,i,0,sk['name'],sk.get('description',''));n=self.compact(sk.get('rank',0),0,10);n.set_hover_steps(True);n.changed.connect(lambda x,sk=sk:self.set_skill_rank(sk,x));t.setCellWidget(i,1,n);rank_editors.append(n);bonus_items.append(self.textitem(t,i,2,''))
        self.fit_table(t,len(items),5,29)
        overflow=button('',lambda:self.toggle_table_all('skills',t),'quietButton');overflow.setObjectName('tableOverflow');v.addWidget(overflow)
        preview=label('','selectionPreview');preview.setWordWrap(True);preview.setMaximumHeight(52);v.addWidget(preview)
        details=Details(126);self.details['skills']=details;v.addWidget(details)
        def display(sk):
            if sk:
                current=next(x for x in engine.skills(self.s) if x['name']==sk['name']);details.display(current,self.s,'skill');preview.setText(self.preview_text(current,'skill'))
            else:preview.setText('')
        self.selection('skills',t,items,display)
        def update():
            current={x['name']:x for x in engine.skills(self.s)}
            for i,sk in enumerate(items):
                live=current[sk['name']];bonus=engine.skill_bonus(self.s,live);bonus_items[i].setText('—' if bonus is None else f'{bonus:+g}');bonus_items[i].setToolTip(engine.skill_formula_text(self.s,live));t.item(i,0).setToolTip((sk.get('description','')+'\n\n'+engine.skill_formula_text(self.s,live)).strip())
            display(self.selected(t,items))
        self.refreshers.append(update)
        menu=QMenu(self);menu.addAction('Скрыть выбранное',lambda:self.hide_skill(self.selected(t,items)));menu.addAction('Вернуть скрытые',self.restore_skills);menu.addAction('Полное описание',lambda:self.full_details('skills'))
        action_row=row(button('+ Умение',self.add_skill,'quietButton'),button('Настроить',lambda:self.edit_skill(self.selected(t,items)),'quietButton'),self.menu_button('Ещё',menu,'inlineMenu'));v.addWidget(action_row)
        def window(state):
            query=engine.norm(search.text());candidates=[i for i,sk in enumerate(items) if not query or query in engine.norm(sk['name'])]
            trained=[i for i in candidates if number(items[i].get('rank'))>0]
            self.table_window(t,state,5,overflow,candidates,trained)
        def density(state,width,height):
            for n in rank_editors:n.set_density(state)
            window(state);self.detail_visibility(details,bool(items) and state==EXPANDED,126,330);preview.setVisible(bool(items) and state==STANDARD)
            search.setVisible(state!=MINIMAL);action_row.setVisible(state!=MINIMAL)
            if state==MINIMAL:t.setColumnWidth(1,52);t.setColumnWidth(2,54)
            elif state==COMPACT:t.setColumnWidth(1,68);t.setColumnWidth(2,58)
            elif state==STANDARD:t.setColumnWidth(1,112);t.setColumnWidth(2,72)
            else:t.setColumnWidth(1,118);t.setColumnWidth(2,76)
        search.textChanged.connect(lambda *_:window(getattr(w,'_density_state',STANDARD)))
        return self.responsive(w,density)

    def feats_panel(self):
        w,v=self.column();items=self.s['feats'];t=self.table(['Навык / способность','Ранг'],[0,58]);v.addWidget(t)
        for i,f in enumerate(items):t.insertRow(i);self.textitem(t,i,0,engine.record(f)['name'],description(engine.record(f),self.s));self.textitem(t,i,1,f['rank'])
        self.fit_table(t,len(items),6,32);t.setVisible(bool(items))
        overflow=button('',lambda:self.toggle_table_all('feats',t),'quietButton');overflow.setObjectName('tableOverflow');v.addWidget(overflow)
        preview=label('','selectionPreview');preview.setWordWrap(True);preview.setMaximumHeight(72);v.addWidget(preview)
        details=Details(180);self.details['feats']=details;v.addWidget(details)
        def display(f):
            if f:
                entry=engine.record(f);details.display(entry,self.s,'feat',f.get('note','')+('\nРешение мастера: '+f['overrideReason'] if f.get('overrideReason') else ''));preview.setText(self.preview_text(entry,'feat',f.get('rank',1)))
            else:preview.setText('')
        self.selection('feats',t,items,display)
        def update():
            for i,f in enumerate(items):
                checks=engine.requirements(self.s,engine.record(f));status='fail' if any(c.state=='fail' for c in checks) else 'manual' if any(c.state=='manual' for c in checks) else 'ok';color='#da8490' if status=='fail' else '#d6b486' if status=='manual' else '#b2cbbd';t.item(i,0).setForeground(QColor('#91b6d1' if f.get('overrideReason') else color))
            display(self.selected(t,items))
        self.refreshers.append(update)
        empty_box=None
        if not items:empty_box=self.empty(v,'Откройте способности персонажа','Навыки, особые ветки и требования из книги.',lambda:self.add_catalog('entries'),'Выбрать навык')
        action_row=self.actions(v,'feats',t,items,lambda:self.add_catalog('entries'),self.custom_feat,self.edit_feat)
        def density(state,width,height):
            self.table_window(t,state,4,overflow);self.detail_visibility(details,bool(items) and state==EXPANDED,170,430);preview.setVisible(bool(items) and state==STANDARD)
            if action_row:action_row.setVisible(state!=MINIMAL)
            if empty_box:empty_box.setProperty('density',state)
        return self.responsive(w,density)

    def magic_panel(self):
        w,v=self.column();m=self.s['magic'];grid=QGridLayout();grid.setSpacing(9);v.addLayout(grid);numbers=[]
        for i,(key,title,hi) in enumerate([('manaRank','Ранг запаса',5),('power','Сила магии',999)]):
            grid.addWidget(label(title,'mutedLabel'),0,i);n=self.compact(m[key],0,hi);n.changed.connect(lambda value,k=key:self.magic_value(k,value));grid.addWidget(n,1,i);numbers.append(n)
        meta=label('','sidebarMeta');schools=button('Школы',self.schools,'quietButton');meta_row=row(meta,schools);v.addWidget(meta_row);self.refreshers.append(lambda:meta.setText(f"Мана {engine.mana(self.s):g}  ·  +{1+engine.rank(self.s,'Медитация')} / раунд"))
        items=m['spells'];t=self.table(['Заклинание','Мана'],[0,58]);v.addWidget(t)
        for i,sp in enumerate(items):t.insertRow(i);self.textitem(t,i,0,sp['name'],description(sp));self.textitem(t,i,1,sp.get('cost',0))
        self.fit_table(t,len(items),3,29);t.setVisible(bool(items))
        overflow=button('',lambda:self.toggle_table_all('magic',t),'quietButton');overflow.setObjectName('tableOverflow');v.addWidget(overflow)
        preview=label('','selectionPreview');preview.setWordWrap(True);preview.setMaximumHeight(72);v.addWidget(preview)
        details=Details(180);self.details['magic']=details;v.addWidget(details)
        def display(sp):
            if sp:details.display(sp,self.s,'spell');preview.setText(self.preview_text(sp,'spell'))
            else:preview.setText('')
        self.selection('magic',t,items,display)
        empty_box=None
        if not items:empty_box=self.empty(v,'Книга заклинаний пуста','Добавьте заклинание из каталога или создайте своё.',lambda:self.add_catalog('spells'),'Выбрать заклинание')
        action_row=self.actions(v,'magic',t,items,lambda:self.add_catalog('spells'),lambda:self.edit_spell(new=True),self.edit_spell)
        def density(state,width,height):
            for n in numbers:n.set_density(state)
            grid.setHorizontalSpacing(6 if state in (COMPACT,MINIMAL) else 9);grid.setVerticalSpacing(3 if state in (COMPACT,MINIMAL) else 6)
            self.table_window(t,state,3,overflow);self.detail_visibility(details,bool(items) and state==EXPANDED,165,450);preview.setVisible(bool(items) and state==STANDARD)
            schools.setVisible(state!=MINIMAL);action_row.setVisible(state!=MINIMAL)
            if empty_box:empty_box.setProperty('density',state)
        return self.responsive(w,density)

    def gear_panel(self):
        w,v=self.column();meta=label('','sidebarMeta');load_button=button('Нагрузка',self.load_settings,'quietButton');meta_row=row(meta,load_button);v.addWidget(meta_row);self.refreshers.append(lambda:meta.setText(f"Нагрузка {engine.load(self.s):g} / {engine.derived(self.s)['Экипировка']:g}"))
        items=self.s['gear'];t=self.table(['Предмет','Кол.'],[0,56]);v.addWidget(t)
        for i,g in enumerate(items):t.insertRow(i);self.textitem(t,i,0,g['name']+('' if g.get('carried',True) else ' · оставлен'),g.get('description',''));self.textitem(t,i,1,g['qty'])
        self.fit_table(t,len(items),3,29);t.setVisible(bool(items))
        overflow=button('',lambda:self.toggle_table_all('gear',t),'quietButton');overflow.setObjectName('tableOverflow');v.addWidget(overflow)
        preview=label('','selectionPreview');preview.setWordWrap(True);preview.setMaximumHeight(68);v.addWidget(preview)
        details=Details(154);self.details['gear']=details;v.addWidget(details)
        def display(g):
            if g:details.display(g,self.s,'gear');preview.setText(self.preview_text(g,'gear'))
            else:preview.setText('')
        self.selection('gear',t,items,display)
        empty_box=None
        if not items:empty_box=self.empty(v,'Снаряжение не добавлено','Оружие, доспехи и личные вещи персонажа.',lambda:self.add_catalog('gear'),'Выбрать предмет')
        action_row=self.actions(v,'gear',t,items,lambda:self.add_catalog('gear'),lambda:self.edit_gear(new=True),self.edit_gear)
        def density(state,width,height):
            self.table_window(t,state,3,overflow);self.detail_visibility(details,bool(items) and state==EXPANDED,145,380);preview.setVisible(bool(items) and state==STANDARD);load_button.setVisible(state!=MINIMAL);action_row.setVisible(state!=MINIMAL)
            if empty_box:empty_box.setProperty('density',state)
        return self.responsive(w,density)

    def cyber_panel(self):
        w=Window.cyber_panel(self);t=w.findChild(QTableWidget);details=next((x for x in w.findChildren(QTextBrowser) if x.objectName()=='detailsPane'),None)
        if details:
            details.setVerticalScrollBarPolicy(Qt.ScrollBarAlwaysOff);details.setHorizontalScrollBarPolicy(Qt.ScrollBarAlwaysOff);details.document().contentsChanged.connect(lambda:QTimer.singleShot(0,lambda:self.fit_text_widget(details,120)))
        def density(state,width,height):
            if t:self.table_density(t,state,5)
            if details:
                self.detail_visibility(details,state==EXPANDED,120,360)
                if state==EXPANDED:self.fit_text_widget(details,120)
        return self.responsive(w,density)
    def modifiers_panel(self):
        w=Window.modifiers_panel(self);t=w.findChild(QTableWidget);intro=next((x for x in w.findChildren(QLabel) if x.text().startswith('Для временных эффектов')),None)
        def density(state,width,height):
            if t:self.table_density(t,state,6)
            if intro:intro.setVisible(state in (EXPANDED,STANDARD))
        return self.responsive(w,density)
    def checks_panel(self):
        w=Window.checks_panel(self);browsers=w.findChildren(QTextBrowser);out=browsers[0] if browsers else None;labels=w.findChildren(QLabel);costs=labels[-1] if labels else None
        if out:
            out.setVerticalScrollBarPolicy(Qt.ScrollBarAlwaysOff);out.setHorizontalScrollBarPolicy(Qt.ScrollBarAlwaysOff);out.document().contentsChanged.connect(lambda:QTimer.singleShot(0,lambda:self.fit_text_widget(out,72)))
        def density(state,width,height):
            if out:self.fit_text_widget(out,140 if state==EXPANDED else 96 if state==STANDARD else 68 if state==COMPACT else 50)
            if costs:costs.setVisible(state!=MINIMAL)
        return self.responsive(w,density)
    def notes_panel(self):
        w=Window.notes_panel(self);edit=w.findChild(QPlainTextEdit)
        if edit:
            edit.setVerticalScrollBarPolicy(Qt.ScrollBarAlwaysOff);edit.setHorizontalScrollBarPolicy(Qt.ScrollBarAlwaysOff);edit.document().contentsChanged.connect(lambda:QTimer.singleShot(0,lambda:self.fit_text_widget(edit,72)))
        def density(state,width,height):
            if edit:self.fit_text_widget(edit,170 if state==EXPANDED else 110 if state==STANDARD else 72 if state==COMPACT else 52)
        return self.responsive(w,density)
    def load_settings(self):
        opt=self.s['options'];r=self.edit('Нагрузка',[('loadAutomatic','Считать по снаряжению','bool',opt['loadAutomatic']),('loadManual','Нагрузка вручную','float',opt['loadManual'],0,999999)])
        if r:opt.update(r);self.changed()
    def autosave(self):
        if not self.autosave_enabled:return True
        if not self.store:return super().autosave()
        self._set_save_state('saving')
        try:
            self.capture_layout();self.store.save(self.s);self.path=self.store.character_path(self.s['id']);self.auto_path=self.path;self.library_id=str(self.s['id']);self.dirty=False;self._set_save_state('saved');return True
        except Exception as exc:
            self._set_save_state('error',str(exc));QMessageBox.warning(self,'Не удалось сохранить автоматически',str(exc));return False
    def save(self,as_new=False):
        if not self.store:return super().save(as_new)
        if as_new:
            base=(self.s['profile'].get('name') or 'character').replace('/','-').replace('\\','-')
            filename,selected=QFileDialog.getSaveFileName(self,'Экспортировать персонажа',str(Path.home()/(base+'.dubl')),'Персонаж Дубль (*.dubl);;JSON (*.json)')
            if not filename:return False
            path=Path(filename)
            if not path.suffix:path=path.with_suffix('.json' if 'JSON' in selected else '.dubl')
            try:
                self._set_save_state('saving');self.capture_layout();self.store.save(self.s);self.store.export(self.s['id'],path);self.dirty=False;self.update_summary();self._set_save_state('saved');return True
            except Exception as exc:self._set_save_state('error',str(exc));QMessageBox.critical(self,'Ошибка экспорта',str(exc));return False
        try:
            self._set_save_state('saving');self.capture_layout();self.store.save(self.s);self.path=self.store.character_path(self.s['id']);self.auto_path=self.path;self.library_id=str(self.s['id']);self.dirty=False;self.update_summary();self._set_save_state('saved');return True
        except Exception as exc:self._set_save_state('error',str(exc));QMessageBox.critical(self,'Ошибка сохранения',str(exc));return False
    def may_replace(self):
        return self.autosave()
    def new(self):
        if not self.store:return super().new()
        if not self.autosave():return
        try:cid=self.store.create()
        except Exception as exc:QMessageBox.critical(self,'Не удалось создать персонажа',str(exc));return
        self.switch_character(cid,save_current=False)
    def open(self):
        if not self.store:return super().open()
        filename,_=QFileDialog.getOpenFileName(self,'Импортировать персонажа',str(Path.home()),'Персонаж Дубль (*.dubl *.json *.bak);;Все файлы (*)')
        if not filename:return
        if not self.autosave():return
        try:cid=self.store.import_file(filename)
        except Exception as exc:QMessageBox.critical(self,'Не удалось импортировать персонажа',str(exc));return
        self.switch_character(cid,save_current=False)
    def help(self):
        QMessageBox.information(self,'Дубль · рабочий лист','Три колонки: персонаж и ресурсы, характеристики и умения, магия и снаряжение.\n\nПерсонажи хранятся во внутренней библиотеке. Откройте меню «Персонажи» для быстрого переключения или выберите «Все персонажи…». Изменения сохраняются автоматически — искать JSON-файлы не нужно.\n\nИмпорт и экспорт остаются для переноса листов между компьютерами. Экспорт по умолчанию создаёт файл .dubl.\n\nНижняя граница каждой карточки меняет её высоту. Двойной щелчок возвращает высоту по содержимому. Меню ⋯ позволяет перемещать и скрывать карточки.\n\nДвойной щелчок по записи открывает полное описание. «Ещё» содержит дополнительные действия.')
    def closeEvent(self,event):
        super().closeEvent(event)
        if event.isAccepted():
            for card in self.docks.values():
                if card.popup:card.popup.close()
            self.timer.stop()
