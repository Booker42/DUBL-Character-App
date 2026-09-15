import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools.rulebook.import_conditions import import_conditions


def p(i, text, level=None, path=None):
    return {"id":f"p-{i:06d}","kind":"paragraph","order":i,"text":text,"normalizedText":text.lower(),"headingLevel":level,"headingPath":path or []}


def test_import_conditions_keeps_headed_and_inline_conditions_with_provenance():
    raw={"blocks":[
        p(1,"Состояния и эффекты",1,["Состояния и эффекты"]),
        p(2,"Повреждение характеристик",4,["Состояния и эффекты","Повреждение характеристик"]),
        p(3,"Персонаж с -6 силой падает и считается беспомощным.",None,["Состояния и эффекты","Повреждение характеристик"]),
        p(4,"Персонаж с -6 ловкостью становится парализованным.",None,["Состояния и эффекты","Повреждение характеристик"]),
        p(5,"Персонаж с -6 волей теряет сознание.",None,["Состояния и эффекты","Повреждение характеристик"]),
        p(6,"Слепота",4,["Состояния и эффекты","Слепота"]),
        p(7,"Персонаж не может видеть.",None,["Состояния и эффекты","Слепота"]),
        p(8,"Истощение",4,["Состояния и эффекты","Истощение"]),
        p(9,"После отдыха персонаж становится уставшим.",None,["Состояния и эффекты","Истощение"]),
        p(10,"Типы урона",2,["Состояния и эффекты","Типы урона"]),
    ]}
    out=import_conditions(raw,"core")
    titles={x["name"] for x in out["conditions"]}
    assert titles == {"Беспомощный","Парализованный","Без сознания","Слепота","Истощение","Уставший"}
    blind=next(x for x in out["conditions"] if x["name"]=="Слепота")
    assert blind["description"] == "Персонаж не может видеть."
    assert blind["sourceRefs"] == ["core:p-000006","core:p-000007"]
    helpless=next(x for x in out["conditions"] if x["name"]=="Беспомощный")
    assert helpless["sourceKind"] == "inline-derived"
    assert helpless["sourceRefs"] == ["core:p-000003"]
    assert [x["name"] for x in out["mechanics"]] == ["Повреждение характеристик"]
