package com.blr19c.falowp.bot.system.scheduling.cron

import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

@Suppress("UNUSED")
class TriggerContext {
    internal class State(
        val lastScheduledExecutionTime: Instant?,
        val lastActualExecutionTime: Instant?,
        val lastCompletionTime: Instant?,
    )

    private val stateRef = AtomicReference(State(null, null, null))

    constructor()

    constructor(lastScheduledExecutionTime: Instant, lastActualExecutionTime: Instant, lastCompletionTime: Instant) {
        stateRef.set(State(lastScheduledExecutionTime, lastActualExecutionTime, lastCompletionTime))
    }

    fun update(lastScheduledExecutionTime: Instant, lastActualExecutionTime: Instant, lastCompletionTime: Instant) {
        stateRef.set(State(lastScheduledExecutionTime, lastActualExecutionTime, lastCompletionTime))
    }

    fun lastScheduledExecutionTime(): Instant? = stateRef.get().lastScheduledExecutionTime

    fun lastActualExecutionTime(): Instant? = stateRef.get().lastActualExecutionTime

    fun lastCompletionTime(): Instant? = stateRef.get().lastCompletionTime
}
