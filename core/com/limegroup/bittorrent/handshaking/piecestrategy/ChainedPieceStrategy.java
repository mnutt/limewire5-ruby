package com.limegroup.bittorrent.handshaking.piecestrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.limewire.collection.BitField;

import com.limegroup.bittorrent.BTInterval;

/**
 * This piece strategy will try a number of piece strategies in order, until one
 * of them returns results.
 */
public class ChainedPieceStrategy implements PieceStrategy {
    private final List<PieceStrategy> pieceStrategies;

    public ChainedPieceStrategy() {
        pieceStrategies = new ArrayList<PieceStrategy>();
    }

    public ChainedPieceStrategy(PieceStrategy... pieceStrategies) {
        this.pieceStrategies = Arrays.asList(pieceStrategies);
    }

    public void addPieceStrategy(PieceStrategy pieceStrategy) {
        assert pieceStrategy != this;
        pieceStrategies.add(pieceStrategy);
    }

    @Override
    public List<BTInterval> getNextPieces(BitField availableBlocks, BitField neededBlocks) {
        for (PieceStrategy pieceStrategy : pieceStrategies) {
            List<BTInterval> pieces = pieceStrategy.getNextPieces(availableBlocks, neededBlocks);
            if (pieces.size() > 0) {
                return pieces;
            }
        }

        return Collections.emptyList();
    }
}
