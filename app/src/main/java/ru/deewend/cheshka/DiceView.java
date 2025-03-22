package ru.deewend.cheshka;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DiceView extends CheshkaView {
    public static final String TAG = DiceView.class.getName();

    public static final String DICE_ATLAS_FILENAME = "dicerolls.png";
    public static final String ROWS_PROPERTIES_FILENAME = "rows.properties";
    public static final int SPRITE_COUNT_X = 16;
    public static final int SPRITE_COUNT_Y = 7;
    public static final int LAST_FRAME = SPRITE_COUNT_X - 1;

    public static final byte MODE_IDLE = 0;
    public static final byte MODE_THINKING = 1;
    public static final byte MODE_ROLLING_EXACT_DIGIT = 2;

    private static final int[] ROWS = new int[SPRITE_COUNT_Y];
    private static boolean rowsPropertiesRead;

    private static byte diceMode;
    private static int diceDigit;
    private static int diceFrame;

    private Bitmap diceAtlas;
    private int width;
    private int height;
    private Rect atlasRect;
    private Rect canvasRect;
    private Properties properties;

    public DiceView(Context context) {
        super(context);
    }

    public DiceView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DiceView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        byte mode = diceMode;
        if (mode == MODE_IDLE) { // can be rewritten without this condition though
            zeroZeroWidthHeight(atlasRect);
        } else {
            int frame = diceFrame++;
            int digit;

            boolean thinking = (mode == MODE_THINKING);
            if (thinking) {
                digit = 0;
            } else if (mode == MODE_ROLLING_EXACT_DIGIT) {
                digit = diceDigit;
            } else {
                throw new IllegalStateException("Unknown mode: " + mode);
            }
            atlasRect.left = frame * width;
            atlasRect.top = ROWS[digit] * height;
            atlasRect.right = atlasRect.left + width;
            atlasRect.bottom = atlasRect.top + height;

            if (frame >= LAST_FRAME) diceFrame = (thinking ? 0 : LAST_FRAME);
        }
        zeroZeroWidthHeight(canvasRect);

        canvas.drawBitmap(diceAtlas, atlasRect, canvasRect, null);
    }

    private void zeroZeroWidthHeight(Rect rect) {
        /*
         * Just to fix Android Studio's preview.
         */
        if (rect == null) return;

        rect.left = 0;
        rect.top = 0;
        rect.right = width;
        rect.bottom = height;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        initializeDiceAtlas();

        width = diceAtlas.getWidth() / SPRITE_COUNT_X;
        height = diceAtlas.getHeight() / SPRITE_COUNT_Y;

        setMeasuredDimension(width, height);
    }

    @Override
    public void initialize() {
        initializeDiceAtlas();

        atlasRect = new Rect();
        canvasRect = new Rect();

        if (!rowsPropertiesRead) {
            properties = new Properties();
            try (InputStream stream = Helper.openFile(activity, ROWS_PROPERTIES_FILENAME)) {
                properties.load(stream);
            } catch (IOException e) {
                Log.w(TAG, "I/O issue when reading " + ROWS_PROPERTIES_FILENAME, e);
            }
            ROWS[0] = getInt("rowThinking", 6);
            ROWS[1] = getInt("row1", 0);
            ROWS[2] = getInt("row2", 3);
            ROWS[3] = getInt("row3", 4);
            ROWS[4] = getInt("row4", 5);
            ROWS[5] = getInt("row5", 1);
            ROWS[6] = getInt("row6", 2);
            properties = null;

            rowsPropertiesRead = true;
        }
    }

    private int getInt(String key, int defaultValue) {
        String result = properties.getProperty(key, Helper.DEFAULT_STRING_VALUE);
        if (result.equals(Helper.DEFAULT_STRING_VALUE)) {
            return defaultValue;
        }
        int parsedResult;
        try {
            parsedResult = Integer.parseInt(result);
        } catch (NumberFormatException e) {
            Log.w(TAG, "Bad value of key \"" + key + "\" in " +
                    ROWS_PROPERTIES_FILENAME + ": " + result + ". Defaulting to " + defaultValue);

            return defaultValue;
        }

        return parsedResult;
    }

    public static void setDigit(int digit) {
        diceDigit = digit;
    }

    public static void setMode(byte mode) {
        diceMode = mode;
        diceFrame = 0;
    }

    public static void reset() {
        setDigit(0);
        setMode(MODE_IDLE);
    }

    private void initializeDiceAtlas() {
        if (diceAtlas != null) return;

        try (InputStream stream = Helper.openFile(activity, DICE_ATLAS_FILENAME)) {
            diceAtlas = BitmapFactory.decodeStream(stream);
        } catch (IOException e) {
            throw new RuntimeException("I/O issue when reading dice atlas file", e);
        }
    }

    public boolean isRolling() {
        return (diceMode == MODE_THINKING || diceMode == MODE_ROLLING_EXACT_DIGIT) &&
                diceFrame < SPRITE_COUNT_X - 1;
    }
}
