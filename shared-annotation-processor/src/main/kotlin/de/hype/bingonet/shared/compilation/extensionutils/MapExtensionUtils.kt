package de.hype.bingonet.shared.compilation.extensionutils

fun <K, V> MutableMap<K, V>.modifyValues(transform: (Map.Entry<K, V>) -> V) {
    return this.entries.forEach { it.setValue(transform(it)) }
}

fun <K, V> MutableMap<K, V>.modifyKeys(transform: (Map.Entry<K, V>) -> K) {
    entries.forEach {
        val newKey = transform(it)
        if (newKey == it.key) return@forEach // No change in key, skip
        this.remove(it.key)
        this[newKey] = it.value
    }
}