package de.hype.bingonet.shared.objects

import de.hype.bingonet.shared.constants.StatusConstants
import java.awt.Color
import java.sql.SQLException
import java.time.Instant
import java.time.temporal.ChronoUnit

open class ChestLobbyData(
    chests: MutableList<ChChestData>,
    serverId: String,
    bbcommand: String,
    extraMessage: String?,
    status: String
) {
    open var contactMan: String = chests.first().finder

    open var chests: MutableList<ChChestData> = ArrayList(chests)

    @JvmField
    var bbcommand: String

    @JvmField
    var extraMessage: String?

    @Transient
    lateinit var color: Color

    @JvmField
    var lobbyId: Int = 0

    @JvmField
    var serverId: String = serverId
    lateinit var status: String
    var playersStillIn: MutableList<String>? = ArrayList()
    var closingTime: Instant = Instant.now().plus(360, ChronoUnit.MINUTES)
        protected set

    init {
        this.bbcommand = bbcommand
        this.extraMessage = extraMessage
        setStatusNoOverride(status)
    }

    constructor(
        chest: MutableList<ChChestData>,
        serverId: String,
        bbcommand: String,
        extraMessage: String?,
        status: StatusConstants
    ) : this(
        chest,
        serverId,
        bbcommand,
        extraMessage,
        status.displayName
    ) {
        this.color = status.color
    }

    @Throws(SQLException::class)
    open fun setStatus(statusBase: StatusConstants) {
        setStatusNoOverride(statusBase)
    }

    fun setStatusNoOverride(statusBase: String) {
        this.status = statusBase
    }

    fun setStatusNoOverride(statusBase: StatusConstants) {
        this.status = statusBase.displayName
        color = statusBase.color

    }

    fun addChest(chest: ChChestData) {
        chests.add(chest)
    }

    fun transferToUser(newContactMan: String) {
        contactMan = newContactMan
    }

    @Throws(SQLException::class)
    open fun setLobbyMetaData(playersStillIn: MutableList<String>?, closingTime: Instant?) {
        this.playersStillIn = playersStillIn
        if (closingTime != null) {
            this.closingTime = closingTime
            onLobbyUpdate()
        }
    }

    open fun onLobbyUpdate() {
        // need to be overridden
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ChestLobbyData) return false
        return other.lobbyId == lobbyId
    }

    protected open fun updateLobby(lobby: ChestLobbyData) {
        bbcommand = lobby.bbcommand
        extraMessage = lobby.extraMessage
        status = lobby.status
        contactMan = lobby.contactMan
        chests = ArrayList(lobby.chests)
        try {
            setLobbyMetaData(lobby.playersStillIn, lobby.closingTime)
        } catch (ignored: SQLException) {
        }
    }

    override fun hashCode(): Int {
        return lobbyId
    }
}
