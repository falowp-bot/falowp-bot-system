package com.blr19c.falowp.bot.system.api

import com.blr19c.falowp.bot.system.expand.ImageUrl
import java.util.*

/**
 * 可以作为发送的消息
 */
interface SendMessage {

    companion object {
        fun builder(content: String): Builder {
            return Builder().text(content)
        }

        fun builder(): Builder {
            return Builder()
        }
    }


    class Builder(private val messageList: List<SendMessage> = arrayListOf()) {

        fun text(content: String): Builder {
            messageList.addLast(TextSendMessage(content))
            return this
        }

        fun at(at: String): Builder {
            messageList.addLast(AtSendMessage(at))
            return this
        }

        fun at(at: List<String>): Builder {
            at.asSequence().forEach { messageList.addLast(AtSendMessage(it)) }
            return this
        }

        fun at(receiveMessage: ReceiveMessage): Builder {
            messageList.addLast(AtSendMessage(receiveMessage.sender.id))
            return this
        }

        fun image(image: String): Builder {
            messageList.addLast(ImageSendMessage(ImageUrl(image)))
            return this
        }

        fun image(images: List<String>): Builder {
            images.asSequence()
                .map { ImageUrl(it) }
                .forEach { messageList.addLast(ImageSendMessage(it)) }
            return this
        }

        fun video(videos: List<String>): Builder {
            videos.asSequence().forEach { messageList.addLast(VideoSendMessage(it)) }
            return this
        }

        fun video(video: String): Builder {
            messageList.addLast(VideoSendMessage(video))
            return this
        }

        fun poke(): Builder {
            messageList.addLast(PokeSendMessage())
            return this
        }

        fun build(): SendMessageChain {
            return SendMessageChain(UUID.randomUUID().toString(), messageList)
        }
    }
}

/**
 * at消息
 */
data class AtSendMessage(
    /**
     * at谁
     */
    val at: String
) : SendMessage

/**
 * 文本消息
 */
data class TextSendMessage(
    /**
     * 消息内容
     */
    val content: String
) : SendMessage

/**
 * 图片消息
 */
data class ImageSendMessage(
    /**
     * 图片(仅支持url和base64)
     */
    val image: ImageUrl
) : SendMessage

/**
 * 视频消息
 */
data class VideoSendMessage(
    /**
     * 视频(仅支持url)
     */
    val video: String
) : SendMessage

/**
 * 戳一戳消息
 */
class PokeSendMessage : SendMessage

/**
 * 消息链
 */
data class SendMessageChain(
    /**
     * 消息id
     */
    val id: String,
    /**
     * 消息内容链
     */
    val messageList: List<SendMessage>
)
