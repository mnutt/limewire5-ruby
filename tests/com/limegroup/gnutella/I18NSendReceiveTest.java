package com.limegroup.gnutella;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.NetworkSettings;
import org.limewire.core.settings.PingPongSettings;
import org.limewire.core.settings.SearchSettings;
import org.limewire.core.settings.UltrapeerSettings;
import org.limewire.net.SocketsManager.ConnectType;
import org.limewire.util.I18NConvert;

import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.connection.BlockingConnection;
import com.limegroup.gnutella.connection.BlockingConnectionFactory;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.util.EmptyResponder;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.LimeXMLReplyCollection;
import com.limegroup.gnutella.xml.SchemaReplyCollectionMapper;

@SuppressWarnings( {"unchecked", "null"})
public class I18NSendReceiveTest extends LimeTestCase {

    private static BlockingConnection CONN_1;
    private static int TEST_PORT = 6667;

  

    //test file names that should be in the shared dir and returned as
    //replies
    private static final String FILE_0 = "hello_0.txt";
    private static final String FILE_1 = 
        "\uff8a\uff9b\uff70\u5143\u6c17\u3067\u3059\u304b\uff1f_\u30d5\u30a3\u30b7\u30e5_1.txt";
    private static final String FILE_2 = 
        "\uff34\uff25\uff33\uff34\uff34\uff28\uff29\uff33\uff3f\uff26\uff29\uff2c\uff25\uff3f\uff2e\uff21\uff2d\uff25_2.txt";
    private static final String FILE_3 = 
        "\u7206\u98a8\uff3ftestth\u00ccs_\uff27\uff2f_3.txt";
    private static final String FILE_4 = 
        "t\u00e9stthis_\u334d_\uff2d\uff21\uff2c\uff23\uff2f\uff2d\u3000\uff38\uff3f\uff8f\uff99\uff7a\uff91_4.txt";

    //these test file names are used for testing xml search
    private static final String META_FILE_0 = "meta1.mpg";
    private static final String META_FILE_1 = 
        "\u30e1\u30bf\u60c5\u5831\u30c6\u30b9\u30c8.mpg";
    private static final String META_FILE_2 = 
        "\uff2d\uff25\uff34\uff21\u60c5\u5831\u30c6\u30b9\u30c8.mpg";

    //array of file names, used in the setup stage
    private static final String[] FILES = {
        FILE_0, FILE_1, FILE_2, FILE_3, FILE_4, META_FILE_0, META_FILE_1, 
        META_FILE_2};
    private HeadersFactory headersFactory;
    private BlockingConnectionFactory connectionFactory;
    private QueryRequestFactory queryRequestFactory;
    private LifecycleManager lifecycleManager;
    private ConnectionServices connectionServices;
    private FileManager fileManager;
    private LimeXMLDocumentFactory limeXMLDocumentFactory;
    private SchemaReplyCollectionMapper schemaReplyCollectionMapper;

    public I18NSendReceiveTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(I18NSendReceiveTest.class);
    }

    
    private static void doSettings() throws Exception {
        NetworkSettings.PORT.setValue(TEST_PORT);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
		UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(false);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(true);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(false);
		ConnectionSettings.NUM_CONNECTIONS.setValue(4);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
        PingPongSettings.PINGS_ACTIVE.setValue(false);
        SearchSettings.MINIMUM_SEARCH_QUALITY.setValue(-2);
    }


    @Override
    protected void setUp() throws Exception {
        TEST_PORT++;
        doSettings();
        
        Injector injector = LimeTestUtils.createInjector(Stage.PRODUCTION);
        headersFactory = injector.getInstance(HeadersFactory.class);
        connectionFactory = injector.getInstance(BlockingConnectionFactory.class);
        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        lifecycleManager = injector.getInstance(LifecycleManager.class);
        connectionServices = injector.getInstance(ConnectionServices.class);
        fileManager = injector.getInstance(FileManager.class);
        limeXMLDocumentFactory = injector.getInstance(LimeXMLDocumentFactory.class);
        schemaReplyCollectionMapper = injector.getInstance(SchemaReplyCollectionMapper.class);
        
        lifecycleManager.start();
        connectionServices.connect();
        connect();        

        for(int i = 0; i < FILES.length; i++) {
            File f = new File(_scratchDir, FILES[i]);
            FileOutputStream fo = new FileOutputStream(f);
            fo.write('a');
            fo.flush();
            fo.close();
            assertNotNull(fileManager.getGnutellaFileList().add(f).get(1, TimeUnit.SECONDS));
        }
    }

    @Override
    public void tearDown() throws Exception {
        BlockingConnectionUtils.drain(CONN_1);
        CONN_1.close();
        connectionServices.disconnect();
    }
    
    private void connect() throws Exception {
        Properties headers = headersFactory.createUltrapeerHeaders("localhost");
        headers.put(HeaderNames.X_DEGREE,"42");
        CONN_1 = connectionFactory.createConnection("localhost", TEST_PORT, ConnectType.PLAIN);
        CONN_1.initialize(headers, new EmptyResponder(), 1000);
        BlockingConnectionUtils.drain(CONN_1);
        LimeTestUtils.establishIncoming(TEST_PORT);
    }


    /**
     * tests that we get a query reply from a file with the normalized
     * name and also that we receive the actual file name in the queryreply
     */
    public void testSendReceive() throws Exception {        
        //test random query 
        QueryRequest qr = queryRequestFactory.createQuery("asdfadf", (byte)2);
        CONN_1.send(qr);
        CONN_1.flush();
        
        Message m = BlockingConnectionUtils.getFirstQueryReply(CONN_1);
        assertNull("should not have received a QueryReply", m);
        BlockingConnectionUtils.drain(CONN_1);

        List expectedReply = new ArrayList();
        //should find FILE_0
        addExpectedReply(expectedReply, FILE_0);
        sendCheckQuery(expectedReply, "hello");

        //should find FILE_2, FILE_3, FILE_4
        addExpectedReply(expectedReply, FILE_2);
        addExpectedReply(expectedReply, FILE_3);
        addExpectedReply(expectedReply, FILE_4);
        sendCheckQuery(expectedReply, "testthis");
        
        //should find FILE_3
        addExpectedReply(expectedReply, FILE_3);
        sendCheckQuery(expectedReply, "\u7206\u98a8");

        //should find FILE_1
        addExpectedReply(expectedReply, FILE_1);
        sendCheckQuery(expectedReply, "\u5143\u6c17");
        
        //should find FILE_4
        addExpectedReply(expectedReply, FILE_4);
        sendCheckQuery(expectedReply, "malcom testthis \u30e1\u30fc\u30c8\u30eb");


    }
    
    /**
     * Adds an expected reply to the expected reply list.
     */
    private void addExpectedReply(List list, String name) throws Exception {
        File f = new File(_scratchDir, name);
        assertTrue("file: " + name + " doesn't exist", f.exists());
        f = f.getCanonicalFile(); // necessary to get the name as it is on disk
        list.add(I18NConvert.instance().compose(f.getName()));
    }

    /**
     * checks that files specified in List are all in the QueryReply created
     * by the keyword 'q'
     */
    private void sendCheckQuery(List expectedReply, String q) throws Exception {
        sendCheckQueryXML(expectedReply, q, "");
    }

    /**
     * checks that files specified in List are all in the QueryReply created
     * by the keyword 'q' and the xml string 'xml'
     */
    private void sendCheckQueryXML(List expectedReply, String q, String xml) 
        throws Exception {
        int size = expectedReply.size();

        QueryRequest qr 
            = queryRequestFactory.createQuery(q, xml);
        CONN_1.send(qr);
        CONN_1.flush();

        QueryReply rp = BlockingConnectionUtils.getFirstQueryReply(CONN_1);
        
        assertTrue("we should of received a QueryReply", rp != null);
        assertEquals("should have " + size + " result(s)", 
                     size,
                     rp.getResultCount());
        for(Iterator iter = rp.getResults(); iter.hasNext(); ) {
            Response res = (Response)iter.next();
			assertTrue("got qr: " + res.getName() + ", expected something in : " + expectedReply,
						expectedReply.remove(res.getName()));
        }

        expectedReply.clear();
    }
    
    //variables used for xml query test
    private final String director1 = "thetestdirector";
    private final String director2 = "\u30e9\u30a4\u30e0\u30ef\u30a4\u30e4\u30fc";
    private final String studio = "\u30d6\u30ed\u30fc\u30c9\u30a6\u30a7\u30a4";
    private final String studio2 = "\u30ab\u30ca\u30eb\u8857";

    /**
     * test that XML queries are sent and replies using the correct name
     */
    public void testSendReceiveXML() throws Exception {
        BlockingConnectionUtils.drain(CONN_1);
        setUpMetaData();
        I18NConvert normer = I18NConvert.instance();

        List expectedReply = new ArrayList();

        addExpectedReply(expectedReply, META_FILE_0);
        sendCheckQueryXML(expectedReply, director1,
                          buildXMLString("director=\"" +
                                         normer.getNorm(director1) + 
                                         "\""));
        
        addExpectedReply(expectedReply, META_FILE_1);
        addExpectedReply(expectedReply, META_FILE_2);
        sendCheckQueryXML(expectedReply, director2,
                          buildXMLString("director=\"" +
                                         normer.getNorm(director2) +
                                         "\""));
        
        addExpectedReply(expectedReply, META_FILE_2);
        sendCheckQueryXML(expectedReply, director2,
                          buildXMLString("director=\"" 
                                         + normer.getNorm(director2)
                                         + "\" "
                                         + "studio=\""
                                         + normer.getNorm(studio2)
                                         + "\""));
    }
    
    /**
     * adds metadata information to the test files
     */
    private void setUpMetaData() throws Exception {
        addMetaData(META_FILE_0, "director=\"" + director1 + "\"");
        addMetaData(META_FILE_1, 
                    "director=\"" + director2 
                    + "\" studio=\"" + studio + "\"");
        addMetaData(META_FILE_2, 
                    "director=\"" + director2 
                    + "\" studio=\"" + studio2 + "\"");
    }

    /**
     * add the metadata
     */
    private void addMetaData(String fname, String xmlstr) throws Exception {
        FileManager fm = fileManager;
        FileDesc fd = 
            fm.getManagedFileList().getFileDesc(new File(_scratchDir, fname));
        
        LimeXMLDocument newDoc = 
            limeXMLDocumentFactory.createLimeXMLDocument(buildXMLString(xmlstr));
        
        SchemaReplyCollectionMapper map = schemaReplyCollectionMapper;
        String uri = newDoc.getSchemaURI();
        LimeXMLReplyCollection collection = map.getReplyCollection(uri);
        
        assert collection != null : "Cant add doc to nonexistent collection";

        collection.addReply(fd, newDoc);
        assertTrue("error commiting xml", collection.writeMapToDisk());
    }
    
    // build xml string for video
    private String buildXMLString(String keyname) {
        return "<?xml version=\"1.0\"?><videos xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/video.xsd\"><video " 
            + keyname 
            + "></video></videos>";
    }

}
