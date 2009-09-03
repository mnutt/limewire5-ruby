package com.limegroup.gnutella.filters;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.concurrent.SimpleTimer;
import org.limewire.core.settings.FilterSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.util.Base32;
import org.limewire.util.Visitor;

import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.spam.SpamManager;

public class URNFilterTest extends LimeTestCase {

    private Mockery context;
    private URNFilter urnFilter;
    private SpamManager spamManager;
    private final String good, badLocal, badRemote, badFile;

    public URNFilterTest(String name) {
        super(name);
        byte[] urn = new byte[20];
        for(int i = 0; i < 20; i++) {
            urn[i] = (byte)i;
        }
        good = Base32.encode(urn);
        for(int i = 0; i < 20; i++) {
            urn[i] = (byte)(i + 1);
        }
        badLocal = Base32.encode(urn);
        for(int i = 0; i < 20; i++) {
            urn[i] = (byte)(i + 2);
        }
        badRemote = Base32.encode(urn);
        for(int i = 0; i < 20; i++) {
            urn[i] = (byte)(i + 3);
        }
        badFile = Base32.encode(urn);
    }

    public static Test suite() {
        return buildTestSuite(URNFilterTest.class);
    }

    @Override
    protected void setUp() throws InterruptedException {
        FilterSettings.FILTERED_URNS_LOCAL.set(new String[] {badLocal});
        FilterSettings.FILTERED_URNS_REMOTE.set(new String[] {badRemote});
        context = new Mockery();
        spamManager = context.mock(SpamManager.class);
        urnFilter = new URNFilterImpl(spamManager,
                new StubURNBlacklistManager(), new SimpleTimer(true));
        final CountDownLatch loaded = new CountDownLatch(1);
        SpamFilter.LoadCallback callback = new SpamFilter.LoadCallback() {
            @Override
            public void spamFilterLoaded() {
                loaded.countDown();
            }
        };
        // The blacklist should not have been loaded yet
        assertNull(urnFilter.getBlacklist());
        urnFilter.refreshURNs(callback);
        assertTrue(loaded.await(10, TimeUnit.SECONDS));
        // The blacklist should now have been loaded
        assertNotNull(urnFilter.getBlacklist());
        context.assertIsSatisfied();
    }

    @Override
    protected void tearDown() {
        FilterSettings.FILTERED_URNS_LOCAL.revertToDefault();
        FilterSettings.FILTERED_URNS_REMOTE.revertToDefault();
    }

    public void testRefreshURNs() {
        // The blacklist should contain three URNs: one each from the local
        // setting, the remote setting, and the blacklist manager's file
        assertEquals(3, urnFilter.getBlacklist().size());
        assertTrue(urnFilter.getBlacklist().contains(badLocal));
        assertTrue(urnFilter.getBlacklist().contains(badRemote));
        assertTrue(urnFilter.getBlacklist().contains(badFile));
        context.assertIsSatisfied();
    }

    public void testAllow() throws Exception {
        final Set<URN> badURNs = new HashSet<URN>();
        badURNs.add(URN.createSHA1Urn("urn:sha1:" + badLocal));
        final Set<URN> goodURNs = new HashSet<URN>();
        goodURNs.add(URN.createSHA1Urn("urn:sha1:" + good));
        final Response response = context.mock(Response.class);
        final QueryReply queryReply = context.mock(QueryReply.class);
        context.checking(new Expectations() {{
            // The first reply should be rejected and passed to the spam manager
            one(queryReply).getResultsArray();
            will(returnValue(new Response[] {response}));
            one(response).getUrns();
            will(returnValue(badURNs));
            one(spamManager).handleSpamQueryReply(queryReply);
            // The second reply should be accepted
            one(queryReply).getResultsArray();
            will(returnValue(new Response[] {response}));
            one(response).getUrns();
            will(returnValue(goodURNs));
        }});
        assertFalse(urnFilter.allow(queryReply));
        assertTrue(urnFilter.allow(queryReply));
        context.assertIsSatisfied();
    }

    public void testIsBlacklistedQueryReply() throws Exception {
        final Set<URN> badURNs = new HashSet<URN>();
        badURNs.add(URN.createSHA1Urn("urn:sha1:" + badLocal));
        final Set<URN> goodURNs = new HashSet<URN>();
        goodURNs.add(URN.createSHA1Urn("urn:sha1:" + good));
        final Response response = context.mock(Response.class);
        final QueryReply queryReply = context.mock(QueryReply.class);
        context.checking(new Expectations() {{
            // The first reply should be rejected but not passed to the spam mgr
            one(queryReply).getResultsArray();
            will(returnValue(new Response[] {response}));
            one(response).getUrns();
            will(returnValue(badURNs));
            // The second reply should be accepted
            one(queryReply).getResultsArray();
            will(returnValue(new Response[] {response}));
            one(response).getUrns();
            will(returnValue(goodURNs));
        }});
        assertTrue(urnFilter.isBlacklisted(queryReply));
        assertFalse(urnFilter.isBlacklisted(queryReply));
        context.assertIsSatisfied();        
    }

    public void testIsBlacklistedURN() throws Exception {
        URN badURN = URN.createSHA1Urn("urn:sha1:" + badLocal);
        URN goodURN = URN.createSHA1Urn("urn:sha1:" + good);
        assertTrue(urnFilter.isBlacklisted(badURN));
        assertFalse(urnFilter.isBlacklisted(goodURN));
        context.assertIsSatisfied();
    }

    private class StubURNBlacklistManager implements URNBlacklistManager {
        @Override
        public void loadURNs(Visitor<String> visitor) {
            visitor.visit(badFile);
        }
    }
}
