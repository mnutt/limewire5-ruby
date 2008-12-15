package com.limegroup.bittorrent.handshaking.piecestrategy;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.limewire.collection.AndView;
import org.limewire.collection.BitField;

import com.limegroup.bittorrent.BTInterval;
import com.limegroup.bittorrent.BTMetaInfo;

public class RandomPieceStrategy extends AbstractPieceStrategy {

    private final Random randomizer;

    public RandomPieceStrategy(BTMetaInfo btMetaInfo) {
        this(btMetaInfo, new Random());
    }

    public RandomPieceStrategy(BTMetaInfo btMetaInfo, Random randomizer) {
        super(btMetaInfo);
        this.randomizer = randomizer;
    }

    public List<BTInterval> getNextPieces(BitField availableBlocks, BitField neededBlocks) {
        BitField interestingBlocks = new AndView(availableBlocks, neededBlocks);

        int selected = getRandomPiece(interestingBlocks);

        if (selected != -1) {
            BTInterval piece = getBtMetaInfo().getPiece(selected);
            return Collections.singletonList(piece);
        }
        return Collections.emptyList();
    }

    protected int getRandomPiece(BitField available) {
        int selectedPiece = -1;
        int current = 1;
        for (int pieceIndex = available.nextSetBit(0); pieceIndex >= 0; pieceIndex = available
                .nextSetBit(pieceIndex + 1)) {
            float random = randomizer.nextFloat();
            float percent = 1f / current++;
            if ( random < percent) {
                selectedPiece = pieceIndex;
            }
        }
        return selectedPiece;
    }

}
