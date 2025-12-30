package com.blr19c.falowp.bot.system.api

import com.blr19c.falowp.bot.system.expand.ImageUrl
import java.net.URI
import java.util.*

/**
 * 可以作为发送的消息
 */
@Suppress("UNUSED")
interface SendMessage {

    companion object {
        fun builder(content: String): Builder {
            return Builder().text(content)
        }

        fun builder(): Builder {
            return Builder()
        }
    }


    class Builder(val messageList: MutableList<SendMessage> = arrayListOf()) {

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

        fun voice(video: URI): Builder {
            messageList.addLast(VoiceSendMessage(video))
            return this
        }

        fun video(videos: List<URI>): Builder {
            videos.asSequence().forEach { messageList.addLast(VideoSendMessage(it)) }
            return this
        }

        fun video(video: URI): Builder {
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
 * 语音消息
 */
data class VoiceSendMessage(
    /**
     * 语音
     */
    val voice: URI
) : SendMessage

/**
 * 图片消息
 */
data class ImageSendMessage(
    /**
     * 图片
     */
    val image: ImageUrl
) : SendMessage

/**
 * 视频消息
 */
data class VideoSendMessage(
    /**
     * 视频
     */
    val video: URI
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
