/**
 * 
 */
package org.limewire.http;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.http.nio.ContentEncoder;
import org.limewire.util.BufferUtils;

public class MockContentEncoder implements ContentEncoder {

    StringBuilder data = new StringBuilder();

    boolean completed;

    IOException exception;

    public void complete() throws IOException {
        completed = true;
    }

    public boolean isCompleted() {
        return completed;
    }

    public int write(ByteBuffer src) throws IOException {
        if (exception != null) {
            throw exception;
        }
        return BufferUtils.transfer(src, data);
    }

}