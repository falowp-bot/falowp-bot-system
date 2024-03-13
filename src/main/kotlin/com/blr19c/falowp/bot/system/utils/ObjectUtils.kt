package com.blr19c.falowp.bot.system.utils

import java.util.*

/**
 * util来源于spring
 */
@Suppress("UNCHECKED_CAST", "MemberVisibilityCanBePrivate", "UNUSED")
object ObjectUtils {
    private const val INITIAL_HASH = 7
    private const val MULTIPLIER = 31
    private const val EMPTY_STRING = ""
    private const val NULL_STRING = "null"
    private const val ARRAY_START = "{"
    private const val ARRAY_END = "}"
    private const val EMPTY_ARRAY = ARRAY_START + ARRAY_END
    private const val ARRAY_ELEMENT_SEPARATOR = ", "
    private val EMPTY_OBJECT_ARRAY = arrayOfNulls<Any>(0)

    /**
     * Return whether the given throwable is a checked exception:
     * that is, neither a RuntimeException nor an Error.
     *
     * @param ex the throwable to check
     * @return whether the throwable is a checked exception
     * @see java.lang.Exception
     *
     * @see java.lang.RuntimeException
     *
     * @see java.lang.Error
     */
    fun isCheckedException(ex: Throwable?): Boolean {
        return !(ex is RuntimeException || ex is Error)
    }

    /**
     * Check whether the given exception is compatible with the specified
     * exception types, as declared in a throws' clause.
     *
     * @param ex                 the exception to check
     * @param declaredExceptions the exception types declared in the throws clause
     * @return whether the given exception is compatible
     */
    fun isCompatibleWithThrowsClause(ex: Throwable?, vararg declaredExceptions: Class<*>): Boolean {
        if (!isCheckedException(ex)) {
            return true
        }
        for (declaredException in declaredExceptions) {
            if (declaredException.isInstance(ex)) {
                return true
            }
        }
        return false
    }

    /**
     * Determine whether the given object is an array:
     * either an Object array or a primitive array.
     *
     * @param obj the object to check
     */
    fun isArray(obj: Any?): Boolean {
        return obj != null && obj.javaClass.isArray
    }

    /**
     * Determine whether the given array is empty:
     * i.e. `null` or of zero length.
     *
     * @param array the array to check
     * @see .isEmpty
     */

    fun isEmpty(array: Array<Any?>?): Boolean {
        return array.isNullOrEmpty()
    }

    /**
     * Determine whether the given object is empty.
     *
     * This method supports the following object types.
     *
     *  * `Optional`: considered empty if not [Optional.isPresent]
     *  * `Array`: considered empty if its length is zero
     *  * [CharSequence]: considered empty if its length is zero
     *  * [Collection]: delegates to [Collection.isEmpty]
     *  * [Map]: delegates to [Map.isEmpty]
     *
     *
     * If the given object is non-null and not one of the aforementioned
     * supported types, this method returns `false`.
     *
     * @param obj the object to check
     * @return `true` if the object is `null` or *empty*
     * @see Optional.isPresent
     * @see ObjectUtils.isEmpty
     * @since 4.2
     */

    fun isEmpty(obj: Any?): Boolean {
        if (obj == null) {
            return true
        }
        if (obj is Optional<*>) {
            return !obj.isPresent
        }
        if (obj is CharSequence) {
            return obj.length == 0
        }
        return if (obj.javaClass.isArray) {
            java.lang.reflect.Array.getLength(obj) == 0
        } else (obj as? Collection<*>)?.isEmpty() ?: ((obj as? Map<*, *>)?.isEmpty() ?: false)

        // else
    }

    /**
     * Unwrap the given object which is potentially a [java.util.Optional].
     *
     * @param obj the candidate object
     * @return either the value held within the `Optional`, `null`
     * if the `Optional` is empty, or simply the given object as-is
     * @since 5.0
     */
    fun unwrapOptional(obj: Any): Any? {
        if (obj is Optional<*>) {
            if (!obj.isPresent) {
                return null
            }
            return obj.get()
        }
        return obj
    }

    /**
     * Check whether the given array contains the given element.
     *
     * @param array   the array to check (maybe `null`,
     * in which case the return value will always be `false`)
     * @param element the element to check for
     * @return whether the element has been found in the given array
     */
    fun containsElement(array: Array<Any?>?, element: Any?): Boolean {
        if (array == null) {
            return false
        }
        for (arrayEle in array) {
            if (nullSafeEquals(arrayEle, element)) {
                return true
            }
        }
        return false
    }

    /**
     * Check whether the given array of enum constants contains a constant with the given name.
     *
     * @param enumValues    the enum values to check, typically obtained via `MyEnum.values()`
     * @param constant      the constant name to find (must not be null or empty string)
     * @param caseSensitive whether case is significant in determining a match
     * @return whether the constant has been found in the given array
     */
    fun containsConstant(enumValues: Array<Enum<*>>, constant: String, caseSensitive: Boolean = false): Boolean {
        for (candidate in enumValues) {
            if (if (caseSensitive) candidate.toString() == constant else candidate.toString()
                    .equals(constant, ignoreCase = true)
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Case insensitive alternative to
     *
     * @param <E>        the concrete Enum type
     * @param enumValues the array of all Enum constants in question, usually per `Enum.values()`
     * @param constant   the constant to get the enum value of
     * @throws IllegalArgumentException if the given constant is not found in the given array
     * of enum values. Use [.containsConstant] as a guard to avoid this exception.
    </E> */
    fun <E : Enum<*>?> caseInsensitiveValueOf(enumValues: Array<E>, constant: String): E {
        for (candidate in enumValues) {
            if (candidate.toString().equals(constant, ignoreCase = true)) {
                return candidate
            }
        }
        throw IllegalArgumentException(
            "Constant [" + constant + "] does not exist in enum type " +
                    enumValues.javaClass.componentType.getName()
        )
    }

    /**
     * Append the given object to the given array, returning a new array
     * consisting of the input array contents plus the given object.
     *
     * @param array the array to append to (can be `null`)
     * @param obj   the object to append
     * @return the new array (of the same component type; never `null`)
     */
    fun <A, O : A?> addObjectToArray(array: Array<A>?, obj: O): Array<A?> {
        return addObjectToArray<A, O>(array, obj, array?.size ?: 0)
    }

    /**
     * Add the given object to the given array at the specified position, returning
     * a new array consisting of the input array contents plus the given object.
     *
     * @param array    the array to add to (can be `null`)
     * @param obj      the object to append
     * @param position the position at which to add the object
     * @return the new array (of the same component type; never `null`)
     * @since 6.0
     */
    fun <A, O : A?> addObjectToArray(array: Array<A>?, obj: O?, position: Int): Array<A?> {
        var componentType: Class<*>? = Any::class.java
        if (array != null) {
            componentType = array.javaClass.componentType
        } else if (obj != null) {
            componentType = obj.javaClass
        }
        val newArrayLength = if (array != null) array.size + 1 else 1
        val newArray = java.lang.reflect.Array.newInstance(componentType, newArrayLength) as Array<A?>
        if (array != null) {
            System.arraycopy(array, 0, newArray, 0, position)
            System.arraycopy(array, position, newArray, position + 1, array.size - position)
        }
        newArray[position] = obj
        return newArray
    }

    /**
     * Convert the given array (which may be a primitive array) to an
     * object array (if necessary of primitive wrapper objects).
     *
     * A `null` source value will be converted to an
     * empty Object array.
     *
     * @param source the (potentially primitive) array
     * @return the corresponding object array (never `null`)
     * @throws IllegalArgumentException if the parameter is not an array
     */

    fun toObjectArray(source: Any?): Array<Any?> {
        if (source is Array<*> && source.isArrayOf<Any>()) {
            return source as Array<Any?>
        }
        if (source == null) {
            return EMPTY_OBJECT_ARRAY
        }
        require(source.javaClass.isArray) { "Source is not an array: $source" }
        val length = java.lang.reflect.Array.getLength(source)
        if (length == 0) {
            return EMPTY_OBJECT_ARRAY
        }
        val wrapperType: Class<*> = java.lang.reflect.Array.get(source, 0).javaClass
        val newArray = java.lang.reflect.Array.newInstance(wrapperType, length) as Array<Any?>
        for (i in 0 until length) {
            newArray[i] = java.lang.reflect.Array.get(source, i)
        }
        return newArray
    }
    //---------------------------------------------------------------------
    // Convenience methods for content-based equality/hash-code handling
    //---------------------------------------------------------------------
    /**
     * Determine if the given objects are equal, returning `true` if
     * both are `null` or `false` if only one is `null`.
     *
     * Compares arrays with `Arrays.equals`, performing an equality
     * check based on the array elements rather than the array reference.
     *
     * @param o1 first Object to compare
     * @param o2 second Object to compare
     * @return whether the given objects are equal
     * @see Object.equals
     * @see java.util.Arrays.equals
     */

    fun nullSafeEquals(o1: Any?, o2: Any?): Boolean {
        if (o1 === o2) {
            return true
        }
        if (o1 == null || o2 == null) {
            return false
        }
        if (o1 == o2) {
            return true
        }
        return if (o1.javaClass.isArray && o2.javaClass.isArray) {
            arrayEquals(o1, o2)
        } else false
    }

    /**
     * Compare the given arrays with `Arrays.equals`, performing an equality
     * check based on the array elements rather than the array reference.
     *
     * @param o1 first array to compare
     * @param o2 second array to compare
     * @return whether the given objects are equal
     * @see .nullSafeEquals
     * @see java.util.Arrays.equals
     */
    private fun arrayEquals(o1: Any, o2: Any): Boolean {
        if (o1 is Array<*> && o1.isArrayOf<Any>() && o2 is Array<*> && o2.isArrayOf<Any>()) {
            return o1.contentEquals(o2)
        }
        if (o1 is BooleanArray && o2 is BooleanArray) {
            return o1.contentEquals(o2)
        }
        if (o1 is ByteArray && o2 is ByteArray) {
            return o1.contentEquals(o2)
        }
        if (o1 is CharArray && o2 is CharArray) {
            return o1.contentEquals(o2)
        }
        if (o1 is DoubleArray && o2 is DoubleArray) {
            return o1.contentEquals(o2)
        }
        if (o1 is FloatArray && o2 is FloatArray) {
            return o1.contentEquals(o2)
        }
        if (o1 is IntArray && o2 is IntArray) {
            return o1.contentEquals(o2)
        }
        if (o1 is LongArray && o2 is LongArray) {
            return o1.contentEquals(o2)
        }
        return if (o1 is ShortArray && o2 is ShortArray) {
            o1.contentEquals(o2)
        } else false
    }

    /**
     * Return as hash code for the given object; typically the value of
     * `Object#hashCode()`}. If the object is an array,
     * this method will delegate to any of the `nullSafeHashCode`
     * methods for arrays in this class. If the object is `null`,
     * this method returns 0.
     *
     * @see Object.hashCode
     * @see .nullSafeHashCode
     * @see .nullSafeHashCode
     * @see .nullSafeHashCode
     * @see .nullSafeHashCode
     * @see .nullSafeHashCode
     * @see .nullSafeHashCode
     * @see .nullSafeHashCode
     * @see .nullSafeHashCode
     * @see .nullSafeHashCode
     */
    fun nullSafeHashCode(obj: Any?): Int {
        if (obj == null) {
            return 0
        }
        if (obj.javaClass.isArray) {
            if (obj is Array<*> && obj.isArrayOf<Any>()) {
                return nullSafeHashCode(obj)
            }
            if (obj is BooleanArray) {
                return nullSafeHashCode(obj)
            }
            if (obj is ByteArray) {
                return nullSafeHashCode(obj)
            }
            if (obj is CharArray) {
                return nullSafeHashCode(obj)
            }
            if (obj is DoubleArray) {
                return nullSafeHashCode(obj)
            }
            if (obj is FloatArray) {
                return nullSafeHashCode(obj)
            }
            if (obj is IntArray) {
                return nullSafeHashCode(obj)
            }
            if (obj is LongArray) {
                return nullSafeHashCode(obj)
            }
            if (obj is ShortArray) {
                return nullSafeHashCode(obj)
            }
        }
        return obj.hashCode()
    }

    /**
     * Return a hash code based on the contents of the specified array.
     * If `array` is `null`, this method returns 0.
     */
    fun nullSafeHashCode(array: Array<Any?>?): Int {
        if (array == null) {
            return 0
        }
        var hash = INITIAL_HASH
        for (element in array) {
            hash = MULTIPLIER * hash + nullSafeHashCode(element)
        }
        return hash
    }

    /**
     * Return a hash code based on the contents of the specified array.
     * If `array` is `null`, this method returns 0.
     */
    fun nullSafeHashCode(array: BooleanArray?): Int {
        if (array == null) {
            return 0
        }
        var hash = INITIAL_HASH
        for (element in array) {
            hash = MULTIPLIER * hash + java.lang.Boolean.hashCode(element)
        }
        return hash
    }

    /**
     * Return a hash code based on the contents of the specified array.
     * If `array` is `null`, this method returns 0.
     */
    fun nullSafeHashCode(array: ByteArray?): Int {
        if (array == null) {
            return 0
        }
        var hash = INITIAL_HASH
        for (element in array) {
            hash = MULTIPLIER * hash + element
        }
        return hash
    }

    /**
     * Return a hash code based on the contents of the specified array.
     * If `array` is `null`, this method returns 0.
     */
    fun nullSafeHashCode(array: CharArray?): Int {
        if (array == null) {
            return 0
        }
        var hash = INITIAL_HASH
        for (element in array) {
            hash = MULTIPLIER * hash + element.code
        }
        return hash
    }

    /**
     * Return a hash code based on the contents of the specified array.
     * If `array` is `null`, this method returns 0.
     */
    fun nullSafeHashCode(array: DoubleArray?): Int {
        if (array == null) {
            return 0
        }
        var hash = INITIAL_HASH
        for (element in array) {
            hash = MULTIPLIER * hash + java.lang.Double.hashCode(element)
        }
        return hash
    }

    /**
     * Return a hash code based on the contents of the specified array.
     * If `array` is `null`, this method returns 0.
     */
    fun nullSafeHashCode(array: FloatArray?): Int {
        if (array == null) {
            return 0
        }
        var hash = INITIAL_HASH
        for (element in array) {
            hash = MULTIPLIER * hash + java.lang.Float.hashCode(element)
        }
        return hash
    }

    /**
     * Return a hash code based on the contents of the specified array.
     * If `array` is `null`, this method returns 0.
     */
    fun nullSafeHashCode(array: IntArray?): Int {
        if (array == null) {
            return 0
        }
        var hash = INITIAL_HASH
        for (element in array) {
            hash = MULTIPLIER * hash + element
        }
        return hash
    }

    /**
     * Return a hash code based on the contents of the specified array.
     * If `array` is `null`, this method returns 0.
     */
    fun nullSafeHashCode(array: LongArray?): Int {
        if (array == null) {
            return 0
        }
        var hash = INITIAL_HASH
        for (element in array) {
            hash = MULTIPLIER * hash + java.lang.Long.hashCode(element)
        }
        return hash
    }

    /**
     * Return a hash code based on the contents of the specified array.
     * If `array` is `null`, this method returns 0.
     */
    fun nullSafeHashCode(array: ShortArray?): Int {
        if (array == null) {
            return 0
        }
        var hash = INITIAL_HASH
        for (element in array) {
            hash = MULTIPLIER * hash + element
        }
        return hash
    }
    //---------------------------------------------------------------------
    // Convenience methods for toString output
    //---------------------------------------------------------------------
    /**
     * Return a String representation of an object's overall identity.
     *
     * @param obj the object (maybe `null`)
     * @return the object's identity as String representation,
     * or an empty String if the object was `null`
     */
    fun identityToString(obj: Any?): String {
        return if (obj == null) {
            EMPTY_STRING
        } else obj.javaClass.getName() + "@" + getIdentityHexString(obj)
    }

    /**
     * Return a hex String form of an object's identity hash code.
     *
     * @param obj the object
     * @return the object's identity code in hex notation
     */
    fun getIdentityHexString(obj: Any?): String {
        return Integer.toHexString(System.identityHashCode(obj))
    }

    /**
     * Return a content-based String representation if `obj` is
     * not `null`; otherwise returns an empty String.
     *
     * Differs from [.nullSafeToString] in that it returns
     * an empty String rather than "null" for a `null` value.
     *
     * @param obj the object to build a display String for
     * @return a display String representation of `obj`
     * @see .nullSafeToString
     */
    fun getDisplayString(obj: Any?): String? {
        return if (obj == null) {
            EMPTY_STRING
        } else nullSafeToString(obj)
    }

    /**
     * Determine the class name for the given object.
     *
     * Returns a `"null"` String if `obj` is `null`.
     *
     * @param obj the object to introspect (maybe `null`)
     * @return the corresponding class name
     */
    fun nullSafeClassName(obj: Any?): String {
        return if (obj != null) obj.javaClass.getName() else NULL_STRING
    }

    /**
     * Return a String representation of the specified Object.
     *
     * Builds a String representation of the contents in case of an array.
     * Returns a `"null"` String if `obj` is `null`.
     *
     * @param obj the object to build a String representation for
     * @return a String representation of `obj`
     */
    fun nullSafeToString(obj: Any?): String? {
        if (obj == null) {
            return NULL_STRING
        }
        if (obj is String) {
            return obj
        }
        if (obj is Array<*> && obj.isArrayOf<Any>()) {
            return nullSafeToString(obj)
        }
        if (obj is BooleanArray) {
            return nullSafeToString(obj)
        }
        if (obj is ByteArray) {
            return nullSafeToString(obj)
        }
        if (obj is CharArray) {
            return nullSafeToString(obj)
        }
        if (obj is DoubleArray) {
            return nullSafeToString(obj)
        }
        if (obj is FloatArray) {
            return nullSafeToString(obj)
        }
        if (obj is IntArray) {
            return nullSafeToString(obj)
        }
        if (obj is LongArray) {
            return nullSafeToString(obj)
        }
        if (obj is ShortArray) {
            return nullSafeToString(obj)
        }
        return obj.toString()
    }

    /**
     * Return a String representation of the contents of the specified array.
     *
     * The String representation consists of a list of the array's elements,
     * enclosed in curly braces (`"{}"`). Adjacent elements are separated
     * by the characters `", "` (a comma followed by a space).
     * Returns a `"null"` String if `array` is `null`.
     *
     * @param array the array to build a String representation for
     * @return a String representation of `array`
     */
    fun nullSafeToString(array: Array<Any>?): String {
        if (array == null) {
            return NULL_STRING
        }
        val length = array.size
        if (length == 0) {
            return EMPTY_ARRAY
        }
        val stringJoiner = StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END)
        for (o in array) {
            stringJoiner.add(o.toString())
        }
        return stringJoiner.toString()
    }

    /**
     * Return a String representation of the contents of the specified array.
     *
     * The String representation consists of a list of the array's elements,
     * enclosed in curly braces (`"{}"`). Adjacent elements are separated
     * by the characters `", "` (a comma followed by a space).
     * Returns a `"null"` String if `array` is `null`.
     *
     * @param array the array to build a String representation for
     * @return a String representation of `array`
     */
    fun nullSafeToString(array: BooleanArray?): String {
        return nullSafeToString(array as Array<Any>?)
    }

    /**
     * Return a String representation of the contents of the specified array.
     *
     * The String representation consists of a list of the array's elements,
     * enclosed in curly braces (`"{}"`). Adjacent elements are separated
     * by the characters `", "` (a comma followed by a space).
     * Returns a `"null"` String if `array` is `null`.
     *
     * @param array the array to build a String representation for
     * @return a String representation of `array`
     */
    fun nullSafeToString(array: ByteArray?): String {
        return nullSafeToString(array as Array<Any>?)
    }

    /**
     * Return a String representation of the contents of the specified array.
     *
     * The String representation consists of a list of the array's elements,
     * enclosed in curly braces (`"{}"`). Adjacent elements are separated
     * by the characters `", "` (a comma followed by a space).
     * Returns a `"null"` String if `array` is `null`.
     *
     * @param array the array to build a String representation for
     * @return a String representation of `array`
     */
    fun nullSafeToString(array: CharArray?): String {
        if (array == null) {
            return NULL_STRING
        }
        val length = array.size
        if (length == 0) {
            return EMPTY_ARRAY
        }
        val stringJoiner = StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END)
        for (c in array) {
            stringJoiner.add("'$c'")
        }
        return stringJoiner.toString()
    }

    /**
     * Return a String representation of the contents of the specified array.
     *
     * The String representation consists of a list of the array's elements,
     * enclosed in curly braces (`"{}"`). Adjacent elements are separated
     * by the characters `", "` (a comma followed by a space).
     * Returns a `"null"` String if `array` is `null`.
     *
     * @param array the array to build a String representation for
     * @return a String representation of `array`
     */
    fun nullSafeToString(array: DoubleArray?): String {
        return nullSafeToString(array as Array<Any>?)
    }

    /**
     * Return a String representation of the contents of the specified array.
     *
     * The String representation consists of a list of the array's elements,
     * enclosed in curly braces (`"{}"`). Adjacent elements are separated
     * by the characters `", "` (a comma followed by a space).
     * Returns a `"null"` String if `array` is `null`.
     *
     * @param array the array to build a String representation for
     * @return a String representation of `array`
     */
    fun nullSafeToString(array: FloatArray?): String {
        return nullSafeToString(array as Array<Any>?)
    }

    /**
     * Return a String representation of the contents of the specified array.
     *
     * The String representation consists of a list of the array's elements,
     * enclosed in curly braces (`"{}"`). Adjacent elements are separated
     * by the characters `", "` (a comma followed by a space).
     * Returns a `"null"` String if `array` is `null`.
     *
     * @param array the array to build a String representation for
     * @return a String representation of `array`
     */
    fun nullSafeToString(array: IntArray?): String {
        return nullSafeToString(array as Array<Any>?)
    }

    /**
     * Return a String representation of the contents of the specified array.
     *
     * The String representation consists of a list of the array's elements,
     * enclosed in curly braces (`"{}"`). Adjacent elements are separated
     * by the characters `", "` (a comma followed by a space).
     * Returns a `"null"` String if `array` is `null`.
     *
     * @param array the array to build a String representation for
     * @return a String representation of `array`
     */
    fun nullSafeToString(array: LongArray?): String {
        return nullSafeToString(array as Array<Any>?)
    }

    /**
     * Return a String representation of the contents of the specified array.
     *
     * The String representation consists of a list of the array's elements,
     * enclosed in curly braces (`"{}"`). Adjacent elements are separated
     * by the characters `", "` (a comma followed by a space).
     * Returns a `"null"` String if `array` is `null`.
     *
     * @param array the array to build a String representation for
     * @return a String representation of `array`
     */
    fun nullSafeToString(array: ShortArray?): String {
        return nullSafeToString(array as Array<Any>?)
    }
}
