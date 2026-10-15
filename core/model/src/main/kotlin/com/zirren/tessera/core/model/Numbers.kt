package com.zirren.tessera.core.model

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * How a region writes numbers: "1,234.5" in the US, "1 234,5" in Sweden.
 * Tessera always shows a true minus sign (−); every bundled font has it.
 */
data class NumberStyle(val decimal: Char, val grouping: Char, val minus: Char = MINUS_SIGN) {
    companion object {
        const val MINUS_SIGN = '\u2212'

        /** "1,234.5": for tests, and when a locale gives nothing usable. */
        val PLAIN = NumberStyle(decimal = '.', grouping = ',')

        fun of(locale: Locale): NumberStyle {
            val symbols = DecimalFormatSymbols.getInstance(locale)
            val decimal = symbols.decimalSeparator
            val grouping = symbols.groupingSeparator.takeIf { it != decimal } ?: if (decimal == ',') '.' else ','
            return NumberStyle(decimal, grouping)
        }
    }
}

/** Numbers as people read them. Groups in threes, which fits every language Tessera targets today. */
object NumberText {

    /**
     * Groups the integer part of a plain decimal string such as "-1234567.80" or "12.", in [style].
     * Everything after the point is kept as typed, including trailing zeros and a trailing point.
     */
    fun group(plain: String, style: NumberStyle): String {
        val negative = plain.startsWith('-')
        val unsigned = if (negative) plain.substring(1) else plain
        val point = unsigned.indexOf('.')
        val integer = if (point >= 0) unsigned.substring(0, point) else unsigned
        val out = StringBuilder(plain.length + plain.length / 3 + 1)
        if (negative) out.append(style.minus)
        integer.forEachIndexed { index, char ->
            if (index > 0 && (integer.length - index) % 3 == 0) out.append(style.grouping)
            out.append(char)
        }
        if (point >= 0) out.append(style.decimal).append(unsigned, point + 1, unsigned.length)
        return out.toString()
    }

    /**
     * At most [significant] significant digits and [maxFraction] decimals, without trailing zeros.
     * Whole-number digits are never rounded away: 123 456 789 012 345 stays exactly that.
     */
    fun round(value: BigDecimal, significant: Int = 12, maxFraction: Int = 12): BigDecimal {
        if (value.signum() == 0) return BigDecimal.ZERO
        val wholeDigits = value.precision() - value.scale()
        var rounded = value.round(MathContext(maxOf(significant, wholeDigits), RoundingMode.HALF_UP))
        if (rounded.scale() > maxFraction) rounded = rounded.setScale(maxFraction, RoundingMode.HALF_UP)
        // Zero is handled on its own: stripTrailingZeros() on zero values differs between Java versions.
        if (rounded.signum() == 0) return BigDecimal.ZERO
        rounded = rounded.stripTrailingZeros()
        return if (rounded.scale() < 0) rounded.setScale(0) else rounded
    }

    /** A value for display, for example "1 234,5" in Sweden. */
    fun format(value: BigDecimal, style: NumberStyle, significant: Int = 12, maxFraction: Int = 12): String =
        group(round(value, significant, maxFraction).toPlainString(), style)

    /** No grouping and an ASCII minus, so it pastes cleanly into other apps: "-1234,5" in Sweden. */
    fun plain(value: BigDecimal, style: NumberStyle, significant: Int = 12, maxFraction: Int = 12): String =
        round(value, significant, maxFraction).toPlainString().replace('.', style.decimal)
}
