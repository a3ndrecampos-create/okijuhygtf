package com.rotacerta.app

import com.rotacerta.app.core.Calculator
import com.rotacerta.app.data.AppSettings
import com.rotacerta.app.data.RideOffer
import com.rotacerta.app.data.Verdict
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatorTest {
    private val s = AppSettings()

    @Test
    fun costPerKmUsesAllComponents() {
        assertEquals(0.85, s.costPerKm, 0.001)
    }

    @Test
    fun goodRideIsGreen() {
        val a = Calculator.analyze(RideOffer(18.5, 1.2, 4, 6.3, 14), s)
        assertEquals(Verdict.GREEN, a.verdict)
        assertEquals(12.125, a.profit, 0.001)
    }

    @Test
    fun mediumRideIsYellow() {
        assertEquals(Verdict.YELLOW, Calculator.analyze(RideOffer(9.8, 2.0, 10, 4.0, 11), s).verdict)
    }

    @Test
    fun badRideIsRed() {
        assertEquals(Verdict.RED, Calculator.analyze(RideOffer(6.5, 3.0, 12, 3.5, 10), s).verdict)
    }

    @Test
    fun rideThatDoesNotCoverCostIsRed() {
        assertEquals(Verdict.RED, Calculator.analyze(RideOffer(3.0, 1.0, 5, 4.0, 10), s).verdict)
    }
}
