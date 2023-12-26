package com.blr19c.falowp.bot.system.listener.events

import com.blr19c.falowp.bot.system.api.SendMessage
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 机器人发送了消息的事件
 */
data class SendMessageEvent(
    /**
     * 发送的消息
     */
    val sendMessage: List<SendMessage>,
    /**
     * 是否引用
     */
    val reference: Boolean,
    /**
     * 是否转发
     */
    val forward: Boolean
) : Plugin.Listener.Event