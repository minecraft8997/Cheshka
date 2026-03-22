package com.deewend.cheshka;

import android.content.Context;

import java.io.IOException;
import java.util.Arrays;

public class GameProtocolException extends IOException {
    private final int reasonStringId;
    private final Object[] fmt;
    private Context context;

    public GameProtocolException() {
        this(R.string.protocol_no_further_information_text);
    }

    public GameProtocolException(int reasonStringId, Object... fmt) {
        this.reasonStringId = reasonStringId;
        this.fmt = fmt;
    }

    @Override
    public String getMessage() {
        return translatedMessage();
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public String translatedMessage() {
        String message;
        if (context != null) {
            if (fmt == null || fmt.length == 0) {
                message = context.getString(reasonStringId);
            } else {
                message = context.getString(reasonStringId, fmt);
            }
        } else {
            message = "unable to translate a string " +
                    "(id=" + reasonStringId + ", format=" + Arrays.toString(fmt) + ")";
        }

        return message;
    }
}
