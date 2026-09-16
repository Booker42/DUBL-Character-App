package com.dubl.character.tests

import com.dubl.character.android.application.CharacterTransferImportResult
import com.dubl.character.android.application.DublApplication
import com.dubl.character.android.data.CharacterExtrasStore
import com.dubl.character.android.data.CharacterTransferCodec
import com.dubl.character.android.data.CharacterTransferRejectReason
import com.dubl.character.android.data.InMemoryCharacterStore
import com.dubl.character.android.data.SnapshotCodec
import com.dubl.character.android.model.AppSnapshot
import com.dubl.character.android.model.AttributeId
import com.dubl.character.android.model.CharacterConditionId
import com.dubl.character.android.model.CharacterSheetExtras
import com.dubl.character.android.model.CharacterSheetResourceId
import com.dubl.character.android.model.DublCharacter
import com.dubl.character.android.model.RulesetRef
import com.dubl.character.android.model.SheetGroup

private class TransferExtrasStore : CharacterExtrasStore {
    private val values = mutableMapOf<String, CharacterSheetExtras>()

    override fun load(characterId: String): CharacterSheetExtras =
        values[characterId] ?: CharacterSheetExtras()

    override fun save(characterId: String, extras: CharacterSheetExtras) {
        values[characterId] = extras
    }

    override fun delete(characterId: String) {
        values.remove(characterId)
    }
}

fun main() {
    var nextId = 1
    val source = DublCharacter(id = "android-source", name = "Радана")
    val app = DublApplication(
        characterStore = InMemoryCharacterStore(AppSnapshot(listOf(source), source.id)),
        extrasStore = TransferExtrasStore(),
        idFactory = { "import-${nextId++}" },
        customConditionIdFactory = { "custom-condition" },
    )

    app.sheet.setPortrait("content://android/local-portrait")
    app.sheet.setNotes("portable note")
    app.sheet.setConditions(setOf(CharacterConditionId.TIRED))
    app.sheet.setResourceHidden(CharacterSheetResourceId.MANA, true)
    app.sheet.setPreferredSkillAttribute("smithing", AttributeId.INTELLIGENCE)
    app.sheet.setSkillGroups(listOf(SheetGroup("craft", "Ремесло", listOf("smithing"))))

    val raw = app.transfer.exportActive()
    check(raw.contains("\"format\":\"dubl.character\""))
    check(!raw.contains("content://android/local-portrait"))

    val first = app.transfer.importCharacter(raw)
    check(first is CharacterTransferImportResult.Imported)
    check(first.characterId == "import-1")
    check(app.active.name == "Радана")
    check(app.activeExtras.notes == "portable note")
    check(CharacterConditionId.TIRED in app.activeExtras.activeConditions)
    check(CharacterSheetResourceId.MANA in app.activeExtras.hiddenResourceIds)
    check(app.activeExtras.preferredSkillAttributes["smithing"] == AttributeId.INTELLIGENCE)
    check(app.activeExtras.skillGroups.single().id == "craft")
    check(app.activeExtras.portraitUri == null)

    val second = app.transfer.importCharacter(raw)
    check(second is CharacterTransferImportResult.Imported)
    check(second.characterId == "import-2")
    check(app.snapshot.characters.size == 3)

    val beforeRejected = app.snapshot
    val malformed = app.transfer.importCharacter("not-json")
    check(malformed is CharacterTransferImportResult.Rejected)
    check(malformed.reason == CharacterTransferRejectReason.INVALID_FILE)
    check(app.snapshot == beforeRejected)

    val foreignRaw = CharacterTransferCodec.encode(
        source.copy(ruleset = RulesetRef("lancer", "1")),
        CharacterSheetExtras(),
    )
    val foreign = app.transfer.importCharacter(foreignRaw)
    check(foreign is CharacterTransferImportResult.Rejected)
    check(foreign.reason == CharacterTransferRejectReason.UNSUPPORTED_RULESET)
    check(app.snapshot == beforeRejected)

    val futureSchemaRaw = raw.replace(
        "\"schema\":${SnapshotCodec.SCHEMA}",
        "\"schema\":${SnapshotCodec.SCHEMA + 1}",
    )
    val futureSchema = app.transfer.importCharacter(futureSchemaRaw)
    check(futureSchema is CharacterTransferImportResult.Rejected)
    check(futureSchema.reason == CharacterTransferRejectReason.UNSUPPORTED_FORMAT_VERSION)
    check(app.snapshot == beforeRejected)

    println("CHARACTER_TRANSFER_OK")
}
