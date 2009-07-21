package com.limegroup.gnutella.spam;

import java.net.UnknownHostException;

import junit.framework.Test;

import org.limewire.core.settings.SearchSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;

import com.google.inject.Injector;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;

public class RatingTableTest extends LimeTestCase {

    private final String addr = "1.1.1.1";
    private final int port = 1111;
    private final String name = "abc.def";
    private final int size = 12345;

    private SpamManager manager;
    private RemoteFileDescFactory rfdFactory;

    public RatingTableTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(RatingTableTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        SearchSettings.ENABLE_SPAM_FILTER.setValue(true);
        Injector inject = LimeTestUtils.createInjector();
        manager = inject.getInstance(SpamManager.class);
        rfdFactory = inject.getInstance(RemoteFileDescFactory.class);
    }
    
    /**
     * Tests that tokens with default ratings are not stored in the table
     */
    public void testDefaultRatingsAreNotStored() throws Exception {
        RatingTable table = manager.getRatingTable();
        RemoteFileDesc rfd = createRFD(addr, port, name, size);
        assertFalse(rfd.isSpam());
        assertEquals(0f, rfd.getSpamRating());
        assertEquals(0, table.size());
    }
    
    /**
     * Tests that tokens with non-default ratings are stored in the table
     */
    public void testNonDefaultRatingsAreStored() throws Exception {
        RatingTable table = manager.getRatingTable();
        RemoteFileDesc rfd = createRFD(addr, port, name, size);
        manager.handleUserMarkedSpam(new RemoteFileDesc[]{rfd});
        assertTrue(rfd.isSpam());
        assertGreaterThan(0f, rfd.getSpamRating());
        // There should be six tokens: address, name, ext, size, approx size,
        // client GUID
        assertEquals(6, table.size());        
    }

    /**
     * Tests that the size and least-recently-used order of the rating table are
     * preserved when the ratings are saved and loaded
     */
    public void testSavingAndLoadingPreservesSizeAndOrder() throws Exception {
        RatingTable table = manager.getRatingTable();
        // Create some tokens with non-default ratings
        for(int i = 0; i < 10; i++) {
            String address = "1.2.3." + i;
            String name = "foo" + i + "." + i; // Unique name and extension
            RemoteFileDesc rfd = createRFD(address, i + 1024, name, i * 1000);
            manager.handleUserMarkedSpam(new RemoteFileDesc[]{rfd});
        }
        // There should be six tokens for each RFD: address, name, ext, size,
        // approx size, client GUID
        assertEquals(60, table.size());
        // Get the least-recently-used token
        Token t = table.getLeastRecentlyUsed();
        // Save and load the ratings
        table.stop();
        table.start();
        // Check that the size hasn't changed
        assertEquals(60, table.size());
        // Check that the least-recently-used token hasn't changed
        assertEquals(t, table.getLeastRecentlyUsed());
    }

    private RemoteFileDesc createRFD(String addr, int port, String name,
            int size) throws UnknownHostException {
        RemoteFileDesc rfd = rfdFactory.createRemoteFileDesc(
                new ConnectableImpl(addr, port, false), 1, name, size,
                GUID.makeGuid(), 3, 3, false, null, URN.NO_URN_SET, false,
                "ALT", 0L);
        // This would normally be called by the SearchResultHandler
        manager.calculateSpamRating(rfd);
        return rfd;
    }
}