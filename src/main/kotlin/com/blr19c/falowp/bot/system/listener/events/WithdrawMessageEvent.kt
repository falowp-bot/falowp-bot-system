package com.blr19c.falowp.bot.system.listener.events

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 撤回消息事件
 */
@Suppress("UNUSED")
data class WithdrawMessageEvent(
    /**
     * 来源
     */
    override val source: ReceiveMessage.Source,
    /**
     * 撤回人
     */
    override val actor: ReceiveMessage.User,
    /**
     * 消息内容
     */
    val receiveMessage: ReceiveMessage,
) : Plugin.Listener.Event

