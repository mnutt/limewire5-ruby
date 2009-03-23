/**
 * 
 */
package com.limegroup.bittorrent.swarm;

import java.util.Arrays;

import org.limewire.collection.NECallable;

import com.limegroup.bittorrent.BTInterval;
import com.limegroup.bittorrent.BTPiece;

public final class BTNECallable implements NECallable<BTPiece> {
    private final int pieceId;

    private final byte[] data;

    private final long pieceHigh;

    private final long pieceLow;

    BTNECallable(int pieceId, long pieceLow, long pieceHigh, byte[] data) {
        this.pieceId = pieceId;
        this.data = data;
        this.pieceHigh = pieceHigh;
        this.pieceLow = pieceLow;
    }

    public BTPiece call() {
        return new BTPiece() {

            public byte[] getData() {
                return data;
            }

            public BTInterval getInterval() {
                BTInterval interval = new BTInterval(pieceLow, pieceHigh, pieceId);
                return interval;
            }
        };
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BTNECallable)) {
            return false;
        }
        BTNECallable bt = (BTNECallable) obj;
        return pieceId == bt.pieceId && pieceLow == bt.pieceLow && pieceHigh == bt.pieceHigh
                && Arrays.equals(data, bt.data);
    }

    @Override
    public int hashCode() {
        return pieceId;
    }

}