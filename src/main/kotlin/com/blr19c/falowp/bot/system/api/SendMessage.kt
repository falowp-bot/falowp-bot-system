package com.blr19c.falowp.bot.system.api

import com.blr19c.falowp.bot.system.image.ImageUrl
import java.util.*

/**
 * 发送的消息
 */
data class SendMessage(
    /**
     * 消息id
     */
    val id: String,
    /**
     * 消息内容
     */
    val content: String,
    /**
     * at谁
     */
    val at: List<String> = emptyList(),
    /**
     * 图片(仅支持url和base64)
     */
    val images: List<ImageUrl> = emptyList(),
    /**
     * 视频(仅支持url)
     */
    val videos: List<String> = emptyList(),
    /**
     * 戳一戳
     */
    val poke: Boolean = false,
) {

    companion object {
        fun builder(content: String): Builder {
            return Builder(content)
        }

        fun builder(): Builder {
            return Builder("")
        }
    }


    class Builder(private var content: String) {
        private val id: String = UUID.randomUUID().toString()
        private val at: MutableList<String> = arrayListOf()
        private val images: MutableList<ImageUrl> = arrayListOf()
        private val videos: MutableList<String> = arrayListOf()
        private var poke: Boolean = false
        fun content(content: String): Builder {
            this.content = content
            return this
        }

        fun at(at: String): Builder {
            this.at.add(at)
            return this
        }

        fun at(at: List<String>): Builder {
            this.at.addAll(at)
            return this
        }

        fun at(receiveMessage: ReceiveMessage): Builder {
            this.at.add(receiveMessage.sender.id)
            return this
        }

        fun images(images: String): Builder {
            this.images.add(ImageUrl(images))
            return this
        }

        fun images(images: List<String>): Builder {
            this.images.addAll(images.map { ImageUrl(it) })
            return this
        }

        fun videos(videos: List<String>): Builder {
            this.videos.addAll(videos)
            return this
        }

        fun videos(video: String): Builder {
            this.videos.add(video)
            return this
        }

        fun poke(): Builder {
            this.poke = true
            return this
        }


        fun build(): SendMessage {
            return SendMessage(id, content, at, images, videos, poke)
        }
    }
}
