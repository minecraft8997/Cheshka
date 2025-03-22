package ru.deewend.cheshka.packet;

import ru.deewend.cheshka.Packet;
import ru.deewend.cheshka.annotation.Clientbound;
import ru.deewend.cheshka.annotation.Serverbound;

@Clientbound
@Serverbound
public class Resign extends Packet {
    @Override
    public int getId() {
        return 0x09;
    }
}
