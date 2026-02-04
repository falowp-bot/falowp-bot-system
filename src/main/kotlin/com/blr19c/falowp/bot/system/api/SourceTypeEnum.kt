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
     * 私聊消息
     */
    PRIVATE,

    /**
     * 系统内部消息
     */
    SYSTEM,

    /**
     * 未知
     */
    UNKNOWN
}