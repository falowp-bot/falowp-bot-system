package com.blr19c.falowp.bot.system.plugin.hook

import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.api.BotSelf
import com.blr19c.falowp.bot.system.api.SendMessageChain
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 钩子的botApi
 */
class HookBotApi(
    private val delegateBotApi: BotApi,
    register: HookPluginRegister<out Plugin.Listener.Hook>
) : BotApi(delegateBotApi.receiveMessage, register.originalClass) {

    override suspend fun sendGroup(
        vararg sendMessageChain: SendMessageChain,
        sourceId: String,
        reference: Boolean,
        forward: Boolean
    ) {
        delegateBotApi.sendGroup(*sendMessageChain, sourceId = sourceId, reference = reference, forward = forward)
    }

    override suspend fun sendAllGroup(vararg sendMessageChain: SendMessageChain, reference: Boolean, forward: Boolean) {
        delegateBotApi.sendAllGroup(*sendMessageChain, reference = reference, forward = forward)
    }

    override suspend fun sendPrivate(
        vararg sendMessageChain: SendMessageChain,
        sourceId: String,
        reference: Boolean,
        forward: Boolean,
    ) {
        delegateBotApi.sendPrivate(*sendMessageChain, sourceId = sourceId, reference = reference, forward = forward)
    }

    override suspend fun sendReply(vararg sendMessage: String, reference: Boolean, forward: Boolean) {
        delegateBotApi.sendReply(*sendMessage, reference = reference, forward = forward)
    }

    override suspend fun sendReply(vararg sendMessageChain: SendMessageChain, reference: Boolean, forward: Boolean) {
        delegateBotApi.sendReply(*sendMessageChain, reference = reference, forward = forward)
    }

    override suspend fun self(): BotSelf {
        return delegateBotApi.self()
    }
}