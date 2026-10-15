package com.zirren.tessera.core.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class DiceCoinTest {

    @Test fun coinFlipUpdatesCounts() {
        val engine = DiceCoinEngine(Random(42))
        var history = CoinFlipHistory()
        for (i in 1..20) {
            val (_, next) = engine.flipCoin(history)
            history = next
        }
        assertEquals(20, history.total)
        assertTrue(history.headsCount > 0)
        assertTrue(history.tailsCount > 0)
        assertEquals(20, history.headsCount + history.tailsCount)
    }

    @Test fun rollDiceValuesWithinBounds() {
        val engine = DiceCoinEngine(Random(123))
        for (type in DieType.entries) {
            val record = engine.rollDice(type, count = 5)
            assertEquals(5, record.results.size)
            for (r in record.results) {
                assertTrue("roll $r out of bounds for ${type.label}", r in 1..type.sides)
            }
            assertEquals(record.results.sum(), record.total)
        }
    }

    @Test fun rollDiceWithModifier() {
        val engine = DiceCoinEngine(Random(999))
        val record = engine.rollDice(DieType.D20, count = 2, modifier = 5)
        assertEquals(record.results.sum() + 5, record.total)
    }
}
