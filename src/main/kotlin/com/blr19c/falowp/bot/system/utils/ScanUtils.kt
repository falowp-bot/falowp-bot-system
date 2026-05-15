package com.blr19c.falowp.bot.system.utils

import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.systemConfigListProperty
import java.net.JarURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.streams.asSequence

/**
 * 扫描包
 */
object ScanUtils {

    const val CLASS_FILE_SUFFIX = ".class"

    private val callerStackWalker: StackWalker =
        StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)

    /**
     * 扫描指定包下的类
     */
    fun scanPackage(packageName: String): List<Class<*>> {
        val classLoader = Thread.currentThread().contextClassLoader
        val packagePath = convertClassNameToResourcePath(packageName)
        return classLoader.getResources(packagePath)
            .asSequence()
            .flatMap { scanDirectoryOrJar(it, packageName, packagePath, classLoader).asSequence() }
            .distinctBy { it.name }
            .toList()
    }

    /**
     * 获取调用方插件类
     */
    fun getCallerClass(packageNames: List<String> = systemConfigListProperty("pluginPackage")): KClass<*> {
        val caller = findPluginCallerClass(packageNames) ?: findCallerClass(packageNames)
        return caller?.kotlin
            ?: throw IllegalStateException("未找到调用方类,packageNames=$packageNames")
    }

    /**
     * 获取Lambda所属类
     */
    fun getLambdaCallerClass(lambda: Function<*>): KClass<*> {
        val className = lambda::class.qualifiedName
        val originalName = className?.substringBefore($$$"$$Lambda")
        return runCatching { Class.forName(originalName).kotlin }
            .getOrElse { throw IllegalStateException("未找到调用方类,className=$className") }
    }

    /**
     * 获取当前插件配置路径
     */
    fun configPath(): String {
        val callerClass = getCallerClass()
        val qualifiedPath = callerClass.qualifiedName?.let {
            val noClassNamePath = it.substringBeforeLast(".")
            noClassNamePath.substringAfterLast("plugins")
        }
        val packageName = qualifiedPath ?: callerClass.java.packageName.substringAfterLast("plugins")
        return "bot.plugin".plus(packageName).plus(".")
    }

    /**
     * 获取当前插件资源路径
     */
    fun pluginPath(): String {
        val callerClass = getCallerClass()
        val qualifiedPath = callerClass.qualifiedName?.let {
            val noClassNamePath = it.substringBeforeLast(".")
            noClassNamePath.substringAfterLast("plugins")
        }
        val packageName = qualifiedPath ?: callerClass.java.packageName.substringAfterLast("plugins")
        return convertClassNameToResourcePath("plugins".plus(packageName))
    }

    /**
     * 在调用栈中查找插件类
     */
    private fun findCallerClass(packageNames: List<String>): Class<*>? {
        return callerStackWalker.walk { frames ->
            frames.map { it.declaringClass }
                .filter { clazz -> packageNames.any { clazz.name.contains(it) } }
                .findFirst()
                .let { optional ->
                    if (optional.isPresent) optional.get() else null
                }
        }
    }

    /**
     * 在调用栈中查找声明了插件注解的插件类
     */
    private fun findPluginCallerClass(packageNames: List<String>): Class<*>? {
        return callerStackWalker.walk { frames ->
            frames.map { it.declaringClass }
                .filter { clazz -> packageNames.any { clazz.name.contains(it) } }
                .filter { clazz -> clazz.isAnnotationPresent(Plugin::class.java) }
                .findFirst()
                .let { optional ->
                    if (optional.isPresent) optional.get() else null
                }
        }
    }

    /**
     * 扫描目录或Jar资源
     */
    private fun scanDirectoryOrJar(
        url: URL,
        packageName: String,
        packagePath: String,
        classLoader: ClassLoader,
    ): List<Class<*>> {
        return when {
            ResourceUtils.isJarURL(url) -> scanJar(url, packagePath, classLoader)
            url.protocol.equals("file", ignoreCase = true) -> scanDirectory(url, packageName, classLoader)
            else -> emptyList()
        }
    }

    /**
     * 扫描文件目录中的类
     */
    private fun scanDirectory(url: URL, packageName: String, classLoader: ClassLoader): List<Class<*>> {
        val root = url.toPathOrNull() ?: return emptyList()
        if (!Files.isDirectory(root)) {
            return emptyList()
        }

        return Files.walk(root).use { pathStream ->
            pathStream
                .asSequence()
                .filter { Files.isRegularFile(it) }
                .filter { it.fileName.toString().endsWith(CLASS_FILE_SUFFIX) }
                .map { root.relativize(it).toString() }
                .map { it.replace('\\', '/').removeSuffix(CLASS_FILE_SUFFIX) }
                .map { "$packageName.${it.replace('/', '.')}" }
                .mapNotNull { loadClassOrNull(it, classLoader) }
                .toList()
        }
    }

    /**
     * 扫描Jar中的类
     */
    private fun scanJar(url: URL, packagePath: String, classLoader: ClassLoader): List<Class<*>> {
        val connection = runCatching { url.openConnection() }.getOrNull()
        if (connection !is JarURLConnection) {
            return emptyList()
        }

        connection.useCaches = false
        return connection.jarFile.use { jar ->
            jar.entries().asSequence()
                .filter { !it.isDirectory }
                .filter { it.name.endsWith(CLASS_FILE_SUFFIX) }
                .filter { it.name.startsWith("$packagePath/") }
                .map { it.name.removeSuffix(CLASS_FILE_SUFFIX).replace('/', '.') }
                .mapNotNull { loadClassOrNull(it, classLoader) }
                .toList()
        }
    }

    /**
     * 尝试加载类
     */
    private fun loadClassOrNull(className: String, classLoader: ClassLoader): Class<*>? {
        return try {
            Class.forName(className, false, classLoader)
        } catch (_: ClassNotFoundException) {
            null
        } catch (_: LinkageError) {
            null
        }
    }

    /**
     * URL转换为Path
     */
    private fun URL.toPathOrNull(): Path? {
        return runCatching { Path.of(this.toURI()) }.getOrNull()
    }

    /**
     * 类名转换为资源路径
     */
    private fun convertClassNameToResourcePath(className: String): String {
        return className.replace('.', '/')
    }
}
