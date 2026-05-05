package com.deewend.cheshka;

import android.graphics.Bitmap;

/*
 * Strictly a 480x320 image containing only black and white pixels.
 * Each byte encodes 8 pixels in a row.
 * Each image size is always 480*320/8=19200 bytes.
 */
public class SimpleBitmapDecoder {
    public static final int WIDTH = 480;
    public static final int HEIGHT = 320;
    public static final int BYTES_IN_LINE = WIDTH / 8;
    public static final int SIZE = HEIGHT * BYTES_IN_LINE;

    public static Bitmap decode(byte[] image) {
        if (image.length != SIZE) return null;

        Bitmap result = Bitmap.createBitmap(480, 320, Bitmap.Config.RGB_565);
        for (int i = 0; i < HEIGHT; i++) {
            for (int j = 0; j < BYTES_IN_LINE; j++) {
                int colors = image[i * BYTES_IN_LINE + j];
                for (int x = 0; x < 8; x++) {
                    int color = (getBit(colors, x) == 0 ? 0x000000 : 0xFFFFFF);
                    result.setPixel(j * 8 + x, i, color);
                }
            }
        }

        return result;
    }

    // thanks https://stackoverflow.com/a/14145767/10945188
    private static int getBit(int n, int i) {
        return (n >> i) & 1;
    }
}
