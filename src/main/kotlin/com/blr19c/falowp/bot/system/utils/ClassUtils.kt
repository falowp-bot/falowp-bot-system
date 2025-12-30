package com.blr19c.falowp.bot.system.utils

import java.beans.Introspector
import java.io.Closeable
import java.io.Externalizable
import java.io.Serializable
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * util来源于spring
 */
@Suppress("UNUSED")
object ClassUtils {
    /**
     * Suffix for array class names: `"[]"`.
     */
    const val ARRAY_SUFFIX = "[]"

    /**
     * Prefix for internal array class names: `"["`.
     */
    private const val INTERNAL_ARRAY_PREFIX = "["

    /**
     * Prefix for internal non-primitive array class names: `"[L"`.
     */
    private const val NON_PRIMITIVE_ARRAY_PREFIX = "[L"

    /**
     * A reusable empty class array constant.
     */
    private val EMPTY_CLASS_ARRAY = arrayOf<Class<*>>()

    /**
     * The package separator character: `'.'`.
     */
    private const val PACKAGE_SEPARATOR = '.'

    /**
     * The path separator character: `'/'`.
     */
    private const val PATH_SEPARATOR = '/'

    /**
     * The nested class separator character: `'$'`.
     */
    private const val NESTED_CLASS_SEPARATOR = '$'

    /**
     * The CGLIB class separator: `"$$"`.
     */
    const val CGLIB_CLASS_SEPARATOR = "$$"

    /**
     * The ".class" file suffix.
     */
    const val CLASS_FILE_SUFFIX = ".class"

    /**
     * Map with primitive wrapper type as key and corresponding primitive
     */
    private val primitiveWrapperTypeMap: MutableMap<Class<*>, Class<*>?> = IdentityHashMap(9)

    /**
     * Map with primitive type as key and corresponding wrapper
     */
    private val primitiveTypeToWrapperMap: MutableMap<Class<*>?, Class<*>> = IdentityHashMap(9)

    /**
     * Map with primitive type name as key and corresponding primitive
     * type as value, for example: "int" -> "int.class".
     */
    private val primitiveTypeNameMap: MutableMap<String, Class<*>?> = HashMap(32)

    /**
     * Map with common Java language class name as key and corresponding Class as value.
     * Primarily for efficient deserialization of remote invocations.
     */
    private val commonClassCache: MutableMap<String, Class<*>> = HashMap(64)

    /**
     * Common Java language interfaces which are supposed to be ignored
     * when searching for 'primary' user-level interfaces.
     */
    private var javaLanguageInterfaces: Set<Class<*>>? = null

    /**
     * Cache for equivalent methods on an interface implemented by the declaring class.
     */
    private val interfaceMethodCache: MutableMap<Method, Method> = ConcurrentHashMap(256)

    init {
        primitiveWrapperTypeMap[Boolean::class.java] = Boolean::class.javaPrimitiveType
        primitiveWrapperTypeMap[Byte::class.java] = Byte::class.javaPrimitiveType
        primitiveWrapperTypeMap[Char::class.java] = Char::class.javaPrimitiveType
        primitiveWrapperTypeMap[Double::class.java] = Double::class.javaPrimitiveType
        primitiveWrapperTypeMap[Float::class.java] = Float::class.javaPrimitiveType
        primitiveWrapperTypeMap[Int::class.java] = Int::class.javaPrimitiveType
        primitiveWrapperTypeMap[Long::class.java] = Long::class.javaPrimitiveType
        primitiveWrapperTypeMap[Short::class.java] = Short::class.javaPrimitiveType
        primitiveWrapperTypeMap[Void::class.java] = Void.TYPE

        // Map entry iteration is less expensive to initialize than forEach with lambdas
        for ((key, value) in primitiveWrapperTypeMap) {
            primitiveTypeToWrapperMap[value] = key
            registerCommonClasses(key)
        }
        val primitiveTypes: MutableSet<Class<*>?> = HashSet(32)
        primitiveTypes.addAll(primitiveWrapperTypeMap.values)
        Collections.addAll(
            primitiveTypes,
            BooleanArray::class.java,
            ByteArray::class.java,
            CharArray::class.java,
            DoubleArray::class.java,
            FloatArray::class.java,
            IntArray::class.java,
            LongArray::class.java,
            ShortArray::class.java
        )
        for (primitiveType in primitiveTypes) {
            primitiveTypeNameMap[primitiveType!!.getName()] = primitiveType
        }
        registerCommonClasses(
            Array<Boolean>::class.java, Array<Byte>::class.java, Array<Char>::class.java, Array<Double>::class.java,
            Array<Float>::class.java, Array<Int>::class.java, Array<Long>::class.java, Array<Short>::class.java
        )
        registerCommonClasses(
            Number::class.java, Array<Number>::class.java, String::class.java, Array<String>::class.java,
            Class::class.java, Array<Any>::class.java, Any::class.java, Array<Any>::class.java
        )
        registerCommonClasses(
            Throwable::class.java, Exception::class.java, RuntimeException::class.java,
            Error::class.java, StackTraceElement::class.java, Array<StackTraceElement>::class.java
        )
        registerCommonClasses(
            Enum::class.java,
            Iterable::class.java,
            MutableIterator::class.java,
            Enumeration::class.java,
            MutableCollection::class.java,
            MutableList::class.java,
            MutableSet::class.java,
            MutableMap::class.java,
            MutableMap.MutableEntry::class.java,
            Optional::class.java
        )
        val javaLanguageInterfaceArray = arrayOf(
            Serializable::class.java, Externalizable::class.java,
            Closeable::class.java, AutoCloseable::class.java, Cloneable::class.java, Comparable::class.java
        )
        registerCommonClasses(*javaLanguageInterfaceArray)
        javaLanguageInterfaces = HashSet(listOf(*javaLanguageInterfaceArray))
    }

    /**
     * Register the given common classes with the ClassUtils cache.
     */
    private fun registerCommonClasses(vararg commonClasses: Class<*>) {
        for (clazz in commonClasses) {
            commonClassCache[clazz.getName()] = clazz
        }
    }

    val defaultClassLoader: ClassLoader?
        /**
         * Return the default ClassLoader to use: typically the thread context
         * ClassLoader, if available; the ClassLoader that loaded the ClassUtils
         * class will be used as fallback.
         *
         * Call this method if you intend to use the thread context ClassLoader
         * in a scenario where you clearly prefer a non-null ClassLoader reference:
         * for example, for class path resource loading (but not necessarily for
         * `Class.forName`, which accepts a `null` ClassLoader
         * reference as well).
         *
         * @return the default ClassLoader (only `null` if even the system
         * ClassLoader isn't accessible)
         * @see Thread.getContextClassLoader
         * @see ClassLoader.getSystemClassLoader
         */
        get() {
            var cl: ClassLoader? = null
            try {
                cl = Thread.currentThread().contextClassLoader
            } catch (ex: Throwable) {
                // Cannot access thread context ClassLoader - falling back...
            }
            if (cl == null) {
                // No thread context class loader -> use class loader of this class.
                cl = ClassUtils::class.java.classLoader
                if (cl == null) {
                    // getClassLoader() returning null indicates the bootstrap ClassLoader
                    try {
                        cl = ClassLoader.getSystemClassLoader()
                    } catch (ex: Throwable) {
                        // Cannot access system ClassLoader - oh well, maybe the caller can live with null...
                    }
                }
            }
            return cl
        }

    /**
     * Override the thread context ClassLoader with the environment's bean ClassLoader
     * if necessary, i.e. if the bean ClassLoader is not equivalent to the thread
     * context ClassLoader already.
     *
     * @param classLoaderToUse the actual ClassLoader to use for the thread context
     * @return the original thread context ClassLoader, or `null` if not overridden
     */

    fun overrideThreadContextClassLoader(classLoaderToUse: ClassLoader?): ClassLoader? {
        val currentThread = Thread.currentThread()
        val threadContextClassLoader = currentThread.contextClassLoader
        return if (classLoaderToUse != null && classLoaderToUse != threadContextClassLoader) {
            currentThread.contextClassLoader = classLoaderToUse
            threadContextClassLoader
        } else {
            null
        }
    }

    /**
     * Replacement for `Class.forName()` that also returns Class instances
     * for primitives (e.g. "int") and array class names (e.g. "String[]").
     * Furthermore, it is also capable of resolving nested class names in Java source
     * style (e.g. "java.lang.Thread.State" instead of "java.lang.Thread$State").
     *
     * @param name        the name of the Class
     * @param classLoader the class loader to use
     * @return a class instance for the supplied name
     * @throws ClassNotFoundException if the class was not found
     * @throws LinkageError           if the class file could not be loaded
     * @see Class.forName
     */
    @Throws(ClassNotFoundException::class, LinkageError::class)
    fun forName(name: String, classLoader: ClassLoader?): Class<*> {
        var clazz = resolvePrimitiveClassName(name)
        if (clazz == null) {
            clazz = commonClassCache[name]
        }
        if (clazz != null) {
            return clazz
        }

        // "java.lang.String[]" style arrays
        if (name.endsWith(ARRAY_SUFFIX)) {
            val elementClassName = name.substring(0, name.length - ARRAY_SUFFIX.length)
            val elementClass = forName(elementClassName, classLoader)
            return java.lang.reflect.Array.newInstance(elementClass, 0).javaClass
        }

        if (name.startsWith(NON_PRIMITIVE_ARRAY_PREFIX) && name.endsWith(";")) {
            val elementName = name.substring(NON_PRIMITIVE_ARRAY_PREFIX.length, name.length - 1)
            val elementClass = forName(elementName, classLoader)
            return java.lang.reflect.Array.newInstance(elementClass, 0).javaClass
        }

        if (name.startsWith(INTERNAL_ARRAY_PREFIX)) {
            val elementName = name.substring(INTERNAL_ARRAY_PREFIX.length)
            val elementClass = forName(elementName, classLoader)
            return java.lang.reflect.Array.newInstance(elementClass, 0).javaClass
        }
        var clToUse = classLoader
        if (clToUse == null) {
            clToUse = defaultClassLoader
        }
        return try {
            Class.forName(name, false, clToUse)
        } catch (ex: ClassNotFoundException) {
            val lastDotIndex = name.lastIndexOf(PACKAGE_SEPARATOR)
            if (lastDotIndex != -1) {
                val nestedClassName =
                    name.substring(0, lastDotIndex) + NESTED_CLASS_SEPARATOR + name.substring(lastDotIndex + 1)
                try {
                    return Class.forName(nestedClassName, false, clToUse)
                } catch (ex2: ClassNotFoundException) {
                    // Swallow - let original exception get through
                }
            }
            throw ex
        }
    }

    /**
     * Resolve the given class name into a Class instance. Supports
     * primitives (like "int") and array class names (like "String[]").
     *
     * This is effectively equivalent to the `forName`
     * method with the same arguments, with the only difference being
     * the exceptions thrown in case of class loading failure.
     *
     * @param className   the name of the Class
     * @param classLoader the class loader to use
     * @return a class instance for the supplied name
     * @throws IllegalArgumentException if the class name was not resolvable
     * (that is, the class could not be found or the class file could not be loaded)
     * @throws IllegalStateException    if the corresponding class is resolvable but
     * there was a readability mismatch in the inheritance hierarchy of the class
     * (typically a missing dependency declaration in a Jigsaw module definition
     * for a superclass or interface implemented by the class to be loaded here)
     * @see .forName
     */
    @Throws(IllegalArgumentException::class)
    fun resolveClassName(className: String, classLoader: ClassLoader?): Class<*> {
        return try {
            forName(className, classLoader)
        } catch (err: IllegalAccessError) {
            throw IllegalStateException(
                "Readability mismatch in inheritance hierarchy of class [" +
                        className + "]: " + err.message, err
            )
        } catch (err: LinkageError) {
            throw IllegalArgumentException("Unresolvable class definition for class [$className]", err)
        } catch (ex: ClassNotFoundException) {
            throw IllegalArgumentException("Could not find class [$className]", ex)
        }
    }

    /**
     * Determine whether the [Class] identified by the supplied name is present
     * and can be loaded. Will return `false` if either the class or
     * one of its dependencies is not present or cannot be loaded.
     *
     * @param className   the name of the class to check
     * @param classLoader the class loader to use
     * (maybe `null` which indicates the default class loader)
     * @return whether the specified class is present (including all of its
     * superclasses and interfaces)
     * @throws IllegalStateException if the corresponding class is resolvable but
     * there was a readability mismatch in the inheritance hierarchy of the class
     * (typically a missing dependency declaration in a Jigsaw module definition
     * for a superclass or interface implemented by the class to be checked here)
     */
    fun isPresent(className: String, classLoader: ClassLoader?): Boolean {
        return try {
            forName(className, classLoader)
            true
        } catch (err: IllegalAccessError) {
            throw IllegalStateException(
                "Readability mismatch in inheritance hierarchy of class [" +
                        className + "]: " + err.message, err
            )
        } catch (ex: Throwable) {
            // Typically ClassNotFoundException or NoClassDefFoundError...
            false
        }
    }

    /**
     * Check whether the given class is visible in the given ClassLoader.
     *
     * @param clazz       the class to check (typically an interface)
     * @param classLoader the ClassLoader to check against
     * (maybe `null` in which case this method will always return `true`)
     */
    fun isVisible(clazz: Class<*>, classLoader: ClassLoader?): Boolean {
        if (classLoader == null) {
            return true
        }
        try {
            if (clazz.classLoader === classLoader) {
                return true
            }
        } catch (ex: SecurityException) {
            // Fall through to loadable check below
        }

        // Visible if same Class can be loaded from given ClassLoader
        return isLoadable(clazz, classLoader)
    }

    /**
     * Check whether the given class is cache-safe in the given context,
     * i.e. whether it is loaded by the given ClassLoader or a parent of it.
     *
     * @param clazz       the class to analyze
     * @param classLoader the ClassLoader to potentially cache metadata in
     * (maybe `null` which indicates the system class loader)
     */
    fun isCacheSafe(clazz: Class<*>, classLoader: ClassLoader?): Boolean {
        try {
            var target = clazz.classLoader
            // Common cases
            if (target === classLoader || target == null) {
                return true
            }
            if (classLoader == null) {
                return false
            }
            // Check for match in ancestors -> positive
            var current = classLoader
            while (current != null) {
                current = current.parent
                if (current === target) {
                    return true
                }
            }
            // Check for match in children -> negative
            while (target != null) {
                target = target.parent
                if (target === classLoader) {
                    return false
                }
            }
        } catch (ex: SecurityException) {
            // Fall through to loadable check below
        }

        // Fallback for ClassLoaders without parent/child relationship:
        // safe if same Class can be loaded from given ClassLoader
        return classLoader != null && isLoadable(clazz, classLoader)
    }

    /**
     * Check whether the given class is loadable in the given ClassLoader.
     *
     * @param clazz       the class to check (typically an interface)
     * @param classLoader the ClassLoader to check against
     * @since 5.0.6
     */
    private fun isLoadable(clazz: Class<*>, classLoader: ClassLoader): Boolean {
        return try {
            clazz == classLoader.loadClass(clazz.getName())
            // Else: different class with same name found
        } catch (ex: ClassNotFoundException) {
            // No corresponding class found at all
            false
        }
    }

    /**
     * Resolve the given class name as primitive class, if appropriate,
     * Does *not* support the "[]" suffix notation for primitive arrays;
     * this is only supported by [.forName].
     *
     * @param name the name of the potentially primitive class
     * @return the primitive class, or `null` if the name does not denote
     * a primitive class or primitive array class
     */

    fun resolvePrimitiveClassName(name: String?): Class<*>? {
        var result: Class<*>? = null
        // Most class names will be quite long, considering that they
        // SHOULD sit in a package, so a length check is worthwhile.
        if (name != null && name.length <= 7) {
            // Could be a primitive - likely.
            result = primitiveTypeNameMap[name]
        }
        return result
    }

    /**
     * Check if the given class represents a primitive wrapper,
     * i.e. Boolean, Byte, Character, Short, Integer, Long, Float, Double, or
     * Void.
     *
     * @param clazz the class to check
     * @return whether the given class is a primitive wrapper class
     */
    fun isPrimitiveWrapper(clazz: Class<*>): Boolean {
        return primitiveWrapperTypeMap.containsKey(clazz)
    }

    /**
     * Check if the given class represents a primitive (i.e. boolean, byte,
     * char, short, int, long, float, or double), `void`, or a wrapper for
     * those types (i.e. Boolean, Byte, Character, Short, Integer, Long, Float,
     * Double, or Void).
     *
     * @param clazz the class to check
     * @return `true` if the given class represents a primitive, void, or
     * a wrapper class
     */
    fun isPrimitiveOrWrapper(clazz: Class<*>): Boolean {
        return clazz.isPrimitive || isPrimitiveWrapper(clazz)
    }

    /**
     * Check if the given class represents an array of primitives,
     * i.e. boolean, byte, char, short, int, long, float, or double.
     *
     * @param clazz the class to check
     * @return whether the given class is a primitive array class
     */
    fun isPrimitiveArray(clazz: Class<*>): Boolean {
        return clazz.isArray && clazz.componentType.isPrimitive
    }

    /**
     * Check if the given class represents an array of primitive wrappers,
     * i.e. Boolean, Byte, Character, Short, Integer, Long, Float, or Double.
     *
     * @param clazz the class to check
     * @return whether the given class is a primitive wrapper array class
     */
    fun isPrimitiveWrapperArray(clazz: Class<*>): Boolean {
        return clazz.isArray && isPrimitiveWrapper(clazz.componentType)
    }

    /**
     * Resolve the given class if it is a primitive class,
     * returning the corresponding primitive wrapper type instead.
     *
     * @param clazz the class to check
     * @return the original class, or a primitive wrapper for the original primitive type
     */
    fun resolvePrimitiveIfNecessary(clazz: Class<*>): Class<*> {
        return if (clazz.isPrimitive && clazz != Void.TYPE) primitiveTypeToWrapperMap[clazz]!! else clazz
    }

    /**
     * Check if the right-hand side type maybe assigned to the left-hand side
     * type, assuming setting by reflection. Considers primitive wrapper
     * classes as assignable to the corresponding primitive types.
     *
     * @param lhsType the target type
     * @param rhsType the value type that should be assigned to the target type
     * @return if the target type is assignable from the value type
     */
    fun isAssignable(lhsType: Class<*>, rhsType: Class<*>): Boolean {
        if (lhsType.isAssignableFrom(rhsType)) {
            return true
        }
        return if (lhsType.isPrimitive) {
            val resolvedPrimitive = primitiveWrapperTypeMap[rhsType]
            lhsType == resolvedPrimitive
        } else {
            val resolvedWrapper = primitiveTypeToWrapperMap[rhsType]
            resolvedWrapper != null && lhsType.isAssignableFrom(resolvedWrapper)
        }
    }

    /**
     * Determine if the given type is assignable from the given value,
     * assuming setting by reflection. Considers primitive wrapper classes
     * as assignable to the corresponding primitive types.
     *
     * @param type  the target type
     * @param value the value that should be assigned to the type
     * @return if the type is assignable from the value
     */
    fun isAssignableValue(type: Class<*>, value: Any?): Boolean {
        return if (value != null) isAssignable(type, value.javaClass) else !type.isPrimitive
    }

    /**
     * Convert a "/"-based resource path to a "."-based fully qualified class name.
     *
     * @param resourcePath the resource path pointing to a class
     * @return the corresponding fully qualified class name
     */
    fun convertResourcePathToClassName(resourcePath: String): String {
        return resourcePath.replace(PATH_SEPARATOR, PACKAGE_SEPARATOR)
    }

    /**
     * Convert a "."-based fully qualified class name to a "/"-based resource path.
     *
     * @param className the fully qualified class name
     * @return the corresponding resource path, pointing to the class
     */
    fun convertClassNameToResourcePath(className: String): String {
        return className.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR)
    }

    /**
     * Return a path suitable for use with `ClassLoader.getResource`
     * (also suitable for use with `Class.getResource` by prepending a
     * slash ('/') to the return value). Built by taking the package of the specified
     * class file, converting all dots ('.') to slashes ('/'), adding a trailing slash
     * if necessary, and concatenating the specified resource name to this.
     * <br></br>As such, this function maybe used to build a path suitable for
     * loading a resource file that is in the same package as a class file,
     *
     * @param clazz        the Class whose package will be used as the base
     * @param resourceName the resource name to append. A leading slash is optional.
     * @return the built-up resource path
     * @see ClassLoader.getResource
     *
     * @see Class.getResource
     */
    fun addResourcePathToPackagePath(clazz: Class<*>?, resourceName: String): String {
        return if (!resourceName.startsWith("/")) {
            classPackageAsResourcePath(clazz) + '/' + resourceName
        } else classPackageAsResourcePath(clazz) + resourceName
    }

    /**
     * Given an input class object, return a string which consists of the
     * class's package name as a pathname, i.e., all dots ('.') are replaced by
     * slashes ('/'). Neither a leading nor trailing slash is added. The result
     * could be concatenated with a slash and the name of a resource and fed
     * directly to `ClassLoader.getResource()`. For it to be fed to
     * `Class.getResource` instead, a leading slash would also have
     * to be prepended to the returned value.
     *
     * @param clazz the input class. A `null` value or the default
     * (empty) package will result in an empty string ("") being returned.
     * @return a path which represents the package name
     * @see ClassLoader.getResource
     *
     * @see Class.getResource
     */
    fun classPackageAsResourcePath(clazz: Class<*>?): String {
        if (clazz == null) {
            return ""
        }
        val className = clazz.getName()
        val packageEndIndex = className.lastIndexOf(PACKAGE_SEPARATOR)
        if (packageEndIndex == -1) {
            return ""
        }
        val packageName = className.substring(0, packageEndIndex)
        return packageName.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR)
    }

    /**
     * Build a String that consists of the names of the classes/interfaces
     * in the given array.
     *
     * Basically like `AbstractCollection.toString()`, but stripping
     * the class " interface " prefix before every class name.
     *
     * @param classes an array of Class objects
     * @return a String of form "[com.foo.Bar, com.foo.Baz]"
     * @see java.util.AbstractCollection.toString
     */
    fun classNamesToString(vararg classes: Class<*>?): String {
        return classNamesToString(listOf(*classes))
    }

    fun classNamesToString(classes: Collection<Class<*>?>): String {
        if (classes.isEmpty()) {
            return "[]"
        }
        val stringJoiner = StringJoiner(", ", "[", "]")
        for (clazz in classes) {
            stringJoiner.add(clazz!!.getName())
        }
        return stringJoiner.toString()
    }

    /**
     * Copy the given `Collection` into a `Class` array.
     *
     * The `Collection` must contain `Class` elements only.
     *
     * @param collection the `Collection` to copy
     * @return the `Class` array
     *
     * @since 3.1
     */
    fun toClassArray(collection: Collection<Class<*>?>): Array<Class<*>> {
        return if (collection.isNotEmpty()) collection.filterNotNull().toTypedArray() else EMPTY_CLASS_ARRAY
    }

    /**
     * Return all interfaces that the given instance implements as an array,
     * including ones implemented by superclasses.
     *
     * @param instance the instance to analyze for interfaces
     * @return all interfaces that the given instance implements as an array
     */
    fun getAllInterfaces(instance: Any): Array<Class<*>> {
        return getAllInterfacesForClass(instance.javaClass)
    }

    /**
     * Return all interfaces that the given class implements as an array,
     * including ones implemented by superclasses.
     *
     * If the class itself is an interface, it gets returned as sole interface.
     *
     * @param clazz the class to analyze for interfaces
     * @return all interfaces that the given object implements as an array
     */
    fun getAllInterfacesForClass(clazz: Class<*>): Array<Class<*>> {
        return getAllInterfacesForClass(clazz, null)
    }

    /**
     * Return all interfaces that the given class implements as an array,
     * including ones implemented by superclasses.
     *
     * If the class itself is an interface, it gets returned as sole interface.
     *
     * @param clazz       the class to analyze for interfaces
     * @param classLoader the ClassLoader that the interfaces need to be visible in
     * (maybe `null` when accepting all declared interfaces)
     * @return all interfaces that the given object implements as an array
     */
    fun getAllInterfacesForClass(clazz: Class<*>, classLoader: ClassLoader?): Array<Class<*>> {
        return toClassArray(getAllInterfacesForClassAsSet(clazz, classLoader))
    }

    /**
     * Return all interfaces that the given instance implements as a Set,
     * including ones implemented by superclasses.
     *
     * @param instance the instance to analyze for interfaces
     * @return all interfaces that the given instance implements as a Set
     */
    fun getAllInterfacesAsSet(instance: Any): Set<Class<*>?> {
        return getAllInterfacesForClassAsSet(instance.javaClass)
    }

    /**
     * Return all interfaces that the given class implements as a Set,
     * including ones implemented by superclasses.
     *
     * If the class itself is an interface, it gets returned as sole interface.
     *
     * @param clazz the class to analyze for interfaces
     * @return all interfaces that the given object implements as a Set
     */
    fun getAllInterfacesForClassAsSet(clazz: Class<*>): Set<Class<*>?> {
        return getAllInterfacesForClassAsSet(clazz, null)
    }

    /**
     * Return all interfaces that the given class implements as a Set,
     * including ones implemented by superclasses.
     *
     * If the class itself is an interface, it gets returned as sole interface.
     *
     * @param clazz       the class to analyze for interfaces
     * @param classLoader the ClassLoader that the interfaces need to be visible in
     * (maybe `null` when accepting all declared interfaces)
     * @return all interfaces that the given object implements as a Set
     */
    fun getAllInterfacesForClassAsSet(clazz: Class<*>, classLoader: ClassLoader?): Set<Class<*>?> {
        if (clazz.isInterface && isVisible(clazz, classLoader)) {
            return setOf(clazz)
        }
        val interfaces: MutableSet<Class<*>?> = LinkedHashSet()
        var current: Class<*>? = clazz
        while (current != null) {
            val cs = current.interfaces
            for (ifc in cs) {
                if (isVisible(ifc, classLoader)) {
                    interfaces.add(ifc)
                }
            }
            current = current.superclass
        }
        return interfaces
    }

    /**
     * Determine the common ancestor of the given classes, if any.
     *
     * @param clazz1 the class to introspect
     * @param clazz2 the other class to introspect
     * @return the common ancestor (i.e. common superclass, one interface
     * extending the other), or `null` if none found. If any of the
     * given classes is `null`, the other class will be returned.
     * @since 3.2.6
     */

    fun determineCommonAncestor(clazz1: Class<*>?, clazz2: Class<*>?): Class<*>? {
        if (clazz1 == null) {
            return clazz2
        }
        if (clazz2 == null) {
            return clazz1
        }
        if (clazz1.isAssignableFrom(clazz2)) {
            return clazz1
        }
        if (clazz2.isAssignableFrom(clazz1)) {
            return clazz2
        }
        var ancestor = clazz1
        do {
            ancestor = ancestor!!.superclass
            if (ancestor == null || Any::class.java == ancestor) {
                return null
            }
        } while (!ancestor.isAssignableFrom(clazz2))
        return ancestor
    }

    fun isJavaLanguageInterface(ifc: Class<*>): Boolean {
        return javaLanguageInterfaces!!.contains(ifc)
    }

    /**
     * Determine if the supplied class is an *inner class*,
     * i.e. a non-static member of an enclosing class.
     *
     * @return `true` if the supplied class is an inner class
     * @see Class.isMemberClass
     * @since 5.0.5
     */
    fun isInnerClass(clazz: Class<*>): Boolean {
        return clazz.isMemberClass && !Modifier.isStatic(clazz.modifiers)
    }

    /**
     * Return the user-defined class for the given instance: usually simply
     * the class of the given instance, but the original class in case of a
     * CGLIB-generated subclass.
     *
     * @param instance the instance to check
     * @return the user-defined class
     */
    fun getUserClass(instance: Any): Class<*> {
        return getUserClass(instance.javaClass)
    }

    /**
     * Return the user-defined class for the given class: usually simply the given
     * class, but the original class in case of a CGLIB-generated subclass.
     *
     * @param clazz the class to check
     * @return the user-defined class
     */
    fun getUserClass(clazz: Class<*>): Class<*> {
        if (clazz.getName().contains(CGLIB_CLASS_SEPARATOR)) {
            val superclass = clazz.superclass
            if (superclass != null && superclass != Any::class.java) {
                return superclass
            }
        }
        return clazz
    }

    /**
     * Return a descriptive name for the given object's type: usually simply
     * the class name, but component type class name + "[]" for arrays,
     * and an appended list of implemented interfaces for JDK proxies.
     *
     * @param value the value to introspect
     * @return the qualified name of the class
     */

    fun getDescriptiveType(value: Any?): String? {
        if (value == null) {
            return null
        }
        val clazz: Class<*> = value.javaClass
        return if (Proxy.isProxyClass(clazz)) {
            val prefix = clazz.getName() + " implementing "
            val result = StringJoiner(",", prefix, "")
            for (ifc in clazz.interfaces) {
                result.add(ifc.getName())
            }
            result.toString()
        } else {
            clazz.getTypeName()
        }
    }

    /**
     * Check whether the given class matches the user-specified type name.
     *
     * @param clazz    the class to check
     * @param typeName the type name to match
     */
    fun matchesTypeName(clazz: Class<*>, typeName: String?): Boolean {
        return typeName != null &&
                (typeName == clazz.getTypeName() || typeName == clazz.getSimpleName())
    }

    /**
     * Get the class name without the qualified package name.
     *
     * @param className the className to get the short name for
     * @return the class name of the class without the package name
     * @throws IllegalArgumentException if the className is empty
     */
    fun getShortName(className: String): String {
        val lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR)
        var nameEndIndex = className.indexOf(CGLIB_CLASS_SEPARATOR)
        if (nameEndIndex == -1) {
            nameEndIndex = className.length
        }
        var shortName = className.substring(lastDotIndex + 1, nameEndIndex)
        shortName = shortName.replace(NESTED_CLASS_SEPARATOR, PACKAGE_SEPARATOR)
        return shortName
    }

    /**
     * Get the class name without the qualified package name.
     *
     * @param clazz the class to get the short name for
     * @return the class name of the class without the package name
     */
    fun getShortName(clazz: Class<*>): String {
        return getShortName(getQualifiedName(clazz))
    }

    /**
     * Return the short string name of a Java class in uncapitalized JavaBeans
     * property format. Strips the outer class name in case of a nested class.
     *
     * @param clazz the class
     * @return the short name rendered in a standard JavaBeans property format
     */
    fun getShortNameAsProperty(clazz: Class<*>): String {
        var shortName = getShortName(clazz)
        val dotIndex = shortName.lastIndexOf(PACKAGE_SEPARATOR)
        shortName = if (dotIndex != -1) shortName.substring(dotIndex + 1) else shortName
        return Introspector.decapitalize(shortName)
    }

    /**
     * Determine the name of the class file, relative to the containing
     * package: e.g. "String.class"
     *
     * @param clazz the class
     * @return the file name of the ".class" file
     */
    fun getClassFileName(clazz: Class<*>): String {
        val className = clazz.getName()
        val lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR)
        return className.substring(lastDotIndex + 1) + CLASS_FILE_SUFFIX
    }

    /**
     * Determine the name of the package of the given class,
     * e.g. "java.lang" for the `java.lang.String` class.
     *
     * @param clazz the class
     * @return the package name, or the empty String if the class
     * is defined in the default package
     */
    fun getPackageName(clazz: Class<*>): String {
        return getPackageName(clazz.getName())
    }

    /**
     * Determine the name of the package of the given fully-qualified class name,
     * e.g. "java.lang" for the `java.lang.String` class name.
     *
     * @param fqClassName the fully-qualified class name
     * @return the package name, or the empty String if the class
     * is defined in the default package
     */
    fun getPackageName(fqClassName: String): String {
        val lastDotIndex = fqClassName.lastIndexOf(PACKAGE_SEPARATOR)
        return if (lastDotIndex != -1) fqClassName.substring(0, lastDotIndex) else ""
    }

    /**
     * Return the qualified name of the given class: usually simply
     * the class name, but component type class name + "[]" for arrays.
     *
     * @param clazz the class
     * @return the qualified name of the class
     */
    fun getQualifiedName(clazz: Class<*>): String {
        return clazz.getTypeName()
    }

    /**
     * Return the qualified name of the given method, consisting of
     * fully qualified interface/class name + "." + method name.
     *
     * @param method the method
     * @return the qualified name of the method
     */
    fun getQualifiedMethodName(method: Method): String {
        return getQualifiedMethodName(method, null)
    }

    /**
     * Return the qualified name of the given method, consisting of
     * fully qualified interface/class name + "." + method name.
     *
     * @param method the method
     * @param clazz  the clazz that the method is being invoked on
     * (maybe `null` to indicate the method's declaring class)
     * @return the qualified name of the method
     * @since 4.3.4
     */
    fun getQualifiedMethodName(method: Method, clazz: Class<*>?): String {
        return (clazz ?: method.declaringClass).getName() + '.' + method.name
    }

    /**
     * Determine whether the given class has a public constructor with the given signature.
     *
     * Essentially translates `NoSuchMethodException` to "false".
     *
     * @param clazz      the clazz to analyze
     * @param paramTypes the parameter types of the method
     * @return whether the class has a corresponding constructor
     * @see Class.getConstructor
     */
    fun hasConstructor(clazz: Class<*>, vararg paramTypes: Class<*>?): Boolean {
        return getConstructorIfAvailable(clazz, *paramTypes) != null
    }

    /**
     * Determine whether the given class has a public constructor with the given signature,
     * and return it if available (else return `null`).
     *
     * Essentially translates `NoSuchMethodException` to `null`.
     *
     * @param clazz      the clazz to analyze
     * @param paramTypes the parameter types of the method
     * @return the constructor, or `null` if not found
     * @see Class.getConstructor
     */

    fun <T> getConstructorIfAvailable(clazz: Class<T>, vararg paramTypes: Class<*>?): Constructor<T>? {
        return try {
            clazz.getConstructor(*paramTypes)
        } catch (ex: NoSuchMethodException) {
            null
        }
    }

    /**
     * Determine whether the given class has a public method with the given signature.
     *
     * @param clazz  the clazz to analyze
     * @param method the method to look for
     * @return whether the class has a corresponding method
     * @since 5.2.3
     */
    fun hasMethod(clazz: Class<*>, method: Method): Boolean {
        if (clazz == method.declaringClass) {
            return true
        }
        val methodName = method.name
        val paramTypes = method.parameterTypes
        return getMethodOrNull(clazz, methodName, paramTypes) != null
    }

    /**
     * Determine whether the given class has a public method with the given signature.
     *
     * Essentially translates `NoSuchMethodException` to "false".
     *
     * @param clazz      the clazz to analyze
     * @param methodName the name of the method
     * @param paramTypes the parameter types of the method
     * @return whether the class has a corresponding method
     * @see Class.getMethod
     */
    fun hasMethod(clazz: Class<*>, methodName: String, vararg paramTypes: Class<*>): Boolean {
        return getMethodIfAvailable(clazz, methodName, *paramTypes) != null
    }

    /**
     * Determine whether the given class has a public method with the given signature,
     * and return it if available (else throws an `IllegalStateException`).
     *
     * In case of any signature specified, only returns the method if there is a
     * unique candidate, i.e. a single public method with the specified name.
     *
     * Essentially translates `NoSuchMethodException` to `IllegalStateException`.
     *
     * @param clazz      the clazz to analyze
     * @param methodName the name of the method
     * @param paramTypes the parameter types of the method
     * @return the method (never `null`)
     * @throws IllegalStateException if the method has not been found
     * @see Class.getMethod
     */
    fun getMethod(clazz: Class<*>, methodName: String, vararg paramTypes: Class<*>): Method {
        return if (paramTypes.isEmpty()) {
            try {
                clazz.getMethod(methodName, *paramTypes)
            } catch (ex: NoSuchMethodException) {
                throw IllegalStateException("Expected method not found: $ex")
            }
        } else {
            val candidates = findMethodCandidatesByName(clazz, methodName)
            if (candidates.size == 1) {
                candidates.iterator().next()
            } else if (candidates.isEmpty()) {
                throw IllegalStateException("Expected method not found: " + clazz.getName() + '.' + methodName)
            } else {
                throw IllegalStateException("No unique method found: " + clazz.getName() + '.' + methodName)
            }
        }
    }

    fun getMethodIfAvailable(clazz: Class<*>, methodName: String, vararg paramTypes: Class<*>): Method? {
        return if (paramTypes.isEmpty()) {
            getMethodOrNull(clazz, methodName, arrayOf(*paramTypes))
        } else {
            val candidates = findMethodCandidatesByName(clazz, methodName)
            if (candidates.size == 1) {
                candidates.iterator().next()
            } else null
        }
    }

    /**
     * Return the number of methods with a given name (with any argument types),
     * for the given class and/or its superclasses. Includes non-public methods.
     *
     * @param clazz      the clazz to check
     * @param methodName the name of the method
     * @return the number of methods with the given name
     */
    fun getMethodCountForName(clazz: Class<*>, methodName: String): Int {
        var count = 0
        val declaredMethods = clazz.declaredMethods
        for (method in declaredMethods) {
            if (methodName == method.name) {
                count++
            }
        }
        val cs = clazz.interfaces
        for (ifc in cs) {
            count += getMethodCountForName(ifc, methodName)
        }
        if (clazz.superclass != null) {
            count += getMethodCountForName(clazz.superclass, methodName)
        }
        return count
    }

    /**
     * Does the given class or one of its superclasses at least have one or more
     * methods with the supplied name (with any argument types)?
     * Includes non-public methods.
     *
     * @param clazz      the clazz to check
     * @param methodName the name of the method
     * @return whether there is at least one method with the given name
     */
    fun hasAtLeastOneMethodWithName(clazz: Class<*>, methodName: String): Boolean {
        val declaredMethods = clazz.declaredMethods
        for (method in declaredMethods) {
            if (method.name == methodName) {
                return true
            }
        }
        val cs = clazz.interfaces
        for (ifc in cs) {
            if (hasAtLeastOneMethodWithName(ifc, methodName)) {
                return true
            }
        }
        return clazz.superclass != null && hasAtLeastOneMethodWithName(clazz.superclass, methodName)
    }

    /**
     * Determine a corresponding interface method for the given method handle, if possible.
     *
     * This is particularly useful for arriving at a public exported type on Jigsaw
     * which can be reflectively invoked without an illegal access warning.
     *
     * @param method the method to be invoked, potentially from an implementation class
     * @return the corresponding interface method, or the original method if none found
     * @see .getMostSpecificMethod
     *
     * @since 5.1
     */
    fun getInterfaceMethodIfPossible(method: Method): Method {
        return if (!Modifier.isPublic(method.modifiers) || method.declaringClass.isInterface) {
            method
        } else interfaceMethodCache.computeIfAbsent(method) { key: Method ->
            var current = key.declaringClass
            while (current != Any::class.java) {
                val cs = current.interfaces
                for (ifc in cs) {
                    try {
                        return@computeIfAbsent ifc.getMethod(key.name, *key.parameterTypes)
                    } catch (ex: NoSuchMethodException) {
                        // ignore
                    }
                }
                current = current.superclass
            }
            key
        }
    }

    /**
     * Determine whether the given method is declared by the user or at least pointing to
     * a user-declared method.
     *
     * Checks [Method.isSynthetic] (for implementation methods) as well as the
     * `GroovyObject` interface (for interface methods; on an implementation class,
     * implementations of the `GroovyObject` methods will be marked as synthetic anyway).
     * Note that, despite being synthetic, bridge methods ([Method.isBridge]) are considered
     * as user-level methods since they are eventually pointing to a user-declared generic method.
     *
     * @param method the method to check
     * @return `true` if the method can be considered as user-declared; `false` otherwise
     */
    fun isUserLevelMethod(method: Method): Boolean {
        return method.isBridge || !method.isSynthetic && !isGroovyObjectMethod(method)
    }

    private fun isGroovyObjectMethod(method: Method): Boolean {
        return method.declaringClass.getName() == "groovy.lang.GroovyObject"
    }

    /**
     * Determine whether the given method is overridable in the given target class.
     *
     * @param method      the method to check
     * @param targetClass the target class to check against
     */
    private fun isOverridable(method: Method, targetClass: Class<*>?): Boolean {
        if (Modifier.isPrivate(method.modifiers)) {
            return false
        }
        return if (Modifier.isPublic(method.modifiers) || Modifier.isProtected(method.modifiers)) {
            true
        } else targetClass == null || getPackageName(method.declaringClass) == getPackageName(
            targetClass
        )
    }

    /**
     * Return a public static method of a class.
     *
     * @param clazz      the class which defines the method
     * @param methodName the static method name
     * @param args       the parameter types to the method
     * @return the static method, or `null` if no static method was found
     * @throws IllegalArgumentException if the method name is blank or the clazz is null
     */

    fun getStaticMethod(clazz: Class<*>, methodName: String, vararg args: Class<*>): Method? {
        return try {
            val method = clazz.getMethod(methodName, *args)
            if (Modifier.isStatic(method.modifiers)) method else null
        } catch (ex: NoSuchMethodException) {
            null
        }
    }


    private fun getMethodOrNull(clazz: Class<*>, methodName: String, paramTypes: Array<Class<*>>): Method? {
        return try {
            clazz.getMethod(methodName, *paramTypes)
        } catch (ex: NoSuchMethodException) {
            null
        }
    }

    private fun findMethodCandidatesByName(clazz: Class<*>, methodName: String): Set<Method> {
        val candidates: MutableSet<Method> = HashSet(1)
        val methods = clazz.methods
        for (method in methods) {
            if (methodName == method.name) {
                candidates.add(method)
            }
        }
        return candidates
    }
}
