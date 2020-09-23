package hohserg.elegant.networking.api;

import hohserg.elegant.networking.impl.DataUtils2;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

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
    default void serialize(ByteBuf acc) {
        DataUtils2.serialize(this, acc);
    }

}
