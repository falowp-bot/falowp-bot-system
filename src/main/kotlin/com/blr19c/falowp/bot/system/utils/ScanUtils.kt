package com.blr19c.falowp.bot.system.utils

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

    private val callerStackWalker: StackWalker =
        StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)

    fun scanPackage(packageName: String): List<Class<*>> {
        val classLoader = Thread.currentThread().contextClassLoader
        val packagePath = ClassUtils.convertClassNameToResourcePath(packageName)
        return classLoader.getResources(packagePath)
            .asSequence()
            .flatMap { scanDirectoryOrJar(it, packageName, packagePath, classLoader).asSequence() }
            .distinctBy { it.name }
            .toList()
    }

    fun getCallerClass(packageNames: List<String> = systemConfigListProperty("pluginPackage")): KClass<*> {
        val caller = findCallerClass(packageNames)
        return caller?.kotlin
            ?: throw IllegalStateException("未找到调用方类，packageNames=$packageNames")
    }

    fun configPath(): String {
        val callerClass = getCallerClass()
        val qualifiedPath = callerClass.qualifiedName?.let {
            val noClassNamePath = it.substringBeforeLast(".")
            noClassNamePath.substringAfterLast("plugins")
        }
        val packageName = qualifiedPath ?: callerClass.java.packageName.substringAfterLast("plugins")
        return "bot.plugin".plus(packageName).plus(".")
    }

    fun pluginPath(): String {
        val callerClass = getCallerClass()
        val qualifiedPath = callerClass.qualifiedName?.let {
            val noClassNamePath = it.substringBeforeLast(".")
            noClassNamePath.substringAfterLast("plugins")
        }
        val packageName = qualifiedPath ?: callerClass.java.packageName.substringAfterLast("plugins")
        return ClassUtils.convertClassNameToResourcePath("plugins".plus(packageName))
    }

    private fun findCallerClass(packageNames: List<String>): Class<*>? {
        return callerStackWalker.walk { frames ->
            frames.map { it.declaringClass }
                .filter { clazz -> packageNames.any { clazz.name.contains(it) } }
                .findFirst()
                .orElse(null)
        }
    }

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

    private fun scanDirectory(url: URL, packageName: String, classLoader: ClassLoader): List<Class<*>> {
        val root = url.toPathOrNull() ?: return emptyList()
        if (!Files.isDirectory(root)) {
            return emptyList()
        }

        return Files.walk(root).use { pathStream ->
            pathStream
                .asSequence()
                .filter { Files.isRegularFile(it) }
                .filter { it.fileName.toString().endsWith(ClassUtils.CLASS_FILE_SUFFIX) }
                .map { root.relativize(it).toString() }
                .map { it.replace('\\', '/').removeSuffix(ClassUtils.CLASS_FILE_SUFFIX) }
                .map { "$packageName.${it.replace('/', '.')}" }
                .mapNotNull { loadClassOrNull(it, classLoader) }
                .toList()
        }
    }

    private fun scanJar(url: URL, packagePath: String, classLoader: ClassLoader): List<Class<*>> {
        val connection = runCatching { url.openConnection() }.getOrNull()
        if (connection !is JarURLConnection) {
            return emptyList()
        }

        connection.useCaches = false
        return connection.jarFile.use { jar ->
            jar.entries().asSequence()
                .filter { !it.isDirectory }
                .filter { it.name.endsWith(ClassUtils.CLASS_FILE_SUFFIX) }
                .filter { it.name.startsWith("$packagePath/") }
                .map { it.name.removeSuffix(ClassUtils.CLASS_FILE_SUFFIX).replace('/', '.') }
                .mapNotNull { loadClassOrNull(it, classLoader) }
                .toList()
        }
    }

    private fun loadClassOrNull(className: String, classLoader: ClassLoader): Class<*>? {
        return try {
            Class.forName(className, false, classLoader)
        } catch (_: ClassNotFoundException) {
            null
        } catch (_: LinkageError) {
            null
        }
    }

    private fun URL.toPathOrNull(): Path? {
        return runCatching { Path.of(this.toURI()) }.getOrNull()
    }
}
