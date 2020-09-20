package hohserg.elegant.networking.api;

import hohserg.elegant.networking.impl.ElegantNetworking;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.INetHandlerPlayServer;

/**
 * Base interface for packet, which can be send from client to server
 */
public interface ClientToServerPacket extends IByteBufSerializable {
    /**
     * Called when the packet is received
     * @param player Sender
     */
    void onReceive(EntityPlayerMP player, INetHandlerPlayServer handler);

    default void sendToServer() {
        ElegantNetworking.getNetwork().sendToServer(this);
    }
}
