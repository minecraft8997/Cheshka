package ru.deewend.cheshka;

import android.util.Pair;

import androidx.annotation.NonNull;

import ru.deewend.cheshka.packet.DiceRolled;
import ru.deewend.cheshka.packet.MakeMove;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

public class Board {
    @SuppressWarnings("unused")
    public static class Piece implements Comparable<Piece> {
        private final boolean whitePiece;
        private int position;
        private boolean revertedPosition;
        private int lastRealPosition;

        public Piece(boolean whitePiece) {
            this.whitePiece = whitePiece;
        }

        /*
         * Make sure to call computeRealPosition() on all
         * pieces that are going to be compared between each other.
         */
        @Override
        public int compareTo(Piece o) {
            return lastRealPosition - o.lastRealPosition;
        }

        public boolean isWhitePiece() {
            return whitePiece;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            if (position < this.position) revertedPosition = true;

            this.position = position;
        }

        public boolean hasRevertedPosition() {
            return revertedPosition;
        }

        public void computeRealPosition(Board board) {
            if (whitePiece) {
                lastRealPosition = position;

                return;
            }
            int realPosition = position;
            if (realPosition >= board.blacksDiagonalStartPlusOne) {
                realPosition -= board.diagonalLength - 1;
            } else {
                if (revertedPosition) realPosition += 2 * board.blacksSpawnPosition;

                realPosition -= board.blacksSpawnPosition;
            }

            this.lastRealPosition = realPosition;
        }

        public int getLastRealPosition() {
            return lastRealPosition;
        }

        public boolean isTurningOntoDiagonalLine(Board board) {
            if (board.getGameState() != GAME_STATE_RUNNING) return false;

            computeRealPosition(board);

            return (lastRealPosition == board.getWhitesDiagonalStart());
        }

        @NonNull
        @Override
        public String toString() {
            return "Piece{" +
                    "whitePiece=" + whitePiece +
                    ", position=" + position +
                    ", revertedPosition=" + revertedPosition +
                    '}';
        }
    }

    @SuppressWarnings("unused")
    public class PossibleMove {
        public static final int NEW_PIECE = -1;

        private final Piece piece;
        private final int destination;

        private PossibleMove(Piece piece, int destination) {
            this.piece = piece;
            this.destination = destination;
        }

        public void makeMove() {
            Piece target = getTarget();
            if (target != null) pieces.remove(target);

            if (isSpawningMove()) {
                Piece piece = new Piece(whitesTurn);
                piece.setPosition(getSpawnPosition());

                pieces.add(piece);
            } else {
                int oldPosition = piece.getPosition();
                piece.setPosition(destination);

                MutablePair<Integer, Integer> coordinates = new MutablePair<>();
                getRenderingXY(oldPosition, coordinates);
                int x1 = coordinates.first;
                int y1 = coordinates.second;
                getRenderingXY(destination, coordinates);
                int x2 = coordinates.first;
                int y2 = coordinates.second;

                MovementAnimationManager
                        .getInstance().registerAnimation(x1, y1, x2, y2, piece.whitePiece);
            }
        }

        public Piece getTarget() {
            Piece target = null;
            for (Piece piece : pieces) {
                boolean guestOnWhiteSide = (piece.position == 0 && destination == whitesDiagonalStart);
                boolean waitingWhitePiece = (piece.position == whitesDiagonalStart && destination == 0);
                if (piece.position == destination || waitingWhitePiece || guestOnWhiteSide) {
                    target = piece;

                    break;
                }
            }

            return target;
        }

        public byte getMoveType() {
            return (isSpawningMove() ? MakeMove.MOVE_TYPE_SPAWNING : MakeMove.MOVE_TYPE_GENERAL);
        }

        public boolean isSpawningMove() {
            return piece == null;
        }

        public Piece getPiece() {
            return piece;
        }

        public int getDestination() {
            return destination;
        }

        @NonNull
        @Override
        public String toString() {
            return "PossibleMove{" +
                    "piece=" + piece +
                    ", destination=" + destination +
                    '}';
        }
    }

    public class NoMove extends PossibleMove {
        private NoMove() {
            super(null, 0);
        }

        @Override
        public void makeMove() {
            // do nothing
        }

        public byte getMoveType() {
            return MakeMove.MOVE_TYPE_NO_MOVE;
        }
    }

    public static final byte GAME_STATE_RUNNING = 0;
    public static final byte GAME_STATE_WHITE_WON = 1;
    public static final byte GAME_STATE_BLACK_WON = 2;
    public static final byte GAME_STATE_DRAW = 3;

    /*
     * Used only when "guaranteeRollOf6" field is set to true.
     */
    public static final int SKIPPED_MOVES_BEFORE_FORCED_ROLL_OF_6 = 10;

    private final boolean clientside;
    private final Random random;
    private final long turnWaitingTimeoutMillis;
    private final int noMoveDrawThreshold;
    private final boolean guaranteeRollOf6;
    private final int diagonalLength;
    private final int whitesDiagonalStart;
    private final int blacksSpawnPosition;
    private final int blacksDiagonalStartPlusOne;
    private final List<Piece> pieces = new ArrayList<>();
    private boolean allowVaults;
    private int moveNumber = 1;
    private int subMoveNumber = 1;
    private boolean whitesTurn = true;
    private int lastCalculatedDestination;
    private Pair<Integer, List<PossibleMove>> lastDiceRollResult;
    private int noMovesCounter;
    private long lastActionTimestamp = System.currentTimeMillis();
    private byte gameState;
    private boolean resigned;
    private boolean lastChance;
    private boolean lastChanceActivated;
    private int whitesAutomaticMoveCount;
    private int blacksAutomaticMoveCount;
    private boolean rolled6NoticeablyHard;
    private boolean doNotReRoll6NoticeablyHard = true;
    private int whitesSkippedMovesGuarantee6;
    private int blacksSkippedMovesGuarantee6;

    public Board(
            boolean clientside,
            Random random,
            int boardSize,
            long turnWaitingTimeoutMillis,
            int noMoveDrawThreshold,
            boolean guaranteeRollOf6
    ) {
        if (boardSize <= 0 || boardSize % 2 != 0) {
            throw new IllegalArgumentException("Bad boardSize");
        }

        this.clientside = clientside;
        this.random = random;
        this.turnWaitingTimeoutMillis = turnWaitingTimeoutMillis;
        this.noMoveDrawThreshold = noMoveDrawThreshold;
        this.guaranteeRollOf6 = guaranteeRollOf6;
        diagonalLength = boardSize / 2;
        whitesDiagonalStart = (boardSize - 1) * 4;
        blacksSpawnPosition = whitesDiagonalStart / 2;
        blacksDiagonalStartPlusOne = whitesDiagonalStart + diagonalLength;
    }

    private static InGameActivity getInGameActivity() {
        CheshkaActivity activity = NetworkingThread.getCurrentActivity();

        return (activity instanceof InGameActivity ? (InGameActivity) activity : null);
    }

    public int randomDigit() {
        int digit = trulyRandomDigit();
        boolean noticeablyHard =
                (PacketHandler.getInstance().singleplayer && Singleplayer.isNoticeablyHard());
        if (noticeablyHard && Singleplayer.isPlayersTurn()) {
            rollDice0(digit);
            boolean rollAgain = false;
            for (PossibleMove move : getPossibleMoves()) {
                if (move.isSpawningMove() && random.nextInt(4) == 0) {
                    if (doNotReRoll6NoticeablyHard) continue;

                    rollAgain = true;
                } else if (move.getTarget() != null && random.nextInt(2) == 0) {
                    rollAgain = true;
                }

                if (rollAgain) break;
            }
            if (rollAgain) {
                digit = trulyRandomDigit();
                lastDiceRollResult = null;
            }
            /*
             * Not setting lastDiceRollResult to null means that right after the digit
             * value is returned and rollDice0 is called, the method will immediately return.
             * This works weird but is exactly what we need.
             */

            /*
             * Let the player roll 6 just by pure random, but only for the first time.
             */
            if (digit == 6) doNotReRoll6NoticeablyHard = false;
        } else if (noticeablyHard && !rolled6NoticeablyHard) {
            digit = 6;

            rolled6NoticeablyHard = true;
        }
        if (guaranteeRollOf6) {
            /*
             * Expecting noticeablyHard == false (but
             * allowing modes to be combined with !load command).
             */
            int skippedMoves = (whitesTurn ? whitesSkippedMovesGuarantee6 : blacksSkippedMovesGuarantee6);
            if (skippedMoves >= SKIPPED_MOVES_BEFORE_FORCED_ROLL_OF_6) {
                if (whitesTurn) whitesSkippedMovesGuarantee6 = 0;
                else            blacksSkippedMovesGuarantee6 = 0;

                digit = 6;
            }
        }

        return digit;
    }

    private int trulyRandomDigit() {
        return 1 + random.nextInt(6);
    }

    public String serializePosition(boolean white) {
        StringBuilder builder = new StringBuilder();
        for (Piece piece : pieces) {
            if (piece.whitePiece == white) {
                builder.append(piece.position);
                if (piece.revertedPosition && piece.position == blacksSpawnPosition) {
                    builder.append('!');
                }
                builder.append(' ');
            }
        }

        return builder.substring(0, builder.length() - 1); // omitting the last space character
    }

    /** @noinspection UnusedReturnValue*/
    public boolean deserialize(boolean white, String position) {
        List<Piece> tmpList = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(position);
        while (tokenizer.hasMoreTokens()) {
            String piecePosition = tokenizer.nextToken();
            boolean revertedPosition = piecePosition.endsWith("!");
            if (revertedPosition) {
                piecePosition = piecePosition.substring(0, piecePosition.length() - 1);
            }
            Piece piece = new Piece(white);
            try {
                piece.setPosition(Integer.parseInt(piecePosition));
            } catch (NumberFormatException e) {
                return false;
            }
            piece.revertedPosition = revertedPosition;

            tmpList.add(piece);
        }
        pieces.addAll(tmpList);

        return true;
    }

    public void getRenderingXY(int position, MutablePair<Integer, Integer> writeTo) {
        int boardSizeM1 = whitesDiagonalStart / 4;

        if (position > whitesDiagonalStart) {
            if (position >= blacksDiagonalStartPlusOne) {
                int offset = (position - blacksDiagonalStartPlusOne) + 1;

                writeTo.first = boardSizeM1 - offset;
                writeTo.second = offset;
            } else {
                int offset = position - whitesDiagonalStart;

                writeTo.first = offset;
                writeTo.second = boardSizeM1 - offset;
            }
        } else {
            if (position > blacksSpawnPosition) {
                int middle = (whitesDiagonalStart + blacksSpawnPosition) / 2;
                if (position > middle) {
                    writeTo.first = boardSizeM1 - (position - middle); // x
                    writeTo.second = boardSizeM1;
                } else {
                    writeTo.first = boardSizeM1;
                    writeTo.second = boardSizeM1 - (middle - position);
                }
            } else {
                int middle = blacksSpawnPosition / 2;
                if (position > middle) {
                    writeTo.first = position - boardSizeM1;
                    writeTo.second = 0;
                } else {
                    writeTo.first = 0;
                    writeTo.second = boardSizeM1 - position;
                }
            }
        }
        /*
         * From every player's perspective their spawn
         * position should be in the edge cell on bottom left.
         */
        if (!PacketHandler.getInstance().whiteColor) {
            writeTo.first = boardSizeM1 - writeTo.first;
            writeTo.second = boardSizeM1 - writeTo.second;
        }
    }

    /*
     * Returns the approximate chance for the whites to win.
     * Blacks' chance can be computed with <code>1.0 - evalBar()</code>.
     *
     * In the current implementation, the chance is
     * determined by simply counting remaining moves for each side.
     */
    public double evalBar() {
        int whiteRemaining = countRemainingMoves(true);
        int blackRemaining = countRemainingMoves(false);
        int totalRemaining = whiteRemaining + blackRemaining;
        double whitesShare = (double) whiteRemaining / totalRemaining;
        // more moves to do (higher whitesShare) = worse position for the whites

        return 1.0 - whitesShare;
    }

    private int countRemainingMoves(boolean white) {
        int routeLength = blacksDiagonalStartPlusOne - 1;

        int totalRemaining = 0;
        List<Piece> myPieces = new ArrayList<>();
        for (Piece piece : pieces) {
            if (piece.whitePiece != white) continue;

            piece.computeRealPosition(this);
            myPieces.add(piece);
        }
        Collections.sort(myPieces);
        for (int i = myPieces.size() - 1; i >= 0; i--) {
            int realPosition = myPieces.get(i).getLastRealPosition();
            totalRemaining += Math.max(routeLength - realPosition, 0);
            routeLength--; // the next piece will have to make a lower count of moves
        }
        int piecesRemaining = diagonalLength - myPieces.size();
        while (piecesRemaining-- > 0) {
            totalRemaining += 6 + (routeLength--);
        }

        return totalRemaining;
    }

    public void diceRolled(int digit) {
        rollDice0(digit);
    }

    public DiceRolled rollDice() {
        return rollDice0(randomDigit());
    }

    private DiceRolled rollDice0(int digit) {
        if (lastDiceRollResult != null) return null;

        List<PossibleMove> possibleMoves = new ArrayList<>();
        if (isMovePossible(null, digit)) {
            possibleMoves.add(new PossibleMove(null, getSpawnPosition()));
        }
        for (Piece piece : pieces) {
            if (piece.whitePiece != whitesTurn) continue;

            if (isMovePossible(piece, digit)) {
                possibleMoves.add(new PossibleMove(piece, lastCalculatedDestination));
            }
        }
        lastDiceRollResult = new Pair<>(digit, possibleMoves);
        DiceRolled packet = new DiceRolled();
        packet.value = (byte) digit;

        lastActionTimestamp = System.currentTimeMillis();

        if (clientside) {
            DiceView.setMode(DiceView.MODE_ROLLING_EXACT_DIGIT);
            DiceView.setDigit(digit);

            InGameActivity activity = getInGameActivity();
            if (activity != null) activity.playDiceRollSound();
        }

        return packet;
    }

    /*
     * Modifies lastCalculatedDestination only if piece != null.
     */
    private boolean isMovePossible(Piece piece, int digit) {
        if (piece == null) {
            if (digit != 6) return false;

            int pieceCount = 0;
            for (Piece aPiece : pieces) {
                if (aPiece.whitePiece == whitesTurn) {
                    pieceCount++;

                    if (pieceCount >= diagonalLength) return false;
                }
            }

            for (Piece aPiece : pieces) {
                if (aPiece.position == getSpawnPosition() || (whitesTurn && aPiece.position == whitesDiagonalStart)) {
                    /*
                     * If aPiece.position == whitesDiagonalStart, then it is
                     * guaranteed that aPiece.whitePiece is true. Maybe simplify that somehow?
                     */

                    return aPiece.whitePiece != whitesTurn;
                }
            }

            return true;
        }

        int oldPosition = piece.position;
        for (int i = 1; i <= digit; i++) {
            int newPosition;
            if (oldPosition == whitesDiagonalStart - 1 && !whitesTurn) {
                newPosition = 0;
            } else if (!whitesTurn && oldPosition == getSpawnPosition() && piece.revertedPosition) {
                newPosition = blacksDiagonalStartPlusOne;
            } else if (whitesTurn && oldPosition == whitesDiagonalStart + diagonalLength - 1) {
                return false;
            } else if (!whitesTurn && oldPosition == blacksDiagonalStartPlusOne + diagonalLength - 2) {
                return false;
            } else {
                newPosition = oldPosition + 1;
            }
            oldPosition = newPosition;
            lastCalculatedDestination = newPosition;

            for (Piece aPiece : pieces) {
                boolean guestOnWhiteSide = (newPosition == whitesDiagonalStart && aPiece.position == 0);
                boolean waitingWhitePiece = (newPosition == 0 && aPiece.position == whitesDiagonalStart);
                if (aPiece.position == newPosition || guestOnWhiteSide || waitingWhitePiece) {
                    if (i != digit) {
                        if (allowVaults) continue;

                        return false;
                    }

                    return aPiece.whitePiece != whitesTurn;
                }
            }
        }

        return true;
    }

    private int getSpawnPosition() {
        return (whitesTurn ? 0 : blacksSpawnPosition);
    }

    public Packet checkTimeout() {
        if (System.currentTimeMillis() - lastActionTimestamp < turnWaitingTimeoutMillis) return null;

        if (lastDiceRollResult == null) return rollDice();

        return makeRandomMove();
    }

    public MakeMove makeRandomMove() {
        List<PossibleMove> possibleMoves = lastDiceRollResult.second;
        if (possibleMoves.isEmpty()) return makeMove(new NoMove(), true);

        for (PossibleMove move : possibleMoves) {
            if (move.isSpawningMove()) return makeMove(move, true);
        }
        int randomIdx = random.nextInt(possibleMoves.size());

        return makeMove(possibleMoves.get(randomIdx), true);
    }

    /** @noinspection UnusedReturnValue*/
    public MakeMove makeMove(MakeMove packet, boolean white) {
        if (whitesTurn != white) return null;
        if (lastDiceRollResult == null) return null;
        if (packet.moveNumber != moveNumber || packet.subMoveNumber != subMoveNumber) return null;

        List<PossibleMove> possibleMoves = lastDiceRollResult.second;

        boolean automatic = (clientside && packet.automatic);
        switch (packet.moveType) {
            case MakeMove.MOVE_TYPE_GENERAL: {
                for (PossibleMove move : possibleMoves) {
                    if (move.isSpawningMove()) continue;

                    if (move.piece.position == packet.piecePosition) {
                        return makeMove(move, automatic);
                    }
                }

                return null;
            }
            case MakeMove.MOVE_TYPE_SPAWNING: {
                for (PossibleMove move : possibleMoves) {
                    if (move.isSpawningMove()) return makeMove(move, automatic);
                }

                return null;
            }
            case MakeMove.MOVE_TYPE_NO_MOVE: {
                if (!possibleMoves.isEmpty()) return null;

                return makeMove(new NoMove(), automatic);
            }
            default: {
                return null;
            }
        }
    }

    @SuppressWarnings("ExtractMethodRecommender")
    public MakeMove makeMove(PossibleMove move, boolean automatic) {
        if (move == null || gameState != GAME_STATE_RUNNING) return null;
        int initialPosition = (move.piece != null ? move.piece.position : move.getDestination());

        move.makeMove();

        if (move instanceof NoMove) {
            noMovesCounter++;
            if (noMovesCounter >= noMoveDrawThreshold) {
                gameState = GAME_STATE_DRAW;
            }
            if (whitesTurn) whitesSkippedMovesGuarantee6++;
            else            blacksSkippedMovesGuarantee6++;
        } else {
            if (clientside) {
                InGameActivity activity = getInGameActivity();

                if (activity != null) activity.playMoveSound();
            }
            noMovesCounter = 0;
            if (whitesTurn) whitesSkippedMovesGuarantee6 = 0;
            else            blacksSkippedMovesGuarantee6 = 0;

            boolean whiteFinished;
            {
                boolean ok = true;
                for (int i = whitesDiagonalStart; i < whitesDiagonalStart + diagonalLength; i++) {
                    if (!checkPiece(i, true, false)) {
                        ok = false;

                        break;
                    }
                }

                whiteFinished = ok;
            }
            boolean blackFinished;
            {
                if (!checkPiece(blacksSpawnPosition, false, true)) {
                    blackFinished = false;
                } else {
                    boolean ok = true;
                    for (int i = blacksDiagonalStartPlusOne; i < blacksDiagonalStartPlusOne + diagonalLength - 1; i++) {
                        if (!checkPiece(i, false, false /* does not really matter, could be true */)) {
                            ok = false;

                            break;
                        }
                    }

                    blackFinished = ok;
                }
            }
            if (lastChanceActivated) {
                if (whiteFinished && blackFinished) {
                    gameState = GAME_STATE_DRAW;

                    lastChance = false;
                }
                if ((whitesTurn && !blackFinished) || (!whitesTurn && !whiteFinished)) {
                    lastChance = false;
                    lastChanceActivated = false; // continue the game
                }
            } else if (whiteFinished || blackFinished) {
                lastChance = true;
            }
        }
        MakeMove packet = new MakeMove();
        packet.moveNumber = moveNumber;
        packet.subMoveNumber = subMoveNumber++;
        packet.piecePosition = initialPosition;
        packet.moveType = move.getMoveType();
        packet.automatic = automatic;

        if (automatic) {
            if (!(move instanceof NoMove)) {
                if (whitesTurn) whitesAutomaticMoveCount++;
                else            blacksAutomaticMoveCount++;
            }
        } else {
            if (whitesTurn) whitesAutomaticMoveCount = 0;
            else            blacksAutomaticMoveCount = 0;
        }
        if (lastDiceRollResult.first != 6) { // rolling 6 gives you the right to make one more move
            if (!whitesTurn) {
                moveNumber++;
                subMoveNumber = 1;
            }

            whitesTurn = !whitesTurn;

            if (lastChance) {
                if (!lastChanceActivated) {
                    lastChanceActivated = true;
                } else {
                    if (whitesTurn) gameState = GAME_STATE_WHITE_WON;
                    else            gameState = GAME_STATE_BLACK_WON;
                }
            }
        }
        lastDiceRollResult = null;
        lastActionTimestamp = System.currentTimeMillis();

        return packet;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean checkPiece(int position, boolean whiteRequired, boolean mustBeReverted) {
        Piece found = null;
        for (Piece piece : pieces) {
            if (piece.position != position) continue;
            if (mustBeReverted && !piece.revertedPosition) continue;

            found = piece;

            break;
        }
        if (found == null) return false;

        return found.whitePiece == whiteRequired;
    }

    public void resign(boolean white) {
        if (gameState != GAME_STATE_RUNNING && !resigned) return;

        if (resigned) {
            gameState = GAME_STATE_DRAW; // both players resigned nearly at the same moment

            return;
        }
        gameState = (white ? GAME_STATE_BLACK_WON : GAME_STATE_WHITE_WON);
        resigned = true;
    }

    public byte getGameState() {
        return gameState;
    }

    public List<Piece> getPieces() {
        return pieces;
    }

    public boolean isDiceRolled() {
        return lastDiceRollResult != null;
    }

    public List<PossibleMove> getPossibleMoves() {
        if (lastDiceRollResult == null) return Collections.emptyList();

        return lastDiceRollResult.second;
    }

    public PossibleMove getSpawningMove() {
        for (Board.PossibleMove move : getPossibleMoves()) {
            if (move.isSpawningMove()) return move;
        }

        return null;
    }

    public boolean isWhitesTurn() {
        return whitesTurn;
    }

    public int getWhitesDiagonalStart() {
        return whitesDiagonalStart;
    }

    public int getBlacksSpawnPosition() {
        return blacksSpawnPosition;
    }

    public long getLastActionTimestamp() {
        return lastActionTimestamp;
    }

    public int getWhitesAutomaticMoveCount() {
        return whitesAutomaticMoveCount;
    }

    public int getBlacksAutomaticMoveCount() {
        return blacksAutomaticMoveCount;
    }

    public void setAllowVaults(boolean allowVaults) {
        this.allowVaults = allowVaults;
    }

    public void setMoveNumber(int moveNumber) {
        this.moveNumber = moveNumber;
    }

    public void setSubMoveNumber(int subMoveNumber) {
        this.subMoveNumber = subMoveNumber;
    }

    public void setWhitesTurn(boolean whitesTurn) {
        this.whitesTurn = whitesTurn;
    }

    public void setLastChanceActivated() {
        lastChance = true;
        lastChanceActivated = true;
    }

    @NonNull
    @Override
    public String toString() {
        String result = "Board{" +
                "turnWaitingTimeoutMillis=" + turnWaitingTimeoutMillis +
                ", noMoveDrawThreshold=" + noMoveDrawThreshold +
                ", guaranteeRollOf6=" + guaranteeRollOf6 +
                ", diagonalLength=" + diagonalLength +
                ", whitesDiagonalStart=" + whitesDiagonalStart +
                ", blacksSpawnPosition=" + blacksSpawnPosition +
                ", blacksDiagonalStartPlusOne=" + blacksDiagonalStartPlusOne +
                ", pieces=" + Helper.listToString(pieces) +
                ", allowVaults=" + allowVaults +
                ", moveNumber=" + moveNumber +
                ", subMoveNumber=" + subMoveNumber +
                ", whitesTurn=" + whitesTurn +
                ", lastCalculatedDestination=" + lastCalculatedDestination;
        if (lastDiceRollResult != null) {
            result += ", lastDiceRollResult.first=" + lastDiceRollResult.first +
                    ", lastDiceRollResult.second=" + Helper.listToString(lastDiceRollResult.second);
        } else {
            result += ", lastDiceRollResult=null";
        }
        result += ", noMovesCounter=" + noMovesCounter +
                ", lastActionTimestamp=" + lastActionTimestamp +
                ", gameState=" + gameState +
                ", resigned=" + resigned +
                ", lastChance=" + lastChance +
                ", lastChanceActivated=" + lastChanceActivated +
                ", whitesAutomaticMoveCount=" + whitesAutomaticMoveCount +
                ", blacksAutomaticMoveCount=" + blacksAutomaticMoveCount +
                ", rolled6NoticeablyHard=" + rolled6NoticeablyHard +
                ", doNotReRoll6NoticeablyHard=" + doNotReRoll6NoticeablyHard +
                ", whitesSkippedMovesGuarantee6=" + whitesSkippedMovesGuarantee6 +
                ", blacksSkippedMovesGuarantee6=" + blacksSkippedMovesGuarantee6 +
                '}';

        return result;
    }
}
