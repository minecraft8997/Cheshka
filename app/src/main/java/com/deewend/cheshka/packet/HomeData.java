package com.deewend.cheshka.packet;

import android.graphics.Bitmap;

import com.deewend.cheshka.Packet;
import com.deewend.cheshka.annotation.Clientbound;
import com.deewend.cheshka.annotation.OnlyWhen;
import com.deewend.cheshka.annotation.Order;

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
