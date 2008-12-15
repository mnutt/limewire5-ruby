package org.limewire.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.security.MessageDigest;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class SecureInputOutputTest extends BaseTestCase {
    
    public SecureInputOutputTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        return buildTestSuite(SecureInputOutputTest.class);
    }
    
    public void testSecureInputOutput() throws IOException {
        
        // Use some odd number
        int blockSize = 123;
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SecureOutputStream sos = new SecureOutputStream(baos, blockSize);
        DataOutputStream dos = new DataOutputStream(sos);
        
        Class[] types = new Class[] { 
                Boolean.class, 
                Byte.class, 
                Short.class, 
                Integer.class, 
                Float.class,
                Long.class,
                Double.class,
                String.class
        };
        
        String[] str = new String[] {
                "Hello World",
                "LimeWire",
                "Mojito",
                "Gnutella Network"
        };
        
        for (int i = 0, j = 0, k = 0; i < 4096; i++) {
            Class clazz = types[i % types.length];
            
            if (clazz.equals(Boolean.class)) {
                dos.writeBoolean((j++ % 2) == 0);
            } else if (clazz.equals(Byte.class)) {
                dos.write((byte)i);
            } else if (clazz.equals(Short.class)) {
                dos.writeShort((short)i);
            } else if (clazz.equals(Integer.class)) {
                dos.writeInt(i);
            } else if (clazz.equals(Float.class)) {
                dos.writeFloat(i);
            } else if (clazz.equals(Long.class)) {
                dos.writeLong(i);
            } else if (clazz.equals(Double.class)) {
                dos.writeDouble(i);
            } else if (clazz.equals(String.class)) {
                dos.writeUTF(str[k++ % str.length]);
            }
        }
        
        dos.close();
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        SecureInputStream sis = new SecureInputStream(bais);
        DataInputStream dis = new DataInputStream(sis);
        
        assertEquals(blockSize, sis.getBlockSize());
        assertEquals(sos.getBlockSize(), sis.getBlockSize());
        assertEquals(sos.getMessageDigest().getAlgorithm(), 
                sis.getMessageDigest().getAlgorithm());
        
        for (int i = 0, j = 0, k = 0; i < 4096; i++) {
            Class clazz = types[i % types.length];
            
            if (clazz.equals(Boolean.class)) {
                assertEquals((j++ % 2) == 0, dis.readBoolean());
            } else if (clazz.equals(Byte.class)) {
                assertEquals((byte)i, dis.readByte());
            } else if (clazz.equals(Short.class)) {
                assertEquals((short)i, dis.readShort());
            } else if (clazz.equals(Integer.class)) {
                assertEquals(i, dis.readInt());
            } else if (clazz.equals(Float.class)) {
                assertEquals((float)i, dis.readFloat());
            } else if (clazz.equals(Long.class)) {
                assertEquals(i, dis.readLong());
            } else if (clazz.equals(Double.class)) {
                assertEquals((double)i, dis.readDouble());
            } else if (clazz.equals(String.class)) {
                assertEquals(str[k++ % str.length], dis.readUTF());
            }
        }
        
        dis.close();
    }
    
    public void testSecureInputOutputHeader() throws IOException {
        
        String algorithm = "This is a very long MessageDigest algorithm name! "
                + "In fact its length must be at least 256 bytes to make sure "
                + "SecureOutputStream's length field in the header is utilitzing "
                + "more than one byte for the length. So what I am doing here is "
                + "to define an extremly long algorithm name for the fake MessageDigest "
                + "implementation. The goal is to make sure the for-loop that re-assembles "
                + "the length field works correctly";
        
        
        assertGreaterThan(0xFF, algorithm.getBytes().length);
        
        MessageDigest md = new FakeMessageDigest(algorithm, 16);
        
        // Use some odd number
        int blockSize = 123;
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SecureOutputStream sos = new SecureOutputStream(baos, md, blockSize);
        DataOutputStream dos = new DataOutputStream(sos);
        
        Class[] types = new Class[] { 
                Boolean.class, 
                Byte.class, 
                Short.class, 
                Integer.class, 
                Float.class,
                Long.class,
                Double.class,
                String.class
        };
        
        String[] str = new String[] {
                "Hello World",
                "LimeWire",
                "Mojito",
                "Gnutella Network"
        };
        
        for (int i = 0, j = 0, k = 0; i < 4096; i++) {
            Class clazz = types[i % types.length];
            
            if (clazz.equals(Boolean.class)) {
                dos.writeBoolean((j++ % 2) == 0);
            } else if (clazz.equals(Byte.class)) {
                dos.write((byte)i);
            } else if (clazz.equals(Short.class)) {
                dos.writeShort((short)i);
            } else if (clazz.equals(Integer.class)) {
                dos.writeInt(i);
            } else if (clazz.equals(Float.class)) {
                dos.writeFloat(i);
            } else if (clazz.equals(Long.class)) {
                dos.writeLong(i);
            } else if (clazz.equals(Double.class)) {
                dos.writeDouble(i);
            } else if (clazz.equals(String.class)) {
                dos.writeUTF(str[k++ % str.length]);
            }
        }
        
        dos.close();
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        SecureInputStream sis = new SecureInputStream(bais, md);
        DataInputStream dis = new DataInputStream(sis);
        
        assertEquals(blockSize, sis.getBlockSize());
        assertEquals(sos.getBlockSize(), sis.getBlockSize());
        assertEquals(sos.getMessageDigest().getAlgorithm(), 
                sis.getMessageDigest().getAlgorithm());
        
        for (int i = 0, j = 0, k = 0; i < 4096; i++) {
            Class clazz = types[i % types.length];
            
            if (clazz.equals(Boolean.class)) {
                assertEquals((j++ % 2) == 0, dis.readBoolean());
            } else if (clazz.equals(Byte.class)) {
                assertEquals((byte)i, dis.readByte());
            } else if (clazz.equals(Short.class)) {
                assertEquals((short)i, dis.readShort());
            } else if (clazz.equals(Integer.class)) {
                assertEquals(i, dis.readInt());
            } else if (clazz.equals(Float.class)) {
                assertEquals((float)i, dis.readFloat());
            } else if (clazz.equals(Long.class)) {
                assertEquals(i, dis.readLong());
            } else if (clazz.equals(Double.class)) {
                assertEquals((double)i, dis.readDouble());
            } else if (clazz.equals(String.class)) {
                assertEquals(str[k++ % str.length], dis.readUTF());
            }
        }
        
        dis.close();
    }

    private static class FakeMessageDigest extends MessageDigest {
        
        private int digestLength;

        public FakeMessageDigest(String algorithm, int diesgtLength) {
            super(algorithm);
            this.digestLength = diesgtLength;
        }

        @Override
        protected int engineGetDigestLength() {
            return digestLength;
        }

        @Override
        protected byte[] engineDigest() {
            return new byte[digestLength];
        }

        @Override
        protected void engineReset() {
        }

        @Override
        protected void engineUpdate(byte input) {
        }

        @Override
        protected void engineUpdate(byte[] input, int offset, int len) {
        }
    }
    
    public void testCorruptHeader() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SecureOutputStream sos = new SecureOutputStream(baos, 128);
        DataOutputStream dos = new DataOutputStream(sos);
        dos.writeUTF("Hello World!");
        dos.close();
        
        byte[] data = baos.toByteArray();
        
        data[5] = (byte)~data[5];
        
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            SecureInputStream sis = new SecureInputStream(bais);
            DataInputStream dis = new DataInputStream(sis);
            fail("Should have thrown a StreamCorruptedException!");
            dis.close();
        } catch (StreamCorruptedException ignored) {}
    }
    
    public void testCorruptPayload() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SecureOutputStream sos = new SecureOutputStream(baos, 128);
        DataOutputStream dos = new DataOutputStream(sos);
        
        for (int i = 0; i < 128; i++) {
            dos.writeShort(i);
        }
        dos.close();
        
        byte[] data = baos.toByteArray();
        assertGreaterThan(2*128, data.length);
        
        // Corrupt the second block
        data[200] = (byte)~data[200];
        
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        SecureInputStream sis = new SecureInputStream(bais);
        DataInputStream dis = new DataInputStream(sis);
        
        // Read the first block but make sure SecureInputStream
        // doesn't call refill which would trigger StreamCorruptedException
        dis.readFully(new byte[50]);
        
        try {
            dis.readFully(new byte[128]);
            fail("Should have thrown a StreamCorruptedException!");
        } catch (StreamCorruptedException ignored) {}
    }
}
