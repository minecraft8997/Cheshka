package ru.deewend.cheshka;

import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class LauncherActivity extends CheshkaActivity {
    private GamePreferences preferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = Cheshka.getInstance(this).getPreferences();

        setContentView(R.layout.activity_launcher);

        int toHide;
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            toHide = R.id.game_image_logo;
        } else {
            toHide = R.id.game_text_logo;
        }
        findViewById(toHide).setVisibility(View.GONE);

        ((TextView) findViewById(R.id.copyright_view))
                .setText(getString(R.string.copyright_text, BuildConfig.VERSION_NAME));
        ((TextView) findViewById(R.id.links_view))
                .setMovementMethod(LinkMovementMethod.getInstance());

        if (preferences.isServerSpecified()) {
            ((EditText) findViewById(R.id.server_address_field))
                    .setText(preferences.getServerAddress());
            ((EditText) findViewById(R.id.server_port_field))
                    .setText(String.valueOf(preferences.getServerPort()));
        }
        if (preferences.hasCredentials()) {
            ((EditText) findViewById(R.id.username_field)).setText(preferences.getUsername());
        }

        restoreButton(R.id.singleplayer_button, savedInstanceState);
        restoreButton(R.id.proceed_button, savedInstanceState);
        restoreButton(R.id.show_assets_attribution_button, savedInstanceState);

        CompoundButton.OnCheckedChangeListener checkBoxListener = (compoundButton, isChecked) -> {
            int id = compoundButton.getId();
            if (id == R.id.sounds_checkbox) {
                preferences.setEnableSounds(isChecked);
            } else if (id == R.id.timer_checkbox) {
                preferences.setShowPlaytime(isChecked);
            } else if (id == R.id.movement_animation_checkbox) {
                preferences.setEnableMovementAnimation(isChecked);
            } else {
                preferences.setEnableEvalBar(isChecked);
            }

            preferences.savePreferences();
        };
        CheckBox soundsCheckbox = findViewById(R.id.sounds_checkbox);
        soundsCheckbox.setChecked(preferences.shouldEnableSounds());
        soundsCheckbox.setOnCheckedChangeListener(checkBoxListener);

        CheckBox timerCheckbox = findViewById(R.id.timer_checkbox);
        timerCheckbox.setChecked(preferences.shouldShowPlaytime());
        timerCheckbox.setOnCheckedChangeListener(checkBoxListener);

        CheckBox movementAnimationCheckbox = findViewById(R.id.movement_animation_checkbox);
        movementAnimationCheckbox.setChecked(preferences.shouldEnableMovementAnimation());
        movementAnimationCheckbox.setOnCheckedChangeListener(checkBoxListener);

        CheckBox evalBarCheckbox = findViewById(R.id.eval_bar_checkbox);
        evalBarCheckbox.setChecked(preferences.shouldEnableEvalBar());
        evalBarCheckbox.setOnCheckedChangeListener(checkBoxListener);
    }

    @Override
    protected byte onClick(int id, Button button) {
        if (id == R.id.singleplayer_button) {
            Singleplayer.init(this, 8);
        } else if (id == R.id.proceed_button) {
            Object serverAddress = getEditTextValue(R.id.server_address_field, true);
            Object serverPort = getEditTextValue(R.id.server_port_field, false);
            Object username = getEditTextValue(R.id.username_field, true);
            if (checkBadValue(serverAddress, serverPort, username)) {
                Toast.makeText(this, R.string.bad_integer_text, Toast.LENGTH_SHORT).show();

                return DO_NOT_DISABLE;
            }
            preferences.setServerAddress((String) serverAddress);
            preferences.setServerPort((int) serverPort);
            preferences.setUsername((String) username);
            preferences.savePreferences();

            NetworkingThread.runThread(preferences, false);
        } else {
            button.setVisibility(View.GONE);
            findViewById(R.id.attribution_title_text).setVisibility(View.VISIBLE);
            TextView attribution = findViewById(R.id.attribution_text);
            String content = Helper.readAssetFully(
                    this, "attribution.html", getString(R.string.io_issue_text)
            );
            attribution.setText(Html.fromHtml(content));
            attribution.setMovementMethod(LinkMovementMethod.getInstance());
            attribution.setVisibility(View.VISIBLE);

            return DO_NOT_DISABLE;
        }

        return DISABLE_ALL_BUTTONS;
    }

    private Object getEditTextValue(int resId, boolean string) {
        String stringValue = Helper.getEditTextStringValue(this, resId);
        if (string) return stringValue;

        int intValue;
        try {
            intValue = Integer.parseInt(stringValue);
        } catch (NumberFormatException e) {
            return Helper.BAD_VALUE;
        }

        return intValue;
    }

    private boolean checkBadValue(Object... objects) {
        for (Object element : objects) {
            if (element == Helper.BAD_VALUE) return true;
        }

        return false;
    }
}
