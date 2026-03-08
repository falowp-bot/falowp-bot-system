package com.blr19c.falowp.bot.system.scheduling.cron

import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.temporal.*

/**
 * Extension of [CronField] for Quartz-specific day-of-month/day-of-week syntax.
 */
internal class QuartzCronField private constructor(
    type: Type,
    private val rollForwardType: Type,
    private val adjuster: TemporalAdjuster,
    private val value: String,
) : CronField(type) {

    private constructor(type: Type, adjuster: TemporalAdjuster, value: String) : this(type, type, adjuster, value)

    override fun nextOrSame(temporal: Temporal): Temporal? {
        var seed = temporal
        var result = adjust(seed) ?: return null
        if (isBefore(result, seed)) {
            // Adjuster landed before the seed date, move to next higher-order unit and retry.
            seed = rollForwardType.rollForward(seed)
            result = adjust(seed) ?: return null
            result = type().reset(result)
        }
        return result
    }

    private fun adjust(temporal: Temporal): Temporal? = adjuster.adjustInto(temporal)

    override fun hashCode(): Int = value.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        return other is QuartzCronField && type() == other.type() && value == other.value
    }

    override fun toString(): String = "${type()} '$value'"

    companion object {
        fun isQuartzDaysOfMonthField(value: String): Boolean = value.contains('L') || value.contains('W')

        fun parseDaysOfMonth(value: String): QuartzCronField {
            val lIndex = value.lastIndexOf('L')
            if (lIndex != -1) {
                val adjuster = when {
                    lIndex != 0 -> throw IllegalArgumentException("Unrecognized characters before 'L' in '$value'")
                    value == "LW" -> lastWeekdayOfMonth()
                    value == "L" -> lastDayOfMonth()
                    else -> {
                        val offset = value.substring(1).toInt()
                        require(offset < 0) { "Offset '$offset should be < 0 '$value'" }
                        lastDayWithOffset(offset)
                    }
                }
                return QuartzCronField(Type.DAY_OF_MONTH, adjuster, value)
            }

            val wIndex = value.lastIndexOf('W')
            if (wIndex == -1) {
                throw IllegalArgumentException("No 'L' or 'W' found in '$value'")
            }
            if (wIndex == 0) {
                throw IllegalArgumentException("No day-of-month before 'W' in '$value'")
            }
            if (wIndex != value.length - 1) {
                throw IllegalArgumentException("Unrecognized characters after 'W' in '$value'")
            }

            var dayOfMonth = value.substring(0, wIndex).toInt()
            dayOfMonth = Type.DAY_OF_MONTH.checkValidValue(dayOfMonth)
            return QuartzCronField(Type.DAY_OF_MONTH, weekdayNearestTo(dayOfMonth), value)
        }

        fun isQuartzDaysOfWeekField(value: String): Boolean = value.contains('L') || value.contains('#')

        fun parseDaysOfWeek(value: String): QuartzCronField {
            val lIndex = value.lastIndexOf('L')
            if (lIndex != -1) {
                if (lIndex != value.length - 1) {
                    throw IllegalArgumentException("Unrecognized characters after 'L' in '$value'")
                }
                if (lIndex == 0) {
                    throw IllegalArgumentException("No day-of-week before 'L' in '$value'")
                }
                val dayOfWeek = parseDayOfWeek(value.substring(0, lIndex))
                return QuartzCronField(Type.DAY_OF_WEEK, Type.DAY_OF_MONTH, lastInMonth(dayOfWeek), value)
            }

            val hashIndex = value.lastIndexOf('#')
            if (hashIndex == -1) {
                throw IllegalArgumentException("No 'L' or '#' found in '$value'")
            }
            require(hashIndex != 0) { "No day-of-week before '#' in '$value'" }
            require(hashIndex != value.length - 1) { "No ordinal after '#' in '$value'" }

            val dayOfWeek = parseDayOfWeek(value.substring(0, hashIndex))
            val ordinal = value.substring(hashIndex + 1).toInt()
            require(ordinal > 0) { "Ordinal '$ordinal' in '$value' must be positive number" }
            return QuartzCronField(Type.DAY_OF_WEEK, Type.DAY_OF_MONTH, dayOfWeekInMonth(ordinal, dayOfWeek), value)
        }

        private fun parseDayOfWeek(value: String): DayOfWeek {
            var dayOfWeek = value.toInt()
            if (dayOfWeek == 0) {
                dayOfWeek = 7
            }
            return try {
                DayOfWeek.of(dayOfWeek)
            } catch (ex: DateTimeException) {
                throw IllegalArgumentException("${ex.message} '$value'", ex)
            }
        }

        private fun atMidnight(): TemporalAdjuster = TemporalAdjuster { temporal ->
            if (temporal.isSupported(ChronoField.NANO_OF_DAY)) {
                temporal.with(ChronoField.NANO_OF_DAY, 0)
            } else {
                temporal
            }
        }

        private fun lastDayOfMonth(): TemporalAdjuster {
            val adjuster = TemporalAdjusters.lastDayOfMonth()
            return TemporalAdjuster { temporal -> rollbackToMidnight(temporal, adjuster.adjustInto(temporal)) }
        }

        private fun lastWeekdayOfMonth(): TemporalAdjuster {
            val adjuster = TemporalAdjusters.lastDayOfMonth()
            return TemporalAdjuster { temporal ->
                val lastDom = adjuster.adjustInto(temporal)
                val result = when (lastDom[ChronoField.DAY_OF_WEEK]) {
                    6 -> lastDom.minus(1, ChronoUnit.DAYS)
                    7 -> lastDom.minus(2, ChronoUnit.DAYS)
                    else -> lastDom
                }
                rollbackToMidnight(temporal, result)
            }
        }

        private fun lastDayWithOffset(offset: Int): TemporalAdjuster {
            val adjuster = TemporalAdjusters.lastDayOfMonth()
            return TemporalAdjuster { temporal ->
                rollbackToMidnight(temporal, adjuster.adjustInto(temporal).plus(offset.toLong(), ChronoUnit.DAYS))
            }
        }

        private fun weekdayNearestTo(dayOfMonth: Int): TemporalAdjuster {
            return TemporalAdjuster { temporal ->
                var candidate = temporal
                var current = Type.DAY_OF_MONTH[candidate]
                var dayOfWeek = DayOfWeek.from(candidate)

                if ((current == dayOfMonth && isWeekday(dayOfWeek)) ||
                    (dayOfWeek == DayOfWeek.FRIDAY && current == dayOfMonth - 1) ||
                    (dayOfWeek == DayOfWeek.MONDAY && current == dayOfMonth + 1) ||
                    (dayOfWeek == DayOfWeek.MONDAY && dayOfMonth == 1 && current == 3)
                ) {
                    return@TemporalAdjuster candidate
                }

                var attempts = 0
                while (attempts++ < CronExpression.MAX_ATTEMPTS) {
                    if (current == dayOfMonth) {
                        dayOfWeek = DayOfWeek.from(candidate)
                        candidate = when (dayOfWeek) {
                            DayOfWeek.SATURDAY -> if (dayOfMonth != 1) {
                                candidate.minus(1, ChronoUnit.DAYS)
                            } else {
                                // Special case for 1W: move to Monday 3rd when the 1st is Saturday.
                                candidate.plus(2, ChronoUnit.DAYS)
                            }

                            DayOfWeek.SUNDAY -> candidate.plus(1, ChronoUnit.DAYS)
                            else -> candidate
                        }
                        return@TemporalAdjuster atMidnight().adjustInto(candidate)
                    }

                    candidate = Type.DAY_OF_MONTH.elapseUntil(candidate, dayOfMonth)
                    current = Type.DAY_OF_MONTH[candidate]
                }
                null
            }
        }

        private fun isWeekday(dayOfWeek: DayOfWeek): Boolean =
            dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY

        private fun lastInMonth(dayOfWeek: DayOfWeek): TemporalAdjuster {
            val adjuster = TemporalAdjusters.lastInMonth(dayOfWeek)
            return TemporalAdjuster { temporal -> rollbackToMidnight(temporal, adjuster.adjustInto(temporal)) }
        }

        private fun dayOfWeekInMonth(ordinal: Int, dayOfWeek: DayOfWeek): TemporalAdjuster {
            val adjuster = TemporalAdjusters.dayOfWeekInMonth(ordinal, dayOfWeek)
            return TemporalAdjuster { temporal -> rollbackToMidnight(temporal, adjuster.adjustInto(temporal)) }
        }

        private fun rollbackToMidnight(current: Temporal, result: Temporal): Temporal {
            return if (result[ChronoField.DAY_OF_MONTH] == current[ChronoField.DAY_OF_MONTH]) {
                current
            } else {
                atMidnight().adjustInto(result)
            }
        }
    }
}
