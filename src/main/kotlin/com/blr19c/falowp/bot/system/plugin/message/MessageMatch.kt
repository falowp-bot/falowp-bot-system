package com.blr19c.falowp.bot.system.plugin.message

import com.blr19c.falowp.bot.system.api.ApiAuth
import com.blr19c.falowp.bot.system.api.MessageTypeEnum
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SourceTypeEnum

/**
 * 消息类插件匹配规则
 */
data class MessageMatch(
    /**
     * 正则匹配内容
     */
    val regex: Regex? = null,
    /**
     * 权限
     */
    val auth: ApiAuth? = null,
    /**
     * 仅响应@机器人的
     */
    val atMe: Boolean? = null,
    /**
     * 匹配发送人
     */
    val sendId: List<String>? = null,
    /**
     * 匹配消息来源
     */
    val sourceType: SourceTypeEnum? = null,
    /**
     * 匹配消息类型
     */
    val messageType: MessageTypeEnum? = null,
    /**
     * 来自的适配器
     */
    val adapterId: String? = null,
    /**
     * 自定义匹配
     */
    val customBlock: ((ReceiveMessage) -> Boolean)? = null,
) {

    @Suppress("unused")
    class Build(private var match: MessageMatch = allMatch()) {

        fun atMe(enabled: Boolean = true) = apply {
            match = match.copy(atMe = enabled)
        }

        fun auth(auth: ApiAuth) = apply {
            match = match.copy(auth = auth)
        }

        fun regex(regex: Regex) = apply {
            match = match.copy(regex = regex)
        }

        fun sendId(vararg ids: String) = apply {
            match = match.copy(sendId = ids.toList())
        }

        fun sourceType(type: SourceTypeEnum) = apply {
            match = match.copy(sourceType = type)
        }

        fun messageType(type: MessageTypeEnum) = apply {
            match = match.copy(messageType = type)
        }

        fun adapterId(id: String) = apply {
            match = match.copy(adapterId = id)
        }

        fun custom(customBlock: (ReceiveMessage) -> Boolean) = apply {
            match = match.copy(customBlock = customBlock)
        }

        fun appendCustom(customBlock: (ReceiveMessage) -> Boolean) = apply {
            match = match.copy(
                customBlock = match.customBlock?.let { prev ->
                    { msg -> prev(msg) && customBlock(msg) }
                } ?: customBlock
            )
        }

        fun build(): MessageMatch = match
    }


    companion object {
        fun allMatch(): MessageMatch {
            return MessageMatch()
        }
    }

    fun checkMath(receiveMessage: ReceiveMessage): Boolean {
        return this.regex?.matches(receiveMessage.content.message) != false
                && this.sendId?.contains(receiveMessage.sender.id) != false
                && this.sourceType?.equals(receiveMessage.source.type) != false
                && this.messageType?.equals(receiveMessage.messageType) != false
                && this.adapterId?.equals(receiveMessage.adapter.id) != false
                && this.atMe?.let { receiveMessage.atMe() } != false
                && this.customBlock?.invoke(receiveMessage) != false
    }
}
