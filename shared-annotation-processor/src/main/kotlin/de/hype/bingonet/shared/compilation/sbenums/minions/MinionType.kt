package de.hype.bingonet.shared.compilation.sbenums.minions

import io.github.moulberry.repo.data.NEUItem

class MinionType(
    val typeId: String,
    val category: MinionCategory,
    val drops: Map<NEUItem, Double>,
    val requiredActions: Int
)

enum class MinionCategory {
    MINING,
    FARMING,
    COMBAT,
    FORAGING,
    FISHING,
    OTHER,
}