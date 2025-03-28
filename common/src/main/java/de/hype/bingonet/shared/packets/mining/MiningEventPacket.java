package de.hype.bingonet.shared.packets.mining;

import de.hype.bingonet.environment.packetconfig.AbstractPacket;
import de.hype.bingonet.shared.constants.Islands;
import de.hype.bingonet.shared.constants.MiningEvents;

/**
 * used to announce a mining event network wide. Can be used by both Client and Server to announce to each other.
 */
public class MiningEventPacket extends AbstractPacket {
    public final MiningEvents event;
    public final String username;
    public final Islands island;

    /**
     * @param event    Event happening {@link MiningEvents (available constants)}
     * @param username username of the finder
     * @param island   Island Event is happening on. Options: {@link Islands#DWARVEN_MINES} , {@link Islands#CRYSTAL_HOLLOWS}
     * @throws Exception when the Island is invalid. Can be when the island is CH but event can only be in Dwarfen Mines
     */
    public MiningEventPacket(MiningEvents event, String username, Islands island) {
        super(1, 1); //Min and Max supported Version
        this.event = event;
        this.username = username;
        if (island != Islands.CRYSTAL_HOLLOWS && island != Islands.DWARVEN_MINES)
            throw new IllegalArgumentException("Invalid Island!");
        if (island.equals(Islands.CRYSTAL_HOLLOWS)) {
            if (event.equals(MiningEvents.MITHRIL_GOURMAND) || event.equals(MiningEvents.RAFFLE) || event.equals(MiningEvents.GOBLIN_RAID)) {
                throw new IllegalArgumentException("The specified event can not happen on this Server");
            }
        }
        this.island = island;
    }
}
