package com.blr19c.falowp.bot.system.plugin.event

import com.blr19c.falowp.bot.system.api.*
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 事件类型的BotApi
 */
class EventBotApi(private val delegateBotApi: BotApi, receiveMessage: ReceiveMessage) :
    BotApi(receiveMessage, delegateBotApi.originalClass) {

    override suspend fun sendGroup(
        vararg sendMessageChain: SendMessageChain,
        sourceId: String,
        reference: Boolean,
        forward: Boolean
    ) {
        delegateBotApi.sendGroup(*sendMessageChain, sourceId = sourceId, reference = reference, forward = forward)
    }

    override suspend fun sendAllGroup(
        vararg sendMessageChain: SendMessageChain,
        reference: Boolean,
        forward: Boolean
    ) {
        delegateBotApi.sendAllGroup(*sendMessageChain, reference = reference, forward = forward)
    }

    override suspend fun sendPrivate(
        vararg sendMessageChain: SendMessageChain,
        sourceId: String,
        reference: Boolean,
        forward: Boolean
    ) {
        delegateBotApi.sendPrivate(*sendMessageChain, sourceId = sourceId, reference = reference, forward = forward)
    }

    override suspend fun self(): BotSelf {
        return delegateBotApi.self()
    }
}

/**
 * 转为EventBotApi
 */
suspend fun BotApi.eventBotApi(event: Plugin.Listener.Event, adapter: ReceiveMessage.Adapter): EventBotApi {
    return EventBotApi(
        this, receiveMessage.copy(
            messageType = MessageTypeEnum.OTHER,
            source = event.source,
            self = runCatching { self() }.getOrDefault(BotSelf.Default()),
            adapter = adapter
        )
    )
}