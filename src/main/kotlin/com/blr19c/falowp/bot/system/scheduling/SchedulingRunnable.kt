package com.blr19c.falowp.bot.system.scheduling

import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.adapter.AdapterApplication
import com.blr19c.falowp.bot.system.listener.hooks.TaskPluginExecutionHook
import com.blr19c.falowp.bot.system.plugin.TaskPluginRegister
import com.blr19c.falowp.bot.system.plugin.hook.withPluginHook
import com.blr19c.falowp.bot.system.scheduling.api.SchedulingBotApi
import com.blr19c.falowp.bot.system.scheduling.cron.Trigger
import com.blr19c.falowp.bot.system.scheduling.cron.TriggerContext
import com.blr19c.falowp.bot.system.scheduling.tasks.GreetingTask
import com.blr19c.falowp.bot.system.utils.ReflectionUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * 协程任务
 */
@Suppress("UNUSED")
class SchedulingRunnable(
    private val plugin: TaskPluginRegister,
    private val executor: CoroutineScope,
    private val trigger: Trigger = plugin.trigger,
    private val triggerContext: TriggerContext = TriggerContext(),
) : Log {
    private val mutex = Mutex()

    @Volatile
    private var currentFuture: Deferred<Unit>? = null

    private var scheduledExecutionTime: Instant? = null

    /**
     * 取消任务执行
     */
    suspend fun cancel() = mutex.withLock {
        obtainCurrentFuture().cancel(CancellationException("任务被手动取消"))
    }

    /**
     * 任务是否取消
     */
    suspend fun isCancelled() = mutex.withLock { obtainCurrentFuture().isCancelled }

    /**
     * 任务是否完成
     */
    suspend fun isCompleted() = mutex.withLock { obtainCurrentFuture() }

    /**
     * 是否初始化完成
     */
    private suspend fun isInit() = mutex.withLock { AdapterApplication.isLoadingCompleted() }

    /**
     * 等待任务直至完成
     */
    suspend fun await() = mutex.withLock { obtainCurrentFuture() }.await()

    suspend fun await(timeout: Duration) = withTimeout(timeout) {
        mutex.withLock { obtainCurrentFuture() }.await()
    }

    /**
     * 开始执行定时任务
     */
    fun schedule(): SchedulingRunnable? {
        scheduledExecutionTime = this.trigger.nextExecutionTime(this.triggerContext) ?: return null
        val initialDelay = (scheduledExecutionTime!!.toEpochMilli() - System.currentTimeMillis()).milliseconds
        this.currentFuture = executor.async { asyncRun(initialDelay) }
        return this
    }


    private suspend fun run() {
        val actualExecutionTime = Instant.now()
        delegateRun()
        val completionTime = Instant.now()
        mutex.withLock {
            this.scheduledExecutionTime ?: throw IllegalStateException("No scheduled execution")
            this.triggerContext.update(this.scheduledExecutionTime!!, actualExecutionTime, completionTime)
            if (!obtainCurrentFuture().isCancelled) {
                schedule()
            }
        }
    }

    private suspend fun delegateRun() {
        try {
            val schedulingBotApi = SchedulingBotApi(plugin.originalClass)
            withPluginHook(schedulingBotApi, TaskPluginExecutionHook(plugin)) {
                plugin.block.invoke(schedulingBotApi)
            }
        } catch (ex: Throwable) {
            log().error("计划任务异常", ReflectionUtils.skipReflectionException(ex))
        }
    }

    private suspend fun asyncRun(initialDelay: Duration) {
        awaitInit()
        delay(initialDelay)
        GreetingTask.delayNextGoodMorning(this.trigger.useGreeting)
        this.run()
    }

    private suspend fun awaitInit() {
        while (!isInit()) {
            delay(500)
        }
    }

    private fun obtainCurrentFuture(): Deferred<Unit> {
        return this.currentFuture!!
    }
}