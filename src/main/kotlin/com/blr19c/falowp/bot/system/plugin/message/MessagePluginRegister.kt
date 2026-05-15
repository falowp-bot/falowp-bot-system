package com.blr19c.falowp.bot.system.plugin.message

import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.PluginManagement
import com.blr19c.falowp.bot.system.plugin.PluginRegister
import com.blr19c.falowp.bot.system.utils.ScanUtils.getCallerClass
import kotlin.reflect.KClass

/**
 * 消息类插件
 */
data class MessagePluginRegister(
    /**
     * 排序
     */
    val order: Int,
    /**
     * 匹配规则
     */
    val match: MessageMatch,
    /**
     * 执行完终止事件传播
     */
    val terminateEvent: Boolean,
    /**
     * 执行内容
     */
    val block: suspend BotApi.(args: Array<String>) -> Unit,
    override val originalClass: KClass<*> = getCallerClass(),
    /**
     * 插件注解信息
     */
    val plugin: Plugin = originalClass.java.getAnnotation(Plugin::class.java)
) : PluginRegister() {

    /**
     * 注册消息插件
     */
    override fun register() {
        PluginManagement.registerMessage(this)
    }

    /**
     * 取消注册消息插件
     */
    override fun unregister() {
        PluginManagement.unregisterMessage(this)
    }
}

/**
 * 消息类插件信息
 */
data class MessagePluginInfo(
    /**
     * 消息类插件注册器ID
     */
    val pluginId: String,
    /**
     * 排序
     */
    val order: Int,
    /**
     * 匹配规则
     */
    val match: MessageMatch,
    /**
     * 执行完终止事件传播
     */
    val terminateEvent: Boolean,
    /**
     * 声明plugin的class
     */
    val originalClass: KClass<*>,
    /**
     * 插件名称
     */
    val pluginName: String,
    /**
     * 插件描述
     */
    val pluginDesc: String,
    /**
     * 插件标签
     */
    val pluginTag: String,
    /**
     * 插件是否启用
     */
    val pluginEnable: Boolean,
    /**
     * 插件是否隐藏
     */
    val pluginHidden: Boolean,
) {
    constructor(register: MessagePluginRegister) : this(
        pluginId = register.pluginId,
        order = register.order,
        match = register.match,
        terminateEvent = register.terminateEvent,
        originalClass = register.originalClass,
        pluginName = register.plugin.name,
        pluginDesc = register.plugin.desc,
        pluginTag = register.plugin.tag,
        pluginEnable = register.plugin.enable,
        pluginHidden = register.plugin.hidden,
    )
}
