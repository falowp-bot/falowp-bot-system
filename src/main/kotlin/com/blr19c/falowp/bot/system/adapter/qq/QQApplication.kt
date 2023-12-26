package com.blr19c.falowp.bot.system.adapter.qq

import com.blr19c.falowp.bot.system.adapter.qq.web.QQWebSocket

object QQApplication {

    suspend fun configure() {
        QQWebSocket.configure()
    }
}