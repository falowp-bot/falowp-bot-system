package com.blr19c.falowp.bot.system.adapter.qq.op.serializer

import com.blr19c.falowp.bot.system.adapter.qq.op.OpCodeEnum
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

/**
 * OpCodeEnum反序列化时改为使用code
 */
class OpCodeEnumJsonDeserializer : JsonDeserializer<OpCodeEnum>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): OpCodeEnum {
        return OpCodeEnum.valueOfCode(p.intValue)
    }

}
