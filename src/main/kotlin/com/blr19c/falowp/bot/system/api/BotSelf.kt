package com.blr19c.falowp.bot.system.api

/**
 * 机器人自身信息
 */
interface BotSelf {
    /**
     * 机器人ID
     */
    val id: String

    data class Default(override val id: String = "") : BotSelf
}