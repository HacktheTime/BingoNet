package de.hype.bingonet.shared.constants

/**
 * chchest Items. used to create custom ones which aren't in the [default list][ChChestItems]
 */
class ChChestItem {
    var displayName: String
        private set
    val displayPath: String?
    val isCustom: Boolean

    constructor(displayName: String, displayPath: String?) {
        this.displayName = displayName
        this.displayPath = displayPath
        this.isCustom = false
    }

    constructor(displayName: String, custom: Boolean) {
        this.displayName = displayName
        this.isCustom = custom
        displayPath = null
    }

    fun setDisplayName(displayName: String): ChChestItem {
        this.displayName = displayName
        return this
    }

    override fun toString(): String {
        return displayName
    }

    val isGemstone: Boolean
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

    fun hasDisplayPath(): Boolean {
        return displayPath != null
    }

    val isPowder: Boolean
        get() = displayName.matches(".*Powder".toRegex())
}