package com.blr19c.falowp.bot.system.utils

import java.lang.reflect.UndeclaredThrowableException

/**
 * 反射操作工具
 */
object ReflectionUtils {

    /**
     * 跳过反射异常
     */
    fun <T : Throwable> skipReflectionException(throwable: T): Throwable {
        var t: Throwable = throwable
        while (t is UndeclaredThrowableException) t = t.undeclaredThrowable
        while (t is ReflectiveOperationException) t = t.cause!!
        return if (t is UndeclaredThrowableException) skipReflectionException(t) else t
    }
}
