package com.blr19c.falowp.bot.system.web

import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.json.jackson3
import com.blr19c.falowp.bot.system.scheduling.api.SchedulingBotApi
import com.blr19c.falowp.bot.system.systemConfigProperty
import com.blr19c.falowp.bot.system.utils.ScanUtils
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

object WebServer : Log {
    private val routes = CopyOnWriteArrayList<RouteContext>()

    fun configure() {
        val port = systemConfigProperty("web.port").toInt()
        val server = embeddedServer(CIO, port = port) {
            module()
            registerDynamicRoute(routes)
        }
        server.start(wait = true)
    }

    fun registerRoute(route: RouteDsl.() -> Unit) {
        routes.add(RouteContext(route, ScanUtils.getLambdaCallerClass(route)))
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

private fun Application.registerDynamicRoute(routes: List<RouteContext>) {
    routing {
        routes.forEach { route ->
            val schedulingBotApi = SchedulingBotApi(route.originalClass)
            route.route(RouteDsl(this, schedulingBotApi))
        }
    }
}

data class RouteContext(val route: RouteDsl.() -> Unit, val originalClass: KClass<*>)
data class RouteDsl(val route: Route, val botApi: BotApi) : Route by route