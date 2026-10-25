package com.arka.ganhos

import com.arka.ganhos.core.TRIAL_DAYS
import com.arka.ganhos.core.trialDaysLeft
import org.junit.Assert.assertEquals
import org.junit.Test

class TrialTest {
    private val day = 24L * 60 * 60 * 1000

    @Test
    fun startsWithSixtyDays() {
        assertEquals(60, TRIAL_DAYS)
        assertEquals(60, trialDaysLeft(1_000_000L, 1_000_000L))
    }

    @Test
    fun countsDownByWholeDays() {
        assertEquals(60, trialDaysLeft(0, day - 1))
        assertEquals(59, trialDaysLeft(0, day))
        assertEquals(1, trialDaysLeft(0, 59 * day + day / 2))
    }

    @Test
    fun endsAtSixtyDaysAndStaysEnded() {
        assertEquals(0, trialDaysLeft(0, 60 * day))
        assertEquals(0, trialDaysLeft(0, 400 * day))
    }

    @Test
    fun clockBeforeStartDoesNotExtendTrial() {
        assertEquals(60, trialDaysLeft(10 * day, 0))
    }
}
