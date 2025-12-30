@file:Suppress("UNUSED")

package com.blr19c.falowp.bot.system.expand

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest


fun String.md5(): String {
    val md5 = MessageDigest.getInstance("MD5")
    val bytes = md5.digest(this.encodeToByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

fun String.sha256(): String {
    val sha256 = MessageDigest.getInstance("SHA-256")
    val bytes = sha256.digest(this.encodeToByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

fun String.urlEncoder(): String {
    return URLEncoder.encode(this, StandardCharsets.UTF_8)
}

fun String.urlDecoder(): String {
    return URLDecoder.decode(this, StandardCharsets.UTF_8)
}