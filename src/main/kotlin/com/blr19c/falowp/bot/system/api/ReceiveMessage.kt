package com.blr19c.falowp.bot.system.api

import com.blr19c.falowp.bot.system.expand.ImageUrl
import java.net.URI

/**
 * 接收的消息
 */
@Suppress("unused")
data class ReceiveMessage(
    /**
     * 消息ID
     */
    val id: String,
    /**
     * 消息类型
     */
    val messageType: MessageTypeEnum,
    /**
     * 消息内容
     */
    val content: Content,
    /**
     * 发送人信息
     */
    val sender: User,
    /**
     * 消息来源信息
     */
    val source: Source,
    /**
     * 机器人信息
     */
    val self: BotSelf,
    /**
     * 消息来源适配器
     */
    val adapter: Adapter
) {
    companion object {
        fun empty(): ReceiveMessage {
            val content = Content.empty()
            val sender = User.empty()
            val source = Source.empty()
            return ReceiveMessage(
                "",
                MessageTypeEnum.OTHER,
                content,
                sender,
                source,
                BotSelf.Default(),
                Adapter.empty()
            )
        }
    }

    /**
     * 是否为群聊消息
     */
    fun group(): Boolean {
        return this.source.type === SourceTypeEnum.GROUP
    }

    /**
     * 是否为私聊
     */
    fun private(): Boolean {
        return this.source.type === SourceTypeEnum.PRIVATE
    }

    /**
     * 是否@我
     */
    fun atMe(): Boolean {
        return private() || content.at.any { it.id == self.id }
    }

    /**
     * 消息内容
     */
    data class Content(
        /**
         * 消息
         */
        val message: String,
        /**
         * 语音
         */
        val voice: Voice?,
        /**
         * at谁
         */
        val at: List<User>,
        /**
         * 图片
         */
        val image: List<ImageUrl>,
        /**
         * 表情
         */
        val emoji: List<Emoji>,
        /**
         * 视频
         */
        val video: Video?,
        /**
         * 分享
         */
        val share: List<Share>,
        /**
         * 文件
         */
        val file: List<File>,
        /**
         * 引用消息
         */
        val reference: suspend () -> ReceiveMessage?
    ) {
        companion object {
            fun empty(): Content {
                return Content("", null, emptyList(), emptyList(), emptyList(), null, emptyList(), emptyList()) { null }
            }
        }

        fun hasMessageContent(): Boolean =
            message.isNotEmpty() || image.isNotEmpty() || at.isNotEmpty() || emoji.isNotEmpty()

        fun toMessageType(): MessageTypeEnum {
            return when {
                this.video != null -> MessageTypeEnum.VIDEO
                this.voice != null -> MessageTypeEnum.VOICE
                this.share.isNotEmpty() -> MessageTypeEnum.SHARE
                this.file.isNotEmpty() -> MessageTypeEnum.FILE
                hasMessageContent() -> MessageTypeEnum.MESSAGE
                else -> MessageTypeEnum.OTHER
            }
        }
    }

    /**
     * 表情
     */
    @Suppress("UNUSED")
    interface Emoji {
        /**
         * 表情ID
         */
        val id: String

        /**
         * 表情类型
         */
        val type: String

        /**
         * 展示/描述
         */
        val display: String
    }


    /**
     * 视频
     */
    data class Video(
        /**
         * 视频ID
         */
        val id: String,
        /**
         * 缩略图
         */
        val thumbnail: ImageUrl,
        /**
         * 视频地址
         */
        val src: URI,
        /**
         * 视频大小
         */
        val size: Long,
    )

    /**
     * 语音
     */
    data class Voice(
        /**
         * 语音ID
         */
        val id: String,
        /**
         * 语音地址
         */
        val src: URI,
    )

    /**
     * 分享内容
     */
    data class Share(
        /**
         * 分享类型
         */
        val type: String,
        /**
         * appId
         */
        val appId: String,
        /**
         * app名称
         */
        val appName: String,
        /**
         * 标题
         */
        val title: String,
        /**
         * 预览图
         */
        val preview: ImageUrl,
        /**
         * 资源(或者点击跳转的地址)
         */
        val sourceUrl: String,
    )

    /**
     * 文件
     */
    data class File(
        /**
         * 文件ID
         */
        val id: String,
        /**
         * 文件类型
         */
        val type: String,
        /**
         * 文件名
         */
        val name: String,
        /**
         * 文件地址
         */
        val src: URI,
        /**
         * 文件大小
         */
        val size: Long,
    )

    /**
     * 用户
     */
    data class User(
        /**
         * id
         */
        val id: String,
        /**
         * 昵称
         */
        val nickname: String,
        /**
         * 权限
         */
        val auth: ApiAuth,
        /**
         * 头像
         */
        val avatar: ImageUrl,
    ) {
        companion object {

            fun empty(): User {
                return User("", "", ApiAuth.ORDINARY_MEMBER, ImageUrl.empty())
            }

        }
    }

    /**
     * 来源
     */
    data class Source(
        /**
         * 消息来源id
         */
        val id: String,
        /**
         * 消息来源类型
         */
        val type: SourceTypeEnum
    ) {
        companion object {
            fun empty(): Source {
                return Source("", SourceTypeEnum.UNKNOWN)
            }

            fun system(): Source {
                return Source("system", SourceTypeEnum.SYSTEM)
            }
        }
    }

    /**
     * 适配器
     */
    data class Adapter(
        /**
         * 适配器id
         */
        val id: String,
        /**
         * 适配器挂载数据
         */
        val data: Any
    ) {

        override fun toString(): String {
            return "Adapter(id=$id, data=适配器挂载:${data::class.simpleName})"
        }

        companion object {
            fun empty(): Adapter {
                return Adapter("", "")
            }
        }
    }
}