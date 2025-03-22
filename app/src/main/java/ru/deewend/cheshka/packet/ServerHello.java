package ru.deewend.cheshka.packet;

import ru.deewend.cheshka.Packet;
import ru.deewend.cheshka.annotation.Clientbound;
import ru.deewend.cheshka.annotation.Order;

@Clientbound
public class ServerHello extends Packet {
    @Order(no = 1) public int magic;
    @Order(no = 2) public int serverVersionCode;
    @Order(no = 3) public String serverMOTD;

    @Override
    public int getId() {
        return 0x00;
    }
}
