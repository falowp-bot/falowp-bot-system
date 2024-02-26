package com.blr19c.falowp.bot.system.adapter.gocqhttp.web

import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.adapter.AdapterApplication
import com.blr19c.falowp.bot.system.adapter.gocqhttp.api.GoCQHttpBotApi
import com.blr19c.falowp.bot.system.adapter.gocqhttp.api.GoCQHttpEchoMessage
import com.blr19c.falowp.bot.system.adapter.gocqhttp.api.GoCQHttpMessage
import com.blr19c.falowp.bot.system.adapter.gocqhttp.api.GoCqHttpBotApiSupport
import com.blr19c.falowp.bot.system.api.MessageTypeEnum
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SourceTypeEnum
import com.blr19c.falowp.bot.system.expand.ImageUrl
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.plugin.PluginManagement
import com.blr19c.falowp.bot.system.systemConfigListProperty
import com.blr19c.falowp.bot.system.systemConfigProperty
import com.fasterxml.jackson.databind.JsonNode
import com.google.common.base.Strings
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
        embeddedServer(Netty, port = systemConfigProperty("adapter.gocqhttp.port").toInt()) {
            config()
        }.start(wait = systemConfigListProperty("adapter.enableAdapter").size == 1)
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
        PluginManagement.message(parseMessage(goCQHttpMessage), GoCQHttpBotApi::class)
    }

    private fun parseMessage(goCQHttpMessage: GoCQHttpMessage): ReceiveMessage {
        val userId = goCQHttpMessage.userId!!
        val content = parseMessageContent(goCQHttpMessage)
        val sender = ReceiveMessage.User(
            userId,
            Strings.emptyToNull(goCQHttpMessage.sender?.card) ?: goCQHttpMessage.sender?.nickname ?: "",
            GoCqHttpBotApiSupport.apiAuth(userId, goCQHttpMessage.sender?.role),
            GoCqHttpBotApiSupport.avatar(userId)
        )
        val source = ReceiveMessage.Source(goCQHttpMessage.groupId ?: userId, messageTypeEnum(goCQHttpMessage))
        val self = ReceiveMessage.Self(goCQHttpMessage.selfId!!)
        val messageId = goCQHttpMessage.messageId ?: UUID.randomUUID().toString()
        val messageType = if (goCQHttpMessage.subType == "poke") MessageTypeEnum.POKE else MessageTypeEnum.MESSAGE
        return ReceiveMessage(messageId, messageType, content, sender, source, self)
    }

    private fun parseMessageContent(goCQHttpMessage: GoCQHttpMessage): ReceiveMessage.Content {
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

        //处理引用
        val referenceRegex = Regex("\\[CQ:reply,id=(\\d+)]")
        val referenceSingle = referenceRegex.findAll(notShareMessage).map { it.groupValues[1] }.singleOrNull()
        val notReferenceMessage = notShareMessage.replace(referenceRegex, "")


        val finalMessage = notReferenceMessage.trim()

        return ReceiveMessage.Content(
            finalMessage,
            atList(atList, goCQHttpMessage),
            imageList(imageList),
            shareList(shareList)
        ) { referenceSingle(referenceSingle, goCQHttpMessage) }
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

    private suspend fun referenceSingle(referenceSingle: String?, goCQHttpMessage: GoCQHttpMessage): ReceiveMessage? {
        referenceSingle ?: return null
        val originalMessage = GoCqHttpBotApiSupport.getMessage(referenceSingle)
        val finalMessage = originalMessage.copy(
            selfId = goCQHttpMessage.selfId,
            userId = originalMessage.sender?.userId
        )
        return parseMessage(finalMessage)
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


    private fun messageTypeEnum(goCQHttpMessage: GoCQHttpMessage): SourceTypeEnum {
        return if (goCQHttpMessage.messageType == "group" || !goCQHttpMessage.groupId.isNullOrBlank())
            SourceTypeEnum.GROUP
        else SourceTypeEnum.PRIVATE
    }
}