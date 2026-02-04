package com.blr19c.falowp.bot.system.plugin.hook

import com.blr19c.falowp.bot.system.plugin.PluginInfo

/**
 * 钩子类插件匹配规则
 */
data class HookPluginRegisterMatch(
    /**
     * 自定义匹配
     */
    val customBlock: ((PluginInfo) -> Boolean)? = null,
) {
    companion object {
        fun allMatch(): HookPluginRegisterMatch {
            return HookPluginRegisterMatch()
        }
    }
}