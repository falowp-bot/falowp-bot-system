package com.blr19c.falowp.bot.system.adapter.qq.op

import com.blr19c.falowp.bot.system.adapter.qq.op.serializer.OpMessageContentJsonDeserializer
import com.blr19c.falowp.bot.system.adapter.qq.op.serializer.OpMessageContentJsonSerializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize

/**
 * 消息内容
 */
@JsonSerialize(using = OpMessageContentJsonSerializer::class)
@JsonDeserialize(using = OpMessageContentJsonDeserializer::class)
data class OpMessageContent(
    val message: String,
    val at: List<String>,
    val channel: List<String>
) {

    fun fullMessage(): String {
        return channel.joinToString { "<#$it>" }
            .plus(" ")
            .plus(at.joinToString { "<@!$it>" })
            .plus(" $message")
    }

    companion object {
        fun of(data: String): OpMessageContent {
            val atRegex = """<@!?(\d+)>""".toRegex()
            val channelRegex = """<#(\d+)>""".toRegex()
            val atMatches = atRegex.findAll(data)
            val channelMatches = channelRegex.findAll(data)
            val atList = atMatches.map { it.groupValues[1] }.toList()
            val channelList = channelMatches.map { it.groupValues[1] }.toList()
            val message = data.replace(atRegex, "").replace(channelRegex, "")
            return OpMessageContent(message, atList, channelList)
        }
    }
}