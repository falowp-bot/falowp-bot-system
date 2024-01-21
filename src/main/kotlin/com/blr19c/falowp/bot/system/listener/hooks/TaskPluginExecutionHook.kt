package com.blr19c.falowp.bot.system.listener.hooks

import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.TaskPluginRegister

/**
 * 任务类插件执行时
 */
data class TaskPluginExecutionHook(

    /**
     * 任务类插件注册器
     */
    val register: TaskPluginRegister
) : Plugin.Listener.Hook