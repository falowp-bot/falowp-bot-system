package com.blr19c.falowp.bot.system

import com.blr19c.falowp.bot.system.utils.ScanUtils.configPath
import com.blr19c.falowp.bot.system.utils.ScanUtils.pluginPath
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * 资源&配置
 */
object Resources : Log {

    fun configure() {
        log().info("初始化资源文件")
        applicationConfig
        log().info("初始化资源文件完成")
    }
}

private val applicationConfig by lazy { HoconApplicationConfig(ConfigFactory.load()) }
private val configPropertyMap by lazy { ConcurrentHashMap<String, String>() }
private val configListPropertyMap by lazy { ConcurrentHashMap<String, List<String>>() }
private val configDefaultValue: (String) -> String = { throw IllegalArgumentException("未找到配置:$it") }
private val configDefaultListValue: (String) -> List<String> = { throw IllegalArgumentException("未找到配置:$it") }

/**
 * 读取application.conf配置文件
 */
fun configProperty(
    key: String,
    defaultValue: (String) -> String = configDefaultValue
) = configPropertyMap.computeIfAbsent(key) {
    applicationConfig.propertyOrNull(key)?.getString() ?: defaultValue.invoke(key)
}


/**
 * 读取application.conf配置文件
 */
fun configListProperty(
    key: String,
    defaultValue: (String) -> List<String> = configDefaultListValue
) = configListPropertyMap.computeIfAbsent(key) {
    applicationConfig.propertyOrNull(key)?.getList() ?: defaultValue.invoke(key)
}


/**
 * 读取application.conf配置文件-添加系统的前缀
 */
fun systemConfigProperty(
    key: String,
    defaultValue: (String) -> String = configDefaultValue
): String {
    return configProperty("bot.system.".plus(key), defaultValue)
}


/**
 * 读取application.conf配置文件-添加系统的前缀
 */
fun systemConfigListProperty(
    key: String,
    defaultValue: (String) -> List<String> = configDefaultListValue
): List<String> {
    return configListProperty("bot.system.".plus(key), defaultValue)
}

/**
 * 读取application.conf配置文件-添加当前插件的前缀
 */
fun pluginConfigProperty(
    key: String,
    defaultValue: (String) -> String = configDefaultValue
): String {
    return configProperty(configPath().plus(key), defaultValue)
}

/**
 * 读取application.conf配置文件-添加当前插件的前缀
 */
fun pluginConfigListProperty(
    key: String,
    defaultValue: (String) -> List<String> = configDefaultListValue
): List<String> {
    return configListProperty(configPath().plus(key), defaultValue)
}

/**
 * 读取资源文件
 */
suspend fun readResource(path: String): ByteArray {
    return readResource(path) { it.readBytes() }
}

/**
 * 读取资源文件
 */
suspend fun <R> readResource(path: String, block: (InputStream) -> R): R {
    return withContext(Dispatchers.IO) {
        (Thread.currentThread().getContextClassLoader().getResourceAsStream(path)
            ?: throw IllegalStateException("资源${path}不存在"))
            .use { block.invoke(it) }
    }
}

/**
 * 读取当前插件的资源文件
 */
suspend fun readPluginResource(path: String): ByteArray {
    return readPluginResource(path) { it.readBytes() }
}

/**
 * 读取当前插件的资源文件
 */
suspend fun <R> readPluginResource(path: String, block: (InputStream) -> R): R {
    val pluginPath = pluginPath()
    return readResource("$pluginPath/$path") { block.invoke(it) }
}