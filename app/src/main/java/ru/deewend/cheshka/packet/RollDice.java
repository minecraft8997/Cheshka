package ru.deewend.cheshka.packet;

import ru.deewend.cheshka.Packet;
import ru.deewend.cheshka.annotation.Serverbound;

@Serverbound
public class RollDice extends Packet {
    @Override
    public int getId() {
        return 0x03;
    }
}
