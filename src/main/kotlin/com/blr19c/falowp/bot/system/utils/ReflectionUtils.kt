package com.blr19c.falowp.bot.system.utils

import ch.qos.logback.core.CoreConstants.EMPTY_CLASS_ARRAY
import com.blr19c.falowp.bot.system.utils.ReflectionUtils.ReflectionFilter
import java.beans.Introspector
import java.beans.PropertyDescriptor
import java.lang.reflect.*

/**
 * 反射操作工具
 *
 * @author blr
 */
@Suppress("MemberVisibilityCanBePrivate", "UNUSED", "DEPRECATION")
object ReflectionUtils {
    /**
     * 匹配所有非桥接非合成方法
     */
    val USER_DECLARED_METHODS = ReflectionFilter { method: Method -> !method.isBridge && !method.isSynthetic }

    /**
     * 匹配所有非静态非不可变的字段
     */
    val COPYABLE_FIELDS =
        ReflectionFilter { field: Field -> !(Modifier.isStatic(field.modifiers) || Modifier.isFinal(field.modifiers)) }

    /**
     * CGLIB重命名的前缀
     *
     * @see .isCglibRenamedMethod
     */
    private const val CGLIB_RENAMED_METHOD_PREFIX = "CGLIB$"

    /**
     * 跳过反射异常
     */
    fun <T : Throwable> skipReflectionException(throwable: T): Throwable {
        var t: Throwable = throwable
        while (t is UndeclaredThrowableException) t = t.undeclaredThrowable
        while (t is ReflectiveOperationException) t = t.cause!!
        return if (t is UndeclaredThrowableException) skipReflectionException(t) else t
    }

    /**
     * 处理反射异常
     */
    fun handleReflectionException(ex: Exception) {
        check(ex !is NoSuchMethodException) { "Method not found: " + ex.message }
        check(ex !is IllegalAccessException) { "Could not access method or field: " + ex.message }
        if (ex is InvocationTargetException) {
            handleInvocationTargetException(ex)
        }
        if (ex is RuntimeException) {
            throw ex
        }
        throw UndeclaredThrowableException(ex)
    }

    /**
     * 处理调用异常
     */
    fun handleInvocationTargetException(ex: InvocationTargetException) {
        rethrowRuntimeException(ex.targetException)
    }

    /**
     * 将异常抛出为运行时异常
     */
    fun rethrowRuntimeException(ex: Throwable?) {
        if (ex is RuntimeException) {
            throw (ex as RuntimeException?)!!
        }
        if (ex is Error) {
            throw (ex as Error?)!!
        }
        throw UndeclaredThrowableException(ex)
    }

    /**
     * 根据类型和参数获取构造函数
     */
    @Throws(NoSuchMethodException::class)
    fun <T> accessibleConstructor(
        clazz: Class<T>,
        vararg parameterTypes: Class<*>?
    ): Constructor<T> {
        val constructor = clazz.getDeclaredConstructor(*parameterTypes)
        makeAccessible(constructor)
        return constructor
    }

    /**
     * 开放构造函数权限
     */
    fun makeAccessible(constructor: Constructor<*>) {
        if ((!Modifier.isPublic(constructor.modifiers) ||
                    !Modifier.isPublic(constructor.declaringClass.modifiers)) && !constructor.isAccessible
        ) {
            constructor.setAccessible(true)
        }
    }

    /**
     * 查询无参数方法
     */
    fun findMethod(clazz: Class<*>?, name: String): Method? {
        return findMethod(clazz, name, *EMPTY_CLASS_ARRAY)
    }

    /**
     * 根据指定类型名称参数查询方法
     */
    fun findMethod(clazz: Class<*>?, name: String, vararg paramTypes: Class<*>): Method? {
        notNull(clazz, "Class must not be null")
        notNull(name, "Method name must not be null")
        var searchType = clazz
        while (searchType != null) {
            val methods = if (searchType.isInterface) searchType.getMethods() else getDeclaredMethods(searchType, false)
            for (method in methods) {
                if (name == method.name && (hasSameParams(method, arrayOf(*paramTypes)))) {
                    return method
                }
            }
            searchType = searchType.superclass
        }
        return null
    }

    private fun hasSameParams(method: Method, paramTypes: Array<Class<*>>): Boolean {
        return paramTypes.size == method.parameterCount && paramTypes.contentEquals(method.parameterTypes)
    }

    /**
     * 执行无参数方法
     */
    fun invokeMethod(method: Method, target: Any?): Any {
        return invokeMethod(method, target, emptyArray<Any>())
    }

    /**
     * 指定参数执行方法
     */
    fun invokeMethod(method: Method, target: Any?, vararg args: Any?): Any {
        try {
            return method.invoke(target, *args)
        } catch (ex: Exception) {
            handleReflectionException(ex)
        }
        throw IllegalStateException("Should never get here")
    }

    /**
     * exceptionType是否已经在method的定义中声明了
     */
    fun declaresException(method: Method, exceptionType: Class<*>): Boolean {
        notNull(method, "Method must not be null")
        val declaredExceptions = method.exceptionTypes
        for (declaredException in declaredExceptions) {
            if (declaredException.isAssignableFrom(exceptionType)) {
                return true
            }
        }
        return false
    }

    /**
     * 遍历当前类所有方法并使用ReflectionCallback<Method>执行
    </Method> */
    fun doWithLocalMethods(clazz: Class<*>, callback: ReflectionCallback<Method?>) {
        val methods = getDeclaredMethods(clazz, false)
        for (method in methods) {
            try {
                callback.doWith(method)
            } catch (ex: Exception) {
                handleReflectionException(ex)
            }
        }
    }

    /**
     * 遍历所有方法并使用ReflectionCallback<Method>执行
    </Method> */
    fun doWithMethods(clazz: Class<*>, callback: ReflectionCallback<Method?>) {
        doWithMethods(clazz, callback, null)
    }

    /**
     * 遍历所有方法通过ReflectionFilter<Method>过滤的使用ReflectionCallback<Method>执行
    </Method></Method> */
    fun doWithMethods(clazz: Class<*>, mc: ReflectionCallback<Method?>, mf: ReflectionFilter<Method?>?) {
        val methods = getDeclaredMethods(clazz, false)
        for (method in methods) {
            if (mf != null && !mf.matches(method)) {
                continue
            }
            try {
                mc.doWith(method)
            } catch (ex: Exception) {
                handleReflectionException(ex)
            }
        }
        if (clazz.superclass != null && (mf !== USER_DECLARED_METHODS || clazz.superclass != Any::class.java)) {
            doWithMethods(clazz.superclass, mc, mf)
        } else if (clazz.isInterface) {
            for (superIfc in clazz.interfaces) {
                doWithMethods(superIfc, mc, mf)
            }
        }
    }

    /**
     * 获取当前类的所有方法(包括当前类所继承和实现的接口)
     */
    fun getAllDeclaredMethods(leafClass: Class<*>): Array<Method> {
        val methods: MutableList<Method?> = ArrayList(20)
        doWithMethods(leafClass) { e -> methods.add(e) }
        return methods.filterNotNull().toTypedArray()
    }

    /**
     * 获取当前类和所有超类的唯一声明方法集
     * 当前类方法首先被包含在内，在遍历超类层次结构时，任何与已包含的方法匹配的签名的方法都会被过滤掉
     */
    fun getUniqueDeclaredMethods(leafClass: Class<*>): Array<Method> {
        return getUniqueDeclaredMethods(leafClass, null)
    }

    /**
     * 获取当前类和所有超类的唯一声明方法集并设置过滤器
     * 当前类方法首先被包含在内，在遍历超类层次结构时，任何与已包含的方法匹配的签名的方法都会被过滤掉
     */
    fun getUniqueDeclaredMethods(leafClass: Class<*>, mf: ReflectionFilter<Method?>?): Array<Method> {
        val methods: MutableList<Method?> = ArrayList(20)
        doWithMethods(leafClass, { method: Method? ->
            var knownSignature = false
            var methodBeingOverriddenWithCovariantReturnType: Method? = null
            for (existingMethod in methods) {
                if (method!!.name == existingMethod!!.name && method.parameterCount == existingMethod.parameterCount && method.parameterTypes.contentEquals(
                        existingMethod.parameterTypes
                    )
                ) {
                    if (existingMethod.returnType != method.returnType &&
                        existingMethod.returnType.isAssignableFrom(method.returnType)
                    ) {
                        methodBeingOverriddenWithCovariantReturnType = existingMethod
                    } else {
                        knownSignature = true
                    }
                    break
                }
            }
            if (methodBeingOverriddenWithCovariantReturnType != null) {
                methods.remove(methodBeingOverriddenWithCovariantReturnType)
            }
            if (!knownSignature && !isCglibRenamedMethod(method)) {
                methods.add(method)
            }
        }, mf)
        return methods.filterNotNull().toTypedArray()
    }

    /**
     * 获取当前类和所有超类的方法集
     */
    fun getDeclaredMethods(clazz: Class<*>): Array<Method?> {
        return getDeclaredMethods(clazz, true)
    }

    private fun getDeclaredMethods(clazz: Class<*>, defensive: Boolean): Array<Method?> {
        notNull(clazz, "Class must not be null")
        val result: Array<Method?>
        try {
            val declaredMethods = clazz.getDeclaredMethods()
            val defaultMethods = findConcreteMethodsOnInterfaces(clazz)
            if (defaultMethods != null) {
                result = arrayOfNulls(declaredMethods.size + defaultMethods.size)
                System.arraycopy(declaredMethods, 0, result, 0, declaredMethods.size)
                var index = declaredMethods.size
                for (defaultMethod in defaultMethods) {
                    result[index] = defaultMethod
                    index++
                }
            } else {
                result = declaredMethods
            }
        } catch (ex: Throwable) {
            throw IllegalStateException(
                "Failed to introspect Class [" + clazz.getName() +
                        "] from ClassLoader [" + clazz.getClassLoader() + "]", ex
            )
        }
        return if (result.isEmpty() || !defensive) result else result.clone()
    }

    /**
     * 获取接口中的非抽象方法
     */
    private fun findConcreteMethodsOnInterfaces(clazz: Class<*>): List<Method>? {
        var result: MutableList<Method>? = null
        for (ifc in clazz.interfaces) {
            for (ifcMethod in ifc.getMethods()) {
                if (!Modifier.isAbstract(ifcMethod.modifiers)) {
                    if (result == null) {
                        result = ArrayList()
                    }
                    result.add(ifcMethod)
                }
            }
        }
        return result
    }

    /**
     * 是否为Object中的equals方法
     *
     * @see Object.equals
     */
    fun isEqualsMethod(method: Method?): Boolean {
        return method != null && method.parameterCount == 1 && "equals" == method.name && method.parameterTypes[0] == Any::class.java
    }

    /**
     * 是否为Object中的hashCode方法
     *
     * @see Object.hashCode
     */
    fun isHashCodeMethod(method: Method?): Boolean {
        return method != null && method.parameterCount == 0 && method.name == "hashCode"
    }

    /**
     * 是否为Object中的toString方法
     *
     * @see Object.toString
     */
    fun isToStringMethod(method: Method?): Boolean {
        return method != null && method.parameterCount == 0 && method.name == "toString"
    }

    /**
     * 方法最初是否由Object类声明
     */
    fun isObjectMethod(method: Method?): Boolean {
        return method != null && (method.declaringClass == Any::class.java ||
                isEqualsMethod(method) || isHashCodeMethod(method) || isToStringMethod(method))
    }

    /**
     * 是否为cglib的重命名方法
     */
    fun isCglibRenamedMethod(renamedMethod: Method?): Boolean {
        val name = renamedMethod!!.name
        if (name.startsWith(CGLIB_RENAMED_METHOD_PREFIX)) {
            var i = name.length - 1
            while (i >= 0 && Character.isDigit(name[i])) {
                i--
            }
            return i > CGLIB_RENAMED_METHOD_PREFIX.length && i < name.length - 1 && name[i] == '$'
        }
        return false
    }

    /**
     * 开放方法访问权限
     */
    fun makeAccessible(method: Method) {
        if ((!Modifier.isPublic(method.modifiers) ||
                    !Modifier.isPublic(method.declaringClass.modifiers)) && !method.isAccessible
        ) {
            method.setAccessible(true)
        }
    }
    /**
     * 查找field(向上直到Object类) 可以单独根据name或者type查找
     */
    /**
     * 查找field(向上直到Object类)
     */
    fun findField(clazz: Class<*>?, name: String?, type: Class<*>? = null): Field? {
        notNull(clazz, "Class must not be null")
        isTrue(name != null || type != null)
        var searchType = clazz
        while (Any::class.java != searchType && searchType != null) {
            val fields = getDeclaredFields(searchType)
            for (field in fields) {
                if ((name == null || name == field.name) &&
                    (type == null || type == field.type)
                ) {
                    return field
                }
            }
            searchType = searchType.superclass
        }
        return null
    }

    /**
     * 设置field的值
     */
    fun setField(field: Field, target: Any?, value: Any?) {
        try {
            field[target] = value
        } catch (ex: IllegalAccessException) {
            handleReflectionException(ex)
        }
    }

    /**
     * 获取field的值
     */
    fun getField(field: Field, target: Any?): Any {
        try {
            return field[target]
        } catch (ex: IllegalAccessException) {
            handleReflectionException(ex)
        }
        throw IllegalStateException("Should never get here")
    }
    /**
     * 遍历当前类所有字段通过ReflectionFilter<Field>过滤的使用ReflectionCallback<Field>执行
    </Field></Field> */
    /**
     * 遍历当前类所有字段并使用ReflectionCallback<Field>执行
    </Field> */
    fun doWithLocalFields(clazz: Class<*>?, fc: ReflectionCallback<Field?>, ff: ReflectionFilter<Field?>? = null) {
        for (field in getDeclaredFields(clazz)) {
            if (ff != null && !ff.matches(field)) {
                continue
            }
            try {
                fc.doWith(field)
            } catch (ex: Exception) {
                handleReflectionException(ex)
            }
        }
    }
    /**
     * 遍历所有字段通过ReflectionFilter<Field>过滤的使用ReflectionCallback<Field>执行
    </Field></Field> */
    /**
     * 遍历所有字段并使用ReflectionCallback<Field>执行
    </Field> */
    fun doWithFields(clazz: Class<*>?, fc: ReflectionCallback<Field>, ff: ReflectionFilter<Field>? = null) {
        var targetClass = clazz
        do {
            val fields = getDeclaredFields(targetClass)
            for (field in fields) {
                if (ff != null && !ff.matches(field)) {
                    continue
                }
                try {
                    fc.doWith(field)
                } catch (ex: Exception) {
                    handleReflectionException(ex)
                }
            }
            targetClass = targetClass!!.superclass
        } while (targetClass != null && targetClass != Any::class.java)
    }

    /**
     * 获取当前类所有字段
     */
    private fun getDeclaredFields(clazz: Class<*>?): Array<Field> {
        notNull(clazz, "Class must not be null")
        return try {
            clazz!!.getDeclaredFields()
        } catch (ex: Throwable) {
            throw IllegalStateException(
                "Failed to introspect Class [" + clazz!!.getName() +
                        "] from ClassLoader [" + clazz.getClassLoader() + "]", ex
            )
        }
    }

    /**
     * 获取java Beans的PropertyDescriptor
     */
    fun getPropertyDescriptors(beanClass: Class<*>?, stopClass: Class<*>?): Array<PropertyDescriptor> {
        try {
            return Introspector.getBeanInfo(beanClass, stopClass).propertyDescriptors
        } catch (ex: Exception) {
            handleReflectionException(ex)
        }
        throw IllegalStateException("Should never get here")
    }

    /**
     * 遍历所有PropertyDescriptor通过ReflectionFilter过滤的使用ReflectionCallback执行
     */
    fun doWithPropertyDescriptors(
        beanClass: Class<*>?, stopClass: Class<*>?,
        pc: ReflectionCallback<PropertyDescriptor?>,
        pf: ReflectionFilter<PropertyDescriptor?>?
    ) {
        val propertyDescriptors = getPropertyDescriptors(beanClass, stopClass)
        for (propertyDescriptor in propertyDescriptors) {
            if (pf != null && !pf.matches(propertyDescriptor)) {
                continue
            }
            try {
                pc.doWith(propertyDescriptor)
            } catch (ex: Exception) {
                handleReflectionException(ex)
            }
        }
    }

    /**
     * 将src的所有字段值复制到dest
     */
    fun shallowCopyFieldState(src: Any, dest: Any) {
        notNull(src, "Source for field copy cannot be null")
        notNull(dest, "Destination for field copy cannot be null")
        require(src.javaClass.isAssignableFrom(dest.javaClass)) {
            "Destination class [" + dest.javaClass.getName() +
                    "] must be same or subclass as source class [" + src.javaClass.getName() + "]"
        }
        doWithFields(src.javaClass, { field: Field ->
            makeAccessible(field)
            val srcValue = field[src]
            field[dest] = srcValue
        }, COPYABLE_FIELDS)
    }

    /**
     * 是否为常量
     */
    fun isPublicStaticFinal(field: Field): Boolean {
        val modifiers = field.modifiers
        return Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)
    }

    /**
     * 开放字段访问权限
     */
    fun makeAccessible(field: Field) {
        if ((!Modifier.isPublic(field.modifiers) ||
                    !Modifier.isPublic(field.declaringClass.modifiers) ||
                    Modifier.isFinal(field.modifiers)) && !field.isAccessible
        ) {
            field.setAccessible(true)
        }
    }

    private fun notNull(`object`: Any?, message: String) {
        requireNotNull(`object`) { message }
    }

    private fun isTrue(expression: Boolean) {
        require(expression) { "Either name or type of the field must be specified" }
    }

    /**
     * 对每个单位行动
     */
    fun interface ReflectionCallback<T> {
        /**
         * 执行操作
         */
        @Throws(Exception::class)
        fun doWith(t: T)
    }

    /**
     * 过滤可用于回调
     */
    fun interface ReflectionFilter<F> {
        /**
         * 是否匹配
         */
        fun matches(f: F): Boolean

        /**
         * 联合过滤器
         */
        fun and(next: ReflectionFilter<F>): ReflectionFilter<F>? {
            notNull(next, "Next ReflectionFilter must not be null")
            return ReflectionFilter { method: F -> matches(method) && next.matches(method) }
        }
    }
}
