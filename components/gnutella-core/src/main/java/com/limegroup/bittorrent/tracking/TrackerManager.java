package com.limegroup.bittorrent.tracking;

import java.net.URI;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.core.settings.BittorrentSettings;

import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.bittorrent.TorrentContext;
import com.limegroup.bittorrent.TorrentLocation;

/**
 * Manages tracker announcements. 
 * <p>
 * Announcements are communications between the client and the tracking server. They
 * are used to request to the tracking server for information
 * about the statistics of the torrent. Also the client announces that it wants to 
 * join the swarm and that the server should add it to the peers in that swarm.
 */

public class TrackerManager {
	
	private static final Log LOG = LogFactory.getLog(TrackerManager.class);
	
	/**
	 * the number of failures after which we consider giving up
	 */
	private static final int MAX_TRACKER_FAILURES = 5;
	
	/**
	 * The trackers we know that track this torrent download.
	 * Note: It will be read much more often than updated. 
	 * Note2: This is not proper multi-tracker support. 
	 */
	private final Collection<Tracker> trackers = new CopyOnWriteArrayList<Tracker>();
	
	private final ExecutorService requestQueue = ExecutorsHelper.newProcessingQueue("tracker requester");
	
	/**
	 * the next time we'll contact the tracker.
	 */
	private volatile long _nextTrackerRequestTime;
	
	private final ManagedTorrent torrent;
	
	private volatile ScheduledFuture<?> scheduledAnnounce;
    
    private volatile String lastFailureReason;
    
    private final ScheduledExecutorService backgroundExecutor;
	
	TrackerManager(ManagedTorrent torrent, TrackerFactory trackerFactory, ScheduledExecutorService backgroundExecutor) {
		this.torrent = torrent;
        this.backgroundExecutor = backgroundExecutor;
		TorrentContext context = torrent.getContext();
		for (URI uri : context.getMetaInfo().getTrackers())
			trackers.add(trackerFactory.create(uri, context, torrent));
	}
	
	public void add(Tracker t) {
		trackers.add(t);
	}
	
	/**
	 * Announce ourselves to a tracker
	 * 
	 * @param t the Tracker to announce to
	 * @param event
	 *            the event to send to the tracker, see TrackerRequester class
	 */
	private void announceBlocking(Tracker t, final Tracker.Event event) {
		if (LOG.isDebugEnabled())
			LOG.debug("connecting to tracker " + t.toString()+" for event "+event);
		TrackerResponse response = t.request(event);
		handleTrackerResponse(response, t);
	}
	
	private void announceToAll(final Tracker.Event event) {
		// announce ourselves to the trackers
		for (final Tracker t : trackers) {
			Runnable announcer = new Runnable(){
				public void run() {
					announceBlocking(t,event);
				}
			};
			requestQueue.execute(announcer);
		}
	}
	
	public void announceStart() {
		_nextTrackerRequestTime = 0;
		announceToAll(Tracker.Event.START);
	}
	
	public void announceStop() {
		// stop any current or future announcements
        ScheduledFuture<?>  t = scheduledAnnounce;
		if (t != null)
			t.cancel(true);
		
		announceToAll(Tracker.Event.STOP);	
	}
	
	public void announceComplete() {
		announceToAll(Tracker.Event.COMPLETE);
	}
	
	/**
	 * Announces ourselves to a tracker
	 * 
	 * @param t the tracker to announce to
	 */
	private void announce(final Tracker t) {
		if (LOG.isDebugEnabled())
			LOG.debug("announce thread for " + t.toString());
		torrent.setScraping();
		announceBlocking(t, Tracker.Event.NONE);
	}
	
	/**
	 * @return whether we've failed to connect to any tracker
	 * too many times and should give up.
	 */
	private boolean isHopeless() {
		if (trackers.isEmpty())
			return true;
		
		int least = Integer.MAX_VALUE;
		for(Tracker t: trackers) {
			
			// shortcut
			if (t.getFailures() == 0)
				return false;
			
			if (t.getFailures() < least)
				least = t.getFailures();
		}
		return least >= MAX_TRACKER_FAILURES;
	}
	
	private void scheduleTrackerRequest(long minDelay, final Tracker t) {
		final Runnable announcer = new Runnable() {
			public void run() {
				if (!torrent.isActive())
					return;
				
				if (LOG.isDebugEnabled())
					LOG.debug("announcing to " + t.toString());
				announce(t);
			}
		};
		Runnable scheduled = new Runnable() {
			public void run() {
				requestQueue.execute(announcer);
			}
		};
		LOG.debug("scheduling new tracker request");
		if (scheduledAnnounce != null)
			scheduledAnnounce.cancel(true);
		scheduledAnnounce = backgroundExecutor.schedule(scheduled, minDelay, TimeUnit.MILLISECONDS);
		_nextTrackerRequestTime = System.currentTimeMillis() + minDelay;
	}
	
	public long getNextTrackerRequestTime() {
		return _nextTrackerRequestTime;
	}
	
    public String getLastFailureReason() {
        return lastFailureReason;
    }
    
	/**
	 * This method handles the response from a tracker
	 */
	private void handleTrackerResponse(TrackerResponse response, Tracker t) {
		LOG.debug("handling tracker response "+response+" from " + t);

		long minWaitTime = BittorrentSettings.TRACKER_MIN_REASK_INTERVAL
				.getValue() * 1000;
		
		if (response != null) {
				for (TorrentLocation next : response.PEERS) 
					torrent.addEndpoint(next);
				
				minWaitTime = response.INTERVAL * 1000;
				
				if (response.FAILURE_REASON != null) {
					t.recordFailure();
                    lastFailureReason = response.FAILURE_REASON;
                    torrent.trackerRequestFailed();
				} else
					t.recordSuccess();
		} else {
			t.recordFailure();
			torrent.trackerRequestFailed();
		}

		if (torrent.isActive()) {
			if (isHopeless() && torrent.shouldStop()) {
				torrent.stopVoluntarily();
			} else
				scheduleTrackerRequest(minWaitTime, t);
		}
	}
}
