package ru.deewend.cheshka.packet;

import android.graphics.Bitmap;

import java.util.UUID;

import ru.deewend.cheshka.Packet;
import ru.deewend.cheshka.annotation.Clientbound;
import ru.deewend.cheshka.annotation.OnlyWhen;
import ru.deewend.cheshka.annotation.Order;

@Clientbound
public class IdentificationResult extends Packet {
    @Order(no = 1) public boolean success;
    @OnlyWhen(field = "success", is = "true")
    @Order(no = 2) public String displayName;
    @OnlyWhen(field = "success", is = "true")
    @Order(no = 3) public UUID clientId;
    @OnlyWhen(field = "success", is = "false")
    @Order(no = 4) public Bitmap captcha;

    @Override
    public int getId() {
        return 0x01;
    }
}
