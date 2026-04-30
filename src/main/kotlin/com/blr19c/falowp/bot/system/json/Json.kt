@file:Suppress("UNUSED")

package com.blr19c.falowp.bot.system.json

import com.fasterxml.jackson.annotation.JsonInclude
import io.ktor.http.*
import tools.jackson.core.JsonParser
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import tools.jackson.databind.node.MissingNode
import tools.jackson.module.kotlin.KotlinModule
import tools.jackson.module.kotlin.convertValue
import java.io.InputStream
import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.util.*


/**
 * json序列化
 */
object Json {
    private val json: ObjectMapper by lazy {
        val module = SimpleModule()
        module.addDeserializer(LocalDateTime::class.java, LocalDateTimeDeserializer)
        JsonMapper.builder()
            .addModule(KotlinModule.Builder().build())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID)
            .changeDefaultPropertyInclusion { it.withValueInclusion(JsonInclude.Include.NON_NULL) }
            .changeDefaultPropertyInclusion { it.withContentInclusion(JsonInclude.Include.NON_NULL) }
            .build()
    }

    /**
     * 获取全局ObjectMapper
     */
    fun objectMapper(): ObjectMapper {
        return json
    }

    /**
     * 从JSON字符串读取对象
     */
    inline fun <reified T : Any> readObj(jsonData: String): T {
        return objectMapper().readValue(jsonData, object : TypeReference<T>() {})
    }

    /**
     * 从字节数组读取对象
     */
    inline fun <reified T : Any> readObj(jsonData: ByteArray): T {
        return objectMapper().readValue(jsonData, object : TypeReference<T>() {})
    }

    /**
     * 从JsonNode读取对象
     */
    inline fun <reified T : Any> readObj(jsonData: JsonNode): T {
        return objectMapper().treeToValue(jsonData, object : TypeReference<T>() {})
    }

    /**
     * 从Map转换对象
     */
    inline fun <reified T : Any> readObj(jsonData: Map<*, *>): T {
        return objectMapper().convertValue(jsonData, object : TypeReference<T>() {})
    }

    /**
     * 从ByteBuffer读取对象
     */
    inline fun <reified T : Any> readObj(jsonData: ByteBuffer): T {
        return objectMapper().readValue(jsonData.array(), object : TypeReference<T>() {})
    }

    /**
     * 从JsonParser读取对象
     */
    inline fun <reified T : Any> readObj(jsonData: JsonParser): T {
        return objectMapper().readValue(jsonData, object : TypeReference<T>() {})
    }

    /**
     * 从输入流读取对象
     */
    inline fun <reified T : Any> readObj(jsonData: InputStream): T {
        return objectMapper().readValue(jsonData, object : TypeReference<T>() {})
    }

    /**
     * 从Properties读取对象
     */
    inline fun <reified T : Any> readObj(jsonData: Properties): T {
        val map = jsonData.stringPropertyNames().associateWith { jsonData[it] }
        return objectMapper().convertValue(map, object : TypeReference<T>() {})
    }

    /**
     * 从JSON字符串读取JsonNode
     */
    fun readJsonNode(jsonData: String): JsonNode {
        return objectMapper().readTree(jsonData)
    }

    /**
     * 从字节数组读取JsonNode
     */
    fun readJsonNode(jsonData: ByteArray): JsonNode {
        return objectMapper().readTree(jsonData)
    }

    /**
     * 从ByteBuffer读取JsonNode
     */
    fun readJsonNode(jsonData: ByteBuffer): JsonNode {
        return objectMapper().readTree(jsonData.array())
    }

    /**
     * 从输入流读取JsonNode
     */
    fun readJsonNode(jsonData: InputStream): JsonNode {
        return objectMapper().readTree(jsonData)
    }

    /**
     * 展开可能被字符串包裹的JsonNode
     */
    fun unwrapJsonNode(jsonNode: JsonNode): JsonNode {
        return when {
            jsonNode.isMissingNode || jsonNode.isNull -> json.createObjectNode()
            jsonNode.isString -> runCatching { readJsonNode(jsonNode.safeString()) }.getOrElse { jsonNode }
            else -> jsonNode
        }
    }

    /**
     * 转为JSON字符串
     */
    fun toJsonString(data: Any): String {
        return objectMapper().writeValueAsString(data)
    }

    /**
     * 转换对象类型
     */
    inline fun <reified T> convertValue(data: Any): T {
        return objectMapper().convertValue<T>(data)
    }
}

/**
 * 安全读取字符串
 */
fun JsonNode.safeString(): String {
    return when {
        this.isMissingNode || this.isNull -> ""
        else -> this.asString()
    }
}

/**
 * 安全读取非空字符串
 */
fun JsonNode.safeStringOrNull(): String? {
    return when {
        this.isMissingNode || this.isNull -> null
        else -> this.asString().ifBlank { null }
    }
}

/**
 * 按点分路径读取节点
 */
fun JsonNode.foldPath(path: String): JsonNode {
    return path.split(".").fold(this) { node, segment -> node.path(segment) }
}

/**
 * 忽略大小写读取字段
 */
fun JsonNode.pathIgnoreCase(field: String): JsonNode {
    return this.propertyNames()
        .firstOrNull { it.equals(field, ignoreCase = true) }
        ?.let { this.path(it) }
        ?: MissingNode.getInstance()
}

/**
 * 注册Ktor客户端Jackson3序列化
 */
fun io.ktor.client.plugins.contentnegotiation.ContentNegotiationConfig.jackson3(
    contentType: ContentType = ContentType.Application.Json,
    block: ObjectMapper.() -> Unit = {}
) {
    val mapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .build()
        .apply(block)

    register(contentType, Jackson3Converter(mapper))
}

/**
 * 注册Ktor服务端Jackson3序列化
 */
fun io.ktor.server.plugins.contentnegotiation.ContentNegotiationConfig.jackson3(
    contentType: ContentType = ContentType.Application.Json,
    block: ObjectMapper.() -> Unit = {}
) {
    val mapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .build()
        .apply(block)

    register(contentType, Jackson3Converter(mapper))
}
