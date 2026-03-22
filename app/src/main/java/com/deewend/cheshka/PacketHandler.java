package com.deewend.cheshka;

import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.deewend.cheshka.packet.DiceRolled;
import com.deewend.cheshka.packet.HomeData;
import com.deewend.cheshka.packet.IdentificationResult;
import com.deewend.cheshka.packet.MakeMove;
import com.deewend.cheshka.packet.MatchmakingStarted;
import com.deewend.cheshka.packet.OpponentFound;
import com.deewend.cheshka.packet.OpponentNotFound;
import com.deewend.cheshka.packet.ServerHello;

public class PacketHandler {
    public static final String TAG = PacketHandler.class.getName();
    public static final int SUSPICIOUS_EVENTS_THRESHOLD = 5;
    public static final long MIN_HOME_DATA_RECEIVE_INTERVAL_MS = 100L;
    public static final long MIN_HOME_DATA_WITH_LOGO_RECEIVE_INTERVAL_MS = 10_000L;

    private static final PacketHandler INSTANCE = new PacketHandler();

    final Random random = new Random();

    private String messageOfTheDay;
    String displayName;
    Bitmap serverLogo;
    int onlinePlayerCount;
    int activeGamesCount;
    private long lastTimeReceivedHomeData;
    private long lastTimeReceivedHomeDataWithLogo;
    boolean identified;
    Bitmap captcha;
    int suspiciousEvents;
    boolean noActivity;
    String invitationCode;
    long ageMillis;
    long gameStartTimestamp;
    boolean singleplayer;
    String opponentDisplayName;
    int boardSize;
    int secondsForTurn;
    int noMoveDrawThreshold;
    boolean guaranteeRollOf6;
    Board board;
    boolean whiteColor;
    boolean myTurnNow; // not updated over time
    private long inGameActivityStartTimestamp;
    int resignsReceived;

    private PacketHandler() {
        reset();
    }

    public static PacketHandler getInstance() {
        return INSTANCE;
    }

    public void handle(Packet packet, Object... additionalData) {
        Helper.enqueueUIJob(() -> {
            CheshkaActivity activity = NetworkingThread.getCurrentActivity();

            int oldCount = suspiciousEvents;
            handle0(activity, packet, additionalData);
            if (suspiciousEvents == oldCount) {
                suspiciousEvents = 0;
            } else {
                String packetName = (packet != null ? packet.getClass().getName() : "packet");
                Log.w(TAG, (noActivity ? "Unhandled " : "Unexpected ") + packetName);

                if (suspiciousEvents >= SUSPICIOUS_EVENTS_THRESHOLD) {
                    int reasonResId;
                    if (noActivity) {
                        reasonResId = R.string.weird_ui_stuff_text;
                    } else {
                        reasonResId = R.string.too_much_data_text;
                    }
                    Helper.disconnect(activity, null, reasonResId);

                    suspiciousEvents = 0;
                }
            }
        });
    }

    private void handle0(CheshkaActivity activity, Packet packet, Object... additionalData) {
        if (activity == null) {
            suspiciousEvents++;
            noActivity = true;

            return;
        }
        noActivity = false;

        boolean noPacket = (packet == null);
        if (noPacket) {
            reset();

            if (CheshkaActivity.requestedDisconnect) {
                CheshkaActivity.requestedDisconnect = false;

                Helper.startActivity(activity, LauncherActivity.class);

                return;
            }
            /*
             * We don't have to check for (packet instanceof Disconnect).
             * Under the current implementation, this clause should never happen.
             */
            IOException exception = (IOException) additionalData[0];
            Helper.disconnect(activity, false, exception, Helper.NO_REASON);

            return;
        }
        boolean matchmakingOrInGame =
                (activity instanceof MatchmakingActivity) || (activity instanceof InGameActivity);

        if (packet instanceof ServerHello) {
            suspiciousEvents++;
        } else if (packet instanceof IdentificationResult) {
            if (identified) {
                suspiciousEvents++;

                return;
            }
            IdentificationResult clientIdentification = (IdentificationResult) packet;
            identified = clientIdentification.success;

            if (!identified) {
                if (activity instanceof CaptchaChallengeActivity) {
                    if (!((CaptchaChallengeActivity) activity).hasRequestedData()) {
                        suspiciousEvents++;

                        return;
                    }
                }
                captcha = clientIdentification.captcha;

                Helper.startActivity(activity, CaptchaChallengeActivity.class);
            } else {
                captcha = null;
                displayName = clientIdentification.displayName;
                GamePreferences preferences = Cheshka.getInstance(activity).getPreferences();
                preferences.setClientId(clientIdentification.clientId);
                preferences.savePreferences();

                Toast.makeText(activity, R.string.successful_auth_text, Toast.LENGTH_SHORT).show();
            }
        } else if (packet instanceof HomeData) {
            HomeData homeData = (HomeData) packet;
            if (homeData.hasServerLogo) {
                serverLogo = homeData.serverLogo;
            }
            onlinePlayerCount = homeData.onlinePlayerCount;
            activeGamesCount = homeData.activeGamesCount;

            long lastTime = homeData.hasServerLogo ?
                    lastTimeReceivedHomeDataWithLogo : lastTimeReceivedHomeData;
            long currentTime = System.currentTimeMillis();
            long deltaTime = currentTime - lastTime;
            long interval = homeData.hasServerLogo ?
                    MIN_HOME_DATA_WITH_LOGO_RECEIVE_INTERVAL_MS : MIN_HOME_DATA_RECEIVE_INTERVAL_MS;

            lastTimeReceivedHomeData = currentTime;
            if (homeData.hasServerLogo) {
                lastTimeReceivedHomeDataWithLogo = currentTime;
            }
            if (deltaTime < interval) {
                suspiciousEvents++;

                return;
            }

            if (matchmakingOrInGame || activity instanceof HomeMenuActivity) return;

            long delta = System.currentTimeMillis() - inGameActivityStartTimestamp;
            /*
             * InGameActivity was started too recently, "activity" variable might
             * refer to an outdated activity. This should fix inability to rejoin the game.
             */
            if (delta < 1_000L) return;

            Helper.startActivity(activity, HomeMenuActivity.class);
        } else if (packet instanceof MatchmakingStarted) {
            if (matchmakingOrInGame) {
                suspiciousEvents++;

                return;
            }
            MatchmakingStarted matchmakingStarted = (MatchmakingStarted) packet;
            if (matchmakingStarted.hasInvitationCode) {
                invitationCode = matchmakingStarted.invitationCode;
            } else {
                invitationCode = null;
            }

            Helper.startActivity(activity, MatchmakingActivity.class);
        } else if (packet instanceof OpponentFound) {
            if (activity instanceof InGameActivity) {
                suspiciousEvents++;

                return;
            }
            OpponentFound opponentFound = (OpponentFound) packet;
            opponentDisplayName = opponentFound.opponentDisplayName;
            boardSize = opponentFound.boardSize;
            secondsForTurn = opponentFound.secondsForTurn;
            noMoveDrawThreshold = opponentFound.noMoveDrawThreshold;
            ageMillis = opponentFound.ageMillis;

            instantiateBoard();
            board.setMoveNumber(opponentFound.moveNumber);
            board.setSubMoveNumber(opponentFound.subMoveNumber);

            whiteColor = opponentFound.whiteColor;
            myTurnNow = opponentFound.myTurnNow;
            if (opponentFound.lastChanceActivated) board.setLastChanceActivated();

            board.setWhitesTurn(whiteColor == myTurnNow);

            board.deserialize(whiteColor, opponentFound.myPiecePositions);
            board.deserialize(!whiteColor, opponentFound.opponentPiecePositions);

            Helper.startActivity(activity, InGameActivity.class);

            inGameActivityStartTimestamp = System.currentTimeMillis();
        } else if (packet instanceof OpponentNotFound) {
            if (activity instanceof InGameActivity) {
                suspiciousEvents++;

                return;
            }
            Helper.startActivity(activity, HomeMenuActivity.class);

            if (activity instanceof MatchmakingActivity &&
                    ((MatchmakingActivity) activity).hasRequestedToCancelMatchmaking()) {
                Toast.makeText(activity,
                        R.string.matchmaking_cancelled_text, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(activity, R.string.opponent_not_found, Toast.LENGTH_LONG).show();
            }
        } else if (packet instanceof DiceRolled) {
            if (board == null) {
                suspiciousEvents++;

                return;
            }
            DiceRolled diceRolled = (DiceRolled) packet;
            int value = diceRolled.value;
            if (value < 1 || value > 6) {
                Helper.disconnect(activity, null, R.string.dice_bad_value_text);

                return;
            }
            board.diceRolled(diceRolled.value);
        } else if (packet instanceof MakeMove) {
            if (board == null) {
                suspiciousEvents++;

                return;
            }
            MakeMove makeMove = (MakeMove) packet;

            board.makeMove(makeMove, makeMove.whitesMove);
        } else { // this is a Resign packet
            if (resignsReceived >= 2 || board == null) {
                suspiciousEvents++;

                return;
            }
            if (InGameActivity.resigned) {
                board.resign(whiteColor);

                InGameActivity.resigned = false;
            } else {
                board.resign(!whiteColor);
            }

            resignsReceived++;
        }
    }

    void instantiateBoard() {
        board = new Board(
                true,
                random,
                boardSize,
                TimeUnit.SECONDS.toMillis(secondsForTurn),
                noMoveDrawThreshold,
                guaranteeRollOf6
        );

        gameStartTimestamp = Math.max(System.currentTimeMillis() - ageMillis, 0L);
    }

    void reset() {
        messageOfTheDay = "<not present>";
        displayName = "You";
        onlinePlayerCount = 0;
        activeGamesCount = 0;
        lastTimeReceivedHomeData = 0L;
        identified = false;
        captcha = null;
        suspiciousEvents = 0;
        noActivity = false;
        invitationCode = null;
        ageMillis = 0L;
        gameStartTimestamp = 0L;
        singleplayer = false;
        opponentDisplayName = null;
        boardSize = 0;
        secondsForTurn = 0;
        guaranteeRollOf6 = false;
        board = null;
        whiteColor = false;
        myTurnNow = false;
        resignsReceived = 0;

        DiceView.reset();
        Singleplayer.reset();
        MovementAnimationManager.getInstance().reset();
        InGameActivity.resigned = false;
    }

    public void setMessageOfTheDay(String messageOfTheDay) {
        this.messageOfTheDay = messageOfTheDay;
    }

    public String getMessageOfTheDay() {
        return messageOfTheDay;
    }
}
