package com.zirren.tessera.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.util.Locale

class NumbersTest {
    private val us = NumberStyle.PLAIN
    private val swedish = NumberStyle(decimal = ',', grouping = '\u00A0')

    @Test fun groupsTheIntegerPartInThrees() {
        assertEquals("0", NumberText.group("0", us))
        assertEquals("999", NumberText.group("999", us))
        assertEquals("1,000", NumberText.group("1000", us))
        assertEquals("123,456,789", NumberText.group("123456789", us))
        assertEquals("1\u00A0234\u00A0567,891", NumberText.group("1234567.891", swedish))
    }

    @Test fun keepsWhatWasTypedAfterThePoint() {
        assertEquals("12.", NumberText.group("12.", us))
        assertEquals("0.50", NumberText.group("0.50", us))
        assertEquals("1\u00A0000,", NumberText.group("1000.", swedish))
    }

    @Test fun writesATrueMinusSign() {
        assertEquals("\u22121,234", NumberText.group("-1234", us))
        assertEquals("\u22120", NumberText.group("-0", us))
        assertEquals("\u22120,5", NumberText.group("-0.5", swedish))
    }

    @Test fun roundsToSignificantDigitsAndDropsTrailingZeros() {
        assertEquals("0.666666666667", NumberText.round(BigDecimal("0.66666666666666666")).toPlainString())
        assertEquals("1", NumberText.round(BigDecimal("0.99999999999999999")).toPlainString())
        assertEquals("2.5", NumberText.round(BigDecimal("2.500")).toPlainString())
        assertEquals("1200", NumberText.round(BigDecimal("1.2E+3")).toPlainString())
        assertEquals("0", NumberText.round(BigDecimal("0.000")).toPlainString())
        assertEquals("62.1371", NumberText.round(BigDecimal("62.137119223733"), significant = 6).toPlainString())
        assertEquals("123456789012345", NumberText.round(BigDecimal("123456789012345")).toPlainString())
        assertEquals("1234568", NumberText.round(BigDecimal("1234567.89"), significant = 6).toPlainString())
    }

    @Test fun capsDecimalsSoTinyNumbersBecomeZero() {
        assertEquals("0", NumberText.round(BigDecimal("3.3E-13")).toPlainString())
        assertEquals("0.000000000001", NumberText.round(BigDecimal("1E-12")).toPlainString())
        assertEquals("0.12", NumberText.round(BigDecimal("0.123"), maxFraction = 2).toPlainString())
    }

    @Test fun formatsAndCopiesInTheRegionsStyle() {
        assertEquals("1\u00A0234,5", NumberText.format(BigDecimal("1234.5"), swedish))
        assertEquals("\u22121,234.5", NumberText.format(BigDecimal("-1234.50"), us))
        assertEquals("-1234,5", NumberText.plain(BigDecimal("-1234.50"), swedish))
    }

    @Test fun readsTheStyleFromTheLocale() {
        val american = NumberStyle.of(Locale.US)
        assertEquals('.', american.decimal)
        assertEquals(',', american.grouping)
        // Android and full JDKs have every locale; trimmed runtimes may only have English.
        val available = DecimalFormatSymbols.getAvailableLocales().toSet()
        if (Locale.forLanguageTag("sv-SE") !in available || Locale.GERMANY !in available) return
        val sweden = NumberStyle.of(Locale.forLanguageTag("sv-SE"))
        assertEquals(',', sweden.decimal)
        assertTrue("grouping was U+%04X".format(sweden.grouping.code), Character.isSpaceChar(sweden.grouping))
        val germany = NumberStyle.of(Locale.GERMANY)
        assertEquals(',', germany.decimal)
        assertEquals('.', germany.grouping)
        assertEquals(NumberStyle.MINUS_SIGN, sweden.minus)
    }
}
