package org.limewire.xmpp.client.impl.messages.filetransfer;

import java.util.EnumMap;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.limewire.core.api.friend.client.FileOffer;
import org.limewire.core.api.friend.client.FileOfferEvent;
import org.limewire.listener.EventBroadcaster;
import org.limewire.util.BaseTestCase;
import org.limewire.xmpp.client.impl.messages.FileMetaDataImpl;
import org.limewire.xmpp.client.impl.messages.FileMetaDataImpl.Element;
import org.limewire.xmpp.client.impl.messages.filetransfer.FileTransferIQ.TransferType;

public class FileTransferIQListenerTest extends BaseTestCase {

    private Mockery context;
    private EventBroadcaster eventBroadcaster;
    private FileTransferIQListener fileTransferIQListener;

    public FileTransferIQListenerTest(String name) {
        super(name);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        eventBroadcaster = context.mock(EventBroadcaster.class);
        fileTransferIQListener = new FileTransferIQListener(eventBroadcaster);
    }

    @SuppressWarnings("unchecked")
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
            one(eventBroadcaster).broadcast(with(any(FileOfferEvent.class)));
            will(new CustomAction("assert") {
                @Override
                public Object invoke(Invocation invocation) throws Throwable {
                    FileOfferEvent event = (FileOfferEvent)invocation.getParameter(0);
                    assertEquals(FileOfferEvent.Type.OFFER, event.getType());
                    FileOffer fileOffer = event.getData();
                    assertEquals("me@you.com", fileOffer.getFromJID());
                    assertSame(fileMetaData, fileOffer.getFile());
                    return null;
                }
            });
        }});
        
        fileTransferIQListener.processPacket(fileTransferIQ);
        
        context.assertIsSatisfied();
    }
}
