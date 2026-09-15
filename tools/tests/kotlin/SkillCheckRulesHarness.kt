import com.dubl.character.android.model.SkillCheckRules

fun main() {
    check(SkillCheckRules.synergyCombinedRank(5, 3) == 7)
    check(SkillCheckRules.synergyCombinedRank(3, 5) == 7)
    check(SkillCheckRules.synergyCombinedRank(5, 0) == 5)
    check(SkillCheckRules.assistanceBonus(0, 30) == null)
    check(SkillCheckRules.assistanceBonus(1, 9) == 0)
    check(SkillCheckRules.assistanceBonus(1, 10) == 1)
    check(SkillCheckRules.assistanceBonus(1, 14) == 2)
    check(SkillCheckRules.assistanceBonus(1, 18) == 3)
    println("SKILL_CHECK_RULES_OK")
}
