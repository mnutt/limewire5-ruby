package com.limegroup.gnutella.uploader;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.security.SecurityToken;
import org.limewire.util.BaseTestCase;

import com.google.inject.Singleton;
import com.limegroup.gnutella.MessageListener;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.guess.GUESSEndpoint;
import com.limegroup.gnutella.messagehandlers.MessageHandler;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.InspectionRequest;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutella.routing.QueryRouteTable;

public class HttpPushRequestHandlerTest extends BaseTestCase {

    private HttpPushRequestHandler requestHandler;

    private HttpContext context;

    private HTTPUploader uploader;

    private StubMessageRouter messageRouter;

    public HttpPushRequestHandlerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HttpPushRequestHandlerTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        messageRouter = new StubMessageRouter();
        MockHTTPUploadSessionManager sessionManager = new MockHTTPUploadSessionManager();
        uploader = new HTTPUploader("filename", new HTTPUploadSession(null,
                null, null)) {
            @Override
            public void setState(UploadStatus state) {
                // avoid assertion errors
            }
        };
        sessionManager.uploader = uploader;
        requestHandler = new HttpPushRequestHandler(sessionManager, messageRouter);
        context = new BasicHttpContext(null);
    }

    public void testHandleMalformedRequest() throws Exception {
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1,
                HttpStatus.SC_OK, "");

        // no parameters
        HttpRequest request = new BasicHttpRequest("GET", "/push/proxy");
        requestHandler.handle(request, response, context);
        assertNull(messageRouter.push);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine()
                .getStatusCode());

        // no node header
        request = new BasicHttpRequest("GET",
                "/push/proxy/?ServerID=6MTB46XQ3PTC3QVZRDW6PAFQAA");
        requestHandler.handle(request, response, context);
        assertNull(messageRouter.push);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine()
                .getStatusCode());

    }

    public void testHandleValidRequest() throws Exception {
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1,
                HttpStatus.SC_OK, "");

        BasicHttpRequest request = new BasicHttpRequest("GET",
                "/push/proxy/?ServerID=6MTB46XQ3PTC3QVZRDW6PAFQAA");
        request.addHeader("X-Node", "localhost:4567");
        requestHandler.handle(request, response, context);
        assertNotNull(messageRouter.push);
        assertEquals(HttpStatus.SC_ACCEPTED, response.getStatusLine()
                .getStatusCode());
    }

    @Singleton
    private class StubMessageRouter implements MessageRouter {
        private PushRequest push;

        protected List<QueryReply> createQueryReply(byte[] guid, byte ttl,
                long speed, Response[] res, byte[] clientGUID, boolean busy,
                boolean uploaded, boolean measuredSpeed, boolean isFromMcast,
                boolean shouldMarkForFWTransfer, SecurityToken securityToken) {
            return null;
        }

        protected void respondToPingRequest(PingRequest request,
                ReplyHandler handler) {
        }

        protected boolean respondToQueryRequest(QueryRequest queryRequest,
                byte[] clientGUID, ReplyHandler handler) {
            return false;
        }

        protected void respondToUDPPingRequest(PingRequest request,
                InetSocketAddress addr, ReplyHandler handler) {
        }

        public void sendPushRequest(PushRequest push) throws IOException {
            this.push = push;
        }

        public boolean addBypassedSource(QueryReply reply, ReplyHandler handler) {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean addBypassedSource(ReplyNumberVendorMessage reply, ReplyHandler handler) {
            // TODO Auto-generated method stub
            return false;
        }

        public void addMessageHandler(Class<? extends Message> clazz, MessageHandler handler) {
            // TODO Auto-generated method stub
            
        }

        public void addMulticastMessageHandler(Class<? extends Message> clazz,
                MessageHandler handler) {
            // TODO Auto-generated method stub
            
        }

        public void addUDPMessageHandler(Class<? extends Message> clazz, MessageHandler handler) {
            // TODO Auto-generated method stub
            
        }

        public void broadcastPingRequest(PingRequest ping) {
            // TODO Auto-generated method stub
            
        }

        public void downloadFinished(GUID guid) throws IllegalArgumentException {
            // TODO Auto-generated method stub
            
        }

        public void forwardInspectionRequestToLeaves(InspectionRequest ir) {
            // TODO Auto-generated method stub
            
        }

        public void forwardQueryRequestToLeaves(QueryRequest query, ReplyHandler handler) {
            // TODO Auto-generated method stub
            
        }

        public MessageHandler getMessageHandler(Class<? extends Message> clazz) {
            // TODO Auto-generated method stub
            return null;
        }

        public MessageHandler getMulticastMessageHandler(Class<? extends Message> clazz) {
            // TODO Auto-generated method stub
            return null;
        }

        public int getNumOOBToRequest(ReplyNumberVendorMessage reply) {
            // TODO Auto-generated method stub
            return 0;
        }

        public long getOOBExpireTime() {
            // TODO Auto-generated method stub
            return 0;
        }

        public String getPingRouteTableDump() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getPushRouteTableDump() {
            // TODO Auto-generated method stub
            return null;
        }

        public Set<GUESSEndpoint> getQueryLocs(GUID guid) {
            // TODO Auto-generated method stub
            return null;
        }

        public QueryRouteTable getQueryRouteTable() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getQueryRouteTableDump() {
            // TODO Auto-generated method stub
            return null;
        }

        public MessageHandler getUDPMessageHandler(Class<? extends Message> clazz) {
            // TODO Auto-generated method stub
            return null;
        }

        public void handleMessage(Message msg, ReplyHandler receivingConnection) {
            // TODO Auto-generated method stub
            
        }

        public void handleMulticastMessage(Message msg, InetSocketAddress addr) {
            // TODO Auto-generated method stub
            
        }

        public void handleQueryReply(QueryReply queryReply, ReplyHandler handler) {
            // TODO Auto-generated method stub
            
        }

        public void handleUDPMessage(Message msg, InetSocketAddress addr) {
            // TODO Auto-generated method stub
            
        }

        public void start() {
            // TODO Auto-generated method stub
            
        }

        public boolean isHostUnicastQueried(GUID guid, IpPort host) {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean isQueryAlive(GUID guid) {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean sendInitialQuery(QueryRequest query, RoutedConnection mc) {
            // TODO Auto-generated method stub
            return false;
        }

        public void originateQueryGUID(byte[] guid) {
            // TODO Auto-generated method stub
            
        }

        public void queryKilled(GUID guid) throws IllegalArgumentException {
            // TODO Auto-generated method stub
            
        }

        public void registerMessageListener(byte[] guid, MessageListener ml) {
            // TODO Auto-generated method stub
            
        }

        public void removeConnection(ReplyHandler rh) {
            // TODO Auto-generated method stub
            
        }

        public Iterable<QueryReply> responsesToQueryReplies(Response[] responses,
                QueryRequest queryRequest) {
            // TODO Auto-generated method stub
            return null;
        }

        public void sendDynamicQuery(QueryRequest query) {
            // TODO Auto-generated method stub
            
        }

        public void sendMulticastPushRequest(PushRequest push) {
            // TODO Auto-generated method stub
            
        }

        public void sendPingRequest(PingRequest request, RoutedConnection connection) {
            // TODO Auto-generated method stub
            
        }

        public void setMessageHandler(Class<? extends Message> clazz, MessageHandler handler) {
            // TODO Auto-generated method stub
            
        }

        public void setMulticastMessageHandler(Class<? extends Message> clazz,
                MessageHandler handler) {
            // TODO Auto-generated method stub
            
        }

        public void setUDPMessageHandler(Class<? extends Message> clazz, MessageHandler handler) {
            // TODO Auto-generated method stub
            
        }

        public void unregisterMessageListener(byte[] guid, MessageListener ml) {
            // TODO Auto-generated method stub
            
        }

        public ReplyHandler getPushHandler(byte[] guid) {
            return null;
        }

        public void stop() {
        }

        public Iterable<QueryReply> responsesToQueryReplies(Response[] responses,
                QueryRequest queryRequest, int replyLimit, SecurityToken token) {
            return null;
        }
        
        public void initialize() {
            // TODO Auto-generated method stub
            
        }
        
        public String getServiceName() {
            // TODO Auto-generated method stub
            return null;
        }

    }
}
