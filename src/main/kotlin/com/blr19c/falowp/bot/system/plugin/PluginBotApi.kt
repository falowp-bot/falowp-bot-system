package com.blr19c.falowp.bot.system.plugin

import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.api.SendMessage
import com.blr19c.falowp.bot.system.listener.events.SendMessageEvent
import io.ktor.util.collections.*

/**
 * 增加插件方法
 */
class PluginBotApi(private val delegateBotApi: BotApi) :
    BotApi(delegateBotApi.receiveMessage, delegateBotApi.originalClass) {
    private val messageIds = ConcurrentSet<String>()

    override suspend fun sendGroup(vararg sendMessage: SendMessage, reference: Boolean, forward: Boolean) {
        publishSendMessageEvent(*sendMessage, reference = reference, forward = forward)
        delegateBotApi.sendGroup(*sendMessage, reference = reference, forward = forward)
    }

    override suspend fun sendAllGroup(vararg sendMessage: SendMessage, reference: Boolean, forward: Boolean) {
        publishSendMessageEvent(*sendMessage, reference = reference, forward = forward)
        delegateBotApi.sendAllGroup(*sendMessage, reference = reference, forward = forward)
    }

    override suspend fun sendPrivate(vararg sendMessage: SendMessage, reference: Boolean, forward: Boolean) {
        publishSendMessageEvent(*sendMessage, reference = reference, forward = forward)
        delegateBotApi.sendPrivate(*sendMessage, reference = reference, forward = forward)
    }

    override suspend fun sendReply(vararg sendMessage: String, reference: Boolean, forward: Boolean) {
        publishSendMessageEvent(
            *sendMessage.map { SendMessage.builder(it).build() }.toTypedArray(),
            reference = reference,
            forward = forward
        )
        delegateBotApi.sendReply(*sendMessage, reference = reference, forward = forward)
    }

    override suspend fun sendReply(vararg sendMessage: SendMessage, reference: Boolean, forward: Boolean) {
        publishSendMessageEvent(*sendMessage, reference = reference, forward = forward)
        delegateBotApi.sendReply(*sendMessage, reference = reference, forward = forward)
    }


    private suspend fun publishSendMessageEvent(vararg sendMessage: SendMessage, reference: Boolean, forward: Boolean) {
        val message = sendMessage.filter { messageIds.add(it.id) }.toList()
        if (message.isEmpty()) return
        this.publishEvent(SendMessageEvent(message, reference, forward))
    }
}