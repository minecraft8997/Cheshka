package ru.deewend.cheshka;

import static ru.deewend.cheshka.Helper.SERVER_HELLO_MAGIC;
import static ru.deewend.cheshka.Helper.getClassName;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Locale;
import java.util.UUID;

import ru.deewend.cheshka.annotation.Serverbound;
import ru.deewend.cheshka.packet.ClientHello;
import ru.deewend.cheshka.packet.ClientIdentification;
import ru.deewend.cheshka.packet.Disconnect;
import ru.deewend.cheshka.packet.IdentificationResult;
import ru.deewend.cheshka.packet.ServerHello;

public class NetworkingThread extends Thread {
    /** @noinspection deprecation*/
    @SuppressLint("StaticFieldLeak") // not in this case
    public class SendPacketTask extends AsyncTask<Packet, Void, Void> {
        @Override
        protected Void doInBackground(Packet... packets) {
            try {
                for (Packet packet : packets) sendPacket(packet);
            } catch (IOException e) {
                /*
                 * Doing this instead of passing SendPacketTask.class.getName() since
                 * static declarations are not supported in inner classes, at the current
                 * language level.
                 */
                Log.w("SendPacketTask", "Failed to deliver a packet", e);
            }

            return null;
        }
    }

    public static final String TAG = NetworkingThread.class.getName();
    public static final int SECRET = 0xA115E11C; // reverted Helper.CLIENT_HELLO_MAGIC

    private static final Object THREAD_LOCK = new Object();

    private static NetworkingThread instance;
    private static WeakReference<CheshkaActivity> currentActivity;

    private final String serverAddress;
    private final int serverPort;
    private final String username;
    private final UUID clientId;

    private volatile boolean socketOpened;
    private Socket socket;
    private DataInputStream inputStream;
    private OutputStream outputStream;
    private Packet received;

    private NetworkingThread(GamePreferences preferences) {
        if (!preferences.isServerSpecified()) {
            throw new IllegalArgumentException("Server credentials are not specified");
        }
        serverAddress = preferences.getServerAddress();
        serverPort = preferences.getServerPort();
        username = preferences.getUsername();
        clientId = preferences.getClientId();

        setName("Networking Thread");
        setDaemon(true);
    }

    public static void runThread(GamePreferences preferences, boolean forcibly) {
        synchronized (THREAD_LOCK) {
            if (isDead0() || forcibly) {
                if (forcibly && instance != null) {
                    Log.w(TAG, "Replacing an existing NetworkingThread with a new one");

                    instance.interrupt();
                }
                instance = new NetworkingThread(preferences);
                instance.start();
            } else {
                Log.w(TAG, "Illegal attempt to start a NetworkingThread", new Throwable());
            }
        }
    }

    @SuppressWarnings("unused")
    public static boolean isDead() {
        synchronized (THREAD_LOCK) {
            boolean dead = isDead0();
            if (dead) instance = null;

            return dead;
        }
    }

    public static void staticClose() {
        staticSend(null);
    }

    /** @noinspection deprecation*/
    public static void staticSend(Packet packet) {
        synchronized (THREAD_LOCK) {
            if (!isDead0() && instance.socketOpened) {
                /*
                 * Expecting this to be a thread-safe operation, since
                 * I/O streams have already been initialized (socketOpened value shows that).
                 * These fields should not be assigned to something else during SendPacketTask's
                 * work.
                 */
                instance.new SendPacketTask().execute(packet);
            }
        }
    }

    public static String getServerCredentials() {
        synchronized (THREAD_LOCK) {
            return getServerCredentials0();
        }
    }

    private static boolean isDead0() {
        return instance == null || !instance.isAlive();
    }

    private static String getServerCredentials0() {
        return instance.serverAddress + ":" + instance.serverPort;
    }

    public static synchronized void setCurrentActivity(CheshkaActivity activity) {
        currentActivity = new WeakReference<>(activity);
    }

    public static synchronized CheshkaActivity getCurrentActivity() {
        return (currentActivity != null ? currentActivity.get() : null);
    }

    private void doLogic() throws IOException {
        // during handshake we expect that the current
        // activity is LauncherActivity, even if the user is reconnecting
        ClientHello hello = new ClientHello();
        hello.serverAddress = serverAddress;
        hello.serverPort = serverPort;
        hello.language = Locale.getDefault().getLanguage();
        sendPacket(hello);

        receivePacket(ServerHello.class);
        ServerHello serverHello = (ServerHello) received;
        if (serverHello.magic != SERVER_HELLO_MAGIC) {
            throw new GameProtocolException(R.string.protocol_bad_magic_text);
        }
        PacketHandler.getInstance().setMessageOfTheDay(serverHello.serverMOTD);

        ClientIdentification identification = new ClientIdentification();
        identification.username = username;
        identification.clientId = clientId;
        sendPacket(identification);

        boolean successfulIdentification = false;
        /*
         * Remember that a GameProtocolException is
         * thrown when the client receives Disconnect packet.
         */
        while (true) {
            Class<? extends Packet> requiredClass;
            if (successfulIdentification) requiredClass = null;
            else requiredClass = IdentificationResult.class;
            receivePacket(requiredClass);

            if (!successfulIdentification) {
                IdentificationResult result = (IdentificationResult) received;

                successfulIdentification = result.success;
            }
            if (isInterrupted()) break;

            PacketHandler.getInstance().handle(received);
        }
        // we don't need to put interrupt() here since isInterrupted() does not clear the status
    }

    @Override
    public void run() {
        Log.i(TAG, "Connecting to " + getServerCredentials0());

        IOException exception = null;
        try (Socket socket = new Socket()) {
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(150_000);
            this.socket = socket;

            socket.connect(new InetSocketAddress(serverAddress, serverPort), 8_000);

            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = socket.getOutputStream();
            socketOpened = true;

            doLogic();
        } catch (RuntimeException | IOException e) {
            //noinspection ExtractMethodRecommender
            IOException actualIOException;
            if (e instanceof RuntimeException) {
                RuntimeException runtimeException = (RuntimeException) e;
                Throwable cause;
                if ((cause = runtimeException.getCause()) instanceof IOException) {
                    actualIOException = (IOException) cause;
                } else {
                    /*
                     * Seems to be a reflection-related bug. Throwing this
                     * RuntimeException will lead to ACRA generating a crash report.
                     */
                    throw runtimeException;
                }
            } else {
                actualIOException = (IOException) e;
            }

            Log.w(TAG, "Disconnected with an " +
                    "IOException (in some cases this is totally fine)", actualIOException);

            exception = actualIOException;
        } finally {
            PacketHandler.getInstance().handle(null, exception);
        }
    }

    private void sendPacket(Packet packet) throws IOException {
        if (packet == null) {
            socket.close();

            return;
        }
        Class<?> clazz = packet.getClass();
        if (!clazz.isAnnotationPresent(Serverbound.class)) {
            throw new RuntimeException("Attempted to send " +
                    clazz.getName() + " packet which is not annotated as @Serverbound");
        }

        try {
            packet.send(outputStream);
        } catch (Exception e) {
            throw new RuntimeException("Sending a packet", e);
        }
    }

    private void receivePacket(Class<? extends Packet> expecting) throws IOException {
        try {
            received = Packet.deserialize(inputStream);
        } catch (Exception e) {
            throw new RuntimeException("Receiving a packet", e);
        }
        if (received instanceof Disconnect) {
            Disconnect disconnect = (Disconnect) received;

            throw new GameProtocolException(R.string.disconnected_text, disconnect.reason);
        }

        if (expecting != null && !expecting.isInstance(received)) {
            throw new GameProtocolException(R.string.protocol_unexpected_text,
                    getClassName(expecting), getClassName(received));
        }
    }
}
