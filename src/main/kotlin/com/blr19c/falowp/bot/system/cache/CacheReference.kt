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
@Suppress("UNUSED")
class CacheReference<T : Any>(
    duration: Duration,
    private val block: suspend () -> T,
) {
    private val cache = CacheBuilder.newBuilder()
        .expireAfterWrite(duration.toJavaDuration())
        .build(CacheLoader.from<String, T> { _ -> runBlocking { block.invoke() } })

    /**
     * 作为委托属性读取缓存值
     */
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return cache.get("onlyKey")
    }

    /**
     * 刷新缓存值
     */
    fun refresh() {
        return cache.refresh("onlyKey")
    }
}