package com.blr19c.falowp.bot.system.plugin

import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.listener.hooks.MessagePluginExecutionHook
import com.blr19c.falowp.bot.system.listener.hooks.ReceiveMessageHook
import com.blr19c.falowp.bot.system.plugin.event.EventManager
import com.blr19c.falowp.bot.system.plugin.hook.HookManager
import com.blr19c.falowp.bot.system.plugin.hook.withPluginHook
import com.blr19c.falowp.bot.system.systemConfigListProperty
import com.blr19c.falowp.bot.system.utils.ScanUtils
import kotlinx.coroutines.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import kotlin.streams.asSequence

/**
 * 插件管理
 */
object PluginManagement : Log {

    private val messagePlugins = CopyOnWriteArrayList<MessagePluginRegister>()
    private val executor = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * 注册消息插件
     */
    fun registerMessage(pluginRegister: MessagePluginRegister) {
        messagePlugins.add(pluginRegister)
    }

    fun configure() {
        log().info("初始化插件")
        val pluginPackage = systemConfigListProperty("pluginPackage")
        if (!pluginPackage.all { it.endsWith(".plugins") }) {
            throw IllegalStateException("pluginPackage:${pluginPackage}路径必须使用/plugins路径结尾")
        }
        val pluginList = pluginPackage
            .map(ScanUtils::scanPackage)
            .flatMap { it.stream().asSequence() }
            .mapNotNull { initPlugin(it) }
            .toList()
        val nameList = pluginList
            .sortedBy { it.plugin.name }
            .filter { it.plugin.enable }
            .map { it.plugin.name }
            .toList()
        messagePlugins.sortBy { it.order }
        log().info("已加载的插件列表:{}", nameList)
        EventManager.configure(pluginList)
        HookManager.configure(pluginList)
        log().info("初始化插件完成")
    }

    /**
     * 处理消息
     *
     * @param receiveMessage 接收到的message
     * @param botApiClass 使用的botApi
     */
    suspend fun <T : BotApi> message(receiveMessage: ReceiveMessage, botApiClass: KClass<T>) {
        executor.launch {
            log().info("接收到消息:$receiveMessage")
            val botApi = botApiClass.primaryConstructor!!.call(receiveMessage, botApiClass) as BotApi
            withPluginHook(PluginBotApi(botApi), ReceiveMessageHook(receiveMessage)) {
                executePlugin(receiveMessage, botApiClass)
            }
        }.invokeOnCompletion { exception ->
            exception ?: return@invokeOnCompletion log().info("消息处理完毕:${receiveMessage.id}")
            log().error("处理消息失败:{}", receiveMessage, exception)
        }
    }

    /**
     * 根据消息执行消息插件
     */
    private suspend fun <T : BotApi> executePlugin(message: ReceiveMessage, botApiClass: KClass<T>) = coroutineScope {
        val allPluginJob = mutableListOf<Job>()
        for (plugin in messagePlugins.filter { filterMessagePlugin(message, it) }) {
            val originalBotApi = botApiClass.primaryConstructor!!.call(message, plugin.originalClass) as BotApi
            val botApi = PluginBotApi(originalBotApi)
            val args = plugin.match.regex?.find(message.content.message)?.destructured?.toList() ?: listOf()
            val job = launch {
                withPluginHook(botApi, MessagePluginExecutionHook(message, plugin)) {
                    plugin.block(botApi, args.toTypedArray())
                }
            }
            job.invokeOnCompletion { exception ->
                exception?.let { log().error("插件:${plugin.originalClass}处理失败", exception) }
            }
            allPluginJob.add(job)
            if (plugin.terminateEvent) {
                break
            }
        }
        allPluginJob.joinAll()
    }

    private fun filterMessagePlugin(receiveMessage: ReceiveMessage, plugin: MessagePluginRegister): Boolean {
        return plugin.match.regex?.matches(receiveMessage.content.message) ?: true
                && plugin.match.sendId?.contains(receiveMessage.sender.id) ?: true
                && plugin.match.sourceType?.equals(receiveMessage.source.type) ?: true
                && plugin.match.messageType?.equals(receiveMessage.messageType) ?: true
                && plugin.match.atMe?.let { receiveMessage.atMe() } ?: true
                && plugin.match.customBlock?.invoke(receiveMessage) ?: true
    }

    private fun initPlugin(plugin: Class<*>): PluginInfo? {
        val annotation = plugin.getAnnotation(Plugin::class.java) ?: return null
        if (!annotation.enable) return null
        return PluginInfo(plugin.constructors.first().newInstance(), annotation)
    }
}