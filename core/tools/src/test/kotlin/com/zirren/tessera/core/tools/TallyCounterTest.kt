package com.zirren.tessera.core.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TallyCounterTest {

    @Test fun incrementAndDecrement() {
        var counter = TallyCounter()
        assertEquals(0L, counter.count)

        counter = counter.increment()
        assertEquals(1L, counter.count)

        counter = counter.increment().increment()
        assertEquals(3L, counter.count)

        counter = counter.decrement()
        assertEquals(2L, counter.count)

        counter = counter.reset()
        assertEquals(0L, counter.count)
    }

    @Test fun decrementDoesNotGoBelowZero() {
        val counter = TallyCounter().decrement()
        assertEquals(0L, counter.count)
    }

    @Test fun stepSizeChanges() {
        var counter = TallyCounter().setStepSize(5)
        counter = counter.increment()
        assertEquals(5L, counter.count)

        counter = counter.increment()
        assertEquals(10L, counter.count)

        counter = counter.decrement()
        assertEquals(5L, counter.count)
    }

    @Test fun targetReachedDetection() {
        val counter = TallyCounter(target = 10, count = 9)
        assertFalse(counter.isTargetReached)

        val done = counter.increment()
        assertTrue(done.isTargetReached)
    }

    @Test fun serializationRoundTrip() {
        val original = TallyCounter(id = "people", title = "Guests", count = 42, step = 5, target = 100)
        val text = original.encode()
        val decoded = TallyCounter.decode(text)
        assertEquals(original, decoded)
    }
}
