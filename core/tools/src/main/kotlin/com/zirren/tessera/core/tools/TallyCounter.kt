package com.zirren.tessera.core.tools

data class TallyCounter(
    val id: String = "default",
    val title: String = "Counter",
    val count: Long = 0L,
    val step: Long = 1L,
    val target: Long? = null,
) {
    val isTargetReached: Boolean
        get() = target != null && count >= target

    fun increment(): TallyCounter = copy(count = count + step)

    fun decrement(): TallyCounter = copy(count = (count - step).coerceAtLeast(0L))

    fun reset(): TallyCounter = copy(count = 0L)

    fun setStepSize(newStep: Long): TallyCounter = copy(step = newStep.coerceAtLeast(1L))

    fun encode(): String = "$id|$title|$count|$step|${target ?: ""}"

    companion object {
        fun decode(text: String?): TallyCounter {
            if (text.isNullOrEmpty()) return TallyCounter()
            val parts = text.split('|')
            if (parts.size != 5) return TallyCounter()
            return try {
                TallyCounter(
                    id = parts[0],
                    title = parts[1],
                    count = parts[2].toLong(),
                    step = parts[3].toLong(),
                    target = parts[4].takeIf { it.isNotEmpty() }?.toLong(),
                )
            } catch (e: Exception) {
                TallyCounter()
            }
        }
    }
}
