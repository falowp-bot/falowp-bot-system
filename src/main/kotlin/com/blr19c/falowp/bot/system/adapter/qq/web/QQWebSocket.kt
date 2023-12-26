package com.blr19c.falowp.bot.system.adapter.qq.web

import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.adapter.AdapterApplication
import com.blr19c.falowp.bot.system.adapter.qq.api.QQBotApi
import com.blr19c.falowp.bot.system.adapter.qq.api.QQBotApiSupport
import com.blr19c.falowp.bot.system.adapter.qq.op.OpCodeEnum
import com.blr19c.falowp.bot.system.adapter.qq.op.OpCodeEnum.*
import com.blr19c.falowp.bot.system.adapter.qq.op.OpException
import com.blr19c.falowp.bot.system.adapter.qq.op.OpReceiveMessage
import com.blr19c.falowp.bot.system.api.ApiAuth
import com.blr19c.falowp.bot.system.api.MessageSubTypeEnum
import com.blr19c.falowp.bot.system.api.MessageTypeEnum
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.image.ImageUrl
import com.blr19c.falowp.bot.system.image.toImageUrl
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.plugin.PluginManagement
import com.blr19c.falowp.bot.system.systemConfigListProperty
import com.blr19c.falowp.bot.system.systemConfigProperty
import com.blr19c.falowp.bot.system.web.bodyAsMap
import com.blr19c.falowp.bot.system.web.webclient
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

object QQWebSocket : Log {

    suspend fun configure() {
        createWebSocketSession()
    }

    private suspend fun createWebSocketSession() {
        autoReconnectCreateWebSocket(
            address = websocketAddress(),
            //最新一次消息的s&sessionId
            loopParameters = (AtomicReference<Int>() to AtomicReference<String>()),
            //首次鉴权
            firstBlock = { authentication(this) },
            //重连发送恢复连接
            reconnectBlock = { (lastS, sessionId) -> resumeAuthentication(this, lastS, sessionId) },
            block = { (lastS, sessionId) ->
                //心跳
                heartbeat(this, lastS)
                //处理消息
                dealMessage(this, lastS, sessionId)
            }
        )
    }

    /**
     * 自动重连的WebSocketClient
     * @param loopParameters 循环参数
     * @param firstBlock 首次连接执行
     * @param reconnectBlock 重新连接执行
     * @param block 每次连接均执行
     * @param reconnectCount 重连次数首次为0
     */
    private suspend fun <C> autoReconnectCreateWebSocket(
        address: String,
        loopParameters: C,
        firstBlock: suspend DefaultClientWebSocketSession.(C) -> Unit,
        reconnectBlock: suspend DefaultClientWebSocketSession.(C) -> Unit,
        block: suspend DefaultClientWebSocketSession.(C) -> Unit,
        reconnectCount: Long = 0
    ) {
        try {
            webclient().webSocket(address) {
                if (reconnectCount == 0L) {
                    firstBlock(this, loopParameters)
                    AdapterApplication.onload()
                } else reconnectBlock(this, loopParameters)
                block(this, loopParameters)
            }
        } catch (e: Exception) {
            val delay = 1000 * minOf(reconnectCount, 30)
            errorLog("websocket连接中断将在${delay}ms后重新连接", e)
            delay(delay)
            autoReconnectCreateWebSocket(
                address,
                loopParameters,
                firstBlock,
                reconnectBlock,
                block,
                reconnectCount + 1
            )
        }
    }

    /**
     * 处理消息
     */
    private suspend fun dealMessage(
        webSocketSession: DefaultClientWebSocketSession,
        lastS: AtomicReference<Int>,
        sessionId: AtomicReference<String>
    ) {
        for (frame in webSocketSession.incoming) {
            val message = frame.readMap()
            //保存s
            saveLastS(message, lastS)
            saveSessionId(message, sessionId)
            if (listOf("READY", "RESUMED").contains(message["t"])) continue
            handlerMessage(frame, message)
        }
    }

    private suspend fun handlerMessage(frame: Frame, message: Map<String, Any>) {
        val opCodeEnum = OpCodeEnum.valueOfCode(message["op"].toString().toInt())
        when (opCodeEnum) {
            DISPATCH -> dispatchMessage(frame.readBytes())
            RECONNECT -> throw OpException("服务器要求客户端重新连接")
            INVALID_SESSION -> log().error("认证失败")
            RESUME, IDENTIFY, HEARTBEAT, HELLO, HEARTBEAT_ACK, HTTP_CALLBACK_ACK -> Unit
        }
    }

    private suspend fun dispatchMessage(readBytes: ByteArray) {
        val opReceiveMessage = Json.readObj(readBytes, OpReceiveMessage::class)
        if (opReceiveMessage.isDirect()) return
        log().info("QQ适配器接收到消息:{}", opReceiveMessage)
        val atList = opReceiveMessage.d.content.at
        val guildId = opReceiveMessage.d.guildId
        val message = opReceiveMessage.d.content.message.trim()
        val imageList = opReceiveMessage.d.attachments
        val content = ReceiveMessage.Content(
            message,
            atList(guildId, atList),
            imageList(imageList),
            emptyList()
        )
        val sender = ReceiveMessage.User(
            opReceiveMessage.d.author.id,
            opReceiveMessage.d.member.nick,
            apiAuth(opReceiveMessage.d.member.roles, opReceiveMessage.d.author.id),
            opReceiveMessage.d.author.avatar.toImageUrl()
        )
        val source = ReceiveMessage.Source(opReceiveMessage.d.channelId)
        val self = ReceiveMessage.Self(QQBotApiSupport.selfId)
        val messageId = opReceiveMessage.d.id
        val messageType = MessageTypeEnum.GROUP
        val subType = MessageSubTypeEnum.MESSAGE
        val receiveMessage = ReceiveMessage(messageId, messageType, subType, content, sender, source, self)
        PluginManagement.message(receiveMessage, QQBotApi::class)
    }

    private fun imageList(imageList: List<OpReceiveMessage.Data.Attachment>?): List<ImageUrl> {
        imageList ?: return emptyList()
        return imageList.filter { it.contentType == "image/jpeg" }
            .map { it.url.toImageUrl() }
            .toList()
    }

    private suspend fun atList(guildId: String, atList: List<String>): List<ReceiveMessage.User> {
        return atList.map {
            val userInfo = QQBotApiSupport.userInfo(guildId, it)
            ReceiveMessage.User(
                it,
                userInfo.username,
                apiAuth(userInfo.roles, userInfo.id),
                userInfo.avatar.toImageUrl()
            )
        }.toList()
    }

    private fun apiAuth(roles: List<String>, id: String): ApiAuth {
        if (systemConfigListProperty("administrator").contains(id)) {
            return ApiAuth.ADMINISTRATOR
        }
        val adminRole = listOf("2", "4", "5")
        if (roles.any { adminRole.contains(it) }) {
            return ApiAuth.MANAGER
        }
        return ApiAuth.ORDINARY_MEMBER
    }


    /**
     * 保存最后一次消息的s
     */
    private fun saveLastS(message: Map<*, *>, lastS: AtomicReference<Int>) {
        if (message["s"] != null) lastS.set(message["s"].toString().toInt())
    }

    /**
     * 保存sessionId
     */
    private fun saveSessionId(message: Map<*, *>, sessionId: AtomicReference<String>) {
        if (DISPATCH.code == message["op"] && "READY" == message["t"]) {
            val data = message["d"] as Map<*, *>
            sessionId.set(data["session_id"].toString())
        }
    }

    /**
     * 鉴权
     */
    private suspend fun authentication(webSocketSession: DefaultClientWebSocketSession) {
        val authMap = mapOf(
            "op" to IDENTIFY,
            "d" to mapOf(
                "token" to QQBotApiSupport.token,
                "intents" to (0 or (1 shl 30) or (1 shl 12)),
                "shard" to intArrayOf(0, 1)
            )
        )
        webSocketSession.sendMessage(authMap)
    }

    /**
     * 重新连接鉴权
     */
    private suspend fun resumeAuthentication(
        webSocketSession: DefaultClientWebSocketSession,
        lastS: AtomicReference<Int>,
        sessionId: AtomicReference<String>
    ) {
        val authMap = mapOf(
            "op" to RESUME,
            "d" to mapOf(
                "token" to QQBotApiSupport.token,
                "session_id" to sessionId.get(),
                "seq" to lastS.get()
            )
        )
        webSocketSession.sendMessage(authMap)
    }

    /**
     * 定期发送心跳数据
     */
    private fun heartbeat(webSocketSession: DefaultClientWebSocketSession, lastS: AtomicReference<Int>) {
        webSocketSession.async {
            while (true) {
                delay(TimeUnit.SECONDS.toMillis(5))
                val map = mapOf("op" to HEARTBEAT, "d" to lastS.get())
                webSocketSession.sendMessage(map, false)
            }
        }.start()
    }

    /**
     * 获取websocket连接地址
     */
    private suspend fun websocketAddress(): String {
        return webclient().get(systemConfigProperty("adapter.qq.websocketAddressUrl")) {
            header(HttpHeaders.Authorization, QQBotApiSupport.token)
        }.bodyAsMap<String, String>()["url"]!!
    }

    private suspend fun WebSocketSession.sendMessage(data: Any, showLog: Boolean = true) {
        val json = if (data is String) data else Json.toJsonString(data)
        if (showLog) log().info("发送消息:$json")
        this.send(json)
    }

    private fun Frame.readMap(): Map<String, Any> {
        if (this is Frame.Text) {
            return Json.readMap(this.readText())
        }
        return Json.readMap(this.readBytes())
    }

    private fun errorLog(message: String, e: Exception) {
        if (e is OpException) log().info("$message, message:${e.message}")
        else log().error(message, e)
    }
}

