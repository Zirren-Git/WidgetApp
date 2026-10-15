package com.zirren.tessera.core.tools

import com.zirren.tessera.core.model.NumberStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorTest {
    /** US style with an ASCII minus, so expectations read naturally; true minus signs are tested on their own. */
    private val us = NumberStyle(decimal = '.', grouping = ',', minus = '-')
    private val swedish = NumberStyle(decimal = ',', grouping = '\u00A0')

    /** Presses keys written the way a person reads them: "12+3=" or "5÷0=". Spaces are ignored. */
    private fun press(keys: String, start: CalculatorState = CalculatorState()): CalculatorState =
        keys.filter { it != ' ' }.fold(start) { state, char -> state.press(key(char)) }

    private fun key(char: Char): CalcKey = when (char) {
        in '0'..'9' -> CalcKey.digit(char - '0')
        '.' -> CalcKey.DECIMAL
        '+' -> CalcKey.PLUS
        '-' -> CalcKey.MINUS
        '×' -> CalcKey.TIMES
        '÷' -> CalcKey.DIVIDE
        '%' -> CalcKey.PERCENT
        '±' -> CalcKey.SIGN
        '=' -> CalcKey.EQUALS
        'C' -> CalcKey.CLEAR
        '<' -> CalcKey.BACKSPACE
        else -> error("no key for $char")
    }

    private fun shows(expected: String, keys: String) = assertEquals("after \"$keys\"", expected, press(keys).display(us))

    // ---- The basics ----------------------------------------------------------------------

    @Test fun decimalArithmeticIsExact() {
        shows("0.3", ".1+.2=")
        shows("0.3", "0.1+0.2=")
        shows("1", "0.1+0.2+0.7=")
    }

    @Test fun startsAtZeroAndReplacesALeadingZero() {
        shows("0", "")
        shows("5", "005")
        shows("0.5", "0.5")
        shows("0.05", "0.05")
    }

    @Test fun ignoresASecondDecimalPoint() = shows("1.25", "1..2.5")

    @Test fun keepsTrailingZerosWhileTyping() {
        shows("2.50", "2.50")
        shows("2.", "2.")
    }

    @Test fun groupsDigitsInThePhonesStyle() {
        assertEquals("1,234,567.89", press("1234567.89").display(us))
        assertEquals("1\u00A0234\u00A0567,89", press("1234567.89").display(swedish))
    }

    @Test fun stopsAtFifteenDigits() {
        shows("123,456,789,012,345", "1234567890123456789")
        shows("1.23456789012345", "1.23456789012345678")
    }

    // ---- Precedence and chains -----------------------------------------------------------

    @Test fun timesAndDivideComeBeforePlusAndMinus() {
        shows("14", "2+3×4=")
        shows("7", "10-6÷2=")
        shows("26", "2×3+4×5=")
        shows("-1", "1-2×3+4=")
    }

    @Test fun showsTheRunningTotalAfterEachOperator() {
        shows("8", "5+3+")
        shows("3", "2+3×")
        shows("12", "2+3×4×")
        shows("14", "2+3×4+")
    }

    @Test fun chainsFromAResult() {
        shows("20", "2+3=×4=")
        shows("1", "2+3=-4=")
    }

    @Test fun anotherOperatorReplacesTheFirst() {
        shows("10", "5+×2=")
        shows("7", "5×+2=")
        shows("5", "2+3×+")
        shows("3", "5+-2=")
    }

    // ---- Repeated equals -----------------------------------------------------------------

    @Test fun repeatedEqualsRepeatsTheLastStep() {
        shows("8", "5+3=")
        shows("11", "5+3==")
        shows("14", "5+3===")
        shows("6", "10-2==")
        shows("18", "2×3==")
        shows("2.5", "10÷2==")
    }

    @Test fun repeatedEqualsWorksOnANewNumber() = shows("13", "5+3=10=")

    @Test fun equalsRightAfterAnOperatorUsesTheShownNumber() {
        shows("10", "5+=")
        shows("25", "5×=")
    }

    @Test fun equalsWithNothingPendingTidiesTheNumber() {
        shows("5", "5.=")
        shows("2.5", "2.50=")
        shows("0", "=")
    }

    @Test fun clearForgetsTheRepeatedStep() = shows("2", "5+3=C2=")

    // ---- Percent, sign, backspace ---------------------------------------------------------

    @Test fun percentAfterPlusOrMinusIsAShareOfTheLeftSide() {
        shows("55", "50+10%=")
        shows("150", "200-25%=")
        shows("75", "50+%=")
    }

    @Test fun percentAfterTimesOrAloneIsHundredths() {
        shows("5", "50×10%=")
        shows("0.1", "10%")
        shows("200", "50÷25%=")
    }

    @Test fun signChangesTheNumberOnTheDisplay() {
        shows("-5", "5±")
        shows("5", "5±±")
        shows("-5", "±5")
        shows("-8", "5+3=±")
        shows("-2", "5+±7=")
        shows("-0.5", "0.5±")
    }

    @Test fun signOnAResultCanBeRepeated() = shows("-5", "5+3=±=")

    @Test fun backspaceDeletesTheLastDigit() {
        shows("12", "123<")
        shows("0", "5<")
        shows("0", "5±<<")
        shows("1.", "1.5<")
        shows("0", "<<<")
    }

    @Test fun backspaceClearsAWorkedOutNumber() {
        shows("0", "2+3=<")
        shows("7", "2+3=<7")
    }

    @Test fun backspaceRightAfterAnOperatorDoesNothing() = shows("5", "5+<")

    @Test fun negativeResultsUseATrueMinusSign() {
        assertEquals("\u22122", press("3-5=").display(NumberStyle.PLAIN))
        assertEquals("\u22121\u00A0000,5", press("1000.5±").display(swedish))
    }

    // ---- Rounding ------------------------------------------------------------------------

    @Test fun roundsLongResultsForTheDisplay() {
        shows("0.666666666667", "2÷3=")
        shows("0.333333333333", "1÷3=")
        shows("1", "1÷3×3=")
        shows("142,857.142857", "1000000÷7=")
    }

    @Test fun tinyResultsRoundToZero() = shows("0", "1÷999999999999999÷999999999999999=")

    // ---- Errors --------------------------------------------------------------------------

    @Test fun divisionByZeroIsAnError() {
        val state = press("5÷0=")
        assertEquals(CalcError.DIVIDE_BY_ZERO, state.error)
        assertEquals("", state.display(us))
        assertNull(state.copyText(us))
        assertEquals(CalcError.DIVIDE_BY_ZERO, press("5÷0+").error)
        assertEquals(CalcError.DIVIDE_BY_ZERO, press("8÷2=0=").let { press("÷0=", it) }.error)
    }

    @Test fun aDigitAfterAnErrorStartsFresh() {
        shows("7", "5÷0=7")
        shows("10", "5÷0=7+3=")
        shows("0.", "5÷0=.")
    }

    @Test fun otherKeysAfterAnErrorJustClearIt() {
        shows("0", "5÷0=+")
        shows("0", "5÷0==")
        assertNull(press("5÷0=+").error)
    }

    @Test fun numbersTooLongForTheDisplayAreAnError() {
        assertEquals(CalcError.TOO_LARGE, press("999999999999999×10=").error)
        assertEquals(CalcError.TOO_LARGE, press("999999999999999+1=").error)
        assertEquals(CalcError.TOO_LARGE, press("999999999×999999999×").error)
        shows("999,999,999,999,999", "999999999999999=")
        // Typing stops at 15 digits, so these larger values are worked out.
        shows("123,456,789,012,346", "123456789012345+0.6=")
        assertEquals(CalcError.TOO_LARGE, press("999999999999999+0.9=").error)
    }

    // ---- What the screen needs -----------------------------------------------------------

    @Test fun highlightsTheOperatorUntilTheNextNumber() {
        assertEquals(CalcOp.PLUS, press("5+").activeOperator)
        assertEquals(CalcOp.TIMES, press("5+3×").activeOperator)
        assertEquals(CalcOp.TIMES, press("5+×").activeOperator)
        assertNull(press("5+3").activeOperator)
        assertNull(press("5+3=").activeOperator)
    }

    @Test fun copiesPlainNumbersThatPasteAnywhere() {
        assertEquals("-1234.5", press("1234.5±").copyText(us))
        assertEquals("-1234,5", press("1234.5±").copyText(swedish))
        assertEquals("0.333333333333", press("1÷3=").copyText(us))
    }

    @Test fun survivesBeingSavedAndRestored() {
        for (keys in listOf("", "12.50", "2+3×", "2+3×4", "5+3=", "50+10%", "5÷0=", "5±", "1÷3")) {
            val state = press(keys)
            val restored = CalculatorState.decode(state.encode())
            assertEquals(keys, state, restored)
            // ...and keeps working the same way afterwards.
            assertEquals(keys, state.press(CalcKey.EQUALS).display(us), restored.press(CalcKey.EQUALS).display(us))
        }
    }

    @Test fun unreadableSavedStateGivesAFreshCalculator() {
        for (text in listOf(null, "", "garbage", "1|TYPING|abc|0||||||||", "2|TYPING|0|0||||||||", "1|NOPE|0|0||||||||")) {
            assertEquals(text.toString(), CalculatorState(), CalculatorState.decode(text))
        }
    }

    @Test fun digitKeysKnowTheirDigit() {
        assertEquals(7, CalcKey.digit(7).digit)
        assertNull(CalcKey.PLUS.digit)
        assertTrue(CalcKey.entries.count { it.digit != null } == 10)
    }
}
