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

    /**
     * 消息匹配规则构建器
     */
    @Suppress("unused")
    class Build(private var match: MessageMatch = allMatch()) {

        /**
         * 设置是否仅匹配at机器人的消息
         */
        fun atMe(enabled: Boolean = true) = apply {
            match = match.copy(atMe = enabled)
        }

        /**
         * 设置权限匹配
         */
        fun auth(auth: ApiAuth) = apply {
            match = match.copy(auth = auth)
        }

        /**
         * 设置正则匹配
         */
        fun regex(regex: Regex) = apply {
            match = match.copy(regex = regex)
        }

        /**
         * 设置发送人匹配
         */
        fun sendId(vararg ids: String) = apply {
            match = match.copy(sendId = ids.toList())
        }

        /**
         * 设置来源类型匹配
         */
        fun sourceType(type: SourceTypeEnum) = apply {
            match = match.copy(sourceType = type)
        }

        /**
         * 设置消息类型匹配
         */
        fun messageType(type: MessageTypeEnum) = apply {
            match = match.copy(messageType = type)
        }

        /**
         * 设置适配器匹配
         */
        fun adapterId(id: String) = apply {
            match = match.copy(adapterId = id)
        }

        /**
         * 设置自定义匹配
         */
        fun custom(customBlock: (ReceiveMessage) -> Boolean) = apply {
            match = match.copy(customBlock = customBlock)
        }

        /**
         * 追加自定义匹配
         */
        fun appendCustom(customBlock: (ReceiveMessage) -> Boolean) = apply {
            match = match.copy(
                customBlock = match.customBlock?.let { prev ->
                    { msg -> prev(msg) && customBlock(msg) }
                } ?: customBlock
            )
        }

        /**
         * 构建匹配规则
         */
        fun build(): MessageMatch = match
    }


    companion object {
        /**
         * 匹配全部消息
         */
        fun allMatch(): MessageMatch {
            return MessageMatch()
        }
    }

    /**
     * 检查消息是否匹配
     */
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
