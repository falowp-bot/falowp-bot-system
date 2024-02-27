package com.blr19c.falowp.bot.system.plugin.hook

import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.PluginInfo
import java.util.concurrent.atomic.AtomicReference

/**
 * hook接入点
 */
interface HookJoinPoint {

    /**
     * 执行原目标方法/或者执行下一个
     */
    suspend fun process()

    /**
     * 终止执行
     */
    fun terminate()

    /**
     * 当前HookProcess的botApi
     */
    fun botApi(): BotApi

    /**
     * 插件信息
     */
    fun pluginInfo(): PluginInfo?
}

class NativeHookJoinPoint(
    private val pluginInfo: PluginInfo?,
    private val hook: Plugin.Listener.Hook,
    private val hooks: ArrayDeque<HookProcess>,
) : HookJoinPoint {

    private val terminate = AtomicReference(false)
    private val currentHookProcess = AtomicReference<HookProcess>(null)

    /**
     * 执行原目标方法/或者执行下一个
     */
    override suspend fun process() {
        if (terminate.get()) return
        val process = hooks.removeFirstOrNull() ?: return
        currentHookProcess.set(process)
        process.process(this, hook)
    }

    /**
     * 终止执行
     */
    override fun terminate() {
        terminate.set(true)
    }

    /**
     * 当前HookProcess的botApi
     */
    override fun botApi(): BotApi = currentHookProcess.get().botApi()

    override fun pluginInfo() = pluginInfo
}

class SpecifiedBotApiNativeHookJoinPoint(private val botApi: BotApi, hookJoinPoint: HookJoinPoint) :
    HookJoinPoint by hookJoinPoint {
    override fun botApi() = botApi
}

