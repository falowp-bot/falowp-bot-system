package com.blr19c.falowp.bot.system.scheduling

import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.plugin.TaskPluginRegister
import com.blr19c.falowp.bot.system.scheduling.api.SchedulingBotApi
import com.blr19c.falowp.bot.system.scheduling.api.SchedulingBotApiSupport
import com.blr19c.falowp.bot.system.scheduling.tasks.GreetingTask
import com.blr19c.falowp.bot.system.utils.ScanUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

/**
 * 定时任务
 */
object Scheduling : Log {
    private val botList = arrayListOf<SchedulingBotApiSupport>()
    private val taskPlugins = CopyOnWriteArrayList<TaskPluginRegister>()
    private val executor = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val executorTaskList = CopyOnWriteArrayList<SchedulingRunnable>()

    suspend fun selectBot(receive: String, originalClass: KClass<*>): BotApi? {
        return botList.firstOrNull { it.supportReceive(receive) }?.bot(receive, originalClass)
    }

    suspend fun allBot(originalClass: KClass<*>): List<BotApi> {
        return botList.map { it.bot("", originalClass) }.toList()
    }

    fun registerTask(pluginRegister: TaskPluginRegister) {
        taskPlugins.add(pluginRegister)
    }

    fun configure() {
        log().info("初始化(周期/cron)任务")
        ScanUtils.scanPackage("com.blr19c.falowp.bot.system.adapter")
            .filter { SchedulingBotApiSupport::class.java.isAssignableFrom(it) }
            .forEach { botList.add(it.kotlin.objectInstance as SchedulingBotApiSupport) }
        botList.sortBy { it.order() }
        val tasks = taskPlugins.map(this::schedulingRunnable)
        log().info("已加载的(周期/cron)任务数量:{}", tasks.size)
        val systemTasks = initSystemTasks().map(this::schedulingRunnable)
        executorTaskList.addAll(systemTasks + tasks)
        executorTaskList.forEach(SchedulingRunnable::schedule)
        log().info("初始化(周期/cron)任务完成")
    }

    private fun initSystemTasks(): List<TaskPluginRegister> {
        return listOf(GreetingTask.goodMorning, GreetingTask.goodNight)
    }

    private fun schedulingRunnable(plugin: TaskPluginRegister): SchedulingRunnable {
        return SchedulingRunnable(
            { plugin.block.invoke(SchedulingBotApi(plugin.originalClass)) },
            executor,
            plugin.trigger
        )
    }
}
