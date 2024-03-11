package com.blr19c.falowp.bot.system.adapter.qq.api

import com.blr19c.falowp.bot.system.adapter.qq.op.OpMessageContent
import com.blr19c.falowp.bot.system.adapter.qq.op.OpSendMessage
import com.blr19c.falowp.bot.system.api.*
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

    override suspend fun sendGroup(
        vararg sendMessageChain: SendMessageChain,
        sourceId: String,
        reference: Boolean,
        forward: Boolean
    ) {
        sendMessageChain.forEach { sendGroup(sourceId, it, reference) }
    }

    override suspend fun sendAllGroup(vararg sendMessageChain: SendMessageChain, reference: Boolean, forward: Boolean) {
        for (channelId in QQBotApiSupport.channelIdList) {
            sendMessageChain.forEach { sendGroup(channelId, it, reference) }
        }
    }

    override suspend fun sendPrivate(
        vararg sendMessageChain: SendMessageChain,
        sourceId: String,
        reference: Boolean,
        forward: Boolean
    ) {
        throw IllegalStateException("QQ适配器不支持私聊消息")
    }

    private suspend fun sendGroup(channelId: String, sendMessageChain: SendMessageChain, reference: Boolean) {
        log().info("QQ适配器发送群组消息:{}", sendMessageChain)
        val messageReference = if (reference) OpSendMessage.Reference(receiveMessage.id) else null
        val content = sendMessageChain.messageList.filterIsInstance<TextSendMessage>().joinToString("") { it.content }
        val atList = sendMessageChain.messageList.filterIsInstance<AtSendMessage>().map { it.at }.toList()
        val imageList = sendMessageChain.messageList.filterIsInstance<ImageSendMessage>().map { it.image }.toList()
        val opMessageContent = OpMessageContent(content, atList, emptyList())
        val opSendMessageList = if (imageList.isEmpty()) {
            listOf(OpSendMessage(opMessageContent, null, messageReference, receiveMessage.id))
        } else {
            imageList
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