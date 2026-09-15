from tools.rulebook.import_skill_effects import import_skill_effects


def test_skill_effects_take_rule_text_from_generated_development():
    development = {
        "entries": [
            {
                "id": "feat_swimmer",
                "name": "Пловец",
                "benefit": "Вы получаете +1 к Атлетике за каждый ранг.",
                "notes": "Работает только при плавании.",
                "sourceRefs": ["core:p-100"],
            }
        ]
    }
    bindings = {
        "bindings": [
            {
                "id": "skill_effect_001",
                "reviewIndex": 1,
                "developmentId": "feat_swimmer",
                "targetText": "SKILL · Атлетика",
                "status": "0.5 · EXISTING",
                "plan": "UI annotation only",
                "mode": "toggle_bonus",
                "rollContext": "SKILL",
                "targetSkills": ["Атлетика"],
                "value": 1,
                "perRank": True,
                "toggleLabel": "Плавание",
            }
        ]
    }

    payload, diagnostics = import_skill_effects(development, bindings)

    assert diagnostics == []
    effect = payload["effects"][0]
    assert effect["sourceName"] == "Пловец"
    assert effect["effectText"] == "Вы получаете +1 к Атлетике за каждый ранг.\nРаботает только при плавании."
    assert effect["sourceRefs"] == ["core:p-100"]
    assert effect["developmentId"] == "feat_swimmer"
    assert effect["mode"] == "toggle_bonus"
    assert effect["targetSkills"] == ["Атлетика"]


def test_missing_development_binding_is_blocking_diagnostic():
    payload, diagnostics = import_skill_effects(
        {"entries": []},
        {"bindings": [{"id": "effect_bad", "reviewIndex": 1, "developmentId": "missing"}]},
    )

    assert payload["effects"] == []
    assert len(diagnostics) == 1
    assert diagnostics[0]["kind"] == "skill-effect-development-missing"
    assert diagnostics[0]["severity"] == "error"
