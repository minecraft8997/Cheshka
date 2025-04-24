package ru.deewend.cheshka;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class EvalBarView extends CheshkaView {
    private Paint whiteColumnPaint;
    private Paint blackColumnPaint;
    private Rect whiteColumnRect;
    private Rect blackColumnRect;
    private int width;
    private int height;

    public EvalBarView(Context context) {
        super(context);
    }

    public EvalBarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public EvalBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        PacketHandler handler = PacketHandler.getInstance();
        Board board = handler.board;

        if (board == null) return;

        double whitesChance = board.evalBar();
        int whiteColumnLength = (int) (width * whitesChance);
        int blackColumnLength = Math.max(width - whiteColumnLength, 0);

        whiteColumnRect.top = 0;
        blackColumnRect.top = 0;
        whiteColumnRect.bottom = height;
        blackColumnRect.bottom = height;
        int adjustment = (int) BoardView.adjustment;
        int lastRight = width - adjustment;
        if (handler.whiteColor) {
            whiteColumnRect.left = adjustment;
            whiteColumnRect.right = whiteColumnLength;

            blackColumnRect.left = whiteColumnLength;
            blackColumnRect.right = lastRight;
        } else {
            blackColumnRect.left = adjustment;
            blackColumnRect.right = blackColumnLength;

            whiteColumnRect.left = blackColumnLength;
            whiteColumnRect.right = lastRight;
        }

        canvas.drawRect(whiteColumnRect, whiteColumnPaint);
        canvas.drawRect(blackColumnRect, blackColumnPaint);
    }

    @Override
    protected void onMeasurePx(int widthSize, int heightSize) {
        int boardSize = BoardView.computeBoardSize(widthSize, heightSize);

        width = boardSize;
        height = (int) (0.015D * boardSize);

        setMeasuredDimension(width, height);
    }

    @Override
    public void initialize() {
        if (whiteColumnPaint == null) {
            whiteColumnPaint = rgbPaint(255, 255, 255);
            blackColumnPaint = rgbPaint(0, 0, 0);
            whiteColumnRect = new Rect();
            blackColumnRect = new Rect();
        }
    }
}
