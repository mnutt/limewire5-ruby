package com.limegroup.gnutella.downloader;

import java.util.Collection;

public interface SourceRanker {

    /**
     * @param hosts a collection of remote hosts to rank
     * @return if we didn't know about at least one of the hosts
     */
    public boolean addToPool(Collection<? extends RemoteFileDescContext> hosts);

    /**
     * @param host the host that the ranker should consider
     * @return if we did not already know about this host
     */
    public boolean addToPool(RemoteFileDescContext host);

    /**
     * @return whether the ranker has any more potential sources
     */
    public boolean hasMore();

    /**
     * @return the source that should be tried next or <code>null</code>
     * if there is none
     */
    public RemoteFileDescContext getBest();

    /**
     * @return the number of hosts this ranker knows about
     */
    public int getNumKnownHosts();

    /**
     * @return the ranker knows about at least one potential source that is
     * not currently busy
     */
    public boolean hasNonBusy();

    /**
     * @return the number of busy hosts the ranker knows about
     */
    public int getNumBusyHosts();

    /**
     * @return how much time we should wait before at least one host
     * will become non-busy
     */
    public int calculateWaitTime();

    /**
     * Stops the ranker, clearing any state.
     */
    public void stop();

    /** Sets the Mesh handler if any. */
    public void setMeshHandler(MeshHandler handler);

    /** 
     * @return the Mesh Handler, if any
     */
    public MeshHandler getMeshHandler();

    /**
     * @return the collection of hosts that can be shared with other rankers
     */
    public Collection<RemoteFileDescContext> getShareableHosts();
}