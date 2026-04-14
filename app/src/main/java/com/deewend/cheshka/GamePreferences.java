package com.deewend.cheshka;

import static com.deewend.cheshka.Helper.*;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Objects;
import java.util.UUID;

public class GamePreferences {
    public static final String PREFERENCE_ID = BuildConfig.APPLICATION_ID + ".GamePreferences";

    private final SharedPreferences preferences;
    private String serverAddress;
    private int serverPort;
    private String username;
    private UUID clientId;
    private boolean enableSounds;
    private boolean showPlaytime;
    private boolean enableMovementAnimation;
    private boolean enableSpecialHighlighting;
    private boolean enableEvalBar;
    private int difficultyDefaultSelection;
    private int boardSizeDefaultSelection;
    private boolean guaranteeRollOf6;
    private String lastCommand;
    private String serializedGame;

    private int latestLoadHashCode = hashCode();

    public GamePreferences(Context context) {
        preferences = context.getSharedPreferences(PREFERENCE_ID, Context.MODE_PRIVATE);

        loadPreferences();
    }

    public void loadPreferences() {
        serverAddress = preferences.getString("serverAddress", DEFAULT_STRING_VALUE);
        serverPort = preferences.getInt("serverPort", DEFAULT_INT_VALUE);
        username = preferences.getString("username", DEFAULT_STRING_VALUE);
        clientId = UUID.fromString(preferences.getString("clientId", NULL_UUID));
        enableSounds = preferences.getBoolean("enableSounds", true);
        showPlaytime = preferences.getBoolean("showPlaytime", true);
        enableMovementAnimation = preferences.getBoolean("enableMovementAnimation", true);
        enableSpecialHighlighting = preferences.getBoolean("enableSpecialHighlighting", true);
        enableEvalBar = preferences.getBoolean("enableEvalBar", false);
        difficultyDefaultSelection = preferences.getInt("difficultyDefaultSelection", 0);
        boardSizeDefaultSelection = preferences.getInt("boardSizeDefaultSelection", 1);
        guaranteeRollOf6 = preferences.getBoolean("guaranteeRollOf6", false);
        lastCommand = preferences.getString("lastCommand", DEFAULT_STRING_VALUE);
        serializedGame = preferences.getString("serializedGame", DEFAULT_STRING_VALUE);

        latestLoadHashCode = hashCode();
    }

    public void savePreferences() {
        savePreferences(false);
    }

    private void savePreferences(boolean forcibly) {
        if (!forcibly && latestLoadHashCode == hashCode()) return; // looks like there are no changes

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("serverAddress", serverAddress);
        editor.putInt("serverPort", serverPort);
        editor.putString("username", username);
        editor.putString("clientId", clientId.toString());
        editor.putBoolean("enableSounds", enableSounds);
        editor.putBoolean("showPlaytime", showPlaytime);
        editor.putBoolean("enableMovementAnimation", enableMovementAnimation);
        editor.putBoolean("enableSpecialHighlighting", enableSpecialHighlighting);
        editor.putBoolean("enableEvalBar", enableEvalBar);
        editor.putInt("difficultyDefaultSelection", difficultyDefaultSelection);
        editor.putInt("boardSizeDefaultSelection", boardSizeDefaultSelection);
        editor.putBoolean("guaranteeRollOf6", guaranteeRollOf6);
        editor.putString("lastCommand", lastCommand);
        editor.putString("serializedGame", serializedGame);
        editor.apply();
    }

    public boolean isServerSpecified() {
        return !serverAddress.isEmpty();
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getUsername() {
        return username;
    }

    public UUID getClientId() {
        return clientId;
    }

    public boolean shouldEnableSounds() {
        return enableSounds;
    }

    public boolean shouldShowPlaytime() {
        return showPlaytime;
    }

    public boolean shouldEnableMovementAnimation() {
        return enableMovementAnimation;
    }

    public boolean shouldEnableSpecialHighlighting() {
        return enableSpecialHighlighting;
    }

    public boolean shouldEnableEvalBar() {
        return enableEvalBar;
    }

    public int getDifficultyDefaultSelection() {
        return difficultyDefaultSelection;
    }

    public int getBoardSizeDefaultSelection() {
        return boardSizeDefaultSelection;
    }

    public boolean shouldGuaranteeRollOf6() {
        return guaranteeRollOf6;
    }

    public String getLastCommand() {
        return lastCommand;
    }

    public String getSerializedGame() {
        return serializedGame;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public void setEnableSounds(boolean enableSounds) {
        this.enableSounds = enableSounds;
    }

    public void setShowPlaytime(boolean showPlaytime) {
        this.showPlaytime = showPlaytime;
    }

    public void setEnableMovementAnimation(boolean enableMovementAnimation) {
        this.enableMovementAnimation = enableMovementAnimation;
    }

    public void setEnableSpecialHighlighting(boolean enableSpecialHighlighting) {
        this.enableSpecialHighlighting = enableSpecialHighlighting;
    }

    public void setEnableEvalBar(boolean enableEvalBar) {
        this.enableEvalBar = enableEvalBar;
    }

    public void setDifficultyDefaultSelection(int difficultyDefaultSelection) {
        this.difficultyDefaultSelection = difficultyDefaultSelection;
    }

    public void setBoardSizeDefaultSelection(int boardSizeDefaultSelection) {
        this.boardSizeDefaultSelection = boardSizeDefaultSelection;
    }

    public void setGuaranteeRollOf6(boolean guaranteeRollOf6) {
        this.guaranteeRollOf6 = guaranteeRollOf6;
    }

    public void setLastCommand(String lastCommand) {
        this.lastCommand = lastCommand;
    }

    /*
     * The sole setter method that immediately saves the change(s) to disk.
     */
    public void setSerializedGame(String serializedGame) {
        this.serializedGame = serializedGame;

        savePreferences(true);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                serverAddress,
                serverPort,
                username,
                clientId,
                enableSounds,
                showPlaytime,
                enableMovementAnimation,
                enableSpecialHighlighting,
                enableEvalBar,
                difficultyDefaultSelection,
                boardSizeDefaultSelection,
                guaranteeRollOf6,
                lastCommand,
                serializedGame
        );
    }
}
