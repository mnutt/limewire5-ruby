package org.limewire.http;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.http.nio.ContentDecoder;
import org.limewire.util.BufferUtils;
import org.limewire.util.StringUtils;

public class MockContentDecoder implements ContentDecoder {

    public ByteBuffer source;

    public boolean completed;

    public MockContentDecoder(String source) {
        this.source = ByteBuffer.wrap(StringUtils.toUTF8Bytes(source));
    }

    public boolean isCompleted() {
        return completed;
    }

    public int read(ByteBuffer dst) throws IOException {
        return BufferUtils.transfer(source, dst);
    }

}
