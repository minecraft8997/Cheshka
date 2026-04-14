package com.deewend.cheshka;

import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.deewend.cheshka.packet.MakeMove;
import com.deewend.cheshka.packet.Resign;
import com.deewend.cheshka.packet.RollDice;

public class InGameActivity extends CheshkaActivity {
    private static final String[] DICE_ROLL_SOUNDS = {
            "zapsplat_dice_roll_1.mp3", "zapsplat_dice_roll_2.mp3", "zapsplat_dice_roll_4.mp3"
    };
    private static final String[] PIECE_SOUNDS = {
            "zapsplat_piece_1.mp3", "zapsplat_piece_4.mp3"
    };

    static boolean resigned;

    private GamePreferences preferences;
    private MediaPlayer[] diceRollPlayers;
    private MediaPlayer[] piecePlayers;
    private DiceView diceView;
    private boolean disableSounds;
    private boolean playerNamesViewSet;
    private boolean resignButtonRenamed;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = Cheshka.getInstance(this).getPreferences();
        disableSounds = !preferences.shouldEnableSounds();

        setContentView(R.layout.activity_in_game);

        restoreButton(R.id.roll_dice_button, savedInstanceState);
        restoreButton(R.id.place_checker_button, savedInstanceState);
        restoreButton(R.id.resign_button, savedInstanceState);

        diceRollPlayers = new MediaPlayer[DICE_ROLL_SOUNDS.length];
        piecePlayers = new MediaPlayer[PIECE_SOUNDS.length];
        loopOver(diceRollPlayers, DICE_ROLL_SOUNDS, true);
        loopOver(piecePlayers, PIECE_SOUNDS, true);

        runCheshkaViews(R.id.dice_view, R.id.board_view);
        if (preferences.shouldEnableEvalBar()) {
            findViewById(R.id.eval_bar_view).setVisibility(View.VISIBLE);

            runCheshkaViews(R.id.eval_bar_view);
        }

        diceView = findViewById(R.id.dice_view);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (PacketHandler.getInstance().board == null) {
            Helper.startActivity(this, );
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        disableSounds = true;
        loopOver(diceRollPlayers, null, false);
        loopOver(piecePlayers, null, false);
    }

    private void loopOver(MediaPlayer[] array, String[] filenames, boolean init) {
        for (int i = 0; i < array.length; i++) {
            MediaPlayer player;
            if (!init) {
                player = array[i];
                if (player != null) player.release();

                continue;
            }
            player = new MediaPlayer();
            try (AssetFileDescriptor fd = getAssets().openFd(filenames[i])) {
                player.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
                player.prepare();
            } catch (IOException e) {
                Toast.makeText(this, R.string.io_issue_text, Toast.LENGTH_LONG).show();
                disableSounds = true;

                return;
            }

            array[i] = player;
        }
    }

    public void playDiceRollSound() {
        playSound(diceRollPlayers);
    }

    public void playMoveSound() {
        playSound(piecePlayers);
    }

    private void playSound(MediaPlayer[] players) {
        if (disableSounds || players == null) return;

        PacketHandler handler = PacketHandler.getInstance();
        MediaPlayer player = players[handler.random.nextInt(players.length)];

        if (player != null) player.start();
    }

    public void renderTick() {
        TextView statusView = findViewById(R.id.game_status_text);
        PacketHandler handler = PacketHandler.getInstance();
        Board board = handler.board;

        boolean showPlaytime = preferences.shouldShowPlaytime();
        String displayName = handler.displayName;
        String opponentDisplayName = handler.opponentDisplayName;
        if (!showPlaytime && !playerNamesViewSet) {
            ((TextView) findViewById(R.id.player_names_view)).setText(getString(
                    R.string.vs_text, displayName, opponentDisplayName, ""));

            playerNamesViewSet = true;
        }
        byte gameState;
        if ((gameState = board.getGameState()) != Board.GAME_STATE_RUNNING) {
            if (!resignButtonRenamed) {
                findViewById(R.id.roll_dice_button).setEnabled(false);
                findViewById(R.id.place_checker_button).setEnabled(false);

                ((Button) findViewById(R.id.resign_button))
                        .setText(getString(R.string.goto_main_menu_text));

                resignButtonRenamed = true;
            }

            if (gameState == Board.GAME_STATE_DRAW) {
                if (handler.resignsReceived >= 2) {
                    statusView.setText(R.string.game_finished_both_resigned_text);
                } else {
                    statusView.setText(R.string.game_finished_draw_text);
                }
            } else {
                if ((gameState == Board.GAME_STATE_WHITE_WON) == handler.whiteColor) {
                    if (handler.resignsReceived > 0) {
                        statusView.setText(R.string.game_finished_opponent_resigned_text);
                    } else {
                        statusView.setText(R.string.game_finished_you_text);
                    }
                } else {
                    if (resigned) {
                        statusView.setText(R.string.game_finished_you_resigned_text);
                    } else {
                        statusView.setText(R.string.game_finished_opponent_text);
                    }
                }
            }

            return;
        }
        boolean myTurn = (handler.whiteColor == board.isWhitesTurn());
        String whoseTurn = (myTurn ?
                getString(R.string.your_turn_fragment) : getString(R.string.opponents_turn_fragment)
        );

        int secondsForTurn = handler.secondsForTurn;
        boolean renderingDiceStill = !isDiceRolling();
        boolean logicalDiceRolled = board.isDiceRolled();
        boolean noMoves = (logicalDiceRolled && board.getPossibleMoves().isEmpty());
        if (noMoves && renderingDiceStill) secondsForTurn /= 2;

        long deltaSeconds = TimeUnit.MILLISECONDS
                .toSeconds(System.currentTimeMillis() - board.getLastActionTimestamp());
        long secondsLeft = secondsForTurn - deltaSeconds;
        if (secondsLeft > secondsForTurn) secondsLeft = secondsForTurn;
        if (secondsLeft < 0L) secondsLeft = 0L;

        int statusResId = (handler.singleplayer ?
                R.string.singleplayer_game_status_text : R.string.game_status_text);
        String statusText = getString(statusResId, whoseTurn, secondsLeft);
        if (noMoves && renderingDiceStill) {
            statusText += ", " + getString(R.string.no_moves_text);
        }
        int opponentsAutomaticMoveCount;
        if (handler.whiteColor) opponentsAutomaticMoveCount = board.getBlacksAutomaticMoveCount();
        else                    opponentsAutomaticMoveCount = board.getWhitesAutomaticMoveCount();
        if (opponentsAutomaticMoveCount >= 2 && !handler.singleplayer) {
            statusText += ". " + getString(R.string.opponent_is_afk);
        }

        statusView.setText(statusText);

        if (showPlaytime) {
            long elapsed = System.currentTimeMillis() - handler.gameStartTimestamp;
            String timePrefix = " | " + Helper.calculatedTimeElapsed(elapsed);

            ((TextView) findViewById(R.id.player_names_view)).setText(getString(
                    R.string.vs_text, displayName, opponentDisplayName, timePrefix));
        }

        boolean enableDiceButton = (myTurn && !logicalDiceRolled);
        boolean enableCheckerButton = (myTurn && renderingDiceStill && board.getSpawningMove() != null);
        findViewById(R.id.roll_dice_button).setEnabled(enableDiceButton);
        findViewById(R.id.place_checker_button).setEnabled(enableCheckerButton);

        Helper.serializeGame(preferences);
    }

    @Override
    protected byte onClick(int id, Button button) {
        PacketHandler handler = PacketHandler.getInstance();
        Board board = handler.board;
        if (id != R.id.resign_button && board.getGameState() != Board.GAME_STATE_RUNNING) {
            toast(R.string.game_already_ended_text);

            return DO_NOT_DISABLE;
        }

        if (id == R.id.roll_dice_button) {
            if (handler.singleplayer) {
                if (handler.whiteColor == board.isWhitesTurn()) board.rollDice();
            } else {
                NetworkingThread.staticSend(new RollDice());
            }
        } else if (id == R.id.place_checker_button) {
            if (board.isWhitesTurn() == handler.whiteColor) {
                Board.PossibleMove spawningMove = board.getSpawningMove();
                MakeMove makeMove = board.makeMove(spawningMove, false);

                if (makeMove != null && !handler.singleplayer) {
                    NetworkingThread.staticSend(makeMove);
                }
            }
        } else {
            if (resignButtonRenamed) {
                if (handler.singleplayer) {
                    handler.reset();

                    Helper.startActivity(this, LauncherActivity.class);
                } else {
                    Helper.startActivity(this, HomeMenuActivity.class);
                }
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.resign_title_text);
                builder.setPositiveButton(R.string.confirm_text, (dialog, which) -> {
                    resigned = true;
                    if (handler.singleplayer) board.resign(handler.whiteColor);
                    else                      NetworkingThread.staticSend(new Resign());
                });
                Helper.defaultNegativeButton(builder);

                builder.create().show();
            }
        }

        return DO_NOT_DISABLE;
    }

    public DiceView getDiceView() {
        return diceView;
    }

    public boolean isDiceRolling() {
        return diceView.isRolling();
    }

    private void toast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }

    public GamePreferences getPreferences() {
        return preferences;
    }
}
