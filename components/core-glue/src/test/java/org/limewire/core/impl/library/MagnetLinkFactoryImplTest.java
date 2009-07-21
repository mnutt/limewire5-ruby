package org.limewire.core.impl.library;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.URN;

public class MagnetLinkFactoryImplTest extends BaseTestCase {

    public MagnetLinkFactoryImplTest(String name) {
        super(name);
    }

    public void testCreateMagnetLinkLocalFileItem() throws Exception {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final NetworkManager networkManager = context.mock(NetworkManager.class);
        final ApplicationServices applicationServices = context.mock(ApplicationServices.class);

        MagnetLinkFactoryImpl magnetLinkFactoryImpl = new MagnetLinkFactoryImpl(networkManager,
                applicationServices);

        final CoreLocalFileItem coreLocalFileItem1 = context.mock(CoreLocalFileItem.class);
        final FileDetails fileDetails1 = context.mock(FileDetails.class);
        final String fileName1 = "filename1";
        final URN urn1 = URN.createSHA1Urn("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        final long size1 = 1234567;

        context.checking(new Expectations() {
            {
                one(coreLocalFileItem1).getFileDetails();
                will(returnValue(fileDetails1));
                one(networkManager).getAddress();
                will(returnValue(new byte[] { 1, 1, 1, 1 }));
                one(networkManager).getPort();
                will(returnValue(1234));
                one(applicationServices).getMyGUID();
                will(returnValue(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
                        16 }));
                one(fileDetails1).getFileName();
                will(returnValue(fileName1));
                one(fileDetails1).getSHA1Urn();
                will(returnValue(urn1));
                one(fileDetails1).getSize();
                will(returnValue(size1));
            }
        });
        String magnetLink1 = magnetLinkFactoryImpl.createMagnetLink(coreLocalFileItem1);

        assertEquals(
                "magnet:?&xt=urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA&dn=filename1&xs=http://1.1.1.1:1234/uri-res/N2R?urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA&xs=urn:guid:0102030405060708090A0B0C0D0E0F10&xl=1234567",
                magnetLink1);
        context.assertIsSatisfied();
    }
}
