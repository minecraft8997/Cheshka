package com.deewend.cheshka.packet;

import com.deewend.cheshka.Packet;
import com.deewend.cheshka.annotation.Clientbound;
import com.deewend.cheshka.annotation.OnlyWhen;
import com.deewend.cheshka.annotation.Order;

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
