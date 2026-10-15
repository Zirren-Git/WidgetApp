package com.zirren.tessera.core.tools

import java.util.Random

enum class CoinSide { HEADS, TAILS }

data class CoinFlipHistory(
    val headsCount: Int = 0,
    val tailsCount: Int = 0,
    val lastResult: CoinSide? = null,
) {
    val total: Int get() = headsCount + tailsCount
}

enum class DieType(val sides: Int, val label: String) {
    D4(4, "d4"),
    D6(6, "d6"),
    D8(8, "d8"),
    D10(10, "d10"),
    D12(12, "d12"),
    D20(20, "d20"),
    D100(100, "d100"),
}

data class RollRecord(
    val dieType: DieType,
    val count: Int,
    val results: List<Int>,
    val modifier: Int = 0,
) {
    val total: Int get() = results.sum() + modifier
}

class DiceCoinEngine(private val random: Random = Random()) {

    fun flipCoin(current: CoinFlipHistory = CoinFlipHistory()): Pair<CoinSide, CoinFlipHistory> {
        val side = if (random.nextBoolean()) CoinSide.HEADS else CoinSide.TAILS
        val next = current.copy(
            headsCount = if (side == CoinSide.HEADS) current.headsCount + 1 else current.headsCount,
            tailsCount = if (side == CoinSide.TAILS) current.tailsCount + 1 else current.tailsCount,
            lastResult = side,
        )
        return side to next
    }

    fun rollDice(dieType: DieType, count: Int = 1, modifier: Int = 0): RollRecord {
        val clampedCount = count.coerceIn(1, 10)
        val rolls = (1..clampedCount).map { random.nextInt(dieType.sides) + 1 }
        return RollRecord(
            dieType = dieType,
            count = clampedCount,
            results = rolls,
            modifier = modifier,
        )
    }
}
