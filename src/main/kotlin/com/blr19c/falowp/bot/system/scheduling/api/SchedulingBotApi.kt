package com.blr19c.falowp.bot.system.scheduling.api

import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SendMessage
import com.blr19c.falowp.bot.system.scheduling.Scheduling
import kotlin.reflect.KClass

/**
 * 定时任务的botApi
 */
class SchedulingBotApi(originalClass: KClass<*>) : BotApi(ReceiveMessage.empty(), originalClass) {
    private val receiveList = arrayListOf<String>()

    override suspend fun sendGroup(vararg sendMessage: SendMessage, reference: Boolean, forward: Boolean) {
        receiveList.map { Scheduling.selectBot(it, originalClass) }
            .forEach { it?.sendGroup(*sendMessage, reference = reference, forward = forward) }
        receiveList.clear()
    }

    override suspend fun sendAllGroup(vararg sendMessage: SendMessage, reference: Boolean, forward: Boolean) {
        Scheduling.allBot(originalClass).map { it.sendAllGroup(*sendMessage, reference = reference, forward = forward) }
        receiveList.clear()
    }

    override suspend fun sendPrivate(vararg sendMessage: SendMessage, reference: Boolean, forward: Boolean) {
        receiveList.map { Scheduling.selectBot(it, originalClass) }
            .forEach { it?.sendPrivate(*sendMessage, reference = reference, forward = forward) }
        receiveList.clear()
    }

    override suspend fun sendReply(vararg sendMessage: SendMessage, reference: Boolean, forward: Boolean) {
        throw IllegalStateException("定时任务BotAPi无法回复消息")
    }

    /**
     * 添加接收人
     */
    fun addReceive(receive: List<String>) {
        receiveList.addAll(receive)
    }

}