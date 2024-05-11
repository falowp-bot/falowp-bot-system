package com.blr19c.falowp.bot.system.cache

import kotlinx.coroutines.runBlocking
import kotlin.reflect.KProperty

/**
 * 仅能读取一次的数据
 */
class OnlyReadOnceReference<T>(
    private val block: suspend () -> T,
) {
    @Volatile
    private var firstCall = true

    @Synchronized
    operator fun getValue(thisRef: Any, property: KProperty<*>): T? {
        return if (firstCall) {
            firstCall = false
            runBlocking { block.invoke() }
        } else {
            null
        }
    }
}