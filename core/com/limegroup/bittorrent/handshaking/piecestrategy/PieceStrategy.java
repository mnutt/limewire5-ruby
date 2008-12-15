package com.limegroup.bittorrent.handshaking.piecestrategy;

import java.util.List;

import org.limewire.collection.BitField;

import com.limegroup.bittorrent.BTInterval;

public interface PieceStrategy {
    /**
     * Returns the next pieces that should be downloaded. For bit torrent peers
     * the list size will always be 1, but for web seed connections we will be
     * downloading several blocks at a time.
     * 
     * @return the next pieces to download
     */
    public List<BTInterval> getNextPieces(BitField availableBlocks, BitField neededBlocks);
}
