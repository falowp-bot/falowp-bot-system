package com.blr19c.falowp.bot.system.adapter.gocqhttp

import com.blr19c.falowp.bot.system.adapter.gocqhttp.web.GoCQHttpWebSocket

object GoCQHttpApplication {

    fun configure() {
        GoCQHttpWebSocket.configure()
    }
}