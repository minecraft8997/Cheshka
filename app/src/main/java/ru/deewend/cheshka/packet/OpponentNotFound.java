package ru.deewend.cheshka.packet;

import ru.deewend.cheshka.Packet;
import ru.deewend.cheshka.annotation.Clientbound;

@Clientbound
public class OpponentNotFound extends Packet {
    @Override
    public int getId() {
        return 0x07;
    }
}
