package com.limegroup.gnutella.uploader;

import java.util.Collections;

import junit.framework.Test;

import org.apache.http.protocol.BasicHttpContext;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileView;
import com.limegroup.gnutella.uploader.FileRequestParser.FileRequest;
import com.limegroup.gnutella.uploader.authentication.HttpRequestFileViewProvider;

public class FileRequestParserTest extends BaseTestCase {

    private URN urn;

    private Mockery context;

    public FileRequestParserTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        urn = URN.createSHA1Urn("urn:sha1:PLSTHIFQGSJZT45FJUPAKUZWUGYQYPFB");
        context = new Mockery();
    }

    public static Test suite() {
        return buildTestSuite(FileRequestParserTest.class);
    }

    public void testParseValidFriendRequest() throws Exception {
        final HttpRequestFileViewProvider fileListProvider = context
                .mock(HttpRequestFileViewProvider.class);
        final BasicHttpContext httpContext = new BasicHttpContext();

        context.checking(new Expectations() {
            {
                one(fileListProvider).getFileViews("me@me.com", httpContext);
                will(returnValue(Collections.emptyList()));
            }
        });

        assertNull(FileRequestParser.parseRequest(fileListProvider, "/friend/download/me%40me.com/"
                + HTTPConstants.URI_RES_N2R + urn.httpStringValue(), httpContext));

        context.assertIsSatisfied();
    }

    public void testParseValidFriendIds() throws Exception {
        String uri = "/friend/download/me%40me.com/";
        assertEquals("input: " + uri, "me@me.com", FileRequestParser.parseFriendId(uri));
    }

    public void testParseValidThexFriendRequest() throws Exception {
        final HttpRequestFileViewProvider fileListProvider = context.mock(HttpRequestFileViewProvider.class);
        final BasicHttpContext httpContext = new BasicHttpContext();
        
        final FileView fileView = context.mock(FileView.class);
        final FileDesc fileDesc = context.mock(FileDesc.class);
        context.checking(new Expectations() {
            {
                one(fileListProvider).getFileViews("me@me.com", httpContext);
                will(returnValue(Collections.singleton(fileView)));
                one(fileView).getFileDesc(urn);
                will(returnValue(fileDesc));
            }
        });
        
        FileRequest fileRequest = FileRequestParser.parseRequest(fileListProvider, "/friend/download/me%40me.com/"
                + HTTPConstants.URI_RES_N2X + urn.httpStringValue(), httpContext);
        assertNotNull(fileRequest);
        assertSame(fileDesc, fileRequest.getFileDesc());
        assertTrue(fileRequest.isThexRequest());
        
        context.assertIsSatisfied();
    }
}
