package com.blr19c.falowp.bot.system.censor

import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.cache.CacheReference
import com.blr19c.falowp.bot.system.systemConfigProperty
import com.blr19c.falowp.bot.system.web.bodyAsJsonNode
import com.blr19c.falowp.bot.system.web.webclient
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import io.ktor.client.request.*
import io.ktor.http.*
import kotlin.time.Duration.Companion.days

/**
 * 百度文本审查
 */
object BaiduTextCensor : Log {
    private val token by CacheReference(1.days) {
        getToken()
    }

    /**
     * 审查
     *
     * @param content 审查内容
     * @param userId 标记的userID
     */
    suspend fun censor(content: String, userId: String? = null): CensorResult {
        try {
            if (content.isBlank()) {
                return CensorResult(CensorConclusionTypeEnum.COMPLIANT)
            }
            val body = webclient().post(systemConfigProperty("censor.baidu.censorUrl")) {
                contentType(ContentType.Application.FormUrlEncoded)
                parameter("access_token", token)
                parameter("text", content)
                userId?.let { parameter("userId", userId) }
            }.bodyAsJsonNode()
            val conclusionType = censorConclusionTypeEnum(body["conclusionType"].asInt())
            //没有明细
            if (!body.hasArrayNode("data"))
                return CensorResult(conclusionType)
            val detail = body["data"] as ArrayNode
            val items = detail.map { itemDetail(it) }.toList()
            return CensorResult(conclusionType, items)
        } catch (e: Exception) {
            log().info("百度文本审查失败", e)
            return CensorResult(CensorConclusionTypeEnum.AUDIT_FAILURE)
        }
    }

    private fun itemDetail(item: JsonNode): CensorResultItem {
        val type = censorConclusionTypeEnum(item["conclusionType"].asInt())
        val subType = censorConclusionSubTypeEnum(item["type"].asInt(), item["subType"].asInt())
        if (!item.hasArrayNode("hits"))
            return CensorResultItem(type, subType)
        val items = (item["hits"] as ArrayNode).mapNotNull { hit ->
            if (!hit.hasArrayNode("words"))
                return@mapNotNull null
            val words = (hit["words"] as ArrayNode).map { it.asText() }.toList()
            val probability = hit["probability"]?.asText()?.toDouble()
            CensorResultItemHit(words, probability)
        }.toList()
        return CensorResultItem(type, subType, items)
    }

    private fun JsonNode.hasArrayNode(key: String): Boolean {
        return this.has(key) && !this[key].isEmpty && this[key] is ArrayNode
    }


    private fun censorConclusionTypeEnum(conclusionType: Int): CensorConclusionTypeEnum {
        return when (conclusionType) {
            1 -> CensorConclusionTypeEnum.COMPLIANT
            2 -> CensorConclusionTypeEnum.NON_COMPLIANT
            3 -> CensorConclusionTypeEnum.SUSPICIOUS
            else -> CensorConclusionTypeEnum.AUDIT_FAILURE
        }
    }

    private fun censorConclusionSubTypeEnum(type: Int, conclusionSubType: Int): CensorConclusionSubTypeEnum {
        if (type != 12) return CensorConclusionSubTypeEnum.OTHER
        return when (conclusionSubType) {
            0 -> CensorConclusionSubTypeEnum.LOW_QUALITY
            2 -> CensorConclusionSubTypeEnum.PORNOGRAPHY
            4 -> CensorConclusionSubTypeEnum.MALICIOUS_PROMOTION
            5 -> CensorConclusionSubTypeEnum.VULGAR_ABUSE
            7 -> CensorConclusionSubTypeEnum.PRIVACY_VIOLATION
            else -> CensorConclusionSubTypeEnum.OTHER
        }
    }

    private suspend fun getToken(): String {
        return webclient().post(systemConfigProperty("censor.baidu.tokenUrl")) {
            parameter("client_id", systemConfigProperty("censor.baidu.clientId"))
            parameter("client_secret", systemConfigProperty("censor.baidu.clientSecret"))
            parameter("grant_type", "client_credentials")
        }.bodyAsJsonNode()["access_token"].asText()
    }
}