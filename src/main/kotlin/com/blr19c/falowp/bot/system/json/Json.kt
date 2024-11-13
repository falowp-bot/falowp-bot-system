package com.blr19c.falowp.bot.system.json

import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.lang.reflect.Type
import java.time.LocalDateTime
import kotlin.reflect.KClass


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

    fun toJsonString(data: Any): String {
        return json.writeValueAsString(data)
    }

    fun <K, V> readMap(jsonData: String): Map<K, V> {
        return json.readValue(jsonData, object : TypeReference<Map<K, V>>() {})
    }

    fun <K, V> readMap(jsonData: ByteArray): Map<K, V> {
        return json.readValue(jsonData, object : TypeReference<Map<K, V>>() {})
    }

    fun readJsonNode(jsonData: String): JsonNode {
        return json.readTree(jsonData)
    }

    fun <T : Any> readObj(jsonData: String, kClass: KClass<T>): T {
        return json.readValue(jsonData, object : TypeReference<T>() {
            override fun getType(): Type {
                return kClass.java
            }
        })
    }

    fun <T : Any> readObj(byteArray: ByteArray, kClass: KClass<T>): T {
        return json.readValue(byteArray, object : TypeReference<T>() {
            override fun getType(): Type {
                return kClass.java
            }
        })
    }

    fun <T : Any> readObj(byteArray: ByteArray, typeReference: TypeReference<T>): T {
        return json.readValue(byteArray, typeReference)
    }

    fun <T : Any> readObj(treeNode: TreeNode, kClass: KClass<T>): T {
        return json.treeToValue(treeNode, object : TypeReference<T>() {
            override fun getType(): Type {
                return kClass.java
            }
        })
    }

    fun <T : Any> readObj(treeNode: TreeNode, typeReference: TypeReference<T>): T {
        return json.treeToValue(treeNode, typeReference)
    }

    fun createArrayNode(): ArrayNode {
        return json.createArrayNode()
    }
}