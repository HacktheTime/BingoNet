package de.hype.bingonet.shared.compilation.extensionutils

fun <T : Comparable<T>> T.min(other: T): T {
    return if (this < other) this else other
}

fun <T : Comparable<T>> T.max(other: T): T {
    return if (this > other) this else other
}