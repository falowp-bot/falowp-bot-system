package com.blr19c.falowp.bot.system.plugin.hook

import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 钩子函数
 * @param hook 钩子的信息
 * @param botApi 当前位置的botApi
 * @param block 执行内容
 */
suspend fun withPluginHook(
    botApi: BotApi,
    hook: Plugin.Listener.Hook,
    block: suspend () -> Unit
) {
    HookManager.buildHookJoinPoint(hook, botApi, block).process()
}