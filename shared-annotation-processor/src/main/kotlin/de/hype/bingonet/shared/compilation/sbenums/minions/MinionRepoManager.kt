package de.hype.bingonet.shared.compilation.sbenums.minions

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import de.hype.bingonet.shared.compilation.Main.PROJECT_DIR
import de.hype.bingonet.shared.compilation.sbenums.NeuRepoManager
import de.hype.bingonet.shared.compilation.sbenums.getAsIngredient
import de.hype.bingonet.shared.compilation.sbenums.groupByItemId
import de.hype.bingonet.shared.compilation.sbenums.recursiveUnenchanted
import io.github.moulberry.repo.data.NEUCraftingRecipe
import io.github.moulberry.repo.data.NEUItem
import java.io.File

object MinionRepoManager {
    val minions: Map<String, MinionData> by lazy {
        NeuRepoManager.items.filter { it.key.matches(".*_GENERATOR_[0-9]+$".toRegex()) }
            .mapValues {
                MinionData(
                    it.value.displayName,
                    it.value.skyblockItemId,
                    it.value.lore
                        .firstOrNull { it.startsWith("§7Time Between Actions:") }!!
                        .replace("((§.)|\\D)*".toRegex(), "").toDouble(),
                    it.value.lore
                        .firstOrNull { it.startsWith("§7Max Storage:") }!!
                        .replace("((§.)|\\D)*".toRegex(), "").toInt(),
                    calculateMinionUpgradeCost(it.value)
                )
            }
    }
    val typeMappedMinions: Map<String, List<MinionData>> by lazy {
        minions.values.groupBy { it.simpleDisplayName }.mapValues { (_, list) -> list.sortedBy { it.tier } }
    }

    private fun calculateMinionUpgradeCost(data: NEUItem): Int? {
        val size = data.recipes.size
        if (size == 0) return null
        if (size >= 2) {
            throw IllegalArgumentException("Minion has more than one recipe: ${data.skyblockItemId}")
        }
        val recipies = data.recipes
        if (recipies.isEmpty()) return null
        if (recipies.size > 1) {
            throw IllegalArgumentException("Minion has more than one recipe: ${data.skyblockItemId}")
        }
        val recipeData = recipies.first()
        val items = recipeData.groupByItemId()
            .map { (item, amount) -> item.getAsIngredient(amount.toInt()).recursiveUnenchanted() }.toMutableList()
        items.removeIf { it.itemId.startsWith("WOOD_", ignoreCase = true) }
        if (items.isEmpty()) return null
        if (items.size > 1) {
            return null
        }
        return items.first().amount.toInt()
    }

    fun getMinionFromString(type: String, tier: String): Minion {
        val tierInt = when (tier) {
            "I" -> 1
            "II" -> 2
            "III" -> 3
            "IV" -> 4
            "V" -> 5
            "VI" -> 6
            "VII" -> 7
            "VIII" -> 8
            "IX" -> 9
            "X" -> 10
            "XI" -> 11
            "XII" -> 12
            else -> throw IllegalArgumentException("Invalid minion tier: $tier")
        }
        val minionType = minionTypes.get(type.uppercase())
        if (minionType == null) {
            throw IllegalArgumentException("Invalid minion type: $type")
        }
        return Minion(minionType, tierInt)
    }

    val minionTypes: MutableMap<String, MinionType> by lazy {
        val file = File(NeuRepoManager.LOCAL_PATH.toFile(), "constants/minions.json")
        val json = file.readText()
        val jsonObject = JsonParser.parseString(json).asJsonObject

        val result = mutableMapOf<String, MinionType>()
        for ((minionId, minionData) in jsonObject.entrySet()) {
            val type = MinionCategory.valueOf(minionData.asJsonObject["type"].asString)
            val dropsJson = minionData.asJsonObject["drops"]?.asJsonObject
            val drops = mutableMapOf<NEUItem, Double>()
            dropsJson?.entrySet()?.forEach { (itemId, amount) ->
                val neuItem = NeuRepoManager.items[itemId] ?: return@forEach
                drops[neuItem] = amount.asDouble
            }
            val actions = minionData.asJsonObject["requiredactions"].asInt
            result[minionId] = MinionType(minionId, type, drops, actions)
        }
        result
    }
}


fun main() {
    MinionRepoManager.typeMappedMinions
    val allowedCompactor = listOf(4, 9)
    val superCompactor = listOf(25, 20, 160, 9 * 64)
    val results: MutableMap<String, Pair<LinkedHashSet<NEUItem>, LinkedHashSet<NEUItem>>> = HashMap()
    var current = 1
    val size = NeuRepoManager.items.size
    NeuRepoManager.items.forEach { (_, original) ->
        val toCheckItems: MutableList<NEUItem> = mutableListOf(original)
        val compactorList: LinkedHashSet<NEUItem> = LinkedHashSet()
        val sc3000List: LinkedHashSet<NEUItem> = LinkedHashSet()
        var first = 0
        while (toCheckItems.isNotEmpty()) {
            val item = toCheckItems.removeFirst()
            first++
            var anyMatch = false
            NeuRepoManager.items.forEach { (_, neuItem) ->
                neuItem.recipes.forEach {
                    if (it !is NEUCraftingRecipe) return@forEach
                    val grouped = it.groupByItemId()
                    if (grouped.size != 1 || it.output.amount.toInt() != 1) return@forEach
                    val entry = grouped.entries.first()
                    if (entry.key.skyblockItemId != (item.skyblockItemId)) {
                        return@forEach
                    }
                    anyMatch = true
                    if (first == 1) {
                        if (entry.value.toInt() in allowedCompactor) {
                            compactorList.add(neuItem)
                            toCheckItems.add(neuItem)
                        } else if (entry.value.toInt() in superCompactor) {
                            sc3000List.add(neuItem)
                            toCheckItems.add(neuItem)
                        } else if (entry.value.toInt() == 5 && item.skyblockItemId.contains("ENCHANTED", true)) {
                            sc3000List.add(neuItem)
                            toCheckItems.add(neuItem)
                        }
                    }
                }
            }
            if (!anyMatch && !item.skyblockItemId.contains("ENCHANTED", true)) {
                compactorList.remove(item)
                sc3000List.remove(item)
            }
        }
        if (compactorList.isNotEmpty() || sc3000List.isNotEmpty()) {
            results.put(original.skyblockItemId, Pair(compactorList, sc3000List))
            println("[$current/$size] ${original.displayName}: ${compactorList.size} compactors, ${sc3000List.size} super compactors")
        }
        current++
    }
    results.map {
        it.key + "," + it.value.component1().joinToString { it.skyblockItemId } + "," + it.value.component2()
            .joinToString { it.skyblockItemId }
    }
    val gson = GsonBuilder().setPrettyPrinting().create()
    val itemsDir = File(PROJECT_DIR, "neu-repo/items")

    results.forEach { (itemId, pair) ->
        val file = File(itemsDir, "$itemId.json")
        if (!file.exists()) return@forEach
        val type = object : TypeToken<MutableMap<String, Any>>() {}.type
        val json = fixNumbers(gson.fromJson(file.readText(), type))
        val compact = pair.first.map { it.skyblockItemId }.firstOrNull()
        if (!compact.isNullOrEmpty()) {
            json["compactstoo"] = compact
        }
        val supercompact = pair.second.map { it.skyblockItemId }.firstOrNull()
        if (!supercompact.isNullOrEmpty()) {
            json["supercompactstoo"] = supercompact
        }
        file.writeText(gson.toJson(json))
    }
    println("Done")
}

fun fixNumbers(map: MutableMap<String, Any>): MutableMap<String, Any> {
    map.forEach { (k, v) ->
        if (v is Double && v % 1 == 0.0) {
            map[k] = v.toInt()
        }
        if (v is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            map[k] = fixNumbers(v as MutableMap<String, Any>)
        }
    }
    return map
}