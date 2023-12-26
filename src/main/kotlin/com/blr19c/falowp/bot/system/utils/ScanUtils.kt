package com.blr19c.falowp.bot.system.utils

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
        val className = Thread.currentThread().stackTrace
            .map { it.className }
            .first { className -> packageNames.any { className.contains(it) } }
        return Class.forName(className).kotlin
    }

    private fun scanDirectoryOrJar(url: URL, packageName: String): List<Class<*>> {
        if (!ResourceUtils.isJarURL(url)) {
            return scanDirectory(File(url.toURI()), packageName)
        }
        return JarFile(ResourceUtils.extractArchiveURL(url).file).use { jarFile ->
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
            Class.forName(className)
        } catch (e: Exception) {
            null
        }
    }
}