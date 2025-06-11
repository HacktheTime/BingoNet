package de.hype.bingonet.shared.compilation.sbenums.minions

interface MinionResourceItem {
    val compactorLevel: Int
        get() = 1

    val displayName: String
    val itemId: String

    enum class UnusedMinionItems(override val displayName: String, override val itemId: String) : MinionResourceItem {
        RED_GIFT("Red Gift", "RED_GIFT"),
        LUSH_BERRIES("Lush Berrbries", "LUSH_BERRIES"),
        PURPLE_CANDY("Purple Candy", "PURPLE_CANDY"),
        EGG("Egg", "EGG"),
        CORRUPTED_FRAGMENT("Corrupted Fragment", "CORRUPTED_FRAGMENT"),
    }
}