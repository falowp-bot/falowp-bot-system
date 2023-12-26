package com.blr19c.falowp.bot.system.adapter.gocqhttp.web

import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.adapter.AdapterApplication
import com.blr19c.falowp.bot.system.adapter.gocqhttp.api.GoCQHttpBotApi
import com.blr19c.falowp.bot.system.adapter.gocqhttp.api.GoCQHttpEchoMessage
import com.blr19c.falowp.bot.system.adapter.gocqhttp.api.GoCQHttpMessage
import com.blr19c.falowp.bot.system.adapter.gocqhttp.api.GoCqHttpBotApiSupport
import com.blr19c.falowp.bot.system.api.MessageSubTypeEnum
import com.blr19c.falowp.bot.system.api.MessageTypeEnum
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.image.ImageUrl
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.plugin.PluginManagement
import com.blr19c.falowp.bot.system.systemConfigProperty
import com.fasterxml.jackson.databind.JsonNode
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

object GoCQHttpWebSocket : Log {

    fun configure() {
        embeddedServer(Netty, port = systemConfigProperty("gocqhttp.port").toInt()) {
            config()
        }.start(wait = false)
    }

    class GoCqHttpWebSocketSession(session: WebSocketSession) : WebSocketSession by session

    private val webSocketSession = AtomicReference<GoCqHttpWebSocketSession>()
    private val echoCache = ConcurrentHashMap<Any, Channel<GoCQHttpEchoMessage>>()
    private val onload by lazy { AdapterApplication.onload() }
    private val executor = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun webSocketSession(): GoCqHttpWebSocketSession {
        return webSocketSession.get()
    }

    private fun Application.config() {
        install(WebSockets)
        routing { webSocket("/ws") { initWebsocket() } }
    }

    private suspend fun DefaultWebSocketServerSession.initWebsocket() {
        webSocketSession.set(GoCqHttpWebSocketSession(this))
        onload
        for (frame in incoming) {
            executor.launch {
                try {
                    websocketFrame(frame)
                } catch (e: Exception) {
                    log().error("GoCQHttp适配器处理消息失败", e)
                }
            }
        }
    }

    private suspend fun websocketFrame(frame: Frame) {
        if (frame !is Frame.Text) return
        val jsonNode = Json.readJsonNode(frame.readText())
        val postType = jsonNode.findPath("post_type").asText()
        //心跳
        if (postType.isNullOrBlank() || postType == "meta_event") {
            return
        }
        //消息
        if (jsonNode.findPath("post_type").asText().isNotBlank()) {
            processMessages(Json.readObj(frame.readText(), GoCQHttpMessage::class))
        }
        //回执
        if (jsonNode.findPath("echo").asText().isNotBlank()) {
            processEcho(Json.readObj(frame.readText(), GoCQHttpEchoMessage::class))
        }
    }

    private suspend fun processEcho(message: GoCQHttpEchoMessage) {
        log().info("GoCQHttp适配器接收到echo回执消息:{}", message)
        val channel = echoCache.remove(message.echo) ?: return
        channel.send(message)
        channel.close()
    }

    private suspend fun processMessages(goCQHttpMessage: GoCQHttpMessage) {
        log().info("GoCQHttp适配器接收到消息:{}", goCQHttpMessage)
        if (goCQHttpMessage.userId.isNullOrBlank()) {
            log().info("GoCQHttp适配器接收到消息没有userId不处理")
            return
        }
        val userId = goCQHttpMessage.userId!!
        val content = parseMessage(goCQHttpMessage)
        val sender = ReceiveMessage.User(
            userId,
            goCQHttpMessage.sender?.card ?: goCQHttpMessage.sender?.nickname ?: "",
            GoCqHttpBotApiSupport.apiAuth(userId, goCQHttpMessage.sender?.role),
            GoCqHttpBotApiSupport.avatar(userId)
        )
        val source = ReceiveMessage.Source(goCQHttpMessage.groupId ?: userId)
        val self = ReceiveMessage.Self(goCQHttpMessage.selfId!!)
        val messageId = goCQHttpMessage.messageId ?: UUID.randomUUID().toString()
        val messageType = messageTypeEnum(goCQHttpMessage)
        val subType = if (goCQHttpMessage.subType == "poke") MessageSubTypeEnum.POKE else MessageSubTypeEnum.MESSAGE
        val receiveMessage = ReceiveMessage(messageId, messageType, subType, content, sender, source, self)
        PluginManagement.message(receiveMessage, GoCQHttpBotApi::class)
    }

    private fun parseMessage(goCQHttpMessage: GoCQHttpMessage): ReceiveMessage.Content {
        val cqMessage = goCQHttpMessage.message ?: return emptyMessageContent(goCQHttpMessage)
        //处理@
        val atRegex = Regex("\\[CQ:at,qq=(\\d+)]")
        val atList = atRegex.findAll(cqMessage).map { it.groupValues[1] }.toList()
        val notAtMessage = cqMessage.replace(atRegex, "")

        //处理图片
        val imageRegex = Regex("\\[CQ:image.+,url=(https?://[^\\s/\$.?#].\\S*)]")
        val imageList = imageRegex.findAll(notAtMessage).map { it.groupValues[1] }.toList()
        val notImageMessage = notAtMessage.replace(imageRegex, "")

        //处理分享
        val shareRegex = Regex("\\[CQ:json,data=([\\s\\S]*)]")
        val shareList = shareRegex.findAll(notImageMessage).map { it.groupValues[1] }.toList()
        val notShareMessage = notImageMessage.replace(shareRegex, "")

        val finalMessage = notShareMessage.trim()

        return ReceiveMessage.Content(
            finalMessage,
            atList(atList, goCQHttpMessage),
            imageList(imageList),
            shareList(shareList)
        )
    }

    private fun emptyMessageContent(goCQHttpMessage: GoCQHttpMessage): ReceiveMessage.Content {
        return ReceiveMessage.Content.empty().copy(at = atList(listOf(), goCQHttpMessage))
    }

    private fun atList(atList: List<String>, goCQHttpMessage: GoCQHttpMessage): List<ReceiveMessage.User> {
        val atUserList = atList.mapNotNull { GoCqHttpBotApiSupport.userInfo(it) }.toMutableList()
        goCQHttpMessage.targetId?.let { GoCqHttpBotApiSupport.userInfo(it) }?.let { atUserList.add(it) }
        return atUserList.toList()
    }

    private fun imageList(imageList: List<String>): List<ImageUrl> {
        return imageList.map { ImageUrl(it) }.toList()
    }

    private fun shareList(shareList: List<String>): List<ReceiveMessage.Share> {
        return shareList
            .map { replaceEscapeCharacter(it) }
            .map { Json.readJsonNode(it) }
            .mapNotNull { shareInfo(it) }
            .toList()
    }

    private fun shareInfo(jsonNode: JsonNode): ReceiveMessage.Share? {
        return if (jsonNode["app"].asText().startsWith("com.tencent.miniapp"))
            shareMiniAppStandard(jsonNode)
        else if (jsonNode["app"].asText().startsWith("com.tencent.structmsg"))
            shareStandard(jsonNode)
        else null
    }

    private fun shareMiniAppStandard(jsonNode: JsonNode): ReceiveMessage.Share {
        val appInfo = jsonNode["meta"].elements().next()
        return ReceiveMessage.Share(
            appInfo["title"].asText(),
            appInfo["desc"].asText(),
            ImageUrl(appInfo["preview"].asText()),
            appInfo["qqdocurl"].asText(),
        )
    }

    private fun shareStandard(jsonNode: JsonNode): ReceiveMessage.Share {
        val view = jsonNode["view"].asText()
        return ReceiveMessage.Share(
            jsonNode["meta"][view]["tag"].asText(),
            jsonNode["meta"][view]["title"].asText(),
            ImageUrl(jsonNode["meta"][view]["preview"].asText()),
            jsonNode["meta"][view]["jumpUrl"].asText(),
        )
    }

    private fun replaceEscapeCharacter(cqMessage: String): String {
        return cqMessage.replace("&#44;", ",")
            .replace("&amp;", "&")
            .replace("&#91;", "[")
            .replace("&#93;", "]")
    }


    private fun messageTypeEnum(goCQHttpMessage: GoCQHttpMessage): MessageTypeEnum {
        return if (goCQHttpMessage.messageType == "group" || !goCQHttpMessage.groupId.isNullOrBlank())
            MessageTypeEnum.GROUP
        else MessageTypeEnum.PRIVATE
    }
}