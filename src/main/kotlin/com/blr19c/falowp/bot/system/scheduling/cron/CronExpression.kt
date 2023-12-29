package com.blr19c.falowp.bot.system.scheduling.cron

import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal

@Suppress("UNCHECKED_CAST", "MemberVisibilityCanBePrivate", "UNUSED")
class CronExpression private constructor(
    seconds: CronField,
    minutes: CronField,
    hours: CronField,
    daysOfMonth: CronField,
    months: CronField,
    daysOfWeek: CronField,
    private val expression: String
) {
    private val fields: Array<CronField>

    init {

        // reverse order, to make big changes first
        // to make sure we end up at 0 nanos, we add an extra field
        fields = arrayOf(daysOfWeek, months, daysOfMonth, hours, minutes, seconds, CronField.zeroNanos())
    }

    /**
     * Calculate the next [Temporal] that matches this expression.
     *
     * @param temporal the seed value
     * @param <T>      the type of temporal
     * @return the next temporal that matches this expression, or `null`
     * if no such temporal can be found
    </T> */

    fun <T> next(temporal: T): T? where T : Temporal? {
        return nextOrSame(ChronoUnit.NANOS.addTo(temporal, 1))
    }


    private fun <T> nextOrSame(temp: T): T? where T : Temporal? {
        var temporal = temp
        for (i in 0 until MAX_ATTEMPTS) {
            val result = nextOrSameInternal(temporal) as Temporal
            if (result == temporal) {
                return result as T?
            }
            temporal = result as T
        }
        return null
    }


    private fun <T> nextOrSameInternal(temp: T): T? where T : Temporal? {
        var temporal: T? = temp
        for (field in fields) {
            temporal = field.nextOrSame(temporal)
            if (temporal == null) {
                return null
            }
        }
        return temporal
    }

    override fun hashCode(): Int {
        return fields.contentHashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        return if (other is CronExpression) {
            fields.contentEquals(other.fields)
        } else {
            false
        }
    }

    /**
     * Return the expression string used to create this `CronExpression`.
     *
     * @return the expression string
     */
    override fun toString(): String {
        return expression
    }

    companion object {
        const val MAX_ATTEMPTS = 366
        private val MACROS = arrayOf(
            "@yearly", "0 0 0 1 1 *",
            "@annually", "0 0 0 1 1 *",
            "@monthly", "0 0 0 1 * *",
            "@weekly", "0 0 0 * * 0",
            "@daily", "0 0 0 * * *",
            "@midnight", "0 0 0 * * *",
            "@hourly", "0 0 * * * *"
        )

        /**
         * Parse the given
         * [crontab expression](https://www.manpagez.com/man/5/crontab/)
         * string into a `CronExpression`.
         * The string has six single space-separated time and date fields:
         * <pre>
         * &#9484;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472; second (0-59)
         * &#9474; &#9484;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472; minute (0 - 59)
         * &#9474; &#9474; &#9484;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472; hour (0 - 23)
         * &#9474; &#9474; &#9474; &#9484;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472; day of the month (1 - 31)
         * &#9474; &#9474; &#9474; &#9474; &#9484;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472; month (1 - 12) (or JAN-DEC)
         * &#9474; &#9474; &#9474; &#9474; &#9474; &#9484;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472; day of the week (0 - 7)
         * &#9474; &#9474; &#9474; &#9474; &#9474; &#9474;          (0 or 7 is Sunday, or MON-SUN)
         * &#9474; &#9474; &#9474; &#9474; &#9474; &#9474;
         * &#42; &#42; &#42; &#42; &#42; &#42;
        </pre> *
         *
         *
         * The following rules apply:
         *
         *  *
         * A field may be an asterisk (`*`), which always stands for
         * "first-last". For the "day of the month" or "day of the week" fields, a
         * question mark (`?`) may be used instead of an asterisk.
         *
         *  *
         * Ranges of numbers are expressed by two numbers separated with a hyphen
         * (`-`). The specified range is inclusive.
         *
         *  * Following a range (or `*`) with `/n` specifies
         * the interval of the number's value through the range.
         *
         *  *
         * English names can also be used for the "month" and "day of week" fields.
         * Use the first three letters of the particular day or month (case does not
         * matter).
         *
         *  *
         * The "day of month" and "day of week" fields can contain a
         * `L`-character, which stands for "last", and has a different meaning
         * in each field:
         *
         *  *
         * In the "day of month" field, `L` stands for "the last day of the
         * month". If followed by an negative offset (i.e. `L-n`), it means
         * "`n`th-to-last day of the month". If followed by `W` (i.e.
         * `LW`), it means "the last weekday of the month".
         *
         *  *
         *  *
         * The "day of month" field can be `nW`, which stands for "the nearest
         * weekday to day of the month `n`".
         * If `n` falls on Saturday, this yields the Friday before it.
         * If `n` falls on Sunday, this yields the Monday after,
         * which also happens if `n` is `1` and falls on a Saturday
         * (i.e. `1W` stands for "the first weekday of the month").
         *
         *  *
         * The "day of week" field can be `d#n` (or `DDD#n`), which
         * stands for "the `n`-th day of week `d` (or `DDD`) in
         * the month".
         *
         *
         *
         *
         * Example expressions:
         *
         *  * `"0 0 * * * *"` = the top of every hour of every day.
         *  * `"*&#47;10 * * * * *"` = every ten seconds.
         *  * `"0 0 8-10 * * *"` = 8, 9 and 10 o'clock of every day.
         *  * `"0 0 6,19 * * *"` = 6:00 AM and 7:00 PM every day.
         *  * `"0 0/30 8-10 * * *"` = 8:00, 8:30, 9:00, 9:30, 10:00 and 10:30 every day.
         *  * `"0 0 9-17 * * MON-FRI"` = on the hour nine-to-five weekdays
         *  * `"0 0 0 25 12 ?"` = every Christmas Day at midnight
         *  * `"0 0 0 L * *"` = last day of the month at midnight
         *  * `"0 0 0 L-3 * *"` = third-to-last day of the month at midnight
         *  * `"0 0 0 1W * *"` = first weekday of the month at midnight
         *  * `"0 0 0 LW * *"` = last weekday of the month at midnight
         *  * `"0 0 0 * * 5L"` = last Friday of the month at midnight
         *  * `"0 0 0 * * TH UL"` = last Thursday of the month at midnight
         *  * `"0 0 0 ? * 5#2"` = the second Friday in the month at midnight
         *  * `"0 0 0 ? * MON#1"` = the first Monday in the month at midnight
         *
         *
         *
         * The following macros are also supported:
         *
         *  * `"@yearly"` (or `"@annually"`) to run un once a year, i.e. `"0 0 0 1 1 *"`,
         *  * `"@monthly"` to run once a month, i.e. `"0 0 0 1 * *"`,
         *  * `"@weekly"` to run once a week, i.e. `"0 0 0 * * 0"`,
         *  * `"@daily"` (or `"@midnight"`) to run once a day, i.e. `"0 0 0 * * *"`,
         *  * `"@hourly"` to run once an hour, i.e. `"0 0 * * * *"`.
         *
         *
         * @param exp the expression string to parse
         * @return the parsed `CronExpression` object
         * @throws IllegalArgumentException in the expression does not conform to
         * the cron format
         */
        fun parse(exp: String): CronExpression {
            var expression = exp
            expression = resolveMacros(expression)
            val fields: Array<String> = expression.split(" ").toTypedArray()
            require(fields.size == 6) {
                String.format(
                    "Cron expression must consist of 6 fields (found %d in \"%s\")", fields.size, expression
                )
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
                val msg = ex.message + " in cron expression \"" + expression + "\""
                throw IllegalArgumentException(msg, ex)
            }
        }

        /**
         * Determine whether the given string represents a valid cron expression.
         *
         * @param expression the expression to evaluate
         * @return `true` if the given expression is a valid cron expression
         * @since 5.3.8
         */
        fun isValidExpression(expression: String?): Boolean {
            return if (expression == null) {
                false
            } else try {
                parse(expression)
                true
            } catch (ex: IllegalArgumentException) {
                false
            }
        }

        private fun resolveMacros(exp: String): String {
            var expression = exp
            expression = expression.trim { it <= ' ' }
            var i = 0
            while (i < MACROS.size) {
                if (MACROS[i].equals(expression, ignoreCase = true)) {
                    return MACROS[i + 1]
                }
                i += 2
            }
            return expression
        }
    }
}
