package com.arka.ganhos

import com.arka.ganhos.core.Calculator
import com.arka.ganhos.data.AppSettings
import com.arka.ganhos.data.RideOffer
import com.arka.ganhos.data.Verdict
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
        assertEquals(Verdict.YELLOW, Calculator.analyze(RideOffer(11.0, 2.0, 10, 4.0, 11), s).verdict)
    }

    @Test
    fun badRideIsRed() {
        assertEquals(Verdict.RED, Calculator.analyze(RideOffer(6.5, 3.0, 12, 3.5, 10), s).verdict)
    }

    @Test
    fun rideThatDoesNotCoverCostIsRed() {
        assertEquals(Verdict.RED, Calculator.analyze(RideOffer(3.0, 1.0, 5, 4.0, 10), s).verdict)
    }

    @Test
    fun appSpecificBandOverridesDefault() {
        val offer = RideOffer(18.5, 1.2, 4, 6.3, 14, source = "Uber")
        assertEquals(Verdict.GREEN, Calculator.analyze(offer, s).verdict)
        val strict = s.withBand("Uber") { it.withGreenKm(4.0) }
        assertEquals(Verdict.YELLOW, Calculator.analyze(offer, strict).verdict)
        // outro app continua com o padrão
        assertEquals(Verdict.GREEN, Calculator.analyze(offer.copy(source = "99"), strict).verdict)
    }

    @Test
    fun belowYellowBandIsRed() {
        val offer = RideOffer(9.0, 1.0, 3, 5.0, 12)  // 1,50 R$/km, 36 R$/h
        val raised = s.withBand(null) { it.withYellowKm(1.6) }
        assertEquals(Verdict.RED, Calculator.analyze(offer, raised).verdict)
    }
}
