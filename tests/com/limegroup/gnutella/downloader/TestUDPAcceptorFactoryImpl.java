package com.limegroup.gnutella.downloader;

import org.limewire.io.GUID;
import org.limewire.net.SocketsManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.vendor.HeadPongFactory;

// no interface yet, extract it if needed
@Singleton
public class TestUDPAcceptorFactoryImpl {

    private final SocketsManager socketsManager;
    private final MessageFactory messageFactory;
    private final HeadPongFactory headPongFactory;

    @Inject
    public TestUDPAcceptorFactoryImpl(SocketsManager socketsManager,
            MessageFactory messageFactory, HeadPongFactory headPongFactory) {
        this.socketsManager = socketsManager;
        this.messageFactory = messageFactory;
        this.headPongFactory = headPongFactory;
    }
    
    public TestUDPAcceptor createTestUDPAcceptor(int port, String testMethod) {
        return new TestUDPAcceptor(socketsManager, messageFactory, headPongFactory, port,
                testMethod);
    }

    public TestUDPAcceptor createTestUDPAcceptor(int portL, int portC,
            String filename, TestUploader uploader, GUID g, String testMethod) {
        return new TestUDPAcceptor(socketsManager, messageFactory, headPongFactory, portL, portC,
                filename, uploader, g, testMethod);
    }

}
