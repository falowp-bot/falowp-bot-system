package com.blr19c.falowp.bot.system.utils

import com.blr19c.falowp.bot.system.utils.ObjectUtils.isEmpty
import com.blr19c.falowp.bot.system.utils.ObjectUtils.nullSafeEquals
import com.blr19c.falowp.bot.system.utils.ObjectUtils.toObjectArray
import java.util.*
import kotlin.math.ceil

/**
 * util来源于spring
 */
@Suppress("UNCHECKED_CAST", "MemberVisibilityCanBePrivate", "UNUSED")
object CollectionUtils {
    /**
     * Default load factor for [HashMap]/[LinkedHashMap] variants.
     *
     * @see .newHashMap
     * @see .newLinkedHashMap
     */
    const val DEFAULT_LOAD_FACTOR = 0.75f

    /**
     * Return `true` if the supplied Collection is `null` or empty.
     * Otherwise, return `false`.
     *
     * @param collection the Collection to check
     * @return whether the given Collection is empty
     */
    fun isEmpty(collection: Collection<*>?): Boolean {
        return collection == null || collection.isEmpty()
    }

    /**
     * Return `true` if the supplied Map is `null` or empty.
     * Otherwise, return `false`.
     *
     * @param map the Map to check
     * @return whether the given Map is empty
     */
    fun isEmpty(map: Map<*, *>?): Boolean {
        return map == null || map.isEmpty()
    }

    /**
     * Instantiate a new [HashMap] with an initial capacity
     * that can accommodate the specified number of elements without
     * any immediate resize/rehash operations to be expected.
     *
     * This differs from the regular [HashMap] constructor
     * which takes an initial capacity relative to a load factor
     * but is effectively aligned with the JDK's
     * [java.util.concurrent.ConcurrentHashMap].
     *
     * @param expectedSize the expected number of elements (with a corresponding
     * capacity to be derived so that no resize/rehash operations are needed)
     * @see .newLinkedHashMap
     * @since 5.3
     */
    fun <K, V> newHashMap(expectedSize: Int): HashMap<K, V> {
        return HashMap(computeMapInitialCapacity(expectedSize), DEFAULT_LOAD_FACTOR)
    }

    fun <K, V> newLinkedHashMap(expectedSize: Int): LinkedHashMap<K, V> {
        return LinkedHashMap(computeMapInitialCapacity(expectedSize), DEFAULT_LOAD_FACTOR)
    }

    private fun computeMapInitialCapacity(expectedSize: Int): Int {
        return ceil(expectedSize / DEFAULT_LOAD_FACTOR.toDouble()).toInt()
    }

    /**
     * Convert the supplied array into a List. A primitive array gets converted
     * into a List of the appropriate wrapper type.
     *
     * **NOTE:** Generally prefer the standard [Arrays.asList] method.
     * This `arrayToList` method is just meant to deal with an incoming Object
     * value that might be an `Object[]` or a primitive array at runtime.
     *
     * A `null` source value will be converted to an empty List.
     *
     * @param source the (potentially primitive) array
     * @return the converted List result
     * @see ObjectUtils.toObjectArray
     * @see Arrays.asList
     */
    fun arrayToList(source: Any?): List<*> {
        return listOf(*toObjectArray(source))
    }

    /**
     * Merge the given array into the given Collection.
     *
     * @param array      the array to merge (maybe `null`)
     * @param collection the target Collection to merge the array into
     */
    fun <E> mergeArrayIntoCollection(array: Any?, collection: MutableCollection<E?>) {
        val arr = toObjectArray(array)
        Collections.addAll(collection, *arr as Array<E?>)
    }

    /**
     * Merge the given Properties instance into the given Map,
     * copying all properties (key-value pairs) over.
     *
     * Uses `Properties.propertyNames()` to even catch
     * default properties linked into the original Properties instance.
     *
     * @param props the Properties instance to merge (maybe `null`)
     * @param map   the target Map to merge the properties into
     */
    fun <K, V> mergePropertiesIntoMap(props: Properties?, map: MutableMap<K, V?>) {
        if (props != null) {
            val en = props.propertyNames()
            while (en.hasMoreElements()) {
                val key = en.nextElement() as String
                var value = props[key]
                if (value == null) {
                    // Allow for defaults fallback or potentially overridden accessor...
                    value = props.getProperty(key)
                }
                map[key as K] = value as V?
            }
        }
    }

    /**
     * Check whether the given Iterator contains the given element.
     *
     * @param iterator the Iterator to check
     * @param element  the element to look for
     * @return `true` if found, `false` otherwise
     */
    fun contains(iterator: Iterator<*>?, element: Any?): Boolean {
        if (iterator != null) {
            while (iterator.hasNext()) {
                val candidate = iterator.next()!!
                if (nullSafeEquals(candidate, element)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Check whether the given Enumeration contains the given element.
     *
     * @param enumeration the Enumeration to check
     * @param element     the element to look for
     * @return `true` if found, `false` otherwise
     */
    fun contains(enumeration: Enumeration<*>?, element: Any?): Boolean {
        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                val candidate = enumeration.nextElement()
                if (nullSafeEquals(candidate, element)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Check whether the given Collection contains the given element instance.
     *
     * Enforces the given instance to be present, rather than returning
     * `true` for an equal element as well.
     *
     * @param collection the Collection to check
     * @param element    the element to look for
     * @return `true` if found, `false` otherwise
     */
    fun containsInstance(collection: Collection<*>?, element: Any): Boolean {
        if (collection != null) {
            for (candidate in collection) {
                if (candidate === element) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Return `true` if any element in '`candidates`' is
     * contained in '`source`'; otherwise returns `false`.
     *
     * @param source     the source Collection
     * @param candidates the candidates to search for
     * @return whether any of the candidates has been found
     */
    fun containsAny(source: Collection<*>, candidates: Collection<*>): Boolean {
        return findFirstMatch(source, candidates) != null
    }

    /**
     * Return the first element in '`candidates`' that is contained in
     * '`source`'. If no element in '`candidates`' is present in
     * '`source`' returns `null`. Iteration order is
     * [Collection] implementation specific.
     *
     * @param source     the source Collection
     * @param candidates the candidates to search for
     * @return the first present object, or `null` if not found
     */
    fun <E> findFirstMatch(source: Collection<*>, candidates: Collection<E>): E? {
        if (isEmpty(source) || isEmpty(candidates)) {
            return null
        }
        for (candidate in candidates) {
            if (source.contains(candidate)) {
                return candidate
            }
        }
        return null
    }

    /**
     * Find a single value of the given type in the given Collection.
     *
     * @param collection the Collection to search
     * @param type       the type to look for
     * @return a value of the given type found if there is a clear match,
     * or `null` if none or more than one such value found
     */
    fun <T> findValueOfType(collection: Collection<*>, type: Class<T>?): T? {
        if (isEmpty(collection)) {
            return null
        }
        var value: T? = null
        for (element in collection) {
            if (type == null || type.isInstance(element)) {
                if (value != null) {
                    // More than one value found... no clear single value.
                    return null
                }
                value = element as T
            }
        }
        return value
    }

    /**
     * Find a single value of one of the given types in the given Collection:
     * searching the Collection for a value of the first type, then
     * searching for a value of the second type, etc.
     *
     * @param collection the collection to search
     * @param types      the types to look for, in prioritized order
     * @return a value of one of the given types found if there is a clear match,
     * or `null` if none or more than one such value found
     */
    fun findValueOfType(collection: Collection<*>?, types: Array<Class<*>>): Any? {
        if (isEmpty(collection) || isEmpty(types)) {
            return null
        }
        for (type in types) {
            val value = findValueOfType(collection!!, type)
            if (value != null) {
                return value
            }
        }
        return null
    }

    /**
     * Determine whether the given Collection only contains a single unique object.
     *
     * @param collection the Collection to check
     * @return `true` if the collection contains a single reference or
     * multiple references to the same instance, `false` otherwise
     */
    fun hasUniqueObject(collection: Collection<*>): Boolean {
        if (isEmpty(collection)) {
            return false
        }
        var hasCandidate = false
        var candidate: Any? = null
        for (elem in collection) {
            if (!hasCandidate) {
                hasCandidate = true
                candidate = elem
            } else if (candidate !== elem) {
                return false
            }
        }
        return true
    }

    /**
     * Find the common element type of the given Collection, if any.
     *
     * @param collection the Collection to check
     * @return the common element type, or `null` if no clear
     * common type has been found (or the collection was empty)
     */
    fun findCommonElementType(collection: Collection<*>): Class<*>? {
        if (isEmpty(collection)) {
            return null
        }
        var candidate: Class<*>? = null
        for (`val` in collection) {
            if (`val` != null) {
                if (candidate == null) {
                    candidate = `val`.javaClass
                } else if (candidate != `val`.javaClass) {
                    return null
                }
            }
        }
        return candidate
    }

    /**
     * Retrieve the first element of the given Set, using [SortedSet.first]
     * or otherwise using the iterator.
     *
     * @param set the Set to check (maybe `null` or empty)
     * @return the first element, or `null` if none
     * @see SortedSet
     *
     * @see LinkedHashMap.keySet
     * @see java.util.LinkedHashSet
     *
     * @since 5.2.3
     */
    fun <T> firstElement(set: Set<T>): T? {
        if (isEmpty(set)) {
            return null
        }
        if (set is SortedSet<T>) {
            return set.first()
        }
        val it = set.iterator()
        var first: T? = null
        if (it.hasNext()) {
            first = it.next()
        }
        return first
    }

    /**
     * Retrieve the first element of the given List, accessing the zero index.
     *
     * @param list the List to check (maybe `null` or empty)
     * @return the first element, or `null` if none
     * @since 5.2.3
     */
    fun <T> firstElement(list: List<T>): T? {
        return if (isEmpty(list)) {
            null
        } else list[0]
    }

    /**
     * Retrieve the last element of the given Set, using [SortedSet.last]
     * or otherwise iterating over all elements (assuming a linked set).
     *
     * @param set the Set to check (maybe `null` or empty)
     * @return the last element, or `null` if none
     * @see SortedSet
     *
     * @see LinkedHashMap.keySet
     * @see java.util.LinkedHashSet
     *
     * @since 5.0.3
     */
    fun <T> lastElement(set: Set<T>): T? {
        if (isEmpty(set)) {
            return null
        }
        if (set is SortedSet<T>) {
            return set.last()
        }

        // Full iteration necessary...
        val it = set.iterator()
        var last: T? = null
        while (it.hasNext()) {
            last = it.next()
        }
        return last
    }

    /**
     * Retrieve the last element of the given List, accessing the highest index.
     *
     * @param list the List to check (maybe `null` or empty)
     * @return the last element, or `null` if none
     * @since 5.0.3
     */
    fun <T> lastElement(list: List<T>): T? {
        return if (isEmpty(list)) {
            null
        } else list[list.size - 1]
    }

    /**
     * Marshal the elements from the given enumeration into an array of the given type.
     * Enumeration elements must be assignable to the type of the given array. The array
     * returned will be a different instance than the array given.
     */
    fun <A> toArray(enumeration: Enumeration<A>, array: Array<A>): Array<A> {
        val elements = ArrayList<A>()
        while (enumeration.hasMoreElements()) {
            elements.add(enumeration.nextElement())
        }
        return elements.toArray(array)
    }

    /**
     * Adapt an [Enumeration] to an [Iterator].
     *
     * @param enumeration the original `Enumeration`
     * @return the adapted `Iterator`
     */
    fun <E> toIterator(enumeration: Enumeration<E>?): Iterator<E> {
        return if (enumeration != null) enumeration.asIterator() else Collections.emptyIterator()
    }
}
