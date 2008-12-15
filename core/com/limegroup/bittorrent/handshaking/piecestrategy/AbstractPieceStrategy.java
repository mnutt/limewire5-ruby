package com.limegroup.bittorrent.handshaking.piecestrategy;

import com.limegroup.bittorrent.BTMetaInfo;

public abstract class AbstractPieceStrategy implements PieceStrategy{

    private final BTMetaInfo btMetaInfo;

    public AbstractPieceStrategy(BTMetaInfo btMetaInfo) {
        this.btMetaInfo = btMetaInfo;

    }

    protected BTMetaInfo getBtMetaInfo() {
        return btMetaInfo;
    }

    
}