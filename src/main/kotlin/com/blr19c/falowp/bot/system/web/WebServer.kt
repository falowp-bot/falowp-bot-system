package com.blr19c.falowp.bot.system.web

import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.systemConfigProperty
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.concurrent.CopyOnWriteArrayList

data class RouteInfo(val path: String, val block: Route.() -> Unit)

object WebServer : Log {
    private val routes = CopyOnWriteArrayList<RouteInfo>()

    fun configure() {
        val port = systemConfigProperty("web.port").toInt()
        val server = embeddedServer(Netty, port = port, module = Application::module).start(wait = false)
        server.application.registerDynamicRoute(routes)
    }

    fun registerRoute(route: RouteInfo) {
        routes.add(route)
    }
}

private fun Application.module() {
    install(ContentNegotiation) {
        jackson { }
    }
    routing {
        get("/ping") {
            call.respond(System.currentTimeMillis())
        }
    }
}

private fun Application.registerDynamicRoute(routeInfos: List<RouteInfo>) {
    routing {
        routeInfos.forEach { route(it.path) { it.block.invoke(this) } }
    }
}
