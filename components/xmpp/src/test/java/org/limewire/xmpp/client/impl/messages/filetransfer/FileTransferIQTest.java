package org.limewire.xmpp.client.impl.messages.filetransfer;

import java.util.Collections;
import java.util.Date;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.friend.api.FileMetaData;
import org.limewire.friend.impl.FileMetaDataImpl;
import org.limewire.util.BaseTestCase;
import org.limewire.xmpp.client.impl.messages.IQTestUtils;
import org.limewire.xmpp.client.impl.messages.InvalidIQException;
import org.limewire.xmpp.client.impl.messages.filetransfer.FileTransferIQ.TransferType;

public class FileTransferIQTest extends BaseTestCase {

    private Mockery context;

    public FileTransferIQTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
    }

    public void testParsesOwnOutput() throws Exception {
        final FileMetaData metaData = context.mock(FileMetaData.class);
        final Date date = new Date();
        context.checking(new Expectations() {{
            one(metaData).getCreateTime();
            will(returnValue(date));
            one(metaData).getDescription();
            will(returnValue("help & description>>>now]][["));
            one(metaData).getId();
            will(returnValue("id"));
            one(metaData).getIndex();
            will(returnValue(Long.MIN_VALUE));
            one(metaData).getName();
            will(returnValue("hold <bold>this</bold>.html"));
            one(metaData).getSize();
            will(returnValue(Long.MAX_VALUE));
            one(metaData).getUrns();
            will(returnValue(Collections.singleton("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB")));
        }});
        // need to copy to test FileMetaData#toXML()
        FileMetaData copy = new FileMetaDataImpl(metaData);
        context.assertIsSatisfied();
        FileTransferIQ iq = new FileTransferIQ(copy, TransferType.OFFER);
        
        FileTransferIQ parsedIQ = new FileTransferIQ(IQTestUtils.createParser(iq.getChildElementXML()));
        FileMetaData parsedMetaData = parsedIQ.getFileMetaData();
        
        assertEquals(date, parsedMetaData.getCreateTime());
        assertEquals("help & description>>>now]][[", parsedMetaData.getDescription());
        assertEquals("id", parsedMetaData.getId());
        assertEquals(Long.MIN_VALUE, parsedMetaData.getIndex());
        assertEquals("hold <bold>this</bold>.html", parsedMetaData.getName());
        assertEquals(Long.MAX_VALUE, parsedMetaData.getSize());
        assertEquals(Collections.singleton("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB"), parsedMetaData.getUrns());
    }
    
    public void testParsesUnknownTransferTypeGracefully() throws Exception {
        try {
            new FileTransferIQ(IQTestUtils.createParser("<file-transfer xmlns='jabber:iq:lw-file-transfer' type='public-transfer'>"
                    + "<file><name>hello</name><size>2</size><index>0</index><createTime>50005</createTime><urns>urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB</urns></file>"
                    + "</file-transfer>"));
            fail("expected invalid iq exception");
        } catch (InvalidIQException iie) {
        }
    }
    
    public void testParsesMissingTransferTypeGracefully() throws Exception {
        try {
            new FileTransferIQ(IQTestUtils.createParser("<file-transfer xmlns='jabber:iq:lw-file-transfer'>"
                    + "<file><name>hello</name><size>2</size><index>0</index><createTime>50005</createTime><urns>urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB</urns></file>"
                    + "</file-transfer>"));
            fail("expected invalid iq exception");
        } catch (InvalidIQException iie) {
        }
    }
}
