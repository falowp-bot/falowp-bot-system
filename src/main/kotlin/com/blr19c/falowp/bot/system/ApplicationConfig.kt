package com.blr19c.falowp.bot.system

import com.blr19c.falowp.bot.system.utils.ResourceUtils
import com.blr19c.falowp.bot.system.utils.ScanUtils.configPath
import com.blr19c.falowp.bot.system.utils.ScanUtils.pluginPath
import io.ktor.server.config.*
import io.ktor.server.config.yaml.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.yamlkt.Yaml
import net.mamoe.yamlkt.YamlMap
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

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
private fun loadYaml(inputStream: InputStream): ApplicationConfig {
    val content = inputStream.readBytes().toString(Charsets.UTF_8)
    val yamlElement = Yaml.decodeYamlFromString(content)
    val yaml = yamlElement as? YamlMap ?: throw IllegalArgumentException("配置应该是yaml字典")
    val constructor = YamlConfig::class.primaryConstructor!!
    constructor.isAccessible = true
    return constructor.call(yaml).apply { checkEnvironmentVariables() }
}

private val applicationConfig by lazy {
    val yamlConfigLoader = YamlConfigLoader()
    var config = yamlConfigLoader.load(null) ?: throw IllegalArgumentException("未找到有效的application.yaml配置")
    for (resource in Thread.currentThread().contextClassLoader.getResources("plugin-conf")) {
        val subConfig = ResourceUtils.resourceToInputStream(resource, ".yaml") { loadYaml(it) }
            .reduce { a1, a2 -> MergedApplicationConfig(a1, a2) }
        config = MergedApplicationConfig(config, subConfig)
    }
    config
}

private val configPropertyMap by lazy { ConcurrentHashMap<String, String>() }
private val configListPropertyMap by lazy { ConcurrentHashMap<String, List<String>>() }
private val configDefaultValue: (String) -> String = { throw IllegalArgumentException("未找到配置:$it") }
private val configDefaultListValue: (String) -> List<String> = { throw IllegalArgumentException("未找到配置:$it") }


internal class MergedApplicationConfig(
    private val first: ApplicationConfig,
    private val second: ApplicationConfig
) : ApplicationConfig {

    private val firstKeys by lazy { first.keys() }
    private val secondKeys by lazy { second.keys() }

    override fun property(path: String): ApplicationConfigValue {
        return propertyOrNull(path) ?: throw ApplicationConfigurationException("Property $path not found.")
    }

    override fun propertyOrNull(path: String): ApplicationConfigValue? {
        if (firstKeys.contains(path) && secondKeys.contains(path)) {
            return MergedApplicationConfigValue(first.property(path), second.property(path))
        }
        return when {
            firstKeys.contains(path) -> first.property(path)
            else -> second.propertyOrNull(path)
        }
    }

    override fun config(path: String): ApplicationConfig {
        if (firstKeys.none { it.startsWith("$path.") }) return second.config(path)
        if (secondKeys.none { it.startsWith("$path.") }) return first.config(path)
        return MergedApplicationConfig(first.config(path), second.config(path))
    }

    override fun configList(path: String): List<ApplicationConfig> {
        val firstList = if (firstKeys.contains(path)) first.configList(path) else emptyList()
        val secondList = if (secondKeys.contains(path)) second.configList(path) else emptyList()
        return firstList + secondList
    }

    override fun keys(): Set<String> = firstKeys + secondKeys

    override fun toMap(): Map<String, Any?> = second.toMap() + first.toMap()
}

internal class MergedApplicationConfigValue(
    private val first: ApplicationConfigValue,
    private val second: ApplicationConfigValue
) : ApplicationConfigValue {

    override fun getString(): String {
        return first.getString()
    }

    override fun getList(): List<String> {
        return (first.getList() + second.getList()).distinct()
    }
}

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