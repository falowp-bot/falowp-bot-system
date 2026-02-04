package com.blr19c.falowp.bot.system.plugin.event

import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.PluginRegister
import com.blr19c.falowp.bot.system.utils.ScanUtils.getCallerClass
import kotlin.reflect.KClass

/**
 * 事件类插件
 */
data class EventPluginRegister<T : Plugin.Listener.Event>(
    /**
     * 监听的事件类型
     */
    val listener: KClass<T>,
    /**
     * 执行内容
     */
    val block: suspend BotApi.(event: T) -> Unit,
    override val originalClass: KClass<*> = getCallerClass()
) : PluginRegister() {

    suspend fun publish(botApi: BotApi, event: Any) {
        @Suppress("UNCHECKED_CAST")//因为data class无法使用reified，导致T被擦出无法正确识别T
        block.invoke(botApi, event as T)
    }

    override fun register() {
        EventManager.registerEvent(this)
    }

    override fun unregister() {
        EventManager.unregisterEvent(this)
    }
}