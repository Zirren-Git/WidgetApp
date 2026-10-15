package com.zirren.tessera.core.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DateSpanTest {

    @Test fun sameDayDifferenceIsZero() {
        val today = LocalDate.of(2026, 10, 15)
        val diff = DateSpanCalculator.difference(today, today)
        assertEquals(0L, diff.totalDays)
        assertEquals(0L, diff.businessDays)
        assertEquals(0L, diff.weeks)
        assertFalse(diff.isPast)
    }

    @Test fun forwardSpanAcrossWeekends() {
        // Thursday 2026-10-15 to Monday 2026-10-19: 4 total days (Thu, Fri, Sat, Sun) -> 2 business days (Thu, Fri), 2 weekend days
        val start = LocalDate.of(2026, 10, 15)
        val end = LocalDate.of(2026, 10, 19)
        val diff = DateSpanCalculator.difference(start, end)
        assertEquals(4L, diff.totalDays)
        assertEquals(2L, diff.businessDays)
        assertEquals(2L, diff.weekendDays)
        assertFalse(diff.isPast)
    }

    @Test fun fullWeekHasFiveBusinessDays() {
        val start = LocalDate.of(2026, 10, 12) // Monday
        val end = LocalDate.of(2026, 10, 19) // Next Monday
        val diff = DateSpanCalculator.difference(start, end)
        assertEquals(7L, diff.totalDays)
        assertEquals(5L, diff.businessDays)
        assertEquals(2L, diff.weekendDays)
        assertEquals(1L, diff.weeks)
        assertEquals(0L, diff.remainderDays)
    }

    @Test fun pastDateIsMarkedPast() {
        val start = LocalDate.of(2026, 10, 15)
        val past = LocalDate.of(2026, 10, 10)
        val diff = DateSpanCalculator.difference(start, past)
        assertEquals(5L, diff.totalDays)
        assertTrue(diff.isPast)
    }

    @Test fun periodBreakdownMonthsAndDays() {
        val start = LocalDate.of(2026, 1, 15)
        val end = LocalDate.of(2026, 3, 20)
        val diff = DateSpanCalculator.difference(start, end)
        assertEquals(2, diff.months)
        assertEquals(5, diff.days)
        assertEquals(0, diff.years)
    }

    @Test fun addDaysCalculation() {
        val start = LocalDate.of(2026, 10, 15)
        assertEquals(LocalDate.of(2026, 11, 14), DateSpanCalculator.addDays(start, 30))
        assertEquals(LocalDate.of(2027, 1, 23), DateSpanCalculator.addDays(start, 100))
    }
}
