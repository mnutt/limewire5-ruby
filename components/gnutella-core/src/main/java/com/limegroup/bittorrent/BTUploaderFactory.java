package com.limegroup.bittorrent;

public interface BTUploaderFactory {

    public abstract BTUploader createBTUploader(ManagedTorrent torrent,
            BTMetaInfo info);

}