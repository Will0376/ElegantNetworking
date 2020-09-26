package z.hohserg.elegant.networking.impl;

import z.hohserg.elegant.networking.api.ElegantPacket;
import z.hohserg.elegant.networking.api.IByteBufSerializable;
import lombok.Value;

import java.util.HashMap;
import java.util.Map;

public class ElegantNetworking {

    static Map<String, String> channelByPacketClassName = new HashMap<>();
    static Map<String, Integer> packetIdByPacketClassName = new HashMap<>();
    static Map<Integer, String> packetClassNameById = new HashMap<>();
    private static Network defaultImpl = new CCLNetworkImpl();

    public static Network getNetwork() {
        return defaultImpl;
    }

    static void register(PacketInfo p, int id) {
        channelByPacketClassName.put(p.className, p.channel);
        packetIdByPacketClassName.put(p.className, id);
        packetClassNameById.put(id, p.className);

    }

    @Value
    static class PacketInfo {
        public String channel;
        public String className;
    }

    private static Map<Class, ISerializator> serialisators = new HashMap<>();

    public static ISerializator getSerialisator(IByteBufSerializable value) {
        Class<?> valueClass = value.getClass();
        if (valueClass.getAnnotation(ElegantPacket.class) != null) {
            return serialisators.computeIfAbsent(valueClass, valueClass1 -> {
                try {
                    return (ISerializator) Class.forName(valueClass1.getName() + "Serializator").newInstance();
                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                    e.printStackTrace();
                    return null;
                }
            });
        } else
            return null;
    }
}
