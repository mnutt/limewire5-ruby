package com.limegroup.gnutella.metadata;

import java.io.File;
import java.util.List;

import junit.framework.Test;

import org.limewire.util.NameValue;
import org.limewire.util.TestUtils;

import com.google.inject.Injector;
import com.limegroup.bittorrent.metadata.TorrentMetaData;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.util.LimeTestCase;

public class MetaDataFactoryImplTest extends LimeTestCase {

    private MetaDataFactoryImpl metaDataFactory;

    public MetaDataFactoryImplTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(MetaDataFactoryImplTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector();
        metaDataFactory = (MetaDataFactoryImpl) injector.getInstance(MetaDataFactory.class);
    }
    
    /**
     * Integration test to ensure {@link TorrentMetaReaderFactory} is registered
     * with {@link MetaDataFactoryImpl} and multi file torrents are parsed correctly.
     */
    @SuppressWarnings("null")
    public void testReadsMultiFileTorrentMetaData() throws Exception {
        File torrentFile = TestUtils.getResourceInPackage("messages.torrent", getClass());
        assertTrue(torrentFile.exists());
        
        MetaData metaData = metaDataFactory.parse(torrentFile);
        assertTrue(metaData instanceof TorrentMetaData);
        List<NameValue<String>> values = metaData.toNameValueList();
        String filepaths = null;
        for (NameValue<String> value : values) {
            if (value.getName().equals(TorrentMetaData.FILE_PATHS)) {
                filepaths = value.getValue();
                break;
            }
        }
        assertNotNull(filepaths);
        String[] fileUris = filepaths.split("\t");
        assertEquals(9, fileUris.length);
    }
    
    /**
     * Integration test to ensure {@link TorrentMetaReaderFactory} is registered
     * with {@link MetaDataFactoryImpl} and single file torrents are parsed correctly.
     */
    public void testReadsSingleFileTorrentMetaData() throws Exception {
        File torrentFile = TestUtils.getResourceInPackage("bthavetest.torrent", getClass());
        assertTrue(torrentFile.exists());
        
        MetaData metaData = metaDataFactory.parse(torrentFile);
        assertTrue(metaData instanceof TorrentMetaData);
        List<NameValue<String>> values = metaData.toNameValueList();
        String name = null;
        for (NameValue<String> value : values) {
            if (value.getName().equals(TorrentMetaData.NAME)) {
                name = value.getValue();
                break;
            }
        }
        assertEquals("BTHaveTest.class", name);
    }

}
