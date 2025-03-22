package ru.deewend.cheshka;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import ru.deewend.cheshka.packet.CancelMatchmaking;

public class MatchmakingActivity extends CheshkaActivity {
    private boolean requestedToCancelMatchmaking;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_matchmaking);

        String invitationCode = PacketHandler.getInstance().invitationCode;
        if (invitationCode != null) {
            TextView invitationCodeText = findViewById(R.id.invitation_code_text);
            invitationCodeText.setVisibility(View.VISIBLE);
            invitationCodeText.setText(getString(R.string.invitation_code_text, invitationCode));
        }
    }

    @Override
    public void onBackPressed() {
        if (requestedToCancelMatchmaking) return;

        requestedToCancelMatchmaking = true;
        NetworkingThread.staticSend(new CancelMatchmaking());
    }

    public boolean hasRequestedToCancelMatchmaking() {
        return requestedToCancelMatchmaking;
    }
}
