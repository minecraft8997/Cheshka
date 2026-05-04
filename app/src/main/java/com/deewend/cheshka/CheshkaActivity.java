package com.deewend.cheshka;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public abstract class CheshkaActivity extends AppCompatActivity {
    public static final long BACK_PRESS_MAX_INTERVAL = 5_000L;

    public static final byte DO_NOT_DISABLE = 0;
    public static final byte DISABLE_CURRENT_BUTTON = 1;
    public static final byte DISABLE_ALL_BUTTONS = 2;

    static boolean requestedDisconnect;

    private final List<Integer> buttons = new ArrayList<>();
    private long lastTimePressed;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        NetworkingThread.setCurrentActivity(this);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        saveButtonsState(outState);
    }

    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - lastTimePressed > BACK_PRESS_MAX_INTERVAL) {
            Toast.makeText(this, R.string.press_again_text, Toast.LENGTH_SHORT).show();
        } else {
            System.exit(0);
        }

        lastTimePressed = System.currentTimeMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();

        CrashReport.getInstance().resumeHook(this);
    }

    protected final void runCheshkaViews(int... viewResIds) {
        for (int resId : viewResIds) runCheshkaView(findViewById(resId));
    }

    protected final void runCheshkaView(CheshkaView view) {
        view.linkActivity();
        view.setRunning(true);
    }

    protected final void restoreButton(int id, Bundle savedInstanceState) {
        Button button = findViewById(id);
        if (savedInstanceState != null) {
            boolean buttonEnabled =
                    savedInstanceState.getBoolean("button" + id + "Enabled", true);
            button.setEnabled(buttonEnabled);

            if (!buttonEnabled) askToWait(button);
        }
        button.setOnClickListener((v) -> {
            byte toDo = onClick(id, button);
            if (toDo > DO_NOT_DISABLE) {
                if (toDo == DISABLE_CURRENT_BUTTON) lockButton(id);
                else                                lockAllButtons();
            }
        });

        buttons.add(id);
    }

    public final void lockAllButtons() {
        for (int buttonId : buttons) lockButton(buttonId);
    }

    private void lockButton(int buttonId) {
        Button layoutButton = findViewById(buttonId);
        layoutButton.setEnabled(false);

        askToWait(layoutButton);
    }

    private void askToWait(Button button) {
        button.setText(R.string.please_wait_text);
    }

    protected byte onClick(int id, Button button) {
        return DO_NOT_DISABLE;
    }

    protected final void saveButtonsState(Bundle outState) {
        if (this instanceof InGameActivity) return;

        for (int id : buttons) {
            outState.putBoolean("button" + id + "Enabled", findViewById(id).isEnabled());
        }
    }
}
