package de.hype.bingonet.shared.compilation

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.WildcardTypeName
import java.util.*

object MainClassRegistry {
    private const val CORE_CLASS = "de.hype.bingonet.server.Core"
    private const val BUTTON_IMPLEMENTATION_CLASS =
        "de.hype.bingonet.server.discord.events.staticimplementations.abstractcore.StaticAbstractButtonInteractionImplementation"
    private val BUTTON_INSTANCE_CLASS: String = "$BUTTON_IMPLEMENTATION_CLASS.ButtonInstance"
    private const val BUTTON_OPTION_PARSER_CLASS = "de.hype.bingonet.server.discord.ButtonOptionParser"
    val ButtonInstance: ClassName = getClassName(BUTTON_INSTANCE_CLASS, 3)
    val ButtonOptionParser: ClassName = getClassName(BUTTON_OPTION_PARSER_CLASS, 2)
    val AbstractButtonImplementation: ClassName = getClassName(BUTTON_IMPLEMENTATION_CLASS, 2)
    val Core: ClassName = getClassName(CORE_CLASS, 2)
    val SingleCommandRegistration: ClassName = getClassName(
        "de.hype.bingonet.server.discord.events.staticimplementations.commands.AbstractSingleCommand.SingleCommandRegistration",
        3
    )
    val SubCommandRegistration: ClassName = getClassName(
        "de.hype.bingonet.server.discord.events.staticimplementations.commands.AbstractSubCommand.SubCommandRegistration",
        3
    )
    val AbstractCommandOption: ClassName =
        getClassName("de.hype.bingonet.server.discord.events.staticimplementations.commands.AbstractCommandOption", 2)
    val AnyAbstractCommandOption: ParameterizedTypeName = ParameterizedTypeName.get(
        AbstractCommandOption, WildcardTypeName.subtypeOf(
            Any::class.java
        )
    )
    val AbstractSingleCommand: ClassName =
        getClassName("de.hype.bingonet.server.discord.events.staticimplementations.commands.AbstractSingleCommand", 2)
    val AbstractDiscordCommand: ClassName =
        getClassName("de.hype.bingonet.server.discord.events.staticimplementations.commands.AbstractDiscordCommand", 2)
    val IDiscordEventExecutorProvider: ClassName =
        getClassName("de.hype.bingonet.server.discord.events.registry.IDiscordEventExecutorProvider", 2)
    val IDiscordSuggestionProvider: ClassName = getClassName(
        "de.hype.bingonet.server.discord.events.staticimplementations.commands.IDiscordSuggestionProvider",
        2
    )
    val AbstractSubCommand: ClassName =
        getClassName("de.hype.bingonet.server.discord.events.staticimplementations.commands.AbstractSubCommand", 2)
    val CommandGroupRegistration: ClassName =
        getClassName("de.hype.bingonet.server.discord.events.registry.CommandGroupRegistration", 2)

    @JvmField
    val Modal_Component: ClassName = getClassName(
        "de.hype.bingonet.server.discord.events.dynamicimplementations.abstractcore.AbstractModal.ModalComponent",
        3
    )
    val UserContextRegistry: ClassName =
        getClassName("de.hype.bingonet.server.discord.events.registry.UserContextRegistry", 2)
    val UserContextImpl: ClassName = getClassName(
        "de.hype.bingonet.server.discord.events.staticimplementations.abstractcore.AbstractUserContextInteractionImplementation",
        2
    )
    val MessageContextRegistry: ClassName =
        getClassName("de.hype.bingonet.server.discord.events.registry.MessageContextRegistry", 2)
    val MessageContextImpl: ClassName = getClassName(
        "de.hype.bingonet.server.discord.events.staticimplementations.abstractcore.AbstractMessageContextInteractionImplementation",
        2
    )

    fun getClassName(full: String, argCount: Int): ClassName {
        try {
            var classNames = arrayOfNulls<String>(argCount)
            var index = full.length
            for (i in 0..<argCount - 1) {
                classNames[i] = full.substring(full.lastIndexOf('.', index - 1) + 1, index)
                index = full.lastIndexOf('.', index - 1)
            }
            classNames[argCount - 1] = full.substring(0, index)
            classNames = classNames.reversedArray()
            if (argCount == 2) {
                return ClassName.get(classNames[0], classNames[1])
            }
            return ClassName.get(classNames[0], classNames[1], *Arrays.copyOfRange<String>(classNames, 2, argCount))
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
