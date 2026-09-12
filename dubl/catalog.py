"""Searchable Qt catalog, with rule checks and complete, selectable descriptions."""
from PySide6.QtCore import Qt
from PySide6.QtWidgets import QDialog,QVBoxLayout,QHBoxLayout,QLineEdit,QComboBox,QCheckBox,QListWidget,QListWidgetItem,QTextBrowser,QSplitter,QDialogButtonBox
from .model import CATALOG
from . import engine
from .widgets import label
from .details import Details

LABELS={'requirements':'Требования','benefit':'Эффект','notes':'Особое','description':'Описание','school':'Школа','manaText':'Мана','time':'Время сотворения','range':'Дальность','area':'Область','action':'Проверка','duration':'Длительность','enhancement':'Усиление','effect':'Эффект','slot':'Размещение','limit':'Нагрузка импланта','costText':'Цена','quality':'Качество'}
def description(e,s=None):
    lines=[e.get('name',''),e.get('category','')]
    if 'ranks' in e:lines.append(f"Цена: {e.get('cost') if e.get('costType')!='ability' else 'по источнику'} {'очков способностей' if e.get('costType')=='ability' else 'опыта / ранг'} • Рангов: {e.get('ranks') or 'не указано'}")
    for option in e.get('abilityOptions',[]):lines.append(f"{option['source']}: {option['value']} очк.")
    order=['school','manaText','time','range','area','action','duration','requirements','benefit','description','enhancement','notes','slot','limit','costText','quality','effect']
    for k in order:
        title=LABELS[k]
        if e.get(k) is not None and e.get(k)!='':lines.extend(['',title,str(e[k])])
    if s is not None and e.get('id','').startswith(('feat_','access_')):
        lines.extend(['','Проверка требований'])
        lines.extend({'ok':'✓ ','fail':'✕ ','manual':'? '}[c.state]+c.text for c in engine.requirements(s,e))
    if e.get('conflictNote'):lines.extend(['','Расхождения книги',e['conflictNote']])
    if e.get('incomplete'):lines.extend(['','Незавершённая запись книги. Добавление возможно только как своя запись после уточнения правил.'])
    return '\n'.join(lines)

class CatalogDialog(QDialog):
    def __init__(self,parent,state,kind='entries',read_only=False):
        super().__init__(parent);self.state=state;self.kind=kind;self.selected=None;self.read_only=read_only
        self.setWindowTitle('Справочник' if read_only else 'Добавить из книги');self.resize(1000,720)
        lay=QVBoxLayout(self);self.search=QLineEdit();self.search.setPlaceholderText('Поиск по названию, требованиям и описанию…');lay.addWidget(self.search)
        filters=QHBoxLayout();self.section=QComboBox();self.category=QComboBox();self.drafts=QCheckBox('Незавершённые');self.available=QCheckBox('Требования выполнены')
        self.entries=list(CATALOG[kind]);self.section.addItems(['Все разделы']+sorted({e.get('section',kind) for e in self.entries}));self.category.addItems(['Все категории']+sorted({e.get('category','Общие') for e in self.entries}));filters.addWidget(self.section);filters.addWidget(self.category,1);filters.addWidget(self.drafts)
        if kind=='entries':filters.addWidget(self.available)
        lay.addLayout(filters);split=QSplitter();self.list=QListWidget();self.details=Details();self.details.setMinimumHeight(0);self.details.setMaximumHeight(16777215);self.details.setOpenExternalLinks(False);split.addWidget(self.list);split.addWidget(self.details);split.setSizes([360,640]);lay.addWidget(split,1)
        self.count=label('');lay.addWidget(self.count)
        buttons=QDialogButtonBox(QDialogButtonBox.Close if read_only else QDialogButtonBox.Ok|QDialogButtonBox.Cancel);self.add=buttons.button(QDialogButtonBox.Ok)
        if self.add:self.add.setText('Выбрать');self.add.setEnabled(False)
        cancel=buttons.button(QDialogButtonBox.Close if read_only else QDialogButtonBox.Cancel);cancel.setText('Закрыть' if read_only else 'Отмена')
        buttons.accepted.connect(self.accept);buttons.rejected.connect(self.reject);lay.addWidget(buttons)
        self.search.textChanged.connect(self.filter);self.section.currentTextChanged.connect(self.filter);self.category.currentTextChanged.connect(self.filter);self.drafts.toggled.connect(self.filter);self.available.toggled.connect(self.filter);self.list.currentItemChanged.connect(self.choose)
        self.filter()
    def filter(self,*_):
        needle=engine.norm(self.search.text());self.list.clear()
        for e in sorted(self.entries,key=lambda x:engine.norm(x['name'])):
            if e.get('incomplete') and not self.drafts.isChecked():continue
            if self.section.currentIndex() and e.get('section',self.kind)!=self.section.currentText():continue
            if self.category.currentIndex() and e.get('category','Общие')!=self.category.currentText():continue
            if needle and needle not in engine.norm(' '.join(str(e.get(k,'')) for k in ['name','benefit','description','requirements','effect','category','school'])):continue
            if self.available.isChecked() and any(c.state!='ok' for c in engine.requirements(self.state,e)):continue
            item=QListWidgetItem(e['name']+'  ·  '+e.get('category',''));item.setData(Qt.UserRole,e);item.setToolTip(e['name']);self.list.addItem(item)
        self.count.setText(f'Записей: {self.list.count()}');self.list.setCurrentRow(0)
    def choose(self,item,*_):
        self.selected=item.data(Qt.UserRole) if item else None;self.details.display(self.selected,self.state,{'entries':'feat','spells':'spell','gear':'gear'}.get(self.kind,'')) if self.selected else self.details.setPlainText('Нет подходящих записей.')
        if self.add:self.add.setEnabled(bool(self.selected) and not self.selected.get('incomplete'))
