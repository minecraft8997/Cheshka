package com.deewend.cheshka;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

public class RestorableDiceView extends DiceView {
    public RestorableDiceView(Context context) {
        super(context);
    }

    public RestorableDiceView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RestorableDiceView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("superState", super.onSaveInstanceState());
        bundle.putByte("diceMode", diceMode);
        bundle.putInt("diceDigit", diceDigit);
        bundle.putInt("diceFrame", diceFrame);

        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            state = bundle.getParcelable("superState");
            diceMode = bundle.getByte("diceMode");
            diceDigit = bundle.getInt("diceDigit");
            diceFrame = bundle.getInt("diceFrame");
        }

        super.onRestoreInstanceState(state);
    }
}
