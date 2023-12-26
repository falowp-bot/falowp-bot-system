package com.blr19c.falowp.bot.system.adapter.gocqhttp.api

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * GoCQHttp消息
 */
data class GoCQHttpMessage(

    /**
     * 消息类型
     */
    @field:JsonProperty("post_type")
    var postType: String? = null,

    /**
     *通知类型
     */
    @field:JsonProperty("notice_type")
    var noticeType: String? = null,

    /**
     * 消息来源
     */
    @field:JsonProperty("message_type")
    var messageType: String? = null,

    /**
     * 时间戳
     */
    @field:JsonProperty("time")
    var time: Long? = null,

    /**
     * 当前机器人QQ号
     */
    @field:JsonProperty("self_id")
    var selfId: String? = null,

    /**
     * 信息子类型（群）
     */
    @field:JsonProperty("sub_type")
    var subType: String? = null,

    /**
     * 匿名（群）
     */
    @field:JsonProperty("anonymous")
    var anonymous: String? = null,

    /**
     * 群号（群）
     */
    @field:JsonProperty("group_id")
    var groupId: String? = null,

    /**
     * 消息序号
     */
    @field:JsonProperty("message_seq")
    var messageSeq: Long? = null,

    /**
     * 发送人信息
     */
    @field:JsonProperty("sender")
    var sender: Sender? = null,

    /**
     * 消息ID（群）
     */
    @field:JsonProperty("message_id")
    var messageId: String? = null,

    /**
     * 字体
     */
    @field:JsonProperty("font")
    var font: Int? = null,

    /**
     * 消息
     */
    @field:JsonProperty("message")
    var message: String? = null,

    /**
     * 消息文本
     */
    @field:JsonProperty("raw_message")
    var rawMessage: String? = null,

    /**
     * 发送人QQ号
     */
    @field:JsonProperty("user_id")
    var userId: String? = null,

    /**
     * 操作人（仅在notice情况下存在）
     */
    @field:JsonProperty("operator_id")
    var operatorId: String? = null,

    /**
     * 发送人QQ号（仅在notice情况下存在）
     */
    @field:JsonProperty("sender_id")
    var senderId: String? = null,

    /**
     * 目标人QQ号（仅在notice情况下存在）
     */
    @field:JsonProperty("target_id")
    var targetId: String? = null,

    /**
     * 回执
     */
    @field:JsonProperty("echo")
    var echo: String? = null,
) {

    /**
     * GoCQHttp消息发送人信息
     */
    data class Sender(

        @field:JsonProperty("age")
        var age: Int? = null,

        @field:JsonProperty("area")
        var area: String? = null,

        @field:JsonProperty("card")
        var card: String? = null,

        @field:JsonProperty("level")
        var level: String? = null,

        @field:JsonProperty("nickname")
        var nickname: String? = null,

        @field:JsonProperty("role")
        var role: String? = null,

        @field:JsonProperty("sex")
        var sex: String? = null,

        @field:JsonProperty("title")
        var title: String? = null,

        @field:JsonProperty("user_id")
        var userId: String? = null
    )
}