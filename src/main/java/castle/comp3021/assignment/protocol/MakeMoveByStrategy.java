package castle.comp3021.assignment.protocol;

import castle.comp3021.assignment.piece.*;

import java.util.*;
import java.util.stream.Collectors;

public class MakeMoveByStrategy {
    private final Strategy strategy;
    private final Game game;
    private final Move[] availableMoves;

    public MakeMoveByStrategy(Game game, Move[] availableMoves, Strategy strategy){
        this.game = game;
        this.availableMoves = availableMoves;
        this.strategy = strategy;
    }

    /**
     * Return next move according to different strategies made by {@link castle.comp3021.assignment.player.ComputerPlayer}
     * You can add helper method if needed, as long as this method returns a next move.
     * - {@link Strategy#RANDOM}: select a random move from the proposed moves by all pieces
     * - {@link Strategy#SMART}: come up with some strategy to select a next move from the proposed moves by all pieces
     *
     * @return a next move
     */
    public Move getNextMove(){
        // TODO
        if (this.strategy.equals(Strategy.SMART)) {
            return getSmartMove();
        }
        return getRandomMove();
    }

    private Move getSmartMove() {
        Behavior[] strategy = {Behavior.GREEDY, Behavior.CAPTURING, Behavior.BLOCKING, Behavior.RANDOM};
        Move nextMove = getWinningMove();
        if (nextMove != null) {
            return nextMove;
        }
        for (Behavior behavior : strategy) {
            nextMove = switch (behavior) {
                case GREEDY -> getSmartGreedyMove();
                case CAPTURING -> getSmartCapturingMove();
                case BLOCKING -> getSmartBlockingMove();
                default -> getRandomMove();
            };
            if (nextMove != null) {
                return nextMove;
            }
        }
        return null;
    }

    private Move getWinningMove() {
        List<Move> moves = new ArrayList<>(Arrays.asList(availableMoves));
        return moves.stream()
                .filter(this::isWinningMove)
                .min(Comparator.comparingInt(move -> getManhattanDistance(move.getSource(), move.getDestination())))
                .orElse(null);
    }

    private Move getRandomMove() {
        if (availableMoves.length <= 0) {
            return null;
        }
        return availableMoves[new Random().nextInt(availableMoves.length)];
    }

    private Move getSmartGreedyMove() {
        List<Move> moves = new ArrayList<>(Arrays.asList(availableMoves));
        moves = moves.stream()
                .filter(this::isKnightMove)
                .collect(Collectors.toList());
        Move greedyMove = moves.stream()
                .filter(this::isSmartGreedyMove)
                .min(Comparator.comparingInt(move -> getManhattanDistance(move.getDestination(), game.getCentralPlace())))
                .orElse(null);
        if (greedyMove == null) {
            greedyMove = moves.stream()
                    .filter(this::isGreedyMove)
                    .min(Comparator.comparingInt(move -> getManhattanDistance(move.getDestination(), game.getCentralPlace())))
                    .orElse(null);
        }
        return greedyMove;
    }

    private Move getSmartCapturingMove() {
        List<Move> moves = new ArrayList<>(Arrays.asList(availableMoves));
        List<Move> capturingMoves = moves.stream()
                .filter(this::isSmartCapturingMove)
                .collect(Collectors.toList());
        if (capturingMoves.size() <= 0) {
            capturingMoves = moves.stream()
                    .filter(this::isCapturingMove)
                    .collect(Collectors.toList());
        } else {
            return capturingMoves.stream()
                    .min(Comparator.comparingInt(move -> getManhattanDistance(move.getDestination(), game.getCentralPlace())))
                    .orElse(null);
        }
        if (capturingMoves.size() <= 0) {
            return null;
        }
        return capturingMoves.get(new Random().nextInt(capturingMoves.size()));
    }

    private Move getSmartBlockingMove() {
        List<Move> moves = new ArrayList<>(Arrays.asList(availableMoves));
        return moves.stream()
                .filter(this::isBlockingMove)
                .min(Comparator.comparingInt(move -> getManhattanDistance(move.getDestination(), game.getCentralPlace())))
                .orElse(null);
    }

    private boolean isWinningMove(Move move) {
        if (move == null || game.getNumMoves() < game.getConfiguration().getNumMovesProtection()) {
            return false;
        }

        // Win by Knight leaving central place
        Piece piece = game.getPiece(move.getSource());
        Place centralPlace = game.getCentralPlace();
        if (piece instanceof Knight
                && move.getSource().equals(centralPlace) && !(move.getDestination().equals(centralPlace))) {
            return true;
        }

        // Win by capturing all enemy pieces
        Piece enemyPiece = game.getPiece(move.getDestination());
        if (enemyPiece == null || enemyPiece.getPlayer().equals(game.currentPlayer)) {
            return false;
        }
        return (getRemainingEnemyPieceCount() == 1);
    }

    private int getRemainingEnemyPieceCount() {
        int count = 0;
        for (Piece[] pieces : game.getBoard()) {
            for (Piece piece : pieces) {
                if (piece != null && !(piece.getPlayer().equals(game.currentPlayer))) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean isKnightMove(Move move) {
        if (move == null) {
            return false;
        }
        Piece piece = game.getPiece(move.getSource());
        return (piece instanceof Knight && piece.getPlayer().equals(game.currentPlayer));
    }

    private int getManhattanDistance(Place source, Place destination) {
        if (source == null || destination == null) {
            return Integer.MAX_VALUE;
        }
        return Math.abs(destination.x() - source.x()) + Math.abs(destination.y() - source.y());
    }

    private boolean isGreedyMove(Move move) {
        if (move == null) {
            return false;
        }
        int oldDistance = getManhattanDistance(move.getSource(), game.getCentralPlace());
        int newDistance = getManhattanDistance(move.getDestination(), game.getCentralPlace());
        return newDistance < oldDistance;
    }

    private boolean isSmartGreedyMove(Move move) {
        if (!isGreedyMove(move)) {
            return false;
        }
        int newDistance = getManhattanDistance(move.getDestination(), game.getCentralPlace());
        return (newDistance % 3 == 0);
    }

    private boolean isCapturingMove(Move move) {
        if (move == null) {
            return false;
        }
        Piece piece = game.getPiece(move.getDestination());
        return !(piece == null || piece.getPlayer().equals(game.currentPlayer));
    }

    private boolean isSmartCapturingMove(Move move) {
        if (!isCapturingMove(move)) {
            return false;
        }
        Piece piece = game.getPiece(move.getDestination());
        return isEnemyKnight(piece);
    }

    private boolean isBlockingMove(Move move) {
        if (move == null) {
            return false;
        }
        Place destination = move.getDestination();
        int[] offsets = {1, -1};
        for (int offset : offsets) {
            Piece pieceX = game.getPiece(destination.x() + offset, destination.y());
            Piece pieceY = game.getPiece(destination.x(), destination.y() + offset);
            if (isEnemyKnight(pieceX) || isEnemyKnight(pieceY)) {
                return true;
            }
        }
        return false;
    }

    private boolean isEnemyKnight(Piece piece) {
        return (piece instanceof Knight && !(piece.getPlayer().equals(game.currentPlayer)));
    }
}
