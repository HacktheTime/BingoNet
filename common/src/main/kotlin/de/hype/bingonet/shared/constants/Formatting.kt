package de.hype.bingonet.shared.constants

enum class Formatting(val mCCode: String, @JvmField val discordFormattingCode: String) {
    BLACK("§0", "\u001b[30m"),
    DARK_BLUE("§1", "\u001b[34m"),
    DARK_GREEN("§2", "\u001b[32m"),
    DARK_AQUA("§3", "\u001b[36m"),
    DARK_RED("§4", "\u001b[31m"),
    DARK_PURPLE("§5", "\u001b[35m"),
    GOLD("§6", "\u001b[33m"),
    GRAY("§7", "\u001b[37m"),
    DARK_GRAY("§8", "\u001b[30m"),
    BLUE("§9", "\u001b[34m"),
    GREEN("§a", "\u001b[32m"),
    AQUA("§b", "\u001b[36m"),
    RED("§c", "\u001b[31m"),
    LIGHT_PURPLE("§d", "\u001b[35m"),
    YELLOW("§e", "\u001b[33m"),
    WHITE("§f", "\u001b[37m"),
    BOLD("§l", "\u001b[1m"),
    STRIKETHROUGH("§m", "\u001b[9m"),
    UNDERLINE("§n", "\u001b[4m"),
    ITALIC("§o", "\u001b[3m"),
    OBFUSCATED("§k", "\u001b[6m"),
    RESET("§r", "\u001b[0m");

    override fun toString(): String {
        return this.mCCode
    }

    /**
     * short for discordFormattingCode
     */
    fun dc(): String {
        return discordFormattingCode
    }

    companion object {
        @JvmStatic
        fun covertToDiscordAnsi(string: String): String {
            var goalString = string
            for (formatting in entries) {
                goalString = goalString.replace(formatting.mCCode.toRegex(), formatting.discordFormattingCode)
            }
            return goalString
        }
    }
}
