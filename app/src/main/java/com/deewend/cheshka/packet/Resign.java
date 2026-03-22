package com.deewend.cheshka.packet;

import com.deewend.cheshka.Packet;
import com.deewend.cheshka.annotation.Clientbound;
import com.deewend.cheshka.annotation.Serverbound;

@Clientbound
@Serverbound
public class Resign extends Packet {
    @Override
    public int getId() {
        return 0x09;
    }
}
