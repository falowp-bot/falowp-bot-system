package com.blr19c.falowp.bot.system.listener.hooks

import com.blr19c.falowp.bot.system.api.SendMessageChain
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 发送消息时
 */
data class SendMessageHook(
    /**
     * 发送的消息
     */
    val sendMessageChain: MutableList<SendMessageChain>
) : Plugin.Listener.Hook