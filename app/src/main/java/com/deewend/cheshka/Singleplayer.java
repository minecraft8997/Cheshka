package com.deewend.cheshka;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Singleplayer {
    public static final int NO_MOVE_DRAW_THRESHOLD = 16;
    public static final int MODE_NORMAL = 0;
    public static final int MODE_HARD = 1;
    public static final int MODE_NOTICEABLY_HARD = 2;

    private static boolean whiteColor; // bot's color
    private static int mode;
    private static boolean preMoveTimeout;
    private static boolean playerHelpTimeout;
    private static long timeoutSince;
    private static long timeoutDuration;
    private static Board board;
    private static List<Board.PossibleMove> possibleMoves;

    private Singleplayer() {
    }

    public static void init(
            CheshkaActivity context,
            int boardSize,
            int mode,
            boolean shouldAddInitialPieces,
            boolean guaranteeRollOf6,
            Boolean whiteColor
    ) {
        PacketHandler handler = PacketHandler.getInstance();
        handler.boardSize = boardSize;
        handler.noMoveDrawThreshold = NO_MOVE_DRAW_THRESHOLD;
        handler.guaranteeRollOf6 = guaranteeRollOf6;
        handler.instantiateBoard();
        init0(context, (whiteColor == null ? handler.random.nextBoolean() : !whiteColor), mode);
        if (shouldAddInitialPieces) {
            Board board = PacketHandler.getInstance().board;
            List<Board.Piece> pieces = board.getPieces();
            pieces.add(new Board.Piece(true));
            pieces.add((new Board.Piece(false))
                    .setPosition(board.getBlacksSpawnPosition()));
        }

        Helper.startActivity(context, InGameActivity.class);
    }

    private static void init0(CheshkaActivity context, boolean whiteColor, int mode) {
        PacketHandler handler = PacketHandler.getInstance();
        Singleplayer.mode = mode;
        handler.singleplayer = true;
        handler.displayName = context.getString(R.string.you_text);
        handler.opponentDisplayName = context.getString(R.string.bot_text);
        handler.secondsForTurn = 0;
        handler.whiteColor = !whiteColor;
        Singleplayer.whiteColor = whiteColor;
    }

    public static void tick(InGameActivity activity) {
        PacketHandler handler = PacketHandler.getInstance();
        Board board = handler.board;
        if (board.getGameState() != Board.GAME_STATE_RUNNING) return;

        if (System.currentTimeMillis() < (timeoutSince + timeoutDuration)) return;

        if (isPlayersTurn()) {
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
            if (possibleMoves.isEmpty() || mode == MODE_NORMAL) {
                board.makeRandomMove();

                return;
            }
            if (takePieceIfPossible()) return;
            if (spawnIfPossible()) return;
            /*
             * Putting spawnIfPossible() check below any of the following 3 checks
             * could potentially result in an NPE since they don't count possibility
             * of move.getPiece() being null if it's a spawning move. Is it worth fixing?
             */
            if (movePieceOnDiagonalIfPossible()) return;
            if (leaveSpawnPointIfPossible()) return;
            if (movePieceWithinDiagonalIfPossible()) return;
            if (doNotStepPastOpponentsSpawnPointIfEffective()) return;

            makeAnyMoveTryToAvoidBadOnes();
        } finally {
            Singleplayer.board = null;
            possibleMoves = null;
        }
    }

    private static boolean takePieceIfPossible() {
        Board.PossibleMove goodMove = null;
        for (Board.PossibleMove move : possibleMoves) {
            Board.Piece target = move.getTarget();
            if (target == null) continue;

            if (move.isSpawningMove()) {
                goodMove = move;

                break;
            }
            if (goodMove == null) {
                goodMove = move;
            }
            target.computeRealPosition(board);

            if (goodMove.getPiece().getLastRealPosition() < target.getLastRealPosition()) {
                goodMove = move;
            }
        }

        return makeMoveIfPresent(goodMove);
    }

    private static boolean spawnIfPossible() {
        for (Board.PossibleMove move : possibleMoves) {
            if (move.isSpawningMove()) return makeMoveIfPresent(move);
        }

        return false;
    }

    private static boolean movePieceOnDiagonalIfPossible() {
        Board.PossibleMove goodMove = null;
        for (Board.PossibleMove move : possibleMoves) {
            int position = move.getPiece().getPosition();
            int destination = move.getDestination();
            int whitesDiagonalStart = board.getWhitesDiagonalStart();
            if (position <= whitesDiagonalStart && destination > whitesDiagonalStart) {
                /*
                 * Board#blacksDiagonalStartPlusOne is always greater than
                 * whitesDiagonalStart, so we're free to perform the comparison above.
                 */
                goodMove = move;

                break;
            }
        }

        return makeMoveIfPresent(goodMove);
    }

    private static boolean leaveSpawnPointIfPossible() {
        Board.PossibleMove goodMove = null;
        for (Board.PossibleMove move : possibleMoves) {
            int position = move.getPiece().getPosition();
            Pair<Boolean, Boolean> spawnCheckResult = checkSpawnPoint(position);
            boolean spawnPoint = spawnCheckResult.first;
            if (spawnPoint) {
                goodMove = move;

                boolean whitesSpawn = spawnCheckResult.second;
                if (whiteColor != whitesSpawn) break;
            }
        }

        return makeMoveIfPresent(goodMove);
    }

    private static boolean movePieceWithinDiagonalIfPossible() {
        Board.PossibleMove goodMove = null;
        for (Board.PossibleMove move : possibleMoves) {
            Board.Piece piece = move.getPiece();
            piece.computeRealPosition(board);

            int realPosition = piece.getLastRealPosition();
            if (realPosition <= board.getWhitesDiagonalStart()) continue;

            if (goodMove == null) {
                goodMove = move;
            }
            if (goodMove.getPiece().getLastRealPosition() < realPosition) {
                goodMove = move;
            }
        }

        return makeMoveIfPresent(goodMove);
    }

    private static boolean doNotStepPastOpponentsSpawnPointIfEffective() {
        for (Board.Piece piece : board.getPieces()) {
            piece.computeRealPosition(board);

            if (piece.isWhitePiece() == whiteColor) continue;

            if (piece.getLastRealPosition() <= board.getWhitesDiagonalStart()) return false;
        }
        for (Board.Piece piece : board.getPieces()) {
            if (piece.isWhitePiece() != whiteColor) continue;

            // we do not need to compute "real position", was already done before
            if (piece.getLastRealPosition() >= board.getBlacksSpawnPosition()) return false;
        }
        Board.PossibleMove goodMove = null;
        for (Board.PossibleMove move : possibleMoves) {
            Board.Piece piece = move.getPiece();

            if (goodMove == null) {
                goodMove = move;
            }
            if (goodMove.getPiece().getLastRealPosition() > piece.getLastRealPosition()) {
                goodMove = move;
            }
        }

        return makeMoveIfPresent(goodMove);
    }

    private static boolean makeMoveIfPresent(Board.PossibleMove move) {
        if (move != null) {
            board.makeMove(move, true);

            return true;
        }

        return false;
    }

    private static void makeAnyMoveTryToAvoidBadOnes() {
        List<Board.PossibleMove> goodMoves = new ArrayList<>();
        for (Board.PossibleMove move : possibleMoves) {
            int destination = move.getDestination();

            if (!checkSpawnPoint(destination).first) goodMoves.add(move);
        }
        if (goodMoves.isEmpty()) {
            board.makeRandomMove();

            return;
        }
        int randomIdx = PacketHandler.getInstance().random.nextInt(goodMoves.size());
        Board.PossibleMove move = goodMoves.get(randomIdx);

        board.makeMove(move, true);
    }

    private static Pair<Boolean, Boolean> checkSpawnPoint(int position) {
        boolean whitesSpawn = (position == 0 || position == board.getWhitesDiagonalStart());
        boolean spawnPoint = (whitesSpawn || position == board.getBlacksSpawnPosition());

        return new Pair<>(spawnPoint, whitesSpawn);
    }

    private static void timeout(long durationMillis) {
        timeoutSince = System.currentTimeMillis();
        timeoutDuration = durationMillis;
    }

    public static boolean isBotWhiteColor() {
        return whiteColor;
    }

    public static boolean isNoticeablyHard() {
        return (mode == MODE_NOTICEABLY_HARD);
    }

    public static boolean isPlayersTurn() {
        return whiteColor != PacketHandler.getInstance().board.isWhitesTurn();
    }

    public static void reset() {
        whiteColor = false;
        mode = MODE_NORMAL;
        preMoveTimeout = false;
        playerHelpTimeout = false;
        timeoutSince = 0L;
        timeoutDuration = 0L;
    }

    public static String _toString(boolean optimized) {
        return "Singleplayer{" +
                "whiteColor=" + whiteColor +
                ", mode=" + mode +
                (optimized ? "" : ", preMoveTimeout=" + preMoveTimeout) +
                (optimized ? "" : ", playerHelpTimeout=" + playerHelpTimeout) +
                (optimized ? "" : ", timeoutSince=" + timeoutSince) +
                (optimized ? "" : ", timeoutDuration=" + timeoutDuration) +
                ", handler.gameStartTimestamp=" + PacketHandler.getInstance().gameStartTimestamp +
                ", InGameActivity.resigned=" + InGameActivity.resigned +
                '}';
    }

    public static void deserialize(CheshkaActivity context, String str) {
        Properties props = Helper.objStringToData(str, "Singleplayer").first();

        boolean whiteColor = Boolean.parseBoolean(props.getProperty("whiteColor"));
        int mode = Integer.parseInt(props.getProperty("mode"));
        long gameStartTimestamp = Long.parseLong(props.getProperty("handler.gameStartTimestamp"));
        boolean resigned = Boolean.parseBoolean(props.getProperty("InGameActivity.resigned"));

        init0(context, whiteColor, mode);
        PacketHandler.getInstance().gameStartTimestamp = gameStartTimestamp;
        InGameActivity.resigned = resigned;
    }
}
