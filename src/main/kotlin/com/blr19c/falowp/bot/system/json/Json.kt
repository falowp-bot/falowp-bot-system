package com.blr19c.falowp.bot.system.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
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
        ObjectMapper()
            .registerModules(JavaTimeModule(), module)
            .registerKotlinModule()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
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