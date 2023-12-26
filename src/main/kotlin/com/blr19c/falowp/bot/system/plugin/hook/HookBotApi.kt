package com.blr19c.falowp.bot.system.plugin.hook

import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.api.SendMessage
import com.blr19c.falowp.bot.system.plugin.HookPluginRegister
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 钩子的botApi
 */
class HookBotApi(
    private val delegateBotApi: BotApi,
    val register: HookPluginRegister<out Plugin.Listener.Hook>
) :
    BotApi(delegateBotApi.receiveMessage, register.originalClass) {

    override suspend fun sendGroup(vararg sendMessage: SendMessage, reference: Boolean, forward: Boolean) {
        delegateBotApi.sendGroup(*sendMessage, reference = reference, forward = forward)
    }

    override suspend fun sendAllGroup(vararg sendMessage: SendMessage, reference: Boolean, forward: Boolean) {
        delegateBotApi.sendAllGroup(*sendMessage, reference = reference, forward = forward)
    }

    override suspend fun sendPrivate(vararg sendMessage: SendMessage, reference: Boolean, forward: Boolean) {
        delegateBotApi.sendPrivate(*sendMessage, reference = reference, forward = forward)
    }

    override suspend fun sendReply(vararg sendMessage: String, reference: Boolean, forward: Boolean) {
        delegateBotApi.sendReply(*sendMessage, reference = reference, forward = forward)
    }

    override suspend fun sendReply(vararg sendMessage: SendMessage, reference: Boolean, forward: Boolean) {
        delegateBotApi.sendReply(*sendMessage, reference = reference, forward = forward)
    }
}