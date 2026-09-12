"""Дубль — native Qt desktop character sheet."""
from __future__ import annotations
import copy,json,sys,uuid
from pathlib import Path
from PySide6.QtCore import Qt,QTimer,QByteArray
from PySide6.QtGui import QAction,QKeySequence,QFont,QColor
from PySide6.QtWidgets import (QApplication,QMainWindow,QWidget,QVBoxLayout,QHBoxLayout,QFormLayout,QGridLayout,QDockWidget,QScrollArea,QLineEdit,QPlainTextEdit,QLabel,QCheckBox,QComboBox,QTableWidget,QTableWidgetItem,QHeaderView,QAbstractItemView,QMessageBox,QFileDialog,QDialog,QListWidget,QDialogButtonBox,QInputDialog,QTextBrowser,QMenu,QToolButton,QFrame)
from .model import fresh,normalize,load_file,atomic_save,data_dir,CATALOG,ENTRIES,ATTRS,number
from . import engine
from .widgets import Number,Editor,label,button,row
from .catalog import CatalogDialog,description

from .theme import elegant_dark, warm_light, fonts

class Window(QMainWindow):
    def __init__(self,state=None,autosave=True):
        super().__init__();self.path=None;self.s=state or fresh();self.autosave_enabled=autosave;self.docks={};self.refreshers=[];self.dirty=False;self.ready=False;self.building=False
        self.auto_path=data_dir()/'autosave.json'
        if state is None and self.auto_path.exists():
            try:self.s=load_file(self.auto_path)
            except Exception as exc:QTimer.singleShot(0,lambda msg=str(exc):QMessageBox.warning(self,'Не удалось восстановить лист',msg+'\nИсходный файл сохранён. Откройте резервную копию через меню Файл.'))
        self.resize(1400,920);self.setMinimumSize(780,560);self.setDockNestingEnabled(True)
        self.timer=QTimer(self);self.timer.setSingleShot(True);self.timer.setInterval(900);self.timer.timeout.connect(self.autosave)
        center=QWidget();center.setMaximumHeight(0);self.setCentralWidget(center)
        self.make_menus();self.rebuild(restore=True);self.ready=True;self.update_summary()
    def action(self,menu,text,fn,shortcut=None,checkable=False):
        a=QAction(text,self);a.setCheckable(checkable);a.triggered.connect(fn)
        if shortcut:a.setShortcut(QKeySequence(shortcut))
        menu.addAction(a);return a
    def make_menus(self):
        # Keep QMenu/QAction for shortcuts and accessibility, but present them
        # through a compact in-app header instead of the native menu bar.
        f=QMenu('Файл',self);self.file_menu=f
        self.action(f,'Новый персонаж',self.new,'Ctrl+N');self.action(f,'Открыть / импортировать…',self.open,'Ctrl+O');self.action(f,'Сохранить',self.save,'Ctrl+S');self.action(f,'Сохранить как…',lambda:self.save(True),'Ctrl+Shift+S');f.addSeparator();self.action(f,'Выйти',self.close,'Ctrl+Q')
        self.view=QMenu('Блоки',self)
        self.settings=QMenu('Вид',self);self.lock_action=self.action(self.settings,'Закрепить блоки',self.lock,checkable=True);self.action(self.settings,'Размер текста…',self.font_size);self.action(self.settings,'Светлый / тёмный интерфейс',self.toggle_theme);self.action(self.settings,'Сбросить расположение',self.reset_layout)
        a=QMenu('Добавить',self);self.add_menu=a
        for title,fn in [('Навык или способность',lambda:self.add_catalog('entries')),('Заклинание',lambda:self.add_catalog('spells')),('Предмет',lambda:self.add_catalog('gear')),('Имплант',lambda:self.add_catalog('implants')),('Свое умение / специализацию',self.add_skill),('Свой ресурс',self.add_resource),('Свой блок',self.add_block),('Поправку к расчётам',self.add_modifier)]:self.action(a,title,fn)
        h=QMenu('Справочник',self);self.reference_menu=h
        self.action(h,'Правила',lambda:self.browse('references'));self.action(h,'Навыки и способности',lambda:self.browse('entries'));self.action(h,'Заклинания',lambda:self.browse('spells'));self.action(h,'Импланты',lambda:self.browse('implants'));self.action(h,'Расхождения в книге',self.audit);self.action(h,'Как пользоваться',self.help)

        header=QWidget(self);header.setObjectName('appHeader');lay=QHBoxLayout(header);lay.setContentsMargins(10,7,10,7);lay.setSpacing(5)
        mark=QLabel('Д');mark.setObjectName('brandMark');lay.addWidget(mark)
        brand=QLabel('ДУБЛЬ');brand.setObjectName('brandText');lay.addWidget(brand)
        def nav(text,menu,role='navButton'):
            b=QToolButton(header);b.setObjectName(role);b.setText(text+'  ▾');b.setPopupMode(QToolButton.InstantPopup);b.setMenu(menu);b.setToolButtonStyle(Qt.ToolButtonTextOnly);lay.addWidget(b);return b
        nav('Файл',f);nav('Блоки',self.view);nav('Добавить',a,'addButton');nav('Справочник',h)
        lay.addStretch(1)
        self.header_name=QLabel('Новый персонаж');self.header_name.setObjectName('headerCharacter');lay.addWidget(self.header_name)
        self.header_meta=QLabel('');self.header_meta.setObjectName('headerMeta');lay.addWidget(self.header_meta)
        save=QToolButton(header);save.setObjectName('saveButton');save.setText('Сохранить');save.setToolTip('Сохранить персонажа · Ctrl+S');save.clicked.connect(lambda:self.save());lay.addWidget(save)
        nav('Вид',self.settings,'viewButton')
        self.setMenuWidget(header)
    def dock(self,key,title,body):
        d=QDockWidget(title,self);d.setObjectName(key);d.setMinimumWidth(220);d.setMinimumHeight(150);d.setAllowedAreas(Qt.AllDockWidgetAreas)
        scroll=QScrollArea();scroll.setWidgetResizable(True);scroll.setFrameShape(QFrame.NoFrame);scroll.setWidget(body);d.setWidget(scroll)
        self.addDockWidget(Qt.LeftDockWidgetArea,d);self.docks[key]=d
        toggle=d.toggleViewAction();toggle.setText(title);self.view.addAction(toggle)
        d.visibilityChanged.connect(lambda *_:self.layout_changed());d.topLevelChanged.connect(lambda *_:self.layout_changed());return d
    def column(self):
        w=QWidget();w.setObjectName('panelBody');v=QVBoxLayout(w);v.setContentsMargins(12,11,12,12);v.setSpacing(9);return w,v
    def rebuild(self,restore=False):
        self.building=True
        if self.docks and not restore:self.capture_layout()
        for d in self.docks.values():self.removeDockWidget(d);d.setObjectName('');d.setParent(None);d.deleteLater()
        self.docks={};self.refreshers=[];self.view.clear()
        self.dock('profile','Персонаж',self.profile_panel());self.dock('attributes','Характеристики',self.attributes_panel());self.dock('resources','Ресурсы',self.resources_panel());self.dock('derived','Показатели',self.derived_panel());self.dock('skills','Умения',self.skills_panel());self.dock('feats','Навыки и способности',self.feats_panel());self.dock('magic','Магия',self.magic_panel());self.dock('gear','Снаряжение',self.gear_panel());self.dock('cyber','Киберпанк',self.cyber_panel());self.dock('modifiers','Поправки',self.modifiers_panel());self.dock('checks','Проверка персонажа',self.checks_panel());self.dock('notes','Заметки',self.notes_panel())
        for b in self.s['customBlocks']:
            w,v=self.column();v.addWidget(button('Настроить блок',lambda b=b:self.edit_block(b)));ed=QPlainTextEdit(b['text']);ed.setMinimumHeight(150);ed.textChanged.connect(lambda b=b,ed=ed:self.assign(b,'text',ed.toPlainText()));v.addWidget(ed);self.dock('custom_'+b['id'],b['name'],w)
        p=self.docks
        # Build full-width rows first, then split each row horizontally.  This
        # produces independent width dividers in each row instead of one rigid
        # set of full-height columns.  It is much closer to a modern card grid.
        self.splitDockWidget(p['profile'],p['resources'],Qt.Vertical)
        self.splitDockWidget(p['profile'],p['attributes'],Qt.Horizontal);self.splitDockWidget(p['attributes'],p['skills'],Qt.Horizontal)
        self.splitDockWidget(p['resources'],p['feats'],Qt.Horizontal);self.splitDockWidget(p['feats'],p['magic'],Qt.Horizontal)
        for key in ['derived','checks','notes']:self.tabifyDockWidget(p['resources'],p[key])
        self.tabifyDockWidget(p['feats'],p['modifiers'])
        for key in ['gear','cyber']:self.tabifyDockWidget(p['magic'],p[key])
        for key in [k for k in p if k.startswith('custom_')]:self.tabifyDockWidget(p['resources'],p[key])
        p['resources'].raise_();p['feats'].raise_();p['magic'].raise_()
        self.resizeDocks([p['profile'],p['attributes'],p['skills']],[330,420,650],Qt.Horizontal)
        self.resizeDocks([p['resources'],p['feats'],p['magic']],[330,650,420],Qt.Horizontal)
        self.resizeDocks([p['profile'],p['resources']],[335,585],Qt.Vertical)
        if self.s['ui'].get('layout'):
            self.restoreState(QByteArray.fromBase64(self.s['ui']['layout'].encode()))
        if restore and self.s['ui'].get('geometry'):self.restoreGeometry(QByteArray.fromBase64(self.s['ui']['geometry'].encode()))
        self.lock_action.setChecked(bool(self.s['ui'].get('locked')));self.lock(self.lock_action.isChecked());self.apply_style();self.building=False;self.update_summary()
    def capture_layout(self):
        self.s['ui']['layout']=bytes(self.saveState().toBase64()).decode();self.s['ui']['geometry']=bytes(self.saveGeometry().toBase64()).decode()
    def layout_changed(self):
        if self.ready and not self.building:self.dirty=True;self.timer.start()
    def lock(self,checked=False):
        self.s['ui']['locked']=checked
        features=QDockWidget.DockWidgetClosable
        if not checked:features|=QDockWidget.DockWidgetMovable|QDockWidget.DockWidgetFloatable
        for d in self.docks.values():d.setFeatures(features)
        self.layout_changed()
    def font_size(self):
        size,ok=QInputDialog.getInt(self,'Размер текста','Размер, пт',self.s['ui']['fontSize'],9,18)
        if ok:self.s['ui']['fontSize']=size;self.apply_style();self.changed()
    def apply_style(self):
        app=QApplication.instance();app.setStyle('Fusion');body,_,_=fonts();app.setFont(QFont(body,self.s['ui']['fontSize']))
        self.setStyleSheet(elegant_dark(self.s['ui']['fontSize']) if self.s['ui'].get('theme')!='light' else warm_light(self.s['ui']['fontSize']))
    def toggle_theme(self):self.s['ui']['theme']='light' if self.s['ui'].get('theme')!='light' else 'dark';self.apply_style();self.changed()
    def reset_layout(self):self.s['ui']['layout']='';self.s['ui']['layoutGeneration']=3;self.rebuild(restore=True);self.changed()
    def assign(self,obj,key,value):obj[key]=value;self.changed()
    def changed(self):
        if self.building:return
        self.dirty=True;self.update_summary();self.timer.start()
    def update_summary(self):
        for fn in self.refreshers:fn()
        c=engine.costs(self.s);ap,budget=engine.ability_points(self.s);name=self.s['profile']['name'] or 'Новый персонаж'
        self.setWindowTitle(f'{name} — Дубль 0.10'+(' •' if self.dirty else ''))
        if hasattr(self,'header_name'):
            self.header_name.setText(name+('  •' if self.dirty else ''))
            self.header_meta.setText(f"Опыт {c['total']:g}/{self.s['profile']['xpTotal']:g}  ·  Способности {ap:g}/{budget:g}")
        self.statusBar().showMessage(f"Опыт: {c['total']:g} / {self.s['profile']['xpTotal']:g}     Способности: {ap:g} / {budget:g}")
    def edit(self,title,fields):
        d=Editor(self,title,fields);return d.values() if d.exec()==QDialog.Accepted else None
    def remove(self,items,item,title='Удалить запись?'):
        if QMessageBox.question(self,title,'Запись будет удалена из этого персонажа. Продолжить?',QMessageBox.Yes|QMessageBox.No,QMessageBox.No)==QMessageBox.Yes:items.remove(item);self.rebuild();self.changed()
    def table(self,headers,widths=None):
        t=QTableWidget(0,len(headers));t.setHorizontalHeaderLabels(headers);t.setSelectionBehavior(QAbstractItemView.SelectRows);t.setSelectionMode(QAbstractItemView.SingleSelection);t.setEditTriggers(QAbstractItemView.NoEditTriggers);t.verticalHeader().hide();t.setWordWrap(True);t.setAlternatingRowColors(True);t.setShowGrid(False);t.setMinimumHeight(130)
        for i in range(len(headers)):t.horizontalHeader().setSectionResizeMode(i,QHeaderView.Interactive)
        t.horizontalHeader().setSectionResizeMode(0,QHeaderView.Stretch)
        if widths:
            for i,width in enumerate(widths[1:],1):t.setColumnWidth(i,width)
        t.horizontalHeader().setMinimumSectionSize(38);return t
    def textitem(self,t,r,c,text,tip=''):
        it=QTableWidgetItem(str(text));it.setToolTip(tip or str(text));t.setItem(r,c,it);return it
    def selected(self,t,items):
        r=t.currentRow();return items[r] if 0<=r<len(items) else None
    def profile_panel(self):
        w,v=self.column();f=QFormLayout();f.setRowWrapPolicy(QFormLayout.WrapLongRows);f.setHorizontalSpacing(12);f.setVerticalSpacing(9);v.addLayout(f);p=self.s['profile']
        for key,title in [('name','Имя'),('concept','Концепция')]:
            ed=QLineEdit(p[key]);ed.textChanged.connect(lambda text,k=key:self.assign(p,k,text));f.addRow(label(title,'fieldLabel'),ed)
        for key,title,lo,hi in [('size','Размер',1,10),('xpTotal','Всего опыта',0,999999),('creationXp','Опыт при создании',0,999999)]:
            n=Number(p[key],lo,hi);n.changed.connect(lambda x,k=key:self.assign(p,k,int(x)));f.addRow(label(title,'fieldLabel'),n)
        cb=QCheckBox('Поправки характеристик от размера');cb.setChecked(self.s['options']['sizeModifiers']);cb.toggled.connect(lambda b:self.assign(self.s['options'],'sizeModifiers',b));v.addWidget(cb)
        v.addWidget(button('Настроить очки способностей',self.ability_budget,'primaryButton'));v.addStretch();return w
    def ability_budget(self):
        p=self.s['profile'];_,budget=engine.ability_points(self.s);r=self.edit('Очки способностей',[('auto','По начальному опыту','bool',p.get('abilityBudget') is None),('value','Лимит вручную','int',budget,0,9999)])
        if r:p['abilityBudget']=None if r['auto'] else r['value'];self.changed()
    def attributes_panel(self):
        w,v=self.column();g=QGridLayout();g.setHorizontalSpacing(10);g.setVerticalSpacing(9);g.setAlignment(Qt.AlignTop);g.setColumnStretch(1,1);v.addLayout(g);outputs={}
        for i,a in enumerate(ATTRS):
            n=Number(self.s['attributes'][a],-5,10);n.changed.connect(lambda x,a=a:self.assign(self.s['attributes'],a,int(x)));g.addWidget(label(a,'statName'),i,0);g.addWidget(n,i,1);out=label('','statResult');out.setAlignment(Qt.AlignCenter);out.setMinimumWidth(54);g.addWidget(out,i,2);outputs[a]=out
        v.addStretch()
        def update():
            values=engine.attrs(self.s)
            for a,o in outputs.items():o.setText(f'{values[a]:g}');o.setToolTip('Итоговое значение с учетом размера и поправок.')
        self.refreshers.append(update);return w
    def resources_panel(self):
        w,v=self.column();cb=QCheckBox('Показывать ману');cb.setChecked(self.s['resources']['manaVisible']);cb.toggled.connect(self.mana_visible);v.addWidget(cb)
        def resource(title,obj,key,maxfn,custom=None):
            v.addWidget(label(title,'sectionLabel'));n=Number(obj.get(key) if obj.get(key) is not None else maxfn(),-999999,999999);n.changed.connect(lambda x:self.assign(obj,key,x));mx=label('');b=button('Полный',lambda:self.assign(obj,key,maxfn()),'quietButton');v.addWidget(row(n,mx,b))
            if custom:v.addWidget(button('Настроить '+title,lambda:self.edit_resource(custom)))
            def refresh():mx.setText('/ '+format(maxfn(),'g'));n.setValue(obj.get(key) if obj.get(key) is not None else maxfn())
            self.refreshers.append(refresh)
        resource('Здоровье',self.s['resources'],'healthCurrent',lambda:engine.derived(self.s)['Здоровье'])
        resource('Выносливость',self.s['resources'],'staminaCurrent',lambda:engine.derived(self.s)['Выносливость'])
        if self.s['resources']['manaVisible']:resource('Мана',self.s['magic'],'currentMana',lambda:engine.mana(self.s))
        for r in self.s['resources']['custom']:resource(r['name'],r,'current',lambda r=r:r['max'],r)
        v.addWidget(button('+ Свой ресурс',self.add_resource,'primaryButton'));v.addStretch();return w
    def mana_visible(self,b):self.s['resources']['manaVisible']=b;self.rebuild();self.changed()
    def add_resource(self):self.edit_resource()
    def edit_resource(self,item=None):
        r=item or {'name':'','current':0,'max':1};d=self.edit('Ресурс',[('name','Название','line',r['name']),('current','Сейчас','float',r['current']),('max','Максимум','float',r['max'],0,999999),('delete','Удалить ресурс','bool',False)]);
        if d:
            if d.pop('delete'):
                if item:self.s['resources']['custom'].remove(item)
            elif d['name']:
                if item is None:item={'id':str(uuid.uuid4())};self.s['resources']['custom'].append(item)
                item.update(d)
            self.rebuild();self.changed()
    def derived_panel(self):
        w,v=self.column();g=QGridLayout();g.setHorizontalSpacing(12);g.setVerticalSpacing(7);g.setAlignment(Qt.AlignTop);g.setColumnStretch(0,1);v.addLayout(g)
        keys=[k for k in engine.derived(self.s) if k not in ['load','burden','loadPenalty']];values={}
        for i,k in enumerate(keys):
            g.addWidget(label(k,'statName'),i,0);val=label('','statValue');val.setAlignment(Qt.AlignCenter);val.setMinimumWidth(58);g.addWidget(val,i,1);values[k]=val
        load_name=label('Нагрузка','sectionLabel');load_value=label('','statValue');load_value.setAlignment(Qt.AlignCenter);g.addWidget(load_name,len(keys)+1,0);g.addWidget(load_value,len(keys)+1,1);burden=label('','mutedLabel');g.addWidget(burden,len(keys)+2,0,1,2);v.addStretch()
        def update():
            d=engine.derived(self.s)
            for k,val in values.items():val.setText(f'{d[k]:g}')
            load_value.setText(f"{d['load']:g}");burden.setText(f"{d['burden']}  ·  штраф {d['loadPenalty']:+g}")
        self.refreshers.append(update);return w
    def skills_panel(self):
        w,v=self.column();search=QLineEdit();search.setObjectName('searchField');search.setPlaceholderText('Найти умение…');v.addWidget(search);items=[x for x in engine.skills(self.s) if x['name'] not in self.s['ui']['hiddenSkills'] and not x.get('hidden')];t=self.table(['Умение','Ранг','Итог'],[0,118,56]);v.addWidget(t,1)
        for i,sk in enumerate(items):
            t.insertRow(i);self.textitem(t,i,0,sk['name'],sk.get('description',''));n=Number(sk.get('rank',0),0,10);n.changed.connect(lambda value,sk=sk:self.set_skill_rank(sk,value));t.setCellWidget(i,1,n);self.textitem(t,i,2,'');t.setRowHeight(i,max(44,36*((len(sk['name'])//22)+1)))
        details=QTextBrowser();details.setObjectName('detailsPane');details.setMinimumHeight(110);details.setMaximumHeight(230);v.addWidget(details)
        def detail():
            sk=self.selected(t,items)
            if sk:details.setPlainText(sk['name']+'\n\n'+sk.get('description','')+'\n\nХарактеристики: '+' + '.join(engine.skill_attrs(sk))+'\n'+engine.skill_formula_text(self.s,sk))
        t.itemSelectionChanged.connect(detail)
        search.textChanged.connect(lambda text:[t.setRowHidden(i,engine.norm(text) not in engine.norm(sk['name'])) for i,sk in enumerate(items)])
        v.addWidget(row(button('+ Умение',self.add_skill,'primaryButton'),button('Настроить',lambda:self.edit_skill(self.selected(t,items))),button('Скрыть',lambda:self.hide_skill(self.selected(t,items)))))
        v.addWidget(button('Вернуть скрытые умения',self.restore_skills,'quietButton'))
        def update():
            current={x['name']:x for x in engine.skills(self.s)}
            for i,sk in enumerate(items):
                live=current[sk['name']];value=engine.skill_bonus(self.s,live);t.item(i,2).setText('—' if value is None else f'{value:+g}');t.item(i,2).setToolTip(engine.skill_formula_text(self.s,live))
        self.refreshers.append(update)
        if items:t.setCurrentCell(0,0)
        return w
    def skill_obj(self,sk):
        if sk.get('custom'):return next(x for x in self.s['customSkills'] if x['name']==sk['name'])
        default=sk.get('defaultAttr','Интеллект');return self.s['skills'].setdefault(sk['name'],{'rank':0,'attr':default,'attrs':[default],'mod':0,'formulaNote':''})
    def set_skill_rank(self,sk,value):self.assign(self.skill_obj(sk),'rank',int(value))
    def add_skill(self):self.edit_skill(new=True)
    def edit_skill(self,sk=None,new=False):
        if not sk and not new:return
        sk=sk or {'name':'','rank':0,'attr':'Интеллект','attrs':['Интеллект'],'mod':0,'formulaNote':'','description':'','untrained':'Да'}
        selected_attrs=engine.skill_attrs(sk)
        fields=[('name','Название / специализация','line',sk['name']),('rank','Ранг','int',sk.get('rank',0),0,10),('attrs','Характеристики','multi',selected_attrs,ATTRS),('mod','Поправка','float',sk.get('mod',0)),('formulaNote','Примечание к расчёту','line',sk.get('formulaNote','')),('description','Описание','text',sk.get('description','')),('untrained','Без обучения','choice',sk.get('untrained','Да'),['Да','Нет','-2'])]
        r=self.edit('Умение',fields)
        if r and r['name']:
            if not r.get('attrs'):
                QMessageBox.information(self,'Нужна характеристика','Выберите хотя бы одну характеристику, от которой считается умение.');return
            r['attr']=r['attrs'][0]
            if (new or r['name']!=sk['name']) and any(engine.norm(x['name'])==engine.norm(r['name']) for x in engine.skills(self.s)):QMessageBox.information(self,'Умение уже есть','Используйте существующую запись.');return
            if new:self.s['customSkills'].append(r)
            else:
                # Core identities stay fixed; editable labels belong to custom skills.
                target=self.skill_obj(sk)
                if not sk.get('custom'):r['name']=sk['name']
                target.update(r)
            self.rebuild();self.changed()
    def hide_skill(self,sk):
        if sk:self.s['ui']['hiddenSkills'].append(sk['name']);self.rebuild();self.changed()
    def restore_skills(self):
        d=QDialog(self);d.setWindowTitle('Вернуть умения');v=QVBoxLayout(d);choices=[]
        for sk in engine.skills(self.s):
            if sk['name'] in self.s['ui']['hiddenSkills'] or sk.get('hidden'):
                cb=QCheckBox(sk['name']);v.addWidget(cb);choices.append((sk,cb))
        b=QDialogButtonBox(QDialogButtonBox.Ok|QDialogButtonBox.Cancel);b.accepted.connect(d.accept);b.rejected.connect(d.reject);v.addWidget(b)
        if d.exec()==QDialog.Accepted:
            for sk,cb in choices:
                if cb.isChecked():self.s['ui']['hiddenSkills']=[x for x in self.s['ui']['hiddenSkills'] if x!=sk['name']];self.skill_obj(sk)['hidden']=False
            self.rebuild();self.changed()
    def feats_panel(self):
        w,v=self.column();items=self.s['feats'];t=self.table(['Навык / способность','Ранг'],[0,55]);v.addWidget(t,1)
        for i,f in enumerate(items):
            e=engine.record(f);t.insertRow(i);self.textitem(t,i,0,e['name']+(' · '+f['choice'] if f.get('choice') else ''),description(e,self.s));self.textitem(t,i,1,f['rank']);t.setRowHeight(i,52)
        details=QTextBrowser();details.setObjectName('detailsPane');details.setMinimumHeight(180);v.addWidget(details,1)
        def detail():
            f=self.selected(t,items)
            for i,entry in enumerate(items):
                bad=[c.state for c in engine.requirements(self.s,engine.record(entry)) if c.state!='ok']
                color='#de7882' if 'fail' in bad else '#d6b66d' if bad else '#78b5a5'
                if entry.get('overrideReason'):color='#86aabe'
                t.item(i,0).setForeground(QColor(color));t.item(i,0).setToolTip(description(engine.record(entry),self.s))
            if f:details.setPlainText(description(engine.record(f),self.s)+('\n\nРешение мастера: '+f['overrideReason'] if f.get('overrideReason') else '')+('\n\nЗаметка: '+f['note'] if f.get('note') else ''))
        t.itemSelectionChanged.connect(detail);self.refreshers.append(detail)
        if items:t.setCurrentCell(0,0)
        v.addWidget(row(button('+ Из книги',lambda:self.add_catalog('entries'),'primaryButton'),button('+ Свой',self.custom_feat)))
        v.addWidget(row(button('Изменить',lambda:self.edit_feat(self.selected(t,items))),button('Удалить',lambda:self.remove_selected(t,items))))
        return w
    def remove_selected(self,t,items):
        item=self.selected(t,items)
        if item is not None:self.remove(items,item)
    def edit_feat(self,f):
        if not f:return
        e=engine.record(f);fields=[('rank','Ранг','int',f['rank'],1,e.get('ranks') or 99),('choice','Выбранная специализация / цель','line',f.get('choice','')),('note','Заметка','text',f.get('note','')),('overrideReason','Решение мастера по требованиям','text',f.get('overrideReason',''))]
        opts=e.get('abilityOptions',[])
        if opts:fields.append(('optionText','Источник способности','choice',opts[f.get('option',0)]['source'],[o['source'] for o in opts]))
        r=self.edit(e['name'],fields)
        if r:
            if opts:
                selected_source=r.pop('optionText')
                r['option']=next(i for i,o in enumerate(opts) if o['source']==selected_source)
            if any(x is not f and x.get('id')==f.get('id') and (not e.get('repeatable') or engine.norm(x.get('choice'))==engine.norm(r['choice'])) for x in self.s['feats']):QMessageBox.warning(self,'Повторная запись','Такая специализация уже добавлена.');return
            f.update(r);self.rebuild();self.changed()
    def custom_feat(self):
        r=self.edit('Свой навык или способность',[('name','Название','line',''),('description','Эффект','text',''),('requirements','Требования','text',''),('cost','Цена за ранг','int',0,0,999999),('costType','Оплата','choice','Опыт',['Опыт','Очки способностей']),('rank','Ранг','int',1,1,99)])
        if r and r['name']:r['costType']='xp' if r['costType']=='Опыт' else 'ability';r.update(id='custom_'+str(uuid.uuid4()),uid=str(uuid.uuid4()),custom=True,choice='',note='');self.s['feats'].append(r);self.rebuild();self.changed()
    def browse(self,kind):CatalogDialog(self,self.s,kind,read_only=True).exec()
    def add_catalog(self,kind):
        d=CatalogDialog(self,self.s,kind)
        if d.exec()!=QDialog.Accepted or not d.selected:return
        e=copy.deepcopy(d.selected)
        if kind=='entries':
            if e['name']=='Базовый запас маны':
                self.docks['magic'].show();self.docks['magic'].raise_();QMessageBox.information(self,'Запас маны','Ранг запаса маны меняется в блоке «Магия». Он оплачивается один раз.');return
            checks=engine.requirements(self.s,e);reason=''
            if any(c.state!='ok' for c in checks):
                reason,ok=QInputDialog.getMultiLineText(self,'Требования навыка','\n'.join(c.text for c in checks if c.state!='ok')+'\n\nУкажите решение мастера, чтобы добавить навык:')
                if not ok or not reason.strip():return
            fields=[('rank','Ранг','int',1,1,e['ranks']),('choice','Специализация / цель (если нужна)','line','')]
            opts=e.get('abilityOptions',[])
            if opts:fields.append(('source','Источник способности','choice',opts[0]['source'],[o['source'] for o in opts]))
            r=self.edit(e['name'],fields)
            if not r:return
            if any(f['id']==e['id'] and (not e.get('repeatable') or engine.norm(f.get('choice'))==engine.norm(r['choice'])) for f in self.s['feats']):QMessageBox.information(self,'Уже добавлено','Измените ранг существующей записи.');return
            if e.get('costType')=='ability' and not e.get('repeatable') and any(f['id']==e['id'] for f in self.s['feats']):QMessageBox.information(self,'Уже добавлено','Эта способность уже открыта.');return
            r.update(id=e['id'],uid=str(uuid.uuid4()),note='',overrideReason=reason.strip(),option=next((i for i,o in enumerate(opts) if o['source']==r.get('source')),0));r.pop('source',None);self.s['feats'].append(r)
        elif kind=='spells':
            if any(x.get('id')==e['id'] or engine.norm(x.get('name'))==engine.norm(e['name']) for x in self.s['magic']['spells']):QMessageBox.information(self,'Уже добавлено','Это заклинание уже есть в листе.');return
            e.update(uid=str(uuid.uuid4()),learned=True);self.s['magic']['spells'].append(e)
        elif kind=='gear':
            fields=e.get('fields',{});e.update(uid=str(uuid.uuid4()),qty=1,carried=True,load=number(str(fields.get('Треб.',fields.get('Требование','0'))).replace(',','.')));self.s['gear'].append(e)
        else:e.update(uid=str(uuid.uuid4()),active=True);self.s['cyber']['implants'].append(e);self.s['cyber']['enabled']=True
        self.rebuild();self.changed()
    def magic_panel(self):
        w,v=self.column();m=self.s['magic'];f=QFormLayout();f.setRowWrapPolicy(QFormLayout.WrapLongRows);v.addLayout(f)
        for k,title,lo,hi in [('manaRank','Ранг запаса маны',0,5),('power','Сила магии',0,999)]:
            n=Number(m[k],lo,hi);n.changed.connect(lambda value,k=k:self.magic_value(k,value));f.addRow(label(title,'fieldLabel'),n)
        total=label('');v.addWidget(total);self.refreshers.append(lambda:total.setText(f"Максимум маны: {engine.mana(self.s):g}\nВосстановление за раунд: {1+engine.rank(self.s,'Медитация'):g}"))
        v.addWidget(button('Школы магии',self.schools));t=self.table(['Заклинание','Мана'],[0,58]);items=m['spells'];v.addWidget(t,1)
        for i,sp in enumerate(items):t.insertRow(i);self.textitem(t,i,0,sp['name'],description(sp));self.textitem(t,i,1,sp.get('cost',0));t.setRowHeight(i,44)
        details=QTextBrowser();details.setObjectName('detailsPane');details.setMinimumHeight(160);v.addWidget(details,1);t.itemSelectionChanged.connect(lambda:details.setPlainText(description(self.selected(t,items)) if self.selected(t,items) else ''))
        v.addWidget(row(button('+ Из книги',lambda:self.add_catalog('spells')),button('+ Своё',lambda:self.edit_spell(new=True))))
        v.addWidget(row(button('Изменить',lambda:self.edit_spell(self.selected(t,items))),button('Удалить',lambda:self.remove_selected(t,items))))
        return w
    def magic_value(self,k,v):
        self.s['magic'][k]=int(v)
        if k=='manaRank' and v>0 and self.s['magic']['power']<1:self.s['magic']['power']=1;self.rebuild()
        if k=='power' and v<1 and self.s['magic']['manaRank']>0:self.s['magic']['power']=1;self.rebuild()
        self.changed()
    def edit_spell(self,sp=None,new=False):
        if sp is None and not new:return
        sp=sp or {};fields=[('name','Название','line',sp.get('name','')),('school','Школа','line',sp.get('school','')),('cost','Стоимость маны','int',sp.get('cost',0),0,999),('learned','Изучено (тратить опыт)','bool',sp.get('learned',True)),('manualXp','Цена изучения вручную','bool',sp.get('xpOverride') is not None),('xpOverride','Опыт за изучение','int',sp.get('xpOverride') or engine.learn_cost(sp.get('cost')) or 0,0,999999)]
        fields += [(k,title,'text' if k in ['description','enhancement'] else 'line',sp.get(k,'')) for k,title in [('time','Время'),('range','Дальность'),('action','Проверка'),('duration','Длительность'),('description','Описание'),('enhancement','Усиление')]]
        r=self.edit('Заклинание',fields)
        if r and r['name']:
            if any(x is not sp and engine.norm(x['name'])==engine.norm(r['name']) for x in self.s['magic']['spells']):QMessageBox.warning(self,'Повтор','Заклинание уже добавлено.');return
            if not r.pop('manualXp'):r['xpOverride']=None
            r['manaText']=str(r['cost'])
            if new:sp={'uid':str(uuid.uuid4())};self.s['magic']['spells'].append(sp)
            sp.update(r);self.rebuild();self.changed()
    def schools(self):
        d=QDialog(self);d.setWindowTitle('Школы магии');d.resize(540,500);v=QVBoxLayout(d);v.addWidget(label('Уровни и заметки школ хранятся в листе. Их стоимость и развитие согласуются с мастером.'));lst=QListWidget();v.addWidget(lst)
        def refresh():lst.clear();lst.addItems([f"{x.get('name','Школа')}: {x.get('rank',x.get('level',0))}" for x in self.s['magic']['schools']])
        def edit(new=False):
            i=lst.currentRow();items=self.s['magic']['schools']
            if not new and i<0:return
            x={} if new else items[i];r=self.edit('Школа',[('name','Название','line',x.get('name','')),('rank','Уровень','int',x.get('rank',x.get('level',0)),0,99),('note','Заметки','text',x.get('note',''))])
            if r and r['name']:
                if new:items.append(r)
                else:x.update(r)
                refresh();self.changed()
        def remove():
            if lst.currentRow()>=0:self.s['magic']['schools'].pop(lst.currentRow());refresh();self.changed()
        v.addWidget(row(button('Добавить',lambda:edit(True)),button('Изменить',edit),button('Удалить',remove)));refresh();d.exec()
    def gear_panel(self):
        w,v=self.column();auto=QCheckBox('Считать нагрузку по снаряжению');auto.setChecked(self.s['options']['loadAutomatic']);auto.toggled.connect(lambda b:self.assign(self.s['options'],'loadAutomatic',b));v.addWidget(auto);v.addWidget(label('Нагрузка вручную'));n=Number(self.s['options']['loadManual'],0,999999,2);n.changed.connect(lambda x:self.assign(self.s['options'],'loadManual',x));v.addWidget(n);items=self.s['gear'];t=self.table(['Предмет','Кол.','Нагр.'],[0,50,65]);v.addWidget(t,1)
        for i,g in enumerate(items):t.insertRow(i);self.textitem(t,i,0,g['name']+('' if g.get('carried',True) else ' · оставлен'),g.get('description',''));self.textitem(t,i,1,g['qty']);self.textitem(t,i,2,g.get('load',0));t.setRowHeight(i,48)
        details=QTextBrowser();details.setObjectName('detailsPane');details.setMinimumHeight(140);v.addWidget(details);t.itemSelectionChanged.connect(lambda:details.setPlainText(description(self.selected(t,items)) if self.selected(t,items) else ''))
        v.addWidget(row(button('+ Из книги',lambda:self.add_catalog('gear')),button('+ Свой',lambda:self.edit_gear(new=True))));v.addWidget(row(button('Изменить',lambda:self.edit_gear(self.selected(t,items))),button('Удалить',lambda:self.remove_selected(t,items))));return w
    def edit_gear(self,g=None,new=False):
        if g is None and not new:return
        g=g or {};r=self.edit('Предмет',[('name','Название','line',g.get('name','')),('qty','Количество','int',g.get('qty',1),0,999999),('load','Нагрузка одного предмета','float',g.get('load',0),0,999999),('carried','На персонаже','bool',g.get('carried',True)),('description','Свойства и описание','text',g.get('description',''))])
        if r and r['name']:
            if new:g={'uid':str(uuid.uuid4())};self.s['gear'].append(g)
            g.update(r);self.rebuild();self.changed()
    def cyber_panel(self):
        w,v=self.column();cb=QCheckBox('Использовать киберпанк');cb.setChecked(self.s['cyber']['enabled']);cb.toggled.connect(lambda x:self.assign(self.s['cyber'],'enabled',x));v.addWidget(cb);out=label('');v.addWidget(out);self.refreshers.append(lambda:out.setText('Лимит имплантов: %g / %g'%engine.cyber_limit(self.s)))
        mode=QComboBox();modes={'По размеру, телосложению или воле':'scale','По телосложению или воле':'body_will','Слоты: половина телосложения':'slots'};mode.addItems(list(modes));mode.setCurrentText(next((k for k,x in modes.items() if x==self.s['cyber']['limitMode']),next(iter(modes))));mode.currentTextChanged.connect(lambda x:self.assign(self.s['cyber'],'limitMode',modes[x]));v.addWidget(mode)
        items=self.s['cyber']['implants'];t=self.table(['Имплант','Лимит'],[0,60]);v.addWidget(t,1)
        for i,x in enumerate(items):t.insertRow(i);self.textitem(t,i,0,x['name']+('' if x.get('active',True) else ' · снят'),description(x));self.textitem(t,i,1,x.get('limit',0));t.setRowHeight(i,50)
        details=QTextBrowser();details.setObjectName('detailsPane');details.setMinimumHeight(160);v.addWidget(details);t.itemSelectionChanged.connect(lambda:details.setPlainText(description(self.selected(t,items)) if self.selected(t,items) else ''))
        v.addWidget(row(button('+ Из книги',lambda:self.add_catalog('implants')),button('+ Свой',lambda:self.edit_implant(new=True))));v.addWidget(row(button('Изменить',lambda:self.edit_implant(self.selected(t,items))),button('Удалить',lambda:self.remove_selected(t,items))));v.addWidget(label('Численные эффекты имплантов добавляются в блоке «Поправки».'));return w
    def edit_implant(self,x=None,new=False):
        if x is None and not new:return
        x=x or {};r=self.edit('Имплант',[(k,title,kind,x.get(k,default),*bounds) for k,title,kind,default,bounds in [('name','Название','line','',[]),('slot','Размещение','line','',[]),('limit','Занятый лимит','float',0,[0,999]),('limitBonus','Бонус лимита','float',0,[-999,999]),('active','Установлен','bool',True,[]),('effect','Эффект','text','',[])]])
        if r and r['name']:
            if new:x={'uid':str(uuid.uuid4())};self.s['cyber']['implants'].append(x)
            x.update(r);self.rebuild();self.changed()
    def modifiers_panel(self):
        w,v=self.column();v.addWidget(label('Для временных эффектов, снаряжения и правил мастера. Постоянные бонусы базовых навыков учитываются автоматически.'));items=self.s['modifiers'];t=self.table(['Источник / показатель','Значение'],[0,85]);v.addWidget(t,1)
        for i,m in enumerate(items):t.insertRow(i);self.textitem(t,i,0,m.get('name','')+' → '+m['target']);self.textitem(t,i,1,str(m.get('value',0))+('' if m.get('active',True) else ' (выкл.)'));t.setRowHeight(i,52)
        v.addWidget(row(button('Добавить',self.add_modifier),button('Изменить',lambda:self.edit_modifier(self.selected(t,items))),button('Удалить',lambda:self.remove_selected(t,items))));return w
    def add_modifier(self):self.edit_modifier(new=True)
    def edit_modifier(self,m=None,new=False):
        if m is None and not new:return
        m=m or {};targets=ATTRS+[k for k in engine.derived(self.s) if k not in ['load','burden','loadPenalty']]+['Мана','Лимит имплантов','Стоимость опыта']+['Умение: '+x['name'] for x in engine.skills(self.s)]
        r=self.edit('Поправка',[('name','Источник','line',m.get('name','')),('target','Показатель','choice',m.get('target','Здоровье'),targets),('value','Значение','float',m.get('value',0)),('active','Действует','bool',m.get('active',True))])
        if r:
            if new:m={};self.s['modifiers'].append(m)
            m.update(r);self.rebuild();self.changed()
    def checks_panel(self):
        w,v=self.column();out=QTextBrowser();out.setMinimumHeight(180);v.addWidget(out);self.refreshers.append(lambda:out.setPlainText('\n\n'.join(engine.warnings(self.s)) or 'Проверяемые условия выполнены.'))
        costs=label('');v.addWidget(costs);self.refreshers.append(lambda:costs.setText('\n'.join(f'{k}: {x:g}' for k,x in engine.costs(self.s).items() if k!='total')));return w
    def notes_panel(self):
        w,v=self.column();ed=QPlainTextEdit(self.s['profile'].get('notes',''));ed.setMinimumHeight(200);ed.textChanged.connect(lambda:self.assign(self.s['profile'],'notes',ed.toPlainText()));v.addWidget(ed);return w
    def add_block(self):self.edit_block()
    def edit_block(self,b=None):
        x=b or {};r=self.edit('Свой блок',[('name','Название','line',x.get('name','')),('text','Содержание','text',x.get('text','')),('delete','Удалить блок','bool',False)])
        if r:
            if r.pop('delete'):
                if b:self.s['customBlocks'].remove(b)
            elif r['name']:
                if b is None:b={'id':str(uuid.uuid4())};self.s['customBlocks'].append(b)
                b.update(r)
            self.rebuild();self.changed()
    def audit(self):
        path=Path(__file__).resolve().parent.parent/'RULES_AUDIT.md';d=QDialog(self);d.setWindowTitle('Сверка книги');d.resize(820,650);v=QVBoxLayout(d);t=QTextBrowser();t.setPlainText(path.read_text(encoding='utf-8') if path.exists() else 'Отчёт находится в data/audit.json.');v.addWidget(t);d.exec()
    def help(self):
        QMessageBox.information(self,'Дубль 0.10','Перетаскивайте блоки за заголовки. Меняйте размеры, потянув разделитель. Можно объединять блоки во вкладки и выносить в отдельные окна.\n\nМеню «Блоки» возвращает скрытые разделы. «Вид» меняет текст, оформление и закрепление.\n\nВыберите запись, чтобы прочитать описание; наведите указатель на название для подсказки.\n\nИзменения автоматически сохраняются локально. «Файл → Сохранить как» создаёт переносимый JSON. Импорт 0.9: экспортируйте JSON из старого приложения и откройте его здесь.\n\nНеизвестные требования требуют записанного решения мастера. Удаление навыка заново проверяет зависимые навыки.\n\nСвои ресурсы и блоки: меню «Добавить».')
    def autosave(self):
        if not self.autosave_enabled:return True
        try:self.capture_layout();atomic_save(self.auto_path,self.s);return True
        except Exception as exc:self.statusBar().showMessage('Не удалось сохранить автоматически: '+str(exc));return False
    def save(self,as_new=False):
        path=self.path
        if as_new or path is None:
            filename,_=QFileDialog.getSaveFileName(self,'Сохранить персонажа',str(path or Path.home()/'dubl-character.json'),'Персонаж (*.json)')
            if not filename:return False
            path=Path(filename)
            if not path.suffix:path=path.with_suffix('.json')
        try:self.capture_layout();atomic_save(path,self.s);self.path=path;self.dirty=False;self.update_summary();return True
        except Exception as exc:QMessageBox.critical(self,'Ошибка сохранения',str(exc));return False
    def may_replace(self):
        if not self.dirty:return True
        result=QMessageBox.question(self,'Сохранить текущего персонажа?','Перед заменой листа сохранить текущие изменения?',QMessageBox.Save|QMessageBox.Discard|QMessageBox.Cancel,QMessageBox.Save)
        return self.save() if result==QMessageBox.Save else result==QMessageBox.Discard
    def new(self):
        if not self.may_replace():return
        self.timer.stop();self.s=fresh();self.path=None;self.rebuild(restore=True);self.changed()
    def open(self):
        filename,_=QFileDialog.getOpenFileName(self,'Открыть персонажа / импорт 0.9',str(Path.home()),'Персонаж (*.json *.bak);;Все файлы (*)')
        if not filename:return
        try:state=load_file(filename)
        except Exception as exc:QMessageBox.critical(self,'Не удалось открыть файл',str(exc));return
        if not self.may_replace():return
        self.timer.stop();self.s=state;self.path=None if state.get('migrationNotes') else Path(filename);self.rebuild(restore=True);self.dirty=True;self.changed()
        if state['migrationNotes']:QMessageBox.information(self,'Импорт завершён','\n'.join(state['migrationNotes'])+'\n\nПроверьте блок «Проверка персонажа» и сохраните новый JSON.')
    def closeEvent(self,event):
        self.timer.stop()
        if not self.autosave():
            if not self.save():event.ignore();return
        event.accept()

def main():
    app=QApplication(sys.argv);app.setApplicationName('Дубль');app.setOrganizationName('Dubl');app.setStyle('Fusion')
    from .library_store import CharacterStore
    from .character_library import CharacterLibraryDialog
    from .shell import CharacterWindow
    store=CharacterStore();store.migrate_legacy_autosave()
    character_id=None
    # Opening an exported .dubl/.json file from the OS imports it into the
    # local library and opens it immediately.
    if len(sys.argv)>1:
        candidate=Path(sys.argv[1])
        if candidate.exists() and candidate.is_file():
            try:character_id=store.import_file(candidate)
            except Exception as exc:QMessageBox.critical(None,'Не удалось открыть персонажа',str(exc))
    if character_id is None:
        chooser=CharacterLibraryDialog(store,None)
        if chooser.exec()!=QDialog.Accepted or not chooser.selected_id:return
        character_id=chooser.selected_id
    try:state=store.load(character_id)
    except Exception as exc:
        QMessageBox.critical(None,'Не удалось открыть персонажа',str(exc));return
    window=CharacterWindow(state,store=store,character_id=character_id);window.show();sys.exit(app.exec())
if __name__=='__main__':main()
