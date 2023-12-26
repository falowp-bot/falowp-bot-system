package com.blr19c.falowp.bot.system.listener.events

import com.blr19c.falowp.bot.system.plugin.Plugin


/**
 * 发送帮助事件
 */
data class HelpEvent(
    /**
     * 显示隐藏插件
     */
    val showHidden: Boolean = false,

    /**
     * 显示禁用插件
     */
    val showDisable: Boolean = true,

    /**
     * 获取功能的帮助
     */
    val pluginName: String? = null,
) : Plugin.Listener.Event
