package com.blr19c.falowp.bot.system.listener.events

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 用户退群事件
 */
@Suppress("UNUSED")
data class GroupDecreaseEvent(
    /**
     * 来源
     */
    override val source: ReceiveMessage.Source,
    /**
     * 操作人
     */
    override val actor: ReceiveMessage.User,
    /**
     * 退群人
     */
    val user: ReceiveMessage.User,
    /**
     * 类型
     */
    val type: String
) : Plugin.Listener.Event