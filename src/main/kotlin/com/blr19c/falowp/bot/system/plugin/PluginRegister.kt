package com.blr19c.falowp.bot.system.plugin

import java.util.*
import kotlin.reflect.KClass

interface Register {

    /**
     * 执行注册
     */
    fun register()
}

interface UnRegister {

    /**
     * 取消注册
     */
    fun unregister()
}

/**
 * 插件注册器
 */
abstract class PluginRegister : Register, UnRegister {
    /**
     * id
     */
    open val pluginId: String = UUID.randomUUID().toString()

    /**
     * 声明plugin的class
     */
    abstract val originalClass: KClass<*>
}