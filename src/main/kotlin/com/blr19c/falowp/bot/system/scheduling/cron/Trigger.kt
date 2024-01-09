package com.blr19c.falowp.bot.system.scheduling.cron

import com.blr19c.falowp.bot.system.cache.OnlyReadOnceReference
import com.blr19c.falowp.bot.system.systemConfigProperty
import java.time.Instant
import java.time.ZoneOffset
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface Trigger {
    /**
     * 跟随系统的闲时状态执行定时
     */
    val useGreeting: Boolean

    /**
     * 获取下次执行时间
     */
    fun nextExecutionTime(triggerContext: TriggerContext): Instant?

    /**
     * 任务执行时间
     */
    fun scheduledDate(triggerContext: TriggerContext): Instant {
        var completion = triggerContext.lastCompletionTime() ?: Instant.now()
        val scheduled = triggerContext.lastScheduledExecutionTime()
        //防止时钟毫秒内回拨
        if (scheduled != null && completion.isBefore(scheduled)) {
            completion = scheduled
        }
        return completion
    }

}

/**
 * Cron表达式
 */
data class CronTrigger(
    /**
     * cron
     */
    val cron: String,
    override val useGreeting: Boolean
) : Trigger {
    private val cronExpression: CronExpression = CronExpression.parse(cron)

    override fun nextExecutionTime(triggerContext: TriggerContext): Instant? {
        val zoneOffset = ZoneOffset.of(systemConfigProperty("zoneOffset") { "+8" })
        val date = scheduledDate(triggerContext).atZone(zoneOffset).toLocalDateTime()
        return cronExpression.next(date)?.toInstant(zoneOffset)
    }

}

/**
 * 周期循环
 */
data class PeriodicTrigger(
    /**
     * 固定速率执行
     */
    val fixedRate: Boolean = false,
    /**
     * 执行周期
     */
    val period: Duration,
    /**
     * 首次执行间隔
     */
    val initialDelay: Duration = 0.seconds,
    override val useGreeting: Boolean,
) : Trigger {

    override fun nextExecutionTime(triggerContext: TriggerContext): Instant? {
        val lastExecution = triggerContext.lastScheduledExecutionTime()
            ?: return Instant.ofEpochMilli(System.currentTimeMillis() + initialDelay.inWholeMilliseconds)
        return if (fixedRate) lastExecution.plusMillis(period.inWholeMilliseconds)
        else scheduledDate(triggerContext).plusMillis(period.inWholeMilliseconds)
    }
}


/**
 * 在程序完全启动之后执行
 */
class ApplicationInitTrigger(override val useGreeting: Boolean) : Trigger {

    private val onlyReadOnceReference by OnlyReadOnceReference {
        Instant.ofEpochMilli(System.currentTimeMillis())
    }

    override fun nextExecutionTime(triggerContext: TriggerContext): Instant? {
        return onlyReadOnceReference
    }
}