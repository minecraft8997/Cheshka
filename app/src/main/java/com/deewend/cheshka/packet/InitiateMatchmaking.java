package com.deewend.cheshka.packet;

import com.deewend.cheshka.Packet;
import com.deewend.cheshka.annotation.OnlyWhen;
import com.deewend.cheshka.annotation.Order;
import com.deewend.cheshka.annotation.Serverbound;

@Serverbound
public class InitiateMatchmaking extends Packet {
    public static final byte MODE_ACCEPT_INVITE = 0;
    public static final byte MODE_CREATE_INVITE = 1;
    public static final byte MODE_RANDOM_OPPONENT = 2;

    @Order(no = 1) public byte mode;
    @OnlyWhen(field = "mode", is = "0")
    @Order(no = 2) public String invitationCode;

    @Override
    public int getId() {
        return 0x02;
    }
}
