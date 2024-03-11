package com.blr19c.falowp.bot.system.scheduling.api

import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SendMessageChain
import com.blr19c.falowp.bot.system.scheduling.Scheduling
import kotlin.reflect.KClass

/**
 * 定时任务的botApi
 */
class SchedulingBotApi(originalClass: KClass<*>) : BotApi(ReceiveMessage.empty(), originalClass) {

    override suspend fun sendGroup(
        vararg sendMessageChain: SendMessageChain,
        sourceId: String,
        reference: Boolean,
        forward: Boolean
    ) {
        selectBot(sourceId) {
            this.sendGroup(*sendMessageChain, sourceId = sourceId, reference = reference, forward = forward)
        }
    }

    override suspend fun sendAllGroup(vararg sendMessageChain: SendMessageChain, reference: Boolean, forward: Boolean) {
        allBot {
            this.sendAllGroup(*sendMessageChain, reference = reference, forward = forward)
        }
    }

    override suspend fun sendPrivate(
        vararg sendMessageChain: SendMessageChain,
        sourceId: String,
        reference: Boolean,
        forward: Boolean
    ) {
        selectBot(sourceId) {
            this.sendPrivate(*sendMessageChain, sourceId = sourceId, reference = reference, forward = forward)
        }
    }

    private suspend fun selectBot(sourceId: String, block: suspend BotApi.() -> Unit) {
        Scheduling.selectBot(sourceId, originalClass)?.let { block.invoke(it) }
    }

    private suspend fun allBot(block: suspend BotApi.() -> Unit) {
        Scheduling.allBot(originalClass)
            .forEach { block.invoke(it) }
    }

    override suspend fun sendReply(vararg sendMessageChain: SendMessageChain, reference: Boolean, forward: Boolean) {
        throw IllegalStateException("定时任务BotAPi无法回复消息")
    }
}