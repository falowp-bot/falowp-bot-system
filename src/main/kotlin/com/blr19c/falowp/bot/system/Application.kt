package com.blr19c.falowp.bot.system

import com.blr19c.falowp.bot.system.adapter.AdapterApplication
import com.blr19c.falowp.bot.system.plugin.PluginManagement
import com.blr19c.falowp.bot.system.scheduling.Scheduling
import com.blr19c.falowp.bot.system.web.WebClient
import com.blr19c.falowp.bot.system.web.WebServer
import com.blr19c.falowp.bot.system.web.Webdriver
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

/**
 * 启动
 */
@Suppress("UNUSED")
fun start() = runBlocking {
    try {
        Resources.configure()
        Webdriver.configure()
        WebClient.configure()
        PluginManagement.configure()
        Scheduling.configure()
        AdapterApplication.configure()
        WebServer.configure()
    } catch (ex: Throwable) {
        val log = LoggerFactory.getLogger(this::class.java)
        log.error("启动失败", ex)
    }
}