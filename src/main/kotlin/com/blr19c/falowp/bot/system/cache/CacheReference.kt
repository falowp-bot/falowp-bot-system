package com.blr19c.falowp.bot.system.cache

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KProperty
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/**
 * 缓存
 */
class CacheReference<T>(
    duration: Duration,
    private val block: suspend () -> T,
) {
    private val cache = CacheBuilder.newBuilder()
        .expireAfterWrite(duration.toJavaDuration())
        .build(CacheLoader.from<String, T> { _ -> runBlocking { block.invoke() } })

    operator fun getValue(thisRef: Any, property: KProperty<*>): T {
        return cache.get("onlyKey")
    }

    fun refresh() {
        return cache.refresh("onlyKey")
    }
}