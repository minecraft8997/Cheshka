package ru.deewend.cheshka.packet;

import ru.deewend.cheshka.Packet;
import ru.deewend.cheshka.annotation.Clientbound;
import ru.deewend.cheshka.annotation.OnlyWhen;
import ru.deewend.cheshka.annotation.Order;

@Clientbound
public class MatchmakingStarted extends Packet {
    @Order(no = 1) public boolean hasInvitationCode;
    @OnlyWhen(field = "hasInvitationCode", is = "true")
    @Order(no = 2) public String invitationCode;

    @Override
    public int getId() {
        return 0x03;
    }
}
