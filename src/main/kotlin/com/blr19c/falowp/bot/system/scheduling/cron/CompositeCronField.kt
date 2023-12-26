package com.blr19c.falowp.bot.system.scheduling.cron

import java.time.temporal.Temporal

/**
 * Extension of [CronField] that wraps an array of cron fields.
 */
@Suppress("UNCHECKED_CAST", "MemberVisibilityCanBePrivate", "UNUSED")
internal class CompositeCronField private constructor(
    type: Type,
    private val fields: Array<CronField>,
    private val value: String
) : CronField(type) {
    override fun <T> nextOrSame(temporal: T): T where T : Temporal? {
        var result: T? = null
        for (field in fields) {
            val candidate = field.nextOrSame(temporal) as Comparable<T>?
            if (result == null ||
                candidate != null && candidate < result
            ) {
                result = candidate as T?
            }
        }
        return result!!
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        return if (other !is CompositeCronField) {
            false
        } else type() === other.type() && value == other.value
    }

    override fun toString(): String {
        return type().toString() + " '" + value + "'"
    }

    companion object {
        /**
         * Composes the given fields into a [CronField].
         */
        fun compose(fields: Array<CronField>, type: Type, value: String): CronField {
            return if (fields.size == 1) {
                fields[0]
            } else {
                CompositeCronField(type, fields, value)
            }
        }
    }
}
