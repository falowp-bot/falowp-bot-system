package com.blr19c.falowp.bot.system.listener.events

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 申请进群事件
 */
@Suppress("UNUSED")
data class RequestJoinGroupEvent(
    /**
     * 来源
     */
    override val source: ReceiveMessage.Source,
    /**
     * 进群人
     */
    override val actor: ReceiveMessage.User,
    /**
     * 验证信息
     */
    val comment: String,
    /**
     * 进群标识
     */
    val flag: Any,
    /**
     * 进群类型
     */
    val type: String
) : Plugin.Listener.Event