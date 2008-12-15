package com.limegroup.gnutella.http;

import junit.framework.TestCase;

import org.apache.http.message.BasicHeader;

import com.limegroup.gnutella.uploader.HTTPUploadSession;
import com.limegroup.gnutella.uploader.HTTPUploader;

public class UserAgentHeaderInterceptorTest extends TestCase {

    public void testProcess() throws Exception {
        HTTPUploadSession session = new HTTPUploadSession(null, null, null);
        HTTPUploader uploader = new HTTPUploader("filename", session); 

        UserAgentHeaderInterceptor interceptor = new UserAgentHeaderInterceptor(uploader);
        assertNull(uploader.getUserAgent());
                
        interceptor.process(new BasicHeader("User-Agent", "WebDownloader"), null);
        assertEquals("WebDownloader", uploader.getUserAgent());

        interceptor.process(new BasicHeader("User-Agent", ""), null);
        assertEquals("", uploader.getUserAgent());

        interceptor.process(new BasicHeader("User-Agent", "Foo"), null);
        assertEquals("Foo", uploader.getUserAgent());
        
        interceptor.process(new BasicHeader("UserAgent", "Bar"), null);
        assertEquals("Foo", uploader.getUserAgent());

        interceptor.process(new BasicHeader("foo", "Bar"), null);
        assertEquals("Foo", uploader.getUserAgent());
    }

    public void testIsFreeloader() {
        assertTrue(UserAgentHeaderInterceptor.isFreeloader("WebDownloader"));
        assertTrue(UserAgentHeaderInterceptor.isFreeloader("  WebDownloader/1.1"));
        assertTrue(UserAgentHeaderInterceptor.isFreeloader(" abcGo!Zilla///"));
        assertFalse(UserAgentHeaderInterceptor.isFreeloader("Konqueror"));
        
    }

}
