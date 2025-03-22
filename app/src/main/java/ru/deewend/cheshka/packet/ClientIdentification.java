package ru.deewend.cheshka.packet;

import java.util.UUID;

import ru.deewend.cheshka.Packet;
import ru.deewend.cheshka.annotation.Order;
import ru.deewend.cheshka.annotation.Serverbound;

@Serverbound
public class ClientIdentification extends Packet {
    @Order(no = 1) public String username;
    @Order(no = 2) public String captcha;
    @Order(no = 3) public UUID clientId;

    @Override
    public int getId() {
        return 0x01;
    }
}
