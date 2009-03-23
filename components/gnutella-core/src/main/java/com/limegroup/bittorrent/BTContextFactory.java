package com.limegroup.bittorrent;

/**
 * Defines an interface to create <code>BTContext</code>s.
 */
public interface BTContextFactory {

    public BTContext createBTContext(BTMetaInfo info);

}