package ru.deewend.cheshka;

public class Singleplayer {
    public static final int NO_MOVE_DRAW_THRESHOLD = 24;

    private static boolean whiteColor;
    private static boolean preMoveTimeout;
    private static boolean playerHelpTimeout;
    private static long timeoutSince;
    private static long timeoutDuration;

    private Singleplayer() {
    }

    public static void init(CheshkaActivity context, int boardSize) {
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

    public static void tick() {
        PacketHandler handler = PacketHandler.getInstance();
        Board board = handler.board;
        if (board.getGameState() != Board.GAME_STATE_RUNNING) return;

        if (System.currentTimeMillis() < (timeoutSince + timeoutDuration)) return;

        if (whiteColor != board.isWhitesTurn()) {
            if (board.isDiceRolled() && board.getPossibleMoves().isEmpty()) {
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
                timeout(2000L + handler.random.nextInt(200));
                preMoveTimeout = true;

                return;
            }
            board.rollDice();
            timeout(1800L + handler.random.nextInt(300));

            return;
        }
        board.makeRandomMove();

        preMoveTimeout = false;
    }

    private static void timeout(long durationMillis) {
        timeoutSince = System.currentTimeMillis();
        timeoutDuration = durationMillis;
    }

    public static void reset() {
        whiteColor = false;
        preMoveTimeout = false;
        playerHelpTimeout = false;
        timeoutSince = 0L;
        timeoutDuration = 0L;
    }
}
