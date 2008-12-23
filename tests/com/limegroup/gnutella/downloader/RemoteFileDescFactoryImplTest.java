package com.limegroup.gnutella.downloader;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.io.Address;
import org.limewire.io.GUID;
import org.limewire.util.BaseTestCase;

import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;

public class RemoteFileDescFactoryImplTest extends BaseTestCase {

    private RemoteFileDescFactory remoteFileDescFactory;
    private Mockery context;

    public RemoteFileDescFactoryImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(RemoteFileDescFactoryImplTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        Injector injector = LimeTestUtils.createInjector();
        remoteFileDescFactory = injector.getInstance(RemoteFileDescFactory.class);
    }
    
    public void testRemoteFileDescCreatorsAreAskedIfTheyCanCreateRFD() {
        final RemoteFileDescCreator creator = context.mock(RemoteFileDescCreator.class);
        remoteFileDescFactory.register(creator);
        final Address address = context.mock(Address.class);
        final byte[] clientGuid = GUID.makeGuid();
        context.checking(new Expectations() {{
            one(creator).canCreateFor(address);
            will(returnValue(true));
            one(creator).create(address, 1, "hello", 2, clientGuid, 1, true, 1, true, null, URN.NO_URN_SET, false, "vendor", -1, false);
            will(returnValue(null));
        }});
        RemoteFileDesc rfd = remoteFileDescFactory.createRemoteFileDesc(address, 1, "hello", 2, clientGuid, 1, true, 1, true, null, URN.NO_URN_SET, false, "vendor", -1, false);
        assertNull(rfd);
        context.assertIsSatisfied();
    }

}
