package ru.deewend.cheshka.packet;

import ru.deewend.cheshka.Packet;
import ru.deewend.cheshka.annotation.Clientbound;
import ru.deewend.cheshka.annotation.Order;

@Clientbound
public class OpponentFound extends Packet {
    @Order(no = 1) public String opponentDisplayName;
    @Order(no = 2) public int boardSize;
    @Order(no = 3) public int secondsForTurn;
    @Order(no = 4) public int noMoveDrawThreshold;
    @Order(no = 5) public int moveNumber;
    @Order(no = 6) public int subMoveNumber;
    @Order(no = 7) public String myPiecePositions;
    @Order(no = 8) public String opponentPiecePositions;
    @Order(no = 9) public boolean whiteColor;
    @Order(no = 10) public boolean myTurnNow;
    @Order(no = 11) public boolean lastChanceActivated;
    @Order(no = 12) public long ageMillis;
    @Order(no = 13) public long reserved;

    @Override
    public int getId() {
        return 0x04;
    }
}
