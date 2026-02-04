@file:Suppress("UNUSED")

package com.blr19c.falowp.bot.system.plugin.event

import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.listener.events.NudgeEvent
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.PluginRegister

/**
 * 订阅事件(内部事件注册)
 */
inline fun <reified T : Plugin.Listener.Event> eventListener(noinline block: suspend BotApi.(T) -> Unit): PluginRegister {
    return EventPluginRegister(T::class, block)
}

/**
 * 轻推事件快捷注册
 */
fun nudgeMe(block: suspend BotApi.(NudgeEvent) -> Unit) = eventListener<NudgeEvent> { event ->
    if (this.self().id == event.target.id) {
        block(event)
    }
}
