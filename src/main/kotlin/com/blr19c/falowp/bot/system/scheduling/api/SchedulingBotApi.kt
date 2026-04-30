package com.blr19c.falowp.bot.system.scheduling.api

import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.api.BotSelf
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SendMessageChain
import com.blr19c.falowp.bot.system.scheduling.Scheduling
import kotlin.reflect.KClass

/**
 * 定时任务的botApi
 */
class SchedulingBotApi(originalClass: KClass<*>) : BotApi(ReceiveMessage.empty(), originalClass) {

    /**
     * 通过定时任务支持发送群聊消息
     */
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

    /**
     * 通过所有定时任务支持发送群聊消息
     */
    override suspend fun sendAllGroup(vararg sendMessageChain: SendMessageChain, reference: Boolean, forward: Boolean) {
        allBot {
            this.sendAllGroup(*sendMessageChain, reference = reference, forward = forward)
        }
    }

    /**
     * 通过定时任务支持发送私聊消息
     */
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

    /**
     * 根据接收人选择BotApi
     */
    private suspend fun selectBot(sourceId: String, block: suspend BotApi.() -> Unit) {
        Scheduling.selectBot(sourceId, originalClass)?.let { block.invoke(it) }
    }

    /**
     * 遍历所有BotApi
     */
    private suspend fun allBot(block: suspend BotApi.() -> Unit) {
        Scheduling.allBot(originalClass)
            .forEach { block.invoke(it) }
    }

    /**
     * 定时任务不支持回复消息
     */
    override suspend fun sendReply(vararg sendMessageChain: SendMessageChain, reference: Boolean, forward: Boolean) {
        throw IllegalStateException("定时任务BotApi无法回复消息")
    }

    /**
     * 定时任务不支持获取自身信息
     */
    override suspend fun self(): BotSelf {
        throw IllegalStateException("定时任务BotApi无法获取自身信息")
    }
}