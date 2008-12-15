package org.limewire.nio.channel;

import java.nio.ByteBuffer;
import java.util.Random;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class BufferReaderTest extends BaseTestCase {
    
    private static Random RND = new Random();
    private ByteBuffer BUFFER = ByteBuffer.allocate(1024);
    private BufferReader READER = new BufferReader(BUFFER);

    public BufferReaderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(BufferReaderTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testReadOne() throws Exception {
        RND.nextBytes(BUFFER.array());
        
        ByteBuffer read = ByteBuffer.allocate(2000);
        read.limit(20);
        assertEquals(20, READER.read(read));
        assertEquals(BUFFER.array(), 0, 20, read.array(), 0, 20);
        read.limit(2000);
        assertEquals(1004, READER.read(read));
        assertEquals(BUFFER.array(), read.array(), 0, 1024);
        assertTrue(read.hasRemaining());
        assertEquals(0, READER.read(read));
        assertEquals(BUFFER.array(), read.array(), 0, 1024);
    }
    
    public void testReadScatter() throws Exception {
        RND.nextBytes(BUFFER.array());
        
        ByteBuffer read1 = ByteBuffer.allocate(100);
        ByteBuffer read2 = ByteBuffer.allocate(201);
        ByteBuffer read3 = ByteBuffer.allocate(399);
        ByteBuffer read4 = ByteBuffer.allocate(1000);
        ByteBuffer[] read = new ByteBuffer[] { read1, read2, read3, read4 };
        
        read1.limit(20);
        read2.limit(0);
        read3.limit(0);
        read4.limit(0);
        assertEquals(20, READER.read(read));
        assertEquals(BUFFER.array(), 0, 20, read1.array(), 0, 20);
        assertEquals(0, READER.read(read));
        
        read1.limit(100);
        read2.limit(201);
        read3.limit(399);
        read4.limit(1);
        assertEquals(681, READER.read(read));
        assertEquals(BUFFER.array(), 0, 100, read1.array());
        assertEquals(BUFFER.array(), 100, 201, read2.array());
        assertEquals(BUFFER.array(), 301, 399, read3.array());
        assertEquals(BUFFER.array(), 700, 1, read4.array(), 0, 1);
        
        read4.limit(1000);
        assertEquals(323, READER.read(read));
        assertEquals(BUFFER.array(), 0, 100, read1.array());
        assertEquals(BUFFER.array(), 100, 201, read2.array());
        assertEquals(BUFFER.array(), 301, 399, read3.array());
        assertEquals(BUFFER.array(), 700, 324, read4.array(), 0, 324);
        assertEquals(0, read1.remaining());
        assertEquals(0, read2.remaining());
        assertEquals(0, read3.remaining());
        assertEquals(676, read4.remaining());
        
        assertEquals(0, READER.read(read));
        assertEquals(BUFFER.array(), 0, 100, read1.array());
        assertEquals(BUFFER.array(), 100, 201, read2.array());
        assertEquals(BUFFER.array(), 301, 399, read3.array());
        assertEquals(BUFFER.array(), 700, 324, read4.array(), 0, 324);
        assertEquals(0, read1.remaining());
        assertEquals(0, read2.remaining());
        assertEquals(0, read3.remaining());
        assertEquals(676, read4.remaining());
    }
    
    public void testReadScatterWithArgs() throws Exception {
        RND.nextBytes(BUFFER.array());
        
        ByteBuffer read1 = ByteBuffer.allocate(100);
        ByteBuffer read2 = ByteBuffer.allocate(201);
        ByteBuffer read3 = ByteBuffer.allocate(399);
        ByteBuffer read4 = ByteBuffer.allocate(1000);
        ByteBuffer[] read = new ByteBuffer[] { read1, read2, read3, read4 };
        
        read1.limit(20);
        assertEquals(20, READER.read(read, 0, 1));
        assertEquals(BUFFER.array(), 0, 20, read1.array(), 0, 20);
        assertEquals(0, READER.read(read, 0, 1));
        
        read1.limit(100);
        read2.limit(201);
        assertEquals(281, READER.read(read, 0, 2));
        assertEquals(BUFFER.array(), 0, 100, read1.array());
        assertEquals(BUFFER.array(), 100, 201, read2.array());
        
        read3.limit(399);
        read4.limit(1);
        assertEquals(400, READER.read(read, 1, 3));
        assertEquals(BUFFER.array(), 0, 100, read1.array());
        assertEquals(BUFFER.array(), 100, 201, read2.array());
        assertEquals(BUFFER.array(), 301, 399, read3.array());
        assertEquals(BUFFER.array(), 700, 1, read4.array(), 0, 1);
        
        read4.limit(1000);
        assertEquals(323, READER.read(read, 0, 4));
        assertEquals(BUFFER.array(), 0, 100, read1.array());
        assertEquals(BUFFER.array(), 100, 201, read2.array());
        assertEquals(BUFFER.array(), 301, 399, read3.array());
        assertEquals(BUFFER.array(), 700, 324, read4.array(), 0, 324);
        assertEquals(0, read1.remaining());
        assertEquals(0, read2.remaining());
        assertEquals(0, read3.remaining());
        assertEquals(676, read4.remaining());
        
        assertEquals(0, READER.read(read, 0, 4));
        assertEquals(BUFFER.array(), 0, 100, read1.array());
        assertEquals(BUFFER.array(), 100, 201, read2.array());
        assertEquals(BUFFER.array(), 301, 399, read3.array());
        assertEquals(BUFFER.array(), 700, 324, read4.array(), 0, 324);
        assertEquals(0, read1.remaining());
        assertEquals(0, read2.remaining());
        assertEquals(0, read3.remaining());
        assertEquals(676, read4.remaining());
    }
}
