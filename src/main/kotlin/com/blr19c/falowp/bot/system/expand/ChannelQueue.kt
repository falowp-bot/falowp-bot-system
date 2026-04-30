package com.blr19c.falowp.bot.system.expand

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.consumeEach
import java.util.concurrent.atomic.LongAdder

/**
 * Channel形式的Queue
 * 新增了计数(非精确)
 */
@Suppress("UNUSED")
class ChannelQueue<E>(capacity: Int = Channel.UNLIMITED) {
    private val channel = Channel<E>(capacity)
    private val count = LongAdder()

    /**
     * 添加元素，队列已满时抛出异常
     */
    fun add(element: E): Boolean {
        if (channel.trySend(element).isFailure) {
            throw IllegalStateException("Queue full")
        }
        count.increment()
        return true
    }

    /**
     * 移除并返回队首元素
     */
    fun remove(): E {
        val result = channel.tryReceive()
        val element = result.getOrNull() ?: throw NoSuchElementException()
        count.decrement()
        return element
    }

    /**
     * 尝试添加元素
     */
    fun offer(element: E): Boolean {
        if (channel.trySend(element).isSuccess) {
            count.increment()
            return true
        }
        return false
    }

    /**
     * 尝试移除并返回队首元素
     */
    fun poll(): E? {
        val element = channel.tryReceive().getOrNull() ?: return null
        count.decrement()
        return element
    }

    /**
     * 挂起等待并返回队首元素
     */
    suspend fun take(): E {
        val element = channel.receive()
        count.decrement()
        return element
    }

    /**
     * 持续消费队列中的元素
     */
    suspend fun drainTo(action: suspend (E) -> Unit) {
        try {
            channel.consumeEach {
                count.decrement()
                action.invoke(it)
            }
        } catch (_: ClosedReceiveChannelException) {

        }
    }

    /**
     * 获取当前队列大小
     */
    fun size(): Int = count.sum().toInt()

    /**
     * 关闭队列
     */
    fun close() {
        channel.close()
    }
}
