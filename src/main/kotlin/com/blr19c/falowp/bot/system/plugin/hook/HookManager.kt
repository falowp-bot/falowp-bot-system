package com.blr19c.falowp.bot.system.plugin.hook

import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.plugin.HookPluginRegister
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.PluginInfo
import com.blr19c.falowp.bot.system.plugin.hook.HookTypeEnum.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 钩子类插件管理
 */
object HookManager : Log {
    private val hookPlugins = ConcurrentHashMap<String, HookPluginRegister<out Plugin.Listener.Hook>>()
    private val pluginList = mutableListOf<PluginInfo>()

    fun configure(pluginList: List<PluginInfo>) {
        this.pluginList.addAll(pluginList)
        log().info("加载的钩子监听器数量:{}", hookPlugins.size)
    }

    fun <T : Plugin.Listener.Hook> registerHook(hookPluginRegister: HookPluginRegister<T>) {
        hookPlugins[hookPluginRegister.pluginId] = hookPluginRegister
    }


    fun <T : Plugin.Listener.Hook> unregister(hookPluginRegister: HookPluginRegister<T>) {
        hookPlugins.remove(hookPluginRegister.pluginId)
    }

    /**
     * 构建withHook
     */
    fun buildHookJoinPoint(
        hook: Plugin.Listener.Hook,
        botApi: BotApi,
        block: suspend () -> Unit
    ): HookJoinPoint {
        val pluginInfo = pluginList.find { botApi.originalClass == it.instance::class } ?: PluginInfo.empty()
        val allHooks = hookPlugins.values
            .filter { filterHookPlugin(pluginInfo, hook, it) }
            .sortedBy { it.order }
        val afterFinally = allHooks.filter { it.hookType == AFTER_FINALLY }
        val afterThrowing = allHooks.filter { it.hookType == AFTER_THROWING }
        val before = allHooks.filter { it.hookType == BEFORE }
        val around = allHooks.filter { it.hookType == AROUND }
        val target = listOf(TargetHookProcess(block))
        val afterReturning = allHooks.filter { it.hookType == AFTER_RETURNING }

        //Finally->Throwing->before->around->target->Returning
        val hooks = ArrayDeque<HookProcess>()
        hooks.addAll(afterFinally.map { toHookProcess(botApi, it) })
        hooks.addAll(afterThrowing.map { toHookProcess(botApi, it) })
        hooks.addAll(before.map { toHookProcess(botApi, it) })
        hooks.addAll(around.map { toHookProcess(botApi, it) })
        hooks.addAll(target)
        hooks.addAll(afterReturning.map { toHookProcess(botApi, it) })
        return NativeHookJoinPoint(pluginInfo, hook, hooks)
    }

    /**
     * 转为HookProcess
     */
    private fun toHookProcess(botApi: BotApi, register: HookPluginRegister<out Plugin.Listener.Hook>): HookProcess {
        val hookBotApi = HookBotApi(botApi, register)
        return when (register.hookType) {
            AROUND -> AroundHookProcess(register, hookBotApi)
            AFTER_THROWING -> ThrowingHookProcess(register, hookBotApi)
            AFTER_FINALLY -> FinallyHookProcess(register, hookBotApi)
            else -> NormalHookProcess(register, hookBotApi)
        }
    }

    /**
     * 过滤合适的HookPlugin
     */
    private fun filterHookPlugin(
        pluginInfo: PluginInfo,
        hook: Plugin.Listener.Hook,
        plugin: HookPluginRegister<out Plugin.Listener.Hook>
    ): Boolean {
        return plugin.listener.isInstance(hook)
                && plugin.match.customBlock?.invoke(pluginInfo) ?: true
    }
}