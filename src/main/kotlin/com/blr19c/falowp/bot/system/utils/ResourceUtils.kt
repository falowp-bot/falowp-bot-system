package com.blr19c.falowp.bot.system.utils

import java.io.InputStream
import java.net.JarURLConnection
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

/**
 * 资源读取工具
 */
object ResourceUtils {

    private const val JAR_URL_SEPARATOR = "!/"
    private val JAR_PROTOCOLS = setOf("jar", "war", "zip", "vfszip", "wsjar")

    fun isJarURL(url: URL): Boolean {
        return url.protocol.lowercase() in JAR_PROTOCOLS || url.toExternalForm().contains(JAR_URL_SEPARATOR)
    }

    fun extractArchiveURL(jarUrl: URL): URL {
        val connection = runCatching { jarUrl.openConnection() }.getOrNull()
        if (connection is JarURLConnection) {
            connection.useCaches = false
            return connection.jarFileURL
        }

        val spec = jarUrl.toExternalForm()
        val separator = spec.indexOf(JAR_URL_SEPARATOR)
        if (separator < 0) {
            return jarUrl
        }

        val archiveSpec = spec.substring(0, separator)
            .removePrefix("jar:")
            .removePrefix("war:")
        return URI.create(archiveSpec).toURL()
    }

    fun <R> resourceToInputStream(resource: URL, suffix: String, consumer: (InputStream) -> R): List<R> {
        if (isJarURL(resource)) {
            return readJarResource(resource, suffix, consumer)
        }

        if (resource.protocol.equals("file", ignoreCase = true)) {
            return readFileResource(resource, suffix, consumer)
        }

        val fileName = resource.path.substringAfterLast('/')
        return if (fileName.endsWith(suffix)) {
            listOf(resource.openStream().use(consumer))
        } else {
            emptyList()
        }
    }

    private fun <R> readJarResource(resource: URL, suffix: String, consumer: (InputStream) -> R): List<R> {
        val connection = runCatching { resource.openConnection() }.getOrNull()
        if (connection is JarURLConnection) {
            connection.useCaches = false
            val entryPrefix = connection.entryName?.trim('/')
            val normalizedPrefix = entryPrefix?.takeIf { it.isNotEmpty() }?.plus('/') ?: ""
            return connection.jarFile.use { jar ->
                jar.entries().asSequence()
                    .filter { !it.isDirectory }
                    .filter { it.name.startsWith(normalizedPrefix) }
                    .filter { it.name.endsWith(suffix) }
                    .map { entry -> jar.getInputStream(entry).use(consumer) }
                    .toList()
            }
        }

        return emptyList()
    }

    private fun <R> readFileResource(resource: URL, suffix: String, consumer: (InputStream) -> R): List<R> {
        val path = resource.toPathOrNull() ?: return emptyList()

        if (Files.isDirectory(path)) {
            Files.list(path).use { stream ->
                return stream
                    .filter(Files::isRegularFile)
                    .filter { it.fileName.toString().endsWith(suffix) }
                    .map { file -> Files.newInputStream(file).use(consumer) }
                    .toList()
            }
        }

        return if (path.fileName.toString().endsWith(suffix)) {
            listOf(Files.newInputStream(path).use(consumer))
        } else {
            emptyList()
        }
    }

    private fun URL.toPathOrNull(): Path? {
        return runCatching { Path.of(this.toURI()) }.getOrNull()
    }
}
