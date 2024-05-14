package com.blr19c.falowp.bot.system.listener.events

import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.api.MessageTypeEnum
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SendMessageChain
import com.blr19c.falowp.bot.system.plugin.MessagePluginRegister
import com.blr19c.falowp.bot.system.plugin.MessagePluginRegisterMatch
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.Register

/**
 * 机器人发送了消息的事件
 */
data class SendMessageEvent(
    /**
     * 发送的消息
     */
    val sendMessage: List<SendMessageChain>,
    /**
     * 是否引用
     */
    val reference: Boolean,
    /**
     * 是否转发
     */
    val forward: Boolean
) : Plugin.Listener.Event

/**
 * 新用户进群事件
 */
data class GroupIncreaseEvent(
    /**
     * 进群人
     */
    val user: ReceiveMessage.User,
    /**
     * 来源
     */
    val source: ReceiveMessage.Source
) : Plugin.Listener.Event

/**
 * 用户退群事件
 */
data class GroupDecreaseEvent(
    /**
     * 退群人
     */
    val user: ReceiveMessage.User,
    /**
     * 来源
     */
    val source: ReceiveMessage.Source
) : Plugin.Listener.Event

/**
 * 撤回消息事件
 */
data class WithdrawMessageEvent(
    /**
     * 消息内容
     */
    val receiveMessage: ReceiveMessage,
    /**
     * 撤回人
     */
    val withdrawUser: ReceiveMessage.User
) : Plugin.Listener.Event

/**
 * 撤回消息
 *
 * @param terminateEvent 执行完终止事件传播
 * @param block 执行内容
 */
fun withdrawMessage(
    order: Int = 0,
    terminateEvent: Boolean = true,
    block: suspend BotApi.(args: Array<String>) -> Unit
): Register {
    return MessagePluginRegister(
        order,
        MessagePluginRegisterMatch(messageType = MessageTypeEnum.WITHDRAW_MESSAGE),
        terminateEvent,
        block
    )
}