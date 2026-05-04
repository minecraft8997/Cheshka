package com.deewend.cheshka;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CrashReport {
    private static class WrappedThrowable extends RuntimeException {
        public final Thread t;
        public final Throwable e;

        public WrappedThrowable(Thread t, Throwable e) {
            this.t = t;
            this.e = e;
        }
    }

    public static final String TAG = CrashReport.class.getName();
    public static final String CRASH_MAIL = "cheshka-crash@deewend.com";
    public static final String[] ADDRESSES = { CRASH_MAIL };

    private static final CrashReport INSTANCE = new CrashReport();

    private Thread.UncaughtExceptionHandler oldHandler;
    @SuppressWarnings("unused")
    private volatile byte[] reserved = new byte[16384];
    private final String manufacturer = Build.MANUFACTURER;
    private final String model = Build.MODEL;
    private final String androidVersion = Build.VERSION.RELEASE;
    private final int sdkVersion = Build.VERSION.SDK_INT;
    private final List<Integer> signatureHashList = new ArrayList<>();
    private boolean hasDisplayMetrics;
    private int availableWidth;
    private int availableHeight;
    private float density;
    private File crashReportFile;

    private CrashReport() {
    }

    public static CrashReport getInstance() {
        return INSTANCE;
    }

    public void setup(Context context) {
        crashReportFile = new File(context.getFilesDir(), "last_report.txt");
        oldHandler = Thread.getDefaultUncaughtExceptionHandler();
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager
                    .getPackageInfo(BuildConfig.APPLICATION_ID, PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                signatureHashList.add(signature.hashCode());
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Unexpected NameNotFoundException", e);
        }

        Thread.setDefaultUncaughtExceptionHandler(this::handleUncaughtException);
    }

    /** @noinspection ExtractMethodRecommender*/
    public void resumeHook(CheshkaActivity activity) {
        updateScreenData(activity);

        if (activity instanceof InGameActivity || !crashReportFile.exists()) return;

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, ADDRESSES);
        intent.putExtra(Intent.EXTRA_SUBJECT, "Cheshka Crash Report");
        if (intent.resolveActivity(activity.getPackageManager()) == null) {
            deleteReport();

            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.issue_detected_text);
        builder.setMessage(R.string.issue_detected_description_text);
        builder.setPositiveButton(R.string.confirm_text, (dialog, which) -> {
            try {
                sendReport(activity, intent);
            } finally {
                deleteReport();
            }
        });
        builder.setNegativeButton(R.string.cancel_text, (dialog, which) -> deleteReport());

        builder.create().show();
    }

    /** @noinspection ResultOfMethodCallIgnored*/
    private void deleteReport() {
        crashReportFile.delete();
    }

    /** @noinspection IOStreamConstructor*/
    private void sendReport(CheshkaActivity activity, Intent intent) {
        String contents;
        try (InputStream stream = new FileInputStream(crashReportFile)) {
            contents = Helper.readFully(stream);
        } catch (IOException e) {
            Log.w(TAG, "Reading a crash report", e);

            return;
        }
        intent.putExtra(Intent.EXTRA_TEXT, contents);

        activity.startActivity(intent);
    }

    public void createReport(String comment, Throwable e) {
        createReport(comment, Thread.currentThread(), e, false);
    }

    /** @noinspection UnusedAssignment*/
    public void createReport(String comment, Thread t, Throwable e, boolean unhandled) {
        if (e instanceof WrappedThrowable) {
            WrappedThrowable throwable = (WrappedThrowable) e;

            t = throwable.t;
            e = throwable.e;
        }

        boolean oom = (e instanceof OutOfMemoryError);
        if (oom) {
            /*
             * Unsure if it really can work anything out.
             */
            reserved = null;
        }
        Thread mainThread = Looper.getMainLooper().getThread();
        if (Thread.currentThread() != mainThread) {
            final Thread finalT = t;
            final Throwable finalE = e;
            Helper.enqueueUIJob(() -> {
                if (!unhandled) {
                    createReport(comment, finalT, finalE, false);
                } else {
                    throw new WrappedThrowable(finalT, finalE);
                }
            });

            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("--- CHESHKA CLIENT CRASH REPORT ---\n");
        builder.append("Generated at ").append(new Date()).append('\n');
        if (comment != null) builder.append("Comment: ").append(comment).append('\n');
        builder.append("Device manufacturer: ").append(manufacturer).append('\n');
        builder.append("Device model: ").append(model).append('\n');
        builder.append("Android version: ").append(androidVersion).append('\n');
        builder.append("SDK version: ").append(sdkVersion).append('\n');
        builder.append("Application version: ").append(BuildConfig.VERSION_NAME).append('\n');
        builder.append("Application version code: ").append(BuildConfig.VERSION_CODE).append('\n');
        builder.append("Build type: ").append(BuildConfig.BUILD_TYPE).append('\n');
        int signatureListSize = signatureHashList.size();
        builder.append("Signature count: ").append(signatureListSize).append('\n');
        for (int i = 0; i < signatureListSize; i++) {
            int hash = signatureHashList.get(i);
            builder.append("Signature #").append(i + 1).append(" hash: ").append(hash).append('\n');
        }
        boolean hasDisplayMetrics;
        int availableWidth;
        int availableHeight;
        float density;
        synchronized (this) {
            hasDisplayMetrics = this.hasDisplayMetrics;
            availableWidth = this.availableWidth;
            availableHeight = this.availableHeight;
            density = this.density;
        }
        if (hasDisplayMetrics) {
            builder.append("Available width: ").append(availableWidth).append('\n');
            builder.append("Available height: ").append(availableHeight).append('\n');
            builder.append("Density: ").append(String.format(Locale.US, "%.2f", density))
                    .append('\n');
        }
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        boolean noLimit = (maxMemory == Long.MAX_VALUE);
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        String percentageUsed = (noLimit ? null :
                Helper.percentsString((double) usedMemory / maxMemory));
        String used = Helper.toGiBOrMiB(usedMemory);

        builder.append("Memory usage: ").append(used).append(" / ");
        if (noLimit) {
            builder.append("unlimited");
        } else {
            String outOf = Helper.toGiBOrMiB(maxMemory);

            builder.append(outOf).append(" (").append(percentageUsed).append(")");
        }
        builder.append('\n');
        builder.append("Occurred in the main thread? ");
        if (t == mainThread) {
            builder.append("Yes\n");
        } else {
            builder.append("No\n");
            builder.append("Faulty thread details: name=").append(t.getName()).append(", id=")
                    .append(t.getId()).append('\n');
        }
        builder.append("Stack trace: ").append(Helper.getStackTraceString(e)).append('\n');
        builder.append("Unhandled? ").append(unhandled ? "Yes" : "No").append('\n');

        fillPacketHandlerData(builder);
        /*
         * Try to free as much memory as possible.
         */
        if (oom) PacketHandler.getInstance().reset();

        String resultStr = builder.toString();
        builder = null;
        Log.w(TAG, resultStr);

        byte[] resultBytes = resultStr.getBytes(Helper.UTF8_CHARSET);
        resultStr = null;
        try {
            //noinspection ResultOfMethodCallIgnored
            crashReportFile.createNewFile();
            //noinspection IOStreamConstructor
            try (OutputStream stream = new FileOutputStream(crashReportFile)) {
                stream.write(resultBytes);
            }
        } catch (IOException ex) {
            Log.w(TAG, "Failed to save the crash report file", ex);
        }

        if (oom) {
            /*
             * Exiting since we've just invalidated PacketHandler state.
             */
            System.exit(-1);
        }
        if (!unhandled) return;

        Thread.UncaughtExceptionHandler oldHandler = this.oldHandler;

        if (oldHandler != null) oldHandler.uncaughtException(t, e);
    }

    private void fillPacketHandlerData(StringBuilder builder) {
        PacketHandler handler = PacketHandler.getInstance();
        builder.append("PacketHandler state:\n");
        builder.append("identified=").append(handler.identified).append('\n');
        builder.append("suspiciousEvents=").append(handler.suspiciousEvents).append('\n');
        builder.append("noActivity=").append(handler.noActivity).append('\n');
        builder.append("singleplayer=").append(handler.singleplayer).append('\n');
        builder.append("resignsReceived=").append(handler.resignsReceived).append('\n');
        Board board = handler.board;

        if (board != null) builder.append("Board state: ").append(board);
    }

    private void handleUncaughtException(Thread t, Throwable e) {
        createReport("unhandled issue", t, e, true);
    }

    private void updateScreenData(CheshkaActivity activity) {
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        synchronized (this) {
            hasDisplayMetrics = true;
            availableWidth = metrics.widthPixels;
            availableHeight = metrics.heightPixels;
            density = metrics.density;
        }
    }
}
