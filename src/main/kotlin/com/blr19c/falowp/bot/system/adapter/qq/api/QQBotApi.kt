package com.blr19c.falowp.bot.system.adapter.qq.api

import com.blr19c.falowp.bot.system.adapter.qq.op.OpMessageContent
import com.blr19c.falowp.bot.system.adapter.qq.op.OpSendMessage
import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SendMessage
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.systemConfigProperty
import com.blr19c.falowp.bot.system.web.webclient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlin.reflect.KClass

/**
 * qqBotApi
 */
class QQBotApi(receiveMessage: ReceiveMessage, originalClass: KClass<*>) : BotApi(receiveMessage, originalClass) {

    override suspend fun sendGroup(vararg sendMessage: SendMessage, reference: Boolean, forward: Boolean) {
        sendMessage.forEach { sendGroup(receiveMessage.source.id, it, reference) }
    }

    override suspend fun sendAllGroup(vararg sendMessage: SendMessage, reference: Boolean, forward: Boolean) {
        for (channelId in QQBotApiSupport.channelIdList) {
            sendMessage.forEach { sendGroup(channelId, it, reference) }
        }
    }

    override suspend fun sendPrivate(vararg sendMessage: SendMessage, reference: Boolean, forward: Boolean) {
        throw IllegalStateException("QQ适配器不支持私聊消息")
    }

    private suspend fun sendGroup(channelId: String, sendMessage: SendMessage, reference: Boolean) {
        log().info("QQ适配器发送群组消息:{}", sendMessage)
        val messageReference = if (reference) OpSendMessage.Reference(receiveMessage.id) else null
        val opMessageContent = OpMessageContent(sendMessage.content, sendMessage.at, emptyList())
        val opSendMessageList = if (sendMessage.images.isEmpty()) {
            listOf(OpSendMessage(opMessageContent, null, messageReference, receiveMessage.id))
        } else {
            sendMessage.images
                .map { it.toUrl() }
                .map { OpSendMessage(opMessageContent, it, messageReference, receiveMessage.id) }
                .toList()
        }
        for (opSendMessage in opSendMessageList) {
            log().info("QQ适配器发送群组消息-OP:{}", opSendMessage)
            val body =
                webclient().post(systemConfigProperty("adapter.qq.apiUrl") + "/channels/$channelId/messages") {
                    setBody(Json.toJsonString(opSendMessage))
                    header(HttpHeaders.Authorization, QQBotApiSupport.token)
                }.body<String>()
            log().info("QQ适配器发送群组消息-OP返回结果:{}", body)
        }
    }
}