package ru.deewend.cheshka.packet;

import android.graphics.Bitmap;

import ru.deewend.cheshka.Packet;
import ru.deewend.cheshka.annotation.Clientbound;
import ru.deewend.cheshka.annotation.OnlyWhen;
import ru.deewend.cheshka.annotation.Order;

@Clientbound
public class HomeData extends Packet {
    @Order(no = 1) public boolean hasServerLogo;
    @OnlyWhen(field = "hasServerLogo", is = "true")
    @Order(no = 2) public Bitmap serverLogo;
    @Order(no = 3) public int onlinePlayerCount;
    @Order(no = 4) public int activeGamesCount;

    @Override
    public int getId() {
        return 0x02;
    }
}
