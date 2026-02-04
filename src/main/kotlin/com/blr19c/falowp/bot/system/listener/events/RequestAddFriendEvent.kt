package com.blr19c.falowp.bot.system.listener.events

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 申请添加好友事件
 */
@Suppress("UNUSED")
data class RequestAddFriendEvent(
    /**
     * 来源
     */
    override val source: ReceiveMessage.Source,
    /**
     * 申请人
     */
    override val actor: ReceiveMessage.User,
    /**
     * 验证信息
     */
    val comment: String,
    /**
     * 加好友标识
     */
    val flag: Any
) : Plugin.Listener.Event