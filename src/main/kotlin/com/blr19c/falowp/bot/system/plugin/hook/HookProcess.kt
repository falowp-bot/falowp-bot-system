package com.blr19c.falowp.bot.system.plugin.hook

import com.blr19c.falowp.bot.system.plugin.HookPluginRegister
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * hook处理程序
 */
interface HookProcess {

    /**
     * 链式处理
     */
    suspend fun process(hookJoinPoint: HookJoinPoint, hook: Plugin.Listener.Hook)

    /**
     * 获取botApi
     */
    fun botApi(): HookBotApi
}

/**
 * 普通的
 */
open class NormalHookProcess(
    private val hookPluginRegister: HookPluginRegister<out Plugin.Listener.Hook>,
    private val hookBotApi: HookBotApi
) : HookProcess {

    override suspend fun process(hookJoinPoint: HookJoinPoint, hook: Plugin.Listener.Hook) {
        val botApi = hookJoinPoint.botApi()
        hookPluginRegister.hook(SpecifiedBotApiNativeHookJoinPoint(botApi, hookJoinPoint), hook)
        hookJoinPoint.process()
    }

    override fun botApi(): HookBotApi {
        return hookBotApi
    }
}

/**
 * Around环绕的
 */
class AroundHookProcess(
    private val hookPluginRegister: HookPluginRegister<out Plugin.Listener.Hook>,
    private val hookBotApi: HookBotApi
) : HookProcess {

    override suspend fun process(hookJoinPoint: HookJoinPoint, hook: Plugin.Listener.Hook) {
        val botApi = hookJoinPoint.botApi()
        hookPluginRegister.hook(SpecifiedBotApiNativeHookJoinPoint(botApi, hookJoinPoint), hook)
    }

    override fun botApi(): HookBotApi {
        return hookBotApi
    }
}

/**
 * Throwing异常的
 */
class ThrowingHookProcess(
    private val hookPluginRegister: HookPluginRegister<out Plugin.Listener.Hook>,
    private val hookBotApi: HookBotApi
) : HookProcess {

    override suspend fun process(hookJoinPoint: HookJoinPoint, hook: Plugin.Listener.Hook) {
        val botApi = hookJoinPoint.botApi()
        try {
            hookJoinPoint.process()
        } catch (ex: Throwable) {
            hookPluginRegister.hook(SpecifiedBotApiNativeHookJoinPoint(botApi, hookJoinPoint), hook)
            throw ex
        }
    }

    override fun botApi(): HookBotApi {
        return hookBotApi
    }
}

/**
 * Finally最终的
 */
class FinallyHookProcess(
    private val hookPluginRegister: HookPluginRegister<out Plugin.Listener.Hook>,
    private val hookBotApi: HookBotApi
) : HookProcess {

    override suspend fun process(hookJoinPoint: HookJoinPoint, hook: Plugin.Listener.Hook) {
        val botApi = hookJoinPoint.botApi()
        try {
            hookJoinPoint.process()
        } finally {
            hookPluginRegister.hook(SpecifiedBotApiNativeHookJoinPoint(botApi, hookJoinPoint), hook)
        }
    }

    override fun botApi(): HookBotApi {
        return hookBotApi
    }
}

/**
 * 目标对象的
 */
class TargetHookProcess(
    private val block: suspend () -> Unit
) : HookProcess {

    override suspend fun process(hookJoinPoint: HookJoinPoint, hook: Plugin.Listener.Hook) {
        block.invoke()
        hookJoinPoint.process()
    }

    override fun botApi(): HookBotApi {
        throw IllegalStateException("TargetHookProcess不应该使用process中的botApi")
    }

}

