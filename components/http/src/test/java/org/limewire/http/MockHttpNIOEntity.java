/**
 * 
 */
package org.limewire.http;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.limewire.http.entity.AbstractProducingNHttpEntity;
import org.limewire.util.StringUtils;

public class MockHttpNIOEntity extends AbstractProducingNHttpEntity {

    IOException exception;
    
    boolean finished;
    
    long contentLength;
    
    int transferred;

    String data;

    boolean initialized;

    boolean timeout;
    
    public MockHttpNIOEntity(String data) {
        this.data = data;
        this.contentLength = data.length();
    }
    
    public void finish() {
        this.finished = true;
    }

    @Override
    public long getContentLength() {
        return contentLength;
    }

    @Override
    public boolean writeContent(ContentEncoder contentEncoder, IOControl ioctrl) throws IOException {
        if (exception != null) {
            throw new IOException();
        }
        contentEncoder.write(ByteBuffer.wrap(StringUtils.toUTF8Bytes(data), transferred, 1));
        transferred++;
        return transferred < contentLength;
    }

    @Override
    public void initialize(ContentEncoder contentEncoder, IOControl ioctrl) throws IOException {
        initialized = true;
    }

    @Override
    public void timeout() {
        timeout = true;
    }
    
}