package com.blr19c.falowp.bot.system.adapter

data class BotAdapterInfo(
    val name: String,
    val clazz: Class<out BotAdapterInterface>
)
