package com.limegroup.bittorrent.choking;

import java.util.List;

import org.limewire.collection.NECallable;

import com.limegroup.bittorrent.Chokable;

public interface ChokerFactory {

    public Choker getChoker(NECallable<List<? extends Chokable>> chokables,
            boolean seed);

}