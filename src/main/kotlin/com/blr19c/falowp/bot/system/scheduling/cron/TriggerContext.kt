package com.blr19c.falowp.bot.system.scheduling.cron

import java.time.Instant
import kotlin.concurrent.Volatile

@Suppress("UNUSED")
class TriggerContext {
    @Volatile
    private var lastScheduledExecutionTime: Instant? = null

    @Volatile
    private var lastActualExecutionTime: Instant? = null

    @Volatile
    private var lastCompletionTime: Instant? = null

    /**
     * Create a SimpleTriggerContext with all time values set to `null`.
     */
    constructor()

    /**
     * Create a SimpleTriggerContext with the given time values.
     *
     * @param lastScheduledExecutionTime last *scheduled* execution time
     * @param lastActualExecutionTime    last *actual* execution time
     * @param lastCompletionTime         last completion time
     */
    constructor(lastScheduledExecutionTime: Instant, lastActualExecutionTime: Instant, lastCompletionTime: Instant) {
        this.lastScheduledExecutionTime = lastScheduledExecutionTime
        this.lastActualExecutionTime = lastActualExecutionTime
        this.lastCompletionTime = lastCompletionTime
    }

    /**
     * Update this holder's state with the latest time values.
     *
     * @param lastScheduledExecutionTime last *scheduled* execution time
     * @param lastActualExecutionTime    last *actual* execution time
     * @param lastCompletionTime         last completion time
     */
    fun update(lastScheduledExecutionTime: Instant, lastActualExecutionTime: Instant, lastCompletionTime: Instant) {
        this.lastScheduledExecutionTime = lastScheduledExecutionTime
        this.lastActualExecutionTime = lastActualExecutionTime
        this.lastCompletionTime = lastCompletionTime
    }

    fun lastScheduledExecutionTime(): Instant? {
        return lastScheduledExecutionTime
    }

    fun lastActualExecutionTime(): Instant? {
        return lastActualExecutionTime
    }

    fun lastCompletionTime(): Instant? {
        return lastCompletionTime
    }
}
