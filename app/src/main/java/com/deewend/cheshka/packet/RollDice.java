package com.deewend.cheshka.packet;

import com.deewend.cheshka.Packet;
import com.deewend.cheshka.annotation.Serverbound;

@Serverbound
public class RollDice extends Packet {
    @Override
    public int getId() {
        return 0x03;
    }
}
