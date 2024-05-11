package com.blr19c.falowp.bot.system

import com.blr19c.falowp.bot.system.utils.ResourceUtils
import com.blr19c.falowp.bot.system.utils.ScanUtils.configPath
import com.blr19c.falowp.bot.system.utils.ScanUtils.pluginPath
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigList
import com.typesafe.config.ConfigValue
import io.ktor.server.config.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile

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

/**
 * 合并Bot的配置文件
 */
private fun mergeBotConfigs(config1: Config, config2: Config): Config {
    val mergedMap: MutableMap<String, Any> = config1.root().unwrapped()
    for ((key, value) in config2.entrySet()) {
        if (value is ConfigList && key.startsWith("bot")) {
            var commonKey: Any? = mergedMap
            val keyItems = key.split(".").toMutableList()
            val lastKeyItem = keyItems.removeLast()
            for (keyItem in keyItems) {
                try {
                    commonKey = (commonKey as Map<*, *>)[keyItem]!!
                } catch (e: Exception) {
                    @Suppress("UNCHECKED_CAST")
                    (commonKey as MutableMap<String, Any>)[keyItem] = hashMapOf<Any, Any>()
                    commonKey = commonKey[keyItem]!!
                }
            }
            if (commonKey != null) {
                val commonValue = (commonKey as Map<*, *>)[lastKeyItem]
                if (commonValue is List<*>) {
                    @Suppress("UNCHECKED_CAST")
                    commonValue as MutableList<ConfigValue>
                    value.forEach { if (!commonValue.contains(it.unwrapped())) commonValue.addLast(it) }
                }
                if (commonValue == null) {
                    @Suppress("UNCHECKED_CAST")
                    (commonKey as MutableMap<String, Any>)[lastKeyItem] = value
                }
            }
        }
    }
    return ConfigFactory.parseMap(mergedMap)
}

private val applicationConfig by lazy {
    var config = ConfigFactory.load()
    for (resource in Thread.currentThread().contextClassLoader.getResources("plugin-conf")) {
        if (ResourceUtils.isJarURL(resource)) {
            val jarPath = resource.path.substringBefore(ResourceUtils.JAR_URL_SEPARATOR)
                .replaceFirst(ResourceUtils.FILE_URL_PREFIX, "")
                .replaceFirst(ResourceUtils.JAR_URL_PREFIX, "")
            val configPath = resource.path.substringAfter(ResourceUtils.JAR_URL_SEPARATOR) + "/"
            JarFile(jarPath).use { jar ->
                jar.entries()
                    .asSequence()
                    .filter { it.name.startsWith(configPath) && !it.isDirectory }
                    .forEach {
                        config = mergeBotConfigs(config, ConfigFactory.parseReader(jar.getInputStream(it).reader()))
                    }
            }
        } else {
            for (file in File(resource.path).listFiles() ?: emptyArray()) {
                config = mergeBotConfigs(config, ConfigFactory.parseFile(file))
            }
        }
    }
    HoconApplicationConfig(config)
}

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
 * 读取application.conf配置文件-添加适配器前缀
 */
fun adapterConfigProperty(
    key: String,
    defaultValue: (String) -> String = configDefaultValue
): String {
    return configProperty("bot.adapter.".plus(key), defaultValue)
}

/**
 * 读取application.conf配置文件-添加适配器前缀
 */
fun adapterConfigListProperty(
    key: String,
    defaultValue: (String) -> List<String> = configDefaultListValue
): List<String> {
    return configListProperty("bot.adapter.".plus(key), defaultValue)
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