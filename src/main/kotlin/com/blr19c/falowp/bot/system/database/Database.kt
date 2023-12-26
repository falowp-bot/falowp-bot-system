package com.blr19c.falowp.bot.system.database

import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.systemConfigProperty

object Database : Log {

    fun configure() {
        log().info("初始化Database")
        org.jetbrains.exposed.sql.Database.connect(
            url = systemConfigProperty("database.url"),
            user = systemConfigProperty("database.username"),
            password = systemConfigProperty("database.password"),
            driver = "org.mariadb.jdbc.Driver",
        )
        init()
        log().info("初始化Database完成")
    }
}