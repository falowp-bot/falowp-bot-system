package com.blr19c.falowp.bot.system.json

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import kotlinx.io.readByteArray
import tools.jackson.databind.ObjectMapper
import java.nio.charset.Charset

class Jackson3Converter(
    private val mapper: ObjectMapper
) : ContentConverter {

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
