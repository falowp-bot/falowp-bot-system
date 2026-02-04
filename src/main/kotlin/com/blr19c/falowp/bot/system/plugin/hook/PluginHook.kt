@file:Suppress("UNUSED")

package com.blr19c.falowp.bot.system.plugin.hook

import com.blr19c.falowp.bot.system.plugin.Plugin.Listener.Hook
import com.blr19c.falowp.bot.system.plugin.PluginRegister

/**
 * 钩子
 * @param hookType 类型
 * @param order 顺序
 * @param block 执行内容
 */
inline fun <reified T : Hook> hook(
    hookType: HookTypeEnum,
    order: Int = 0,
    match: HookPluginRegisterMatch = HookPluginRegisterMatch.allMatch(),
    noinline block: suspend HookJoinPoint.(T) -> Unit
): PluginRegister {
    return HookPluginRegister(order, T::class, hookType, match, block)
}

/**
 * 前置
 */
inline fun <reified T : Hook> beforeHook(
    order: Int = 0,
    match: HookPluginRegisterMatch = HookPluginRegisterMatch.allMatch(),
    noinline block: suspend HookJoinPoint.(T) -> Unit
): PluginRegister {
    return hook<T>(HookTypeEnum.BEFORE, order, match, block)
}

/**
 * 后置
 */
inline fun <reified T : Hook> afterReturningHook(
    order: Int = 0,
    match: HookPluginRegisterMatch = HookPluginRegisterMatch.allMatch(),
    noinline block: suspend HookJoinPoint.(T) -> Unit
): PluginRegister {
    return hook<T>(HookTypeEnum.AFTER_RETURNING, order, match, block)
}

/**
 * 异常通知
 */
inline fun <reified T : Hook> afterThrowingHook(
    order: Int = 0,
    match: HookPluginRegisterMatch = HookPluginRegisterMatch.allMatch(),
    noinline block: suspend HookJoinPoint.(T) -> Unit
): PluginRegister {
    return hook<T>(HookTypeEnum.AFTER_THROWING, order, match, block)
}

/**
 * 最终通知
 */
inline fun <reified T : Hook> afterFinallyHook(
    order: Int = 0,
    match: HookPluginRegisterMatch = HookPluginRegisterMatch.allMatch(),
    noinline block: suspend HookJoinPoint.(T) -> Unit
): PluginRegister {
    return hook<T>(HookTypeEnum.AFTER_FINALLY, order, match, block)
}

/**
 * 环绕
 */
inline fun <reified T : Hook> aroundHook(
    order: Int = 0,
    match: HookPluginRegisterMatch = HookPluginRegisterMatch.allMatch(),
    noinline block: suspend HookJoinPoint.(T) -> Unit
): PluginRegister {
    return hook<T>(HookTypeEnum.AROUND, order, match, block)
}