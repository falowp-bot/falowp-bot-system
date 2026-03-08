package com.blr19c.falowp.bot.system.scheduling.cron

import java.time.DateTimeException
import java.time.temporal.Temporal
import java.time.temporal.ValueRange

internal class BitsCronField private constructor(
    type: Type,
    private val bits: Long,
) : CronField(type) {

    override fun nextOrSame(temporal: Temporal): Temporal? {
        var candidate = temporal
        var current = type()[candidate]
        var next = nextSetBit(current)
        if (next == -1) {
            candidate = type().rollForward(candidate)
            next = nextSetBit(0)
        }
        if (next == current) {
            return candidate
        }

        var attempts = 0
        while (current != next && attempts++ < CronExpression.MAX_ATTEMPTS) {
            candidate = type().elapseUntil(candidate, next)
            current = type()[candidate]
            next = nextSetBit(current)
            if (next == -1) {
                candidate = type().rollForward(candidate)
                next = nextSetBit(0)
            }
        }
        if (attempts >= CronExpression.MAX_ATTEMPTS) {
            return null
        }
        return type().reset(candidate)
    }

    private fun getBit(index: Int): Boolean = bits and (1L shl index) != 0L

    private fun nextSetBit(fromIndex: Int): Int {
        val result = bits and (MASK shl fromIndex)
        return if (result != 0L) java.lang.Long.numberOfTrailingZeros(result) else -1
    }

    override fun hashCode(): Int = java.lang.Long.hashCode(bits)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        return other is BitsCronField && type() == other.type() && bits == other.bits
    }

    override fun toString(): String {
        val builder = StringBuilder(type().toString()).append(" {")
        var i = nextSetBit(0)
        var first = true
        while (i != -1) {
            if (!first) {
                builder.append(", ")
            }
            builder.append(i)
            first = false
            i = nextSetBit(i + 1)
        }
        return builder.append('}').toString()
    }

    companion object {
        private const val MASK = -0x1L
        private const val BIT_0 = 1L

        private val ZERO_NANOS: BitsCronField by lazy(LazyThreadSafetyMode.PUBLICATION) {
            BitsCronField(Type.NANO, BIT_0)
        }

        fun zeroNanos(): BitsCronField = ZERO_NANOS

        fun parseSeconds(value: String): BitsCronField = parseField(value, Type.SECOND)

        fun parseMinutes(value: String): BitsCronField = parseField(value, Type.MINUTE)

        fun parseHours(value: String): BitsCronField = parseField(value, Type.HOUR)

        fun parseDaysOfMonth(value: String): BitsCronField = parseDate(value, Type.DAY_OF_MONTH)

        fun parseMonth(value: String): BitsCronField = parseField(value, Type.MONTH)

        fun parseDaysOfWeek(value: String): BitsCronField {
            var result = parseDate(value, Type.DAY_OF_WEEK)
            if (result.getBit(0)) {
                // Cron allows 0 for Sunday, but java.time uses 7.
                result = result.copySetting(7).copyClearing(0)
            }
            return result
        }

        private fun parseDate(value: String, type: Type): BitsCronField {
            val normalized = if (value == "?") "*" else value
            return parseField(normalized, type)
        }

        private fun parseField(value: String, type: Type): BitsCronField {
            return try {
                var bits = 0L
                var start = 0
                while (start <= value.length) {
                    val comma = value.indexOf(',', start).let { if (it == -1) value.length else it }
                    require(comma > start) { "Empty cron field segment" }
                    val segment = value.substring(start, comma)
                    val slashPos = segment.indexOf('/')
                    if (slashPos == -1) {
                        bits = bits or mask(parseRange(segment, type))
                    } else {
                        val rangeStr = segment.substring(0, slashPos)
                        val delta = segment.substring(slashPos + 1).toInt()
                        require(delta > 0) { "Incrementer delta must be 1 or higher" }

                        var range = parseRange(rangeStr, type)
                        if ('-' !in rangeStr) {
                            range = ValueRange.of(range.minimum, type.range().maximum)
                        }
                        bits = bits or mask(range, delta)
                    }
                    if (comma == value.length) {
                        break
                    }
                    start = comma + 1
                }
                BitsCronField(type, bits)
            } catch (ex: DateTimeException) {
                throw IllegalArgumentException("${ex.message} '$value'", ex)
            } catch (ex: IllegalArgumentException) {
                throw IllegalArgumentException("${ex.message} '$value'", ex)
            }
        }

        private fun parseRange(value: String, type: Type): ValueRange {
            if (value == "*") {
                return type.range()
            }
            val hyphenPos = value.indexOf('-')
            if (hyphenPos == -1) {
                val point = type.checkValidValue(value.toInt())
                return ValueRange.of(point.toLong(), point.toLong())
            }

            var min = value.substring(0, hyphenPos).toInt()
            var max = value.substring(hyphenPos + 1).toInt()
            min = type.checkValidValue(min)
            max = type.checkValidValue(max)
            if (type == Type.DAY_OF_WEEK && min == 7) {
                // If used as range minimum, Sunday means 0.
                min = 0
            }
            return ValueRange.of(min.toLong(), max.toLong())
        }

        private fun mask(range: ValueRange): Long {
            if (range.minimum == range.maximum) {
                return BIT_0 shl range.minimum.toInt()
            }
            val minMask = MASK shl range.minimum.toInt()
            val maxMask = MASK ushr -(range.maximum + 1).toInt()
            return minMask and maxMask
        }

        private fun mask(range: ValueRange, delta: Int): Long {
            if (delta == 1) {
                return mask(range)
            }
            var bits = 0L
            var i = range.minimum.toInt()
            while (i <= range.maximum) {
                bits = bits or (BIT_0 shl i)
                i += delta
            }
            return bits
        }
    }

    private fun copySetting(index: Int): BitsCronField = BitsCronField(type(), bits or (BIT_0 shl index))

    private fun copyClearing(index: Int): BitsCronField = BitsCronField(type(), bits and (BIT_0 shl index).inv())
}
