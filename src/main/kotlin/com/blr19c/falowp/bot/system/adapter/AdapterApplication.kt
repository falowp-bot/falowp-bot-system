package com.blr19c.falowp.bot.system.adapter

import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.adapter.gocqhttp.GoCQHttpApplication
import com.blr19c.falowp.bot.system.adapter.qq.QQApplication
import com.blr19c.falowp.bot.system.systemConfigListProperty
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.LongAdder

/**
 * 对于不同的api适配
 */
object AdapterApplication : Log {

    private val loadSize = LongAdder()

    suspend fun configure() = runBlocking {
        log().info("初始化协议适配")
        launch { runAdapter("qq") { QQApplication.configure() } }
        launch { runAdapter("gocqhttp") { GoCQHttpApplication.configure() } }
        log().info("初始化协议适配完成:{}", systemConfigListProperty("adapter.enableAdapter"))
    }

    private suspend fun runAdapter(adapterName: String, block: suspend () -> Unit) {
        if (systemConfigListProperty("adapter.enableAdapter").contains(adapterName))
            block.invoke()
    }

    fun onload() {
        loadSize.increment()
    }

    fun isLoadingCompleted(): Boolean {
        val size = systemConfigListProperty("adapter.enableAdapter").size
        return loadSize.sum() == size.toLong()
    }
}
