package com.blr19c.falowp.bot.system.scheduling.cron

import com.blr19c.falowp.bot.system.scheduling.cron.QuartzCronField.Companion.isQuartzDaysOfMonthField
import com.blr19c.falowp.bot.system.scheduling.cron.QuartzCronField.Companion.isQuartzDaysOfWeekField
import java.time.DateTimeException
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import java.time.temporal.ValueRange
import java.util.*

/**
 * Single field in a cron pattern. Created using the `parse*` methods.
 */
internal abstract class CronField protected constructor(private val type: Type) {

    abstract fun nextOrSame(temporal: Temporal): Temporal?

    protected fun type(): Type = type

    enum class Type(
        private val field: ChronoField,
        private val higherOrder: ChronoUnit,
        vararg lowerOrders: ChronoField,
    ) {
        NANO(ChronoField.NANO_OF_SECOND, ChronoUnit.SECONDS),
        SECOND(ChronoField.SECOND_OF_MINUTE, ChronoUnit.MINUTES, ChronoField.NANO_OF_SECOND),
        MINUTE(ChronoField.MINUTE_OF_HOUR, ChronoUnit.HOURS, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND),
        HOUR(
            ChronoField.HOUR_OF_DAY,
            ChronoUnit.DAYS,
            ChronoField.MINUTE_OF_HOUR,
            ChronoField.SECOND_OF_MINUTE,
            ChronoField.NANO_OF_SECOND,
        ),
        DAY_OF_MONTH(
            ChronoField.DAY_OF_MONTH,
            ChronoUnit.MONTHS,
            ChronoField.HOUR_OF_DAY,
            ChronoField.MINUTE_OF_HOUR,
            ChronoField.SECOND_OF_MINUTE,
            ChronoField.NANO_OF_SECOND,
        ),
        MONTH(
            ChronoField.MONTH_OF_YEAR,
            ChronoUnit.YEARS,
            ChronoField.DAY_OF_MONTH,
            ChronoField.HOUR_OF_DAY,
            ChronoField.MINUTE_OF_HOUR,
            ChronoField.SECOND_OF_MINUTE,
            ChronoField.NANO_OF_SECOND,
        ),
        DAY_OF_WEEK(
            ChronoField.DAY_OF_WEEK,
            ChronoUnit.WEEKS,
            ChronoField.HOUR_OF_DAY,
            ChronoField.MINUTE_OF_HOUR,
            ChronoField.SECOND_OF_MINUTE,
            ChronoField.NANO_OF_SECOND,
        );

        private val lowerOrders: Array<ChronoField> = arrayOf(*lowerOrders)

        operator fun get(date: Temporal): Int = date[field]

        fun range(): ValueRange = field.range()

        fun checkValidValue(value: Int): Int {
            if (this == DAY_OF_WEEK && value == 0) {
                return 0
            }
            return try {
                field.checkValidIntValue(value.toLong())
            } catch (ex: DateTimeException) {
                throw IllegalArgumentException(ex.message, ex)
            }
        }

        fun elapseUntil(temporal: Temporal, goal: Int): Temporal {
            val current = get(temporal)
            val range = temporal.range(field)
            return if (current < goal) {
                if (range.isValidIntValue(goal.toLong())) {
                    temporal.with(field, goal.toLong())
                } else {
                    // Goal is invalid for this date, e.g. 29th of February in a non-leap year.
                    val amount = range.maximum - current + 1
                    field.baseUnit.addTo(temporal, amount)
                }
            } else {
                val amount = goal + range.maximum - current + 1 - range.minimum
                field.baseUnit.addTo(temporal, amount)
            }
        }

        fun rollForward(temporal: Temporal): Temporal {
            val result = higherOrder.addTo(temporal, 1)
            val range = result.range(field)
            return field.adjustInto(result, range.minimum)
        }

        fun reset(temporal: Temporal): Temporal {
            var candidate = temporal
            for (lowerOrder in lowerOrders) {
                if (candidate.isSupported(lowerOrder)) {
                    candidate = lowerOrder.adjustInto(candidate, candidate.range(lowerOrder).minimum)
                }
            }
            return candidate
        }

        override fun toString(): String = field.toString()
    }

    companion object {
        private val MONTHS = arrayOf(
            "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC",
        )
        private val DAYS = arrayOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

        fun zeroNanos(): CronField = BitsCronField.zeroNanos()

        fun parseSeconds(value: String): CronField = BitsCronField.parseSeconds(value)

        fun parseMinutes(value: String): CronField = BitsCronField.parseMinutes(value)

        fun parseHours(value: String): CronField = BitsCronField.parseHours(value)

        fun parseDaysOfMonth(value: String): CronField {
            if (!isQuartzDaysOfMonthField(value)) {
                return BitsCronField.parseDaysOfMonth(value)
            }
            return parseList(value, Type.DAY_OF_MONTH) { segment ->
                if (isQuartzDaysOfMonthField(segment)) {
                    QuartzCronField.parseDaysOfMonth(segment)
                } else {
                    BitsCronField.parseDaysOfMonth(segment)
                }
            }
        }

        fun parseMonth(value: String): CronField = BitsCronField.parseMonth(replaceOrdinals(value, MONTHS))

        fun parseDaysOfWeek(value: String): CronField {
            val normalized = replaceOrdinals(value, DAYS)
            if (!isQuartzDaysOfWeekField(normalized)) {
                return BitsCronField.parseDaysOfWeek(normalized)
            }
            return parseList(normalized, Type.DAY_OF_WEEK) { segment ->
                if (isQuartzDaysOfWeekField(segment)) {
                    QuartzCronField.parseDaysOfWeek(segment)
                } else {
                    BitsCronField.parseDaysOfWeek(segment)
                }
            }
        }

        private fun parseList(value: String, type: Type, parseField: (String) -> CronField): CronField {
            val fields = ArrayList<CronField>(4)
            var start = 0
            while (start <= value.length) {
                val end = value.indexOf(',', start).let { if (it == -1) value.length else it }
                require(end > start) { "Empty cron field segment '$value'" }
                fields.add(parseField(value.substring(start, end)))
                if (end == value.length) {
                    break
                }
                start = end + 1
            }
            return CompositeCronField.compose(fields.toTypedArray(), type, value)
        }

        private fun replaceOrdinals(value: String, names: Array<String>): String {
            var result = value.uppercase(Locale.ROOT)
            for ((index, token) in names.withIndex()) {
                result = result.replace(token, (index + 1).toString())
            }
            return result
        }

        @Suppress("UNCHECKED_CAST")
        fun isBefore(left: Temporal, right: Temporal): Boolean {
            return (left as Comparable<Any>) < (right as Any)
        }
    }
}
