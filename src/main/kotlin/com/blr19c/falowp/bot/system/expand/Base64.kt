@file:Suppress("UNUSED")

package com.blr19c.falowp.bot.system.expand

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.*
import javax.imageio.ImageIO

fun ByteArray.encodeToBase64String(): String = Base64.getEncoder().encodeToString(this)

fun String.decodeFromBase64String(): ByteArray = Base64.getDecoder().decode(this)

fun String.base64ToBufferedImage(): BufferedImage = ImageIO.read(ByteArrayInputStream(this.decodeFromBase64String()))