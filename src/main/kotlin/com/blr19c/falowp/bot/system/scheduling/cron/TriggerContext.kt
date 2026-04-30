package com.blr19c.falowp.bot.system.scheduling.cron

import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

/**
 * 定时任务触发上下文
 */
@Suppress("UNUSED")
class TriggerContext {
    /**
     * 最近一次任务执行状态
     */
    internal class State(
        /**
         * 上次计划执行时间
         */
        val lastScheduledExecutionTime: Instant?,
        /**
         * 上次实际执行时间
         */
        val lastActualExecutionTime: Instant?,
        /**
         * 上次完成时间
         */
        val lastCompletionTime: Instant?,
    )

    private val stateRef = AtomicReference(State(null, null, null))

    constructor()

    /**
     * 使用指定执行状态创建上下文
     */
    constructor(lastScheduledExecutionTime: Instant, lastActualExecutionTime: Instant, lastCompletionTime: Instant) {
        stateRef.set(State(lastScheduledExecutionTime, lastActualExecutionTime, lastCompletionTime))
    }

    /**
     * 更新执行状态
     */
    fun update(lastScheduledExecutionTime: Instant, lastActualExecutionTime: Instant, lastCompletionTime: Instant) {
        stateRef.set(State(lastScheduledExecutionTime, lastActualExecutionTime, lastCompletionTime))
    }

    /**
     * 上次计划执行时间
     */
    fun lastScheduledExecutionTime(): Instant? = stateRef.get().lastScheduledExecutionTime

    /**
     * 上次实际执行时间
     */
    fun lastActualExecutionTime(): Instant? = stateRef.get().lastActualExecutionTime

    /**
     * 上次完成时间
     */
    fun lastCompletionTime(): Instant? = stateRef.get().lastCompletionTime
}
