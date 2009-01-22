package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.collection.Range;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.GUID;
import org.limewire.net.address.AddressFactory;
import org.limewire.util.NameValue;
import org.limewire.util.TestUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.downloader.DownloaderType;
import com.limegroup.gnutella.downloader.serial.BTDownloadMemento;
import com.limegroup.gnutella.downloader.serial.BTMetaInfoMemento;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;
import com.limegroup.gnutella.downloader.serial.GnutellaDownloadMemento;
import com.limegroup.gnutella.downloader.serial.MagnetDownloadMemento;
import com.limegroup.gnutella.downloader.serial.OldDownloadConverter;
import com.limegroup.gnutella.downloader.serial.RemoteHostMemento;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.util.LimeTestCase;

public class OldDownloadConverterImplTest extends LimeTestCase {
    private Injector injector;
    private OldDownloadConverterImpl oldDownloadConverter;
    private AddressFactory addressFactory;
    private PushEndpointFactory pushEndpointFactory;
    private Mockery context;


    public OldDownloadConverterImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(OldDownloadConverterImplTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        injector = LimeTestUtils.createInjector();
        oldDownloadConverter = (OldDownloadConverterImpl) injector.getInstance(OldDownloadConverter.class);
        addressFactory = injector.getInstance(AddressFactory.class);
        pushEndpointFactory = injector.getInstance(PushEndpointFactory.class);
//        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        context = new Mockery();
    }

    public void testConversionForTypes() throws Exception {
        File file = TestUtils.getResourceInPackage("allKindsOfDownloads.dat", DownloadUpgradeTask.class);
        
        List<DownloadMemento> mementos = oldDownloadConverter.readAndConvertOldDownloads(file);
        assertEquals(5, mementos.size());
        
        DownloadMemento read1 = mementos.get(0);
        DownloadMemento read2 = mementos.get(1);
        DownloadMemento read3 = mementos.get(2);
        DownloadMemento read4 = mementos.get(3);
        DownloadMemento read5 = mementos.get(4);
        
        {
            GnutellaDownloadMemento mem = (GnutellaDownloadMemento)read1;
            assertEquals(DownloaderType.MANAGED, mem.getDownloadType());
            assertEquals(new File("C:/Documents and Settings/Sam/Incomplete/T-123-fileA.txt"), mem.getIncompleteFile());
            assertEquals(0, mem.getSavedBlocks().size());
            assertEquals(1, mem.getRemoteHosts().size());
            RemoteHostMemento rmem = mem.getRemoteHosts().iterator().next();
            Address addr = rmem.getAddress(addressFactory, pushEndpointFactory);
            assertInstanceof(PushEndpoint.class, addr);
            PushEndpoint pe = (PushEndpoint)addr;
            assertNull(pe.getInetSocketAddress()); // It's a private address, so filtered out
            assertEquals(1, rmem.getIndex());
            assertEquals("fileA.txt", rmem.getFileName());
            assertEquals(123, rmem.getSize());
            // CHECK GUID!!!!
            assertEquals(1, rmem.getSpeed());
            assertEquals(true, rmem.isChat());
            assertEquals(1, rmem.getQuality());
            assertEquals(false, rmem.isBrowseHost());
            assertEquals(null, rmem.getXml());
            assertEquals(UrnHelper.URN_SETS[0], rmem.getUrns());
            assertEquals(false, rmem.isReplyToMulticast());
            assertEquals("MNGD", rmem.getVendor());
            
            Map<String, Object> attributes = mem.getAttributes();
            assertEquals("VALUE", attributes.get("KEY"));
            assertEquals(123L, mem.getContentLength());
            assertEquals("fileA.txt", mem.getDefaultFileName());
            assertEquals(UrnHelper.URNS[0], mem.getSha1Urn());
        }
            
        {
            GnutellaDownloadMemento mem = (GnutellaDownloadMemento)read2;
            assertEquals(DownloaderType.STORE, mem.getDownloadType());
            assertEquals(new File("C:/Documents and Settings/Sam/Incomplete/T-123-fileA.txt"), mem.getIncompleteFile());
            assertEquals(0, mem.getSavedBlocks().size());
            assertEquals(1, mem.getRemoteHosts().size());
            RemoteHostMemento rmem = mem.getRemoteHosts().iterator().next();
            Address addr = rmem.getAddress(addressFactory, pushEndpointFactory);
            assertInstanceof(PushEndpoint.class, addr);
            PushEndpoint pe = (PushEndpoint)addr;
            assertNull(pe.getInetSocketAddress()); // It's a private address, so filtered out
            assertEquals(1, rmem.getIndex());
            assertEquals("fileB.txt", rmem.getFileName());
            assertEquals(123, rmem.getSize());
            // CHECK GUID!!!!
            assertEquals(1, rmem.getSpeed());
            assertEquals(true, rmem.isChat());
            assertEquals(1, rmem.getQuality());
            assertEquals(false, rmem.isBrowseHost());
            assertEquals(null, rmem.getXml());
            assertEquals(UrnHelper.URN_SETS[0], rmem.getUrns());
            assertEquals(false, rmem.isReplyToMulticast());
            assertEquals("STOR", rmem.getVendor());
            assertEquals(new HashMap(), mem.getAttributes());
            assertEquals(123L, mem.getContentLength());
            assertEquals("fileB.txt", mem.getDefaultFileName());
            assertEquals(UrnHelper.URNS[0], mem.getSha1Urn());
        }
        
          
        {
            GnutellaDownloadMemento mem = (GnutellaDownloadMemento)read3;
            assertEquals(DownloaderType.MANAGED, mem.getDownloadType());
            // Normally the incompleteFile would be canonical -- we just didn't create it
            // that way in the test file.
            assertEquals(new File("T-123453-incompleteName"), mem.getIncompleteFile());
            assertEquals(0, mem.getSavedBlocks().size());
            assertEquals(0, mem.getRemoteHosts().size());
            assertEquals(new HashMap(), mem.getAttributes());
            assertEquals(123453L, mem.getContentLength());
            assertEquals("incompleteName", mem.getDefaultFileName());
            assertEquals(null, mem.getSha1Urn());
        }
        
        {
            MagnetDownloadMemento mem = (MagnetDownloadMemento)read4;
            assertEquals(DownloaderType.MAGNET, mem.getDownloadType());
            // Null because the download never started.
            assertEquals(null, mem.getIncompleteFile());
            assertEquals(0, mem.getSavedBlocks().size());
            assertEquals(0, mem.getRemoteHosts().size());
            assertEquals(-1, mem.getContentLength());
            assertEquals("magnetName", mem.getDefaultFileName());
            assertEquals(UrnHelper.URNS[1], mem.getSha1Urn());
            assertEquals(new HashMap(), mem.getAttributes());
            
            MagnetOptions mo = mem.getMagnet();
            assertEquals("magnetName", mo.getDisplayName());
            assertEquals(UrnHelper.URNS[1], mo.getSHA1Urn());
            assertEquals(Collections.singletonList("http://127.0.0.3:3/uri-res/N2R?" + UrnHelper.URNS[1]), mo.getXS());
        }
        
        {
            BTDownloadMemento mem = (BTDownloadMemento)read5;
            assertEquals(DownloaderType.BTDOWNLOADER, mem.getDownloadType());
            assertEquals("btName", mem.getDefaultFileName());
            BTMetaInfoMemento info = mem.getBtMetaInfoMemento();
            assertEquals("http://www.example.com/announce", info.getTrackers()[0].toString());
            assertEquals(UrnHelper.URNS[3].getBytes(), info.getInfoHash());
            assertEquals(123L, info.getFileSystem().getTotalSize());
            assertEquals(1235L, info.getPieceLength());
            assertEquals(true, info.isPrivate());
        }        
    }
    
    public void testConversionForRanges() throws Exception {
        File file = TestUtils.getResourceInPackage("allKindsOfRanges.dat", DownloadUpgradeTask.class);
        
        OldDownloadConverterImpl oldDownloadConverter = new OldDownloadConverterImpl(pushEndpointFactory, addressFactory);
        List<DownloadMemento> mementos = oldDownloadConverter.readAndConvertOldDownloads(file);
        assertEquals(6, mementos.size());
        
        DownloadMemento read1 = mementos.get(0);
        DownloadMemento read2 = mementos.get(1);
        DownloadMemento read3 = mementos.get(2);
        DownloadMemento read4 = mementos.get(3);
        DownloadMemento read5 = mementos.get(4);
        DownloadMemento read6 = mementos.get(5);
        
        {
            GnutellaDownloadMemento mem = (GnutellaDownloadMemento)read1;
            assertEquals(DownloaderType.MANAGED, mem.getDownloadType());
            assertEquals(new File("C:/Documents and Settings/sberlin/My Documents/LimeWire/Incomplete/T-4495072-LimeWireWin4.16.0.exe"), mem.getIncompleteFile());
            assertEquals(1, mem.getSavedBlocks().size());
            assertEquals(Range.createRange(3276800, 3316399), mem.getSavedBlocks().get(0));
            
//            Map<String, Object> attributes = mem.getAttributes();
//            SearchInformation so = SearchInformation.createFromMap((Map)attributes.get("searchInformationMap"));
//            assertEquals("*", so.getMediaType().getSchema());
//            assertEquals("limewire", so.getQuery());
//            assertEquals(null, so.getXML());
//            assertTrue(so.isKeywordSearch());
//            assertEquals("limewire", so.getTitle());
            assertEquals(4495072L, mem.getContentLength());
            assertEquals("LimeWireWin4.16.0.exe", mem.getDefaultFileName());
            assertEquals(URN.createSHA1Urn("urn:sha1:A6DGMXEOJDBQOIJUQTAQWSWC2IQKFD5J"), mem.getSha1Urn());
            
            assertEquals(2, mem.getRemoteHosts().size());
            Iterator<RemoteHostMemento> mementoIterator = mem.getRemoteHosts().iterator();            
            RemoteHostMemento rmem = mementoIterator.next();
            RemoteHostMemento rmem2 = mementoIterator.next();
            
            // Since remoteHosts is a HashSet, it can be out-of-order..
            if(2147483647 == rmem.getSpeed()) {
                RemoteHostMemento tmp = rmem;
                rmem = rmem2;
                rmem2 = tmp;
            }   
            
            Address address = rmem.getAddress(addressFactory, pushEndpointFactory);
            assertInstanceof(Connectable.class, address);
            Connectable connectable = (Connectable)address;
            assertEquals("92.1.246.69", connectable.getAddress());
            assertEquals(6346, connectable.getPort());
            assertEquals(4, rmem.getIndex());
            assertEquals("LimeWireWin4.16.0.exe", rmem.getFileName());
            assertEquals(4495072L, rmem.getSize());
            // CHECK GUID!!!!
            assertEquals(960, rmem.getSpeed());
            assertEquals(false, rmem.isChat());
            assertEquals(3, rmem.getQuality());
            assertEquals(true, rmem.isBrowseHost());
            assertEquals(null, rmem.getXml());
            assertEquals(new UrnSet(URN.createSHA1Urn("urn:sha1:A6DGMXEOJDBQOIJUQTAQWSWC2IQKFD5J")), rmem.getUrns());
            assertEquals(false, rmem.isReplyToMulticast());
            assertEquals("LIME", rmem.getVendor());
            
            address = rmem2.getAddress(addressFactory, pushEndpointFactory);
            assertInstanceof(Connectable.class, address);
            connectable = (Connectable)address;
            assertEquals("10.254.0.101", connectable.getAddress());
            assertEquals(33053, connectable.getPort());
            assertEquals(0, rmem2.getIndex());
            assertEquals("LimeWireWin4.16.0.exe", rmem2.getFileName());
            assertEquals(4495072L, rmem2.getSize());
            // CHECK GUID!!!!
            assertEquals(2147483647, rmem2.getSpeed());
            assertEquals(false, rmem2.isChat());
            assertEquals(4, rmem2.getQuality());
            assertEquals(true, rmem2.isBrowseHost());
            assertEquals(null, rmem2.getXml());
            assertEquals(new UrnSet(URN.createSHA1Urn("urn:sha1:A6DGMXEOJDBQOIJUQTAQWSWC2IQKFD5J")), rmem2.getUrns());
            assertEquals(true, rmem2.isReplyToMulticast());
            assertEquals("LIME", rmem2.getVendor());
        }
        
        {
            GnutellaDownloadMemento mem = (GnutellaDownloadMemento)read2;
            assertEquals(DownloaderType.MANAGED, mem.getDownloadType());
            assertEquals(new File("C:/Documents and Settings/sberlin/My Documents/LimeWire/Incomplete/T-4400168-LimeWireWin4.15.5.exe"), mem.getIncompleteFile());
            assertEquals(3, mem.getSavedBlocks().size());
            assertEquals(Range.createRange(0, 16895), mem.getSavedBlocks().get(0));
            assertEquals(Range.createRange(786432, 823029), mem.getSavedBlocks().get(1));
            assertEquals(Range.createRange(3801088, 3932159), mem.getSavedBlocks().get(2));
            
//            Map<String, Object> attributes = mem.getAttributes();
//            SearchInformation so = SearchInformation.createFromMap((Map)attributes.get("searchInformationMap"));
//            assertEquals("*", so.getMediaType().getSchema());
//            assertEquals("limewire", so.getQuery());
//            assertEquals(null, so.getXML());
//            assertTrue(so.isKeywordSearch());
//            assertEquals("limewire", so.getTitle());
            assertEquals(4400168L, mem.getContentLength());
            assertEquals("LimeWireWin4.15.5.exe", mem.getDefaultFileName());
            assertEquals(URN.createSHA1Urn("urn:sha1:ZKPIRLABHCFSNTMOFO7AK7FFVVIHBRQO"), mem.getSha1Urn());            

            assertEquals(2, mem.getRemoteHosts().size());
        }
        
        {
            GnutellaDownloadMemento mem = (GnutellaDownloadMemento)read3;
            assertEquals(DownloaderType.MANAGED, mem.getDownloadType());
            assertEquals(new File("C:/Documents and Settings/sberlin/My Documents/LimeWire/Incomplete/T-3381280-LimeWireWin4.14.12.exe"), mem.getIncompleteFile());
            assertEquals(6, mem.getSavedBlocks().size());
            assertEquals(Range.createRange(0, 23551), mem.getSavedBlocks().get(0));
            assertEquals(Range.createRange(524288, 634879), mem.getSavedBlocks().get(1));
            assertEquals(Range.createRange(1048576, 1050623), mem.getSavedBlocks().get(2));
            assertEquals(Range.createRange(1572864, 1667071), mem.getSavedBlocks().get(3));
            assertEquals(Range.createRange(2621440, 2661103), mem.getSavedBlocks().get(4));
            assertEquals(Range.createRange(3145728, 3381279), mem.getSavedBlocks().get(5));
            
//            Map<String, Object> attributes = mem.getAttributes();
//            SearchInformation so = SearchInformation.createFromMap((Map)attributes.get("searchInformationMap"));
//            assertEquals("*", so.getMediaType().getSchema());
//            assertEquals("limewire", so.getQuery());
//            assertEquals(null, so.getXML());
//            assertTrue(so.isKeywordSearch());
//            assertEquals("limewire", so.getTitle());
            assertEquals(3381280L, mem.getContentLength());
            assertEquals("LimeWireWin4.14.12.exe", mem.getDefaultFileName());
            assertEquals(URN.createSHA1Urn("urn:sha1:SROVXQRNE6ZA6N26OKL6BMERSAIO4HVE"), mem.getSha1Urn());            

            assertEquals(9, mem.getRemoteHosts().size());
        }
        
        {
            GnutellaDownloadMemento mem = (GnutellaDownloadMemento)read4;
            assertEquals(DownloaderType.MANAGED, mem.getDownloadType());
            assertEquals(new File("C:/Documents and Settings/sberlin/My Documents/LimeWire/Incomplete/T-2305127-LimeWirePackedJars4.12.6.7z"), mem.getIncompleteFile());
            assertEquals(4, mem.getSavedBlocks().size());
            assertEquals(Range.createRange(262144, 263543), mem.getSavedBlocks().get(0));
            assertEquals(Range.createRange(524288, 1310719), mem.getSavedBlocks().get(1));
            assertEquals(Range.createRange(1572864, 1654783), mem.getSavedBlocks().get(2));
            assertEquals(Range.createRange(1835008, 2097151), mem.getSavedBlocks().get(3));
            
//            Map<String, Object> attributes = mem.getAttributes();
//            SearchInformation so = SearchInformation.createFromMap((Map)attributes.get("searchInformationMap"));
//            assertEquals("*", so.getMediaType().getSchema());
//            assertEquals("limewire", so.getQuery());
//            assertEquals(null, so.getXML());
//            assertTrue(so.isKeywordSearch());
//            assertEquals("limewire", so.getTitle());
            assertEquals(2305127L, mem.getContentLength());
            assertEquals("LimeWirePackedJars4.12.6.7z", mem.getDefaultFileName());
            assertEquals(URN.createSHA1Urn("urn:sha1:XOOJZHTKRTKTIFHOHXYOEXVAJPYVAGDE"), mem.getSha1Urn());            

            assertEquals(3, mem.getRemoteHosts().size());
        }
        
        {
            GnutellaDownloadMemento mem = (GnutellaDownloadMemento)read5;
            assertEquals(DownloaderType.MANAGED, mem.getDownloadType());
            assertEquals(new File("C:/Documents and Settings/sberlin/My Documents/LimeWire/Incomplete/T-3380048-LimeWireWin4.14.10.exe"), mem.getIncompleteFile());
            assertEquals(5, mem.getSavedBlocks().size());
            assertEquals(Range.createRange(0, 99703), mem.getSavedBlocks().get(0));
            assertEquals(Range.createRange(262144, 292328), mem.getSavedBlocks().get(1));
            assertEquals(Range.createRange(1048576, 1049087), mem.getSavedBlocks().get(2));
            assertEquals(Range.createRange(1310720, 2621439), mem.getSavedBlocks().get(3));
            assertEquals(Range.createRange(3145728, 3238639), mem.getSavedBlocks().get(4));
            
//            Map<?, ?> attributes = mem.getAttributes();
//            SearchInformation so = SearchInformation.createFromMap((Map)attributes.get("searchInformationMap"));
//            assertEquals("*", so.getMediaType().getSchema());
//            assertEquals("limewire", so.getQuery());
//            assertEquals(null, so.getXML());
//            assertTrue(so.isKeywordSearch());
//            assertEquals("limewire", so.getTitle());
            assertEquals(3380048L, mem.getContentLength());
            assertEquals("LimeWireWin4.14.10.exe", mem.getDefaultFileName());
            assertEquals(URN.createSHA1Urn("urn:sha1:DSGYQ4XCX6VIIAHACM3JNY2UXREK7OGK"), mem.getSha1Urn());           

            assertEquals(11, mem.getRemoteHosts().size());
        }
        
        {
            GnutellaDownloadMemento mem = (GnutellaDownloadMemento)read6;
            assertEquals(DownloaderType.MANAGED, mem.getDownloadType());
            assertEquals(new File("C:/Documents and Settings/sberlin/My Documents/LimeWire/Incomplete/T-3064200-LimeWireWin4.12.6.exe"), mem.getIncompleteFile());
            assertEquals(3, mem.getSavedBlocks().size());
            assertEquals(Range.createRange(262144, 490223), mem.getSavedBlocks().get(0));
            assertEquals(Range.createRange(1572864, 1886959), mem.getSavedBlocks().get(1));
            assertEquals(Range.createRange(2097152, 2191925), mem.getSavedBlocks().get(2));
            
//            Map<String, Object> attributes = mem.getAttributes();
//            SearchInformation so = SearchInformation.createFromMap((Map)attributes.get("searchInformationMap"));
//            assertEquals("*", so.getMediaType().getSchema());
//            assertEquals("limewire", so.getQuery());
//            assertEquals(null, so.getXML());
//            assertTrue(so.isKeywordSearch());
//            assertEquals("limewire", so.getTitle());
            assertEquals(3064200L, mem.getContentLength());
            assertEquals("LimeWireWin4.12.6.exe", mem.getDefaultFileName());
            assertEquals(URN.createSHA1Urn("urn:sha1:B3KUDG6BOAMIXEIFL6YCW27LH3A4ODL6"), mem.getSha1Urn());            

            assertEquals(3, mem.getRemoteHosts().size());
        }        
    }
    
    public void testMagnet() throws Exception {
        File file = TestUtils.getResourceInPackage("magnet.dat", DownloadUpgradeTask.class);
        
        OldDownloadConverterImpl oldDownloadConverter = new OldDownloadConverterImpl(pushEndpointFactory, addressFactory);
        List<DownloadMemento> mementos = oldDownloadConverter.readAndConvertOldDownloads(file);
        assertEquals(1, mementos.size());
        
        DownloadMemento read = mementos.get(0);
        MagnetDownloadMemento mem = (MagnetDownloadMemento)read;
        assertEquals(DownloaderType.MAGNET, mem.getDownloadType());
        assertEquals(new File("C:/Documents and Settings/sberlin/My Documents/LimeWire/Incomplete/T-12229522-01-steve_winwood-dear_mr_fantasy-jun.mp3"), mem.getIncompleteFile());
        assertEquals(2, mem.getSavedBlocks().size());
        assertEquals(Range.createRange(0, 1310719), mem.getSavedBlocks().get(0));
        assertEquals(Range.createRange(4718592, 4890161), mem.getSavedBlocks().get(1));
        assertEquals(1, mem.getRemoteHosts().size());            
        assertEquals(12229522L, mem.getContentLength());
        assertEquals("01-steve_winwood-dear_mr_fantasy-jun.mp3", mem.getDefaultFileName());
        assertEquals(URN.createSHA1Urn("urn:sha1:VSHERRKVKU4FZVUDOB6UVERRC2BOEBG4"), mem.getSha1Urn());
        assertEquals(new HashMap(), mem.getAttributes());        
        MagnetOptions mo = mem.getMagnet();
        assertEquals("01-steve_winwood-dear_mr_fantasy-jun.mp3", mo.getDisplayName());
        assertEquals(URN.createSHA1Urn("urn:sha1:VSHERRKVKU4FZVUDOB6UVERRC2BOEBG4"), mo.getSHA1Urn());
        assertEquals(Collections.singletonList("http://64.61.25.138:6346/uri-res/N2R?urn:sha1:VSHERRKVKU4FZVUDOB6UVERRC2BOEBG4"), mo.getXS());
    }
    
    @SuppressWarnings("unchecked")
    public void testXml() throws Exception {
        File file = TestUtils.getResourceInPackage("xml.dat", DownloadUpgradeTask.class);
        
        OldDownloadConverterImpl oldDownloadConverter = new OldDownloadConverterImpl(pushEndpointFactory, addressFactory);
        List<DownloadMemento> mementos = oldDownloadConverter.readAndConvertOldDownloads(file);
        assertEquals(1, mementos.size());
        
        DownloadMemento read = mementos.get(0);
        GnutellaDownloadMemento mem = (GnutellaDownloadMemento)read;
        assertEquals(DownloaderType.MANAGED, mem.getDownloadType());
        assertEquals(new File("C:/Documents and Settings/sberlin/My Documents/LimeWire/Incomplete/T-12229522-01-steve_winwood-dear_mr_fantasy-jun.mp3"), mem.getIncompleteFile());
        assertEquals(1, mem.getSavedBlocks().size());
        assertEquals(Range.createRange(0, 131071), mem.getSavedBlocks().get(0));
        assertEquals(1, mem.getRemoteHosts().size());
        assertEquals(12229522L, mem.getContentLength());
        assertEquals("01-steve_winwood-dear_mr_fantasy-jun.mp3", mem.getDefaultFileName());
        assertEquals(URN.createSHA1Urn("urn:sha1:VSHERRKVKU4FZVUDOB6UVERRC2BOEBG4"), mem.getSha1Urn());
        String xmlStart = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio ";
        String xmlEnd = "/></audios>";
        String readXml = mem.getRemoteHosts().iterator().next().getXml();
        assertTrue("read: " + readXml, readXml.startsWith(xmlStart));
        assertTrue("read: " + readXml, readXml.endsWith(xmlEnd));
        NameValue<String>[] nvs = new NameValue[] {
            new NameValue("track", "1"),
            new NameValue("year", "2004"),
            new NameValue("title", "Dear Mr. Fantasy"),
            new NameValue("artist", "Steve Winwood"),
            new NameValue("license", "Access Hollywood"),
            new NameValue("album", "Dear Mr. Fantasy Album Single"),
            new NameValue("genre", "Classic Rock"),
            new NameValue("seconds", "509"),
            new NameValue("bitrate", "192"),
            new NameValue("comments", "Visit www.AccessHollywood.com/Abouttime for more free media content from Steve Winwood."),
        };
        String leftoverXml = readXml.substring(xmlStart.length(), readXml.length() - xmlEnd.length());
        for(int i = 0; i < nvs.length; i++) {
            String attr = nvs[i].getName() + "=\"" + nvs[i].getValue() + "\"";
            assertTrue("has left: " + leftoverXml, leftoverXml.contains(attr));
            int idx = leftoverXml.indexOf(attr);
            if(idx == 0)
                leftoverXml = leftoverXml.substring(attr.length());
            else if(idx == leftoverXml.length() - attr.length())
                leftoverXml = leftoverXml.substring(0, idx);
            else
                leftoverXml = leftoverXml.substring(0, idx) + leftoverXml.substring(idx + attr.length());
            leftoverXml = leftoverXml.trim();
        }
        assertEquals("has left: " + leftoverXml, 0, leftoverXml.length());
        
    }
    
    public void testGetAddressHandlesRFDWithBogusPushEndpointIp() throws Exception {
        final SerialRemoteFileDesc serialRemoteFileDesc = context.mock(SerialRemoteFileDesc.class);
        final GUID clientGuid = new GUID();
        context.checking(new Expectations() {{
            allowing(serialRemoteFileDesc).isFirewalled();
            will(returnValue(true));
            allowing(serialRemoteFileDesc).getHttpPushAddr();
            will(returnValue(null));
            allowing(serialRemoteFileDesc).getHost();
            // return bogus ip for host
            will(returnValue("1.1.1.1"));
            allowing(serialRemoteFileDesc).getClientGUID();
            will(returnValue(clientGuid.bytes()));
            
        }});
        Address address = oldDownloadConverter.getAddress(serialRemoteFileDesc);
        assertInstanceof(PushEndpoint.class, address);
        assertEquals(clientGuid.bytes(), ((PushEndpoint)address).getClientGUID());
        context.assertIsSatisfied();
    }
}
