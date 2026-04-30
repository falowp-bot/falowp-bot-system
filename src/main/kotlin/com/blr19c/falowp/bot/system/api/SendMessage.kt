package com.blr19c.falowp.bot.system.api

import com.blr19c.falowp.bot.system.expand.ImageUrl
import com.blr19c.falowp.bot.system.expand.toImageUrl
import com.blr19c.falowp.bot.system.listener.events.NudgeEvent
import java.net.URI
import java.util.*

/**
 * 可以作为发送的消息
 */
interface SendMessage {

    companion object {
        /**
         * 创建包含文本内容的消息链构建器
         */
        fun builder(content: String): Builder {
            return Builder().text(content)
        }

        /**
         * 创建消息链构建器
         */
        fun builder(): Builder {
            return Builder()
        }
    }

    /**
     * 消息链构建器
     */
    @Suppress("unused")
    class Builder(val messageList: MutableList<SendMessage> = arrayListOf()) {

        /**
         * 添加文本消息
         */
        fun text(content: String) = apply {
            messageList.addLast(TextSendMessage(content))
        }

        /**
         * 添加单个at消息
         */
        fun at(at: String) = apply {
            messageList.addLast(AtSendMessage(at))
        }

        /**
         * 添加多个at消息
         */
        fun at(at: List<String>) = apply {
            at.forEach { messageList.addLast(AtSendMessage(it)) }
        }

        /**
         * at消息发送人
         */
        fun at(receiveMessage: ReceiveMessage) = apply {
            messageList.addLast(AtSendMessage(receiveMessage.sender.id))
        }

        /**
         * 添加图片消息
         */
        fun image(image: String) = apply {
            messageList.addLast(ImageSendMessage(image.toImageUrl()))
        }

        /**
         * 添加多张图片消息
         */
        fun image(images: List<String>) = apply {
            images.forEach {
                messageList.addLast(ImageSendMessage(it.toImageUrl()))
            }
        }

        /**
         * 添加语音消息
         */
        fun voice(video: URI) = apply {
            messageList.addLast(VoiceSendMessage(video))
        }

        /**
         * 添加视频消息
         */
        fun video(video: URI) = apply {
            messageList.addLast(VideoSendMessage(video))
        }

        /**
         * 添加多个视频消息
         */
        fun video(videos: List<URI>) = apply {
            videos.forEach { messageList.addLast(VideoSendMessage(it)) }
        }

        /**
         * 添加轻推消息
         */
        fun nudge(id: String) = apply {
            messageList.addLast(NudgeSendMessage(id))
        }

        /**
         * 回复轻推事件
         */
        fun nudge(nudgeEvent: NudgeEvent) = apply {
            messageList.addLast(NudgeSendMessage(nudgeEvent.actor.id))
        }

        /**
         * 添加表情消息
         */
        fun emoji(id: String, type: String, display: String) = apply {
            messageList.addLast(EmojiSendMessage(id, type, display))
        }

        /**
         * 添加表情消息
         */
        fun emoji(emojiSendMessage: EmojiSendMessage) = apply {
            messageList.addLast(emojiSendMessage)
        }

        /**
         * 添加多个表情消息
         */
        fun emoji(emojiSendMessages: List<EmojiSendMessage>) = apply {
            emojiSendMessages.forEach { messageList.addLast(it) }
        }

        /**
         * 构建消息链
         */
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
 * 轻推消息(戳一戳/拍一拍/无内容的强提醒)
 */
data class NudgeSendMessage(
    /**
     * 目标
     */
    val id: String
) : SendMessage

/**
 * 表情消息
 */
data class EmojiSendMessage(
    /**
     * 表情ID
     */
    val id: String,
    /**
     * 表情类型
     */
    val type: String,
    /**
     * 展示/描述
     */
    val display: String
) : SendMessage


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
