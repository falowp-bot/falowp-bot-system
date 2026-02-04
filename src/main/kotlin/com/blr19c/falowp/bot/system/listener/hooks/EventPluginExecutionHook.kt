package com.blr19c.falowp.bot.system.listener.hooks

import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.event.EventPluginRegister

/**
 * 事件类插件执行时
 */
data class EventPluginExecutionHook(

    /**
     * 事件
     */
    val event: Plugin.Listener.Event,

    /**
     * 事件类插件注册器
     */
    val register: EventPluginRegister<out Plugin.Listener.Event>
) : Plugin.Listener.Hook