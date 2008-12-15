package org.limewire.http.handler;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolException;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.limewire.http.entity.FileNIOEntity;

public class FileRequestHandlerTest extends TestCase {

    private File root;

    private File subdir;

    private File subdirFile;

    private File indexFile;

    @Override
    protected void setUp() throws Exception {
        root = File.createTempFile("lime", null);
        root.delete();
        assertTrue(root.mkdirs());
        root.deleteOnExit();

        indexFile = new File(root, "index.html");
        indexFile.createNewFile();
        indexFile.deleteOnExit();

        subdir = new File(root, "subdir");
        subdir.mkdir();
        subdir.deleteOnExit();

        subdirFile = new File(subdir, "file");
        subdirFile.createNewFile();
        subdirFile.deleteOnExit();
    }

    public void testGetIndexFilename() throws IOException {
        FileRequestHandler handler = new FileRequestHandler(root,
                new BasicMimeTypeProvider());
        assertEquals("index.html", handler.getIndexFilename());
        handler.setIndexFilename("foo");
        assertEquals("foo", handler.getIndexFilename());
    }

    public void testHandle() throws Exception {
        FileRequestHandler handler = new FileRequestHandler(root,
                new BasicMimeTypeProvider());

        HttpRequest request = new BasicHttpRequest("GET", "/");
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1,
                HttpStatus.SC_OK, "");
        handler.handle(request, response, null);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(indexFile, ((FileNIOEntity) response.getEntity())
                .getFile());

        request = new BasicHttpRequest("GET", "/index.html");
        response = new BasicHttpResponse(HttpVersion.HTTP_1_1,
                HttpStatus.SC_OK, "");
        handler.handle(request, response, null);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(indexFile, ((FileNIOEntity) response.getEntity())
                .getFile());
        assertEquals("text/html", response.getEntity().getContentType().getValue());

        request = new BasicHttpRequest("GET", "/foo");
        response = new BasicHttpResponse(HttpVersion.HTTP_1_1,
                HttpStatus.SC_OK, "");
        handler.handle(request, response, null);
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());

    }

    public void testHandleSubDir() throws Exception {
        FileRequestHandler handler = new FileRequestHandler(root,
                new BasicMimeTypeProvider());

        HttpRequest request = new BasicHttpRequest("GET", "/subdir");
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1,
                HttpStatus.SC_OK, "");
        handler.handle(request, response, null);
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
        
        request = new BasicHttpRequest("GET", "/subdir/");
        response = new BasicHttpResponse(HttpVersion.HTTP_1_1,
                HttpStatus.SC_OK, "");
        handler.handle(request, response, null);
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());

        request = new BasicHttpRequest("GET", "/subdir/foo");
        response = new BasicHttpResponse(HttpVersion.HTTP_1_1,
                HttpStatus.SC_OK, "");
        handler.handle(request, response, null);
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());

        request = new BasicHttpRequest("GET", "/subdir/file");
        response = new BasicHttpResponse(HttpVersion.HTTP_1_1,
                HttpStatus.SC_OK, "");
        handler.handle(request, response, null);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(subdirFile, ((FileNIOEntity) response.getEntity())
                .getFile());
    }

    public void testHandleInvalidRequest() throws Exception {
        FileRequestHandler handler = new FileRequestHandler(root,
                new BasicMimeTypeProvider());

        HttpRequest request = new BasicHttpRequest("GET", "");
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1,
                HttpStatus.SC_OK, "");
        try {
            handler.handle(request, response, null);
            fail("Expected ProtocolException");
        } catch (ProtocolException expected) {
        }
        
        request = new BasicHttpRequest("GET", "index.html");
        response = new BasicHttpResponse(HttpVersion.HTTP_1_1,
                HttpStatus.SC_OK, "");
        try {
            handler.handle(request, response, null);
            fail("Expected ProtocolException");
        } catch (ProtocolException expected) {
        }

        request = new BasicHttpRequest("GET", "../index.html");
        response = new BasicHttpResponse(HttpVersion.HTTP_1_1,
                HttpStatus.SC_OK, "");
        try {
            handler.handle(request, response, null);
            fail("Expected ProtocolException");
        } catch (ProtocolException expected) {
        }

        request = new BasicHttpRequest("GET", "..");
        response = new BasicHttpResponse(HttpVersion.HTTP_1_1,
                HttpStatus.SC_OK, "");
        try {
            handler.handle(request, response, null);
            fail("Expected ProtocolException");
        } catch (ProtocolException expected) {
        }

        request = new BasicHttpRequest("GET", ".");
        response = new BasicHttpResponse(HttpVersion.HTTP_1_1,
                HttpStatus.SC_OK, "");
        try {
            handler.handle(request, response, null);
            fail("Expected ProtocolException");
        } catch (ProtocolException expected) {
        }
        
        request = new BasicHttpRequest("GET", "/../index.html");
        response = new BasicHttpResponse(HttpVersion.HTTP_1_1,
                HttpStatus.SC_OK, "");
        try {
            handler.handle(request, response, null);
            fail("Expected ProtocolException");
        } catch (ProtocolException expected) {
        }
        
        request = new BasicHttpRequest("GET", "  ../index.html");
        response = new BasicHttpResponse(HttpVersion.HTTP_1_1,
                HttpStatus.SC_OK, "");
        try {
            handler.handle(request, response, null);
            fail("Expected ProtocolException");
        } catch (ProtocolException expected) {
        }
        
        request = new BasicHttpRequest("GET", "  ../index.html");
        response = new BasicHttpResponse(HttpVersion.HTTP_1_1,
                HttpStatus.SC_OK, "");
        try {
            handler.handle(request, response, null);
            fail("Expected ProtocolException");
        } catch (ProtocolException expected) {
        }

        request = new BasicHttpRequest("GET", "/subdir/../index.html");
        response = new BasicHttpResponse(HttpVersion.HTTP_1_1,
                HttpStatus.SC_OK, "");
        try {
            handler.handle(request, response, null);
            fail("Expected ProtocolException");
        } catch (ProtocolException expected) {
        }
    }

}
