package com.blr19c.falowp.bot.system.plugin

import com.blr19c.falowp.bot.system.api.*
import com.blr19c.falowp.bot.system.plugin.event.EventManager
import com.blr19c.falowp.bot.system.plugin.hook.HookJoinPoint
import com.blr19c.falowp.bot.system.plugin.hook.HookManager
import com.blr19c.falowp.bot.system.plugin.hook.HookTypeEnum
import com.blr19c.falowp.bot.system.scheduling.Scheduling
import com.blr19c.falowp.bot.system.scheduling.cron.Trigger
import com.blr19c.falowp.bot.system.utils.ScanUtils.getCallerClass
import kotlinx.coroutines.channels.Channel
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
     * 来自的适配器
     */
    val adapterId: String? = null,
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
        return this.regex?.matches(receiveMessage.content.message) != false
                && this.sendId?.contains(receiveMessage.sender.id) != false
                && this.sourceType?.equals(receiveMessage.source.type) != false
                && this.messageType?.equals(receiveMessage.messageType) != false
                && this.adapterId?.equals(receiveMessage.adapter.id) != false
                && this.atMe?.let { receiveMessage.atMe() } != false
                && this.customBlock?.invoke(receiveMessage) != false
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

    override fun unregister() {
        PluginManagement.unregisterMessage(this)
    }
}

/**
 * 队列消息类插件
 */
data class QueueMessagePluginRegister(
    /**
     * 原消息类插件
     */
    val messagePluginRegister: MessagePluginRegister,
    /**
     * 最大等待长度限制
     */
    val queueCapacity: Int = Channel.UNLIMITED,
    /**
     * 成功进入队列回调
     */
    val onSuccess: suspend BotApi.(queueIndex: Int) -> Unit = {},
    /**
     * 超过最大等待长度限制回调
     */
    val onOverFlow: suspend BotApi.() -> Unit = {},

    override val pluginId: String = messagePluginRegister.pluginId,
    override val originalClass: KClass<*> = messagePluginRegister.originalClass
) : PluginRegister() {

    override fun register() {
        PluginManagement.registerMessage(this)
    }

    override fun unregister() {
        PluginManagement.unregisterMessage(this)
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
    val block: suspend BotApi.() -> Unit,
    override val originalClass: KClass<*> = getCallerClass()
) : PluginRegister() {

    override fun register() {
        Scheduling.registerTask(this)
    }

    override fun unregister() {
        Scheduling.unregisterTask(this)
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

    suspend fun publish(botApi: BotApi, event: Any) {
        @Suppress("UNCHECKED_CAST")//因为data class无法使用reified，导致T被擦出无法正确识别T
        block.invoke(botApi, event as T)
    }

    override fun register() {
        EventManager.registerEvent(this)
    }

    override fun unregister() {
        EventManager.unregisterEvent(this)
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
