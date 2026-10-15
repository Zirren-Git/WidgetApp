package com.zirren.tessera.core.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class TipPercentTest {

    @Test fun simpleTipCalculation() {
        val res = TipCalculator.calculate(
            bill = BigDecimal("100.00"),
            tipPercent = BigDecimal(15),
            people = 1,
            roundUpTotal = false,
        )
        assertEquals(BigDecimal("15.00"), res.tipAmount)
        assertEquals(BigDecimal("115.00"), res.totalAmount.setScale(2))
        assertEquals(BigDecimal("115.00"), res.totalPerPerson.setScale(2))
    }

    @Test fun splitBetweenFourPeople() {
        val res = TipCalculator.calculate(
            bill = BigDecimal("80.00"),
            tipPercent = BigDecimal(20),
            people = 4,
            roundUpTotal = false,
        )
        assertEquals(BigDecimal("16.00"), res.tipAmount)
        assertEquals(BigDecimal("96.00"), res.totalAmount)
        assertEquals(BigDecimal("24.00"), res.totalPerPerson)
        assertEquals(BigDecimal("4.00"), res.tipPerPerson)
    }

    @Test fun roundUpTotalAdjustsTip() {
        val res = TipCalculator.calculate(
            bill = BigDecimal("43.20"),
            tipPercent = BigDecimal(15), // 15% is 6.48 -> total 49.68 -> rounds up to 50.00
            people = 2,
            roundUpTotal = true,
        )
        assertEquals(BigDecimal("50.00"), res.totalAmount)
        assertEquals(BigDecimal("6.80"), res.tipAmount)
        assertEquals(BigDecimal("25.00"), res.totalPerPerson)
    }

    @Test fun percentHelpers() {
        // 20% of 250 is 50
        val p = PercentCalculator.percentageOf(BigDecimal(20), BigDecimal(250))
        assertEquals(50.0, p.toDouble(), 1e-4)

        // 25 is what % of 200? (12.5%)
        val frac = PercentCalculator.fractionOf(BigDecimal(25), BigDecimal(200))
        assertNotNull(frac)
        assertEquals(12.5, frac!!.toDouble(), 1e-4)

        // Division by zero returns null
        assertNull(PercentCalculator.fractionOf(BigDecimal(25), BigDecimal.ZERO))

        // Change from 80 to 100 (+25%)
        val change = PercentCalculator.percentageChange(BigDecimal(80), BigDecimal(100))
        assertNotNull(change)
        assertEquals(25.0, change!!.toDouble(), 1e-4)
    }
}
