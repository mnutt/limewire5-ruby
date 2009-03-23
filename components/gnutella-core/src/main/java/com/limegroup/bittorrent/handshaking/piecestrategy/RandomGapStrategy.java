package com.limegroup.bittorrent.handshaking.piecestrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.limewire.collection.AndView;
import org.limewire.collection.BitField;

import com.limegroup.bittorrent.BTInterval;
import com.limegroup.bittorrent.BTMetaInfo;

public class RandomGapStrategy extends RandomPieceStrategy {
    private int max = 10;

    public RandomGapStrategy(BTMetaInfo btMetaInfo) {
        super(btMetaInfo);
    }
    
    public RandomGapStrategy(BTMetaInfo btMetaInfo, Random randomizer) {
        super(btMetaInfo, randomizer);
    }

    public List<BTInterval> getNextPieces(BitField availableBlocks, BitField neededBlocks) {
        List<BTInterval> nextPieces = new ArrayList<BTInterval>();
        BitField interestingBlocks = new AndView(availableBlocks, neededBlocks);

        int nextPiece = getRandomPiece(interestingBlocks);

        if (nextPiece != -1) {
            do {
                if (interestingBlocks.get(nextPiece)) {
                    BTInterval btInterval = getBtMetaInfo().getPiece(nextPiece);
                    nextPieces.add(btInterval);
                } else {
                    break;
                }
                nextPiece++;
                if (nextPieces.size() >= max || nextPiece >= getBtMetaInfo().getNumBlocks()) {
                    break;
                }
            } while (true);
        }

        return nextPieces;
    }
}
