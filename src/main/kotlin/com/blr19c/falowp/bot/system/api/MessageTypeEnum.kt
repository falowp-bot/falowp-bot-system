package com.blr19c.falowp.bot.system.api

/**
 * 消息类型
 */
enum class MessageTypeEnum {

    /**
     * 消息
     */
    MESSAGE,

    /**
     * 撤回消息
     */
    WITHDRAW_MESSAGE,

    /**
     * 戳一戳
     */
    POKE,

    /**
     * 新用户进群
     */
    GROUP_INCREASE,

    /**
     * 用户退群
     */
    GROUP_DECREASE,
}