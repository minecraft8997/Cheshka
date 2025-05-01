package ru.deewend.cheshka;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

public class LauncherActivity extends CheshkaActivity {
    public static final long TOUCH_MAX_DELTA = 1000L;
    public static final int TOUCHES_NEEDED_TO_SHOW_COMMAND_DISPATCHER = 5;

    private static boolean showCommandMenuAutomatically;

    private GamePreferences preferences;
    private int difficultySelection;
    private int boardSizeSelection;
    private int touchCount;
    private long lastTouchTimestamp;
    private boolean shownCommandMenu;

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

        restoreButton(R.id.singleplayer_button, savedInstanceState);
        restoreButton(R.id.multiplayer_button, savedInstanceState);
        restoreButton(R.id.settings_button, savedInstanceState);
        restoreButton(R.id.execute_command_button, savedInstanceState);
        restoreButton(R.id.show_assets_attribution_button, savedInstanceState);

        if (showCommandMenuAutomatically) {
            showCommandMenu();

            return;
        }
        int logoId = (toHide == R.id.game_image_logo ? R.id.game_text_logo : R.id.game_image_logo);
        findViewById(logoId).setOnTouchListener((v, e) -> {
            v.performClick();

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastTouchTimestamp > TOUCH_MAX_DELTA) {
                touchCount = 0;
            }
            touchCount++;
            lastTouchTimestamp = currentTime;
            if (touchCount >= TOUCHES_NEEDED_TO_SHOW_COMMAND_DISPATCHER) {
                touchCount = 0;

                showCommandMenu();
            }

            return false;
        });
    }

    private void showCommandMenu() {
        if (shownCommandMenu) return;

        TextView dispatcherTitle = findViewById(R.id.cmd_dispatcher_title);
        dispatcherTitle.setMovementMethod(LinkMovementMethod.getInstance());
        dispatcherTitle.setVisibility(View.VISIBLE);
        EditText commandField = findViewById(R.id.command_field);
        commandField.setText(preferences.getLastCommand());
        commandField.setVisibility(View.VISIBLE);
        findViewById(R.id.execute_command_button).setVisibility(View.VISIBLE);

        shownCommandMenu = true;
        showCommandMenuAutomatically = true;
    }

    @Override
    protected byte onClick(int id, Button button) {
        if (id == R.id.singleplayer_button) {
            showSingleplayerDialog();

            return DO_NOT_DISABLE;
        }
        if (id == R.id.multiplayer_button) {
            showMultiplayerDialog();

            return DO_NOT_DISABLE;
        }
        if (id == R.id.settings_button) {
            showSettingsDialog();

            return DO_NOT_DISABLE;
        }
        if (id == R.id.execute_command_button) {
            String command = (String) getEditTextValue(R.id.command_field, true);
            preferences.setLastCommand(command);
            preferences.savePreferences();
            if (command.startsWith("!")) {
                processCommand(command.substring(1), false);
            } else {
                Toast.makeText(this, R.string.cmd_should_start_with, Toast.LENGTH_SHORT).show();
            }

            return DO_NOT_DISABLE;
        }
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

    private void processCommand(String command, boolean confirm) {
        if (!confirm) {
            String finalCommand = command;

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.command_title_text);
            builder.setMessage(R.string.command_warning_text);
            builder.setPositiveButton(R.string.confirm_text,
                    (dialog, which) -> processCommand(finalCommand, true));
            Helper.defaultNegativeButton(builder);

            builder.create().show();

            return;
        }
        command = command.toLowerCase();
        if (command.startsWith("load")) {
            command = command.substring(4).trim();

            int boardSize = getIntArgument(command, "size", 8);
            int mode = getIntArgument(command, "mode", Singleplayer.MODE_NORMAL);
            String whitesPos = getArgument(command, "white");
            String blacksPos = getArgument(command, "black");
            //noinspection SpellCheckingInspection
            boolean guaranteeRollOf6 = command.contains("guaranteerollof6");
            //noinspection SpellCheckingInspection
            boolean allowVaults = command.contains("allowvaults");

            String color = getArgument(command, "color");
            if (color == null) color = getArgument(command, "colour");
            Boolean whiteColor = null;
            if ("white".equals(color)) whiteColor = Boolean.TRUE;
            else if ("black".equals(color)) whiteColor = Boolean.FALSE;
            // else, the color will be chosen randomly

            Singleplayer.init(this, boardSize, mode, guaranteeRollOf6, whiteColor);

            Board board = PacketHandler.getInstance().board;
            if (whitesPos != null) board.deserialize(true, whitesPos);
            if (blacksPos != null) board.deserialize(false, blacksPos);
            board.setAllowVaults(allowVaults);
        } else {
            Toast.makeText(this, R.string.unknown_command_text, Toast.LENGTH_SHORT).show();
        }
    }

    private int getIntArgument(String src, String key, int defaultValue) {
        try {
            return Integer.parseInt(getArgument(src, key));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String getArgument(String src, String key) {
        int idx = src.indexOf(key);
        int lastIdx = src.length() - 1;
        if (idx == -1 || idx == lastIdx) return null;

        int nextI = idx + key.length();
        if (src.charAt(nextI) != '(') return null;

        int endI = -1;
        for (int i = 1; ; i++) {
            int realI = nextI + i;
            if (realI > lastIdx) break;

            if (src.charAt(realI) == ')') {
                endI = realI;

                break;
            }
        }
        if (endI == -1) return null;

        return src.substring((nextI + 1), endI);
    }

    private void showSingleplayerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.singleplayer_options_title);
        builder.setView(getSingleplayerOptionsView());
        builder.setPositiveButton(R.string.confirm_text, (dialog, which) -> {
            int boardSize = 6 + boardSizeSelection * 2;
            boolean guaranteeRollOf6 = preferences.shouldGuaranteeRollOf6();

            if (difficultySelection == Singleplayer.MODE_NOTICEABLY_HARD && guaranteeRollOf6) {
                return; // fixme
            }

            Singleplayer.init(
                    this, boardSize, difficultySelection, guaranteeRollOf6, null
            );
        });
        Helper.defaultNegativeButton(builder);

        builder.create().show();
    }

    private void showMultiplayerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.multiplayer_text);
        builder.setView(getMultiplayerOptionsView());
        Helper.defaultNegativeButton(builder);

        builder.create().show();
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.settings_text);
        builder.setView(getSettingsView());
        builder.setNegativeButton(R.string.close_text, null);

        builder.create().show();
    }

    @SuppressLint("InflateParams")
    private View getSingleplayerOptionsView() {
        View view = getLayoutInflater().inflate(R.layout.layout_singleplayer_options, null);
        configureSpinner(view.findViewById(R.id.difficulty_spinner), R.array.difficulty_array,
                preferences.getDifficultyDefaultSelection());
        configureSpinner(view.findViewById(R.id.board_size_spinner), R.array.board_size_array,
                preferences.getBoardSizeDefaultSelection());
        CheckBox guarantee10 = view.findViewById(R.id.guarantee_10);
        guarantee10.setChecked(preferences.shouldGuaranteeRollOf6());
        guarantee10.setOnCheckedChangeListener((checkbox, isChecked) -> {
            preferences.setGuaranteeRollOf6(isChecked);
            preferences.savePreferences();
        });

        return view;
    }

    @SuppressLint("InflateParams")
    private View getMultiplayerOptionsView() {
        View view = getLayoutInflater().inflate(R.layout.layout_multiplayer_options, null);
        if (preferences.isServerSpecified()) {
            ((EditText) view.findViewById(R.id.server_address_field))
                    .setText(preferences.getServerAddress());
            ((EditText) view.findViewById(R.id.server_port_field))
                    .setText(String.valueOf(preferences.getServerPort()));
        }
        if (preferences.hasCredentials()) {
            ((EditText) view.findViewById(R.id.username_field)).setText(preferences.getUsername());
        }
        view.findViewById(R.id.proceed_button).setOnClickListener((v) -> {
            Object serverAddress = getEditTextValue(view, R.id.server_address_field, true);
            Object serverPort = getEditTextValue(view, R.id.server_port_field, false);
            if (checkBadValue(serverPort)) {
                Toast.makeText(this, R.string.bad_integer_text, Toast.LENGTH_SHORT).show();

                return;
            }
            Object username = getEditTextValue(view, R.id.username_field, true);

            String serverAddressStr = (String) serverAddress;
            preferences.setServerAddress(serverAddressStr);
            preferences.setServerPort((int) serverPort);
            preferences.setUsername((String) username);
            preferences.savePreferences();

            NetworkingThread.runThread(preferences, false);

            lockAllButtons();
            v.setEnabled(false);
        });

        return view;
    }

    @SuppressLint("InflateParams")
    private View getSettingsView() {
        View view = getLayoutInflater().inflate(R.layout.layout_settings, null);

        CompoundButton.OnCheckedChangeListener checkBoxListener = (checkbox, isChecked) -> {
            int id = checkbox.getId();
            if (id == R.id.sounds_checkbox) {
                preferences.setEnableSounds(isChecked);
            } else if (id == R.id.timer_checkbox) {
                preferences.setShowPlaytime(isChecked);
            } else if (id == R.id.movement_animation_checkbox) {
                preferences.setEnableMovementAnimation(isChecked);
            } else if (id == R.id.special_highlighting_checkbox) {
                preferences.setEnableSpecialHighlighting(isChecked);
            } else {
                preferences.setEnableEvalBar(isChecked);
            }

            preferences.savePreferences();
        };
        CheckBox soundsCheckbox = view.findViewById(R.id.sounds_checkbox);
        soundsCheckbox.setChecked(preferences.shouldEnableSounds());
        soundsCheckbox.setOnCheckedChangeListener(checkBoxListener);

        CheckBox timerCheckbox = view.findViewById(R.id.timer_checkbox);
        timerCheckbox.setChecked(preferences.shouldShowPlaytime());
        timerCheckbox.setOnCheckedChangeListener(checkBoxListener);

        CheckBox movementAnimationCheckbox = view.findViewById(R.id.movement_animation_checkbox);
        movementAnimationCheckbox.setChecked(preferences.shouldEnableMovementAnimation());
        movementAnimationCheckbox.setOnCheckedChangeListener(checkBoxListener);

        CheckBox specialHighlightingCheckbox = view.findViewById(R.id.special_highlighting_checkbox);
        specialHighlightingCheckbox.setChecked(preferences.shouldEnableSpecialHighlighting());
        specialHighlightingCheckbox.setOnCheckedChangeListener(checkBoxListener);

        CheckBox evalBarCheckbox = view.findViewById(R.id.eval_bar_checkbox);
        evalBarCheckbox.setChecked(preferences.shouldEnableEvalBar());
        evalBarCheckbox.setOnCheckedChangeListener(checkBoxListener);

        return view;
    }

    private void configureSpinner(Spinner spinner, int arrayId, int selection) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                arrayId,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onSpinnerSelection(arrayId, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spinner.setSelection(selection);
    }

    private void onSpinnerSelection(int arrayId, int position) {
        if (arrayId == R.array.difficulty_array) {
            difficultySelection = position;
            preferences.setDifficultyDefaultSelection(position);
        } else {
            boardSizeSelection = position;
            preferences.setBoardSizeDefaultSelection(position);
        }

        preferences.savePreferences();
    }

    /** @noinspection SameParameterValue*/
    private Object getEditTextValue(int resId, boolean string) {
        return getEditTextValue(this, resId, string);
    }

    private Object getEditTextValue(Object from, int resId, boolean string) {
        String stringValue = Helper.getEditTextStringValue(from, resId);
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
