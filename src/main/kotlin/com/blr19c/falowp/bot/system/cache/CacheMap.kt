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
class CacheMap<K : Any, V : Any>(
    duration: Duration,
    maximumSize: Long? = null,
    private val block: suspend (K) -> V,
) {
    private val cache = CacheBuilder.newBuilder()
        .expireAfterWrite(duration.toJavaDuration())
        .apply { maximumSize?.let { maximumSize(it) } }
        .build(CacheLoader.from<K, V> { key -> runBlocking { block.invoke(key) } })

    operator fun getValue(thisRef: Any?, property: KProperty<*>): suspend (K) -> V {
        return { key: K -> cache.get(key) }
    }

    fun refresh(key: K) {
        return cache.refresh(key)
    }

    fun refreshAll() {
        cache.cleanUp()
    }

    fun put(key: K, value: V) {
        cache.put(key, value)
    }

    fun putAll(map: Map<K, V>) {
        cache.putAll(map)
    }
}