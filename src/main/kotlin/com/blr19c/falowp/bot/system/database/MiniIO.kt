package com.blr19c.falowp.bot.system.database

import com.blr19c.falowp.bot.system.systemConfigProperty
import io.ktor.http.*
import io.minio.MinioClient
import io.minio.PutObjectArgs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.util.*


private val minioClient by lazy {
    MinioClient.builder()
        .endpoint(systemConfigProperty("miniIO.url"))
        .credentials(systemConfigProperty("miniIO.accessKey"), systemConfigProperty("miniIO.secretKey"))
        .build()
}

internal fun init() {
    if (checkEnable())
        minioClient
}

private fun checkEnable(): Boolean {
    return systemConfigProperty("miniIO.enable") == "true"
}

/**
 * 上传临时照片并获取url
 */
suspend fun tempImageUrl(byteArray: ByteArray): String {
    if (!checkEnable()) throw IllegalStateException("未开启miniIO")
    val name = UUID.randomUUID().toString() + ".png"
    withContext(Dispatchers.IO) {
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(systemConfigProperty("miniIO.tempBucket"))
                .`object`(name)
                .stream(ByteArrayInputStream(byteArray), byteArray.size.toLong(), -1)
                .contentType("image/png")
                .build()
        )
    }
    return systemConfigProperty("miniIO.url")
        .plus("/")
        .plus(systemConfigProperty("miniIO.tempBucket"))
        .plus("/")
        .plus(name)
        .encodeURLPath()
}

/**
 * 上传永久照片并获取url
 */
suspend fun permanentImageUrl(byteArray: ByteArray, path: String = ""): String {
    if (!checkEnable()) throw IllegalStateException("未开启miniIO")
    val name = "$path/${UUID.randomUUID()}.png"
    withContext(Dispatchers.IO) {
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(systemConfigProperty("miniIO.permanentBucket"))
                .`object`(name)
                .stream(ByteArrayInputStream(byteArray), byteArray.size.toLong(), -1)
                .contentType("image/png")
                .build()
        )
    }
    return systemConfigProperty("miniIO.url")
        .plus("/")
        .plus(systemConfigProperty("miniIO.permanentBucket"))
        .plus("/")
        .plus(name)
        .encodeURLPath()
}