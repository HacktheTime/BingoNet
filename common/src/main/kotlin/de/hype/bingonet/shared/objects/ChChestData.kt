package de.hype.bingonet.shared.objects

import de.hype.bingonet.shared.constants.ChChestItem

open class ChChestData(
    open var finder: String,
    @JvmField var coords: Position,
    @JvmField var items: List<ChChestItem>
) {
    override fun equals(other: Any?): Boolean {
        if (other !is ChChestData) return false
        return other.coords == coords
    }

    override fun hashCode(): Int {
        return coords.hashCode()
    }
}
