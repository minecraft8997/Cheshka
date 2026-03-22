package com.deewend.cheshka.packet;

import com.deewend.cheshka.Packet;
import com.deewend.cheshka.annotation.Clientbound;
import com.deewend.cheshka.annotation.Order;

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
