package com.blr19c.falowp.bot.system.plugin.event

import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.listener.events.HelpEvent
import com.blr19c.falowp.bot.system.listener.hooks.EventPluginExecutionHook
import com.blr19c.falowp.bot.system.plugin.EventPluginRegister
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.PluginInfo
import com.blr19c.falowp.bot.system.plugin.hook.withPluginHook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * 事件类插件管理
 */
object EventManager : Log {

    private val executor = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val eventPlugins = ConcurrentHashMap<KClass<out Plugin.Listener.Event>,
            List<EventPluginRegister<out Plugin.Listener.Event>>>()


    fun configure(pluginList: List<PluginInfo>) {
        registerHelp(pluginList)
        log().info("加载的事件监听器数量:{}", eventPlugins.values.sumOf { it.size })
    }

    /**
     * 注册事件
     */
    fun <T : Plugin.Listener.Event> registerEvent(pluginRegister: EventPluginRegister<T>) {
        eventPlugins.compute(pluginRegister.listener) { _, v ->
            v?.let { it + listOf(pluginRegister) } ?: listOf(pluginRegister)
        }
    }

    /**
     * 取消注册事件
     */
    fun <T : Plugin.Listener.Event> unregisterEvent(pluginRegister: EventPluginRegister<T>) {
        eventPlugins.computeIfPresent(pluginRegister.listener) { _, v ->
            v.filter { it != pluginRegister }
        }
    }

    /**
     * 处理事件
     */
    fun <T : Plugin.Listener.Event> publishEvent(botApi: BotApi, event: T) {
        eventPlugins[event::class]?.forEach {
            executor.launch {
                withPluginHook(botApi, EventPluginExecutionHook(event, it)) {
                    it.publish(botApi, event)
                }
            }
        }
    }

    /**
     * 帮助事件注册
     */
    private fun registerHelp(pluginList: List<PluginInfo>) {
        registerEvent(EventPluginRegister(HelpEvent::class, PluginHelp(pluginList), PluginHelp::class))
    }
}