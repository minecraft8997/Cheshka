package ru.deewend.cheshka;

import java.util.List;

public class Singleplayer {
    public static final int NO_MOVE_DRAW_THRESHOLD = 24;

    private static boolean whiteColor;
    private static boolean hardMode;
    private static boolean preMoveTimeout;
    private static boolean playerHelpTimeout;
    private static long timeoutSince;
    private static long timeoutDuration;
    private static Board board;
    private static List<Board.PossibleMove> possibleMoves;

    private Singleplayer() {
    }

    public static void init(CheshkaActivity context, int boardSize, boolean hardMode) {
        Singleplayer.hardMode = hardMode;

        PacketHandler handler = PacketHandler.getInstance();
        handler.singleplayer = true;
        handler.opponentDisplayName = context.getString(R.string.bot_text);
        handler.boardSize = boardSize;
        handler.secondsForTurn = 0;
        handler.noMoveDrawThreshold = NO_MOVE_DRAW_THRESHOLD;
        handler.whiteColor = handler.random.nextBoolean();
        whiteColor = !handler.whiteColor;
        handler.instantiateBoard();

        Helper.startActivity(context, InGameActivity.class);
    }

    public static void tick(InGameActivity activity) {
        PacketHandler handler = PacketHandler.getInstance();
        Board board = handler.board;
        if (board.getGameState() != Board.GAME_STATE_RUNNING) return;

        if (System.currentTimeMillis() < (timeoutSince + timeoutDuration)) return;

        if (whiteColor != board.isWhitesTurn()) {
            if (board.isDiceRolled() && !activity.isDiceRolling() && board.getPossibleMoves().isEmpty()) {
                if (!playerHelpTimeout) {
                    timeout(1250L + handler.random.nextInt(150));
                    playerHelpTimeout = true;

                    return;
                }
                board.makeRandomMove(); // make a NoMove for the real player

                playerHelpTimeout = false;
            }

            return;
        }
        if (!board.isDiceRolled()) {
            if (!preMoveTimeout) {
                timeout(950L + handler.random.nextInt(200));
                preMoveTimeout = true;

                return;
            }
            board.rollDice();
            timeout(2000L + handler.random.nextInt(300));

            return;
        }
        makeMove(board);

        preMoveTimeout = false;
    }

    private static void makeMove(Board board) {
        Singleplayer.board = board;
        possibleMoves = board.getPossibleMoves();
        try {
            if (possibleMoves.isEmpty() || !hardMode) {
                board.makeRandomMove();

                return;
            }
            if (takePieceIfPossible()) return;
            if (spawnIfPossible()) return;
            if (leaveSpawnPointIfPossible()) return;

            board.makeRandomMove();
        } finally {
            Singleplayer.board = null;
            possibleMoves = null;
        }
    }

    private static boolean takePieceIfPossible() {
        Board.Piece mostValuableTarget = null;
        Board.PossibleMove bestMove = null;
        for (Board.PossibleMove move : possibleMoves) {
            Board.Piece target = move.getTarget();
            if (target == null) continue;

            if (mostValuableTarget == null) {
                mostValuableTarget = target;
                bestMove = move;
            }
            target.computeRealPosition(board);

            if (mostValuableTarget.getLastRealPosition() < target.getLastRealPosition()) {
                mostValuableTarget = target;
                bestMove = move;
            }
        }
        if (bestMove != null) {
            board.makeMove(bestMove, true);

            return true;
        }

        return false;
    }

    private static boolean spawnIfPossible() {
        for (Board.PossibleMove move : possibleMoves) {
            if (move.isSpawningMove()) {
                board.makeMove(move, true);

                return true;
            }
        }

        return false;
    }

    private static boolean leaveSpawnPointIfPossible() {
        for (Board.PossibleMove move : possibleMoves) {
            int position = move.getPiece().getPosition();
            if (
                    position == 0 ||
                    position == board.getBlacksSpawnPosition() ||
                    position == board.getWhitesDiagonalStart()
            ) { // this works fine regardless the bot's color
                board.makeMove(move, true);

                return true;
            }
        }

        return false;
    }

    private static void timeout(long durationMillis) {
        timeoutSince = System.currentTimeMillis();
        timeoutDuration = durationMillis;
    }

    public static void reset() {
        whiteColor = false;
        hardMode = false;
        preMoveTimeout = false;
        playerHelpTimeout = false;
        timeoutSince = 0L;
        timeoutDuration = 0L;
    }
}
