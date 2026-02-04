@file:Suppress("UNUSED")

package com.blr19c.falowp.bot.system.plugin.message

import com.blr19c.falowp.bot.system.api.ApiAuth
import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.plugin.PluginRegister
import kotlinx.coroutines.channels.Channel

/**
 * 消息
 *
 * @param regex 正则匹配
 * @param auth 权限
 * @param terminateEvent 执行完终止事件传播
 * @param block 执行内容
 */
fun message(
    regex: Regex,
    order: Int = 0,
    auth: ApiAuth = ApiAuth.ORDINARY_MEMBER,
    terminateEvent: Boolean = true,
    block: suspend BotApi.(args: Array<String>) -> Unit
): PluginRegister {
    return MessagePluginRegister(
        order,
        MessageMatch.Build().regex(regex).auth(auth).build(),
        terminateEvent,
        block
    )
}

/**
 * 消息
 *
 * @param match 匹配规则
 * @param terminateEvent 执行完终止事件传播
 * @param block 执行内容
 */
fun message(
    match: MessageMatch = MessageMatch.allMatch(),
    order: Int = 0,
    terminateEvent: Boolean = true,
    block: suspend BotApi.(args: Array<String>) -> Unit
): PluginRegister {
    return MessagePluginRegister(
        order,
        match,
        terminateEvent,
        block
    )
}

/**
 * 队列消息
 *
 * @param regex 正则匹配
 * @param auth 权限
 * @param terminateEvent 执行完终止事件传播
 * @param block 执行内容
 * @param queueCapacity 队列长度限制(默认无限)
 */
fun queueMessage(
    regex: Regex,
    order: Int = 0,
    auth: ApiAuth = ApiAuth.ORDINARY_MEMBER,
    terminateEvent: Boolean = true,
    queueCapacity: Int = Channel.UNLIMITED,
    block: suspend BotApi.(args: Array<String>) -> Unit
): PluginRegister {
    return queueMessage(
        MessageMatch.Build().regex(regex).auth(auth).build(),
        order,
        terminateEvent,
        queueCapacity,
        block = block
    )
}

/**
 * 队列消息
 *
 * @param match 匹配规则
 * @param terminateEvent 执行完终止事件传播
 * @param block 执行内容
 * @param queueCapacity 队列长度限制(默认无限)
 * @param onSuccess 成功进入队列时的回调
 * @param onOverFlow 队列已满时回调
 */
fun queueMessage(
    match: MessageMatch = MessageMatch.allMatch(),
    order: Int = 0,
    terminateEvent: Boolean = true,
    queueCapacity: Int = Channel.UNLIMITED,
    onSuccess: suspend BotApi.(queueIndex: Int) -> Unit = { queueIndex ->
        if (queueIndex > 0) this.sendReply(
            "已成功进入队列,当前位于第${queueIndex}位,请耐心等待",
            reference = true
        )
    },
    onOverFlow: suspend BotApi.() -> Unit = {
        this.sendReply("当前功能请求人数较多,请稍后再试")
    },
    block: suspend BotApi.(args: Array<String>) -> Unit
): PluginRegister {
    return QueueMessagePluginRegister(
        message(match, order, terminateEvent, block) as MessagePluginRegister,
        queueCapacity,
        onSuccess,
        onOverFlow
    )
}