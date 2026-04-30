package com.blr19c.falowp.bot.system.plugin

import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.api.BotSelf
import com.blr19c.falowp.bot.system.api.SendMessage
import com.blr19c.falowp.bot.system.api.SendMessageChain
import com.blr19c.falowp.bot.system.listener.events.SendMessageEvent
import com.blr19c.falowp.bot.system.listener.hooks.SendMessageHook
import com.blr19c.falowp.bot.system.plugin.hook.withPluginHook
import io.ktor.util.collections.*

/**
 * 增加插件方法
 */
class PluginBotApi(private val delegateBotApi: BotApi) :
    BotApi(delegateBotApi.receiveMessage, delegateBotApi.originalClass) {
    private val messageIds = ConcurrentSet<String>()

    /**
     * 通过插件钩子发送群聊消息
     */
    override suspend fun sendGroup(
        vararg sendMessageChain: SendMessageChain,
        sourceId: String,
        reference: Boolean,
        forward: Boolean
    ) {
        val sendMessageHook = SendMessageHook(sendMessageChain.toMutableList())
        withPluginHook(delegateBotApi, sendMessageHook) {
            val message = sendMessageHook.sendMessageChain.toTypedArray()
            publishSendMessageEvent(*message, reference = reference, forward = forward)
            delegateBotApi.sendGroup(*message, sourceId = sourceId, reference = reference, forward = forward)
        }
    }

    /**
     * 通过插件钩子发送所有群聊消息
     */
    override suspend fun sendAllGroup(vararg sendMessageChain: SendMessageChain, reference: Boolean, forward: Boolean) {
        val sendMessageHook = SendMessageHook(sendMessageChain.toMutableList())
        withPluginHook(delegateBotApi, sendMessageHook) {
            val message = sendMessageHook.sendMessageChain.toTypedArray()
            publishSendMessageEvent(*message, reference = reference, forward = forward)
            delegateBotApi.sendAllGroup(*message, reference = reference, forward = forward)
        }
    }

    /**
     * 通过插件钩子发送私聊消息
     */
    override suspend fun sendPrivate(
        vararg sendMessageChain: SendMessageChain,
        sourceId: String,
        reference: Boolean,
        forward: Boolean
    ) {
        val sendMessageHook = SendMessageHook(sendMessageChain.toMutableList())
        withPluginHook(delegateBotApi, sendMessageHook) {
            val message = sendMessageHook.sendMessageChain.toTypedArray()
            publishSendMessageEvent(*message, reference = reference, forward = forward)
            delegateBotApi.sendPrivate(*message, sourceId = sourceId, reference = reference, forward = forward)
        }
    }

    /**
     * 通过插件钩子发送文本回复
     */
    override suspend fun sendReply(vararg sendMessage: String, reference: Boolean, forward: Boolean) {
        sendReply(
            *sendMessage.map { SendMessage.builder(it).build() }.toTypedArray(),
            reference = reference,
            forward = forward
        )
    }

    /**
     * 通过插件钩子发送消息链回复
     */
    override suspend fun sendReply(vararg sendMessageChain: SendMessageChain, reference: Boolean, forward: Boolean) {
        val sendMessageHook = SendMessageHook(sendMessageChain.toMutableList())
        withPluginHook(delegateBotApi, sendMessageHook) {
            val message = sendMessageHook.sendMessageChain.toTypedArray()
            publishSendMessageEvent(*message, reference = reference, forward = forward)
            delegateBotApi.sendReply(*message, reference = reference, forward = forward)
        }
    }

    /**
     * 获取机器人自身信息
     */
    override suspend fun self(): BotSelf {
        return delegateBotApi.self()
    }

    /**
     * 发布发送消息事件
     */
    private suspend fun publishSendMessageEvent(
        vararg sendMessageChain: SendMessageChain,
        reference: Boolean,
        forward: Boolean
    ) {
        val message = sendMessageChain.filter { messageIds.add(it.id) }.toList()
        if (message.isEmpty()) return
        this.publishEvent(SendMessageEvent(message, reference, forward))
    }
}
