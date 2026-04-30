package com.blr19c.falowp.bot.system.api

/**
 * 机器人自身信息
 */
interface BotSelf {
    /**
     * 机器人ID
     */
    val id: String

    /**
     * 默认机器人自身信息
     */
    data class Default(override val id: String = "") : BotSelf
}