package de.hype.bingonet.shared.compilation.sbenums.minions

class MinionData(
    val displayName: String,
    val itemId: String,
    var timeBetweenActions: Double,
    var storage: Int,
    val simpleUpgradeCost: Int?,
) {
    val simpleDisplayName: String by lazy {
        Regex("(([a-zA-Z\\s])* Minion).*").find(displayName.replace("§.".toRegex(), ""))?.groupValues?.get(1)!!
    }
    val tier: Int = itemId.replace("\\D+".toRegex(), "").toInt()

    override fun toString(): String {
        return "$simpleDisplayName $tier"
    }

    fun copy(): MinionData {
        return MinionData(
            displayName,
            itemId,
            timeBetweenActions,
            storage,
            simpleUpgradeCost
        )
    }

}