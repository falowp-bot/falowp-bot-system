package com.blr19c.falowp.bot.system.json

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import kotlinx.io.readByteArray
import tools.jackson.databind.ObjectMapper
import java.nio.charset.Charset

/**
 * Ktor使用的Jackson3内容转换器
 */
class Jackson3Converter(
    private val mapper: ObjectMapper
) : ContentConverter {

    /**
     * 序列化响应内容
     */
    override suspend fun serialize(
        contentType: ContentType,
        charset: Charset,
        typeInfo: TypeInfo,
        value: Any?
    ): OutgoingContent? {
        if (value == null) return null
        val text = mapper.writeValueAsString(value)
        return TextContent(text, contentType.withCharset(charset))
    }

    /**
     * 反序列化请求内容
     */
    override suspend fun deserialize(
        charset: io.ktor.utils.io.charsets.Charset,
        typeInfo: TypeInfo,
        content: ByteReadChannel
    ): Any? {
        val bytes = content.readRemaining().readByteArray()
        val valueType = mapper.typeFactory.constructType(typeInfo.reifiedType)
        return mapper.readValue(bytes, valueType)
    }
}
