package com.blr19c.falowp.bot.system.json

import io.ktor.http.*
import tools.jackson.core.JsonParser
import tools.jackson.core.TreeNode
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import tools.jackson.module.kotlin.KotlinModule
import java.io.InputStream
import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.util.*


/**
 * json序列化
 */
@Suppress("UNUSED")
object Json {
    private val json: ObjectMapper by lazy {
        val module = SimpleModule()
        module.addDeserializer(LocalDateTime::class.java, LocalDateTimeDeserializer)
        JsonMapper.builder()
            .addModule(KotlinModule.Builder().build())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
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

    fun toJsonString(data: Any): String {
        return objectMapper().writeValueAsString(data)
    }
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