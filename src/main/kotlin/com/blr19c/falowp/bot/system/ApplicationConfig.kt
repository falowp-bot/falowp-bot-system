@file:Suppress("UNUSED")

package com.blr19c.falowp.bot.system

import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.json.foldPath
import com.blr19c.falowp.bot.system.json.safeString
import com.blr19c.falowp.bot.system.json.safeStringOrNull
import com.blr19c.falowp.bot.system.utils.ResourceUtils
import com.blr19c.falowp.bot.system.utils.ScanUtils.configPath
import com.blr19c.falowp.bot.system.utils.ScanUtils.pluginPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.NullNode
import tools.jackson.dataformat.yaml.YAMLFactory
import tools.jackson.dataformat.yaml.YAMLMapper
import tools.jackson.module.kotlin.convertValue
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

/**
 * 加载YAML
 */
private fun loadYaml(inputStream: InputStream): JacksonYAMLValue {
    val yamlMapper = YAMLMapper(YAMLFactory())
    return JacksonYAMLValue(yamlMapper.readTree(inputStream))
}

/**
 * 加载 application.yml
 */
private fun localApplicationYaml(): JacksonYAMLValue {
    val resource = Thread.currentThread().contextClassLoader.getResource("application.yaml")
    resource ?: throw IllegalArgumentException("未找到有效的application.yaml配置")
    return resource.openStream().use { loadYaml(it) }
}

private val applicationConfig by lazy {
    var applicationYaml = localApplicationYaml()
    for (resource in Thread.currentThread().contextClassLoader.getResources("plugin-conf")) {
        val subConfig = ResourceUtils.resourceToInputStream(resource, ".yaml") { loadYaml(it) }
            .reduce { a1, a2 -> a1.merge(a2) }
        applicationYaml = applicationYaml.merge(subConfig)
    }
    applicationYaml
}

private val configPropertyMap by lazy { ConcurrentHashMap<String, String>() }
private val configListPropertyMap by lazy { ConcurrentHashMap<String, List<String>>() }
private val configDefaultValue: (String) -> String = { throw IllegalArgumentException("未找到配置:$it") }
private val configDefaultListValue: (String) -> List<String> = { throw IllegalArgumentException("未找到配置:$it") }

open class JacksonYAMLValue(
    val jsonNode: JsonNode,
) {

    constructor() : this(NullNode.instance)

    open fun getConfig(path: String): JacksonYAMLValue {
        return JacksonYAMLValue(jsonNode.foldPath(path))
    }

    open fun getListConfig(path: String): List<JacksonYAMLValue> {
        val value = jsonNode.foldPath(path)
        return value.map { JacksonYAMLValue(it) }.toList()
    }

    open fun getStringOrNull(path: String): String? {
        return jsonNode.foldPath(path).safeStringOrNull()
    }

    open fun getListStringOrNull(path: String): List<String>? {
        val firstValue = jsonNode.foldPath(path)
        if (firstValue.isArray) {
            return firstValue.map { it.safeString() }.toList()
        }
        return null
    }

    open fun toMap(): Map<String, Any?> {
        return Json.objectMapper().convertValue(jsonNode)
    }

    fun merge(value: JacksonYAMLValue?): JacksonYAMLValue {
        return JacksonYAMLMergeValue(this, value ?: return this)
    }
}


internal class JacksonYAMLMergeValue(
    private val first: JacksonYAMLValue,
    private val second: JacksonYAMLValue,
) : JacksonYAMLValue() {

    override fun getConfig(path: String): JacksonYAMLValue {
        val firstValue = first.getConfig(path)
        val secondValue = second.getConfig(path)
        return firstValue.merge(secondValue)
    }

    override fun getListConfig(path: String): List<JacksonYAMLValue> {
        return (first.getListConfig(path) + second.getListConfig(path)).distinct()
    }

    override fun getStringOrNull(path: String): String? {
        val firstValue = first.getStringOrNull(path)
        val secondValue = second.getStringOrNull(path)
        return firstValue ?: secondValue
    }

    override fun getListStringOrNull(path: String): List<String>? {
        val firstValue = first.getListStringOrNull(path)
        val secondValue = second.getListStringOrNull(path)
        if (firstValue == null && secondValue == null) {
            return null
        }
        val list = mutableListOf<String>()
        firstValue?.let { list.addAll(it) }
        secondValue?.let { list.addAll(it) }
        return list.distinct()
    }

    override fun toMap(): Map<String, Any?> {
        return first.toMap() + second.toMap()
    }
}

/**
 * 读取application.conf配置文件
 */
fun configProperty(
    key: String,
    defaultValue: (String) -> String = configDefaultValue
): String {
    val finalKey = key.takeIf { !it.endsWith(".") } ?: key.dropLast(1)
    return configPropertyMap.computeIfAbsent(finalKey) {
        applicationConfig.getStringOrNull(finalKey) ?: defaultValue.invoke(finalKey)
    }
}


/**
 * 读取application.conf配置文件
 */
fun configListProperty(
    key: String,
    defaultValue: (String) -> List<String> = configDefaultListValue
): List<String> {
    val finalKey = key.takeIf { !it.endsWith(".") } ?: key.dropLast(1)
    return configListPropertyMap.computeIfAbsent(finalKey) {
        applicationConfig.getListStringOrNull(finalKey) ?: defaultValue.invoke(finalKey)
    }
}


/**
 * 读取application.conf配置文件
 */
fun configList(key: String): List<JacksonYAMLValue> {
    val finalKey = key.takeIf { !it.endsWith(".") } ?: key.dropLast(1)
    return applicationConfig.getListConfig(finalKey)
}

/**
 * 读取application.conf配置文件
 */
fun config(key: String): JacksonYAMLValue {
    val finalKey = key.takeIf { !it.endsWith(".") } ?: key.dropLast(1)
    return applicationConfig.getConfig(finalKey)
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
 * 读取application.conf配置文件-添加系统的前缀
 */
fun systemConfig(key: String): JacksonYAMLValue {
    return config("bot.system.".plus(key))
}

/**
 * 读取application.conf配置文件-添加系统的前缀
 */
fun systemConfigList(key: String): List<JacksonYAMLValue> {
    return configList("bot.system.".plus(key))
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
 * 读取application.conf配置文件-添加适配器前缀
 */
fun adapterConfigList(key: String): List<JacksonYAMLValue> {
    return configList("bot.adapter.".plus(key))
}

/**
 * 读取application.conf配置文件-添加适配器前缀
 */
fun adapterConfig(key: String): JacksonYAMLValue {
    return config("bot.adapter.".plus(key))
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
 * 读取application.conf配置文件-添加当前插件的前缀
 */
fun pluginConfig(key: String): JacksonYAMLValue {
    return config(configPath().plus(key))
}

/**
 * 读取application.conf配置文件-添加当前插件的前缀
 */
fun pluginConfigList(key: String): List<JacksonYAMLValue> {
    return configList(configPath().plus(key))
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
        (Thread.currentThread().contextClassLoader.getResourceAsStream(path)
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