package com.limegroup.gnutella.filters;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.io.GUID;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryRequest;

public class AnomalousQueryFilterTest extends BaseTestCase {

    public AnomalousQueryFilterTest(String name){
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AnomalousQueryFilterTest.class);
    }

    QueryRequest query;
    PingRequest ping;
    AnomalousQueryFilter filter;
    Mockery context;

    @Override
    public void setUp() throws Exception {
        context = new Mockery();
        query = context.mock(QueryRequest.class);
        ping = context.mock(PingRequest.class); 
        filter = new AnomalousQueryFilter();
    }

    /**
     * Returns a GUID that starts with 4 fixed bytes
     */
    private static byte[] makeSuspiciousGuid() {
        byte[] guid = GUID.makeGuid();
        guid[0] = 0;
        guid[1] = 1;
        guid[2] = 2;
        guid[3] = 3;
        return guid;
    }

    /**
     * Tests that suspicious queries are not blocked until sufficient
     * queries have been seen to identify suspicious queries reliably
     */
    public void testAllowedUntilTotalIsSufficient() {
        final int total = AnomalousQueryFilter.PREFIXES_TO_COUNT;
        context.checking(new Expectations() {{
            exactly(total - 1).of(query).getGUID();
            will(returnValue(makeSuspiciousGuid()));
        }});
        for(int i = 0; i < total - 1; i++) {
            assertTrue(filter.allow(query));
        }
        context.assertIsSatisfied();
    }

    /**
     * Tests that suspicious queries are blocked as soon as sufficient
     * queries have been seen to identify suspicious queries reliably
     */
    public void testBlockedWhenTotalIsSufficient() {
        final int total = AnomalousQueryFilter.PREFIXES_TO_COUNT;
        context.checking(new Expectations() {{
            exactly(total).of(query).getGUID();
            will(returnValue(makeSuspiciousGuid()));
            // These methods will be called for the last query
            one(query).desiresOutOfBandReplies();
            will(returnValue(false));
            one(query).getMinSpeed();
            will(returnValue(0));
        }});
        for(int i = 0; i < total - 1; i++) {
            assertTrue(filter.allow(query));
        }
        // The last query should be blocked
        assertFalse(filter.allow(query));
        context.assertIsSatisfied();
    }

    /**
     * Tests that suspicious queries will not be blocked unless they
     * make up a sufficient fraction of observed queries
     */
    public void testAllowedUntilFractionIsSufficient() {
        final int total = AnomalousQueryFilter.PREFIXES_TO_COUNT;
        final int suspiciousCount =
            (int)(AnomalousQueryFilter.PREFIXES_TO_COUNT
                    * AnomalousQueryFilter.MAX_FRACTION_PER_PREFIX);
        final int innocentCount = total - suspiciousCount;
        context.checking(new Expectations() {{
            exactly(innocentCount).of(query).getGUID();
            will(returnValue(GUID.makeGuid()));
            exactly(suspiciousCount).of(query).getGUID();
            will(returnValue(makeSuspiciousGuid()));
        }});
        for(int i = 0; i < total; i++) {
            assertTrue(filter.allow(query));
        }
        context.assertIsSatisfied();
    }

    /**
     * Tests that suspicious queries will be blocked as soon as they
     * make up a sufficient fraction of observed queries
     */
    public void testBlockedWhenFractionIsSufficient() {
        final int total = AnomalousQueryFilter.PREFIXES_TO_COUNT;
        final int suspiciousCount =
            (int)(AnomalousQueryFilter.PREFIXES_TO_COUNT
                    * AnomalousQueryFilter.MAX_FRACTION_PER_PREFIX) + 1;
        final int innocentCount = total - suspiciousCount;
        context.checking(new Expectations() {{
            exactly(innocentCount).of(query).getGUID();
            will(returnValue(GUID.makeGuid()));
            exactly(suspiciousCount).of(query).getGUID();
            will(returnValue(makeSuspiciousGuid()));
            // These methods will be called for the last query
            one(query).desiresOutOfBandReplies();
            will(returnValue(false));
            one(query).getMinSpeed();
            will(returnValue(0));
        }});
        for(int i = 0; i < total - 1; i++) {
            assertTrue(filter.allow(query));
        }
        // The last query should be blocked
        assertFalse(filter.allow(query));
        context.assertIsSatisfied();        
    }
    
    /**
     * Tests that suspicious queries will only be blocked if they
     * don't ask for out of band results
     */
    public void testAllowedIfAsksForOutOfBand() {
        final int total = AnomalousQueryFilter.PREFIXES_TO_COUNT;
        final int suspiciousCount =
            (int)(AnomalousQueryFilter.PREFIXES_TO_COUNT
                    * AnomalousQueryFilter.MAX_FRACTION_PER_PREFIX) + 1;
        final int innocentCount = total - suspiciousCount;
        context.checking(new Expectations() {{
            exactly(innocentCount).of(query).getGUID();
            will(returnValue(GUID.makeGuid()));
            exactly(suspiciousCount).of(query).getGUID();
            will(returnValue(makeSuspiciousGuid()));
            // This method will be called for the last query
            one(query).desiresOutOfBandReplies();
            will(returnValue(true));
            // Minimum speed will never be checked
        }});
        for(int i = 0; i < total; i++) {
            assertTrue(filter.allow(query));
        }
        context.assertIsSatisfied();        
    }
    
    /**
     * Tests that suspicious queries will only be blocked if they
     * have non-zero minimum speed
     */
    public void testAllowedIfNonZeroMinimumSpeed() {
        final int total = AnomalousQueryFilter.PREFIXES_TO_COUNT;
        final int suspiciousCount =
            (int)(AnomalousQueryFilter.PREFIXES_TO_COUNT
                    * AnomalousQueryFilter.MAX_FRACTION_PER_PREFIX) + 1;
        final int innocentCount = total - suspiciousCount;
        context.checking(new Expectations() {{
            exactly(innocentCount).of(query).getGUID();
            will(returnValue(GUID.makeGuid()));
            exactly(suspiciousCount).of(query).getGUID();
            will(returnValue(makeSuspiciousGuid()));
            // These methods will be called for the last query
            one(query).desiresOutOfBandReplies();
            will(returnValue(false));
            one(query).getMinSpeed();
            will(returnValue(QueryRequest.SPECIAL_MINSPEED_MASK));
        }});
        for(int i = 0; i < total; i++) {
            assertTrue(filter.allow(query));
        }
        context.assertIsSatisfied();        
    }
    
    public void testOtherMessagesAreIgnored() throws Exception {
        context.checking(new Expectations() {{
            never(ping);
        }});        
        assertTrue(filter.allow(ping));
        context.assertIsSatisfied();
    }
}