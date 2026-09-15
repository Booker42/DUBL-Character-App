import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools.rulebook.source_index import build_source_index, normalize_lookup


def test_source_index_keeps_duplicate_headings_and_normalizes_russian_text():
    raw = {
        "blocks": [
            {"id": "p-000001", "kind": "paragraph", "text": "Ёж — тест", "normalizedText": "еж - тест", "headingLevel": 1, "headingPath": ["Ёж — тест"]},
            {"id": "p-000002", "kind": "paragraph", "text": "Текст   правила", "normalizedText": "текст правила", "headingLevel": None, "headingPath": ["Ёж — тест"]},
            {"id": "p-000003", "kind": "paragraph", "text": "Ёж – тест", "normalizedText": "еж - тест", "headingLevel": 1, "headingPath": ["Ёж – тест"]},
        ]
    }
    index = build_source_index(raw)
    assert normalize_lookup("  ЁЖ –   тест ") == "еж - тест"
    assert index["headings"]["еж - тест"] == ["p-000001", "p-000003"]
    assert index["text"]["текст правила"] == ["p-000002"]


def test_source_index_keeps_search_text_for_inline_provenance():
    raw = {"blocks": [
        {"id":"p-000001","kind":"paragraph","text":"Удар тигра — стоимость 40","normalizedText":"удар тигра - стоимость 40","headingLevel":None,"headingPath":["Приёмы"],"order":1},
        {"id":"t-000002","kind":"table","rows":[["Разряд 2", "Эффект"]],"normalizedRows":[["разряд 2","эффект"]],"headingPath":["Ци"],"order":2},
    ]}
    index = build_source_index(raw, source_key="melee")
    assert index["blocks"]["melee:p-000001"]["normalizedSearchText"] == "удар тигра - стоимость 40"
    assert index["blocks"]["melee:t-000002"]["normalizedSearchText"] == "разряд 2 | эффект"
