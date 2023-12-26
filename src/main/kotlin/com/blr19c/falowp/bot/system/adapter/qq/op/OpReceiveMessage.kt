package com.blr19c.falowp.bot.system.adapter.qq.op

import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI
import java.time.LocalDateTime

/**
 * Op事件消息
 */
data class OpReceiveMessage(
    /**
     * 消息类型
     */
    val op: OpCodeEnum,

    /**
     * 序列号
     */
    val s: Int,

    /**
     * 事件类型
     */
    val t: String,

    /**
     * 事件id
     */
    val id: String,

    /**
     * 内容
     */
    val d: Data
) {
    /**
     * 是否为私聊
     */
    fun isDirect(): Boolean {
        return this.t == "DIRECT_MESSAGE_CREATE"
    }

    data class Data(
        /**
         * 消息id
         */
        val id: String,

        /**
         * (子)频道消息序号
         */
        @field:JsonProperty("seq_in_channel")
        val seq: Int,

        /**
         * 频道id
         */
        @field:JsonProperty("guild_id")
        val guildId: String,

        /**
         * (子)频道id
         */
        @field:JsonProperty("channel_id")
        val channelId: String,

        /**
         * 消息内容
         */
        val content: OpMessageContent,

        /**
         * 附加资源
         */
        val attachments: List<Attachment>?,

        /**
         * 消息的发送人信息
         */
        val author: User,

        /**
         * 消息的发送人的频道会员信息
         */
        val member: Member
    ) {

        data class Attachment(
            /**
             * 资源id
             */
            val id: String,

            /**
             * 资源地址
             */
            val url: URI,

            /**
             * 资源ContentType
             */
            @field:JsonProperty("content_type")
            val contentType: String,

            /**
             * 资源名称
             */
            val filename: String,

            /**
             * 资源大小
             */
            val size: Long,

            /**
             * 如果是图片,则存在图片属性height
             */
            val height: Int?,

            /**
             * 如果是图片,则存在图片属性width
             */
            val width: Int?
        )

        data class User(
            /**
             * 头像
             */
            val avatar: URI,

            /**
             * 是否为机器人
             */
            val bot: Boolean,

            /**
             * 用户在频道中的id
             */
            val id: String,

            /**
             * 用户昵称
             */
            val username: String,
        )

        data class Member(
            /**
             * 加入频道时间
             */
            @field:JsonProperty("joined_at")
            val joinedAt: LocalDateTime,

            /**
             * 用户的昵称
             */
            val nick: String,

            /**
             * 在频道的身份
             */
            val roles: List<String>,
        )
    }
}



