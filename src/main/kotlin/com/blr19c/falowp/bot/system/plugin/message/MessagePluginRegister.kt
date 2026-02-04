package com.blr19c.falowp.bot.system.plugin.message

import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.plugin.PluginManagement
import com.blr19c.falowp.bot.system.plugin.PluginRegister
import com.blr19c.falowp.bot.system.utils.ScanUtils.getCallerClass
import kotlin.reflect.KClass

/**
 * 消息类插件
 */
data class MessagePluginRegister(
    /**
     * 排序
     */
    val order: Int,
    /**
     * 匹配规则
     */
    val match: MessageMatch,
    /**
     * 执行完终止事件传播
     */
    val terminateEvent: Boolean,
    /**
     * 执行内容
     */
    val block: suspend BotApi.(args: Array<String>) -> Unit,
    override val originalClass: KClass<*> = getCallerClass()
) : PluginRegister() {

    override fun register() {
        PluginManagement.registerMessage(this)
    }

    override fun unregister() {
        PluginManagement.unregisterMessage(this)
    }
}