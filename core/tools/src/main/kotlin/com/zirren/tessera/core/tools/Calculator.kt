package com.zirren.tessera.core.tools

import com.zirren.tessera.core.model.NumberStyle
import com.zirren.tessera.core.model.NumberText
import java.math.BigDecimal
import java.math.MathContext

/** The calculator's keys. There is no expression field: people only ever press these. */
enum class CalcKey {
    DIGIT_0, DIGIT_1, DIGIT_2, DIGIT_3, DIGIT_4, DIGIT_5, DIGIT_6, DIGIT_7, DIGIT_8, DIGIT_9,
    DECIMAL, PLUS, MINUS, TIMES, DIVIDE, PERCENT, SIGN, EQUALS, CLEAR, BACKSPACE;

    /** 0–9 for digit keys, otherwise null. */
    val digit: Int? get() = if (ordinal <= 9) ordinal else null

    companion object {
        fun digit(value: Int): CalcKey {
            require(value in 0..9) { "not a digit: $value" }
            return entries[value]
        }
    }
}

enum class CalcOp {
    PLUS, MINUS, TIMES, DIVIDE;

    val multiplicative: Boolean get() = this == TIMES || this == DIVIDE
}

enum class CalcError { DIVIDE_BY_ZERO, TOO_LARGE }

/**
 * A calculator that behaves like the ones people know from their phones:
 * - × and ÷ come before + and − (2 + 3 × 4 = 14), with no brackets to learn;
 * - pressing "=" again repeats the last step (5 + 3 = = gives 8, then 11);
 * - "50 + 10 %" adds 10 % of 50, while "50 × 10 %" multiplies by 0.1;
 * - all arithmetic is decimal, so 0.1 + 0.2 is exactly 0.3.
 *
 * Immutable: [press] returns the next state. Values are kept to 34 significant digits and shown
 * rounded to [SIGNIFICANT] digits, so 1 ÷ 3 × 3 shows 1.
 */
data class CalculatorState(
    val phase: Phase = Phase.TYPING,
    /** The number being typed, as plain ASCII ("-12.50"). Only meaningful while [phase] is TYPING. */
    val input: String = "0",
    /** The number on the display in every other phase: a result or a running total. */
    val shown: BigDecimal = BigDecimal.ZERO,
    /** Everything left of a pending + or −, already worked out. */
    val sum: BigDecimal? = null,
    val addOp: CalcOp? = null,
    /** Everything left of a pending × or ÷ within the current term. */
    val product: BigDecimal? = null,
    val mulOp: CalcOp? = null,
    /** What a repeated "=" does: the last step and its right-hand number. */
    val lastOp: CalcOp? = null,
    val lastOperand: BigDecimal? = null,
    val error: CalcError? = null,
) {
    enum class Phase {
        /** The user is typing [input]. */
        TYPING,
        /** An operator was just pressed; the display shows the running total. */
        OPERATOR,
        /** % or ± changed the number on the display; it is the next operand. */
        COMPUTED,
        /** "=" was just pressed. */
        RESULT,
        ERROR,
    }

    /** The operator to highlight: the one just pressed, until the next number starts. */
    val activeOperator: CalcOp? get() = if (phase == Phase.OPERATOR) mulOp ?: addOp else null

    /** Text for the display, in the phone's number style. Empty while [error] is set. */
    fun display(style: NumberStyle): String = when (phase) {
        Phase.TYPING -> NumberText.group(input, style)
        Phase.ERROR -> ""
        else -> NumberText.format(shown, style, SIGNIFICANT, MAX_FRACTION)
    }

    /** The number on the display without grouping, for copying into other apps. Null while [error] is set. */
    fun copyText(style: NumberStyle): String? = when (phase) {
        Phase.ERROR -> null
        Phase.TYPING -> NumberText.plain(BigDecimal(input), style, MAX_DIGITS, MAX_DIGITS)
        else -> NumberText.plain(shown, style, SIGNIFICANT, MAX_FRACTION)
    }

    fun press(key: CalcKey): CalculatorState {
        if (phase == Phase.ERROR) {
            // After an error, digits start a fresh calculation and every other key just clears it.
            val fresh = CalculatorState()
            return if (key.digit != null || key == CalcKey.DECIMAL) fresh.press(key) else fresh
        }
        key.digit?.let { return typeDigit(it) }
        return when (key) {
            CalcKey.DECIMAL -> typeDecimal()
            CalcKey.BACKSPACE -> backspace()
            CalcKey.CLEAR -> CalculatorState()
            CalcKey.SIGN -> toggleSign()
            CalcKey.PERCENT -> percent()
            CalcKey.PLUS -> operator(CalcOp.PLUS)
            CalcKey.MINUS -> operator(CalcOp.MINUS)
            CalcKey.TIMES -> operator(CalcOp.TIMES)
            CalcKey.DIVIDE -> operator(CalcOp.DIVIDE)
            CalcKey.EQUALS -> evaluate()
            else -> this
        }
    }

    fun pressAll(vararg keys: CalcKey): CalculatorState = keys.fold(this) { state, key -> state.press(key) }

    /** One line of text, for saving across rotation and process death. */
    fun encode(): String = listOf(
        FORMAT, phase.name, input, shown.toPlainString(), sum?.toPlainString(), addOp?.name,
        product?.toPlainString(), mulOp?.name, lastOp?.name, lastOperand?.toPlainString(), error?.name,
    ).joinToString("|") { it.orEmpty() }

    // ---- Keys ----------------------------------------------------------------------------

    private val operand: BigDecimal get() = if (phase == Phase.TYPING) BigDecimal(input) else shown

    private fun typeDigit(digit: Int): CalculatorState = when (phase) {
        Phase.TYPING -> when {
            input == "0" -> copy(input = "$digit")
            input == "-0" -> copy(input = "-$digit")
            input.count { it.isDigit() } >= MAX_DIGITS -> this
            else -> copy(input = input + digit)
        }
        // A new number after "=" starts a new calculation; "=" can still repeat the last step on it.
        Phase.RESULT -> CalculatorState(input = "$digit", lastOp = lastOp, lastOperand = lastOperand)
        else -> copy(phase = Phase.TYPING, input = "$digit")
    }

    private fun typeDecimal(): CalculatorState = when (phase) {
        Phase.TYPING -> if ('.' in input) this else copy(input = "$input.")
        Phase.RESULT -> CalculatorState(input = "0.", lastOp = lastOp, lastOperand = lastOperand)
        else -> copy(phase = Phase.TYPING, input = "0.")
    }

    private fun backspace(): CalculatorState = when (phase) {
        Phase.TYPING -> {
            val shorter = input.dropLast(1)
            copy(input = if (shorter.isEmpty() || shorter == "-") "0" else shorter)
        }
        // A worked-out number can't be edited digit by digit, so it clears to 0.
        Phase.COMPUTED, Phase.RESULT -> copy(phase = Phase.TYPING, input = "0")
        else -> this
    }

    private fun toggleSign(): CalculatorState = when (phase) {
        Phase.TYPING -> copy(input = if (input.startsWith('-')) input.substring(1) else "-$input")
        Phase.OPERATOR -> copy(phase = Phase.TYPING, input = "-0")
        else -> copy(phase = Phase.COMPUTED, shown = shown.negate())
    }

    private fun percent(): CalculatorState {
        val x = operand
        val base = sum
        val value = if (mulOp == null && addOp != null && base != null) base.multiply(x).divide(HUNDRED) else x.divide(HUNDRED)
        return checked(value) { copy(phase = Phase.COMPUTED, shown = it) }
    }

    private fun operator(op: CalcOp): CalculatorState {
        if (phase == Phase.OPERATOR) return replaceOperator(op)
        val x = operand
        val term = currentTerm(x) ?: return divideByZero()
        return if (op.multiplicative) {
            checked(term) { copy(phase = Phase.OPERATOR, product = it, mulOp = op, shown = it) }
        } else {
            checked(withSum(term)) { copy(phase = Phase.OPERATOR, sum = it, addOp = op, product = null, mulOp = null, shown = it) }
        }
    }

    /** Pressing another operator right away replaces the first one: 5 + × 2 means 5 × 2. */
    private fun replaceOperator(op: CalcOp): CalculatorState = when {
        op.multiplicative && mulOp != null -> copy(mulOp = op)
        op.multiplicative -> copy(product = shown, mulOp = op, sum = null, addOp = null)
        mulOp != null -> checked(withSum(product ?: shown)) { copy(sum = it, addOp = op, product = null, mulOp = null, shown = it) }
        else -> copy(addOp = op)
    }

    private fun evaluate(): CalculatorState {
        val x = operand
        if (addOp == null && mulOp == null) {
            val op = lastOp
            val repeatWith = lastOperand
            if (op == null || repeatWith == null) return checked(x) { copy(phase = Phase.RESULT, shown = it) }
            val repeated = apply(x, op, repeatWith) ?: return divideByZero()
            return checked(repeated) { copy(phase = Phase.RESULT, shown = it) }
        }
        val step = mulOp ?: addOp
        val term = currentTerm(x) ?: return divideByZero()
        return checked(withSum(term)) { CalculatorState(phase = Phase.RESULT, shown = it, lastOp = step, lastOperand = x) }
    }

    /** The current term with [x] as its last factor, or null for a division by zero. */
    private fun currentTerm(x: BigDecimal): BigDecimal? {
        val left = product
        val op = mulOp
        return if (left != null && op != null) apply(left, op, x) else x
    }

    private fun withSum(term: BigDecimal): BigDecimal {
        val left = sum
        val op = addOp
        return if (left != null && op != null) apply(left, op, term) ?: term else term
    }

    private fun divideByZero() = CalculatorState(phase = Phase.ERROR, error = CalcError.DIVIDE_BY_ZERO)

    /** Keeps 34 significant digits, and refuses numbers that would show more than 15 whole digits. */
    private inline fun checked(value: BigDecimal, next: (BigDecimal) -> CalculatorState): CalculatorState {
        val rounded = value.round(MathContext.DECIMAL128)
        val asShown = NumberText.round(rounded, SIGNIFICANT, MAX_FRACTION)
        return if (asShown.abs() >= LIMIT) CalculatorState(phase = Phase.ERROR, error = CalcError.TOO_LARGE) else next(rounded)
    }

    companion object {
        /** Digits a person can type into one number. */
        const val MAX_DIGITS = 15

        /** Significant digits and decimals shown for a worked-out number. */
        const val SIGNIFICANT = 12
        const val MAX_FRACTION = 12

        private const val FORMAT = "1"
        private val HUNDRED = BigDecimal(100)
        private val LIMIT = BigDecimal("1E15")

        private fun apply(left: BigDecimal, op: CalcOp, right: BigDecimal): BigDecimal? = when (op) {
            CalcOp.PLUS -> left.add(right)
            CalcOp.MINUS -> left.subtract(right)
            CalcOp.TIMES -> left.multiply(right)
            CalcOp.DIVIDE -> if (right.signum() == 0) null else left.divide(right, MathContext.DECIMAL128)
        }

        /** Restores [encode]d text; anything unreadable gives a fresh calculator. */
        fun decode(text: String?): CalculatorState {
            val parts = text?.split('|') ?: return CalculatorState()
            if (parts.size != 11 || parts[0] != FORMAT) return CalculatorState()
            return try {
                fun number(i: Int) = parts[i].takeIf { it.isNotEmpty() }?.let(::BigDecimal)
                fun op(i: Int) = parts[i].takeIf { it.isNotEmpty() }?.let(CalcOp::valueOf)
                val phase = Phase.valueOf(parts[1])
                val input = parts[2]
                BigDecimal(input) // must be a valid number
                CalculatorState(
                    phase = phase,
                    input = input,
                    shown = number(3) ?: BigDecimal.ZERO,
                    sum = number(4),
                    addOp = op(5),
                    product = number(6),
                    mulOp = op(7),
                    lastOp = op(8),
                    lastOperand = number(9),
                    error = parts[10].takeIf { it.isNotEmpty() }?.let(CalcError::valueOf),
                )
            } catch (e: IllegalArgumentException) {
                // NumberFormatException is an IllegalArgumentException too.
                CalculatorState()
            }
        }
    }
}
