package com.blr19c.falowp.bot.system.api

import com.blr19c.falowp.bot.system.expand.ImageUrl

/**
 * 机器人自身信息
 */
interface BotSelf {
    /**
     * 机器人ID
     */
    val id: String

    /**
     * 机器人昵称
     */
    val nickname: String

    /**
     * 机器人头像
     */
    val avatar: ImageUrl

    /**
     * 默认机器人自身信息
     */
    data class Default(
        override val id: String = "",
        override val nickname: String = "",
        override val avatar: ImageUrl = ImageUrl.empty()
    ) : BotSelf
}