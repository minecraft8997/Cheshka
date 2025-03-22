package ru.deewend.cheshka.packet;

import ru.deewend.cheshka.Packet;
import ru.deewend.cheshka.annotation.Clientbound;
import ru.deewend.cheshka.annotation.Order;

@Clientbound
public class Disconnect extends Packet {
    @Order(no = 1) public String reason;

    @Override
    public int getId() {
        return 0x08;
    }
}
