package com.deewend.cheshka.packet;

import java.util.UUID;

import com.deewend.cheshka.Packet;
import com.deewend.cheshka.annotation.Order;
import com.deewend.cheshka.annotation.Serverbound;

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
