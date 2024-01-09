package com.blr19c.falowp.bot.system.listener.events

import com.blr19c.falowp.bot.system.plugin.Plugin


/**
 * 早晚安事件(晚安之后系统进入闲时状态,直到第二天早安)
 */
data class GreetingEvent(
    /**
     * 早安
     */
    val goodMorning: Boolean,
    /**
     * 晚安
     */
    val goodNight: Boolean,
) : Plugin.Listener.Event
