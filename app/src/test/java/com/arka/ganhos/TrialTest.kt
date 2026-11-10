package com.arka.ganhos

import com.arka.ganhos.core.TRIAL_DAYS
import com.arka.ganhos.core.trialDaysLeft
import org.junit.Assert.assertEquals
import org.junit.Test

class TrialTest {
    private val day = 24L * 60 * 60 * 1000

    @Test
    fun startsWithOneTwentyDays() {
        assertEquals(120, TRIAL_DAYS)
        assertEquals(120, trialDaysLeft(1_000_000L, 1_000_000L))
    }

    @Test
    fun countsDownByWholeDays() {
        assertEquals(120, trialDaysLeft(0, day - 1))
        assertEquals(119, trialDaysLeft(0, day))
        assertEquals(1, trialDaysLeft(0, 119 * day + day / 2))
    }

    @Test
    fun endsAtOneTwentyDaysAndStaysEnded() {
        assertEquals(0, trialDaysLeft(0, 120 * day))
        assertEquals(0, trialDaysLeft(0, 400 * day))
    }

    @Test
    fun clockBeforeStartDoesNotExtendTrial() {
        assertEquals(120, trialDaysLeft(10 * day, 0))
    }
}
