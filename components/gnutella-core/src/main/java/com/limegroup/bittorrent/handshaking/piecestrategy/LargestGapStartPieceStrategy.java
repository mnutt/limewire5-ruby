package com.limegroup.bittorrent.handshaking.piecestrategy;

import java.util.ArrayList;
import java.util.List;

import org.limewire.collection.AndView;
import org.limewire.collection.BitField;

import com.limegroup.bittorrent.BTInterval;
import com.limegroup.bittorrent.BTMetaInfo;

public class LargestGapStartPieceStrategy extends AbstractPieceStrategy {

    //TODO test this
    private static final int DEFAULT_MAX_NUM_PIECES = 10;

    private final int maxNumPieces;

    public LargestGapStartPieceStrategy(BTMetaInfo btMetaInfo) {
        this(btMetaInfo, DEFAULT_MAX_NUM_PIECES);
    }

    public LargestGapStartPieceStrategy(BTMetaInfo btMetaInfo,
            int maxNumPieces) {
        super(btMetaInfo);
        this.maxNumPieces = maxNumPieces;

    }

    public List<BTInterval> getNextPieces(BitField availableBlocks, BitField neededBlocks) {

        BitField interestingBlocks = new AndView(availableBlocks,neededBlocks);
        List<BTInterval> nextPieces = new ArrayList<BTInterval>();
        int currentGapStartIndex = interestingBlocks.nextSetBit(0);
        int lastPieceIndex = -1;
        int currentGapSize = 0;

        int selectedGapStartIndex = 0;
        int selectedGapSize = 0;

        for (int currentPieceIndex = interestingBlocks.nextSetBit(0); currentPieceIndex >= 0; currentPieceIndex = interestingBlocks
                .nextSetBit(currentPieceIndex + 1)) {
            if ((lastPieceIndex + 1) != currentPieceIndex) {
                currentGapStartIndex = currentPieceIndex;
            }

            currentGapSize = currentPieceIndex - currentGapStartIndex + 1;

            if (currentGapSize > selectedGapSize) {
                selectedGapStartIndex = currentGapStartIndex;
                selectedGapSize = currentGapSize;
            }

            if (selectedGapSize >= maxNumPieces) {
                break;
            }
        }

        if (selectedGapSize > 0) {
            int gapEndIndex = selectedGapStartIndex + selectedGapSize;
            for (int gapIndex = selectedGapStartIndex; gapIndex < gapEndIndex; gapIndex++) {
                nextPieces.add(getBtMetaInfo().getPiece(gapIndex));
            }
        }
        return nextPieces;
    }

}
