package com.blr19c.falowp.bot.system.scheduling.cron

import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.temporal.*

/**
 * Extension of [CronField] for
 */
@Suppress("UNCHECKED_CAST", "MemberVisibilityCanBePrivate", "UNUSED")
internal class QuartzCronField
/**
 * Constructor for fields that need to roll forward over a different type
 * than the type this field represents. See [.parseDaysOfWeek].
 */ private constructor(
    type: Type,
    private val rollForwardType: Type,
    private val adjuster: TemporalAdjuster,
    private val value: String
) : CronField(type) {
    private constructor(type: Type, adjuster: TemporalAdjuster, value: String) : this(type, type, adjuster, value)

    override fun <T> nextOrSame(temporal: T): T where T : Temporal? {
        var temp = temporal
        var result = adjust(temp) as Comparable<T>?
        if (result != null) {
            if (result < temp) {
                // We ended up before the start, roll forward and try again
                temp = rollForwardType.rollForward(temp)
                result = adjust(temp) as Comparable<T>?
                if (result != null) {
                    result = type().reset(result as T?) as Comparable<T>?
                }
            }
        }
        return result as T
    }

    private fun <T> adjust(temporal: T): T where T : Temporal? {
        return adjuster.adjustInto(temporal) as T
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        return if (other !is QuartzCronField) {
            false
        } else type() == other.type() && value == other.value
    }

    override fun toString(): String {
        return type().toString() + " '" + value + "'"
    }

    companion object {
        /**
         * Returns whether the given value is a Quartz day-of-month field.
         */
        @JvmStatic
        fun isQuartzDaysOfMonthField(value: String): Boolean {
            return value.contains("L") || value.contains("W")
        }

        /**
         * Parse the given value into a days of months `QuartzCronField`, the fourth entry of a cron expression.
         * Expects a "L" or "W" in the given value.
         */
        @JvmStatic
        fun parseDaysOfMonth(value: String): QuartzCronField {
            var idx = value.lastIndexOf('L')
            if (idx != -1) {
                val adjuster: TemporalAdjuster = if (idx != 0) {
                    throw IllegalArgumentException("Unrecognized characters before 'L' in '$value'")
                } else if (value.length == 2 && value[1] == 'W') { // "LW"
                    lastWeekdayOfMonth()
                } else {
                    if (value.length == 1) { // "L"
                        lastDayOfMonth()
                    } else { // "L-[0-9]+"
                        val offset = Integer.parseInt(value, 1, value.length, 10)
                        require(offset < 0) { "Offset '$offset should be < 0 '$value'" }
                        lastDayWithOffset(offset)
                    }
                }
                return QuartzCronField(Type.DAY_OF_MONTH, adjuster, value)
            }
            idx = value.lastIndexOf('W')
            if (idx != -1) {
                return if (idx == 0) {
                    throw IllegalArgumentException("No day-of-month before 'W' in '$value'")
                } else if (idx != value.length - 1) {
                    throw IllegalArgumentException("Unrecognized characters after 'W' in '$value'")
                } else { // "[0-9]+W"
                    var dayOfMonth = Integer.parseInt(value, 0, idx, 10)
                    dayOfMonth = Type.DAY_OF_MONTH.checkValidValue(dayOfMonth)
                    val adjuster = weekdayNearestTo(dayOfMonth)
                    QuartzCronField(Type.DAY_OF_MONTH, adjuster, value)
                }
            }
            throw IllegalArgumentException("No 'L' or 'W' found in '$value'")
        }

        /**
         * Returns whether the given value is a Quartz day-of-week field.
         */
        @JvmStatic
        fun isQuartzDaysOfWeekField(value: String): Boolean {
            return value.contains("L") || value.contains("#")
        }

        /**
         * Parse the given value into a days of week `QuartzCronField`, the sixth entry of a cron expression.
         * Expects a "L" or "#" in the given value.
         */
        @JvmStatic
        fun parseDaysOfWeek(value: String): QuartzCronField {
            var idx = value.lastIndexOf('L')
            if (idx != -1) {
                return if (idx != value.length - 1) {
                    throw IllegalArgumentException("Unrecognized characters after 'L' in '$value'")
                } else {
                    val adjuster: TemporalAdjuster = if (idx == 0) {
                        throw IllegalArgumentException("No day-of-week before 'L' in '$value'")
                    } else { // "[0-7]L"
                        val dayOfWeek = parseDayOfWeek(value.substring(0, idx))
                        lastInMonth(dayOfWeek)
                    }
                    QuartzCronField(Type.DAY_OF_WEEK, Type.DAY_OF_MONTH, adjuster, value)
                }
            }
            idx = value.lastIndexOf('#')
            if (idx != -1) {
                require(idx != 0) { "No day-of-week before '#' in '$value'" }
                require(idx != value.length - 1) { "No ordinal after '#' in '$value'" }
                // "[0-7]#[0-9]+"
                val dayOfWeek = parseDayOfWeek(value.substring(0, idx))
                val ordinal = Integer.parseInt(value, idx + 1, value.length, 10)
                require(ordinal > 0) {
                    "Ordinal '" + ordinal + "' in '" + value +
                            "' must be positive number "
                }
                val adjuster = dayOfWeekInMonth(ordinal, dayOfWeek)
                return QuartzCronField(Type.DAY_OF_WEEK, Type.DAY_OF_MONTH, adjuster, value)
            }
            throw IllegalArgumentException("No 'L' or '#' found in '$value'")
        }

        private fun parseDayOfWeek(value: String): DayOfWeek {
            var dayOfWeek = value.toInt()
            if (dayOfWeek == 0) {
                dayOfWeek = 7 // cron is 0 based; java.time 1 based
            }
            return try {
                DayOfWeek.of(dayOfWeek)
            } catch (ex: DateTimeException) {
                val msg = ex.message + " '" + value + "'"
                throw IllegalArgumentException(msg, ex)
            }
        }

        /**
         * Returns an adjuster that resets to midnight.
         */
        private fun atMidnight(): TemporalAdjuster {
            return TemporalAdjuster { temporal: Temporal ->
                if (temporal.isSupported(ChronoField.NANO_OF_DAY)) {
                    return@TemporalAdjuster temporal.with(ChronoField.NANO_OF_DAY, 0)
                } else {
                    return@TemporalAdjuster temporal
                }
            }
        }

        /**
         * Returns an adjuster that returns a new temporal set to the last
         * day of the current month at midnight.
         */
        private fun lastDayOfMonth(): TemporalAdjuster {
            val adjuster = TemporalAdjusters.lastDayOfMonth()
            return TemporalAdjuster { temporal: Temporal ->
                val result = adjuster.adjustInto(temporal)
                rollbackToMidnight(temporal, result)
            }
        }

        /**
         * Returns an adjuster that returns the last weekday of the month.
         */
        private fun lastWeekdayOfMonth(): TemporalAdjuster {
            val adjuster = TemporalAdjusters.lastDayOfMonth()
            return TemporalAdjuster { temporal: Temporal ->
                val lastDom = adjuster.adjustInto(temporal)
                val result: Temporal
                val dow = lastDom[ChronoField.DAY_OF_WEEK]
                result = when (dow) {
                    6 -> { // Saturday
                        lastDom.minus(1, ChronoUnit.DAYS)
                    }

                    7 -> { // Sunday
                        lastDom.minus(2, ChronoUnit.DAYS)
                    }

                    else -> {
                        lastDom
                    }
                }
                rollbackToMidnight(temporal, result)
            }
        }

        /**
         * Return a temporal adjuster that finds the nth-to-last day of the month.
         * @param offset the negative offset, i.e. -3 means third-to-last
         * @return a nth-to-last day-of-month adjuster
         */
        private fun lastDayWithOffset(offset: Int): TemporalAdjuster {
            val adjuster = TemporalAdjusters.lastDayOfMonth()
            return TemporalAdjuster { temporal: Temporal ->
                val result = adjuster.adjustInto(temporal).plus(offset.toLong(), ChronoUnit.DAYS)
                rollbackToMidnight(temporal, result)
            }
        }

        /**
         * Return a temporal adjuster that finds the weekday nearest to the given
         * day-of-month. If `dayOfMonth` falls on a Saturday, the date is
         * moved back to Friday; if it falls on a Sunday (or if `dayOfMonth`
         * is 1, and it falls on a Saturday), it is moved forward to Monday.
         * @param dayOfMonth the goal day-of-month
         * @return the weekday-nearest-to adjuster
         */
        private fun weekdayNearestTo(dayOfMonth: Int): TemporalAdjuster {
            return TemporalAdjuster { temp: Temporal ->
                var temporal = temp
                var current = Type.DAY_OF_MONTH[temporal]
                var dayOfWeek = DayOfWeek.from(temporal)
                if (current == dayOfMonth && isWeekday(dayOfWeek) || dayOfWeek == DayOfWeek.FRIDAY && current == dayOfMonth - 1 || dayOfWeek == DayOfWeek.MONDAY && current == dayOfMonth + 1 || dayOfWeek == DayOfWeek.MONDAY && dayOfMonth == 1 && current == 3) { // dayOfMonth is Saturday 1st, so Monday 3rd
                    return@TemporalAdjuster temporal
                }
                var count = 0
                while (count++ < CronExpression.MAX_ATTEMPTS) {
                    if (current == dayOfMonth) {
                        dayOfWeek = DayOfWeek.from(temporal)
                        if (dayOfWeek == DayOfWeek.SATURDAY) {
                            temporal = if (dayOfMonth != 1) {
                                temporal.minus(1, ChronoUnit.DAYS)
                            } else {
                                // exception for "1W" fields: execute on next Monday
                                temporal.plus(2, ChronoUnit.DAYS)
                            }
                        } else if (dayOfWeek == DayOfWeek.SUNDAY) {
                            temporal = temporal.plus(1, ChronoUnit.DAYS)
                        }
                        return@TemporalAdjuster atMidnight().adjustInto(temporal)
                    } else {
                        temporal = Type.DAY_OF_MONTH.elapseUntil(cast(temporal), dayOfMonth)
                        current = Type.DAY_OF_MONTH[temporal]
                    }
                }
                null
            }
        }

        private fun isWeekday(dayOfWeek: DayOfWeek): Boolean {
            return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY
        }

        /**
         * Return a temporal adjuster that finds the last of the given doy-of-week
         * in a month.
         */
        private fun lastInMonth(dayOfWeek: DayOfWeek): TemporalAdjuster {
            val adjuster = TemporalAdjusters.lastInMonth(dayOfWeek)
            return TemporalAdjuster { temporal: Temporal ->
                val result = adjuster.adjustInto(temporal)
                rollbackToMidnight(temporal, result)
            }
        }

        /**
         * Returns a temporal adjuster that finds `ordinal`-th occurrence of
         * the given day-of-week in a month.
         */
        private fun dayOfWeekInMonth(ordinal: Int, dayOfWeek: DayOfWeek): TemporalAdjuster {
            val adjuster = TemporalAdjusters.dayOfWeekInMonth(ordinal, dayOfWeek)
            return TemporalAdjuster { temporal: Temporal ->
                val result = adjuster.adjustInto(temporal)
                rollbackToMidnight(temporal, result)
            }
        }

        /**
         * Rolls back the given `result` to midnight. When
         * `current` has the same day of month as `result`, the former
         * is returned, to make sure that we don't end up before where we started.
         */
        private fun rollbackToMidnight(current: Temporal, result: Temporal): Temporal {
            return if (result[ChronoField.DAY_OF_MONTH] == current[ChronoField.DAY_OF_MONTH]) {
                current
            } else {
                atMidnight().adjustInto(result)
            }
        }
    }
}
