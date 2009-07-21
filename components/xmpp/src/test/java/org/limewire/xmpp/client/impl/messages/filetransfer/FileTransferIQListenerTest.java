package org.limewire.xmpp.client.impl.messages.filetransfer;

import java.util.EnumMap;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.friend.api.FileMetaData;
import org.limewire.friend.api.feature.FeatureTransport;
import org.limewire.friend.api.feature.FeatureTransport.Handler;
import org.limewire.friend.impl.FileMetaDataImpl;
import org.limewire.friend.impl.FileMetaDataImpl.Element;
import org.limewire.util.BaseTestCase;
import org.limewire.xmpp.client.impl.messages.filetransfer.FileTransferIQ.TransferType;

public class FileTransferIQListenerTest extends BaseTestCase {

    private Mockery context;
    private FileTransferIQListener fileTransferIQListener;
    private Handler<FileMetaData> fileMetaDataHandler;

    public FileTransferIQListenerTest(String name) {
        super(name);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        fileMetaDataHandler = context.mock(FeatureTransport.Handler.class);
        fileTransferIQListener = new FileTransferIQListener(null, fileMetaDataHandler);
    }

    public void testProcessPacketFiresEvent() {
        Map<Element, String> data = new EnumMap<Element, String>(Element.class);
        data.put(Element.index, "2");
        data.put(Element.urns, "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB");
        data.put(Element.name, "filename");
        data.put(Element.size, "4545");
        data.put(Element.createTime, "4945");
        final FileMetaDataImpl fileMetaData = new FileMetaDataImpl(data);
        FileTransferIQ fileTransferIQ = new FileTransferIQ(fileMetaData, TransferType.OFFER);
        fileTransferIQ.setFrom("me@you.com");
        
        context.checking(new Expectations() {{
            one(fileMetaDataHandler).featureReceived("me@you.com", fileMetaData);
        }});
        
        fileTransferIQListener.processPacket(fileTransferIQ);
        
        context.assertIsSatisfied();
    }
}
