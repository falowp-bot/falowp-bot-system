package com.blr19c.falowp.bot.system.utils

import java.io.File
import java.io.FileNotFoundException
import java.net.*
import java.util.*

/**
 * util来源于spring
 */
@Suppress("MemberVisibilityCanBePrivate", "UNUSED", "SpellCheckingInspection")
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
     * Return whether the given resource location is a URL:
     * either a special "classpath" pseudo URL or a standard URL.
     *
     * @param resourceLocation the location String to check
     * @return whether the location qualifies as a URL
     * @see .CLASSPATH_URL_PREFIX
     *
     * @see java.net.URL
     */
    fun isUrl(resourceLocation: String?): Boolean {
        if (resourceLocation == null) {
            return false
        }
        return if (resourceLocation.startsWith(CLASSPATH_URL_PREFIX)) {
            true
        } else try {
            URI.create(resourceLocation)
            true
        } catch (ex: MalformedURLException) {
            false
        }
    }

    /**
     * Resolve the given resource location to a `java.net.URL`.
     *
     * Does not check whether the URL actually exists; simply returns
     * the URL that the given location would correspond to.
     *
     * @param resourceLocation the resource location to resolve: either a
     * "classpath:" pseudo URL, a "file:" URL, or a plain file path
     * @return a corresponding URL object
     * @throws FileNotFoundException if the resource cannot be resolved to a URL
     */
    @Throws(FileNotFoundException::class)
    fun getURL(resourceLocation: String): URL {
        if (resourceLocation.startsWith(CLASSPATH_URL_PREFIX)) {
            val path = resourceLocation.substring(CLASSPATH_URL_PREFIX.length)
            val cl: ClassLoader? = ClassUtils.defaultClassLoader
            val url = if (cl != null) cl.getResource(path) else ClassLoader.getSystemResource(path)
            if (url === null) {
                val description = "class path resource [$path]"
                throw FileNotFoundException(
                    description +
                            " cannot be resolved to URL because it does not exist"
                )
            }
            return url
        }
        return try {
            // try URL
            URI.create(resourceLocation).toURL()
        } catch (ex: MalformedURLException) {
            // no URL -> treat as file path
            try {
                File(resourceLocation).toURI().toURL()
            } catch (ex2: MalformedURLException) {
                throw FileNotFoundException(
                    "Resource location [" + resourceLocation +
                            "] is neither a URL not a well-formed file path"
                )
            }
        }
    }

    /**
     * Resolve the given resource location to a `java.io.File`,
     * i.e. to a file in the file system.
     *
     * Does not check whether the file actually exists; simply returns
     * the File that the given location would correspond to.
     *
     * @param resourceLocation the resource location to resolve: either a
     * "classpath:" pseudo URL, a "file:" URL, or a plain file path
     * @return a corresponding File object
     * @throws FileNotFoundException if the resource cannot be resolved to
     * a file in the file system
     */
    @Throws(FileNotFoundException::class)
    fun getFile(resourceLocation: String): File {
        if (resourceLocation.startsWith(CLASSPATH_URL_PREFIX)) {
            val path = resourceLocation.substring(CLASSPATH_URL_PREFIX.length)
            val description = "class path resource [$path]"
            val cl: ClassLoader? = ClassUtils.defaultClassLoader
            val url = (if (cl != null) cl.getResource(path) else ClassLoader.getSystemResource(path))
                ?: throw FileNotFoundException(
                    description +
                            " cannot be resolved to absolute file path because it does not exist"
                )
            return getFile(url, description)
        }
        return try {
            // try URL
            getFile(URI.create(resourceLocation))
        } catch (ex: MalformedURLException) {
            // no URL -> treat as file path
            File(resourceLocation)
        }
    }

    /**
     * Resolve the given resource URL to a `java.io.File`,
     * i.e. to a file in the file system.
     *
     * @param resourceUrl the resource URL to resolve
     * @return a corresponding File object
     * @throws FileNotFoundException if the URL cannot be resolved to
     * a file in the file system
     */
    @Throws(FileNotFoundException::class)
    fun getFile(resourceUrl: URL): File {
        return getFile(resourceUrl, "URL")
    }

    /**
     * Resolve the given resource URL to a `java.io.File`,
     * i.e. to a file in the file system.
     *
     * @param resourceUrl the resource URL to resolve
     * @param description a description of the original resource that
     * the URL was created for (for example, a class path location)
     * @return a corresponding File object
     * @throws FileNotFoundException if the URL cannot be resolved to
     * a file in the file system
     */
    @Throws(FileNotFoundException::class)
    fun getFile(resourceUrl: URL, description: String): File {
        if (URL_PROTOCOL_FILE != resourceUrl.protocol) {
            throw FileNotFoundException(
                description + " cannot be resolved to absolute file path " +
                        "because it does not reside in the file system: " + resourceUrl
            )
        }
        return try {
            File(toURI(resourceUrl).getSchemeSpecificPart())
        } catch (ex: URISyntaxException) {
            // Fallback for URLs that are not valid URIs (should hardly ever happen).
            File(resourceUrl.file)
        }
    }

    /**
     * Resolve the given resource URI to a `java.io.File`,
     * i.e. to a file in the file system.
     *
     * @param resourceUri the resource URI to resolve
     * @return a corresponding File object
     * @throws FileNotFoundException if the URL cannot be resolved to
     * a file in the file system
     * @since 2.5
     */
    @Throws(FileNotFoundException::class)
    fun getFile(resourceUri: URI): File {
        return getFile(resourceUri, "URI")
    }

    /**
     * Resolve the given resource URI to a `java.io.File`,
     * i.e. to a file in the file system.
     *
     * @param resourceUri the resource URI to resolve
     * @param description a description of the original resource that
     * the URI was created for (for example, a class path location)
     * @return a corresponding File object
     * @throws FileNotFoundException if the URL cannot be resolved to
     * a file in the file system
     * @since 2.5
     */
    @Throws(FileNotFoundException::class)
    fun getFile(resourceUri: URI, description: String): File {
        if (URL_PROTOCOL_FILE != resourceUri.scheme) {
            throw FileNotFoundException(
                description + " cannot be resolved to absolute file path " +
                        "because it does not reside in the file system: " + resourceUri
            )
        }
        return File(resourceUri.getSchemeSpecificPart())
    }

    /**
     * Determine whether the given URL points to a resource in the file system,
     * i.e. has protocol "file", "vfsfile" or "vfs".
     *
     * @param url the URL to check
     * @return whether the URL has been identified as a file system URL
     */
    fun isFileURL(url: URL): Boolean {
        val protocol = url.protocol
        return URL_PROTOCOL_FILE == protocol || URL_PROTOCOL_VFSFILE == protocol || URL_PROTOCOL_VFS == protocol
    }

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
     * Determine whether the given URL points to a jar file itself,
     * that is, has protocol "file" and ends with the ".jar" extension.
     *
     * @param url the URL to check
     * @return whether the URL has been identified as a JAR file URL
     * @since 4.1
     */
    fun isJarFileURL(url: URL): Boolean {
        return URL_PROTOCOL_FILE == url.protocol &&
                url.path.lowercase(Locale.getDefault()).endsWith(JAR_FILE_EXTENSION)
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
                // Probably no protocol in original jar URL, like "jar:C:/mypath/myjar.jar".
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
            // Tomcat's "war:file:...mywar.war*/WEB-INF/lib/myjar.jar!/myentry.txt"
            val warFile = urlFile.substring(0, endIndex)
            if (URL_PROTOCOL_WAR == jarUrl.protocol) {
                return URI.create(warFile).toURL()
            }
            val startIndex = warFile.indexOf(WAR_URL_PREFIX)
            if (startIndex != -1) {
                return URI.create(warFile.substring(startIndex + WAR_URL_PREFIX.length)).toURL()
            }
        }

        // Regular "jar:file:...myjar.jar!/myentry.txt"
        return extractJarFileURL(jarUrl)
    }

    /**
     * Create a URI instance for the given URL,
     * replacing spaces with "%20" URI encoding first.
     *
     * @param url the URL to convert into a URI instance
     * @return the URI instance
     * @throws URISyntaxException if the URL wasn't a valid URI
     * @see java.net.URL.toURI
     */
    @Throws(URISyntaxException::class)
    fun toURI(url: URL): URI {
        return toURI(url.toString())
    }

    /**
     * Create a URI instance for the given location String,
     * replacing spaces with "%20" URI encoding first.
     *
     * @param location the location String to convert into a URI instance
     * @return the URI instance
     * @throws URISyntaxException if the location wasn't a valid URI
     */
    @Throws(URISyntaxException::class)
    fun toURI(location: String): URI {
        return URI(location.replace(" ", "%20"))
    }

    /**
     * Set the [&quot;useCaches&quot;][URLConnection.setUseCaches] flag on the
     * given connection, preferring `false` but leaving the
     * flag at `true` for JNLP based resources.
     *
     * @param con the URLConnection to set the flag on
     */
    fun useCachesIfNecessary(con: URLConnection) {
        con.setUseCaches(con.javaClass.getSimpleName().startsWith("JNLP"))
    }
}
