package com.dubl.character.android.model

/** Pure DUBL 3.69 rules used by skill-check workflows on every platform. */
object SkillCheckRules {
    /**
     * For a two-skill synergy, use the higher rank plus half of the lower rank.
     * DUBL's general division rule rounds fractions upward.
     */
    fun synergyCombinedRank(firstRank: Int, secondRank: Int): Int {
        val higherRank = maxOf(firstRank, secondRank).coerceAtLeast(0)
        val lowerRank = minOf(firstRank, secondRank).coerceAtLeast(0)
        return higherRank + (lowerRank + 1) / 2
    }

    /**
     * A helper needs at least one rank in the same skill. A total of 10 grants
     * +1, with another +1 for every four points above that threshold.
     * Null means the character is not eligible to assist with this skill.
     */
    fun assistanceBonus(helperSkillRank: Int, helperRollTotal: Int): Int? {
        if (helperSkillRank < 1) return null
        if (helperRollTotal < 10) return 0
        return 1 + (helperRollTotal - 10) / 4
    }
}
