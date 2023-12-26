package com.blr19c.falowp.bot.system.adapter.qq.op.serializer

import com.blr19c.falowp.bot.system.adapter.qq.op.OpMessageContent
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

class OpMessageContentJsonDeserializer : JsonDeserializer<OpMessageContent>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): OpMessageContent {
        return OpMessageContent.of(p.text)
    }
}