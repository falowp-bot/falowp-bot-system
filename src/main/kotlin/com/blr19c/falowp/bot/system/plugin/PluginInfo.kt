package com.blr19c.falowp.bot.system.plugin

/**
 * 插件信息
 */
data class PluginInfo(
    /**
     * 插件真实对象
     */
    val instance: Any,
    /**
     * 插件信息
     */
    val plugin: Plugin
) {
    companion object {
        fun empty(): PluginInfo {
            return PluginInfo(Any(), Plugin(name = ""))
        }
    }
}