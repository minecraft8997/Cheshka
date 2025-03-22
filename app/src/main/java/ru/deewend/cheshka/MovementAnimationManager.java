package ru.deewend.cheshka;

import androidx.annotation.IntRange;

import java.util.ArrayList;
import java.util.List;

public class MovementAnimationManager {
    public static class Point {
        public final float x;
        public final float y;
        @IntRange(from = 0, to = 255)
        private int opacity;

        public Point(float x, float y, int opacity) {
            this.x = x;
            this.y = y;
            this.opacity = opacity;
        }

        public boolean isVisible() {
            return opacity > 0;
        }

        public int getOpacity() {
            return opacity;
        }
    }

    public static class Animation {
        public static final float DURATION_SECONDS = 1.2F;
        public static final int DURATION_TICKS = (int) (CheshkaView.FPS * DURATION_SECONDS);
        public static final int OPACITY_DELTA = MAX_INTERMEDIATE_OPACITY / DURATION_TICKS;

        private final List<Point> points;
        private final boolean whitePiece;

        private Animation(List<Point> points, boolean whitePiece) {
            this.points = points;
            this.whitePiece = whitePiece;
        }

        public boolean tick() {
            boolean foundVisible = false;
            for (Point point : points) {
                point.opacity = Math.max(point.opacity - OPACITY_DELTA, 0);

                if (point.isVisible()) foundVisible = true;
            }

            return foundVisible;
        }

        public boolean isWhitePiece() {
            return whitePiece;
        }

        public List<Point> getPoints() {
            return points;
        }
    }

    public static final float STEP = 1.0F;
    public static final int MAX_INTERMEDIATE_OPACITY = 128;

    private static final MovementAnimationManager INSTANCE = new MovementAnimationManager();

    private final List<Animation> activeAnimations = new ArrayList<>();
    /*
     * Pre-allocating a list following the recommendation not to instantiate
     * anything during rendering (tick method is going to be called in BoardView#onDraw).
     */
    private final List<Animation> toRemove = new ArrayList<>();

    private MovementAnimationManager() {
    }

    public static MovementAnimationManager getInstance() {
        return INSTANCE;
    }

    public void registerAnimation(int x1, int y1, int x2, int y2, boolean whitePiece) {
        float distance = (float) Math.hypot(x2 - x1, y2 - y1);
        float dx = (x2 - x1) / distance * STEP;
        float dy = (y2 - y1) / distance * STEP;

        int numPoints = (int) (distance / STEP) + 1;
        List<Point> intermediatePoints = new ArrayList<>(numPoints);
        int opacityStep = MAX_INTERMEDIATE_OPACITY / numPoints;
        int opacity = opacityStep;
        for (int i = 0; i < numPoints; i++) {
            float x = x1 + i * dx;
            float y = y1 + i * dy;
            if (x == x2 && y == y2) break;

            Point point = new Point(x, y, opacity);
            opacity = Math.min(opacity + opacityStep, MAX_INTERMEDIATE_OPACITY);

            intermediatePoints.add(point);
        }

        activeAnimations.add(new Animation(intermediatePoints, whitePiece));
    }

    public List<Animation> getActiveAnimations() {
        return activeAnimations;
    }

    public void tick() {
        for (Animation animation : activeAnimations) {
            boolean stillActive = animation.tick();
            if (!stillActive) toRemove.add(animation);
        }
        activeAnimations.removeAll(toRemove);

        toRemove.clear();
    }

    public void reset() {
        activeAnimations.clear();
    }
}
