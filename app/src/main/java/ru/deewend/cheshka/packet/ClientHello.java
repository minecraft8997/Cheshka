package ru.deewend.cheshka.packet;

import ru.deewend.cheshka.BuildConfig;
import ru.deewend.cheshka.Helper;
import ru.deewend.cheshka.Packet;
import ru.deewend.cheshka.annotation.Order;
import ru.deewend.cheshka.annotation.Serverbound;

@Serverbound
public class ClientHello extends Packet {
    @Order(no = 1) public final int magic = Helper.CLIENT_HELLO_MAGIC;
    @Order(no = 2) public final int clientVersionCode = BuildConfig.VERSION_CODE;
    @Order(no = 3) public final byte protocolVersion = 5;
    @Order(no = 4) public String serverAddress;
    @Order(no = 5) public int serverPort;
    @Order(no = 6) public String language; // ISO 639 two-letter code
    @Order(no = 7) public byte reserved;

    @Override
    public int getId() {
        return 0x00;
    }
}
