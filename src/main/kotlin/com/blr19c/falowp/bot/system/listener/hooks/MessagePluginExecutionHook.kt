package com.blr19c.falowp.bot.system.listener.hooks

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.message.MessagePluginRegister

/**
 * 消息类插件执行时
 */
data class MessagePluginExecutionHook(
    /**
     * 接收到的消息
     */
    val receiveMessage: ReceiveMessage,
    /**
     * 消息类插件注册器
     */
    val register: MessagePluginRegister
) : Plugin.Listener.Hook