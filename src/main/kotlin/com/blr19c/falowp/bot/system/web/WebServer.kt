package com.blr19c.falowp.bot.system.web

import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.json.jackson3
import com.blr19c.falowp.bot.system.systemConfigProperty
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import java.util.concurrent.CopyOnWriteArrayList

object WebServer : Log {
    private val routes = CopyOnWriteArrayList<Route.() -> Unit>()

    fun configure() {
        val port = systemConfigProperty("web.port").toInt()
        val server = embeddedServer(CIO, port = port) {
            module()
            registerDynamicRoute(routes)
        }
        server.start(wait = true)
    }

    fun registerRoute(route: Route.() -> Unit) {
        routes.add(route)
    }
}

private fun Application.module() {
    install(WebSockets)
    install(Authentication) {
        basic {
            realm = "falowp-bot"
            validate {
                if (it.password == systemConfigProperty("web.auth")) {
                    return@validate UserIdPrincipal("falowp-bot-user")
                }
                return@validate null
            }
        }
    }
    install(ContentNegotiation) {
        jackson3 {}
    }
    routing {
        get("/ping") {
            call.respond(System.currentTimeMillis())
        }
    }
}

private fun Application.registerDynamicRoute(routes: List<Route.() -> Unit>) {
    routing {
        routes.forEach { it() }
    }
}
