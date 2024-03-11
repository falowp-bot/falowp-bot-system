package com.blr19c.falowp.bot.system.api

import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.event.EventManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

/**
 * 机器人Api
 */
abstract class BotApi(val receiveMessage: ReceiveMessage, val originalClass: KClass<*>) : Log, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    override fun Log.log(): Logger {
        return LoggerFactory.getLogger(originalClass.java)
    }

    /**
     * 发布事件
     */
    suspend fun <T : Plugin.Listener.Event> publishEvent(event: T) {
        log().info("发布事件:{}", event)
        EventManager.publishEvent(this, event)
    }

    /**
     * 发送群聊消息
     * @param sendMessageChain 需要发送的消息链
     * @param reference 是否引用原消息
     * @param forward 是否已转发形式发送
     */
    abstract suspend fun sendGroup(
        vararg sendMessageChain: SendMessageChain,
        sourceId: String = receiveMessage.source.id,
        reference: Boolean = false,
        forward: Boolean = false
    )

    /**
     * 发送给所有群聊
     * @param sendMessageChain 需要发送的消息链
     * @param reference 是否引用原消息
     * @param forward 是否已转发形式发送
     */
    abstract suspend fun sendAllGroup(
        vararg sendMessageChain: SendMessageChain,
        reference: Boolean = false,
        forward: Boolean = false
    )

    /**
     * 发送私聊消息
     * @param sendMessageChain 需要发送的消息链
     * @param reference 是否引用原消息
     */
    abstract suspend fun sendPrivate(
        vararg sendMessageChain: SendMessageChain,
        sourceId: String = receiveMessage.source.id,
        reference: Boolean = false,
        forward: Boolean = false
    )

    /**
     * 发送回复消息(仅在回复消息时使用，不区分群聊与私聊)
     * @param sendMessage 需要发送的消息
     * @param reference 是否引用原消息
     */
    open suspend fun sendReply(
        vararg sendMessage: String,
        reference: Boolean = false,
        forward: Boolean = false
    ) {
        sendReply(
            *sendMessage.map { SendMessage.builder(it).build() }.toTypedArray(),
            reference = reference,
            forward = forward
        )
    }


    /**
     * 发送回复消息(仅在回复消息时使用，不区分群聊与私聊)
     * @param sendMessageChain 需要发送的消息链
     * @param reference 是否引用原消息
     */
    open suspend fun sendReply(
        vararg sendMessageChain: SendMessageChain,
        reference: Boolean = false,
        forward: Boolean = false
    ) {
        if (receiveMessage.group()) sendGroup(*sendMessageChain, reference = reference, forward = forward)
        if (receiveMessage.private()) sendPrivate(*sendMessageChain, reference = reference, forward = forward)
    }

}