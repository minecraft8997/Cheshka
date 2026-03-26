package com.deewend.cheshka;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.zip.CRC32;

import com.deewend.cheshka.annotation.Order;

public class Helper {
    public static final String DEFAULT_STRING_VALUE = "";
    public static final int DEFAULT_INT_VALUE = 0;
    public static final Object BAD_VALUE = new Object();
    public static final int NO_REASON = 0;

    public static final String NULL_UUID = "00000000-0000-0000-0000-000000000000";
    public static final UUID NULL_UUID_OBJ = UUID.fromString(NULL_UUID);

    public static final int MAX_FIELD_SIZE = 65535;

    /*
     * 11 is an attempt to represent H;
     * 5  is an attempt to represent S;
     * K  is skipped;
     *
     * CHESH[K]A.
     */
    public static final int CLIENT_HELLO_MAGIC = 0xC11E511A;

    /*
     * Expected to be equal to 0xDEE111ED.
     *
     * 111 is an attempt to represent W;
     * N   is skipped.
     *
     * DEEWE[N]D.
     */
    public static final int SERVER_HELLO_MAGIC = 0xDEE111ED;

    /*
     * StandardCharsets.UTF_8 is unsupported on API level < 19.
     */
    /** @noinspection CharsetObjectCanBeUsed*/
    public static final Charset UTF8_CHARSET = Charset.forName("UTF8");

    private Helper() {
    }

    public static void enqueueUIJob(Runnable runnable) {
        (new Handler(Looper.getMainLooper())).post(runnable);
    }

    public static void startActivity(
            CheshkaActivity current, Class<? extends CheshkaActivity> clazz
    ) {
        startActivity(current, clazz, null);
    }

    public static void startActivity(
            CheshkaActivity current, Class<? extends CheshkaActivity> clazz, Bundle extras
    ) {
        Intent intent = new Intent(current, clazz);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        if (extras != null) intent.putExtras(extras);

        current.startActivity(intent);
    }

    public static String getEditTextStringValue(Object from, int resId) {
        View v;
        if (from instanceof Activity) {
            v = ((Activity) from).findViewById(resId);
        } else {
            v = ((View) from).findViewById(resId);
        }
        Editable text = ((EditText) v).getText();

        return (text != null ? text.toString().trim() : Helper.DEFAULT_STRING_VALUE);
    }

    public static void defaultNegativeButton(AlertDialog.Builder builder) {
        builder.setNegativeButton(R.string.cancel_text, null);
    }

    public static int calculateCRC32(byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);

        return (int) crc32.getValue();
    }

    private static byte[] readByteArray(DataInputStream stream) throws IOException {
        int length = stream.readUnsignedShort();
        int signature = stream.readInt();

        byte[] contents = new byte[length];
        stream.readFully(contents);

        if ((signature ^ NetworkingThread.SECRET) != calculateCRC32(contents)) {
            throw new GameProtocolException(R.string.protocol_bad_signature_text);
        }

        return contents;
    }

    public static void writeByteArray(DataOutputStream stream, byte[] contents) throws IOException {
        int length = contents.length;

        if (length > MAX_FIELD_SIZE) {
            throw new RuntimeException("Array is too long");
        }
        stream.writeShort(length);
        int signature = calculateCRC32(contents) ^ NetworkingThread.SECRET;
        stream.writeInt(signature);

        stream.write(contents);
    }

    public static String readString(DataInputStream stream) throws IOException {
        byte[] contents = readByteArray(stream);

        return new String(contents, UTF8_CHARSET);
    }

    public static void writeString(DataOutputStream stream, String str) throws IOException {
        byte[] contents = str.getBytes(UTF8_CHARSET);

        writeByteArray(stream, contents);
    }

    public static Bitmap readBitmap(DataInputStream stream) throws IOException {
        byte[] contents = readByteArray(stream);

        return BitmapFactory.decodeByteArray(contents, 0, contents.length);
    }

    public static void disconnect(CheshkaActivity activity, IOException exception, int reason) {
        disconnect(activity, true, exception, reason);
    }

    public static void disconnect(
            CheshkaActivity activity, boolean closeNetworking, IOException exception, int reason
    ) {
        if (closeNetworking) NetworkingThread.staticClose();

        if (activity != null) {
            Bundle parameters = new Bundle();
            parameters.putSerializable(DisconnectedActivity.THROWABLE_KEY, exception);
            parameters.putInt(DisconnectedActivity.REASON_KEY, reason);

            Helper.startActivity(activity, DisconnectedActivity.class, parameters);
        }
    }

    /** @noinspection DataFlowIssue*/
    public static List<Field> fixOrder(Field[] fields) {
        List<Field> annotatedOnly = new ArrayList<>();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Order.class)) annotatedOnly.add(field);
        }
        Collections.sort(annotatedOnly, (first, second) -> {
            int firstNo = first.getAnnotation(Order.class).no();
            int secondNo = second.getAnnotation(Order.class).no();

            return firstNo - secondNo;
        });

        return annotatedOnly;
    }

    public static String getClassName(Object object) {
        Class<?> clazz;
        if (object instanceof Class) {
            clazz = (Class<?>) object;
        } else {
            clazz = object.getClass();
        }

        return clazz.getName();
    }

    public static String calculatedTimeElapsed(long elapsedMillis) {
        if (elapsedMillis < 0L) return "-";
        if (elapsedMillis / 3600_000L >= 1) {
            return ">59:59";
        }
        long minutes = elapsedMillis / 60_000L;
        long seconds = (elapsedMillis - minutes * 60_000L) / 1_000L;

        return adjust2(minutes) + ":" + adjust2(seconds);
    }

    private static String adjust2(long value) {
        if (value >= 10L) return String.valueOf(value);

        return "0" + value;
    }

    public static InputStream openFile(Context context, String filename) throws IOException {
        return context.getAssets().open(filename);
    }

    public static String readAssetFully(Context context, String filename, String defaultValue) {
        try (InputStream stream = openFile(context, filename)) {
            return readFully(stream);
        } catch (IOException e) {
            return defaultValue;
        }
    }

    public static String readFully(InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }

            return builder.toString();
        }
    }

    public static String listToString(List<?> list) {
        if (list == null) list = Collections.EMPTY_LIST;

        StringBuilder builder = new StringBuilder();
        builder.append('[');
        int size = list.size();
        for (int i = 0; i < size; i++) {
            builder.append(list.get(i));
            if (i < size - 1) builder.append(", ");
        }
        builder.append(']');

        return builder.toString();
    }

    public static String toGiBOrMiB(long bytes) {
        double result = bytes / 1024.0D / 1024.0D;
        boolean gib = false;
        if (result >= 1024.0D) {
            result /= 1024.0D;

            gib = true;
        }

        return (long) result + " " + (gib ? "GiB" : "MiB");
    }

    public static String percentsString(double value) {
        return (long) (value * 100.0D) + "%";
    }

    public static String getStackTraceString(Throwable e) {
        StringWriter writer0 = new StringWriter();
        PrintWriter writer = new PrintWriter(writer0);
        e.printStackTrace(writer);

        return writer0.toString();
    }

    /*
     * If expectedClassName == null, retrieve the return value with
     * Either#second and treat it as the className of the provided serialized object.
     *
     * Otherwise, performs the primary stage of object deserialization by converting
     * its field=value entries to a Properties instance.
     */
    /** @noinspection CharsetObjectCanBeUsed */
    public static Either<Properties, String> objStringToData(
            String str, String expectedClassName
    ) {
        int firstBrace = str.indexOf('{');
        int secondBrace = str.lastIndexOf('}');
        if (firstBrace == -1 || secondBrace != str.length() - 1) {
            throw new IllegalArgumentException("Bad string: " + str);
        }
        String className = str.substring(0, firstBrace);
        if (expectedClassName == null) return Either.of(className);
        if (!className.equals(expectedClassName)) {
            throw new IllegalArgumentException("Bad className." +
                    "Expected: " + expectedClassName + ", got: " + className);
        }
        byte[] contents;
        try {
            contents = str
                    .substring(firstBrace + 1, secondBrace)
                    .getBytes("US-ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("US-ASCII is not supported by the device", e);
        }
        int braceLevel = 0;
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] == '{') braceLevel++;
            else if (contents[i] == '}') braceLevel = Math.max(braceLevel - 1, 0);
            else if (contents[i] == ',' && braceLevel == 0) contents[i] = '\n';
        }
        Properties props = new Properties();
        try (InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(contents))) {
            props.load(reader);
        } catch (IOException ignored) {
            // should never happen
        }

        return Either.of(props);
    }

    public static List<?> deserializeList(Board board, String str) {
        if (!str.startsWith("[") || !str.endsWith("]")) {
            throw new IllegalArgumentException("Not a list: " + str);
        }
        String[] unparsedElements = str.substring(1, str.length() - 1).split(", ");

        List<Object> result = new ArrayList<>();
        for (String element : unparsedElements) {
            String className = objStringToData(element, null).second();

            Object obj;
            if (className.equals("Piece")) {
                obj = Board.Piece.deserialize(element);
            } else if (className.equals("PossibleMove")) {
                obj = Board.deserializePossibleMove(board, element);
            } else {
                throw new IllegalArgumentException("Unsupported className: " + className);
            }
            result.add(obj);
        }

        return result;
    }
}
