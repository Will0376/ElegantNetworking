package hohserg.elegant.networking.impl;

import io.netty.buffer.ByteBuf;

public interface ISerializator {
    void serialize(Object value, ByteBuf acc);
}
