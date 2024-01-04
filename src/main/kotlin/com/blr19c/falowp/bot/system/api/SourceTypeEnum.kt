package com.blr19c.falowp.bot.system.api

/**
 * 消息来源
 */
enum class SourceTypeEnum {

    /**
     * 群聊消息
     */
    GROUP,

    /**
     * 频道消息
     */
    CHANNEL,

    /**
     * 私聊消息
     */
    PRIVATE,

    /**
     * 未知
     */
    UNKNOWN
}