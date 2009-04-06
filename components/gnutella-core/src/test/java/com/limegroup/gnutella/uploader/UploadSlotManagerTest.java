package com.limegroup.gnutella.uploader;

import org.limewire.core.settings.UploadSettings;
import org.limewire.gnutella.tests.LimeTestCase;

import junit.framework.Test;

import com.limegroup.gnutella.BandwidthTrackerImpl;

public class UploadSlotManagerTest extends LimeTestCase {
	public UploadSlotManagerTest(String name){
		super(name);
	}

	public static Test suite() {
        return buildTestSuite(UploadSlotManagerTest.class);
    }
	
	private UploadSlotManager manager;
	
	@Override
    public void setUp() {
		UploadSettings.SOFT_MAX_UPLOADS.revertToDefault();
		UploadSettings.HARD_MAX_UPLOADS.revertToDefault();
		UploadSettings.UPLOAD_QUEUE_SIZE.revertToDefault();
		manager = new UploadSlotManagerImpl();
	}
	
	/**
	 * Tests case where poll requests with equal priority 
	 * are processed in a fifo manner until the queue fills up, 
	 * after that are rejected.
	 * Also tests cancelling of poll requests.
	 */
	public void testEqualPriorityPolling() throws Exception {
		UploadSettings.SOFT_MAX_UPLOADS.setValue(1);
		UploadSettings.HARD_MAX_UPLOADS.setValue(2);
		UploadSettings.UPLOAD_QUEUE_SIZE.setValue(1);
		UploadSlotUser queued = new UploadSlotUserAdapter();
		UploadSlotUser granted = new UploadSlotUserAdapter();
		// free slot
		assertEquals(0, manager.pollForSlot(granted, true, false));
		assertEquals(-1, manager.positionInQueue(granted));
		// queued
		assertEquals(1, manager.pollForSlot(queued, true, false));
		assertEquals(1, manager.getNumQueued());
		assertEquals(0, manager.positionInQueue(queued)); // queue starts at pos 0
		// reject
		assertEquals(-1, manager.pollForSlot(new UploadSlotUserAdapter(), true, false));
		// another reject
		assertEquals(-1, manager.pollForSlot(new UploadSlotUserAdapter(), true, false));
		// was already in queue, stays there.
		assertEquals(1, manager.pollForSlot(queued, true, false));
		assertEquals(1, manager.getNumQueued());
		assertEquals(0, manager.positionInQueue(queued));
		// the one that used the slot is done, the queued one is granted.
		manager.requestDone(granted);
		assertEquals(0, manager.pollForSlot(queued, true, false));
		assertEquals(0, manager.getNumQueued());
		
		// there are no slots - try a non-queuable poll which gets rejected
		assertEquals(-1, manager.pollForSlot(new UploadSlotUserAdapter(), false, false));
		assertEquals(0, manager.getNumQueued());
		
		// there is room in the queue - try to queue someone else - they get queued
		UploadSlotUser queued2 = new UploadSlotUserAdapter();
		assertEquals(1, manager.pollForSlot(queued2, true, false));
		assertEquals(1, manager.getNumQueued());
		
		// cancel the queued one, try to queue another one - queued
		manager.cancelRequest(queued2);
		assertEquals(0, manager.getNumQueued());
		assertEquals(1, manager.pollForSlot(new UploadSlotUserAdapter(), true, false));
		assertEquals(1, manager.getNumQueued());
	}
	
	/**
	 * Tests that listeners with equal priority get processed/queued/rejected in
	 * fifo manner and notified when a slot becomes available. 
	 */
	public void testEqualPriorityListeners() throws Exception {
		UploadSettings.SOFT_MAX_UPLOADS.setValue(1);
		UploadSettings.HARD_MAX_UPLOADS.setValue(2);
		UploadSettings.UPLOAD_QUEUE_SIZE.setValue(2);
		UploadSlotListenerAdapter granted = new UploadSlotListenerAdapter();
		UploadSlotListenerAdapter queued = new UploadSlotListenerAdapter();
		UploadSlotListenerAdapter queued2 = new UploadSlotListenerAdapter();
		UploadSlotListenerAdapter rejected = new UploadSlotListenerAdapter();
		
		// first is granted
		assertEquals(0, manager.requestSlot(granted, false));
		assertFalse(granted.notified);
		// second and third are queued
		assertEquals(1, manager.requestSlot(queued, false));
		assertEquals(2, manager.requestSlot(queued2, false));
		// last one rejected
		assertEquals(-1, manager.requestSlot(rejected, false));
		assertEquals(2, manager.getNumQueuedResumable());
		
		// when the one using the slot is done, the first queued one gets notified
		// and becomes active
		manager.requestDone(granted);
		assertEquals(1, manager.getNumQueuedResumable());
		assertTrue(queued.notified);
		assertFalse(queued2.notified);
		granted = queued;
		
		// queue a third one - there is room in the queue now
		UploadSlotListenerAdapter queued3 = new UploadSlotListenerAdapter();
		assertEquals(2, manager.requestSlot(queued3, false));
		
		// if the first queued one gives up their spot, the one behind
		// him will not get notified
		manager.cancelRequest(queued2);
		assertEquals(1, manager.getNumQueuedResumable());
		assertFalse(queued3.notified);
		
		// but if the one that was using the slot finishes,
		// the next in the queue gets notification.
		manager.cancelRequest(granted);
		assertEquals(0, manager.getNumQueuedResumable());
		assertTrue(queued3.notified);
	}
	
	/**
	 * tests that listeners with low priority get preempted should
	 * there be pollers with high priority.
	 */
	public void testPollerPreemptsLowPriorityListeners() throws Exception {
		UploadSettings.SOFT_MAX_UPLOADS.setValue(2);
		UploadSettings.HARD_MAX_UPLOADS.setValue(3);
		UploadSettings.UPLOAD_QUEUE_SIZE.setValue(1);
		
		UploadSlotUserAdapter poller = new UploadSlotUserAdapter();
		UploadSlotListenerAdapter lowPriority = new UploadSlotListenerAdapter();
		UploadSlotListenerAdapter lowPriority2 = new UploadSlotListenerAdapter();
		
		
		// start off with two active low priority listeners
		assertEquals(0, manager.requestSlot(lowPriority, false));
		assertEquals(0, manager.requestSlot(lowPriority2, false));
		
		// add a poller
		assertEquals(0, manager.pollForSlot(poller, true, false));
		// the listeners should have been preempted
		assertTrue(lowPriority.releaseSlot);
		assertTrue(lowPriority2.releaseSlot);
		// nothing on the queue
		assertEquals(0,manager.getNumQueuedResumable());
	}
	
	/**
	 * tests that listeners with low priority get preempted should
	 * there be a listener with high priority.
	 */	
	public void testListenerPreemptsLowPriorityListeners() throws Exception {
		UploadSettings.SOFT_MAX_UPLOADS.setValue(2);
		UploadSettings.HARD_MAX_UPLOADS.setValue(3);
		UploadSettings.UPLOAD_QUEUE_SIZE.setValue(1);
		
		UploadSlotListenerAdapter lowPriority = new UploadSlotListenerAdapter();
		UploadSlotListenerAdapter lowPriority2 = new UploadSlotListenerAdapter();
		UploadSlotListenerAdapter highPriority = new UploadSlotListenerAdapter();
		
		
		// start off with two active low priority listeners
		assertEquals(0, manager.requestSlot(lowPriority, false));
		assertEquals(0, manager.requestSlot(lowPriority2, false));
		
		// add a high-priority listener
		assertEquals(0, manager.requestSlot(highPriority, true));
		// the other listeners should have been preempted
		assertTrue(lowPriority.releaseSlot);
		assertTrue(lowPriority2.releaseSlot);
		// nothing on the queue
		assertEquals(0,manager.getNumQueuedResumable());
	}
	
	/**
	 * Tests that requests with low priority get queued if there are
	 * active requests with higher priority
	 */
	public void testLowerPriorityQueuableQueued() throws Exception {
		UploadSettings.SOFT_MAX_UPLOADS.setValue(3);
		UploadSettings.HARD_MAX_UPLOADS.setValue(4);
		UploadSettings.UPLOAD_QUEUE_SIZE.setValue(20);
		
		UploadSlotListenerAdapter lowListener = new UploadSlotListenerAdapter();
		UploadSlotListenerAdapter highListener = new UploadSlotListenerAdapter();
		
		UploadSlotUserAdapter lowPoller = new UploadSlotUserAdapter();
		UploadSlotUserAdapter highPoller = new UploadSlotUserAdapter();
		
		// if there is a high priority poller, everyone else gets queued
		// except the high priority listener
		assertEquals(0, manager.pollForSlot(highPoller, true, true));
		assertGreaterThan(0, manager.pollForSlot(lowPoller, true, false));
		assertEquals(0, manager.requestSlot(highListener, true));
		assertGreaterThan(0, manager.requestSlot(lowListener, false));
		manager = new UploadSlotManagerImpl();
		
		// if there is a high priority listener, everyone but the high
		// priority poller gets queued.  The latter gets in but does not
		// preempt the listener
		assertEquals(0, manager.requestSlot(highListener, true));
		assertGreaterThan(0, manager.requestSlot(lowListener, false));
		assertGreaterThan(0, manager.pollForSlot(lowPoller, true, false));
		assertEquals(0, manager.pollForSlot(highPoller, false, true));
		manager = new UploadSlotManagerImpl();
		
		// low priority pollers force low priority listeners to get queued
		// the rest go through and do not preempt it
		assertEquals(0, manager.pollForSlot(lowPoller, true, false));
		assertGreaterThan(0, manager.requestSlot(lowListener, false));
		assertEquals(0, manager.requestSlot(highListener, true));
		assertEquals(0, manager.pollForSlot(highPoller, true, true));
		assertFalse(lowPoller.releaseSlot);
	}
	
	/**
	 * tests that listeners get notified for available slot
	 * if there are no other users with high priority
	 */
	public void testListenersNotified() throws Exception {
		final int SLOTS = 3;
		UploadSettings.SOFT_MAX_UPLOADS.setValue(SLOTS);
		UploadSettings.HARD_MAX_UPLOADS.setValue(4);
		UploadSettings.UPLOAD_QUEUE_SIZE.setValue(20);
		UploadSlotListenerAdapter[] lowListener = new UploadSlotListenerAdapter[5];
		for (int i = 0; i < 5; i++)
			lowListener[i] = new UploadSlotListenerAdapter();
		
		UploadSlotListenerAdapter highListener = new UploadSlotListenerAdapter();
		UploadSlotListenerAdapter highListener2 = new UploadSlotListenerAdapter();
		
		// add two high listeners, all low listeners should get queued
		assertEquals(0, manager.requestSlot(highListener, true));
		assertEquals(0, manager.requestSlot(highListener2, true));
		for (int i = 0; i < lowListener.length; i++) {
			assertEquals(i+1, manager.requestSlot(lowListener[i], false));
			assertEquals(i, manager.positionInQueue(lowListener[i]));
		}
		assertEquals(lowListener.length, manager.getNumQueuedResumable());
		
		// kill one of the high listeners - none of the lower should not be notified
		manager.cancelRequest(highListener);
		for (UploadSlotListenerAdapter l : lowListener)
			assertFalse(l.notified);
		
		// kill the second high listeners - enough low listeners should be notified
		// to fill the slots.
		manager.cancelRequest(highListener2);
		assertTrue(lowListener[0].notified);
		assertTrue(lowListener[1].notified);
		assertTrue(lowListener[2].notified);
		// the rest should not be notified, but should move forward in the queue.
		for (int i = SLOTS; i < lowListener.length; i++) {
			assertFalse(lowListener[i].notified);
			assertEquals(i - SLOTS, manager.positionInQueue(lowListener[i]));
		}
		assertEquals(lowListener.length - SLOTS, manager.getNumQueuedResumable());
	}
	
	public void testMeasureBandwidth() throws Exception {
	    UploadSettings.SOFT_MAX_UPLOADS.setValue(2);
	    UploadSettings.HARD_MAX_UPLOADS.setValue(2);
	    UploadSettings.UPLOAD_QUEUE_SIZE.setValue(2);

        // add two high listeners, all low listeners should get queued
	    UploadSlotUserAdapter user1 = new UploadSlotUserAdapter(100);
        assertEquals(0, manager.pollForSlot(user1, false, false));
        for (int i = 0; i < 40; i++) {
            manager.measureBandwidth();
            Thread.sleep(90);
        }
        float bw = manager.getMeasuredBandwidth();
        assertGreaterThan(0.9f, bw);	    
        assertLessThan(1.1f, bw);
        bw = manager.getAverageBandwidth();
        assertGreaterThan(0.9f, bw);        
        assertLessThan(1.1f, bw);
	}
	
	
	private static class UploadSlotUserAdapter extends BandwidthTrackerImpl implements UploadSlotUser {

	    private int bandwidth;
	    
	    private int transfered;
	    
        public UploadSlotUserAdapter(int bandwidth) {
	        this.bandwidth = bandwidth;
	    }

	    public UploadSlotUserAdapter() {
	    }
	    
		boolean releaseSlot;
		public String getHost() {
			return null;
		}
		
		public void releaseSlot() {
			releaseSlot = true;
		}

        public void measureBandwidth() {
            transfered += bandwidth;
            measureBandwidth(transfered);
        }
		
	}
	
	private static class UploadSlotListenerAdapter 
	extends UploadSlotUserAdapter implements UploadSlotListener {
		boolean notified;
		public void slotAvailable() {notified = true;}

	}
}
