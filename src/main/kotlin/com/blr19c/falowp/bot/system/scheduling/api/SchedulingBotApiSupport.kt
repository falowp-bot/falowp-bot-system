package com.blr19c.falowp.bot.system.scheduling.api

import com.blr19c.falowp.bot.system.api.BotApi
import kotlin.reflect.KClass

/**
 * 定时任务支持
 */
interface SchedulingBotApiSupport {

    /**
     * @param receiveId 接收人id
     */
    suspend fun supportReceive(receiveId: String): Boolean

    /**
     * @param receiveId 接收人id
     */
    suspend fun bot(receiveId: String, originalClass: KClass<*>): BotApi

    /**
     * 排序
     */
    fun order(): Int = 0

}
