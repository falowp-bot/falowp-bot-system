package com.blr19c.falowp.bot.system.plugin

import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.api.SendMessage
import com.blr19c.falowp.bot.system.api.SendMessageChain
import com.blr19c.falowp.bot.system.listener.events.SendMessageEvent
import io.ktor.util.collections.*

/**
 * 增加插件方法
 */
class PluginBotApi(private val delegateBotApi: BotApi) :
    BotApi(delegateBotApi.receiveMessage, delegateBotApi.originalClass) {
    private val messageIds = ConcurrentSet<String>()

    override suspend fun sendGroup(vararg sendMessageChain: SendMessageChain, reference: Boolean, forward: Boolean) {
        publishSendMessageEvent(*sendMessageChain, reference = reference, forward = forward)
        delegateBotApi.sendGroup(*sendMessageChain, reference = reference, forward = forward)
    }

    override suspend fun sendAllGroup(vararg sendMessageChain: SendMessageChain, reference: Boolean, forward: Boolean) {
        publishSendMessageEvent(*sendMessageChain, reference = reference, forward = forward)
        delegateBotApi.sendAllGroup(*sendMessageChain, reference = reference, forward = forward)
    }

    override suspend fun sendPrivate(vararg sendMessageChain: SendMessageChain, reference: Boolean, forward: Boolean) {
        publishSendMessageEvent(*sendMessageChain, reference = reference, forward = forward)
        delegateBotApi.sendPrivate(*sendMessageChain, reference = reference, forward = forward)
    }

    override suspend fun sendReply(vararg sendMessage: String, reference: Boolean, forward: Boolean) {
        publishSendMessageEvent(
            *sendMessage.map { SendMessage.builder(it).build() }.toTypedArray(),
            reference = reference,
            forward = forward
        )
        delegateBotApi.sendReply(*sendMessage, reference = reference, forward = forward)
    }

    override suspend fun sendReply(vararg sendMessageChain: SendMessageChain, reference: Boolean, forward: Boolean) {
        publishSendMessageEvent(*sendMessageChain, reference = reference, forward = forward)
        delegateBotApi.sendReply(*sendMessageChain, reference = reference, forward = forward)
    }

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