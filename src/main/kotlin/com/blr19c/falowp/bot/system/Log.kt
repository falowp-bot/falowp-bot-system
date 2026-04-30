package com.blr19c.falowp.bot.system

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * 日志能力
 */
interface Log {

    /**
     * 获取当前对象的日志记录器
     */
    fun Log.log(): Logger {
        return LoggerFactory.getLogger(this::class.java)
    }
}
