package com.blr19c.falowp.bot.system.api

import com.blr19c.falowp.bot.system.image.ImageUrl

/**
 * 接收的消息
 */
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
    val self: Self
) {
    companion object {
        fun empty(): ReceiveMessage {
            val content = Content.empty()
            val sender = User.empty()
            val source = Source.empty()
            return ReceiveMessage(
                "",
                MessageTypeEnum.MESSAGE,
                content,
                sender,
                source,
                Self("")
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
     * 是否为频道消息
     */
    fun channel(): Boolean {
        return this.source.type === SourceTypeEnum.CHANNEL
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
         * at谁
         */
        val at: List<User>,
        /**
         * 图片
         */
        val image: List<ImageUrl>,
        /**
         * 分享
         */
        val share: List<Share>,
    ) {
        companion object {
            fun empty(): Content {
                return Content("", emptyList(), emptyList(), emptyList())
            }
        }
    }

    /**
     * 分享内容
     */
    data class Share(
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
        }
    }

    data class Self(
        /**
         * 机器人id
         */
        val id: String,
    )
}