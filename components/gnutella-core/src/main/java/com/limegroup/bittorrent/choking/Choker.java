package com.limegroup.bittorrent.choking;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.NECallable;
import org.limewire.core.settings.BittorrentSettings;
import org.limewire.nio.observer.Shutdownable;

import com.limegroup.bittorrent.Chokable;
import com.limegroup.gnutella.UploadServices;

/**
 * As defined at <a href="http://www.bittorrent.org/beps/bep_0003.html">The 
 * BitTorrent Protocol Specification</a> "Choking is done for several reasons. 
 * TCP congestion control behaves very poorly when sending over many 
 * connections at once. Also, choking lets each peer use a 
 * tit-for-tat-ish algorithm to ensure that they get a consistent download rate."
 */
public abstract class Choker implements Runnable, Shutdownable {
	
	private static final Log LOG = LogFactory.getLog(Choker.class);
	/*
	 * time in milliseconds between choking/unchoking of connections
	 */
	private static final int RECHOKE_TIMEOUT = 10 * 1000;
	
	/**
	 * The invoker on which to perform network-related tasks
	 */
	protected final ScheduledExecutorService invoker;
	
	/**
	 * The source of the chokables.  The list it provides must be
	 * modifyable from.
	 */
	protected final NECallable<List<? extends Chokable>> chokablesSource;
	protected int round;
	
	private volatile Future periodic;
	private final Runnable immediateChoker = new ImmediateChoker();
	
	private final UploadServices uploadServices;
	
	Choker(NECallable<List<? extends Chokable>> chokables,
            ScheduledExecutorService invoker,
            UploadServices uploadServices) {
        this.invoker = invoker;
        this.chokablesSource = chokables;
        this.uploadServices = uploadServices;
	}
	
	/**
	 * Stops this choker, cancelling any scheduled choking.
	 */
	public void shutdown() {
		if (periodic != null)
			periodic.cancel(false);
	}

	/**
	 * Starts this choker
	 */
	public void start() {
		shutdown();
		periodic = invoker.schedule(this, RECHOKE_TIMEOUT, TimeUnit.MILLISECONDS);
	}

	/**
	 * Triggers an immediate rechoke
	 */
	public final void rechoke() {
		// final to make sure immediate rechokes happen through
		// the invoker.
		invoker.execute(immediateChoker);
	}
	
	/**
	 * the actual choking logic
	 * @param force whether to force an unchoke.  May be ignored
	 * by actual implementation.
	 */
	protected abstract void rechokeImpl(boolean force);

	public void run() {
		
		round++;
		
		if (LOG.isDebugEnabled())
			LOG.debug("scheduling rechoke");
		
		rechokeImpl(true);
		
		periodic = invoker.schedule(this, RECHOKE_TIMEOUT, TimeUnit.MILLISECONDS);
	}
	
	
	/**
	 * @return the number of uploads that should be unchoked
	 * Note: Copied verbatim from mainline BT
	 */
	protected int getNumUploads() {
		int uploads = BittorrentSettings.TORRENT_MAX_UPLOADS.getValue();
		if (uploads > 0)
			return uploads;
		
		float rate = uploadServices.getRequestedUploadSpeed();
		if (rate == Float.MAX_VALUE)
			return 7; //"unlimited, just guess something here..." - Bram
		else if (rate < 9000)
			return 2;
		else if (rate < 15000)
			return 3;
		else if (rate < 42000)
			return 4;
		else 
			return (int) Math.sqrt(rate * 0.6f);
		
	}
	
	private class ImmediateChoker implements Runnable {
		public void run() {
			rechokeImpl(false);
		}
	}
	
	/*
	 * Compares two BTConnections by their average download 
	 * or upload speeds.  Higher speeds get preference.
	 */
	protected static class SpeedComparator implements Comparator<Chokable> {
		
		private final boolean download;
		public SpeedComparator(boolean download) {
			this.download = download;
		}
		
		public int compare(Chokable c1, Chokable c2) {
			if (c1 == c2)
				return 0;
			
			float bw1 = c1.getMeasuredBandwidth(download, false);
			float bw2 = c2.getMeasuredBandwidth(download, false);
			
			if (bw1 == bw2)
				return 0;
			else if (bw1 > bw2)
				return -1;
			else
				return 1;
		}
	}
}