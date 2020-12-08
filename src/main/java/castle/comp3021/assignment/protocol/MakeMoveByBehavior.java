package castle.comp3021.assignment.protocol;

import castle.comp3021.assignment.piece.Knight;

import java.util.*;
import java.util.stream.Collectors;

public class MakeMoveByBehavior {
    private final Behavior behavior;
    private final Game game;
    private final Move[] availableMoves;

    public MakeMoveByBehavior(Game game, Move[] availableMoves, Behavior behavior){
        this.game = game;
        this.availableMoves = availableMoves;
        this.behavior = behavior;
    }

    /**
     * Return next move according to different strategies made by each piece.
     * You can add helper method if needed, as long as this method returns a next move.
     * - {@link Behavior#RANDOM}: return a random move from {@link this#availableMoves}
     * - {@link Behavior#GREEDY}: prefer the moves towards central place, the closer, the better
     * - {@link Behavior#CAPTURING}: prefer the moves that captures the enemies, killing the more, the better.
     *                               when there are many pieces that can captures, randomly select one of them
     * - {@link Behavior#BLOCKING}: prefer the moves that block enemy's {@link Knight}.
     *                              See how to block a knight here: https://en.wikipedia.org/wiki/Xiangqi (see `Horse`)
     *
     * @return a selected move adopting strategy specified by {@link this#behavior}
     */
    public Move getNextMove(){
        // TODO
        return switch (this.behavior) {
            case GREEDY -> getGreedyMove();
            case CAPTURING -> getCapturingMove();
            case BLOCKING -> getBlockingMove();
            default -> getRandomMove();
        };
    }

    /**
     * @return move chosen by RANDOM behavior
     */
    private Move getRandomMove() {
        if (availableMoves.length <= 0) {
            return null;
        }
        return availableMoves[new Random().nextInt(availableMoves.length)];
    }

    /**
     * @return move chosen by GREEDY behavior
     */
    private Move getGreedyMove() {
        List<Move> moves = new ArrayList<>(Arrays.asList(availableMoves));
        return moves.stream()
                .filter(this::isGreedyMove)
                .min(Comparator.comparing(move -> getManhattanDistance(move.getDestination(), game.getCentralPlace())))
                .orElseGet(this::getRandomMove);
    }

    /**
     * @return move chosen by CAPTURING behavior
     */
    private Move getCapturingMove() {
        List<Move> moves = new ArrayList<>(Arrays.asList(availableMoves));
        moves = moves.stream()
                .filter(this::isCapturingMove)
                .collect(Collectors.toList());
        if (moves.size() <= 0) {
            return getRandomMove();
        }
        return moves.get(new Random().nextInt(moves.size()));
    }

    /**
     * @return move chosen by BLOCKING behavior
     */
    private Move getBlockingMove() {
        List<Move> moves = new ArrayList<>(Arrays.asList(availableMoves));
        moves = moves.stream()
                .filter(this::isBlockingMove)
                .collect(Collectors.toList());
        if (moves.size() <= 0) {
            return getRandomMove();
        }
        return moves.get(new Random().nextInt(moves.size()));
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

    private boolean isCapturingMove(Move move) {
        if (move == null) {
            return false;
        }
        Piece piece = game.getPiece(move.getDestination());
        return !(piece == null || piece.getPlayer().equals(game.currentPlayer));
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

