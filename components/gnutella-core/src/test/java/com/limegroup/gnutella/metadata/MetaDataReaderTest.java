package com.limegroup.gnutella.metadata;

import java.io.File;

import org.limewire.util.TestUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public class MetaDataReaderTest extends LimeTestCase {

    private MetaDataReader metaDataReader;

    public MetaDataReaderTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector();
        metaDataReader = injector.getInstance(MetaDataReader.class);
    }
    
    /**
     * Integration test to ensure torrents are read into xml documents correctly. 
     */
    public void testReadsTorrentDocument() throws Exception {
        File torrentFile = TestUtils.getResourceInPackage("messages.torrent", getClass());
        assertTrue(torrentFile.exists());
        LimeXMLDocument document = metaDataReader.readDocument(torrentFile);
        assertEquals("http://www.limewire.com/schemas/torrent.xsd", document.getSchemaURI());
    }

}
