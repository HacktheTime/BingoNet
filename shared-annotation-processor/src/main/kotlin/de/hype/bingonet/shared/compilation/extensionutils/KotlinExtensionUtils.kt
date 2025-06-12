package de.hype.bingonet.shared.compilation.extensionutils

import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty

fun <S, T> lazyRemap(
    source: () -> S,
    mapper: (S) -> T
): ReadOnlyProperty<Any?, T> =
    object : ReadOnlyProperty<Any?, T> {
        private var lastSource: S? = null
        private var cache: T? = null

        override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            val current = source()
            if (cache == null || lastSource != current) {
                cache = mapper(current)
                lastSource = current
            }
            return cache!!
        }
    }

// for `var x by lazyRemap(::sourceProp) { mapper } reverse { toSource }`
fun <R, S> lazyRemap(
    prop: KMutableProperty0<R>,
    mapper: (R) -> S,
    reverse: (S) -> R
): ReadWriteProperty<Any?, S> =
    object : ReadWriteProperty<Any?, S> {
        private var lastSource: R? = null
        private var cache: S? = null

        override operator fun getValue(thisRef: Any?, property: KProperty<*>): S {
            val current = prop.get()
            if (cache == null || lastSource != current) {
                cache = mapper(current)
                lastSource = current
            }
            return cache!!
        }

        override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: S) {
            cache = value
            val newSource = reverse(value)
            lastSource = newSource
            prop.set(newSource)
        }
    }

fun <T : List<V>, V> T.subList(from: Int): List<V> {
    if (from > size) return listOf()
    return subList(from, size)
}

fun <T : List<V>, V> T.subListTo(to: Int): List<V> {
    val to = to.coerceAtMost(size)
    if (to <= 0) return listOf()
    return subList(0, to)
}