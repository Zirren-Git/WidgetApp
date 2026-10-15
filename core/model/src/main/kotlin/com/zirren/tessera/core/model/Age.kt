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

/** The words for [Age]. English defaults; Android can build this from string resources. */
data class AgePhrases(
    val justNow: String = "just now",
    val minutesAgo: String = "%d min ago",
    val hoursAgo: String = "%d h ago",
    val daysAgo: String = "%d days ago",
) {
    fun format(age: Age): String = when (age) {
        Age.JustNow -> justNow
        is Age.Minutes -> minutesAgo.format(age.value)
        is Age.Hours -> hoursAgo.format(age.value)
        is Age.Days -> daysAgo.format(age.value)
    }
}
