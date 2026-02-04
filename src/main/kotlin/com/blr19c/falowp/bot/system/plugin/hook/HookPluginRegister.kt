@file:Suppress("UNUSED")

package com.blr19c.falowp.bot.system.plugin.hook

import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.PluginRegister
import com.blr19c.falowp.bot.system.utils.ScanUtils.getCallerClass
import kotlin.reflect.KClass

/**
 * 钩子类插件
 */
data class HookPluginRegister<T : Plugin.Listener.Hook>(
    /**
     * 排序
     */
    val order: Int,
    /**
     * 监听的钩子类型
     */
    val listener: KClass<T>,
    /**
     * 钩子类型
     */
    val hookType: HookTypeEnum,
    /**
     * 匹配规则
     */
    val match: HookPluginRegisterMatch = HookPluginRegisterMatch.allMatch(),
    /**
     * 执行内容
     */
    val block: suspend HookJoinPoint.(hook: T) -> Unit,
    override val originalClass: KClass<*> = getCallerClass()
) : PluginRegister() {

    override fun register() {
        HookManager.registerHook(this)
    }

    override fun unregister() {
        HookManager.unregister(this)
    }

    suspend fun hook(hookJoinPoint: HookJoinPoint, event: Any) {
        @Suppress("UNCHECKED_CAST")//因为data class无法使用reified，导致T被擦出无法正确识别T
        block.invoke(hookJoinPoint, event as T)
    }
}