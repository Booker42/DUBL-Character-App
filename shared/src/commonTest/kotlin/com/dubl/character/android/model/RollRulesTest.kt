package com.dubl.character.android.model

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.Test

class RollRulesTest {
    private fun dice(vararg values: Int): () -> Int {
        val iterator = values.iterator()
        return { iterator.nextInt() }
    }

    @Test
    fun normalDoubleGrantsAdvantageDieAndRecalculatesUsingTwoHighest() {
        val initial = rollCheck(
            mode = RollMode.NORMAL,
            effectCount = 0,
            checkBonus = 3,
            checkBonusLabel = "Бонус",
            situationalBonus = 0,
            rollDie = dice(4, 4),
        )
        assertEquals(RollFollowUp.ADVANTAGE_DIE, initial.followUp)

        val resolved = rollFollowUp(initial, dice(6))
        assertEquals(listOf(4, 4, 6), resolved.dice)
        assertEquals(setOf(0, 2), resolved.chosenIndices)
        assertEquals(13, resolved.total)
        assertNull(resolved.followUp)
    }

    @Test
    fun criticalFailureStaysFailedAndCanBeConfirmed() {
        val initial = rollCheck(
            mode = RollMode.NORMAL,
            effectCount = 0,
            checkBonus = 5,
            checkBonusLabel = "Бонус",
            situationalBonus = 0,
            rollDie = dice(1, 1),
        )
        assertEquals(RollSpecialResult.CRITICAL_FAILURE, initial.specialResult)
        assertEquals(RollFollowUp.CRITICAL_FAILURE_CONFIRMATION, initial.followUp)
        assertEquals(7, initial.total)

        val confirmed = rollFollowUp(initial, dice(1))
        assertEquals(RollSpecialResult.CONFIRMED_CRITICAL_FAILURE, confirmed.specialResult)
        assertEquals(7, confirmed.total)
        assertEquals(1, confirmed.followUpDie)
        assertNull(confirmed.followUp)
    }

    @Test
    fun criticalFailureConfirmationDoesNotChangeNumericTotalWhenNotConfirmed() {
        val initial = rollCheck(
            mode = RollMode.NORMAL,
            effectCount = 0,
            checkBonus = 2,
            checkBonusLabel = "Бонус",
            situationalBonus = -1,
            rollDie = dice(1, 1),
        )
        val resolved = rollFollowUp(initial, dice(5))
        assertEquals(RollSpecialResult.CRITICAL_FAILURE, resolved.specialResult)
        assertEquals(initial.total, resolved.total)
        assertEquals(5, resolved.followUpDie)
    }

    @Test
    fun superiorityDieAddsToCriticalSuccessAndThirdSixConfirmsIt() {
        val initial = rollCheck(
            mode = RollMode.NORMAL,
            effectCount = 0,
            checkBonus = 2,
            checkBonusLabel = "Бонус",
            situationalBonus = 1,
            rollDie = dice(6, 6),
        )
        assertEquals(RollSpecialResult.CRITICAL_SUCCESS, initial.specialResult)
        assertEquals(RollFollowUp.SUPERIORITY_DIE, initial.followUp)
        assertEquals(15, initial.total)

        val confirmed = rollFollowUp(initial, dice(6))
        assertEquals(RollSpecialResult.CONFIRMED_CRITICAL_SUCCESS, confirmed.specialResult)
        assertEquals(21, confirmed.total)
        assertEquals(6, confirmed.followUpDie)
        assertNull(confirmed.followUp)
    }

    @Test
    fun hindranceKeepsTwoLowestAfterDoubleFollowUpDie() {
        val initial = rollCheck(
            mode = RollMode.HINDRANCE,
            effectCount = 1,
            checkBonus = 0,
            checkBonusLabel = "Бонус",
            situationalBonus = 0,
            rollDie = dice(2, 2, 6),
        )
        assertEquals(RollFollowUp.ADVANTAGE_DIE, initial.followUp)

        val resolved = rollFollowUp(initial, dice(1))
        assertEquals(setOf(0, 3), resolved.chosenIndices)
        assertEquals(3, resolved.total)
    }
    @Test
    fun comparesRollAgainstOptionalTargetWithoutChangingRollTotal() {
        val success = compareRollToTarget(total = 14, target = 11)
        assertEquals(3, success.margin)
        assertEquals(RollTargetOutcome.SUCCESS, success.outcome)

        val tie = compareRollToTarget(total = 11, target = 11)
        assertEquals(0, tie.margin)
        assertEquals(RollTargetOutcome.TIE, tie.outcome)

        val failure = compareRollToTarget(total = 8, target = 11)
        assertEquals(-3, failure.margin)
        assertEquals(RollTargetOutcome.FAILURE, failure.outcome)
    }

}
