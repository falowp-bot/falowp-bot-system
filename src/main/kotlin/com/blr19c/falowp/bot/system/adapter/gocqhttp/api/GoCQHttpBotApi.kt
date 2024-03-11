package com.blr19c.falowp.bot.system.adapter.gocqhttp.api

import com.blr19c.falowp.bot.system.api.*
import com.blr19c.falowp.bot.system.expand.ImageUrl
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.systemConfigProperty
import com.blr19c.falowp.bot.system.web.longTimeoutWebclient
import com.fasterxml.jackson.databind.node.ArrayNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass

/**
 * GoCQHttpBotApi
 */
class GoCQHttpBotApi(receiveMessage: ReceiveMessage, originalClass: KClass<*>) : BotApi(receiveMessage, originalClass) {

    private val baseUrl by lazy { systemConfigProperty("adapter.gocqhttp.apiUrl") }

    private val client by lazy { longTimeoutWebclient() }

    override suspend fun sendGroup(
        vararg sendMessageChain: SendMessageChain,
        sourceId: String,
        reference: Boolean,
        forward: Boolean
    ) {
        //当forward时reference失效
        if (forward) return sendForwardMessage(
            sourceId,
            *sendMessageChain,
            action = "send_group_forward_msg"
        )
        sendMessageChain.forEach { sendGroup(sourceId, it, reference) }
    }

    override suspend fun sendAllGroup(vararg sendMessageChain: SendMessageChain, reference: Boolean, forward: Boolean) {
        for (groupId in GoCqHttpBotApiSupport.groupIdList) {
            if (forward) sendForwardMessage(groupId, *sendMessageChain, action = "send_group_forward_msg")
            else sendMessageChain.forEach { sendGroup(groupId, it, reference) }
        }
    }

    override suspend fun sendPrivate(
        vararg sendMessageChain: SendMessageChain,
        sourceId: String,
        reference: Boolean,
        forward: Boolean
    ) {
        if (forward) return sendForwardMessage(
            sourceId,
            *sendMessageChain,
            action = "send_private_forward_msg"
        )
        sendMessageChain.forEach { sendPrivate(it, reference, sourceId) }
    }

    private suspend fun sendGroup(groupId: String, sendMessageChain: SendMessageChain, reference: Boolean) {
        log().info("GoCQHttp适配器发送群组消息:{}", sendMessageChain)
        val body = mapOf(
            "group_id" to groupId,
            "message" to buildMessage(sendMessageChain, reference)
        )
        retry {
            client.post("$baseUrl/send_group_msg") {
                setBody(body)
            }
        }
    }

    private suspend fun sendPrivate(sendMessageChain: SendMessageChain, reference: Boolean, sourceId: String) {
        log().info("GoCQHttp适配器发送私聊消息:{}", sendMessageChain)
        val body = mapOf(
            "user_id" to sourceId,
            "message" to buildMessage(sendMessageChain, reference)
        )
        retry {
            client.post("$baseUrl/send_private_msg") {
                setBody(body)
            }
        }
    }

    private suspend fun sendForwardMessage(groupId: String, vararg sendMessageChain: SendMessageChain, action: String) {
        val body = mapOf(
            "group_id" to groupId,
            "user_id" to groupId,
            "messages" to buildForwardMessage(*sendMessageChain),
        )
        val bodyJsonString = Json.toJsonString(body)
        log().info("GoCQHttp适配器发送转发消息:{}", bodyJsonString)
        val res = retry {
            client.post("$baseUrl/$action") {
                setBody(bodyJsonString)
            }
        }
        log().info("GoCQHttp适配器发送转发消息结果:{}", res)
    }

    /**
     * 转发消息
     */
    private fun buildForwardMessage(vararg sendMessageChains: SendMessageChain): ArrayNode {
        val nickname = systemConfigProperty("nickname")
        val selfId = this.receiveMessage.self.id
        val messageNode = Json.createArrayNode()
        for (sendMessageChain in sendMessageChains) {
            val message = buildMessage(sendMessageChain, false)
            val forwardData = """{"type":"node","data":{"name":"$nickname","uin":"$selfId","content":"$message"}}"""
            messageNode.add(Json.readJsonNode(forwardData))
        }
        return messageNode
    }

    private suspend fun retry(
        retryCount: Int = 2,
        oldBody: String = "",
        block: suspend () -> HttpResponse
    ): String {
        try {
            if (retryCount < 0) {
                return oldBody
            }
            val body = block.invoke().body<String>()
            val jsonBody = Json.readJsonNode(body)
            if (jsonBody["status"].asText().equals("OK", true)) {
                return body
            }
            return retry(retryCount - 1, body, block)
        } catch (e: Exception) {
            log().error("GoCQHttp-retry发送请求失败", e)
            return retry(retryCount - 1, oldBody, block)
        }
    }

    /**
     * 单个消息
     */
    private fun buildMessage(sendMessageChain: SendMessageChain, reference: Boolean): String {
        val builder = StringBuilder()
        for (sendMessage in sendMessageChain.messageList) {
            val message = when (sendMessage) {
                is AtSendMessage -> atCQ(sendMessage.at)
                is TextSendMessage -> sendMessage.content
                is ImageSendMessage -> imageCQ(sendMessage.image)
                is VideoSendMessage -> videoCQ(sendMessage.video)
                is PokeSendMessage -> pokeCQ(receiveMessage.sender.id)
                else -> ""
            }
            builder.append(message)
        }
        if (reference) {
            builder.append(replyCQ(receiveMessage.id))
        }
        return builder.toString()
    }

    private fun atCQ(at: String): String {
        return "[CQ:at,qq=${at}]"
    }

    private fun replyCQ(messageId: String): String {
        return "[CQ:reply,id=$messageId]"
    }

    private fun imageCQ(image: ImageUrl): String = runBlocking {
        if (image.isUrl()) "[CQ:image,type=custom,url=${image.toUrl()},file=图片]"
        else "[CQ:image,type=custom,url=base64://${image.toBase64()},file=图片]"
    }

    private fun videoCQ(videos: String): String {
        return "[CQ:video,file=$videos]"
    }

    private fun pokeCQ(sendId: String): String {
        return "[CQ:poke,qq=$sendId]"
    }
}