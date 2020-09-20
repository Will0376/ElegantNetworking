package hohserg.elegant.networking.impl;

import com.google.common.collect.ImmutableMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Value;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DataUtils {

    private static Map<Class, Actions> actions = ImmutableMap.<Class, Actions>builder()
            .put(int.class, new Actions((value, field, buf) -> buf.writeInt(field.getInt(value)), (buf, field, value) -> field.setInt(value, buf.readInt()), ByteBuf::readInt))
            .put(boolean.class, new Actions((value, field, buf) -> buf.writeBoolean(field.getBoolean(value)), (buf, field, value) -> field.setBoolean(value, buf.readBoolean()), ByteBuf::readBoolean))
            .put(byte.class, new Actions((value, field, buf) -> buf.writeByte(field.getByte(value)), (buf, field, value) -> field.setByte(value, buf.readByte()), ByteBuf::readByte))
            .put(long.class, new Actions((value, field, buf) -> buf.writeLong(field.getLong(value)), (buf, field, value) -> field.setLong(value, buf.readLong()), ByteBuf::readLong))
            .put(double.class, new Actions((value, field, buf) -> buf.writeDouble(field.getDouble(value)), (buf, field, value) -> field.setDouble(value, buf.readDouble()), ByteBuf::readDouble))
            .put(float.class, new Actions((value, field, buf) -> buf.writeFloat(field.getFloat(value)), (buf, field, value) -> field.setFloat(value, buf.readFloat()), ByteBuf::readFloat))
            .put(char.class, new Actions((value, field, buf) -> buf.writeChar(field.getChar(value)), (buf, field, value) -> field.setChar(value, buf.readChar()), ByteBuf::readChar))
            .put(short.class, new Actions((value, field, buf) -> buf.writeShort(field.getShort(value)), (buf, field, value) -> field.setShort(value, buf.readShort()), ByteBuf::readShort))
            .put(String.class, new Actions((value, field, buf) -> ByteBufUtils.writeUTF8String(buf, (String) field.get(value)), (buf, field, value) -> field.set(value, ByteBufUtils.readUTF8String(buf)), ByteBufUtils::readUTF8String))
            .build();

    private static Actions genericActions = new Actions((value, field, buf) -> {
        ByteBuf byteBuf = serialize(field.get(value));
        buf.writeInt(byteBuf.readableBytes());
        buf.writeBytes(byteBuf);
    }, (buf, field, value) -> {
        int capacity = buf.readInt();
        field.set(value, unserialize(buf.readBytes(capacity)));
    }, buf -> {
        int capacity = buf.readInt();
        return unserialize(buf.readBytes(capacity));
    });


    @Value
    private static class Actions {
        FieldValueToBuffer pack;
        BufferValueToField unpackAndSet;
        BufferValueToList unpackAndCollect;


        interface FieldValueToBuffer {
            void apply(Object value, Field field, ByteBuf buf) throws IllegalAccessException;
        }

        interface BufferValueToField {
            void apply(ByteBuf buf, Field field, Object value) throws IllegalAccessException;
        }

        interface BufferValueToList {
            default void apply(ByteBuf buf, List<Object> args) {
                args.add(read(buf));
            }

            Object read(ByteBuf buf);

        }
    }

    /**
     * Generic serialization
     *
     * @param value Some object
     * @return ByteBuf representation of object
     */

    public static ByteBuf serialize(Object value) {
        try {
            ByteBuf buf = Unpooled.buffer();
            ByteBufUtils.writeUTF8String(buf, value.getClass().getName());
            for (Field field : value.getClass().getDeclaredFields())
                if (!Modifier.isTransient(field.getModifiers())) {
                    field.setAccessible(true);
                    actions.getOrDefault(field.getType(), genericActions).pack.apply(value, field, buf);
                }
            System.out.println("serialize " + value.getClass().getName() + " " + buf.readableBytes());
            return buf;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException("Unable to serialize object " + value);
        }
    }

    /**
     * Generic serialization
     *
     * @param buf ByteBuf representation of packet
     * @return Packet object
     */

    public static Object unserialize(ByteBuf buf) {
        int readableBytes = buf.readableBytes();
        String className = ByteBufUtils.readUTF8String(buf);
        try {
            System.out.println("unserialize " + className + " " + readableBytes);
            Class<?> aClass = Class.forName(className);
            try {
                Constructor<?> defaultConstructor = aClass.getDeclaredConstructor();
                defaultConstructor.setAccessible(true);
                Object instance = defaultConstructor.newInstance();
                for (Field field : aClass.getDeclaredFields())
                    if (!Modifier.isTransient(field.getModifiers())) {
                        field.setAccessible(true);
                        actions.getOrDefault(field.getType(), genericActions).unpackAndSet.apply(buf, field, instance);
                    }

                return instance;

            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                Constructor<?> declaredConstructor = aClass.getDeclaredConstructors()[0];
                declaredConstructor.setAccessible(true);
                List<Object> args = new ArrayList<>();
                for (Field field : aClass.getDeclaredFields())
                    if (!Modifier.isTransient(field.getModifiers())) {
                        field.setAccessible(true);
                        actions.getOrDefault(field.getType(), genericActions).unpackAndCollect.apply(buf, args);
                    }
                return declaredConstructor.newInstance(args.toArray(new Object[0]));
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException("Unable to unserialize " + className + " from " + buf);
        }

    }

    /**
     * Packet implementation sensitive version of unserialize
     * For internal using
     * Uses unserialization constructor, if exists
     *
     * @param buffer   ByteBuf representation of packet
     * @param packetId Packet id
     * @return Packet object
     */
    static <A> A unserialize(ByteBuf buffer, int packetId) {
        try {
            Constructor<?> unserializeConstructor = Class.forName(ElegantNetworking.packetClassNameById.get(packetId)).getDeclaredConstructor(ByteBuf.class);
            try {
                return (A) unserializeConstructor.newInstance(buffer);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("Unserialization constructor exists, but perform exception", e);
            }
        } catch (NoSuchMethodException | ClassNotFoundException e) {
            return (A) unserialize(buffer);
        }
    }
}
