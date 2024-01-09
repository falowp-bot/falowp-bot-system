package com.blr19c.falowp.bot.system.scheduling.cron

import com.blr19c.falowp.bot.system.scheduling.cron.QuartzCronField.Companion.isQuartzDaysOfMonthField
import com.blr19c.falowp.bot.system.scheduling.cron.QuartzCronField.Companion.isQuartzDaysOfWeekField
import java.time.DateTimeException
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import java.time.temporal.ValueRange
import java.util.*
import java.util.function.BiFunction

/**
 * Single field in a cron pattern. Created using the `parse*` methods,
 * main and only entry point is [.nextOrSame].
 */
@Suppress("UNCHECKED_CAST", "MemberVisibilityCanBePrivate", "UNUSED")
internal abstract class CronField protected constructor(private val type: Type) {
    /**
     * Get the next or same [Temporal] in the sequence matching this
     * cron field.
     *
     * @param temporal the seed value
     * @return the next or same temporal matching the pattern
     */
    abstract fun <T> nextOrSame(temporal: T): T? where T : Temporal?
    protected fun type(): Type {
        return type
    }

    /**
     * Represents the type of cron field, i.e. seconds, minutes, hours,
     * day-of-month, month, day-of-week.
     */
    enum class Type(
        private val field: ChronoField,
        private val higherOrder: ChronoUnit,
        vararg lowerOrders: ChronoField
    ) {
        NANO(ChronoField.NANO_OF_SECOND, ChronoUnit.SECONDS),
        SECOND(ChronoField.SECOND_OF_MINUTE, ChronoUnit.MINUTES, ChronoField.NANO_OF_SECOND),
        MINUTE(ChronoField.MINUTE_OF_HOUR, ChronoUnit.HOURS, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND),
        HOUR(
            ChronoField.HOUR_OF_DAY,
            ChronoUnit.DAYS,
            ChronoField.MINUTE_OF_HOUR,
            ChronoField.SECOND_OF_MINUTE,
            ChronoField.NANO_OF_SECOND
        ),
        DAY_OF_MONTH(
            ChronoField.DAY_OF_MONTH,
            ChronoUnit.MONTHS,
            ChronoField.HOUR_OF_DAY,
            ChronoField.MINUTE_OF_HOUR,
            ChronoField.SECOND_OF_MINUTE,
            ChronoField.NANO_OF_SECOND
        ),
        MONTH(
            ChronoField.MONTH_OF_YEAR,
            ChronoUnit.YEARS,
            ChronoField.DAY_OF_MONTH,
            ChronoField.HOUR_OF_DAY,
            ChronoField.MINUTE_OF_HOUR,
            ChronoField.SECOND_OF_MINUTE,
            ChronoField.NANO_OF_SECOND
        ),
        DAY_OF_WEEK(
            ChronoField.DAY_OF_WEEK,
            ChronoUnit.WEEKS,
            ChronoField.HOUR_OF_DAY,
            ChronoField.MINUTE_OF_HOUR,
            ChronoField.SECOND_OF_MINUTE,
            ChronoField.NANO_OF_SECOND
        );

        private val lowerOrders: Array<ChronoField>

        init {
            this.lowerOrders = arrayOf(* lowerOrders)
        }

        /**
         * Return the value of this type for the given temporal.
         *
         * @return the value of this type
         */
        operator fun get(date: Temporal): Int {
            return date[field]
        }

        /**
         * Return the general range of this type. For instance, this method
         * will return 0-31 for [.MONTH].
         *
         * @return the range of this field
         */
        fun range(): ValueRange {
            return field.range()
        }

        /**
         * Check whether the given value is valid, i.e. whether it falls in
         * [range][.range].
         *
         * @param value the value to check
         * @return the value that was passed in
         * @throws IllegalArgumentException if the given value is invalid
         */
        fun checkValidValue(value: Int): Int {
            return if (this == DAY_OF_WEEK && value == 0) {
                0
            } else {
                try {
                    field.checkValidIntValue(value.toLong())
                } catch (ex: DateTimeException) {
                    throw IllegalArgumentException(ex.message, ex)
                }
            }
        }

        /**
         * Elapse the given temporal for the difference between the current
         * value of this field and the goal value. Typically, the returned
         * temporal will have the given goal as the current value for this type,
         * but this is not the case for [.DAY_OF_MONTH].
         *
         * @param temporal the temporal to elapse
         * @param goal     the goal value
         * @param <T>      the type of temporal
         * @return the elapsed temporal, typically with `goal` as value
         * for this type.
        </T> */
        fun <T> elapseUntil(temporal: T, goal: Int): T where T : Temporal? {
            val current = get(temporal as Temporal)
            val range = temporal.range(field)
            return if (current < goal) {
                if (range.isValidIntValue(goal.toLong())) {
                    cast(temporal.with(field, goal.toLong()))
                } else {
                    // goal is invalid, eg. 29th Feb, so roll forward
                    val amount = range.maximum - current + 1
                    field.baseUnit.addTo(temporal, amount)
                }
            } else {
                val amount = goal + range.maximum - current + 1 - range.minimum
                field.baseUnit.addTo(temporal, amount)
            }
        }

        /**
         * Roll forward the give temporal until it reaches the next higher
         * order field. Calling this method is equivalent to calling
         * [.elapseUntil] with goal set to the
         * minimum value of this field's range.
         *
         * @param temporal the temporal to roll forward
         * @param <T>      the type of temporal
         * @return the rolled forward temporal
        </T> */
        fun <T> rollForward(temporal: T): T where T : Temporal? {
            val result = higherOrder.addTo(temporal, 1)
            val range = result!!.range(field)
            return field.adjustInto(result, range.minimum)
        }

        /**
         * Reset this and all lower order fields of the given temporal to their
         * minimum value. For instance for [.MINUTE], this method
         * resets nanos, seconds, **and** minutes to 0.
         *
         * @param temp the temporal to reset
         * @param <T>      the type of temporal
         * @return the reset temporal
        </T> */
        fun <T : Temporal?> reset(temp: T): T {
            var temporal = temp
            for (lowerOrder in lowerOrders) {
                if (temporal!!.isSupported(lowerOrder)) {
                    temporal = lowerOrder.adjustInto(temporal, temporal.range(lowerOrder).minimum)
                }
            }
            return temporal
        }

        override fun toString(): String {
            return field.toString()
        }
    }

    companion object {
        private val MONTHS = arrayOf(
            "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP",
            "OCT", "NOV", "DEC"
        )
        private val DAYS = arrayOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

        /**
         * Return a `CronField` enabled for 0 nanoseconds.
         */
        fun zeroNanos(): CronField {
            return BitsCronField.zeroNanos()
        }

        /**
         * Parse the given value into a seconds `CronField`, the first entry of a cron expression.
         */
        fun parseSeconds(value: String?): CronField {
            return BitsCronField.parseSeconds(value!!)
        }

        /**
         * Parse the given value into a minutes `CronField`, the second entry of a cron expression.
         */
        fun parseMinutes(value: String?): CronField {
            return BitsCronField.parseMinutes(value!!)
        }

        /**
         * Parse the given value into an hours `CronField`, the third entry of a cron expression.
         */
        fun parseHours(value: String?): CronField {
            return BitsCronField.parseHours(value!!)
        }

        /**
         * Parse the given value into a days of months `CronField`, the fourth entry of a cron expression.
         */
        fun parseDaysOfMonth(value: String): CronField {
            return if (!isQuartzDaysOfMonthField(value)) {
                BitsCronField.parseDaysOfMonth(value)
            } else {
                parseList(value, Type.DAY_OF_MONTH) { field: String?, _: Type? ->
                    if (isQuartzDaysOfMonthField(
                            field!!
                        )
                    ) {
                        return@parseList QuartzCronField.parseDaysOfMonth(field)
                    } else {
                        return@parseList BitsCronField.parseDaysOfMonth(field)
                    }
                }
            }
        }

        /**
         * Parse the given value into a month `CronField`, the fifth entry of a cron expression.
         */
        fun parseMonth(v: String): CronField {
            var value = v
            value = replaceOrdinals(value, MONTHS)
            return BitsCronField.parseMonth(value)
        }

        /**
         * Parse the given value into a days of week `CronField`, the sixth entry of a cron expression.
         */
        fun parseDaysOfWeek(v: String): CronField {
            var value = v
            value = replaceOrdinals(value, DAYS)
            return if (!isQuartzDaysOfWeekField(value)) {
                BitsCronField.parseDaysOfWeek(value)
            } else {
                parseList(value, Type.DAY_OF_WEEK) { field: String?, _: Type? ->
                    if (isQuartzDaysOfWeekField(
                            field!!
                        )
                    ) {
                        return@parseList QuartzCronField.parseDaysOfWeek(field)
                    } else {
                        return@parseList BitsCronField.parseDaysOfWeek(field)
                    }
                }
            }
        }

        private fun parseList(
            value: String,
            type: Type,
            parseFieldFunction: BiFunction<String, Type, CronField>
        ): CronField {
            val fields: Array<String> = value.split(",").toTypedArray()
            val cronFields = arrayOfNulls<CronField>(fields.size)
            for (i in fields.indices) {
                cronFields[i] = parseFieldFunction.apply(fields[i], type)
            }
            return CompositeCronField.compose(cronFields.filterNotNull().toTypedArray(), type, value)
        }

        private fun replaceOrdinals(v: String, list: Array<String>): String {
            var value = v
            value = value.uppercase(Locale.getDefault())
            for (i in list.indices) {
                val replacement = (i + 1).toString()
                value = value.replace(list[i], replacement)
            }
            return value
        }

        fun <T> cast(temporal: Temporal): T where T : Temporal? {
            return temporal as T
        }
    }
}
