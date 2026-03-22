package com.deewend.cheshka;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.UUID;

import com.deewend.cheshka.annotation.Clientbound;
import com.deewend.cheshka.annotation.OnlyWhen;
import com.deewend.cheshka.packet.DiceRolled;
import com.deewend.cheshka.packet.Disconnect;
import com.deewend.cheshka.packet.HomeData;
import com.deewend.cheshka.packet.IdentificationResult;
import com.deewend.cheshka.packet.MakeMove;
import com.deewend.cheshka.packet.MatchmakingStarted;
import com.deewend.cheshka.packet.OpponentFound;
import com.deewend.cheshka.packet.OpponentNotFound;
import com.deewend.cheshka.packet.Resign;
import com.deewend.cheshka.packet.ServerHello;

public abstract class Packet {
    /** @noinspection rawtypes*/
    private static final Class[] clientboundPIDMappings;

    static {
        clientboundPIDMappings = new Class[10];

        clientboundPIDMappings[0x00] = ServerHello.class;
        clientboundPIDMappings[0x01] = IdentificationResult.class;
        clientboundPIDMappings[0x02] = HomeData.class;
        clientboundPIDMappings[0x03] = MatchmakingStarted.class;
        clientboundPIDMappings[0x04] = OpponentFound.class;
        clientboundPIDMappings[0x05] = DiceRolled.class;
        clientboundPIDMappings[0x06] = MakeMove.class;
        clientboundPIDMappings[0x07] = OpponentNotFound.class;
        clientboundPIDMappings[0x08] = Disconnect.class;
        clientboundPIDMappings[0x09] = Resign.class;

        for (Class<?> clazz : clientboundPIDMappings) {
            if (!clazz.isAnnotationPresent(Clientbound.class)) {
                throw new RuntimeException(clazz.getName() +
                        " packet is expected to be annotated as @Clientbound");
            }
        }
    }

    private static boolean shouldContinue(
            Field field, Packet packet, Class<?> cachedClass
    ) throws NoSuchFieldException, IllegalAccessException {
        if (Modifier.isStatic(field.getModifiers())) return true;

        if (cachedClass == null) {
            cachedClass = packet.getClass();
        }
        OnlyWhen annotation = field.getAnnotation(OnlyWhen.class);
        if (annotation != null) {
            String fieldValue =
                    String.valueOf(cachedClass.getField(annotation.field()).get(packet));

            return !fieldValue.equals(annotation.is());
        }

        return false;
    }

    public static Packet deserialize(DataInputStream stream) throws Exception {
        int pid = stream.readUnsignedByte();
        Class<?> clazz;
        if (pid >= clientboundPIDMappings.length || (clazz = clientboundPIDMappings[pid]) == null) {
            throw new GameProtocolException(R.string.protocol_unknown_pid_text, pid);
        }
        Packet packet = (Packet) clazz.newInstance();

        for (Field field : Helper.fixOrder(clazz.getFields())) {
            if (shouldContinue(field, packet, clazz)) continue;

            Class<?> type = field.getType();
            if (type == byte.class) {
                field.set(packet, stream.readByte());
            } else if (type == boolean.class) {
                field.set(packet, stream.readBoolean());
            } else if (type == int.class) {
                field.set(packet, stream.readInt());
            } else if (type == long.class) {
                field.set(packet, stream.readLong());
            } else if (type == UUID.class) {
                long most = stream.readLong();
                long least = stream.readLong();

                field.set(packet, new UUID(most, least));
            } else if (type == String.class) {
                field.set(packet, Helper.readString(stream));
            } else if (type == Bitmap.class) {
                field.set(packet, Helper.readBitmap(stream));
            } else {
                throw new RuntimeException("Unsupported packet field type: " + type);
            }
        }

        return packet;
    }

    public abstract int getId();

    public final byte[] serialize() throws Exception {
        byte[] result;
        try (ByteArrayOutputStream stream0 = new ByteArrayOutputStream()) {
            // closing ByteArrayOutputStream should not be mandatory though

            DataOutputStream stream = new DataOutputStream(stream0);
            stream.writeByte(getId());

            Class<?> clazz = getClass();
            for (Field field : Helper.fixOrder(clazz.getFields())) {
                if (shouldContinue(field, this, clazz)) continue;

                Class<?> type = field.getType();
                if (type == byte.class) {
                    stream.writeByte(field.getByte(this));
                } else if (type == boolean.class) {
                    stream.writeBoolean(field.getBoolean(this));
                } else if (type == int.class) {
                    stream.writeInt(field.getInt(this));
                } else if (type == long.class) {
                    stream.writeLong(field.getLong(this));
                } else if (type == UUID.class) {
                    UUID uuid = (UUID) field.get(this);
                    if (uuid == null) uuid = Helper.NULL_UUID_OBJ;

                    stream.writeLong(uuid.getMostSignificantBits());
                    stream.writeLong(uuid.getLeastSignificantBits());
                } else if (type == String.class) {
                    String v = (String) field.get(this);
                    if (v == null) v = Helper.DEFAULT_STRING_VALUE;

                    Helper.writeString(stream, v);
                } else {
                    throw new RuntimeException("Unsupported packet field type: " + type);
                }
            }

            result = stream0.toByteArray();
        }

        return result;
    }

    public final void send(OutputStream stream) throws Exception {
        stream.write(serialize());
        stream.flush();
    }
}
