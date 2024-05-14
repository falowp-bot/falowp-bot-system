package com.blr19c.falowp.bot.system.adapter

import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.scheduling.api.SchedulingBotApiSupport
import com.blr19c.falowp.bot.system.systemConfigListProperty
import com.blr19c.falowp.bot.system.utils.ScanUtils
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.LongAdder
import kotlin.streams.asSequence

/**
 * 对于不同的api适配
 */
object AdapterApplication : Log {

    private lateinit var load: Any
    private val loadSize = LongAdder()
    private val loadAdapter = CopyOnWriteArrayList<BotAdapterInterface>()
    private val botApiSupportList = CopyOnWriteArrayList<SchedulingBotApiSupport>()

    suspend fun configure() = coroutineScope {
        log().info("初始化协议适配")
        val botAdapterRegister = BotAdapterRegister(loadAdapter)
        systemConfigListProperty("adapterPackage")
            .map(ScanUtils::scanPackage)
            .flatMap { it.stream().asSequence() }
            .forEach { launch { initAdapter(it, botAdapterRegister) } }
        log().info("初始化协议适配完成")
    }

    private suspend fun initAdapter(adapter: Class<*>, botAdapterRegister: BotAdapterRegister): BotAdapterInfo? {
        if (SchedulingBotApiSupport::class.java.isAssignableFrom(adapter)) {
            botApiSupportList.add(adapter.kotlin.objectInstance as SchedulingBotApiSupport)
            botApiSupportList.sortBy { it.order() }
            return null
        }
        val annotation = adapter.getAnnotation(BotAdapter::class.java) ?: return null
        @Suppress("UNCHECKED_CAST")
        adapter as Class<out BotAdapterInterface>
        loadSize.increment()
        load = true
        val adapterInstance = adapter.constructors.first().newInstance() as BotAdapterInterface
        adapterInstance.start(botAdapterRegister)
        return BotAdapterInfo(annotation.name, adapter)
    }

    fun isLoadingCompleted(): Boolean {
        return ::load.isInitialized && loadSize.toInt() == loadAdapter.size
    }

    fun botApiSupportList(): List<SchedulingBotApiSupport> {
        return Collections.unmodifiableList(botApiSupportList)
    }
}
