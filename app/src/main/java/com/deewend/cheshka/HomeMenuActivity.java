package com.deewend.cheshka;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.deewend.cheshka.packet.InitiateMatchmaking;

public class HomeMenuActivity extends CheshkaActivity {
    private boolean initiatedMatchmaking;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home_menu);

        PacketHandler handler = PacketHandler.getInstance();

        Bitmap serverLogo = handler.serverLogo;
        if (serverLogo != null) {
            ImageView imageView = findViewById(R.id.server_logo_view);
            imageView.setImageBitmap(serverLogo);
        }
        TextView youArePlayingAt = findViewById(R.id.you_are_playing_at_text);
        youArePlayingAt.setText(getString(R.string.you_are_playing_at_text,
                NetworkingThread.getServerCredentials(),
                handler.getMessageOfTheDay())
        );
        if (Cheshka.getInstance(this).getPreferences().shouldShowUserGeneratedContent()) {
            TextView serverStats = findViewById(R.id.server_stats_text);
            serverStats.setText(getString(R.string.server_stats_text,
                    handler.onlinePlayerCount,
                    handler.activeGamesCount)
            );
        }

        restoreButton(R.id.accept_invitation_button, savedInstanceState);
        restoreButton(R.id.create_invitation_button, savedInstanceState);
        restoreButton(R.id.play_with_a_random_opponent_button, savedInstanceState);
        restoreButton(R.id.disconnect_button, savedInstanceState);
    }

    @Override
    protected byte onClick(int id, Button button) {
        InitiateMatchmaking initiateMatchmaking = new InitiateMatchmaking();

        if (id == R.id.accept_invitation_button) {
            initiateMatchmaking.invitationCode =
                    Helper.getEditTextStringValue(this, R.id.invitation_code_field);
        } else if (id == R.id.create_invitation_button) {
            initiateMatchmaking.mode = InitiateMatchmaking.MODE_CREATE_INVITE;
        } else if (id == R.id.play_with_a_random_opponent_button) {
            initiateMatchmaking.mode = InitiateMatchmaking.MODE_RANDOM_OPPONENT;
        } else {
            requestedDisconnect = true;

            NetworkingThread.staticClose();
        }
        if (!requestedDisconnect) {
            initiatedMatchmaking = true;

            NetworkingThread.staticSend(initiateMatchmaking);
        }

        return DISABLE_ALL_BUTTONS;
    }

    public boolean hasInitiatedMatchmaking() {
        return initiatedMatchmaking;
    }
}
