package com.blr19c.falowp.bot.system.adapter.gocqhttp.api

import com.blr19c.falowp.bot.system.api.ApiAuth
import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.cache.CacheReference
import com.blr19c.falowp.bot.system.image.ImageUrl
import com.blr19c.falowp.bot.system.scheduling.api.SchedulingBotApiSupport
import com.blr19c.falowp.bot.system.systemConfigListProperty
import com.blr19c.falowp.bot.system.systemConfigProperty
import com.blr19c.falowp.bot.system.web.bodyAsJsonNode
import com.blr19c.falowp.bot.system.web.webclient
import com.fasterxml.jackson.databind.node.ArrayNode
import io.ktor.client.request.*
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.hours

object GoCqHttpBotApiSupport : SchedulingBotApiSupport {
    val groupIdList by CacheReference(12.hours) { groupList() }
    private val friendList by CacheReference(12.hours) { friendList() }
    private val groupFriendList by CacheReference(12.hours) { groupFriendList() }

    override suspend fun supportReceive(receiveId: String): Boolean {
        return groupIdList.contains(receiveId) || friendList.any { it.id == receiveId }
    }

    override suspend fun bot(receiveId: String, originalClass: KClass<*>): BotApi {
        return GoCQHttpBotApi(ReceiveMessage.empty().copy(source = ReceiveMessage.Source(receiveId)), originalClass)
    }

    override fun order(): Int {
        return Int.MAX_VALUE
    }

    fun userInfo(userId: String): ReceiveMessage.User? {
        return friendList.find { it.id == userId } ?: groupFriendList.find { it.id == userId }
    }

    fun avatar(userId: String): ImageUrl {
        return ImageUrl("https://q1.qlogo.cn/g?b=qq&nk=$userId&s=640")
    }

    fun apiAuth(userId: String, role: String? = null): ApiAuth {
        if (systemConfigListProperty("administrator").contains(userId)) {
            return ApiAuth.ADMINISTRATOR
        }
        if (role == "owner" || role == "admin") {
            return ApiAuth.MANAGER
        }
        return ApiAuth.ORDINARY_MEMBER
    }

    private suspend fun groupList(): List<String> {
        val url = systemConfigProperty("gocqhttp.apiUrl")
        val nodeList = webclient().get("$url/get_group_list")
            .bodyAsJsonNode()["data"] as ArrayNode
        return nodeList.map { it["group_id"].asText() }.toList()
    }

    private suspend fun friendList(): List<ReceiveMessage.User> {
        val url = systemConfigProperty("gocqhttp.apiUrl")
        val nodeList = webclient()
            .get("$url/get_friend_list")
            .bodyAsJsonNode()["data"] as ArrayNode
        return nodeList.map {
            val userId = it["user_id"].asText()
            ReceiveMessage.User(userId, it["nickname"].asText(), apiAuth(userId), avatar(userId))
        }.toList()
    }

    private suspend fun groupFriendList(): List<ReceiveMessage.User> {
        val url = systemConfigProperty("gocqhttp.apiUrl")
        val list = arrayListOf<ReceiveMessage.User>()
        for (groupId in groupIdList) {
            val nodeList = webclient()
                .get("$url/get_group_member_list?group_id=$groupId")
                .bodyAsJsonNode()["data"] as ArrayNode
            list.addAll(nodeList.map {
                val userId = it["user_id"].asText()
                val nickname = it["nickname"].asText()
                val role = it["role"].asText()
                ReceiveMessage.User(userId, nickname, apiAuth(userId, role), avatar(userId))
            })
        }
        return list.toList()
    }
}
