package org.limewire.xmpp.client.impl.messages.filetransfer;

import org.limewire.util.BaseTestCase;
import org.limewire.xmpp.client.impl.messages.IQTestUtils;
import org.limewire.friend.impl.FileMetaDataImpl;
import org.limewire.xmpp.client.impl.messages.InvalidIQException;
import org.xmlpull.v1.XmlPullParser;

public class XMPPFileMetaDataTest extends BaseTestCase {

    public XMPPFileMetaDataTest(String name) {
        super(name);
    }

    public void testInvalidElementReadFromXML() throws Exception {
        XmlPullParser parser = IQTestUtils.createParser("<file><name>hello</name><invalidtag>harhar</invalidtag><size>2</size><index>0</index><createTime>50005</createTime><urns>urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB</urns></file>");
        parser.next();
        FileMetaDataImpl metaDataImpl = new XMPPFileMetaData(parser);
        assertEquals("hello", metaDataImpl.getName());
        assertEquals(2, metaDataImpl.getSize());
        assertEquals(0, metaDataImpl.getIndex());
    }
    
    public void testRandomUnescapedText() throws Exception {
        XmlPullParser parser = IQTestUtils.createParser("<file><name>&quot;&amp;\\//[[hello\"'kdf;;.?---</name><size>2</size><index>0</index><createTime>50005</createTime><urns>urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB</urns></file>");
        parser.next();
        FileMetaDataImpl metaData = new XMPPFileMetaData(parser);
        assertEquals("\"&\\//[[hello\"'kdf;;.?---", metaData.getName());
    }
    
    public void testHandlesMissingNonMandatoryParsedElementsGracefully() throws Exception {
        XmlPullParser parser = IQTestUtils.createParser("<file><urns>urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB</urns><name>hello</name><size>2</size><index>0</index><createTime>50005</createTime></file>");
        parser.next();
        FileMetaDataImpl metaData = new XMPPFileMetaData(parser);
        metaData.getCreateTime();
        metaData.getName();
        assertNull(metaData.getDescription());
        metaData.getSize();
        assertNull(metaData.getId());
        metaData.getIndex();
        metaData.getUrns();
    }
    
    public void assertFieldIsMandatory(String incompleteInput, String missingField) throws Exception {
        try {
            XmlPullParser parser = IQTestUtils.createParser("<file>" + incompleteInput + "</file>");
            parser.next();
            new XMPPFileMetaData(parser);
            fail("invalid iq exception expected for missing field: " + missingField);
        } catch (InvalidIQException iie) {
        }
        XmlPullParser parser = IQTestUtils.createParser("<file>" + incompleteInput + missingField + "</file>");
        parser.next();
        new XMPPFileMetaData(parser);
    }
    
    public void testUrnsAreMandatory() throws Exception {
        assertFieldIsMandatory("<name>hello</name><size>2</size><index>0</index><createTime>50005</createTime>",
                "<urns>urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB</urns>");
    }
    
    public void testNameIsMandatory() throws Exception {
        assertFieldIsMandatory("<urns>urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB</urns><size>2</size><index>0</index><createTime>50005</createTime>",
                "<name>name</name>");
    }
    
    public void testSizeIsMandatory() throws Exception {
        assertFieldIsMandatory("<name>name</name><urns>urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB</urns><index>0</index><createTime>50005</createTime>",
        "<size>2</size>");
    }
    
    public void testIndexIsMandatory() throws Exception {
        assertFieldIsMandatory("<size>2</size><name>name</name><urns>urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB</urns><createTime>50005</createTime>",
        "<index>0</index>");
    }
    
    public void testCreateTimeIsMandatory() throws Exception {
        assertFieldIsMandatory("<index>0</index><size>2</size><name>name</name><urns>urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB</urns>",
        "<createTime>50005</createTime>");
    }
}
