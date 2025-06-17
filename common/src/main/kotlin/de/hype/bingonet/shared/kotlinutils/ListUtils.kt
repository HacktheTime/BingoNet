package de.hype.bingonet.shared.kotlinutils

fun <T> List<T>.skip(int: Int): List<T> {
    return this.subList(int, this.size)
}