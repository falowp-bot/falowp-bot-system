package com.blr19c.falowp.bot.system.scheduling.cron

import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal

class CronExpression private constructor(
    seconds: CronField,
    minutes: CronField,
    hours: CronField,
    daysOfMonth: CronField,
    months: CronField,
    daysOfWeek: CronField,
    private val expression: String,
) {

    // Reverse order to apply larger field adjustments first.
    private val fields: Array<CronField> =
        arrayOf(daysOfWeek, months, daysOfMonth, hours, minutes, seconds, CronField.zeroNanos())

    fun <T> next(temporal: T): T? where T : Temporal? {
        if (temporal == null) {
            return null
        }
        val seed = ChronoUnit.NANOS.addTo(temporal, 1)
        val result = nextOrSame(seed) ?: return null
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    private fun nextOrSame(temporal: Temporal): Temporal? {
        var candidate = temporal
        repeat(MAX_ATTEMPTS) {
            val next = nextOrSameInternal(candidate) ?: return null
            if (next == candidate) {
                return next
            }
            candidate = next
        }
        return null
    }

    private fun nextOrSameInternal(temporal: Temporal): Temporal? {
        var candidate: Temporal? = temporal
        for (field in fields) {
            candidate = candidate?.let { field.nextOrSame(it) }
            if (candidate == null) {
                return null
            }
        }
        return candidate
    }

    override fun hashCode(): Int = fields.contentHashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        return other is CronExpression && fields.contentEquals(other.fields)
    }

    override fun toString(): String = expression

    companion object {
        const val MAX_ATTEMPTS = 366

        private val MACROS: Map<String, String> = mapOf(
            "@yearly" to "0 0 0 1 1 *",
            "@annually" to "0 0 0 1 1 *",
            "@monthly" to "0 0 0 1 * *",
            "@weekly" to "0 0 0 * * 0",
            "@daily" to "0 0 0 * * *",
            "@midnight" to "0 0 0 * * *",
            "@hourly" to "0 0 * * * *",
        )

        fun parse(exp: String): CronExpression {
            val expression = resolveMacros(exp)
            val fields = tokenizeFields(expression)
            require(fields.size == 6) {
                "Cron expression must consist of 6 fields (found ${fields.size} in \"$expression\")"
            }

            return try {
                val seconds = CronField.parseSeconds(fields[0])
                val minutes = CronField.parseMinutes(fields[1])
                val hours = CronField.parseHours(fields[2])
                val daysOfMonth = CronField.parseDaysOfMonth(fields[3])
                val months = CronField.parseMonth(fields[4])
                val daysOfWeek = CronField.parseDaysOfWeek(fields[5])
                CronExpression(seconds, minutes, hours, daysOfMonth, months, daysOfWeek, expression)
            } catch (ex: IllegalArgumentException) {
                throw IllegalArgumentException("${ex.message} in cron expression \"$expression\"", ex)
            }
        }

        private fun resolveMacros(exp: String): String {
            val expression = exp.trim()
            return MACROS[expression.lowercase()] ?: expression
        }

        private fun tokenizeFields(expression: String): Array<String> {
            val tokens = ArrayList<String>(6)
            val length = expression.length
            var index = 0
            while (index < length) {
                while (index < length && expression[index].isWhitespace()) {
                    index++
                }
                if (index >= length) {
                    break
                }
                val start = index
                while (index < length && !expression[index].isWhitespace()) {
                    index++
                }
                tokens.add(expression.substring(start, index))
                if (tokens.size > 6) {
                    break
                }
            }
            return tokens.toTypedArray()
        }
    }
}
