package com.blr19c.falowp.bot.system.utils

import com.blr19c.falowp.bot.system.expand.urlDecoder
import com.blr19c.falowp.bot.system.systemConfigListProperty
import com.blr19c.falowp.bot.system.utils.ClassUtils.convertClassNameToResourcePath
import com.blr19c.falowp.bot.system.utils.ClassUtils.convertResourcePathToClassName
import java.io.File
import java.net.URL
import java.util.jar.JarFile
import kotlin.reflect.KClass
import kotlin.streams.asSequence


/**
 * 扫描包
 */
object ScanUtils {

    fun scanPackage(packageName: String): List<Class<*>> {
        return Thread.currentThread()
            .contextClassLoader
            .getResources(convertClassNameToResourcePath(packageName))
            .asSequence()
            .map { scanDirectoryOrJar(it, packageName) }
            .flatMap { it.stream().asSequence() }
            .toList()
    }

    fun getCallerClass(packageNames: List<String> = systemConfigListProperty("pluginPackage")): KClass<*> {
        return getCallerClassOrNull(packageNames)!!
    }

    fun getCallerClassOrNull(packageNames: List<String> = systemConfigListProperty("pluginPackage")): KClass<*>? {
        val className = Thread.currentThread().stackTrace
            .map { it.className }
            .firstOrNull { className -> packageNames.any { className.contains(it) } }
        return className?.let { Class.forName(it).kotlin }
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
        return convertClassNameToResourcePath("plugins".plus(packageName))
    }

    private fun scanDirectoryOrJar(url: URL, packageName: String): List<Class<*>> {
        if (!ResourceUtils.isJarURL(url)) {
            return scanDirectory(File(ResourceUtils.extractArchiveURL(url).path.urlDecoder()), packageName)
        }
        return JarFile(File(ResourceUtils.extractArchiveURL(url).path.urlDecoder())).use { jarFile ->
            jarFile.entries()
                .asSequence()
                .filter { it.name.endsWith(ClassUtils.CLASS_FILE_SUFFIX) }
                .map { convertResourcePathToClassName(it.name) }
                .map { it.substring(0, it.indexOf(ClassUtils.CLASS_FILE_SUFFIX)) }
                .filter { it.startsWith(packageName) }
                .map { forName(it) }
                .filterNotNull()
                .toList()
        }
    }

    private fun scanDirectory(directory: File, packageName: String): List<Class<*>> {
        val files = directory.listFiles() ?: return listOf()
        return files.flatMap { file ->
            if (file.isDirectory) {
                scanDirectory(file, "$packageName.${file.name}")
            } else {
                listOfNotNull(forName("$packageName.${file.nameWithoutExtension}"))
            }
        }
    }

    private fun forName(className: String): Class<*>? {
        return try {
            Class.forName(className, false, this.javaClass.classLoader)
        } catch (_: Exception) {
            null
        }
    }
}