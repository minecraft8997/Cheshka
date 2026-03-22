package com.deewend.cheshka.packet;

import com.deewend.cheshka.Packet;
import com.deewend.cheshka.annotation.Clientbound;

@Clientbound
public class OpponentNotFound extends Packet {
    @Override
    public int getId() {
        return 0x07;
    }
}
