package com.blr19c.falowp.bot.system.plugin.message

import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.plugin.PluginManagement
import com.blr19c.falowp.bot.system.plugin.PluginRegister
import kotlinx.coroutines.channels.Channel
import kotlin.reflect.KClass

/**
 * 队列消息类插件
 */
data class QueueMessagePluginRegister(
    /**
     * 原消息类插件
     */
    val messagePluginRegister: MessagePluginRegister,
    /**
     * 最大等待长度限制
     */
    val queueCapacity: Int = Channel.UNLIMITED,
    /**
     * 成功进入队列回调
     */
    val onSuccess: suspend BotApi.(queueIndex: Int) -> Unit = {},
    /**
     * 超过最大等待长度限制回调
     */
    val onOverFlow: suspend BotApi.() -> Unit = {},

    override val pluginId: String = messagePluginRegister.pluginId,
    override val originalClass: KClass<*> = messagePluginRegister.originalClass
) : PluginRegister() {

    override fun register() {
        PluginManagement.registerMessage(this)
    }

    override fun unregister() {
        PluginManagement.unregisterMessage(this)
    }
}