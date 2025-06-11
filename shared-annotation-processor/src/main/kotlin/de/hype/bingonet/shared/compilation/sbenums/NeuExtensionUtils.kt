package de.hype.bingonet.shared.compilation.sbenums

import io.github.moulberry.repo.data.NEUCraftingRecipe
import io.github.moulberry.repo.data.NEUIngredient
import io.github.moulberry.repo.data.NEUItem
import io.github.moulberry.repo.data.NEURecipe

fun NEUItem.getAsIngredient(amount: Int): NEUIngredient {
    return NEUIngredient.fromItem(this, amount)
}

fun NEUItem.getCompactTooItem(): CompactInfo? {
    val new = NeuRepoManager.items[this.compactsTooItemId]
    if (new == null) return null
    var requiredAmount: Double? = null
    var output: Double? = null
    for (recipe in new.recipes) {
        if (recipe !is NEUCraftingRecipe) continue
        val grouped = recipe.groupByItemId()
        if (grouped.size != 1) {
            val thisEntry = grouped.get(this)
            if (thisEntry != null) {
                requiredAmount = thisEntry
                output = recipe.output.amount
            }
        }
    }
    if (requiredAmount == null || output == null) {
        throw IllegalStateException("No matching recipe found for item ${this.skyblockItemId} in compact too item ${new.skyblockItemId}.")
    }
    return CompactInfo(
        oldItem = this,
        requiredAmount = requiredAmount.toInt(),
        newItem = new,
        newAmount = output.toInt(),
    )
}

fun NEUItem.getSuperCompactsTooItem(): CompactInfo? {
    val new = NeuRepoManager.items[this.superCompactsTooItemId]
    if (new == null) return null
    var requiredAmount: Double? = null
    var output: Double? = null
    for (recipe in new.recipes) {
        if (recipe !is NEUCraftingRecipe) continue
        val grouped = recipe.groupByItemId()
        if (grouped.size != 1) {
            val thisEntry = grouped.get(this)
            if (thisEntry != null) {
                requiredAmount = thisEntry
                output = recipe.output.amount
            }
        }
    }
    if (requiredAmount == null || output == null) {
        throw IllegalStateException("No matching recipe found for item ${this.skyblockItemId} in super compact too item ${new.skyblockItemId}.")
    }
    return CompactInfo(
        oldItem = this,
        requiredAmount = requiredAmount.toInt(),
        newItem = new,
        newAmount = output.toInt(),
    )
}


data class CompactInfo(
    val oldItem: NEUItem,
    val requiredAmount: Int,
    val newItem: NEUItem,
    val newAmount: Int,
)

fun NEUIngredient.recursiveUnenchanted(): NEUIngredient {
    var currentIngredient = this
    while (currentIngredient.itemId.contains("ENCHANTED", ignoreCase = true)) {
        currentIngredient = currentIngredient.toUnenchanted()
    }
    return currentIngredient
}

fun NEUIngredient.toUnenchanted(): NEUIngredient {
    if (!this.itemId.contains("ENCHANTED", ignoreCase = true)) return this
    val recipe =
        this.asItem().recipes.firstOrNull() ?: throw IllegalArgumentException("No recipe found for item ${this.itemId}")
    val output = recipe.allOutputs
    if (output.size != 1) {
        throw IllegalArgumentException("Recipe for item ${this.itemId} has multiple outputs: $output")
    }
    val outputCount = output.first().amount

    var groupedInputs: Map<NEUItem, Double> = recipe.groupByItemId()
    if (this.itemId.contains("ENCHANTED", ignoreCase = true)) {
        if (groupedInputs.size > 1) {
            groupedInputs = groupedInputs.filterKeys { it.skyblockItemId.contains("ENCHANTED") }
        }
    }
    if (groupedInputs.size != 1) {
        throw IllegalArgumentException("Recipe for item ${this.itemId} has multiple input types: $groupedInputs")
    }
    val groupResult = groupedInputs.entries.first()
    val inputItem = groupResult.key
    val inputCount = groupResult.value / outputCount
    return inputItem.getAsIngredient(inputCount.toInt())
}

fun NEUIngredient.asItem(): NEUItem {
    return NeuRepoManager.items[this.itemId]
        ?: throw IllegalArgumentException("Item with ID ${this.itemId} not found in NEU repository.")
}

fun NEURecipe.groupByItemId(): Map<NEUItem, Double> {
    return this.allInputs.filter { it.itemId != "NEU_SENTINEL_EMPTY" && it.itemId != "SKYBLOCK_COIN" }
        .groupBy { it.asItem() }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
}