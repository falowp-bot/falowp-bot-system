package com.blr19c.falowp.bot.system.scheduling.cron

import com.blr19c.falowp.bot.system.utils.StringUtils
import java.time.DateTimeException
import java.time.temporal.Temporal
import java.time.temporal.ValueRange


internal class BitsCronField private constructor(type: Type) : CronField(type) {
    // we store at most 60 bits, for seconds and minutes, so a 64-bit long suffices
    private var bits: Long = 0
    override fun <T> nextOrSame(temporal: T): T? where T : Temporal? {
        var temp = temporal
        var current = type()[temp as Temporal]
        var next = nextSetBit(current)
        if (next == -1) {
            temp = type().rollForward(temp)
            next = nextSetBit(0)
        }
        return if (next == current) {
            temp
        } else {
            var count = 0
            current = type()[temp]
            while (current != next && count++ < CronExpression.MAX_ATTEMPTS) {
                temp = type().elapseUntil(temp, next)
                current = type()[temp as Temporal]
                next = nextSetBit(current)
                if (next == -1) {
                    temp = type().rollForward(temp)
                    next = nextSetBit(0)
                }
            }
            if (count >= CronExpression.MAX_ATTEMPTS) {
                null
            } else type().reset(temp)
        }
    }

    fun getBit(index: Int): Boolean {
        return bits and (1L shl index) != 0L
    }

    private fun nextSetBit(fromIndex: Int): Int {
        val result = bits and (MASK shl fromIndex)
        return if (result != 0L) {
            java.lang.Long.numberOfTrailingZeros(result)
        } else {
            -1
        }
    }

    private fun setBits(range: ValueRange) {
        if (range.minimum == range.maximum) {
            setBit(range.minimum.toInt())
        } else {
            val minMask = MASK shl range.minimum.toInt()
            val maxMask = MASK ushr -(range.maximum + 1).toInt()
            bits = bits or (minMask and maxMask)
        }
    }

    private fun setBits(range: ValueRange, delta: Int) {
        if (delta == 1) {
            setBits(range)
        } else {
            var i = range.minimum.toInt()
            while (i <= range.maximum) {
                setBit(i)
                i += delta
            }
        }
    }

    private fun setBit(index: Int) {
        bits = bits or (1L shl index)
    }

    private fun clearBit(index: Int = 0) {
        bits = bits and (1L shl index).inv()
    }

    override fun hashCode(): Int {
        return java.lang.Long.hashCode(bits)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        return if (other !is BitsCronField) {
            false
        } else type() == other.type() && bits == other.bits
    }

    override fun toString(): String {
        val builder = StringBuilder(type().toString())
        builder.append(" {")
        var i = nextSetBit(0)
        if (i != -1) {
            builder.append(i)
            i = nextSetBit(i + 1)
            while (i != -1) {
                builder.append(", ")
                builder.append(i)
                i = nextSetBit(i + 1)
            }
        }
        builder.append('}')
        return builder.toString()
    }

    companion object {
        private const val MASK = -0x1L

        private var zeroNanos: BitsCronField? = null

        /**
         * Return a `BitsCronField` enabled for 0 nanoseconds.
         */
        fun zeroNanos(): BitsCronField {
            if (zeroNanos == null) {
                val field = BitsCronField(Type.NANO)
                field.setBit(0)
                zeroNanos = field
            }
            return zeroNanos!!
        }

        /**
         * Parse the given value into a seconds `BitsCronField`, the first entry of a cron expression.
         */
        @JvmStatic
        fun parseSeconds(value: String): BitsCronField {
            return parseField(value, Type.SECOND)
        }

        /**
         * Parse the given value into a minutes `BitsCronField`, the second entry of a cron expression.
         */
        @JvmStatic
        fun parseMinutes(value: String): BitsCronField {
            return parseField(value, Type.MINUTE)
        }

        /**
         * Parse the given value into an hours `BitsCronField`, the third entry of a cron expression.
         */
        @JvmStatic
        fun parseHours(value: String): BitsCronField {
            return parseField(value, Type.HOUR)
        }

        /**
         * Parse the given value into a days of months `BitsCronField`, the fourth entry of a cron expression.
         */
        @JvmStatic
        fun parseDaysOfMonth(value: String): BitsCronField {
            return parseDate(value, Type.DAY_OF_MONTH)
        }

        /**
         * Parse the given value into a month `BitsCronField`, the fifth entry of a cron expression.
         */
        @JvmStatic
        fun parseMonth(value: String): BitsCronField {
            return parseField(value, Type.MONTH)
        }

        /**
         * Parse the given value into a days of week `BitsCronField`, the sixth entry of a cron expression.
         */
        @JvmStatic
        fun parseDaysOfWeek(value: String): BitsCronField {
            val result = parseDate(value, Type.DAY_OF_WEEK)
            if (result.getBit(0)) {
                // cron supports 0 for Sunday; we use 7 like java.time
                result.setBit(7)
                result.clearBit()
            }
            return result
        }

        private fun parseDate(v: String, type: Type): BitsCronField {
            var value = v
            if (value == "?") {
                value = "*"
            }
            return parseField(value, type)
        }

        private fun parseField(value: String, type: Type): BitsCronField {
            return try {
                val result = BitsCronField(type)
                val fields: Array<String> = StringUtils.delimitedListToStringArray(value, ",")
                for (field in fields) {
                    val slashPos = field.indexOf('/')
                    if (slashPos == -1) {
                        val range = parseRange(field, type)
                        result.setBits(range)
                    } else {
                        val rangeStr = field.substring(0, slashPos)
                        val deltaStr = field.substring(slashPos + 1)
                        var range = parseRange(rangeStr, type)
                        if (rangeStr.indexOf('-') == -1) {
                            range = ValueRange.of(range.minimum, type.range().maximum)
                        }
                        val delta = deltaStr.toInt()
                        require(delta > 0) { "Incrementer delta must be 1 or higher" }
                        result.setBits(range, delta)
                    }
                }
                result
            } catch (ex: DateTimeException) {
                val msg = ex.message + " '" + value + "'"
                throw IllegalArgumentException(msg, ex)
            } catch (ex: IllegalArgumentException) {
                val msg = ex.message + " '" + value + "'"
                throw IllegalArgumentException(msg, ex)
            }
        }

        private fun parseRange(value: String, type: Type): ValueRange {
            return if (value == "*") {
                type.range()
            } else {
                val hyphenPos = value.indexOf('-')
                if (hyphenPos == -1) {
                    val result = type.checkValidValue(value.toInt())
                    ValueRange.of(result.toLong(), result.toLong())
                } else {
                    var min = Integer.parseInt(value, 0, hyphenPos, 10)
                    var max = Integer.parseInt(value, hyphenPos + 1, value.length, 10)
                    min = type.checkValidValue(min)
                    max = type.checkValidValue(max)
                    if (type == Type.DAY_OF_WEEK && min == 7) {
                        // If used as a minimum in a range, Sunday means 0 (not 7)
                        min = 0
                    }
                    ValueRange.of(min.toLong(), max.toLong())
                }
            }
        }
    }
}