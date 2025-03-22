package ru.deewend.cheshka;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import ru.deewend.cheshka.packet.ClientIdentification;

public class CaptchaChallengeActivity extends CheshkaActivity {
    private boolean requestedData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_captcha_challenge);

        Bitmap captcha = PacketHandler.getInstance().captcha;
        if (captcha != null) {
            ImageView imageView = findViewById(R.id.captcha_view);
            imageView.setImageBitmap(captcha);
        }

        restoreButton(R.id.generate_another_captcha_button, savedInstanceState);
        restoreButton(R.id.proceed_button, savedInstanceState);
    }

    @Override
    protected byte onClick(int id, Button button) {
        ClientIdentification identification = new ClientIdentification();
        if (id == R.id.proceed_button) {
            identification.captcha = Helper.getEditTextStringValue(this, R.id.captcha_field);
        }
        requestedData = true;
        NetworkingThread.staticSend(identification);

        return DISABLE_ALL_BUTTONS;
    }

    public boolean hasRequestedData() {
        return requestedData;
    }
}
