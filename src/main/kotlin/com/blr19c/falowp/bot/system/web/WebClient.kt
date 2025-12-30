@file:Suppress("UNUSED")

package com.blr19c.falowp.bot.system.web

import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.json.jackson3
import com.blr19c.falowp.bot.system.readResource
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets.UTF_8
import kotlin.time.Duration.Companion.minutes

object WebClient : Log {

    fun configure() {
        log().info("初始化WebClient")
        client
        log().info("初始化WebClient完成")
    }
}

private val client by lazy(LazyThreadSafetyMode.PUBLICATION) {
    HttpClient(CIO) {
        defaultRequest {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, commonUserAgent())
        }
        install(WebSockets)
        install(ContentNegotiation) {
            jackson3 {}
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 5000
            connectTimeoutMillis = 5000
            socketTimeoutMillis = 5000
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.NONE
        }
    }
}

private val commonUserAgent by lazy(LazyThreadSafetyMode.PUBLICATION) {
    runBlocking {
        readResource("system/webclient/useragent.txt") {
            val reader = BufferedReader(InputStreamReader(it))
            reader.lines().toList()
        }
    }
}

/**
 * 获取webclient
 */
fun webclient(): HttpClient {
    return client
}

/**
 * 获取更长超时时间的webclient
 */
fun longTimeoutWebclient(): HttpClient {
    return client.config {
        install(HttpTimeout) {
            requestTimeoutMillis = 10.minutes.inWholeMilliseconds
            connectTimeoutMillis = 10.minutes.inWholeMilliseconds
            socketTimeoutMillis = 10.minutes.inWholeMilliseconds
        }
    }
}

/**
 * 请求头
 */
fun commonUserAgent(): String = commonUserAgent.random()

/**
 * body转为JsonNode
 */
suspend fun HttpResponse.bodyAsJsonNode(): JsonNode {
    return Json.readJsonNode(this.bodyAsText(UTF_8))
}

/**
 * body转为ArrayNode
 */
suspend fun HttpResponse.bodyAsArrayNode(): ArrayNode {
    return bodyAsJsonNode() as ArrayNode
}

/**
 * body转为Map
 */
suspend inline fun <reified K, reified V> HttpResponse.bodyAsMap(): Map<K, V> {
    return Json.readObj(this.bodyAsText(UTF_8))
}

/**
 * 获取重定向地址
 */
suspend fun HttpClient.urlToRedirectUrl(url: String, httpMethod: HttpMethod = HttpMethod.Get): String? {
    return this.config { followRedirects = false }.request(url) { method = httpMethod }.headers["Location"]
}