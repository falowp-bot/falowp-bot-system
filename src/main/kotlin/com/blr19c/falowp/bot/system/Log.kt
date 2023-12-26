package com.blr19c.falowp.bot.system

import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface Log {

    fun Log.log(): Logger {
        return LoggerFactory.getLogger(this::class.java)
    }
}