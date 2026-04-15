package com.deewend.cheshka;

import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;

public class ContextLostActivity extends CheshkaActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_context_lost);

        restoreButton(R.id.goto_main_menu_button, savedInstanceState);
    }

    @Override
    protected byte onClick(int id, Button button) {
        Helper.startActivity(this, LauncherActivity.class);

        return DISABLE_ALL_BUTTONS;
    }
}
