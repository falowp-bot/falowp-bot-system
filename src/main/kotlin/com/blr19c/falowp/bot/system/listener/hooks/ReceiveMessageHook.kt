package com.blr19c.falowp.bot.system.listener.hooks

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin


/**
 * 接收到消息时
 */
data class ReceiveMessageHook(
    /**
     * 接收到的消息
     */
    val receiveMessage: ReceiveMessage
) : Plugin.Listener.Hook