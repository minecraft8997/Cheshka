package com.deewend.cheshka.packet;

import com.deewend.cheshka.BuildConfig;
import com.deewend.cheshka.Helper;
import com.deewend.cheshka.Packet;
import com.deewend.cheshka.annotation.Order;
import com.deewend.cheshka.annotation.Serverbound;

@Serverbound
public class ClientHello extends Packet {
    @Order(no = 1) public final int magic = Helper.CLIENT_HELLO_MAGIC;
    @Order(no = 2) public final int clientVersionCode = BuildConfig.VERSION_CODE;
    @Order(no = 3) public final byte protocolVersion = 6;
    @Order(no = 4) public String serverAddress;
    @Order(no = 5) public int serverPort;
    @Order(no = 6) public String language; // ISO 639 two-letter code
    @Order(no = 7) public byte reserved;

    @Override
    public int getId() {
        return 0x00;
    }
}
