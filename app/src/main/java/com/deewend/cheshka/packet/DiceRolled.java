package com.deewend.cheshka.packet;

import com.deewend.cheshka.Packet;
import com.deewend.cheshka.annotation.Clientbound;
import com.deewend.cheshka.annotation.Order;

@Clientbound
public class DiceRolled extends Packet {
    @Order(no = 1) public byte value;

    @Override
    public int getId() {
        return 0x05;
    }
}
