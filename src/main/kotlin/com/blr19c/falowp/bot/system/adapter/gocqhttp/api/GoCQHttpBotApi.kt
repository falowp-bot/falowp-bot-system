package com.blr19c.falowp.bot.system.adapter.gocqhttp.api

import com.blr19c.falowp.bot.system.adapter.gocqhttp.web.GoCQHttpWebSocket
import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SendMessage
import com.blr19c.falowp.bot.system.image.ImageUrl
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.systemConfigProperty
import com.blr19c.falowp.bot.system.web.longTimeoutWebclient
import com.fasterxml.jackson.databind.node.ArrayNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass

/**
 * GoCQHttpBotApi
 */
class GoCQHttpBotApi(receiveMessage: ReceiveMessage, originalClass: KClass<*>) : BotApi(receiveMessage, originalClass) {

    override suspend fun sendGroup(vararg sendMessage: SendMessage, reference: Boolean, forward: Boolean) {
        //当forward时reference失效
        if (forward) return sendForwardMessage(
            receiveMessage.source.id,
            *sendMessage,
            action = "send_group_forward_msg"
        )
        sendMessage.forEach { sendGroup(receiveMessage.source.id, it, reference) }
    }

    override suspend fun sendAllGroup(vararg sendMessage: SendMessage, reference: Boolean, forward: Boolean) {
        for (groupId in GoCqHttpBotApiSupport.groupIdList) {
            if (forward) sendForwardMessage(groupId, *sendMessage, action = "send_group_forward_msg")
            else sendMessage.forEach { sendGroup(groupId, it, reference) }
        }
    }

    override suspend fun sendPrivate(vararg sendMessage: SendMessage, reference: Boolean, forward: Boolean) {
        if (forward) return sendForwardMessage(
            receiveMessage.source.id,
            *sendMessage,
            action = "send_private_forward_msg"
        )
        sendMessage.forEach { sendPrivate(it, reference) }
    }

    private suspend fun sendGroup(groupId: String, sendMessage: SendMessage, reference: Boolean) {
        log().info("GoCQHttp适配器发送群组消息:{}", sendMessage)
        val body = mapOf(
            "action" to "send_group_msg",
            "params" to mapOf(
                "message_type" to "group",
                "group_id" to groupId,
                "message" to buildMessage(sendMessage, reference)
            )
        )
        GoCQHttpWebSocket.webSocketSession().send(Json.toJsonString(body))
    }

    private suspend fun sendPrivate(sendMessage: SendMessage, reference: Boolean) {
        log().info("GoCQHttp适配器发送私聊消息:{}", sendMessage)
        val body = mapOf(
            "action" to "send_private_msg",
            "params" to mapOf(
                "message_type" to "private",
                "user_id" to receiveMessage.source.id,
                "message" to buildMessage(sendMessage, reference)
            )
        )
        GoCQHttpWebSocket.webSocketSession().send(Json.toJsonString(body))
    }

    private suspend fun sendForwardMessage(groupId: String, vararg sendMessage: SendMessage, action: String) {
        val body = mapOf(
            "group_id" to groupId,
            "user_id" to groupId,
            "messages" to buildForwardMessage(*sendMessage),
        )
        val url = systemConfigProperty("adapter.gocqhttp.apiUrl")
        val bodyJsonString = Json.toJsonString(body)
        log().info("GoCQHttp适配器发送转发消息:{}", bodyJsonString)
        val res = longTimeoutWebclient().post("$url/$action") {
            setBody(bodyJsonString)
        }.body<String>()
        log().info("GoCQHttp适配器发送转发消息结果:{}", res)
    }

    /**
     * 转发消息
     */
    private fun buildForwardMessage(vararg sendMessages: SendMessage): ArrayNode {
        val nickname = systemConfigProperty("nickname")
        val selfId = this.receiveMessage.self.id
        val messageNode = Json.createArrayNode()
        for (sendMessage in sendMessages) {
            val message = buildMessage(sendMessage, false)
            val forwardData = """{"type":"node","data":{"name":"$nickname","uin":"$selfId","content":"$message"}}"""
            messageNode.add(Json.readJsonNode(forwardData))
        }
        return messageNode
    }

    /**
     * 单个消息
     */
    private fun buildMessage(sendMessage: SendMessage, reference: Boolean): String {
        return atCQ(sendMessage.at)
            .plus(if (reference) replyCQ(receiveMessage.id) else "")
            .plus(if (sendMessage.poke) pokeCQ(receiveMessage.sender.id) else "")
            .plus(imageCQ(sendMessage.images))
            .plus(videoCQ(sendMessage.videos))
            .plus(sendMessage.content)
    }

    private fun atCQ(at: List<String>): String {
        return at.joinToString(" ") { "[CQ:at,qq=${it}]" }
    }

    private fun replyCQ(messageId: String): String {
        return "[CQ:reply,id=$messageId]"
    }

    private fun imageCQ(image: List<ImageUrl>): String {
        return image.joinToString(" ") {
            runBlocking {
                if (it.isUrl()) "[CQ:image,type=custom,url=${it.toUrl()},file=图片]"
                else "[CQ:image,type=custom,url=base64://${it.toBase64()},file=图片]"
            }
        }
    }

    private fun videoCQ(videos: List<String>): String {
        return videos.joinToString(" ") { "[CQ:video,file=$it]" }
    }

    private fun pokeCQ(sendId: String): String {
        return "[CQ:poke,qq=$sendId]"
    }
}