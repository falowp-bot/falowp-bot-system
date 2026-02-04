package com.blr19c.falowp.bot.system.listener.events

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 轻推事件(戳一戳/拍一拍/无内容的强提醒)
 */
@Suppress("UNUSED")
data class NudgeEvent(
    /**
     * 来源
     */
    override val source: ReceiveMessage.Source,
    /**
     * 发起人
     */
    override val actor: ReceiveMessage.User,
    /**
     * 接收人
     */
    val target: ReceiveMessage.User,
    /**
     * 其他动作
     */
    val action: Any? = null
) : Plugin.Listener.Event
