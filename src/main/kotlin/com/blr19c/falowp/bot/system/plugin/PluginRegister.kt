package com.blr19c.falowp.bot.system.plugin

import com.blr19c.falowp.bot.system.api.*
import com.blr19c.falowp.bot.system.plugin.event.EventManager
import com.blr19c.falowp.bot.system.plugin.hook.HookJoinPoint
import com.blr19c.falowp.bot.system.plugin.hook.HookManager
import com.blr19c.falowp.bot.system.plugin.hook.HookTypeEnum
import com.blr19c.falowp.bot.system.scheduling.Scheduling
import com.blr19c.falowp.bot.system.scheduling.api.SchedulingBotApi
import com.blr19c.falowp.bot.system.scheduling.cron.Trigger
import com.blr19c.falowp.bot.system.utils.ScanUtils.getCallerClass
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
abstract class PluginRegister : Register {
    /**
     * id
     */
    val pluginId: String = UUID.randomUUID().toString()

    /**
     * 声明plugin的class
     */
    abstract val originalClass: KClass<*>
}

/**
 * 消息类插件匹配规则
 */
data class MessagePluginRegisterMatch(
    /**
     * 正则匹配内容
     */
    val regex: Regex? = null,
    /**
     * 权限
     */
    val auth: ApiAuth? = null,
    /**
     * 仅响应@机器人的
     */
    val atMe: Boolean? = null,
    /**
     * 匹配发送人
     */
    val sendId: List<String>? = null,
    /**
     * 匹配消息来源
     */
    val sourceType: SourceTypeEnum? = null,
    /**
     * 匹配消息类型
     */
    val messageType: MessageTypeEnum? = null,
    /**
     * 自定义匹配
     */
    val customBlock: ((ReceiveMessage) -> Boolean)? = null,
) {
    companion object {
        fun allMatch(): MessagePluginRegisterMatch {
            return MessagePluginRegisterMatch()
        }
    }

    fun checkMath(receiveMessage: ReceiveMessage): Boolean {
        return this.regex?.matches(receiveMessage.content.message) ?: true
                && this.sendId?.contains(receiveMessage.sender.id) ?: true
                && this.sourceType?.equals(receiveMessage.source.type) ?: true
                && this.messageType?.equals(receiveMessage.messageType) ?: true
                && this.atMe?.let { receiveMessage.atMe() } ?: true
                && this.customBlock?.invoke(receiveMessage) ?: true
    }
}

/**
 * 钩子类插件匹配规则
 */
data class HookPluginRegisterMatch(
    /**
     * 自定义匹配
     */
    val customBlock: ((PluginInfo) -> Boolean)? = null,
) {
    companion object {
        fun allMatch(): HookPluginRegisterMatch {
            return HookPluginRegisterMatch()
        }
    }
}

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
    val match: MessagePluginRegisterMatch,
    /**
     * 执行完终止事件传播
     */
    val terminateEvent: Boolean,
    /**
     * 执行内容
     */
    val block: suspend BotApi.(args: Array<String>) -> Unit,
    override val originalClass: KClass<*> = getCallerClass()
) : PluginRegister() {

    override fun register() {
        PluginManagement.registerMessage(this)
    }
}

/**
 * 任务类插件
 */
data class TaskPluginRegister(
    /**
     * 触发器
     */
    val trigger: Trigger,
    /**
     * 执行内容
     */
    val block: suspend SchedulingBotApi.() -> Unit,
    override val originalClass: KClass<*> = getCallerClass()
) : PluginRegister() {

    override fun register() {
        Scheduling.registerTask(this)
    }
}

/**
 * 事件类插件
 */
data class EventPluginRegister<T : Plugin.Listener.Event>(
    /**
     * 监听的事件类型
     */
    val listener: KClass<T>,
    /**
     * 执行内容
     */
    val block: suspend BotApi.(event: T) -> Unit,
    override val originalClass: KClass<*> = getCallerClass()
) : PluginRegister() {

    override fun register() {
        EventManager.registerEvent(this)
    }

    suspend fun publish(botApi: BotApi, event: Any) {
        @Suppress("UNCHECKED_CAST")//因为data class无法使用reified，导致T被擦出无法正确识别T
        block.invoke(botApi, event as T)
    }
}

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
) : PluginRegister(), UnRegister {

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
