package com.blr19c.falowp.bot.system.scheduling.cron

import java.time.temporal.Temporal

/**
 * Extension of [CronField] that wraps a collection of cron fields.
 */
internal class CompositeCronField private constructor(
    type: Type,
    private val fields: Array<CronField>,
    private val value: String,
) : CronField(type) {

    override fun nextOrSame(temporal: Temporal): Temporal? {
        var best: Temporal? = null
        for (field in fields) {
            val candidate = field.nextOrSame(temporal) ?: continue
            if (best == null || isBefore(candidate, best)) {
                best = candidate
            }
        }
        return best
    }

    override fun hashCode(): Int = value.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        return other is CompositeCronField && type() == other.type() && value == other.value
    }

    override fun toString(): String = "${type()} '$value'"

    companion object {
        fun compose(fields: Array<CronField>, type: Type, value: String): CronField {
            return if (fields.size == 1) fields[0] else CompositeCronField(type, fields, value)
        }
    }
}
