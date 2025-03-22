package ru.deewend.cheshka;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class DisconnectedActivity extends CheshkaActivity {
    public static final String THROWABLE_KEY = BuildConfig.APPLICATION_ID + ".throwable";
    public static final String REASON_KEY = BuildConfig.APPLICATION_ID + ".reason";

    private Throwable throwable;
    private int reason;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_disconnected);

        restoreButton(R.id.reconnect_button, savedInstanceState);
        restoreButton(R.id.goto_main_menu_button, savedInstanceState);

        Bundle extras;
        if ((extras = getIntent().getExtras()) != null) {
            throwable = (Throwable) extras.getSerializable(THROWABLE_KEY);
            reason = extras.getInt(REASON_KEY);
        }
        String stackTrace;
        if (throwable != null) {
            if (throwable instanceof GameProtocolException) {
                ((GameProtocolException) throwable).setContext(this);
            }

            stackTrace = Helper.getStackTraceString(throwable);
        } else {
            stackTrace = getString(R.string.not_specified_text);
        }
        if (reason == 0) reason = R.string.not_specified_text;

        ((TextView) findViewById(R.id.details_view))
                .setText(getString(R.string.details_text, stackTrace, getString(reason)));
    }

    @Override
    protected byte onClick(int id, Button button) {
        if (id == R.id.reconnect_button) {
            GamePreferences preferences = Cheshka.getInstance(this).getPreferences();

            NetworkingThread.runThread(preferences, false);
        } else {
            Helper.startActivity(this, LauncherActivity.class);
        }

        return DISABLE_ALL_BUTTONS;
    }
}
