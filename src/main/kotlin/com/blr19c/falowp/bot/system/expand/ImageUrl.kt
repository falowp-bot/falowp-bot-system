package com.blr19c.falowp.bot.system.expand

import com.blr19c.falowp.bot.system.web.webclient
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.net.URI
import javax.imageio.ImageIO

private lateinit var toUrlFunction: suspend (ImageUrl) -> String

fun registerImageUrlToUrlFun(function: suspend (ImageUrl) -> String) {
    toUrlFunction = function
}

/**
 * image(支持base64和url)
 */
data class ImageUrl(
    val info: String
) {
    @Volatile
    private var cachedBytes: ByteArray? = null
    private val mutex = Mutex()

    private val summary by lazy {
        runBlocking {
            return@runBlocking toBase64().md5()
        }
    }

    fun isUrl(): Boolean {
        return info.matches(Regex("https?://.+"))
    }

    suspend fun toUrl(): String {
        if (isUrl()) return info
        if (!::toUrlFunction.isInitialized) {
            throw IllegalStateException("无可用Image存储")
        }
        return toUrlFunction.invoke(this)
    }

    suspend fun toBytes(webclient: HttpClient = webclient()): ByteArray {
        return cachedBytes ?: mutex.withLock {
            cachedBytes ?: run {
                val data = if (isUrl()) webclient.get(toUrl()).readRawBytes()
                else toBase64(webclient).decodeFromBase64String()
                cachedBytes = data
                data
            }
        }
    }

    suspend fun toSummary(): String {
        return withContext(Dispatchers.IO) {
            summary
        }
    }

    suspend fun toBase64(webclient: HttpClient = webclient()): String {
        return withContext(Dispatchers.IO) {
            if (!isUrl()) info
            else toBytes(webclient).encodeToBase64String()
        }
    }

    suspend fun toHtmlBase64(webclient: HttpClient = webclient()): String {
        return "data:image/jpeg;base64,${toBase64(webclient)}"
    }

    suspend fun toBufferedImage(webclient: HttpClient = webclient()): BufferedImage {
        return withContext(Dispatchers.IO) {
            ImageIO.read(ByteArrayInputStream(toBytes(webclient)))
        }
    }

    companion object {
        fun empty(): ImageUrl {
            return ImageUrl("")
        }
    }
}


fun ByteArray.toImageUrl(): ImageUrl = ImageUrl(this.encodeToBase64String())
fun URI.toImageUrl(): ImageUrl = ImageUrl(this.toString())