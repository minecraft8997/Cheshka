package ru.deewend.cheshka.packet;

import ru.deewend.cheshka.Packet;
import ru.deewend.cheshka.annotation.Clientbound;
import ru.deewend.cheshka.annotation.Order;

@Clientbound
public class DiceRolled extends Packet {
    @Order(no = 1) public byte value;

    @Override
    public int getId() {
        return 0x05;
    }
}
