package com.blr19c.falowp.bot.system.image

import com.madgag.gif.fmsware.AnimatedGifEncoder
import java.awt.AlphaComposite
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream

/**
 * 调整大小
 */
fun BufferedImage.resize(width: Int, height: Int): BufferedImage {
    val resizedImage = BufferedImage(width, height, this.type)
    val g = resizedImage.createGraphics()
    g.drawImage(this, 0, 0, width, height, null)
    g.dispose()
    return resizedImage
}

/**
 * 转为圆形图像
 */
fun BufferedImage.circle(): BufferedImage {
    val circleImage = BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB)
    val g = circleImage.createGraphics()
    g.clip = Ellipse2D.Float(0f, 0f, this.width.toFloat(), this.height.toFloat())
    g.drawImage(this, 0, 0, null)
    g.dispose()
    return circleImage
}

/**
 * 旋转图片
 */
fun BufferedImage.rotate(degrees: Int): BufferedImage {
    val rotatedImage = BufferedImage(this.height, this.width, this.type)
    val g = rotatedImage.createGraphics()
    g.rotate(Math.toRadians(degrees.toDouble()), (this.width / 2).toDouble(), (this.height / 2).toDouble())
    g.drawImage(this, null, 0, 0)
    g.dispose()
    return rotatedImage
}

/**
 * 叠加图像
 */
fun BufferedImage.overlay(overlay: BufferedImage, x: Int, y: Int, alpha: Boolean): BufferedImage {
    val g = this.createGraphics()
    g.drawImage(overlay, x, y, null)
    if (alpha) {
        g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f)
    }
    g.dispose()
    return this
}

fun List<BufferedImage>.toGif(delay: Double): ByteArray {
    val out = ByteArrayOutputStream()
    val animatedGifEncoder = AnimatedGifEncoder()
    animatedGifEncoder.setSize(this[0].width, this[0].height)
    animatedGifEncoder.start(out)
    animatedGifEncoder.setDelay(delay.toInt())
    animatedGifEncoder.setRepeat(0)
    this.forEach { animatedGifEncoder.addFrame(it) }
    animatedGifEncoder.finish()
    return out.toByteArray()
}
