package ru.deewend.cheshka;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import ru.deewend.cheshka.packet.MakeMove;

public class BoardView extends CheshkaView {
    public static final int NOT_SELECTED = -1;
    public static final int SELECTED_COLOR_DELTA = 32;

    private final MutablePair<Integer, Integer> renderPos = new MutablePair<>();
    private final MutablePair<Float, Float> touchPos = new MutablePair<>();
    static float adjustment;
    private int size;
    private int cellSize;
    private Paint whiteCellPaint;
    private Paint blackCellPaint;
    private Paint whitePiecePaint;
    private Paint blackPiecePaint;
    private Paint pieceBorderPaint;
    private Paint redPieceBorderPaint;
    private Paint whitePieceReliefPaint;
    private Paint blackPieceReliefPaint;
    private Paint highlightedCellPaint;
    private int strokeWidthSetFor;
    private int selectedCellX = NOT_SELECTED;
    private int selectedCellY = NOT_SELECTED;
    private Board.Piece highlightedFor;
    private int highlightedCellX = NOT_SELECTED;
    private int highlightedCellY = NOT_SELECTED;
    float pieceRadius;
    float relief1Radius;
    float relief2Radius;

    public BoardView(Context context) {
        super(context);
    }

    public BoardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BoardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public static int computeBoardSize(int widthSize, int heightSize) {
        return Math.min(widthSize, heightSize);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        PacketHandler handler = PacketHandler.getInstance();
        Board board = handler.board;

        if (board == null) return;

        InGameActivity activity = (InGameActivity) this.activity;

        cellSize = size / handler.boardSize;
        int renderedSize = cellSize * handler.boardSize;
        int sizeDiff = size - renderedSize;
        adjustment = sizeDiff / 2.0f;
        if (strokeWidthSetFor != handler.boardSize) {
            float borderWidth = cellSize / 16.0f;
            float reliefWidth = borderWidth / 1.25f;

            pieceBorderPaint.setStrokeWidth(borderWidth);
            redPieceBorderPaint.setStrokeWidth(borderWidth);
            whitePieceReliefPaint.setStrokeWidth(reliefWidth);
            blackPieceReliefPaint.setStrokeWidth(reliefWidth);

            strokeWidthSetFor = handler.boardSize;
        }
        pieceRadius = cellSize * 0.45f; // (cellSize * 0.9) / 2.0
        relief1Radius = cellSize * 0.3f;
        relief2Radius = cellSize * 0.15f;

        for (int x = 0; x < handler.boardSize; x++) {
            for (int y = 0; y < handler.boardSize; y++) {
                float left = adjustment + x * cellSize;
                float top = adjustment + y * cellSize;
                float right = adjustment + (x + 1) * cellSize;
                float bottom = adjustment + (y + 1) * cellSize;

                boolean whiteCell;
                if (y % 2 == 0) {
                    whiteCell = (x % 2 == 0);
                } else {
                    whiteCell = (x % 2 != 0);
                }
                Paint paint = (whiteCell ? whiteCellPaint : blackCellPaint);
                boolean selected = (x == selectedCellX && y == selectedCellY);
                int color = 0;
                if (selected) {
                    color = paint.getColor();
                    int r = Math.max(Color.red(color) - SELECTED_COLOR_DELTA, 0);
                    int g = Math.max(Color.green(color) - SELECTED_COLOR_DELTA, 0);
                    int b = Math.max(Color.blue(color) - SELECTED_COLOR_DELTA, 0);

                    paint.setColor(Color.rgb(r, g, b));
                }
                canvas.drawRect(left, top, right, bottom, paint);

                if (selected) paint.setColor(color);

                if (x == highlightedCellX && y == highlightedCellY) {
                    canvas.drawRect(left, top, right, bottom, highlightedCellPaint);
                }
            }
        }

        if (activity.getPreferences().shouldEnableMovementAnimation()) {
            List<MovementAnimationManager.Animation> animations =
                    MovementAnimationManager.getInstance().getActiveAnimations();
            for (MovementAnimationManager.Animation animation : animations) {
                boolean whitePiece = animation.isWhitePiece();
                for (MovementAnimationManager.Point intermediatePoint : animation.getPoints()) {
                    if (!intermediatePoint.isVisible()) continue;

                    float x = intermediatePoint.x;
                    float y = intermediatePoint.y;
                    int opacity = intermediatePoint.getOpacity();
                    drawPieceAt(canvas, whitePiece, x, y, true, opacity);
                }
            }
        }

        boolean highlighting = false;
        for (Board.Piece piece : board.getPieces()) {
            board.getRenderingXY(piece.getPosition(), renderPos);

            int x = renderPos.first;
            int y = renderPos.second;
            boolean defaultBorder = !piece.isTurningOntoDiagonalLine(board);
            drawPieceAt(canvas, piece.isWhitePiece(), x, y, defaultBorder, 255);

            if (x != selectedCellX || y != selectedCellY) continue;
            if (piece.isWhitePiece() != handler.whiteColor) continue;
            if (isDiceRolling()) continue;

            for (Board.PossibleMove possibleMove : board.getPossibleMoves()) {
                if (possibleMove.getPiece() != piece) continue;

                board.getRenderingXY(possibleMove.getDestination(), renderPos);
                highlightedFor = piece;
                highlightedCellX = renderPos.first;
                highlightedCellY = renderPos.second;
                highlighting = true;

                break;
            }
        }
        if (!highlighting) stopHighlighting();

        activity.renderTick();
        if (handler.singleplayer) Singleplayer.tick(activity);
        MovementAnimationManager.getInstance().tick();
    }

    private void drawPieceAt(
            Canvas canvas, boolean whitePiece, float x, float y, boolean defaultBorder, int alpha
    ) {
        float cx = adjustment + (x * cellSize) + cellSize / 2.0f;
        float cy = adjustment + (y * cellSize) + cellSize / 2.0f;
        Paint piecePaint = (whitePiece ? whitePiecePaint : blackPiecePaint);
        int oldAlpha = piecePaint.getAlpha();
        piecePaint.setAlpha(alpha);
        canvas.drawCircle(cx, cy, pieceRadius, piecePaint);
        Paint borderPaint = (defaultBorder ? pieceBorderPaint : redPieceBorderPaint);
        borderPaint.setAlpha(alpha);
        canvas.drawCircle(cx, cy, pieceRadius, borderPaint);
        Paint reliefPaint = (whitePiece ? whitePieceReliefPaint : blackPieceReliefPaint);
        reliefPaint.setAlpha(alpha);
        canvas.drawCircle(cx, cy, relief1Radius, reliefPaint);
        canvas.drawCircle(cx, cy, relief2Radius, reliefPaint);

        piecePaint.setAlpha(oldAlpha);
        borderPaint.setAlpha(oldAlpha);
        reliefPaint.setAlpha(oldAlpha);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        touchPos.first = event.getX();
        touchPos.second = event.getY();
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            performClick();
        }

        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        boolean returnValue = super.performClick();

        if (cellSize == 0) return returnValue;

        float x = touchPos.first;
        float y = touchPos.second;
        float upperEdge = size - adjustment;
        if (x < adjustment || y < adjustment || x >= upperEdge || y >= upperEdge) {
            return returnValue;
        }
        int oldSelectedCellX = selectedCellX;
        int oldSelectedCellY = selectedCellY;
        selectedCellX = (int) (x - adjustment) / cellSize;
        selectedCellY = (int) (y - adjustment) / cellSize;
        if (selectedCellX == oldSelectedCellX && selectedCellY == oldSelectedCellY) {
            markUnselected();
        }
        if (selectedCellX == highlightedCellX && selectedCellY == highlightedCellY && !isDiceRolling()) {
            PacketHandler handler = PacketHandler.getInstance();
            Board board = handler.board;
            for (Board.PossibleMove move : board.getPossibleMoves()) {
                if (move.getPiece() == highlightedFor) {
                    MakeMove makeMove = board.makeMove(move, false);
                    if (!handler.singleplayer) NetworkingThread.staticSend(makeMove);

                    break;
                }
            }

            markUnselected();
            stopHighlighting();
        }

        return returnValue;
    }

    private boolean isDiceRolling() {
        return ((InGameActivity) activity).isDiceRolling();
    }

    @Override
    protected void onMeasurePx(int widthSize, int heightSize) {
        size = computeBoardSize(widthSize, heightSize);
        strokeWidthSetFor = 0;

        setMeasuredDimension(size, size);
    }

    @Override
    public void initialize() {
        if (whiteCellPaint == null) {
            whiteCellPaint = rgbPaint(237, 222, 200);
            blackCellPaint = rgbPaint(130, 101, 59);
            whitePiecePaint = rgbPaint(255, 255, 255);
            blackPiecePaint = rgbPaint(0, 0, 0);
            pieceBorderPaint = rgbPaint(128, 128, 128);
            redPieceBorderPaint = rgbPaint(255, 0, 0);
            whitePieceReliefPaint = rgbPaint(191, 191, 191); // -64
            blackPieceReliefPaint = rgbPaint(64, 64, 64);
            highlightedCellPaint = argbPaint(192, 255, 255, 0);

            pieceBorderPaint.setStyle(Paint.Style.STROKE);
            redPieceBorderPaint.setStyle(Paint.Style.STROKE);
            whitePieceReliefPaint.setStyle(Paint.Style.STROKE);
            blackPieceReliefPaint.setStyle(Paint.Style.STROKE);
        }
    }

    public void markUnselected() {
        selectedCellX = NOT_SELECTED;
        selectedCellY = NOT_SELECTED;
    }

    public void stopHighlighting() {
        highlightedFor = null;
        highlightedCellX = NOT_SELECTED;
        highlightedCellY = NOT_SELECTED;
    }
}
