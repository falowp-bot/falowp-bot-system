package com.blr19c.falowp.bot.system.scheduling.tasks

import com.blr19c.falowp.bot.system.listener.events.GreetingEvent
import com.blr19c.falowp.bot.system.plugin.task.TaskPluginRegister
import com.blr19c.falowp.bot.system.scheduling.cron.CronTrigger
import com.blr19c.falowp.bot.system.scheduling.cron.TriggerContext
import com.blr19c.falowp.bot.system.systemConfigProperty
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 早晚安事件
 */
object GreetingTask {

    private val goodMorningCron = CronTrigger(systemConfigProperty("greeting.goodMorning") { "0 0 7 * * ?" }, false)
    private val goodNightCron = CronTrigger(systemConfigProperty("greeting.goodNight") { "0 0 23 * * ?" }, false)

    private val leisureTime = run {
        val goodMorningCronTime = goodMorningCron.nextExecutionTime(TriggerContext())
        val goodNightCronTime = goodNightCron.nextExecutionTime(TriggerContext())
        goodMorningCronTime!!.isBefore(goodNightCronTime)
    }.let { AtomicBoolean(it) }

    val goodMorning = TaskPluginRegister(goodMorningCron, {
        leisureTime.set(false)
        this.publishEvent(GreetingEvent(goodMorning = true, goodNight = false))
    }, GreetingTask::class)

    val goodNight = TaskPluginRegister(goodNightCron, {
        leisureTime.set(true)
        this.publishEvent(GreetingEvent(goodMorning = false, goodNight = true))
    }, GreetingTask::class)

    /**
     * 等待到下一次早安
     */
    suspend fun delayNextGoodMorning(useGreeting: Boolean = true) {
        if (useGreeting && leisureTime.get()) {
            val nextExecutionTime = goodMorningCron.nextExecutionTime(TriggerContext())
            val delay = nextExecutionTime!!.toEpochMilli() - System.currentTimeMillis()
            if (leisureTime.get()) {
                delay(delay)
            }
        }
    }
}
