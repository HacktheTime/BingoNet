package de.hype.bingonet.shared.objects

import de.hype.bingonet.server.objects.BBUser
import de.hype.bingonet.shared.constants.StatusConstants
import de.hype.bingonet.shared.constants.TradeType
import java.time.Instant

open class BBServiceData(
    @JvmField protected val type: TradeType?,
    open var description: String,
    open var hoster: BBUser,
    @JvmField var price: Int,
    @JvmField var helpers: MutableList<Helper>,
    @JvmField var maxUsers: Int,
    @JvmField var forceModOnline: Boolean
) {
    var serviceId: Int = 0

    @JvmField
    var status: StatusConstants = StatusConstants.OPEN

    @JvmField
    var title: String? = null

    open var participants: MutableList<Participant> = ArrayList<Participant>()

    protected lateinit var dcMessageID: String

    @JvmField
    var joinLock: Boolean = false

    @JvmField
    var circulateParticipants: Boolean = false

    constructor(
        type: TradeType,
        hoster: BBUser,
        price: Int,
        helpers: MutableList<Helper>,
        forceModOnline: Boolean
    ) : this(type, type.description, hoster, price, helpers, type.getMaximumUsers(helpers.size), forceModOnline)

    class Participant(
        @JvmField var user: BBUser,
        @JvmField var priority: Boolean,
        @JvmField var joinTime: Instant,
        @JvmField var autoRequeue: Boolean,
        @JvmField var price: Int
    ) {
        constructor(user: BBUser, priority: Boolean, free: Boolean, data: BBServiceData) : this(
            user,
            priority,
            free,
            Instant.now(),
            false,
            data
        )

        constructor(
            user: BBUser,
            priority: Boolean,
            free: Boolean,
            joinTime: Instant,
            autoRequeue: Boolean,
            data: BBServiceData
        ) : this(user, priority, joinTime, autoRequeue, if (free) 0 else data.price)

        override fun equals(other: Any?): Boolean {
            if (other is Participant) return other.user == user
            if (other is BBUser) return other == this.user
            return false
        }

        override fun hashCode(): Int {
            return user.hashCode()
        }


    }

    class Helper {
        private var user: BBUser?
        private var username: String?

        constructor(user: BBUser) {
            this.user = user
            this.username = user.mcusername
        }

        constructor(user: BBUser?, username: String) {
            this.user = user
            this.username = username
        }

        override fun equals(other: Any?): Boolean {
            if (other is Helper) return other.username == username
            if (other is String) {
                if (username != null) return other.equals(username, ignoreCase = true)
                else return user!!.mcusername.equals(other, ignoreCase = true)
            }
            if (other is BBUser) {
                if (user != null) return other == user
                else return other.mcusername.equals(username, ignoreCase = true)
            }
            return false
        }

        override fun hashCode(): Int {
            if (username != null) return username.hashCode()
            else return user!!.mcusername.hashCode()
        }

        fun getUserName(): String {
            return username ?: user!!.mcusername
        }

        fun getUser(): BBUser? {
            return user
        }
    }
}
