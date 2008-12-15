package org.limewire.http;

import java.io.IOException;

import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.limewire.http.entity.AbstractProducingNHttpEntity;

import junit.framework.TestCase;

public class AbstractHttpNIOEntityTest extends TestCase {

    public void testGetContent() throws Exception {
        MockHttpNIOEntity entity = new MockHttpNIOEntity();
        try {
            entity.getContent();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
        try {
            entity.isRepeatable();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
        try {
            entity.isStreaming();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
        try {
            entity.writeTo(null);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }

    public void testGetContentType() {
        MockHttpNIOEntity entity = new MockHttpNIOEntity();
        entity.setContentType("type");
        assertEquals("type", entity.getContentType().getValue());
    }

    public void testIsChunked() {
        MockHttpNIOEntity entity = new MockHttpNIOEntity();
        assertFalse(entity.isChunked());
    }

    private class MockHttpNIOEntity extends AbstractProducingNHttpEntity {

        public void finish() {
        }

        @Override
        public long getContentLength() {
            return 0;
        }

        @Override
        public boolean writeContent(ContentEncoder contentEncoder, IOControl ioctrl) throws IOException {
            return false;
        }

        @Override
        public void initialize(ContentEncoder contentEncoder, IOControl ioctrl) throws IOException {
        }

        @Override
        public void timeout() {
        }
        
    }
    
}
