package com.blr19c.falowp.bot.system.scheduling

import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.adapter.AdapterApplication
import com.blr19c.falowp.bot.system.scheduling.cron.Trigger
import com.blr19c.falowp.bot.system.scheduling.cron.TriggerContext
import com.blr19c.falowp.bot.system.utils.ReflectionUtils
import kotlinx.coroutines.*
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTime

/**
 * 协程任务
 */
@Suppress("UNUSED")
class SchedulingRunnable(
    private val delegate: suspend () -> Unit,
    private val executor: CoroutineScope,
    private val trigger: Trigger,
    private val triggerContext: TriggerContext = TriggerContext(),
) : Log {
    private val triggerContextMonitor = Any()

    @Volatile
    private var currentFuture: Deferred<Unit>? = null

    private var scheduledExecutionTime: Instant? = null

    /**
     * 取消任务执行
     */
    fun cancel() = synchronized(triggerContextMonitor) {
        obtainCurrentFuture().cancel(CancellationException("任务被手动取消"))
    }

    /**
     * 任务是否取消
     */
    fun isCancelled() = synchronized(triggerContextMonitor) { obtainCurrentFuture().isCancelled }

    /**
     * 任务是否完成
     */
    fun isCompleted() = synchronized(triggerContextMonitor) { obtainCurrentFuture() }

    /**
     * 是否初始化完成
     */
    private fun isInit() = synchronized(triggerContextMonitor) { AdapterApplication.isLoadingCompleted() }

    /**
     * 等待任务直至完成
     */
    suspend fun await() = synchronized(triggerContextMonitor) { obtainCurrentFuture() }.await()

    suspend fun await(timeout: Duration) = withTimeout(timeout) {
        synchronized(triggerContextMonitor) { obtainCurrentFuture() }.await()
    }

    /**
     * 开始执行定时任务
     */
    fun schedule(): SchedulingRunnable? {
        synchronized(triggerContextMonitor) {
            scheduledExecutionTime = this.trigger.nextExecutionTime(this.triggerContext) ?: return null
            val initialDelay = (scheduledExecutionTime!!.toEpochMilli() - System.currentTimeMillis()).milliseconds
            this.currentFuture = executor.async { asyncRun(initialDelay) }
            return this
        }
    }


    private suspend fun run() {
        val actualExecutionTime = Instant.now()
        delegateRun()
        val completionTime = Instant.now()
        synchronized(triggerContextMonitor) {
            this.scheduledExecutionTime ?: throw IllegalStateException("No scheduled execution")
            this.triggerContext.update(this.scheduledExecutionTime!!, actualExecutionTime, completionTime)
            if (!obtainCurrentFuture().isCancelled) {
                schedule()
            }
        }
    }

    private suspend fun delegateRun() {
        try {
            delegate.invoke()
        } catch (ex: Throwable) {
            log().error("计划任务异常", ReflectionUtils.skipReflectionException(ex))
        }
    }

    private suspend fun asyncRun(initialDelay: Duration) {
        delay(initialDelay.plus(awaitInit()))
        this.run()
    }

    private suspend fun awaitInit(): Duration = measureTime {
        while (!isInit()) {
            delay(500)
        }
    }

    private fun obtainCurrentFuture(): Deferred<Unit> {
        return this.currentFuture!!
    }
}