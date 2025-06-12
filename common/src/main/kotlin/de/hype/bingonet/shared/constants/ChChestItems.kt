package de.hype.bingonet.shared.constants

import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * Enumeration representing various ChChest items in the game.
 * These constants define specific ChChest items that players can obtain.
 * Use these constants to refer to specific ChChest items. For non-listed use the Custom Value. Example usage below.
 *
 *
 * The available ChChest items are:
 *
 *  * `PrehistoricEgg`: Represents a prehistoric egg item.
 *  * `Pickonimbus2000`: Represents the Pickonimbus 2000 item.
 *  * `ControlSwitch`: Represents a control switch item.
 *  * `ElectronTransmitter`: Represents an electron transmitter item.
 *  * `FTX3070`: Represents the FTX 3070 item.
 *  * `RobotronReflector`: Represents the Robotron Reflector item.
 *  * `SuperliteMotor`: Represents a Superlite Motor item.
 *  * `SyntheticHeart`: Represents a synthetic heart item.
 *  * `FlawlessGemstone`: Represents a flawless gemstone item.
 *  * `JungleHeart`: Represents a Jungle Heart item.
 *
 * How to create a Custom Enum:
 * <pre>
 * `new ChChestItem("(Your Item name)")`
 * Make sure too use the EXACT display name!
</pre> *
 */
object ChChestItems {
    @JvmField
    val PrehistoricEgg: ChChestItem = ChChestItem("Prehistoric Egg", "prehistoric_egg")

    @JvmField
    val Pickonimbus2000: ChChestItem = ChChestItem("Pickonimbus 2000", "pickonimbus")

    @JvmField
    val ControlSwitch: ChChestItem = ChChestItem("Control Switch", "control_switch")

    @JvmField
    val ElectronTransmitter: ChChestItem = ChChestItem("Electron Transmitter", "electron_transmitter")

    @JvmField
    val FTX3070: ChChestItem = ChChestItem("FTX 3070", "ftx_3070")

    @JvmField
    val RobotronReflector: ChChestItem = ChChestItem("Robotron Reflector", "robotron_reflector")

    @JvmField
    val SuperliteMotor: ChChestItem = ChChestItem("Superlite Motor", "superlite_motor")

    @JvmField
    val SyntheticHeart: ChChestItem = ChChestItem("Synthetic Heart", "synthetic_heart")

    @JvmField
    val FlawlessGemstone: ChChestItem = ChChestItem("Flawless Gemstone", "flawless_gemstone")
    val GEMSTONE_POWDER: ChChestItem =
        ChChestItem("1,200-4,800 ${Formatting.LIGHT_PURPLE}Gemstone Powder", "legendary_gemstone_powder")
    val MITHRIL_POWDER: ChChestItem =
        ChChestItem("1,200-4,800 ${Formatting.GREEN}Mithril Powder", "legendary_mithril_powder")

    @JvmStatic
    val allItems: MutableList<ChChestItem> = ArrayList()

    // Automatically populate predefined items using reflection
    init {
        val fields = ChChestItems::class.java.declaredFields
        for (field in fields) {
            if (field.type == ChChestItem::class.java && isPublicStaticFinal(field)) {
                try {
                    allItems.add((field.get(null) as ChChestItem))
                } catch (e: IllegalAccessException) {
                    // Handle exception
                }
            }
        }
    }

    fun getItem(displayName: String): ChChestItem {
        val existingItem = getPredefinedItem(displayName)

        if (existingItem != null) {
            return existingItem
        }

        val customItem = ChChestItem(displayName, true)
        return customItem
    }

    @JvmStatic
    fun getItems(itemInput: Array<String>): MutableList<ChChestItem> {
        val items = listOf(*itemInput)
        val allItems: MutableList<ChChestItem> = allItems
        val foundItems: MutableList<ChChestItem> = ArrayList()
        for (item in items) {
            var foundItem: ChChestItem? = null
            for (allItem in allItems) {
                if (allItem.displayName == item) {
                    foundItem = allItem
                    break
                }
            }
            requireNotNull(foundItem) { "Unknown Item: $item" }
            foundItems.add(foundItem)
        }
        return foundItems
    }

    @JvmStatic
    fun getPredefinedItem(displayName: String): ChChestItem? {
        for (item in allItems) {
            var amount = 1
            val countString = displayName.replace("\\D".toRegex(), "")
            if (!countString.isEmpty()) amount = countString.toInt()
            if (item.isPowder) {
                if (displayName.matches(".*Powder".toRegex())) {
                    if (amount >= 1200) {
                        return item
                    }
                } else continue
            }

            if (item == FlawlessGemstone) {
                if (displayName.matches(".*Flawless.*Gemstone.*".toRegex())) return item
            } else if (item.displayName.contains(displayName)) {
                return item
            }
        }
        return null
    }

    // Utility method to check if a field is public, static, and final
    private fun isPublicStaticFinal(field: Field): Boolean {
        return Modifier.isPublic(field.modifiers) &&
                Modifier.isStatic(field.modifiers) &&
                Modifier.isFinal(field.modifiers)
    }

    @JvmStatic
    fun createCustomItem(displayName: String): ChChestItem {
        val customItem = ChChestItem(displayName, true)
        allItems.add(customItem)
        return customItem
    }

    @JvmStatic
    val allItemNames: MutableList<String> by lazy {
        allItems.map(ChChestItem::displayName).toMutableList()
    }
    //very fancy way to convert a list to a list of values from the previous list
}