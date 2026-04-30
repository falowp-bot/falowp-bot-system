@file:Suppress("UNUSED")

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
import java.io.File
import java.net.URI
import javax.imageio.ImageIO

private lateinit var toUrlFunction: suspend (ImageUrl) -> String

/**
 * 注册图片转URL的方法
 */
fun registerImageUrlToUrlFun(function: suspend (ImageUrl) -> String) {
    toUrlFunction = function
}

/**
 * image(支持base64和url)
 */
data class ImageUrl(
    /**
     * 图片信息，支持base64、URL、file URI
     */
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

    /**
     * 是否为网络URL
     */
    fun isUrl(): Boolean {
        return info.matches(Regex("https?://.+"))
    }

    /**
     * 是否为本地文件URI
     */
    fun isFile(): Boolean {
        return info.matches(Regex("file://.+"))
    }

    /**
     * 转为可访问URL
     */
    suspend fun toUrl(): String {
        if (isUrl()) return info
        if (!::toUrlFunction.isInitialized) {
            throw IllegalStateException("无可用Image存储")
        }
        return toUrlFunction.invoke(this)
    }

    /**
     * 转为字节数组
     */
    suspend fun toBytes(webclient: HttpClient = webclient()): ByteArray {
        return cachedBytes ?: mutex.withLock {
            cachedBytes ?: run {
                val data = if (isUrl()) webclient.get(toUrl()).readRawBytes()
                else if (isFile()) File(URI(info)).readBytes()
                else toBase64(webclient).decodeFromBase64String()
                cachedBytes = data
                data
            }
        }
    }

    /**
     * 获取图片摘要
     */
    suspend fun toSummary(): String {
        return withContext(Dispatchers.IO) {
            summary
        }
    }

    /**
     * 转为Base64字符串
     */
    suspend fun toBase64(webclient: HttpClient = webclient()): String {
        return withContext(Dispatchers.IO) {
            if (isUrl() || isFile()) toBytes(webclient).encodeToBase64String()
            else info
        }
    }

    /**
     * 转为HTML可用的Base64图片地址
     */
    suspend fun toHtmlBase64(webclient: HttpClient = webclient()): String {
        return "data:image/jpeg;base64,${toBase64(webclient)}"
    }

    /**
     * 转为BufferedImage
     */
    suspend fun toBufferedImage(webclient: HttpClient = webclient()): BufferedImage {
        return withContext(Dispatchers.IO) {
            ImageIO.read(ByteArrayInputStream(toBytes(webclient)))
        }
    }

    /**
     * 写入文件
     */
    suspend fun toFile(file: File, webclient: HttpClient = webclient()): File {
        file.writeBytes(toBytes(webclient))
        return file
    }

    companion object {
        /**
         * 创建空图片地址
         */
        fun empty(): ImageUrl {
            return "".toImageUrl()
        }
    }
}

/**
 * 文件转图片地址
 */
fun File.toImageUrl(): ImageUrl = ImageUrl(this.toURI().toString())

/**
 * 字符串转图片地址
 */
fun String.toImageUrl(): ImageUrl = ImageUrl(this)

/**
 * 字节数组转图片地址
 */
fun ByteArray.toImageUrl(): ImageUrl = ImageUrl(this.encodeToBase64String())

/**
 * URI转图片地址
 */
fun URI.toImageUrl(): ImageUrl = ImageUrl(this.toString())
