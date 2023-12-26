package com.blr19c.falowp.bot.system

import com.blr19c.falowp.bot.system.adapter.AdapterApplication
import com.blr19c.falowp.bot.system.database.Database
import com.blr19c.falowp.bot.system.plugin.PluginManagement
import com.blr19c.falowp.bot.system.scheduling.Scheduling
import com.blr19c.falowp.bot.system.web.WebClient
import com.blr19c.falowp.bot.system.web.WebServer
import com.blr19c.falowp.bot.system.web.Webdriver
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory


suspend fun start() = runBlocking {
    try {
        Resources.configure()
        Webdriver.configure()
        WebClient.configure()
        Database.configure()
        PluginManagement.configure()
        Scheduling.configure()
        launch { AdapterApplication.configure() }
        launch { WebServer.configure() }
    } catch (e: Exception) {
        val log = LoggerFactory.getLogger(this::class.java)
        log.error("启动失败", e)
    }
}

suspend fun main() {
    start()
}