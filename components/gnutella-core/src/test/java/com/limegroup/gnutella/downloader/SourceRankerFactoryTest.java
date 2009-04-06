package com.limegroup.gnutella.downloader;

import java.util.Collections;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.util.BaseTestCase;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.UDPPinger;

public class SourceRankerFactoryTest extends BaseTestCase {

    private NetworkManager networkManager;
    private UDPPinger pinger;
    private MessageRouter messageRouter;
    private SourceRankerFactory factory;
    private Mockery context;
    private RemoteFileDescFactory remoteFileDescFactory;    

    public SourceRankerFactoryTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(SourceRankerFactoryTest.class);
    }    

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        networkManager = context.mock(NetworkManager.class);
        pinger = context.mock(UDPPinger.class);
        messageRouter = context.mock(MessageRouter.class);
        remoteFileDescFactory = context.mock(RemoteFileDescFactory.class);
        Module module = new AbstractModule() {
            @Override
            protected void configure() {
                bind(UDPPinger.class).toInstance(pinger);
                bind(NetworkManager.class).toInstance(networkManager);
                bind(MessageRouter.class).toInstance(messageRouter);
                bind(RemoteFileDescFactory.class).toInstance(remoteFileDescFactory);
            }
        };
        Injector injector = LimeTestUtils.createInjector(module);
        factory = injector.getInstance(SourceRankerFactory.class);
    }
    
    @Override
    protected void tearDown() throws Exception {
        DownloadSettings.USE_HEADPINGS.revertToDefault();
    }
    
    
    private void expectLegacyRanker() {
        context.checking(new Expectations() {{
            one(networkManager).canReceiveSolicited();
            will(returnValue(false));
        }});   
    }
    
    private void expectPingRanker() {
        context.checking(new Expectations() {{
            one(networkManager).canReceiveSolicited();
            will(returnValue(true));
        }});
    }
    
    public void testGetAppropriateRankerReturnsLegacyRanker() {
        // if we cannot do solicited udp, we should get the legacy ranker
        expectLegacyRanker();
        
        SourceRanker ranker = factory.getAppropriateRanker();
        assertTrue(ranker instanceof LegacyRanker);
        context.assertIsSatisfied();
    
        expectPingRanker();
        DownloadSettings.USE_HEADPINGS.setValue(false);
        
        ranker = factory.getAppropriateRanker();
        assertTrue(ranker instanceof LegacyRanker);
        context.assertIsSatisfied();
    }
    
    public void testGetAppropriateRankerReturnsPingRanker() {
        assertTrue(DownloadSettings.USE_HEADPINGS.getValue());
        expectPingRanker();
        
        SourceRanker ranker = factory.getAppropriateRanker();
        assertTrue(ranker instanceof FriendsFirstSourceRanker);
        context.assertIsSatisfied();
    }

    private SourceRanker getLegacyRanker() {
        expectLegacyRanker();
        SourceRanker ranker = factory.getAppropriateRanker();
        assertTrue(ranker instanceof LegacyRanker);
        context.assertIsSatisfied();
        return ranker;
    }
    
    private SourceRanker getFriendsFirstRanker() {
        expectPingRanker();
        SourceRanker ranker = factory.getAppropriateRanker();
        assertTrue(ranker instanceof FriendsFirstSourceRanker);
        context.assertIsSatisfied();
        return ranker;
    }
    
    public void testGetAppropriateRankerSourceRankerRemainsUnchanged() {
        // legacy ranker staying the same
        SourceRanker ranker = getLegacyRanker();
        expectLegacyRanker();
        assertSame(ranker, factory.getAppropriateRanker(ranker));
        context.assertIsSatisfied();
        
        // ping ranker
        ranker = getFriendsFirstRanker();
        
        expectPingRanker();
        assertSame(ranker, factory.getAppropriateRanker(ranker));
        context.assertIsSatisfied();
    }

    public void testGetAppropriateRankerSourceRankerChangesFromLegacyToPingRanker() {
        // legacy to ping
        SourceRanker original = getLegacyRanker();
        MeshHandler handler = context.mock(MeshHandler.class);
        original.setMeshHandler(handler);
        
        expectPingRanker();
        SourceRanker copy = factory.getAppropriateRanker(original);
        assertTrue(copy instanceof FriendsFirstSourceRanker);
        assertSame(handler, copy.getMeshHandler());
        
    }
    
    public void testGetAppropriateRankerSourceRankerChangesFromPingToLegacyRanker() {
        // ping to legacy
        SourceRanker original = getFriendsFirstRanker();
        MeshHandler handler = context.mock(MeshHandler.class);
        original.setMeshHandler(handler);
        
        expectLegacyRanker();
        SourceRanker copy = factory.getAppropriateRanker(original);
        assertTrue(copy instanceof LegacyRanker);
        assertSame(handler, copy.getMeshHandler());
    }
    
    public void testOriginalRankerIsStoppedAndValuesAreCopied() {
        final SourceRanker original = context.mock(SourceRanker.class);
        
        context.checking(new Expectations() {{
            one(original).stop();
            one(original).getMeshHandler();
            will(returnValue(null));
            one(original).getShareableHosts();
            will(returnValue(Collections.emptyList()));
        }});
        
        expectPingRanker();
        SourceRanker copy = factory.getAppropriateRanker(original);
        assertNotSame(original, copy);
        
        context.assertIsSatisfied();
    }
}
