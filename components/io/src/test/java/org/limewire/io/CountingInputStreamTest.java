package org.limewire.io;

import java.io.IOException;
import java.io.InputStream;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class CountingInputStreamTest extends BaseTestCase {
    
    AnInputStream wrapped;
    
    @Override
    protected void setUp() throws Exception {
        wrapped = new AnInputStream();
    }
    
    public CountingInputStreamTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(CountingInputStreamTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testGetReadAmount() throws IOException {
        CountingInputStream in = new CountingInputStream(wrapped);
        wrapped.readValue = 1;
        in.read();
        in.read();
        in.read();
        assertEquals(3, in.getAmountRead());
        
        in.clearAmountRead();
        assertEquals(0, in.getAmountRead());
        
        in.read();
        assertEquals(1, in.getAmountRead());
        
        wrapped.readValue = -1;
        in.read();
        in.read();
        assertEquals(1, in.getAmountRead());
    }
    
    public void testSkip() throws IOException {
        CountingInputStream in = new CountingInputStream(wrapped);
    
        assertEquals(5, in.skip(5));
        
        assertEquals(5, in.getAmountRead());
        
        assertEquals(0, in.skip(0));
        
        assertEquals(5, in.getAmountRead());
    }
    
    public void testReadByteArray() throws IOException {
        CountingInputStream in = new CountingInputStream(wrapped);
        byte[] array = new byte[5];
        wrapped.readValue = 5;
        
        assertEquals(5, in.read(array));
        assertEquals(5, in.getAmountRead());

        assertEquals(5, in.read(array));
        assertEquals(10, in.getAmountRead());
        
        in.clearAmountRead();
        assertEquals(0, in.getAmountRead());
        
        wrapped.readValue = 3;
        assertEquals(3, in.read(array, 1, 3));
        assertEquals(3, in.getAmountRead());
        
        wrapped.readValue = -1;
        assertEquals(-1, in.read(array));
        assertEquals(3, in.getAmountRead());
        
        assertEquals(-1, in.read(array, 1, 3));
        assertEquals(3, in.getAmountRead());
    }
    
    private static class AnInputStream extends InputStream {

        public int readValue = 0;
        
        @Override
        public int read() throws IOException {
            return readValue;
        }
        
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return readValue;
        }
        
        @Override
        public long skip(long n) throws IOException {
            return n;
        }
        
    }
    
}
