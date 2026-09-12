"""Escaped rich text for native Qt content panels, with structured metadata."""
from html import escape
from PySide6.QtWidgets import QTextBrowser,QSizePolicy
from PySide6.QtCore import QSize,Qt,QTimer
from . import engine

class Details(QTextBrowser):
    def __init__(self,height=170):
        super().__init__();self.setObjectName('structuredDetails');self.base_height=height;self._content_height=height;self._last_fit_width=0;self.setMinimumHeight(height);self.setOpenExternalLinks(False);self.setOpenLinks(False);self.setVerticalScrollBarPolicy(Qt.ScrollBarAlwaysOff);self.setHorizontalScrollBarPolicy(Qt.ScrollBarAlwaysOff);self.setSizePolicy(QSizePolicy.Expanding,QSizePolicy.Fixed);self.entry=None;self._html='';self._identity=None;self.fit_cap=460;self.was_clipped=False
    def sizeHint(self):return QSize(300,max(self.base_height,self._content_height))
    def display(self,e,state=None,kind='',note=''):
        self.entry=e
        if not e:self.setHtml('');return
        light=False
        parent=self.window()
        if hasattr(parent,'s'):light=parent.s['ui'].get('theme')=='light'
        text='#e8e3dc' if not light else '#27282d';muted='#a0a3ad' if not light else '#62666f';accent='#d6b486' if not light else '#805624';line='#30343d' if not light else '#dedad3'
        esc=lambda s:escape(str(s if s is not None else '')).replace('\n','<br>')
        html=[f'<body style="color:{text};font-size:10pt;"><p style="color:{accent};font-size:8pt;margin:0 0 5px">{esc(e.get("category",{"skill":"Умение","spell":"Заклинание","feat":"Навык / способность","gear":"Предмет"}.get(kind,"Справочник"))).upper()}</p>',f'<h3 style="font-size:15pt;margin:0 0 10px">{esc(e.get("name",""))}</h3>']
        facts=[]
        if kind=='skill':facts=[('Характеристики',' + '.join(engine.skill_attrs(e))),('Ранг',e.get('rank',0))]
        elif kind=='spell':facts=[('Школа',e.get('school','—')),('Мана',e.get('manaText',e.get('cost','—'))),('Время',e.get('time','—')),('Дальность',e.get('range','—')),('Длительность',e.get('duration','—')),('Проверка',e.get('action','—'))]
        elif kind=='feat':facts=[('Цена',str(e.get('cost',0))+' опыта / ранг' if e.get('costType')!='ability' else 'Очки способностей'),('Рангов',e.get('ranks','—'))]
        elif kind=='gear':facts=list(e.get('fields',{}).items())
        facts += [(o['source'],str(o['value'])+' очк.') for o in e.get('abilityOptions',[])]
        if kind=='skill':
            html.append(f'<p style="color:{muted};font-size:9pt;margin:0 0 8px">'+ ' · '.join(esc(k)+': '+esc(v) for k,v in facts)+'</p>');facts=[]
            if state is not None:
                formula=engine.skill_formula_text(state,e)
                if formula:html.append(f'<p style="color:{muted};font-size:9pt;margin:4px 0 8px">{esc(formula)}</p>')
        if facts:
            html.append('<table width="100%" cellspacing="0" cellpadding="5">')
            for i in range(0,len(facts),2):
                html.append('<tr>')
                for k,val in facts[i:i+2]:html.append(f'<td width="50%" style="border-bottom:1px solid {line}"><span style="color:{muted};font-size:8pt">{esc(k)}</span><br>{esc(val)}</td>')
                html.append('</tr>')
            html.append('</table>')
        for key,title in [('requirements','Требования'),('benefit','Эффект'),('description','Описание'),('effect','Эффект'),('enhancement','Усиление'),('notes','Особое'),('conflictNote','Уточнение правил')]:
            if e.get(key) and kind=='skill' and key=='description':
                html.append(f'<p style="margin:4px 0">{esc(e[key])}</p>');continue
            if e.get(key):html.append(f'<p style="color:{muted};font-size:9pt;margin:12px 0 3px"><b>{title}</b></p><p style="margin:0;line-height:130%">{esc(e[key])}</p>')
        if state is not None and kind=='feat':
            html.append(f'<p style="color:{muted};margin:12px 0 4px"><b>Проверка требований</b></p>')
            for c in engine.requirements(state,e):html.append(f'<p style="margin:3px 0">{ {"ok":"✓","fail":"✕","manual":"?"}[c.state]} {esc(c.text)}</p>')
        if note:html.append(f'<p style="margin-top:10px">{esc(note)}</p>')
        html.append('</body>');rendered=''.join(html)
        size=state.get('ui',{}).get('fontSize',11) if state else 11
        for old,new in [(15,size+4),(10,size),(9,max(9,size-1)),(8,max(8,size-2))]:rendered=rendered.replace('font-size:'+str(old)+'pt','font-size:'+str(new)+'pxTEMP')
        rendered=rendered.replace('pxTEMP','pt');identity=e.get('id') or e.get('name')
        if rendered==self._html:
            QTimer.singleShot(0,self.fit_content);return
        self._html=rendered;self._identity=identity;self.setHtml(rendered);QTimer.singleShot(0,self.fit_content)
    def _notify_card(self):
        parent=self.parentWidget()
        while parent is not None:
            fn=getattr(parent,'request_content_fit',None)
            if callable(fn):fn();return
            parent=parent.parentWidget()
    def fit_content(self):
        if not self.isVisible():return
        width=max(180,self.viewport().width())
        self.document().setTextWidth(width)
        natural=max(self.base_height,int(self.document().size().height())+24)
        cap=max(self.base_height,int(getattr(self,'fit_cap',460) or 460));h=min(natural,cap);self.was_clipped=natural>cap
        self.setToolTip('Описание сокращено в карточке. Полный текст: «Ещё → Полное описание».' if self.was_clipped else '')
        changed=h!=self._content_height
        self._content_height=h;self._last_fit_width=width
        if self.height()!=h:self.setFixedHeight(h)
        self.updateGeometry()
        if changed:self._notify_card()
    def resizeEvent(self,event):
        super().resizeEvent(event)
        # A height adjustment is the result of fitting text, not a reason to
        # recalculate it again.  Only width changes affect wrapping.
        if event.oldSize().width()!=event.size().width():QTimer.singleShot(0,self.fit_content)
