from tools.rulebook.render_skill_catalog_kotlin import render_skill_catalog_kotlin


def test_renderer_emits_deterministic_shared_skill_catalog():
    payload = {
        "schemaVersion": 1,
        "source": "core",
        "rankCosts": [0, 10, 30],
        "rankCostSourceRefs": ["core:t-000004"],
        "skills": [
            {
                "id": "athletics",
                "name": "Атлетика",
                "description": "Прыжки, лазание, плавание",
                "auto6": "Да",
                "auto12": "Нет",
                "untrained": "YES",
                "category": "PHYSICAL",
                "defaultAttributeHint": "STRENGTH",
                "template": False,
                "sourceRefs": ["core:t-000002"],
            },
            {
                "id": "computers",
                "name": "Компьютеры",
                "description": "",
                "auto6": "Не указано в базовой таблице",
                "auto12": "Не указано в базовой таблице",
                "untrained": "UNSPECIFIED",
                "category": "TECHNICAL",
                "defaultAttributeHint": "INTELLIGENCE",
                "template": False,
                "sourceRefs": ["core:p-000005"],
            },
        ],
    }

    rendered = render_skill_catalog_kotlin(payload)

    assert rendered.startswith("package com.dubl.character.android.model\n")
    assert "internal object GeneratedSkillCatalog" in rendered
    assert "val rankCosts: List<Int> = listOf(0, 10, 30)" in rendered
    assert 'id = "athletics"' in rendered
    assert "category = SkillCategory.PHYSICAL" in rendered
    assert "defaultAttribute = AttributeId.STRENGTH" in rendered
    assert "untrained = UntrainedRule.YES" in rendered
    assert 'id = "computers"' in rendered
    assert "untrained = UntrainedRule.UNSPECIFIED" in rendered
    assert rendered.endswith("\n")
