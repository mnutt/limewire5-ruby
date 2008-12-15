package com.limegroup.bittorrent.handshaking.piecestrategy;

import java.util.Collections;
import java.util.List;

import org.limewire.collection.AndView;
import org.limewire.collection.BitField;

import com.limegroup.bittorrent.BTConnection;
import com.limegroup.bittorrent.BTInterval;
import com.limegroup.bittorrent.BTLinkManager;
import com.limegroup.bittorrent.BTMetaInfo;

public class RarestPieceFirstStrategy extends AbstractPieceStrategy {

    private static final int DEFAULT_MIN_RARENESS = 1;

    private final BTLinkManager btLinkManager;

    private final int minRareness;

    public RarestPieceFirstStrategy(BTMetaInfo btMetaInfo, BTLinkManager btLinkManager,
            int minRareness) {
        super(btMetaInfo);
        assert btLinkManager != null;
        assert minRareness >= 0;
        this.btLinkManager = btLinkManager;
        this.minRareness = minRareness;
    }

    public RarestPieceFirstStrategy(BTMetaInfo btMetaInfo, BTLinkManager btLinkManager) {
        this(btMetaInfo, btLinkManager, DEFAULT_MIN_RARENESS);
    }

    public List<BTInterval> getNextPieces(BitField availableBlocks, BitField neededBlocks) {
        BitField interestingBlocks = new AndView(availableBlocks, neededBlocks);
        int rarestPiece = getCurrentRarestPiece(interestingBlocks, minRareness);

        if (rarestPiece > -1) {
            BTInterval nextPiece = getBtMetaInfo().getPiece(rarestPiece);
            return Collections.singletonList(nextPiece);
        }
        return Collections.emptyList();
    }

    /**
     * Gets the rareness of an individual piece. A smaller number means it is
     * more rare. The number indicates how many of your peers has the piece. The
     * algorithm stops after the specified max rareness is reached, and the max
     * is returned.
     * 
     * @param pieceIndex the piece in the bit torrent file to test.
     * @param max - number of peers having piece to assume not rare at all
     * @return
     */
    public int getPieceRareness(int pieceIndex, int max) {
        int rareness = 0;
        for (BTConnection connection : getBTLinkManager().getConnections()) {
            if (connection.hasPiece(pieceIndex)) {
                rareness++;
                if (rareness >= max) {
                    return max;
                }
            }

        }
        return rareness;
    }

    /**
     * This algorithm attempts to find the rarest piece among your peers. It
     * will return the first piece that matches the minimum indicated rareness
     * for returning. Otherwise it will iterate through all pieces and rareness
     * combinations until the rarest is found. 
     * 
     * @param interestingBlocks
     * 
     * @param numBlocks
     * @return
     */
    private int getCurrentRarestPiece(BitField interestingBlocks, int min) {
        int currentRarestPiece = -1;

        int mostRare = Integer.MAX_VALUE;

        for (int pieceIndex = interestingBlocks.nextSetBit(0); pieceIndex >= 0; pieceIndex = interestingBlocks
                .nextSetBit(pieceIndex + 1)) {
            int currentPieceRareness = getPieceRareness(pieceIndex, mostRare);
            if (currentPieceRareness < mostRare) {
                mostRare = currentPieceRareness;
                currentRarestPiece = pieceIndex;
                if (mostRare <= min) {
                    break;
                }
            }
        }

        return currentRarestPiece;
    }

    public BTLinkManager getBTLinkManager() {
        return btLinkManager;
    }

}
