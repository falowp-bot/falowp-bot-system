package com.blr19c.falowp.bot.system.expand

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import java.util.concurrent.atomic.LongAdder

/**
 * Channel形式的Queue
 * 新增了计数(非精确)
 */
class ChannelQueue<E>(capacity: Int = Channel.UNLIMITED) {
    private val channel = Channel<E>(capacity)
    private val count = LongAdder()

    fun add(element: E): Boolean {
        if (channel.trySend(element).isFailure) {
            throw IllegalStateException("Queue full")
        }
        count.increment()
        return true
    }

    fun remove(): E {
        val result = channel.tryReceive()
        val element = result.getOrNull() ?: throw NoSuchElementException()
        count.decrement()
        return element
    }

    fun offer(element: E): Boolean {
        if (channel.trySend(element).isSuccess) {
            count.increment()
            return true
        }
        return false
    }

    fun poll(): E? {
        val element = channel.tryReceive().getOrNull() ?: return null
        count.decrement()
        return element
    }

    suspend fun take(): E {
        val element = channel.receive()
        count.decrement()
        return element
    }

    suspend fun drainTo(action: suspend (E) -> Unit) {
        channel.consumeEach {
            count.decrement()
            action.invoke(it)
        }
    }

    fun size(): Int = count.sum().toInt()
}
