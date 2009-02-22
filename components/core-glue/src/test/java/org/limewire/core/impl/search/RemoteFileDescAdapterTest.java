package org.limewire.core.impl.search;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.Category;
import org.limewire.core.impl.URNImpl;
import org.limewire.io.Address;
import org.limewire.io.IpPort;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;

public class RemoteFileDescAdapterTest extends BaseTestCase {

    public RemoteFileDescAdapterTest(String name) {
        super(name);
    }

    /**
     * Tests the most basic methods of the RemoteFileDescAdapter.
     */
    public void testBasics() throws Exception {
        Mockery context = new Mockery();

        final RemoteFileDesc remoteFileDesc1 = context.mock(RemoteFileDesc.class);
        final Set<IpPort> ipPorts = new HashSet<IpPort>();
        final Address address1 = context.mock(Address.class);
        final byte[] guid1 = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
        final String fileName1 = "remote file name 1.txt";
        final long fileSize = 1234L;
        final URN urn1 = URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA1");

        context.checking(new Expectations() {
            {
                allowing(remoteFileDesc1).getAddress();
                will(returnValue(address1));
                allowing(remoteFileDesc1).getClientGUID();
                will(returnValue(guid1));
                allowing(address1).getAddressDescription();
                will(returnValue("address 1 description"));
                allowing(remoteFileDesc1).getFileName();

                will(returnValue(fileName1));
                allowing(remoteFileDesc1).getSize();
                will(returnValue(fileSize));
                allowing(remoteFileDesc1).getXMLDocument();
                will(returnValue(null));
                allowing(remoteFileDesc1).getCreationTime();
                will(returnValue(5678L));
                allowing(remoteFileDesc1).getSHA1Urn();
                will(returnValue(urn1));
                allowing(remoteFileDesc1).isSpam();
                will(returnValue(false));
            }
        });

        RemoteFileDescAdapter remoteFileDescAdapter1 = new RemoteFileDescAdapter(remoteFileDesc1,
                ipPorts);

        assertEquals(Category.DOCUMENT, remoteFileDescAdapter1.getCategory());
        assertEquals("txt", remoteFileDescAdapter1.getFileExtension());
        assertEquals(fileName1, remoteFileDescAdapter1.getFileName());
        assertEquals(fileSize, remoteFileDescAdapter1.getSize());
        assertEquals(remoteFileDesc1, remoteFileDescAdapter1.getRfd());
        assertEquals(new URNImpl(urn1), remoteFileDescAdapter1.getUrn());
        assertFalse(remoteFileDescAdapter1.isLicensed());
        assertFalse(remoteFileDescAdapter1.isSpam());

        context.assertIsSatisfied();
    }

    /**
     * Tests the getRelevance of the RemoteFileDescAdapter.
     */
    public void testGetRelevance() throws Exception {
        final Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final RemoteFileDesc remoteFileDesc1 = context.mock(RemoteFileDesc.class);
        final Set<IpPort> ipPorts = new HashSet<IpPort>();
        final Address address1 = context.mock(Address.class);
        final byte[] guid1 = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
        final String fileName1 = "remote file name 1.txt";
        final long fileSize = 1234L;
        final URN urn1 = URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA1");

        context.checking(new Expectations() {
            {
                allowing(remoteFileDesc1).getAddress();
                will(returnValue(address1));
                allowing(remoteFileDesc1).getClientGUID();
                will(returnValue(guid1));
                allowing(address1).getAddressDescription();
                will(returnValue("address 1 description"));
                allowing(remoteFileDesc1).getFileName();
                will(returnValue(fileName1));
                allowing(remoteFileDesc1).getSize();
                will(returnValue(fileSize));
                allowing(remoteFileDesc1).getXMLDocument();
                will(returnValue(null));
                allowing(remoteFileDesc1).getCreationTime();
                will(returnValue(5678L));
                allowing(remoteFileDesc1).getSHA1Urn();
                will(returnValue(urn1));

                one(remoteFileDesc1).isBrowseHostEnabled();
                will(returnValue(false));
            }
        });

        RemoteFileDescAdapter remoteFileDescAdapter1 = new RemoteFileDescAdapter(remoteFileDesc1,
                ipPorts);

        assertEquals(1, remoteFileDescAdapter1.getRelevance());

        context.checking(new Expectations() {
            {
                one(remoteFileDesc1).isBrowseHostEnabled();
                will(returnValue(true));
            }
        });

        remoteFileDescAdapter1 = new RemoteFileDescAdapter(remoteFileDesc1, ipPorts);
        assertEquals(6, remoteFileDescAdapter1.getRelevance());

        final IpPort ipPort1 = context.mock(IpPort.class);
        final IpPort ipPort2 = context.mock(IpPort.class);

        context.checking(new Expectations() {
            {
                one(remoteFileDesc1).isBrowseHostEnabled();
                will(returnValue(false));
                InetAddress inetAddress1 = context.mock(InetAddress.class);
                InetSocketAddress inetSocketAddress1 = context.mock(InetSocketAddress.class);
                allowing(ipPort1).getInetAddress();
                will(returnValue(inetAddress1));
                allowing(ipPort1).getInetSocketAddress();
                will(returnValue(inetSocketAddress1));
                allowing(inetSocketAddress1).toString();
                will(returnValue("iport1"));
                InetAddress inetAddress2 = context.mock(InetAddress.class);
                InetSocketAddress inetSocketAddress2 = context.mock(InetSocketAddress.class);
                allowing(ipPort2).getInetAddress();
                will(returnValue(inetAddress2));
                allowing(ipPort2).getInetSocketAddress();
                will(returnValue(inetSocketAddress2));
                allowing(inetSocketAddress2).toString();
                will(returnValue("iport2"));

                allowing(inetAddress1).getAddress();
                will(returnValue(new byte[] { 1, 2, 3, 4 }));
                allowing(inetAddress2).getAddress();
                will(returnValue(new byte[] { 4, 3, 2, 1 }));
            }
        });

        ipPorts.add(ipPort1);
        ipPorts.add(ipPort2);

        remoteFileDescAdapter1 = new RemoteFileDescAdapter(remoteFileDesc1, ipPorts);
        assertEquals(2, remoteFileDescAdapter1.getRelevance());

        context.assertIsSatisfied();
    }
}
