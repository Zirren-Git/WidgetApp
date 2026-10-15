package com.zirren.tessera.core.model

import java.time.Duration
import java.time.Instant

/** How old a piece of data is, bucketed for display: "Updated 25 min ago". */
sealed interface Age {
    data object JustNow : Age
    data class Minutes(val value: Int) : Age
    data class Hours(val value: Int) : Age
    data class Days(val value: Int) : Age

    companion object {
        fun between(then: Instant, now: Instant): Age {
            val elapsed = Duration.between(then, now)
            val minutes = elapsed.toMinutes()
            return when {
                minutes < 1 -> JustNow
                minutes < 60 -> Minutes(minutes.toInt())
                minutes < 48 * 60 -> Hours((minutes / 60).toInt())
                else -> Days((minutes / (24 * 60)).toInt())
            }
        }
    }
}

/**
 * The words for [Age]. English defaults; Android builds these from string and plural resources,
 * which is why counts are functions: many languages change more than one word with the number.
 */
class AgePhrases(
    val justNow: String = "just now",
    val minutesAgo: (Int) -> String = { "$it min ago" },
    val hoursAgo: (Int) -> String = { "$it h ago" },
    val daysAgo: (Int) -> String = { "$it days ago" },
) {
    fun format(age: Age): String = when (age) {
        Age.JustNow -> justNow
        is Age.Minutes -> minutesAgo(age.value)
        is Age.Hours -> hoursAgo(age.value)
        is Age.Days -> daysAgo(age.value)
    }
}
