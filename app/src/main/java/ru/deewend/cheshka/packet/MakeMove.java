package ru.deewend.cheshka.packet;

import ru.deewend.cheshka.Packet;
import ru.deewend.cheshka.annotation.Clientbound;
import ru.deewend.cheshka.annotation.Order;
import ru.deewend.cheshka.annotation.Serverbound;

@Clientbound
@Serverbound
public class MakeMove extends Packet {
    public static final byte MOVE_TYPE_GENERAL = 0;
    public static final byte MOVE_TYPE_SPAWNING = 1;
    public static final byte MOVE_TYPE_NO_MOVE = 2;

    @Order(no = 1) public int moveNumber;
    @Order(no = 2) public int subMoveNumber;
    /*
     * Should be ignored by the server-side, only useful for the client.
     */
    @Order(no = 3) public boolean whitesMove;
    @Order(no = 4) public int piecePosition;
    @Order(no = 5) public byte moveType;
    /*
     * Also should be ignored by the server-side.
     */
    @Order(no = 6) public boolean automatic;

    @Override
    public int getId() {
        return 0x06;
    }
}
