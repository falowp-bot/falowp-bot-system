package com.blr19c.falowp.bot.system.api

/**
 * 消息类型
 */
enum class MessageTypeEnum {

    /**
     * 消息(图片和文本消息)
     */
    MESSAGE,

    /**
     * 戳一戳(戳一戳消息)
     */
    POKE,

    /**
     * 语音消息(发送的语音)
     */
    VOICE,

    /**
     * 视频消息
     */
    VIDEO,

    /**
     * 分享内容(应用和小程序分享的消息)
     */
    SHARE,

    /**
     * 上传的文件(上传文件或者是自动转换的音频视频文件等)
     */
    FILE,

    /**
     * 其他消息
     */
    OTHER,
}