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

fun <T : Comparable<T>> T.min(other: T): T {
    return if (this < other) this else other
}

fun <T : Comparable<T>> T.max(other: T): T {
    return if (this > other) this else other
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

