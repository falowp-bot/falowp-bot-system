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

    /**
     * 根据接收人选择可用BotApi
     */
    suspend fun selectBot(receive: String, originalClass: KClass<*>): BotApi? {
        return botApiSupportList()
            .firstOrNull { it.supportReceive(receive) }
            ?.bot(receive, originalClass)
            ?.let { PluginBotApi(it) }
    }

    /**
     * 获取所有可用BotApi
     */
    suspend fun allBot(originalClass: KClass<*>): List<BotApi> {
        return botApiSupportList().map { PluginBotApi(it.bot("", originalClass)) }.toList()
    }

    /**
     * 注册任务
     */
    fun registerTask(pluginRegister: TaskPluginRegister) {
        executorTaskList.add(schedulingRunnable(pluginRegister))
    }

    /**
     * 取消注册任务
     */
    fun unregisterTask(pluginRegister: TaskPluginRegister) {
        executor.launch {
            executorTaskList.singleOrNull { it.plugin.pluginId == pluginRegister.pluginId }?.let {
                it.cancel()
                executorTaskList.remove(it)
            }
        }
    }

    /**
     * 初始化定时任务
     */
    fun configure() {
        log().info("初始化(周期/cron)任务")
        val systemTasks = initSystemTasks()
        executorTaskList.addAll(systemTasks)
        log().info("已加载的(周期/cron)任务数量:{}", executorTaskList.size)
        log().info("初始化(周期/cron)任务完成")
    }

    /**
     * 初始化系统内置任务
     */
    private fun initSystemTasks(): List<SchedulingRunnable> {
        return listOf(
            schedulingRunnable(GreetingTask.goodMorning),
            schedulingRunnable(GreetingTask.goodNight)
        )
    }

    /**
     * 创建并调度任务
     */
    private fun schedulingRunnable(plugin: TaskPluginRegister): SchedulingRunnable {
        val runnable = SchedulingRunnable(plugin, executor)
        runnable.schedule()
        return runnable
    }
}
