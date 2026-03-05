package com.blr19c.falowp.bot.system.utils

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.UndeclaredThrowableException

/**
 * 反射异常处理工具
 */
object ReflectionUtils {

    /**
     * 跳过反射异常
     */
    fun skipReflectionException(throwable: Throwable): Throwable {
        var current = throwable
        while (true) {
            current = when (current) {
                is UndeclaredThrowableException -> current.undeclaredThrowable ?: current
                is InvocationTargetException -> current.targetException ?: current
                is ReflectiveOperationException -> current.cause ?: current
                else -> return current
            }
        }
    }
}
