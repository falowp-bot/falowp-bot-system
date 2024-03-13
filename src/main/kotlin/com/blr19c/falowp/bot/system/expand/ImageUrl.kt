package com.blr19c.falowp.bot.system.expand

import com.blr19c.falowp.bot.system.database.tempImageUrl
import com.blr19c.falowp.bot.system.web.commonUserAgent
import com.blr19c.falowp.bot.system.web.longTimeoutWebclient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.net.URI
import javax.imageio.ImageIO

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
                .readBytes()
            else toBase64().decodeFromBase64String()
        }
    }

    private val summaryBytes by lazy {
        if (bytesDelegate.isInitialized()) {
            bytesDelegate.value.take(1024 * 100).toByteArray()
        } else {
            runBlocking {
                if (isUrl()) longTimeoutWebclient()
                    .get(toUrl()) { header(HttpHeaders.UserAgent, commonUserAgent()) }
                    .bodyAsChannel()
                    .readRemaining(1024 * 100)
                    .use { it.readBytes() }
                else toBase64().decodeFromBase64String()
            }
        }
    }

    fun isUrl(): Boolean {
        return info.matches(Regex("https?://.+"))
    }

    suspend fun toUrl(): String {
        if (isUrl()) return info
        return tempImageUrl(info.decodeFromBase64String())
    }

    suspend fun toBytes(): ByteArray {
        return withContext(Dispatchers.IO) {
            bytesDelegate.value
        }
    }

    suspend fun toSummaryBytes(): ByteArray {
        return withContext(Dispatchers.IO) {
            summaryBytes
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