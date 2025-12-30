package com.blr19c.falowp.bot.system.json

import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object LocalDateTimeDeserializer : ValueDeserializer<LocalDateTime>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LocalDateTime {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return LocalDateTime.parse(p.string, formatter)
    }
}