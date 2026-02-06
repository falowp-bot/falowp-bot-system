@file:Suppress("UNUSED")

package com.blr19c.falowp.bot.system.json

import com.fasterxml.jackson.annotation.JsonInclude
import io.ktor.http.*
import tools.jackson.core.JsonParser
import tools.jackson.core.TreeNode
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

    fun objectMapper(): ObjectMapper {
        return json
    }

    inline fun <reified T : Any> readObj(jsonData: String): T {
        return objectMapper().readValue(jsonData, object : TypeReference<T>() {})
    }

    inline fun <reified T : Any> readObj(jsonData: ByteArray): T {
        return objectMapper().readValue(jsonData, object : TypeReference<T>() {})
    }

    inline fun <reified T : Any> readObj(jsonData: TreeNode): T {
        return objectMapper().treeToValue(jsonData, object : TypeReference<T>() {})
    }

    inline fun <reified T : Any> readObj(jsonData: Map<*, *>): T {
        return objectMapper().convertValue(jsonData, object : TypeReference<T>() {})
    }

    inline fun <reified T : Any> readObj(jsonData: ByteBuffer): T {
        return objectMapper().readValue(jsonData.array(), object : TypeReference<T>() {})
    }

    inline fun <reified T : Any> readObj(jsonData: JsonParser): T {
        return objectMapper().readValue(jsonData, object : TypeReference<T>() {})
    }

    inline fun <reified T : Any> readObj(jsonData: InputStream): T {
        return objectMapper().readValue(jsonData, object : TypeReference<T>() {})
    }

    inline fun <reified T : Any> readObj(jsonData: Properties): T {
        val map = jsonData.stringPropertyNames().associateWith { jsonData[it] }
        return objectMapper().convertValue(map, object : TypeReference<T>() {})
    }

    fun readJsonNode(jsonData: String): JsonNode {
        return objectMapper().readTree(jsonData)
    }

    fun readJsonNode(jsonData: ByteArray): JsonNode {
        return objectMapper().readTree(jsonData)
    }

    fun readJsonNode(jsonData: ByteBuffer): JsonNode {
        return objectMapper().readTree(jsonData.array())
    }

    fun readJsonNode(jsonData: InputStream): JsonNode {
        return objectMapper().readTree(jsonData)
    }

    fun unwrapJsonNode(jsonNode: JsonNode): JsonNode {
        return when {
            jsonNode.isMissingNode || jsonNode.isNull -> json.createObjectNode()
            jsonNode.isString -> runCatching { readJsonNode(jsonNode.safeString()) }.getOrElse { jsonNode }
            else -> jsonNode
        }
    }

    fun toJsonString(data: Any): String {
        return objectMapper().writeValueAsString(data)
    }

    inline fun <reified T> convertValue(data: Any): T {
        return objectMapper().convertValue<T>(data)
    }
}

fun JsonNode.safeString(): String {
    return when {
        this.isMissingNode || this.isNull -> ""
        else -> this.asString()
    }
}

fun JsonNode.safeStringOrNull(): String? {
    return when {
        this.isMissingNode || this.isNull -> null
        else -> this.asString().ifBlank { null }
    }
}

fun JsonNode.foldPath(path: String): JsonNode {
    return path.split(".").fold(this) { node, segment -> node.path(segment) }
}

fun JsonNode.pathIgnoreCase(field: String): JsonNode {
    return this.propertyNames()
        .firstOrNull { it.equals(field, ignoreCase = true) }
        ?.let { this.path(it) }
        ?: MissingNode.getInstance()
}

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