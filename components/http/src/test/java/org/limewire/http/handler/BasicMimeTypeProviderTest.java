package org.limewire.http.handler;

import java.io.File;

import junit.framework.TestCase;

public class BasicMimeTypeProviderTest extends TestCase {

    public void testDefaultMimeTypes() {
        BasicMimeTypeProvider provider = new BasicMimeTypeProvider();
        assertEquals("text/html", provider.getMimeType(new File(".html")));
        assertEquals("image/png", provider.getMimeType(new File(".png")));
        assertEquals("image/gif", provider.getMimeType(new File(".gif")));
        assertEquals("application/octet-stream", provider.getMimeType(new File("foo")));
        assertEquals("application/octet-stream", provider.getMimeType(new File("")));        
    }

    public void testDefaultMimeTypeConstructor() {
        BasicMimeTypeProvider provider = new BasicMimeTypeProvider("foo");
        assertEquals("text/html", provider.getMimeType(new File(".html")));
        assertEquals("foo", provider.getMimeType(new File("foo")));
        assertEquals("foo", provider.getMimeType(new File("bar")));        
    }

    public void testAddRemoveMimeTypeByExtension() {
        BasicMimeTypeProvider provider = new BasicMimeTypeProvider();
        assertEquals("text/html", provider.getMimeType(new File(".html")));
        provider.addMimeTypeByExtension("html", "foo");
        assertEquals("foo", provider.getMimeType(new File(".html")));
        provider.removeMimeTypeByExtension("html");
        assertEquals("application/octet-stream", provider.getMimeType(new File(".html")));
        provider.addMimeTypeByExtension("foo", "foo/bar");
        assertEquals("application/octet-stream", provider.getMimeType(new File("foo/bar")));
        assertEquals("application/octet-stream", provider.getMimeType(new File(".html")));
        assertEquals("foo/bar", provider.getMimeType(new File("foo")));
    }

    public void testGetMimeType() {
        BasicMimeTypeProvider provider = new BasicMimeTypeProvider();
        assertEquals("text/html", provider.getMimeType(new File("foo.bar.html")));
        assertEquals("text/html", provider.getMimeType(new File("foo.html")));
        assertEquals("text/html", provider.getMimeType(new File(".html")));
        assertEquals("text/html", provider.getMimeType(new File("html")));
    }

}
