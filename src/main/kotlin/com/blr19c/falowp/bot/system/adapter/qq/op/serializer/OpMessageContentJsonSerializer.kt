package com.blr19c.falowp.bot.system.adapter.qq.op.serializer

import com.blr19c.falowp.bot.system.adapter.qq.op.OpMessageContent
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

class OpMessageContentJsonSerializer : JsonSerializer<OpMessageContent>() {
    override fun serialize(value: OpMessageContent, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeString(value.fullMessage())
    }
}