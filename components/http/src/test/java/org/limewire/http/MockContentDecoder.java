package org.limewire.http;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.http.nio.ContentDecoder;
import org.limewire.util.BufferUtils;

public class MockContentDecoder implements ContentDecoder {

    public ByteBuffer source;

    public boolean completed;

    public MockContentDecoder(String source) {
        this.source = ByteBuffer.wrap(source.getBytes());
    }

    public boolean isCompleted() {
        return completed;
    }

    public int read(ByteBuffer dst) throws IOException {
        return BufferUtils.transfer(source, dst);
    }

}
