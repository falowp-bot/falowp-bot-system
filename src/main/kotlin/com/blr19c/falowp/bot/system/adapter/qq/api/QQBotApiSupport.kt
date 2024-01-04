package com.blr19c.falowp.bot.system.adapter.qq.api

import com.blr19c.falowp.bot.system.adapter.qq.op.OpUser
import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.cache.CacheMap
import com.blr19c.falowp.bot.system.cache.CacheReference
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.scheduling.api.SchedulingBotApiSupport
import com.blr19c.falowp.bot.system.systemConfigProperty
import com.blr19c.falowp.bot.system.web.bodyAsArrayNode
import com.blr19c.falowp.bot.system.web.bodyAsJsonNode
import com.blr19c.falowp.bot.system.web.webclient
import com.fasterxml.jackson.databind.node.ArrayNode
import io.ktor.client.request.*
import io.ktor.http.*
import kotlin.reflect.KClass
import kotlin.streams.asSequence
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

object QQBotApiSupport : SchedulingBotApiSupport {

    val selfId by CacheReference(1.days) { selfId() }
    val token by CacheReference(1.days) { token() }
    val channelIdList by CacheReference(1.hours) { channelIdList() }
    private val guildIdList by CacheReference(1.hours) { guildsIdList() }
    private val userInfoMap by CacheMap<Pair<String, String>, OpUser>(1.hours) { (guildId, userId) ->
        loadUserInfo(guildId, userId)
    }

    override suspend fun supportReceive(receiveId: String): Boolean {
        return channelIdList.contains(receiveId)
    }

    override suspend fun bot(receiveId: String, originalClass: KClass<*>): BotApi {
        val message = ReceiveMessage.empty().copy(source = ReceiveMessage.Source.empty().copy(id = receiveId))
        return QQBotApi(message, originalClass)
    }

    suspend fun userInfo(guildId: String, userId: String): OpUser {
        return userInfoMap(guildId to userId)
    }

    private suspend fun guildsIdList(): List<String> {
        return webclient().get(systemConfigProperty("adapter.qq.apiUrl") + "/users/@me/guilds") {
            header(HttpHeaders.Authorization, token())
        }.bodyAsArrayNode()
            .map { it["id"].asText() }
            .toList()
    }

    private suspend fun channelIdList(): List<String> {
        return guildIdList.map {
            webclient().get(systemConfigProperty("adapter.qq.apiUrl") + "/guilds/${it}/channels") {
                header(HttpHeaders.Authorization, token())
            }.bodyAsArrayNode().map { data -> data["id"].asText() }
        }.flatMap { it.stream().asSequence() }.toList()
    }


    private suspend fun selfId(): String {
        return webclient().get(systemConfigProperty("adapter.qq.apiUrl") + "/users/@me") {
            header(HttpHeaders.Authorization, token())
        }.bodyAsJsonNode()["id"].asText()
    }

    private suspend fun loadUserInfo(guildId: String, userId: String): OpUser {
        val jsonNode =
            webclient().get(systemConfigProperty("adapter.qq.apiUrl") + "/guilds/${guildId}/members/$userId") {
                header(HttpHeaders.Authorization, token())
            }.bodyAsJsonNode()
        val opUser = Json.readObj(jsonNode["user"], OpUser::class)
        val nick = jsonNode["nick"].asText()
        val roles = (jsonNode["roles"] as ArrayNode).map { it.asText() }
        return opUser.copy(nick = nick, roles = roles)
    }


    /**
     * Bot {appid}.{app_token}
     */
    private fun token(): String {
        return "Bot ${systemConfigProperty("adapter.qq.appId")}.${systemConfigProperty("adapter.qq.token")}"
    }

}