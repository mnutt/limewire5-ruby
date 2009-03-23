package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;
import org.limewire.util.GenericsUtils;
import org.limewire.util.TestUtils;
import org.limewire.util.GenericsUtils.ScanMode;

import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.helpers.UrnHelper;

public class DownloadConversionTest extends BaseTestCase {

    public DownloadConversionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(DownloadConversionTest.class);
    }

    public void testConversion() throws Exception {
        File file = TestUtils
                .getResourceFile("com/limegroup/gnutella/downloader/serial/conversion/allKindsOfDownloads.dat");
        ObjectInputStream in = new DownloadConverterObjectInputStream(new FileInputStream(file));
        Object read1 = in.readObject();
        Object read2 =  in.readObject();
        try {
            Object read3 = in.readObject();
            fail("read another object: " + read3);
        } catch (EOFException expected) {
        }

        List<SerialRoot> serialRead = GenericsUtils.scanForList(read1, SerialRoot.class,
                ScanMode.EXCEPTION);
        SerialIncompleteFileManager serialIncompleteFileManager = (SerialIncompleteFileManager) read2;
        
        { // test ManagedDownloader conversion
            SerialManagedDownloader smd = (SerialManagedDownloader)serialRead.get(0);
            SerialRemoteFileDesc srfd = smd.getDefaultRFD();
            Set<SerialRemoteFileDesc> setSRFD = smd.getRemoteFileDescs();
            SerialIncompleteFileManager sifm = smd.getIncompleteFileManager();
            Map<String, Serializable> properties = smd.getProperties();
            
            assertSame(sifm, serialIncompleteFileManager);
            assertEquals(1, setSRFD.size());
            assertSame(srfd, setSRFD.iterator().next());
            assertEquals("127.0.0.1", srfd.getHost());
            assertEquals(1, srfd.getPort());
            assertEquals(1, srfd.getIndex());
            assertEquals("fileA.txt", srfd.getFilename());
            assertEquals(123, srfd.getSize());
            // CHECK GUID!!!!
            assertEquals(1, srfd.getSpeed());
            assertEquals(true, srfd.isChatEnabled());
            assertEquals(1, srfd.getQuality());
            assertEquals(false, srfd.isBrowseHostEnabled());
            assertEquals(null, srfd.getXml());
            assertEquals(UrnHelper.URN_SETS[0], srfd.getUrns());
            assertEquals(false, srfd.isReplyToMulticast());
            assertEquals(true, srfd.isFirewalled());
            assertEquals("MNGD", srfd.getVendor());
            assertNull(srfd.getPropertiesMap());
            Map<?, ?> attributes = (Map)properties.get("attributes");
            assertEquals("VALUE", attributes.get("KEY"));
            assertEquals(123L, properties.get("fileSize"));
            assertEquals("fileA.txt", properties.get("defaultFileName"));
            assertEquals(UrnHelper.URNS[0], properties.get("sha1Urn"));
        }
            
        {
            SerialStoreDownloader ssd = (SerialStoreDownloader)serialRead.get(1);
            SerialRemoteFileDesc srfd = ssd.getDefaultRFD();
            Set<SerialRemoteFileDesc> setSRFD = ssd.getRemoteFileDescs();
            SerialIncompleteFileManager sifm = ssd.getIncompleteFileManager();
            Map<String, Serializable> properties = ssd.getProperties();
            
            assertSame(sifm, serialIncompleteFileManager);
            assertEquals(1, setSRFD.size());
            assertSame(srfd, setSRFD.iterator().next());
            assertEquals("127.0.0.2", srfd.getHost());
            assertEquals(2, srfd.getPort());
            assertEquals(1, srfd.getIndex());
            assertEquals("fileB.txt", srfd.getFilename());
            assertEquals(123, srfd.getSize());
            // CHECK GUID!!!!
            assertEquals(1, srfd.getSpeed());
            assertEquals(true, srfd.isChatEnabled());
            assertEquals(1, srfd.getQuality());
            assertEquals(false, srfd.isBrowseHostEnabled());
            assertEquals(null, srfd.getXml());
            assertEquals(UrnHelper.URN_SETS[0], srfd.getUrns());
            assertEquals(false, srfd.isReplyToMulticast());
            assertEquals(true, srfd.isFirewalled());
            assertEquals("STOR", srfd.getVendor());
            assertNull(srfd.getPropertiesMap());
            assertEquals(123L, properties.get("fileSize"));
            assertEquals("fileB.txt", properties.get("defaultFileName"));
            assertEquals(UrnHelper.URNS[0], properties.get("sha1Urn"));
        }
        
        {
            // No need to check anything other than existence -- we don't convert these.
            assertInstanceof(SerialInNetworkDownloader.class, serialRead.get(2));
        }
        
        {
            SerialResumeDownloader srd = (SerialResumeDownloader)serialRead.get(3);
            SerialRemoteFileDesc srfd = srd.getDefaultRFD();
            Set<SerialRemoteFileDesc> setSRFD = srd.getRemoteFileDescs();
            SerialIncompleteFileManager sifm = srd.getIncompleteFileManager();
            
            assertSame(sifm, serialIncompleteFileManager);
            assertEquals(0, setSRFD.size());
            assertNull(srfd);
            assertEquals("T-123453-incompleteName", srd.getIncompleteFile().getName());
            assertEquals("incompleteName", srd.getName());
            assertEquals(123453L, srd.getSize());
        }
        
        {
            SerialMagnetDownloader smd = (SerialMagnetDownloader)serialRead.get(4);
            SerialRemoteFileDesc srfd = smd.getDefaultRFD();
            Set<SerialRemoteFileDesc> setSRFD = smd.getRemoteFileDescs();
            SerialIncompleteFileManager sifm = smd.getIncompleteFileManager();
            Map<String, Serializable> properties = smd.getProperties();
                        
            assertSame(sifm, serialIncompleteFileManager);
            assertEquals(0, setSRFD.size());
            assertNull(srfd);
            assertNull(smd.getTextQuery());
            assertNull(smd.getUrn());
            assertNull(smd.getFilename());
            assertNull(smd.getDefaultUrls());
            assertEquals("magnetName", properties.get("defaultFileName"));
            MagnetOptions mo = (MagnetOptions)properties.get("MAGNET");
            assertEquals("magnetName", mo.getDisplayName());
            assertEquals(UrnHelper.URNS[1], mo.getSHA1Urn());
            assertEquals(Collections.singletonList("http://127.0.0.3:3/uri-res/N2R?" + UrnHelper.URNS[1]), mo.getXS());
        }
        
        {
            // No need to do anything other than check these can be deserialized.
            assertInstanceof(SerialRequeryDownloader.class, serialRead.get(5));
        }
        
        {
            SerialBTDownloader sbtd = (SerialBTDownloader)serialRead.get(6);
            Map<String, Serializable> properties = sbtd.getProperties();
            assertEquals("btName", properties.get("defaultFileName"));
            SerialBTMetaInfo info = (SerialBTMetaInfo)properties.get("metainfo");
            assertEquals("http://www.example.com/announce", info.getTrackers()[0].toString());
            assertEquals(UrnHelper.URNS[3].getBytes(), info.getInfoHash());
            assertEquals(123L, info.getFileSystem().getTotalSize());
            assertEquals(1235L, info.getPieceLength());
            assertEquals(true, info.isPrivate());
        }

    }

}
