package com.blr19c.falowp.bot.system.api

import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.plugin.HookPluginRegister
import com.blr19c.falowp.bot.system.plugin.HookPluginRegisterMatch
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.UnRegister
import com.blr19c.falowp.bot.system.plugin.event.EventManager
import com.blr19c.falowp.bot.system.plugin.hook.HookJoinPoint
import com.blr19c.falowp.bot.system.plugin.hook.HookTypeEnum
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
     * 注册钩子
     * 注意请在hook中使用this.botApi而不要要使用注册hook的botApi
     */
    inline fun <reified T : Plugin.Listener.Hook> hook(
        hookType: HookTypeEnum,
        order: Int = 0,
        match: HookPluginRegisterMatch = HookPluginRegisterMatch.allMatch(),
        noinline block: suspend HookJoinPoint.(T) -> Unit
    ): UnRegister {
        val hook = HookPluginRegister(order, T::class, hookType, match, block)
        hook.register()
        return hook
    }

    /**
     * 发送群聊消息
     * @param sendMessage 需要发送的消息
     * @param reference 是否引用原消息
     * @param forward 是否已转发形式发送
     */
    abstract suspend fun sendGroup(
        vararg sendMessage: SendMessage,
        reference: Boolean = false,
        forward: Boolean = false
    )

    /**
     * 发送给所有群聊
     * @param sendMessage 需要发送的消息
     * @param reference 是否引用原消息
     * @param forward 是否已转发形式发送
     */
    abstract suspend fun sendAllGroup(
        vararg sendMessage: SendMessage,
        reference: Boolean = false,
        forward: Boolean = false
    )

    /**
     * 发送私聊消息
     * @param sendMessage 需要发送的消息
     * @param reference 是否引用原消息
     */
    abstract suspend fun sendPrivate(
        vararg sendMessage: SendMessage,
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
     * @param sendMessage 需要发送的消息
     * @param reference 是否引用原消息
     */
    open suspend fun sendReply(
        vararg sendMessage: SendMessage,
        reference: Boolean = false,
        forward: Boolean = false
    ) {
        if (receiveMessage.group()) sendGroup(*sendMessage, reference = reference, forward = forward)
        if (receiveMessage.private()) sendPrivate(*sendMessage, reference = reference, forward = forward)
    }

}