package com.blr19c.falowp.bot.system.adapter

/**
 * 协议适配器信息
 */
data class BotAdapterInfo(
    /**
     * 协议名称
     */
    val name: String,
    /**
     * 协议适配器类
     */
    val clazz: Class<out BotAdapterInterface>
)
