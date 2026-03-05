package com.blr19c.falowp.bot.system.utils

/**
 * class
 */
object ClassUtils {

    const val CLASS_FILE_SUFFIX = ".class"

    fun convertResourcePathToClassName(resourcePath: String): String {
        return resourcePath.replace('/', '.')
    }

    fun convertClassNameToResourcePath(className: String): String {
        return className.replace('.', '/')
    }
}
