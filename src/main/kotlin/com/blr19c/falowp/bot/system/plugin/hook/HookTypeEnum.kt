package com.blr19c.falowp.bot.system.plugin.hook

/**
 * 注入类型
 */
enum class HookTypeEnum {

    /**
     * 前置
     */
    BEFORE,

    /**
     * 后置
     */
    AFTER_RETURNING,

    /**
     * 异常通知
     */
    AFTER_THROWING,

    /**
     * 最终通知
     */
    AFTER_FINALLY,

    /**
     * 环绕
     */
    AROUND
}