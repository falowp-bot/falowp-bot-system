package com.blr19c.falowp.bot.system.utils

import java.io.File
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.util.jar.JarFile

/**
 * util来源于spring
 */
@Suppress("UNUSED", "SpellCheckingInspection")
object ResourceUtils {
    /**
     * Pseudo URL prefix for loading from the class path: "classpath:".
     */
    const val CLASSPATH_URL_PREFIX = "classpath:"

    /**
     * Pseudo URL prefix for loading from the class path: "classpath*:".
     */
    const val CLASSPATH_ALL_URL_PREFIX = "classpath*:"

    /**
     * URL prefix for loading from the file system: "file:".
     */
    const val FILE_URL_PREFIX = "file:"

    /**
     * URL prefix for loading from a jar file: "jar:".
     */
    const val JAR_URL_PREFIX = "jar:"

    /**
     * URL prefix for loading from a war file on Tomcat: "war:".
     */
    const val WAR_URL_PREFIX = "war:"

    /**
     * URL protocol for a file in the file system: "file".
     */
    const val URL_PROTOCOL_FILE = "file"

    /**
     * URL protocol for an entry from a jar file: "jar".
     */
    const val URL_PROTOCOL_JAR = "jar"

    /**
     * URL protocol for an entry from a war file: "war".
     */
    const val URL_PROTOCOL_WAR = "war"

    /**
     * URL protocol for an entry from a zip file: "zip".
     */
    const val URL_PROTOCOL_ZIP = "zip"

    /**
     * URL protocol for an entry from a WebSphere jar file: "wsjar".
     */
    const val URL_PROTOCOL_WSJAR = "wsjar"

    /**
     * URL protocol for an entry from a JBoss jar file: "vfszip".
     */
    const val URL_PROTOCOL_VFSZIP = "vfszip"

    /**
     * URL protocol for a JBoss file system resource: "vfsfile".
     */
    const val URL_PROTOCOL_VFSFILE = "vfsfile"

    /**
     * URL protocol for a general JBoss VFS resource: "vfs".
     */
    const val URL_PROTOCOL_VFS = "vfs"

    /**
     * File extension for a regular jar file: ".jar".
     */
    const val JAR_FILE_EXTENSION = ".jar"

    /**
     * Separator between JAR URL and file path within the JAR: "!/".
     */
    const val JAR_URL_SEPARATOR = "!/"

    /**
     * Special separator between WAR URL and jar part on Tomcat.
     */
    const val WAR_URL_SEPARATOR = "*/"


    /**
     * Determine whether the given URL points to a resource in a jar file.
     * i.e. has protocol "jar", "war, ""zip", "vfszip" or "wsjar".
     *
     * @param url the URL to check
     * @return whether the URL has been identified as a JAR URL
     */
    fun isJarURL(url: URL): Boolean {
        val protocol = url.protocol
        return URL_PROTOCOL_JAR == protocol || URL_PROTOCOL_WAR == protocol || URL_PROTOCOL_ZIP == protocol || URL_PROTOCOL_VFSZIP == protocol || URL_PROTOCOL_WSJAR == protocol
    }


    /**
     * Extract the URL for the actual jar file from the given URL
     * (which may point to a resource in a jar file or to a jar file itself).
     *
     * @param jarUrl the original URL
     * @return the URL for the actual jar file
     * @throws MalformedURLException if no valid jar file URL could be extracted
     */
    @Throws(MalformedURLException::class)
    fun extractJarFileURL(jarUrl: URL): URL {
        val urlFile = jarUrl.file
        val separatorIndex = urlFile.indexOf(JAR_URL_SEPARATOR)
        return if (separatorIndex != -1) {
            var jarFile = urlFile.substring(0, separatorIndex)
            try {
                URI.create(jarFile).toURL()
            } catch (ex: MalformedURLException) {
                // Probably no protocol in original jar URL, like "jar:C:/mypath/my-jar.jar".
                // This usually indicates that the jar file resides in the file system.
                if (!jarFile.startsWith("/")) {
                    jarFile = "/$jarFile"
                }
                URI.create(FILE_URL_PREFIX + jarFile).toURL()
            }
        } else {
            jarUrl
        }
    }

    /**
     * Extract the URL for the outermost archive from the given jar/war URL
     * (which may point to a resource in a jar file or to a jar file itself).
     *
     * In the case of a jar file nested within a war file, this will return
     * a URL to the war file since that is the one resolvable in the file system.
     *
     * @param jarUrl the original URL
     * @return the URL for the actual jar file
     * @throws MalformedURLException if no valid jar file URL could be extracted
     * @see .extractJarFileURL
     * @since 4.1.8
     */
    @Throws(MalformedURLException::class)
    fun extractArchiveURL(jarUrl: URL): URL {
        val urlFile = jarUrl.file
        val endIndex = urlFile.indexOf(WAR_URL_SEPARATOR)
        if (endIndex != -1) {
            // Tomcat's "war:file:...my-war.war*/WEB-INF/lib/my-jar.jar!/my-entry.txt"
            val warFile = urlFile.substring(0, endIndex)
            if (URL_PROTOCOL_WAR == jarUrl.protocol) {
                return URI.create(warFile).toURL()
            }
            val startIndex = warFile.indexOf(WAR_URL_PREFIX)
            if (startIndex != -1) {
                return URI.create(warFile.substring(startIndex + WAR_URL_PREFIX.length)).toURL()
            }
        }

        // Regular "jar:file:...my-jar.jar!/my-entry.txt"
        return extractJarFileURL(jarUrl)
    }

    /**
     *
     * Get the file jar or file list of the specified url
     *
     * @param resource the original URL
     * @return the URL for the actual files InputStream
     */
    fun <R> resourceToInputStream(resource: URL, suffix: String, consumer: (InputStream) -> R): List<R> {
        if (isJarURL(resource)) {
            val jarPath = resource.path.substringBefore(JAR_URL_SEPARATOR)
                .replaceFirst(FILE_URL_PREFIX, "")
                .replaceFirst(JAR_URL_PREFIX, "")
            val configPath = resource.path.substringAfter(JAR_URL_SEPARATOR) + "/"
            return JarFile(jarPath).use { jar ->
                jar.entries()
                    .asSequence()
                    .filter { it.name.startsWith(configPath) && !it.isDirectory }
                    .filter { it.name.endsWith(suffix) }
                    .map { jar.getInputStream(it) }
                    .map { it.use { inp -> consumer.invoke(inp) } }
                    .toList()
            }
        }
        val file = File(resource.path)
        if (file.isDirectory) {
            return file.listFiles()
                ?.filter { it.name.endsWith(suffix) }
                ?.map { it.inputStream() }
                ?.map { it.use { inp -> consumer.invoke(inp) } }
                ?: emptyList()
        }
        if (file.name.endsWith(suffix)) {
            return listOf(file.inputStream().use { consumer.invoke(it) })
        }
        return emptyList()
    }
}
