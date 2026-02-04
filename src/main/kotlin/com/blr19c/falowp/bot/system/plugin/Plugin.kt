package com.blr19c.falowp.bot.system.plugin

import com.blr19c.falowp.bot.system.api.ReceiveMessage

/**
 * 插件工具/一个特殊的插件 只会激活其object::init方法
 */
annotation class PluginUtils

/**
 * 插件
 */
@Suppress("UNUSED")
annotation class Plugin(
    /**
     * 插件名称
     */
    val name: String,
    /**
     * 插件描述(支持html)
     */
    val desc: String = "",
    /**
     * 标签(相同标签的插件在默认的帮助展示时会放在一起)
     */
    val tag: String = "其他",
    /**
     * 是否启用
     */
    val enable: Boolean = true,
    /**
     * 是否隐藏
     */
    val hidden: Boolean = false,
) {

    /**
     * 监听器
     */
    interface Listener {

        /**
         * 事件
         */
        interface Event {
            /**
             * 来源
             */
            val source: ReceiveMessage.Source

            /**
             * 触发人
             */
            val actor: ReceiveMessage.User
        }

        /**
         * 在执行期间注入
         */
        interface Hook
    }
}