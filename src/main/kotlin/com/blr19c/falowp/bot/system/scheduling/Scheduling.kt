package com.blr19c.falowp.bot.system.scheduling

import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.adapter.AdapterApplication.botApiSupportList
import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.plugin.PluginBotApi
import com.blr19c.falowp.bot.system.plugin.task.TaskPluginRegister
import com.blr19c.falowp.bot.system.scheduling.tasks.GreetingTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

/**
 * 定时任务
 */
object Scheduling : Log {

    private val executor = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val executorTaskList = CopyOnWriteArrayList<SchedulingRunnable>()

    suspend fun selectBot(receive: String, originalClass: KClass<*>): BotApi? {
        return botApiSupportList()
            .firstOrNull { it.supportReceive(receive) }
            ?.bot(receive, originalClass)
            ?.let { PluginBotApi(it) }
    }

    suspend fun allBot(originalClass: KClass<*>): List<BotApi> {
        return botApiSupportList().map { PluginBotApi(it.bot("", originalClass)) }.toList()
    }

    fun registerTask(pluginRegister: TaskPluginRegister) {
        executorTaskList.add(schedulingRunnable(pluginRegister))
    }

    fun unregisterTask(pluginRegister: TaskPluginRegister) {
        executor.launch {
            executorTaskList.singleOrNull { it.plugin.pluginId == pluginRegister.pluginId }?.let {
                it.cancel()
                executorTaskList.remove(it)
            }
        }
    }

    fun configure() {
        log().info("初始化(周期/cron)任务")
        val systemTasks = initSystemTasks()
        executorTaskList.addAll(systemTasks)
        log().info("已加载的(周期/cron)任务数量:{}", executorTaskList.size)
        log().info("初始化(周期/cron)任务完成")
    }

    private fun initSystemTasks(): List<SchedulingRunnable> {
        return listOf(
            schedulingRunnable(GreetingTask.goodMorning),
            schedulingRunnable(GreetingTask.goodNight)
        )
    }

    private fun schedulingRunnable(plugin: TaskPluginRegister): SchedulingRunnable {
        val runnable = SchedulingRunnable(plugin, executor)
        runnable.schedule()
        return runnable
    }
}
