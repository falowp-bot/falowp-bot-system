package com.blr19c.falowp.bot.system.plugin.event

import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.listener.events.HelpEvent
import com.blr19c.falowp.bot.system.plugin.EventPluginRegister
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.PluginInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 事件类插件管理
 */
object EventManager : Log {

    private val eventPlugins = CopyOnWriteArrayList<EventPluginRegister<out Plugin.Listener.Event>>()
    private val executor = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun configure(pluginList: List<PluginInfo>) {
        registerHelp(pluginList)
        log().info("加载的事件监听器数量:{}", eventPlugins.size)
    }

    /**
     * 注册事件
     */
    fun <T : Plugin.Listener.Event> registerEvent(pluginRegister: EventPluginRegister<T>) {
        eventPlugins.add(pluginRegister)
    }

    /**
     * 处理事件
     */
    suspend fun <T : Plugin.Listener.Event> publishEvent(botApi: BotApi, event: T) {
        eventPlugins.filter { it.listener.isInstance(event) }.forEach { executor.launch { it.publish(botApi, event) } }
    }

    /**
     * 帮助事件注册
     */
    private fun registerHelp(pluginList: List<PluginInfo>) {
        registerEvent(EventPluginRegister(HelpEvent::class, PluginHelp(pluginList), PluginHelp::class))
    }
}