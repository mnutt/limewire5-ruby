package com.limegroup.gnutella.auth;

import org.limewire.core.settings.ContentSettings;
import org.limewire.io.IpPortImpl;

import com.google.inject.Injector;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Nightly integration test to ensure filtering server is running.
 */
public class ContentAuthServerTest extends LimeTestCase {

    private ContentManager contentManager;
    private UDPService udpService;
    private Injector injector;

    public ContentAuthServerTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        ContentSettings.CONTENT_MANAGEMENT_ACTIVE.setValue(true);
        ContentSettings.USER_WANTS_MANAGEMENTS.setValue(true);
        
        injector = LimeTestUtils.createInjectorAndStart();
        contentManager = injector.getInstance(ContentManager.class);
        udpService = injector.getInstance(UDPService.class);
    }
    
    @Override
    protected void tearDown() throws Exception {
        injector.getInstance(LifecycleManager.class).shutdown();
    }
    
    public void testRequestBlockedUrn() throws Exception {
        contentManager.setContentAuthority(new IpPortContentAuthority(new IpPortImpl("fserv1.limewire.com", 10000), udpService));
        ContentResponseData contentResponseData = contentManager.request(URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"), 10 * 10000);
        assertNotNull(contentResponseData);
        assertFalse(contentResponseData.isOK());
    }
    
    public void testRequestNonBlockedUrn() throws Exception {
        contentManager.setContentAuthority(new IpPortContentAuthority(new IpPortImpl("fserv1.limewire.com", 10000), udpService));
        ContentResponseData contentResponseData = contentManager.request(URN.createUrnFromString("urn:sha1:BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB"), 10 * 10000);
        assertNotNull(contentResponseData);
        assertTrue(contentResponseData.isOK());
    }
}
