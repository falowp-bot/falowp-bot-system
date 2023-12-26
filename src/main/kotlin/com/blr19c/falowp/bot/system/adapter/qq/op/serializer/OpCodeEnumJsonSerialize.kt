package com.blr19c.falowp.bot.system.adapter.qq.op.serializer

import com.blr19c.falowp.bot.system.adapter.qq.op.OpCodeEnum
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

/**
 * OpCodeEnum序列化时改为使用code
 */
class OpCodeEnumJsonSerialize : JsonSerializer<OpCodeEnum>() {

    override fun serialize(value: OpCodeEnum, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeObject(value.code)
    }

}
