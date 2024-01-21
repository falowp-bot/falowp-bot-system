package com.blr19c.falowp.bot.system.listener.hooks

import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.PluginInfo

/**
 * 在HelpEvent事件触发时可以手动修改一些插件信息
 */
data class HelpEventHook(var pluginInfo: List<PluginInfo>) : Plugin.Listener.Hook