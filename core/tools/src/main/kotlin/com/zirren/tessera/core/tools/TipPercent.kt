package com.zirren.tessera.core.tools

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

data class TipSplitResult(
    val billAmount: BigDecimal,
    val tipPercent: BigDecimal,
    val tipAmount: BigDecimal,
    val totalAmount: BigDecimal,
    val numberOfPeople: Int,
    val totalPerPerson: BigDecimal,
    val tipPerPerson: BigDecimal,
)

object TipCalculator {

    fun calculate(
        bill: BigDecimal,
        tipPercent: BigDecimal,
        people: Int = 1,
        roundUpTotal: Boolean = false,
    ): TipSplitResult {
        val safeBill = bill.max(BigDecimal.ZERO)
        val safePeople = people.coerceAtLeast(1)
        val safePercent = tipPercent.max(BigDecimal.ZERO)

        val rawTip = safeBill.multiply(safePercent).divide(BigDecimal(100), MathContext.DECIMAL128)
        var total = safeBill.add(rawTip)

        val finalTip = if (roundUpTotal && total > BigDecimal.ZERO) {
            val roundedTotal = total.setScale(0, RoundingMode.CEILING)
            val diff = roundedTotal.subtract(safeBill)
            total = roundedTotal
            diff.max(BigDecimal.ZERO)
        } else {
            rawTip.setScale(2, RoundingMode.HALF_UP)
        }

        val finalTotal = safeBill.add(finalTip).setScale(2, RoundingMode.HALF_UP)
        val totalPerPerson = finalTotal.divide(BigDecimal(safePeople), 2, RoundingMode.CEILING)
        val tipPerPerson = finalTip.divide(BigDecimal(safePeople), 2, RoundingMode.CEILING)

        return TipSplitResult(
            billAmount = safeBill.setScale(2, RoundingMode.HALF_UP),
            tipPercent = safePercent,
            tipAmount = finalTip,
            totalAmount = finalTotal,
            numberOfPeople = safePeople,
            totalPerPerson = totalPerPerson,
            tipPerPerson = tipPerPerson,
        )
    }
}

object PercentCalculator {

    /** What is [percent]% of [total]? */
    fun percentageOf(percent: BigDecimal, total: BigDecimal): BigDecimal =
        total.multiply(percent).divide(BigDecimal(100), MathContext.DECIMAL128)

    /** [part] is what % of [total]? */
    fun fractionOf(part: BigDecimal, total: BigDecimal): BigDecimal? {
        if (total.signum() == 0) return null
        return part.multiply(BigDecimal(100)).divide(total, MathContext.DECIMAL128)
    }

    /** Percentage change from [from] to [to]. */
    fun percentageChange(from: BigDecimal, to: BigDecimal): BigDecimal? {
        if (from.signum() == 0) return null
        val diff = to.subtract(from)
        return diff.multiply(BigDecimal(100)).divide(from.abs(), MathContext.DECIMAL128)
    }
}
