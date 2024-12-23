package com.blr19c.falowp.bot.system.plugin

import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.expand.ChannelQueue
import com.blr19c.falowp.bot.system.listener.hooks.MessagePluginExecutionHook
import com.blr19c.falowp.bot.system.listener.hooks.ReceiveMessageHook
import com.blr19c.falowp.bot.system.plugin.event.EventManager
import com.blr19c.falowp.bot.system.plugin.hook.HookManager
import com.blr19c.falowp.bot.system.plugin.hook.withPluginHook
import com.blr19c.falowp.bot.system.systemConfigListProperty
import com.blr19c.falowp.bot.system.utils.ScanUtils
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import kotlin.streams.asSequence

/**
 * 插件管理
 */
object PluginManagement : Log {

    private val messagePlugins = CopyOnWriteArrayList<MessagePluginRegister>()
    private val queueMessageInfos = ConcurrentHashMap<String, QueueMessagePluginRegister>()
    private val queueMessagePlugins = ConcurrentHashMap<String, ChannelQueue<BotApiJob>>()
    private val executor = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * 注册消息插件
     */
    fun registerMessage(pluginRegister: MessagePluginRegister) {
        messagePlugins.add(pluginRegister)
    }

    /**
     * 取消注册消息插件
     */
    fun unregisterMessage(pluginRegister: MessagePluginRegister) {
        messagePlugins.remove(pluginRegister)
    }

    /**
     * 注册消息插件
     */
    fun registerMessage(pluginRegister: QueueMessagePluginRegister) {
        messagePlugins.add(pluginRegister.messagePluginRegister)
        queueMessageInfos[pluginRegister.pluginId] = pluginRegister
        queueMessagePlugins[pluginRegister.pluginId] = ChannelQueue(capacity = pluginRegister.queueCapacity)
        executor.launch {
            queueMessagePlugins[pluginRegister.pluginId]!!.drainTo { botApiJob ->
                botApiJob.job.start()
                botApiJob.job.join()
            }
        }
    }

    /**
     * 取消注册消息插件
     */
    fun unregisterMessage(pluginRegister: QueueMessagePluginRegister) {
        messagePlugins.remove(pluginRegister.messagePluginRegister)
        queueMessageInfos.remove(pluginRegister.pluginId)
        queueMessagePlugins.remove(pluginRegister.pluginId)?.close()
    }

    fun configure() {
        log().info("初始化插件")
        val pluginPackage = systemConfigListProperty("pluginPackage")
        if (!pluginPackage.all { it.endsWith(".plugins") }) {
            throw IllegalStateException("pluginPackage:${pluginPackage}路径必须使用/plugins路径结尾")
        }
        val classList = pluginPackage
            .map(ScanUtils::scanPackage)
            .flatMap { it.stream().asSequence() }

        classList.forEach { initPluginUtils(it) }

        val pluginList = classList
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
    fun <T : BotApi> message(receiveMessage: ReceiveMessage, botApiClass: KClass<T>) {
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
        for (plugin in messagePlugins.filter { it.match.checkMath(message) }) {
            val originalBotApi = botApiClass.primaryConstructor!!.call(message, plugin.originalClass) as BotApi
            val botApiJob = this.buildJob(message, plugin, originalBotApi)
            if (!executeQueueMessagePlugin(plugin, botApiJob)) {
                botApiJob.job.start()
                allPluginJob.add(botApiJob.job)
            }
            if (plugin.terminateEvent) {
                break
            }
        }
        allPluginJob.joinAll()
    }

    private suspend fun executeQueueMessagePlugin(plugin: MessagePluginRegister, botApiJob: BotApiJob): Boolean {
        if (!queueMessageInfos.containsKey(plugin.pluginId)) {
            return false
        }
        val queueMessageInfo = queueMessageInfos[plugin.pluginId]!!

        val channel = queueMessagePlugins[plugin.pluginId]!!
        if (channel.offer(botApiJob)) {
            queueMessageInfo.onSuccess.invoke(botApiJob.botApi, channel.size())
        } else {
            queueMessageInfo.onOverFlow.invoke(botApiJob.botApi)
        }
        return true
    }


    private fun CoroutineScope.buildJob(
        message: ReceiveMessage,
        plugin: MessagePluginRegister,
        originalBotApi: BotApi
    ): BotApiJob {
        val botApi = PluginBotApi(originalBotApi)
        val args = plugin.match.regex?.find(message.content.message)?.destructured?.toList() ?: listOf()
        val job = launch(start = CoroutineStart.LAZY) {
            withPluginHook(botApi, MessagePluginExecutionHook(message, plugin)) {
                plugin.block(botApi, args.toTypedArray())
            }
        }
        job.invokeOnCompletion { exception ->
            exception?.let { log().error("插件:${plugin.originalClass}处理失败", exception) }
        }
        return BotApiJob(botApi, job)
    }

    private fun initPluginUtils(plugin: Class<*>) {
        if (plugin.isAnnotationPresent(PluginUtils::class.java)) {
            plugin.kotlin.objectInstance
        }
    }

    private fun initPlugin(plugin: Class<*>): PluginInfo? {
        val annotation = plugin.getAnnotation(Plugin::class.java) ?: return null
        if (!annotation.enable) return null
        return PluginInfo(plugin.constructors.first().newInstance(), annotation)
    }

    data class BotApiJob(val botApi: BotApi, val job: Job)
}