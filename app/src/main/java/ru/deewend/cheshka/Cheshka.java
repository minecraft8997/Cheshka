package ru.deewend.cheshka;

import android.app.Application;
import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

public class Cheshka extends Application {
    public static final boolean APPGALLERY_BUILD = false;

    private GamePreferences preferences;

    public static Cheshka getInstance(CheshkaActivity activity) {
        return (Cheshka) activity.getApplication();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        preferences = new GamePreferences(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        CrashReport.getInstance().setup(this);
    }

    public GamePreferences getPreferences() {
        return preferences;
    }
}
