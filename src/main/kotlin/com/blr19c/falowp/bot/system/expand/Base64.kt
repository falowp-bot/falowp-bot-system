@file:Suppress("UNUSED")

package com.blr19c.falowp.bot.system.expand

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.*
import javax.imageio.ImageIO

/**
 * 将字节数组编码为Base64字符串
 */
fun ByteArray.encodeToBase64String(): String = Base64.getEncoder().encodeToString(this)

/**
 * 将Base64字符串解码为字节数组
 */
fun String.decodeFromBase64String(): ByteArray = Base64.getDecoder().decode(this)

/**
 * 将Base64字符串转换为图片
 */
fun String.base64ToBufferedImage(): BufferedImage = ImageIO.read(ByteArrayInputStream(this.decodeFromBase64String()))