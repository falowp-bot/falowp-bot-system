package com.blr19c.falowp.bot.system.plugin.task

import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.plugin.PluginRegister
import com.blr19c.falowp.bot.system.scheduling.Scheduling
import com.blr19c.falowp.bot.system.scheduling.cron.Trigger
import com.blr19c.falowp.bot.system.utils.ScanUtils.getCallerClass
import kotlin.reflect.KClass

/**
 * 任务类插件
 */
data class TaskPluginRegister(
    /**
     * 触发器
     */
    val trigger: Trigger,
    /**
     * 执行内容
     */
    val block: suspend BotApi.() -> Unit,
    override val originalClass: KClass<*> = getCallerClass()
) : PluginRegister() {

    override fun register() {
        Scheduling.registerTask(this)
    }

    override fun unregister() {
        Scheduling.unregisterTask(this)
    }
}