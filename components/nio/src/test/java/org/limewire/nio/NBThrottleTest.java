package org.limewire.nio;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;

import org.limewire.nio.observer.StubReadWriteObserver;
import org.limewire.util.BaseTestCase;
import org.limewire.util.PrivilegedAccessor;

/**
 * Tests that Throttle does its thing.
 */
@SuppressWarnings( { "unchecked", "cast" } )
public final class NBThrottleTest extends BaseTestCase {

    private final int TICKS_PER_SECOND = 10;
    private final float RATE = 3 * 1024; // 3KB/s
    private final int BYTES_PER_TICK = (int)((float)RATE / TICKS_PER_SECOND);
    private final int MILLIS_PER_TICK = 1000 / TICKS_PER_SECOND;
    
    private Data[] DATA = new Data[50];
    private NBThrottle THROTTLE;

	public NBThrottleTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(NBThrottleTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	@Override
    public void setUp() throws Exception {
	    THROTTLE = newNBThrottle(true, RATE, MILLIS_PER_TICK);
	    
	    for(int i = 0; i < DATA.length; i++)
	        DATA[i] = new Data(THROTTLE);

        // control.
	    for(int i = 0; i < DATA.length; i++)
	        assertEquals(0, DATA[i].STUB.bandwidthAvailableCalls());
	}
	
	// nothing should have bandwidth if nothing was interested
	public void testNoBandwidthIfNothingInterested() throws Exception {
        THROTTLE.tick(0);
	    for(int i = 0; i < DATA.length; i++)
	        assertEquals(0, DATA[i].STUB.bandwidthAvailableCalls());
    }   
	
	// only those that were interested should be given bandwidth.
	public void testBandwidthOnTickWithInterest() throws Exception {
        // Set interest on some things, tick & check.
	    for(int i = 0; i < DATA.length; i+=3)
	        THROTTLE.interest(DATA[i].STUB);
	    THROTTLE.tick(1000);
	    
	    for(int i = 0; i < DATA.length; i++)
	        assertEquals(i%3==0 ? 1 : 0, DATA[i].STUB.bandwidthAvailableCalls());
    }
    
    // make sure that if bandwidth is still available, ticks within the tick interval
    // will spread bandwidth also.
    public void testBandwidthWithinTickIntervalSpreadsAvailable() throws Exception {
	    for(int i = 0; i < DATA.length; i+=3)
	        THROTTLE.interest(DATA[i].STUB);
	    THROTTLE.tick(1000);
        
        for(int i = 0; i < DATA.length; i+=5)
	        THROTTLE.interest(DATA[i].STUB);            
        THROTTLE.tick(1001);
     
        // if this wasn't working (i%5 && !i%3) would give 0 available.   
	    for(int i = 0; i < DATA.length; i++) {
	    	int calls = DATA[i].STUB.bandwidthAvailableCalls();
	    	if (i%3==0 || i%5 == 0)
	    		assertGreaterThan(0, calls);
	    	else 
	    		assertEquals(0, calls);
	    }
    }
    
    // make sure it gives no bandwidth if it isn't active (within a selectableKeys call)
    public void testNoRequestIfNotActive() throws Exception {
        THROTTLE.tick(1000);
        assertEquals(0, THROTTLE.request());
    }
    
    // tests the internal implementation details of NBThrottle --
    // but, if this is wrong, all other tests aren't going to work
    // ('cause they rely on this being the implementation)
    public void testBytesPerTick() throws Exception {
        int bytesPerTick = ((Integer)PrivilegedAccessor.getValue(THROTTLE, "_bytesPerTick")).intValue();
        assertEquals(BYTES_PER_TICK, bytesPerTick);
    }
    
    // make sure we give bandwidth within a selectableKeys after it was active.
    public void testRequestWithinSelectable() throws Exception {
        THROTTLE.interest(DATA[0].STUB);
        THROTTLE.tick(1000);
        THROTTLE.selectableKeys(set(DATA[0].KEY));
        assertEquals(BYTES_PER_TICK, DATA[0].STUB.given());
    } 
    
    // make sure that no bandwidth is given if we're ticked and bandwidth is used up.
    public void testNoBandwidthIfEmptyWithinTickInterval() throws Exception {
        THROTTLE.interest(DATA[0].STUB);
        THROTTLE.tick(1000);
        assertEquals(1, DATA[0].STUB.bandwidthAvailableCalls());
        THROTTLE.selectableKeys(set(DATA[0].KEY));
        assertEquals(BYTES_PER_TICK, DATA[0].STUB.given());
        DATA[0].STUB.clear();
        
	    THROTTLE.interest(DATA[1].STUB);
	    THROTTLE.tick(1001);
	    assertEquals(0, DATA[0].STUB.bandwidthAvailableCalls());
	    assertEquals(0, DATA[1].STUB.bandwidthAvailableCalls());
    }
    
    // make sure bandwidth fills back up after tick interval
    public void testBandwidthFillsAfterTickInterval() throws Exception {
        THROTTLE.interest(DATA[0].STUB);
        THROTTLE.tick(1000);
        assertEquals(1, DATA[0].STUB.bandwidthAvailableCalls());
        THROTTLE.selectableKeys(set(DATA[0].KEY));
        assertEquals(BYTES_PER_TICK, DATA[0].STUB.given());
        DATA[0].STUB.clear();
        
	    THROTTLE.interest(DATA[1].STUB);
	    THROTTLE.tick(1001);
	    assertEquals(0, DATA[0].STUB.bandwidthAvailableCalls());
	    assertEquals(0, DATA[1].STUB.bandwidthAvailableCalls());
	    
	    THROTTLE.tick(1000 + MILLIS_PER_TICK + 1);
	    assertEquals(1, DATA[1].STUB.bandwidthAvailableCalls());
	    THROTTLE.selectableKeys(set(DATA[1].KEY));
        assertEquals(BYTES_PER_TICK, DATA[1].STUB.given());
    }
    
    // make sure that if the first interested ready host used up all the bandwidth,
    // the next one won't be told to write.
    public void testStopsReadiesWhenNoneLeft() throws Exception {
        // interest in two different ticks to ensure that 0 is in line before 1.
        THROTTLE.interest(DATA[0].STUB);
        THROTTLE.tick(1000);
        THROTTLE.interest(DATA[1].STUB);
        THROTTLE.tick(2000); 
        
        THROTTLE.selectableKeys(set( new Object[] { DATA[0].KEY, DATA[1].KEY } ) );
        assertEquals(BYTES_PER_TICK, DATA[0].STUB.given());
        assertEquals(0, DATA[1].STUB.given());
        assertEquals(1, DATA[0].ATTACHMENT.wrote());
        assertEquals(0, DATA[1].ATTACHMENT.wrote());
    }

    // make sure that releases are added back into the pool & given to future folks.
    public void testReleaseGivesToReadies() throws Exception {
        // interest in two different ticks to ensure that 0 is in line before 1.
        THROTTLE.interest(DATA[0].STUB);
        THROTTLE.tick(1000);
        THROTTLE.interest(DATA[1].STUB);
        THROTTLE.tick(2000); 
        
        DATA[0].ATTACHMENT.setAmountToUse(BYTES_PER_TICK - 100);
        THROTTLE.selectableKeys(set( new Object[] { DATA[0].KEY, DATA[1].KEY } ) );
        assertEquals(BYTES_PER_TICK, DATA[0].STUB.given());
        assertEquals(100, DATA[1].STUB.given());
        assertEquals(1, DATA[0].ATTACHMENT.wrote());
        assertEquals(1, DATA[1].ATTACHMENT.wrote());
    }
    
    // make sure that if someone didn't get to write on this turn, they'll be told to write
    // before others on the next turn.
    public void testInterestGivenReadiesInOrderThroughSuccessiveCalls() throws Exception {
        THROTTLE.interest(DATA[0].STUB);
        THROTTLE.tick(1000);
        THROTTLE.interest(DATA[1].STUB);
        THROTTLE.tick(2000); 
        
        THROTTLE.selectableKeys(set( new Object[] { DATA[0].KEY, DATA[1].KEY } ) );
        assertEquals(BYTES_PER_TICK, DATA[0].STUB.given());
        assertEquals(0, DATA[1].STUB.given());
        assertEquals(1, DATA[0].ATTACHMENT.wrote());
        assertEquals(0, DATA[1].ATTACHMENT.wrote());
        
        //[1] didn't get used, so it's still in line.
        THROTTLE.interest(DATA[0].STUB);
        THROTTLE.tick(3000);
        DATA[1].ATTACHMENT.setAmountToUse(BYTES_PER_TICK - 50);
        
        // make sure the selector ticks so that the key can get processed
        NIODispatcher.instance().wakeup();
        Thread.sleep(10);
        
        THROTTLE.selectableKeys(set( new Object[] { DATA[0].KEY, DATA[1].KEY } ) );
        assertEquals(BYTES_PER_TICK, DATA[1].STUB.given());
        assertEquals(50, DATA[0].STUB.given());
        assertEquals(1, DATA[1].ATTACHMENT.wrote());
        assertEquals(2, DATA[0].ATTACHMENT.wrote());
    }
    
    // make sure that uninterested parties aren't given any data to write.
    public void testUniterestedNotUsed() throws Exception {
        THROTTLE.tick(1000);
        THROTTLE.selectableKeys(set(DATA[0].KEY));
        assertEquals(0, DATA[0].STUB.given());
        assertEquals(0, DATA[0].ATTACHMENT.wrote());
    }

    // tests that the tick interval is dependent on the millis per tick.
    public void testTickFollowsGivenMillis() throws Exception {
        THROTTLE = newNBThrottle(true, 5000, 77);
        fixDataThrottles();
        
        THROTTLE.interest(DATA[0].STUB);
        THROTTLE.tick(1000);
        assertEquals(1, DATA[0].STUB.bandwidthAvailableCalls());
        THROTTLE.selectableKeys(set(DATA[0].KEY));
                
        THROTTLE.interest(DATA[1].STUB);
        assertEquals(0, DATA[1].STUB.bandwidthAvailableCalls());
        THROTTLE.tick(1076);
        assertEquals(0, DATA[1].STUB.bandwidthAvailableCalls());
        THROTTLE.tick(1077);
        assertEquals(1, DATA[1].STUB.bandwidthAvailableCalls());
    }
    
    // tests whether retrieving the next tick time works properly
    public void testNextTickTime() {
    	// with nobody interested, the next tick is never
    	assertEquals(Long.MAX_VALUE, THROTTLE.nextTickTime());
    	THROTTLE.tick(0);
    	assertEquals(Long.MAX_VALUE, THROTTLE.nextTickTime());
    	
    	// with somebody interested, the next tick is 100
    	THROTTLE.interest(DATA[0].STUB);
    	assertEquals(100, THROTTLE.nextTickTime());
    	
    	// and until that time elapses, it stays 100
    	THROTTLE.tick(50);
    	assertEquals(100, THROTTLE.nextTickTime());
    	THROTTLE.tick(99);
    	assertEquals(100, THROTTLE.nextTickTime());
    	
    	// after it elapses, it becomes 200
    	THROTTLE.tick(100);
    	assertEquals(200, THROTTLE.nextTickTime());
    	
    	// if nobody is interested anymore, its never again
    	DATA[0].STUB.setClosed(true);
    	THROTTLE.selectableKeys( set (new Object[] { DATA[0].KEY }) );
    	assertEquals(Long.MAX_VALUE, THROTTLE.nextTickTime());
    	
    }
    
    private Set set(Object o) {
        Set set = new HashSet();
        set.add(o);
        return set;
    }
    
    private Set set(Object[] os) {
        Set set = new HashSet(Arrays.asList(os));
        return set;
    }
    
    private void fixDataThrottles() {
        for(int i = 0; i < DATA.length; i++)
            DATA[i].STUB.setThrottle(THROTTLE);
    }
	
	private static class Data {
        private StubReadWriteObserver ATTACHMENT;
        private StubThrottleListener STUB;
        private FakeSelectionKey KEY;
        Data(Throttle throttle) {
            ATTACHMENT  = new StubReadWriteObserver();
            STUB = new StubThrottleListener(ATTACHMENT, throttle);
            KEY =  new FakeSelectionKey(NIODispatcher.instance().new Attachment(ATTACHMENT));
        }
    }
    
    private NBThrottle newNBThrottle(boolean write, float bps, int millisPerTick) throws Exception {
        return (NBThrottle)PrivilegedAccessor.invokeConstructor(
            NBThrottle.class,
            new Object[] { new Boolean(write), new Float(bps), Boolean.FALSE, new Integer(millisPerTick)  },
            new Class[] { Boolean.TYPE, Float.TYPE, Boolean.TYPE, Integer.TYPE }
       );
    }
                                
}