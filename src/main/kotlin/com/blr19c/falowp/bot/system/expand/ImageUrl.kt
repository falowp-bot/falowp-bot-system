package com.blr19c.falowp.bot.system.expand

import com.blr19c.falowp.bot.system.web.commonUserAgent
import com.blr19c.falowp.bot.system.web.longTimeoutWebclient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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

    private val bytesDelegate = lazy {
        runBlocking {
            if (isUrl()) longTimeoutWebclient()
                .get(toUrl()) { header(HttpHeaders.UserAgent, commonUserAgent()) }
                .readRawBytes()
            else toBase64().decodeFromBase64String()
        }
    }

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

    suspend fun toBytes(): ByteArray {
        return withContext(Dispatchers.IO) {
            bytesDelegate.value
        }
    }

    suspend fun toSummary(): String {
        return withContext(Dispatchers.IO) {
            summary
        }
    }

    suspend fun toBase64(): String {
        return withContext(Dispatchers.IO) {
            if (!isUrl()) info
            else toBytes().encodeToBase64String()
        }
    }

    suspend fun toHtmlBase64(): String {
        return "data:image/jpeg;base64,${toBase64()}"
    }

    suspend fun toBufferedImage(): BufferedImage {
        return withContext(Dispatchers.IO) {
            ImageIO.read(ByteArrayInputStream(toBytes()))
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