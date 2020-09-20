package hohserg.elegant.networking.api;

import hohserg.elegant.networking.impl.DataUtils;
import io.netty.buffer.ByteBuf;

/**
 * Base interface for serializable packets
 * <p>
 * Optional: constructor with one ByteBuf argument
 * will be used for unserialize packet
 */
public interface IByteBufSerializable {

    /**
     * Serialize this packet to ByteBuf
     * for send by net
     *
     * @return ByteBuf representation
     */
    default ByteBuf serialize() {
        return DataUtils.serialize(this);
    }

}
