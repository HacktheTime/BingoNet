package de.hype.bingonet.shared.constants

/**
 * chchest Items. used to create custom ones which aren't in the [default list][ChChestItems]
 */
class ChChestItem(
    val displayName: String,
    val itemFormatting: Formatting,
    val countFormatting: Formatting,
) {

    override fun toString(): String {
        return displayName
    }

    val isFlawlessGemstone: Boolean
        get() = displayName.startsWith("Flawless") && displayName.endsWith("Gemstone")

    val isRoboPart: Boolean
        get() {
            val roboParts = arrayOf<String?>(
                "Control Switch",
                "Electron Transmitter",
                "FTX 3070",
                "Robotron Reflector",
                "Superlite Motor",
                "Synthetic Heart"
            )
            for (roboPart in roboParts) {
                if (displayName == roboPart) return true
            }
            return false
        }

    val isPowder: Boolean
        get() = displayName.matches(".*Powder".toRegex())


    val asValueableItem: ValueableChChestItem by lazy {
        ValueableChChestItem.get(displayName)
    }

    companion object {
        var mc: String = Formatting.MC_FORMAT_REGEX
        var itemRegex = Regex("^$mc\\s+($mc)+(.\\s)?([\\w\\s]+?)(\\s$mc${mc}x([\\d,]{1,5}))?$mc")
        fun parse(message: String): Pair<ChChestItem, IntRange>? {
            val results = itemRegex.find(message)?.groups
            if (results == null || results.size < 6) return null
            val itemCountString = results[5]?.value?.split("-")
            val itemCount: IntRange = if (itemCountString == null) 1..1 else if (itemCountString.size == 2) IntRange(
                itemCountString[0].toInt(),
                itemCountString[1].toInt()
            ) else IntRange(itemCountString[0].toInt(), itemCountString[0].toInt())
            return Pair(
                ChChestItem(
                    results.get(3)?.value!!,
                    results[1]?.value!!
                ), itemCount
            )
        }
    }
}