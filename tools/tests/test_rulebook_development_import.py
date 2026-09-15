import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools.rulebook.import_development import import_regular_development


def test_regular_development_import_reads_mechanics_from_source_not_bindings():
    raw = {
        "blocks": [
            {"id": "p-1", "kind": "paragraph", "headingPath": ["Навыки", "Атлетика"], "text": "Плавание"},
            {"id": "p-2", "kind": "paragraph", "headingPath": ["Навыки", "Атлетика"], "text": "Стоимость: 10"},
            {"id": "p-3", "kind": "paragraph", "headingPath": ["Навыки", "Атлетика"], "text": "Ранги: 2"},
            {"id": "p-4", "kind": "paragraph", "headingPath": ["Навыки", "Атлетика"], "text": "Требование: Ловкость 2"},
            {"id": "p-5", "kind": "paragraph", "headingPath": ["Навыки", "Атлетика"], "text": "Выгода: Каноническая выгода"},
        ]
    }
    bindings = {
        "bindings": [
            {
                "id": "feat-swim",
                "name": "Плавание",
                "category": "Атлетика",
                "runtimeCost": 999,
                "runtimeRanks": 9,
            }
        ]
    }

    payload, diagnostics = import_regular_development(raw, bindings, "core")

    assert diagnostics == []
    assert payload["entries"] == [
        {
            "id": "feat-swim",
            "name": "Плавание",
            "section": "Навыки",
            "category": "Атлетика",
            "cost": 10,
            "ranks": 2,
            "requirements": "Ловкость 2",
            "benefit": "Каноническая выгода",
            "sourceRefs": ["core:p-1", "core:p-2", "core:p-3", "core:p-4", "core:p-5"],
        }
    ]


def test_regular_development_import_marks_incomplete_source_instead_of_guessing():
    raw = {
        "blocks": [
            {"id": "p-1", "kind": "paragraph", "headingPath": ["Навыки", "Бой"], "text": "Сломанная запись"},
            {"id": "p-2", "kind": "paragraph", "headingPath": ["Навыки", "Бой"], "text": "Стоимость: 20"},
        ]
    }
    bindings = {"bindings": [{"id": "broken", "name": "Сломанная запись", "category": "Бой"}]}

    payload, diagnostics = import_regular_development(raw, bindings, "core")

    assert payload["entries"][0]["cost"] == 20
    assert payload["entries"][0]["ranks"] is None
    assert payload["entries"][0]["benefit"] is None
    assert any(d["kind"] == "development-fields-missing" and d["severity"] == "warning" for d in diagnostics)


def test_regular_development_runtime_diff_reports_mechanical_drift():
    from tools.rulebook.import_development import compare_regular_development_runtime

    imported = {
        "entries": [
            {
                "id": "feat-swim",
                "name": "Плавание",
                "section": "Навыки",
                "category": "Атлетика",
                "cost": 10,
                "ranks": 2,
                "requirements": "Ловкость 2",
                "benefit": "Канон",
                "sourceRefs": ["core:p-1"],
            }
        ]
    }
    runtime = {
        "entries": [
            {
                "id": "feat-swim",
                "name": "Плавание",
                "section": "Навыки",
                "category": "Атлетика",
                "cost": 30,
                "ranks": 2,
                "requirements": "Ловкость 2",
                "benefit": "Канон",
            }
        ]
    }

    diagnostics = compare_regular_development_runtime(imported, runtime)

    assert len(diagnostics) == 1
    diagnostic = diagnostics[0]
    assert diagnostic["kind"] == "development-runtime-drift"
    assert diagnostic["subject"] == "development:feat-swim"
    assert diagnostic["severity"] == "warning"
    assert diagnostic["differences"] == {"cost": {"rulebook": 10, "runtime": 30}}


def test_regular_development_import_collects_benefit_continuations_and_tables_until_entry_boundary():
    raw = {
        "blocks": [
            {"id": "p-1", "kind": "paragraph", "headingPath": ["Навыки", "Общие"], "text": "Красота"},
            {"id": "p-2", "kind": "paragraph", "headingPath": ["Навыки", "Общие"], "text": "Стоимость: 30"},
            {"id": "p-3", "kind": "paragraph", "headingPath": ["Навыки", "Общие"], "text": "Ранг: 2"},
            {"id": "p-4", "kind": "paragraph", "headingPath": ["Навыки", "Общие"], "text": "Выгода: Первая строка."},
            {"id": "p-5", "kind": "paragraph", "headingPath": ["Навыки", "Общие"], "text": "Вторая строка."},
            {"id": "t-6", "kind": "table", "headingPath": ["Навыки", "Общие"], "rows": [["A", "B"], ["1", "2"]]},
            {"id": "p-7", "kind": "paragraph", "headingPath": ["Навыки", "Общие"], "text": ""},
            {"id": "p-8", "kind": "paragraph", "headingPath": ["Навыки", "Общие"], "text": "Следующая запись"},
            {"id": "p-9", "kind": "paragraph", "headingPath": ["Навыки", "Общие"], "text": "Стоимость: 99"},
        ]
    }
    bindings = {"bindings": [{"id": "beauty", "name": "Красота", "category": "Общие"}]}

    payload, diagnostics = import_regular_development(raw, bindings, "core")

    assert diagnostics == []
    assert payload["entries"][0]["benefit"] == "Первая строка.\nВторая строка.\nA\nB\n1\n2"
    assert payload["entries"][0]["sourceRefs"] == ["core:p-1", "core:p-2", "core:p-3", "core:p-4", "core:p-5", "core:t-6"]


def test_regular_development_import_disambiguates_by_skill_category_heading_path():
    raw = {
        "blocks": [
            {"id": "p-1", "kind": "paragraph", "headingPath": ["Навыки", "Бартер"], "text": "Посредник"},
            {"id": "p-2", "kind": "paragraph", "headingPath": ["Навыки", "Бартер"], "text": "Стоимость: 40"},
            {"id": "p-3", "kind": "paragraph", "headingPath": ["Навыки", "Бартер"], "text": "Ранги: 1"},
            {"id": "p-4", "kind": "paragraph", "headingPath": ["Навыки", "Бартер"], "text": "Выгода: Бартерная версия"},
            {"id": "p-5", "kind": "paragraph", "headingPath": ["Навыки", "Красноречие"], "text": "Посредник"},
            {"id": "p-6", "kind": "paragraph", "headingPath": ["Навыки", "Красноречие"], "text": "Стоимость: 40"},
            {"id": "p-7", "kind": "paragraph", "headingPath": ["Навыки", "Красноречие"], "text": "Ранги: 1"},
            {"id": "p-8", "kind": "paragraph", "headingPath": ["Навыки", "Красноречие"], "text": "Выгода: Речевая версия"},
        ]
    }
    bindings = {"bindings": [{"id": "middleman", "name": "Посредник", "category": "Бартер"}]}

    payload, diagnostics = import_regular_development(raw, bindings, "core")

    assert diagnostics == []
    assert payload["entries"][0]["benefit"] == "Бартерная версия"
    assert payload["entries"][0]["sourceRefs"][0] == "core:p-1"


def test_regular_development_import_uses_explicit_source_context_for_later_duplicate_variant():
    raw = {
        "blocks": [
            {"id": "p-1", "kind": "paragraph", "headingPath": ["Навыки", "Компьютеры"], "text": "Технарь"},
            {"id": "p-2", "kind": "paragraph", "headingPath": ["Навыки", "Компьютеры"], "text": "Стоимость: 40"},
            {"id": "p-3", "kind": "paragraph", "headingPath": ["Навыки", "Компьютеры"], "text": "Ранги: 2"},
            {"id": "p-4", "kind": "paragraph", "headingPath": ["Навыки", "Компьютеры"], "text": "Выгода: Базовая версия"},
            {"id": "p-5", "kind": "paragraph", "headingPath": ["Хакерские штучки", "Факторы"], "text": "Технарь"},
            {"id": "p-6", "kind": "paragraph", "headingPath": ["Хакерские штучки", "Факторы"], "text": "Стоимость: 20"},
            {"id": "p-7", "kind": "paragraph", "headingPath": ["Хакерские штучки", "Факторы"], "text": "Ранги: 2"},
            {"id": "p-8", "kind": "paragraph", "headingPath": ["Хакерские штучки", "Факторы"], "text": "Выгода: Поздняя версия"},
        ]
    }
    bindings = {"bindings": [{"id": "tech", "name": "Технарь", "category": "Киберпанк", "sourceContext": ["Хакерские штучки"]}]}

    payload, diagnostics = import_regular_development(raw, bindings, "core")

    assert diagnostics == []
    assert payload["entries"][0]["cost"] == 20
    assert payload["entries"][0]["benefit"] == "Поздняя версия"
    assert payload["entries"][0]["sourceRefs"][0] == "core:p-5"


def test_regular_development_import_prefers_duplicate_title_directly_followed_by_mechanical_fields():
    raw = {
        "blocks": [
            {"id": "p-1", "kind": "paragraph", "headingPath": ["Хакерские штучки", "Хакер"], "text": "Хакер"},
            {"id": "p-2", "kind": "paragraph", "headingPath": ["Хакерские штучки", "Хакер"], "text": "Хакер"},
            {"id": "p-3", "kind": "paragraph", "headingPath": ["Хакерские штучки", "Хакер"], "text": "Стоимость: 100"},
            {"id": "p-4", "kind": "paragraph", "headingPath": ["Хакерские штучки", "Хакер"], "text": "Ранги: 1"},
            {"id": "p-5", "kind": "paragraph", "headingPath": ["Хакерские штучки", "Хакер"], "text": "Выгода: Поздняя версия"},
        ]
    }
    bindings = {"bindings": [{"id": "hacker", "name": "Хакер", "category": "Хакер", "sourceContext": ["Хакерские штучки", "Хакер"]}]}

    payload, diagnostics = import_regular_development(raw, bindings, "core")

    assert diagnostics == []
    assert payload["entries"][0]["sourceRefs"][0] == "core:p-2"
    assert payload["entries"][0]["cost"] == 100


def test_regular_development_runtime_promotion_uses_source_mechanics_and_only_structural_bindings():
    from tools.rulebook.import_development import promote_regular_development

    imported = {
        "schemaVersion": 1,
        "source": "core",
        "entries": [
            {
                "id": "feat-x",
                "name": "Приём",
                "section": "Навыки",
                "category": "Бой",
                "cost": 40,
                "ranks": 1,
                "requirements": "Ловкость 3",
                "benefit": "Можно брать несколько раз.",
                "notes": "Примечание из книги",
                "sourceRefs": ["core:p-1"],
            },
            {
                "id": "feat-unresolved",
                "name": "Недописанный",
                "section": "Навыки",
                "category": "Общие",
                "cost": 20,
                "ranks": None,
                "requirements": "Воля 4",
                "benefit": "Эффект",
                "sourceRefs": ["core:p-2"],
            },
        ],
    }
    bindings = {
        "bindings": [
            {"id": "feat-x", "name": "Приём", "category": "Бой", "tags": ["Прием"], "accessId": "access-x"},
            {
                "id": "feat-unresolved",
                "name": "Недописанный",
                "category": "Общие",
                "legacyFallback": {
                    "ranks": 1,
                    "mechanicsConflict": "В рулбуке не указан ранг",
                    "conflictNote": "1 ранг сохранён только для совместимости.",
                },
            },
        ]
    }

    runtime = promote_regular_development(imported, bindings, version="3.69")
    by_id = {entry["id"]: entry for entry in runtime["entries"]}

    normal = by_id["feat-x"]
    assert normal["cost"] == 40
    assert normal["ranks"] == 1
    assert normal["requirements"] == "Ловкость 3"
    assert normal["benefit"] == "Можно брать несколько раз."
    assert normal["notes"] == "Примечание из книги"
    assert normal["costType"] == "xp"
    assert normal["tags"] == ["Прием"]
    assert normal["accessId"] == "access-x"
    assert normal["repeatable"] is True
    assert normal["sourceRefs"] == ["core:p-1"]

    unresolved = by_id["feat-unresolved"]
    assert unresolved["ranks"] == 1
    assert unresolved["incomplete"] is True
    assert unresolved["mechanicsConflict"] == "В рулбуке не указан ранг"
    assert unresolved["conflictNote"] == "1 ранг сохранён только для совместимости."


def test_special_development_import_reads_branch_child_mechanics_from_source():
    from tools.rulebook.import_development import import_special_development

    raw = {
        "blocks": [
            {"id": "p-1", "kind": "paragraph", "headingPath": ["Особые навыки", "Дуэлянт"], "text": "Ответный выпад"},
            {"id": "p-2", "kind": "paragraph", "headingPath": ["Особые навыки", "Дуэлянт"], "text": "Стоимость: 40"},
            {"id": "p-3", "kind": "paragraph", "headingPath": ["Особые навыки", "Дуэлянт"], "text": "Ранги: 2"},
            {"id": "p-4", "kind": "paragraph", "headingPath": ["Особые навыки", "Дуэлянт"], "text": "Требование: Дуэлянт, Парирование"},
            {"id": "p-5", "kind": "paragraph", "headingPath": ["Особые навыки", "Дуэлянт"], "text": "Выгода: Канонический эффект"},
        ]
    }
    bindings = {"bindings": [{"id": "feat-riposte", "name": "Ответный выпад", "category": "Дуэлянт"}]}

    payload, diagnostics = import_special_development(raw, bindings, "core")

    assert diagnostics == []
    assert payload["entries"] == [{
        "id": "feat-riposte",
        "name": "Ответный выпад",
        "section": "Ветки способностей",
        "category": "Дуэлянт",
        "cost": 40,
        "ranks": 2,
        "requirements": "Дуэлянт, Парирование",
        "benefit": "Канонический эффект",
        "sourceRefs": ["core:p-1", "core:p-2", "core:p-3", "core:p-4", "core:p-5"],
    }]


def test_special_development_prefers_repeated_card_title_with_tag_line_before_mechanics():
    from tools.rulebook.import_development import import_special_development

    raw = {"blocks": [
        {"id":"p-1","kind":"paragraph","headingPath":["Особые навыки","Авангард"],"text":"Авангард"},
        {"id":"p-2","kind":"paragraph","headingPath":["Особые навыки","Авангард"],"text":"описание ветки"},
        {"id":"p-3","kind":"paragraph","headingPath":["Особые навыки","Авангард"],"text":"Авангард"},
        {"id":"p-4","kind":"paragraph","headingPath":["Особые навыки","Авангард"],"text":"[Авангард]"},
        {"id":"p-5","kind":"paragraph","headingPath":["Особые навыки","Авангард"],"text":"Стоимость: 50"},
        {"id":"p-6","kind":"paragraph","headingPath":["Особые навыки","Авангард"],"text":"Ранги: 1"},
        {"id":"p-7","kind":"paragraph","headingPath":["Особые навыки","Авангард"],"text":"Выгода: Эффект"},
    ]}
    bindings={"bindings":[{"id":"vanguard","name":"Авангард","category":"Авангард"}]}
    payload, diagnostics = import_special_development(raw, bindings, "core")
    assert diagnostics == []
    assert payload["entries"][0]["sourceRefs"][0] == "core:p-3"
    assert payload["entries"][0]["cost"] == 50


def test_special_development_ignores_same_name_outside_special_skills_domain():
    from tools.rulebook.import_development import import_special_development

    raw = {"blocks": [
        {"id":"p-special","kind":"paragraph","headingPath":["Особые навыки","Удача"],"text":"Удача"},
        {"id":"p-cost","kind":"paragraph","headingPath":["Особые навыки","Удача"],"text":"Стоимость: 50"},
        {"id":"p-rank","kind":"paragraph","headingPath":["Особые навыки","Удача"],"text":"Ранги: 5"},
        {"id":"p-benefit","kind":"paragraph","headingPath":["Особые навыки","Удача"],"text":"Выгода: Пункты удачи"},
        {"id":"p-spell","kind":"paragraph","headingPath":["Магия","Описание заклинаний","Удача"],"text":"Удача"},
        {"id":"p-spell-cost","kind":"paragraph","headingPath":["Магия","Описание заклинаний","Удача"],"text":"Стоимость: 6"},
    ]}
    bindings={"bindings":[{"id":"luck","name":"Удача","category":"Удача"}]}
    payload, diagnostics = import_special_development(raw, bindings, "core")
    assert diagnostics == []
    assert payload["entries"][0]["sourceRefs"][0] == "core:p-special"
    assert payload["entries"][0]["cost"] == 50


def test_special_development_can_bind_placeholder_duplicates_by_source_occurrence():
    from tools.rulebook.import_development import import_special_development

    raw = {"blocks": []}
    for n, cost in enumerate((30, 35, 40)):
        raw["blocks"].extend([
            {"id":f"p-{n}-title","kind":"paragraph","headingPath":["Особые навыки","Авангард"],"text":"Название"},
            {"id":f"p-{n}-tag","kind":"paragraph","headingPath":["Особые навыки","Авангард"],"text":"[Авангард]"},
            {"id":f"p-{n}-cost","kind":"paragraph","headingPath":["Особые навыки","Авангард"],"text":f"Стоимость: {cost}"},
            {"id":f"p-{n}-rank","kind":"paragraph","headingPath":["Особые навыки","Авангард"],"text":"Ранги: 1"},
            {"id":f"p-{n}-benefit","kind":"paragraph","headingPath":["Особые навыки","Авангард"],"text":f"Выгода: Эффект {n}"},
            {"id":f"p-{n}-blank","kind":"paragraph","headingPath":["Особые навыки","Авангард"],"text":""},
        ])
    bindings={"bindings":[{"id":"middle","name":"Название","category":"Авангард","sourceOccurrence":1}]}
    payload, diagnostics = import_special_development(raw, bindings, "core")
    assert diagnostics == []
    assert payload["entries"][0]["sourceRefs"][0] == "core:p-1-title"
    assert payload["entries"][0]["cost"] == 35


def test_development_parser_keeps_continuation_before_special_field():
    from tools.rulebook.import_development import import_special_development
    raw={"blocks":[
        {"id":"t","kind":"paragraph","headingPath":["Особые навыки","Ветка"],"text":"Карточка"},
        {"id":"c","kind":"paragraph","headingPath":["Особые навыки","Ветка"],"text":"Стоимость: 10"},
        {"id":"r","kind":"paragraph","headingPath":["Особые навыки","Ветка"],"text":"Ранги: 1"},
        {"id":"b","kind":"paragraph","headingPath":["Особые навыки","Ветка"],"text":"Выгода: Первая строка"},
        {"id":"x","kind":"paragraph","headingPath":["Особые навыки","Ветка"],"text":"Вторая строка эффекта"},
        {"id":"n","kind":"paragraph","headingPath":["Особые навыки","Ветка"],"text":"Особое: Ограничение"},
    ]}
    payload,diags=import_special_development(raw,{"bindings":[{"id":"x","name":"Карточка","category":"Ветка"}]},"core")
    assert not diags
    assert payload["entries"][0]["benefit"] == "Первая строка\nВторая строка эффекта"
    assert payload["entries"][0]["notes"] == "Ограничение"


def test_development_parser_keeps_table_after_blank_as_benefit():
    from tools.rulebook.import_development import import_special_development
    raw={"blocks":[
        {"id":"t","kind":"paragraph","headingPath":["Особые навыки","Связи"],"text":"Связи"},
        {"id":"c","kind":"paragraph","headingPath":["Особые навыки","Связи"],"text":"Стоимость: 50"},
        {"id":"r","kind":"paragraph","headingPath":["Особые навыки","Связи"],"text":"Ранги: 5"},
        {"id":"b","kind":"paragraph","headingPath":["Особые навыки","Связи"],"text":"Выгода: Основной эффект"},
        {"id":"blank","kind":"paragraph","headingPath":["Особые навыки","Связи"],"text":""},
        {"id":"table","kind":"table","headingPath":["Особые навыки","Связи"],"rows":[["Ранг","Эффект"],["1","Помощь"]]},
    ]}
    payload,diags=import_special_development(raw,{"bindings":[{"id":"links","name":"Связи","category":"Связи"}]},"core")
    assert not diags
    assert payload["entries"][0]["benefit"] == "Основной эффект\nРанг\nЭффект\n1\nПомощь"


def test_development_parser_stops_at_title_style_boundary():
    from tools.rulebook.import_development import import_special_development
    raw={"blocks":[
        {"id":"t","kind":"paragraph","headingPath":["Способности","Киберпанк"],"text":"Жертва"},
        {"id":"c","kind":"paragraph","headingPath":["Способности","Киберпанк"],"text":"Стоимость: 45"},
        {"id":"r","kind":"paragraph","headingPath":["Способности","Киберпанк"],"text":"Ранги: 1"},
        {"id":"b","kind":"paragraph","headingPath":["Способности","Киберпанк"],"text":"Выгода: Строка"},
        {"id":"cont","kind":"paragraph","headingPath":["Способности","Киберпанк"],"text":"4. Воля","style":"normal"},
        {"id":"next","kind":"paragraph","headingPath":["Способности","Киберпанк"],"text":"Импланты","style":"Title"},
        {"id":"junk","kind":"paragraph","headingPath":["Способности","Киберпанк"],"text":"Следующий раздел"},
    ]}
    payload,diags=import_special_development(raw,{"bindings":[{"id":"victim","name":"Жертва","category":"Киберпанк"}]},"core")
    assert not diags
    assert payload["entries"][0]["benefit"] == "Строка\n4. Воля"


def test_special_development_prefers_mechanical_card_even_if_heading_path_context_is_broken():
    from tools.rulebook.import_development import import_special_development
    raw={"blocks":[
        {"id":"heading","kind":"paragraph","headingPath":["Особые навыки","Мастер оружия"],"text":"Мастер оружия"},
        {"id":"blank","kind":"paragraph","headingPath":["Особые навыки","Мастер оружия"],"text":""},
        {"id":"card","kind":"paragraph","headingPath":["Особые навыки"],"text":"Мастер оружия"},
        {"id":"tag","kind":"paragraph","headingPath":["Особые навыки"],"text":"[Мастер оружия]"},
        {"id":"cost","kind":"paragraph","headingPath":["Особые навыки"],"text":"Стоимость: 100"},
        {"id":"rank","kind":"paragraph","headingPath":["Особые навыки"],"text":"Ранги: 1"},
        {"id":"benefit","kind":"paragraph","headingPath":["Особые навыки"],"text":"Выгода: Максимальный урон"},
    ]}
    bindings={"bindings":[{"id":"master","name":"Мастер оружия","category":"Мастер оружия"}]}
    payload,diags=import_special_development(raw,bindings,"core")
    assert diags == []
    assert payload["entries"][0]["sourceRefs"][0] == "core:card"
    assert payload["entries"][0]["cost"] == 100


def test_promote_special_development_uses_source_mechanics_and_structural_bindings():
    from tools.rulebook.import_development import promote_special_development
    imported={"entries":[{"id":"child","name":"Приём","category":"Ветка","cost":40,"ranks":2,"requirements":"Ветка","benefit":"Книжный эффект","sourceRefs":["core:p1"]}]}
    bindings={"bindings":[{"id":"child","name":"Приём","category":"Ветка","tags":["Ветка"],"accessId":"access-root","perfectRoot":True}]}
    result=promote_special_development(imported,bindings)
    entry=result["entries"][0]
    assert entry["section"] == "Ветки способностей"
    assert entry["cost"] == 40 and entry["ranks"] == 2
    assert entry["requirements"] == "Ветка"
    assert entry["benefit"] == "Книжный эффект"
    assert entry["tags"] == ["Ветка"]
    assert entry["accessId"] == "access-root"
    assert entry["perfectRoot"] is True


def test_promote_special_development_requires_explicit_fallback_for_missing_cost_or_rank():
    from tools.rulebook.import_development import promote_special_development
    imported={"entries":[{"id":"broken","name":"Недописано","category":"Ветка","cost":None,"ranks":None,"requirements":"-","benefit":"Эффект","sourceRefs":["core:p1"]}]}
    bindings={"bindings":[{"id":"broken","name":"Недописано","category":"Ветка","legacyFallback":{"cost":0,"ranks":1,"mechanicsConflict":"В книге нет стоимости и ранга","conflictNote":"Локальная правка разрешена"}}]}
    entry=promote_special_development(imported,bindings)["entries"][0]
    assert entry["cost"] == 0 and entry["ranks"] == 1
    assert entry["incomplete"] is True
    assert "нет стоимости" in entry["mechanicsConflict"]
    assert "Локальная" in entry["conflictNote"]


def test_promote_special_development_marks_placeholders_and_explicit_conflicts_incomplete():
    from tools.rulebook.import_development import promote_special_development
    imported={"entries":[
        {"id":"placeholder","name":"???","category":"Ветка","cost":40,"ranks":1,"requirements":"-","benefit":"Эффект","sourceRefs":["core:p1"]},
        {"id":"conflict","name":"Убийство","category":"Ассассин","cost":75,"ranks":1,"requirements":"Ассассин","benefit":"критическая (не крит)","sourceRefs":["core:p2"]},
    ]}
    bindings={"bindings":[
        {"id":"placeholder","name":"???","category":"Ветка"},
        {"id":"conflict","name":"Убийство","category":"Ассассин","unresolved":{"mechanicsConflict":"Текст противоречив","conflictNote":"Выберите трактовку локально"}},
    ]}
    by_id={e["id"]:e for e in promote_special_development(imported,bindings)["entries"]}
    assert by_id["placeholder"]["incomplete"] is True
    assert "чернов" in by_id["placeholder"]["mechanicsConflict"].casefold()
    assert by_id["conflict"]["incomplete"] is True
    assert by_id["conflict"]["mechanicsConflict"] == "Текст противоречив"
