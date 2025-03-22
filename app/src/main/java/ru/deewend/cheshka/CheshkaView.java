package ru.deewend.cheshka;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.View;

import androidx.annotation.Nullable;

public abstract class CheshkaView extends View implements Choreographer.FrameCallback {
    public static final int FPS = 10;
    public static final long DELAY = 1000L / FPS;
    private final Choreographer choreographer = Choreographer.getInstance();
    private boolean running;
    protected CheshkaActivity activity;

    public CheshkaView(Context context) {
        super(context);
    }

    public CheshkaView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CheshkaView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public final void doFrame(long frameTimeNanos) {
        if (!running) return;

        invalidate();
        postFrameCallback();
    }

    final void linkActivity(CheshkaActivity activity) {
        this.activity = activity;
    }

    public void initialize() {
    }

    public final boolean isRunning() {
        return running;
    }

    public final void setRunning(boolean running) {
        if (this.running == running) return;

        if (running) {
            initialize();
            postFrameCallback(false);
        } else {
            choreographer.removeFrameCallback(this);
        }

        this.running = running;
    }

    private void postFrameCallback() {
        postFrameCallback(true);
    }

    private void postFrameCallback(boolean delayed) {
        choreographer.postFrameCallbackDelayed(this, (delayed ? DELAY : 0L));
    }

    protected Paint rgbPaint(int r, int g, int b) {
        return argbPaint(255, r, g, b);
    }

    protected Paint argbPaint(int a, int r, int g, int b) {
        Paint result = new Paint();
        result.setColor(Color.argb(a, r, g, b));

        return result;
    }
}
