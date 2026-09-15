import com.dubl.character.android.data.InMemoryCharacterStore
import com.dubl.character.android.model.AppSnapshot
import com.dubl.character.android.model.AttributeId
import com.dubl.character.android.model.AttributeValue
import com.dubl.character.android.model.DublCharacter
import com.dubl.character.android.state.CharacterSheetSession

private fun expect(value: Boolean, message: String) {
    if (!value) error(message)
}

fun main() {
    val initial = DublCharacter(
        id = "desktop-test",
        name = "Initial",
        experience = 5000,
        creationExperience = 5000,
        attributes = AttributeId.entries.associateWith { AttributeValue(base = 2) },
        hpCurrent = 1,
        enduranceCurrent = 3,
    )
    val session = CharacterSheetSession(
        InMemoryCharacterStore(AppSnapshot(listOf(initial), initial.id)),
    )

    session.editIdentity(
        name = "",
        concept = "Кузнец",
        experience = 7000,
        size = 6,
        legs = 2,
        manaEnabled = true,
    )
    expect(session.active.name == "Новый персонаж", "blank name must use Android fallback")
    expect(session.active.concept == "Кузнец", "concept must update")
    expect(session.active.experience == 7000, "experience must update")
    expect(session.active.creationExperience == 7000, "creation XP follows XP before creation completes")
    expect(session.active.size == 6, "size must update")
    expect(session.active.hpCurrent == session.active.healthMaximum, "identity edit refills HP during creation like Android")

    val hpBeforeAttribute = session.active.healthMaximum
    session.changeAttribute(AttributeId.CONSTITUTION, 50)
    expect(session.active.attributes.getValue(AttributeId.CONSTITUTION).base == 10, "attribute base must clamp to 10")
    expect(session.active.healthMaximum >= hpBeforeAttribute, "shared formula must recompute max HP")
    expect(session.active.hpCurrent == session.active.healthMaximum, "attribute edit refills HP during creation like Android")

    session.changeHp(-3)
    expect(session.active.hpCurrent == session.active.healthMaximum - 3, "HP delta must apply")
    val hpBeforeLethalDamage = session.active.hpCurrent
    session.changeHp(-10000)
    expect(session.active.hpCurrent == hpBeforeLethalDamage - 10000, "negative HP must remain representable for DUBL death/survival rules")

    session.changeEndurance(-100)
    expect(session.active.enduranceCurrent == 0, "endurance must normalize at zero")

    session.setExperience(-50)
    expect(session.active.experience == 0, "experience must clamp at zero")
    expect(session.active.creationExperience == 0, "creation XP must stay valid")

    println("CHARACTER_SHEET_SESSION_OK")
}
