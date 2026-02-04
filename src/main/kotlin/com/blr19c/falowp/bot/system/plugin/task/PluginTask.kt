@file:Suppress("UNUSED")

package com.blr19c.falowp.bot.system.plugin.task

import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.plugin.PluginRegister
import com.blr19c.falowp.bot.system.scheduling.cron.ApplicationInitTrigger
import com.blr19c.falowp.bot.system.scheduling.cron.CronTrigger
import com.blr19c.falowp.bot.system.scheduling.cron.PeriodicTrigger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


/**
 * cron表达式执行任务
 * @param cron cron表达式
 * @param useGreeting 跟随系统的闲时状态执行定时
 * @param block 执行内容
 */
fun cronScheduling(
    cron: String,
    useGreeting: Boolean = true,
    block: suspend BotApi.() -> Unit
): PluginRegister {
    return TaskPluginRegister(CronTrigger(cron, useGreeting), block)
}

/**
 * 周期执行任务
 *
 * @param period 执行周期
 * @param initialDelay 首次执行延时时间
 * @param fixedRate 是否为固定速率执行
 * @param useGreeting 跟随系统的闲时状态执行定时
 * @param block 执行内容
 */
fun periodicScheduling(
    period: Duration,
    initialDelay: Duration = 0.seconds,
    fixedRate: Boolean = false,
    useGreeting: Boolean = true,
    block: suspend BotApi.() -> Unit
): PluginRegister {
    return TaskPluginRegister(PeriodicTrigger(fixedRate, period, initialDelay, useGreeting), block)
}

/**
 * 在程序完全启动之后执行
 *
 * @param useGreeting 跟随系统的闲时状态执行定时
 * @param block 执行内容
 */
fun applicationInitScheduling(
    useGreeting: Boolean = true,
    block: suspend BotApi.() -> Unit
): PluginRegister {
    return TaskPluginRegister(ApplicationInitTrigger(useGreeting), block)
}