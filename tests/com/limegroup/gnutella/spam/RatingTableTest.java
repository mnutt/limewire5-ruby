package com.limegroup.gnutella.spam;

import java.net.UnknownHostException;

import junit.framework.Test;

import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.SearchSettings;
import org.limewire.io.ConnectableImpl;

import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.util.LimeTestCase;

public class RatingTableTest extends LimeTestCase {

    private final String addr1 = "1.1.1.1", addr2 = "2.2.2.2";
    private final int port1 = 1111, port2 = 2222;
    private final String name1 = "abc.def", name2 = "ghi.jkl";
    private final int size1 = 12345, size2 = 67890;

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
        // Whitelist the first address so it gets a default rating
        String[] whitelist = new String[] {addr1, addr2};
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(whitelist);
        Injector inject = LimeTestUtils.createInjector();
        manager = inject.getInstance(SpamManager.class);
        rfdFactory = inject.getInstance(RemoteFileDescFactory.class);
    }

    /**
     * Tests that tokens with default ratings are not written to disk
     */
    public void testSavingAndLoadingDiscardsDefaultRatings() throws Exception {
        RatingTable table = manager.getRatingTable();
        RemoteFileDesc rfd1 = createRFD(addr1, port1, name1, size1);
        assertFalse(rfd1.isSpam());
        assertEquals(0f, rfd1.getSpamRating());
        // There should be five tokens: address, name, ext, size, approx size
        assertEquals(5, table.size());
        RemoteFileDesc rfd2 = createRFD(addr2, port2, name2, size2);
        manager.handleUserMarkedSpam(new RemoteFileDesc[]{rfd2});
        assertTrue(rfd2.isSpam());
        assertGreaterThan(rfd1.getSpamRating(), rfd2.getSpamRating());
        // There should now be ten tokens, five for each RFD
        assertEquals(10, table.size());
        // Save and load the ratings
        table.stop();
        table.start();
        // Tokens with default ratings should have been discarded
        assertEquals(5, table.size());
    }
    
    /**
     * Tests that the least-recently-used order of the rating table is
     * preserved when the ratings are saved and loaded
     */
    public void testSavingAndLoadingPreservesOrder() throws Exception {
        RatingTable table = manager.getRatingTable();
        // Create some tokens with non-default ratings
        for(int i = 0; i < 10; i++) {
            String address = "1.2.3." + i;
            String name = "foo" + i + ".bar";
            RemoteFileDesc rfd = createRFD(address, i + 1024, name, i * 1000);
            manager.handleUserMarkedSpam(new RemoteFileDesc[]{rfd});
        }
        // Get the least-recently-used token
        Token t = table.getLeastRecentlyUsed();
        // Save and load the ratings
        table.stop();
        table.start();
        // Check that the least-recently-used token hasn't changed
        assertEquals(t, table.getLeastRecentlyUsed());
    }

    private RemoteFileDesc createRFD(String addr, int port, String name,
            int size) throws UnknownHostException {
        RemoteFileDesc rfd = rfdFactory.createRemoteFileDesc(new ConnectableImpl(addr, port, false), 1, name, size,
                DataUtils.EMPTY_GUID, 3, false, 3, false, null, URN.NO_URN_SET,
                false, "ALT", 0L);
        // This would normally be called by the SearchResultHandler
        manager.calculateSpamRating(rfd);
        return rfd;
    }
}