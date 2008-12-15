package org.limewire.security;


import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Random;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;
import org.limewire.util.ByteUtils;
import org.limewire.util.PrivilegedAccessor;


public class TEAMACCalculatorTest extends BaseTestCase {

    public TEAMACCalculatorTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(TEAMACCalculatorTest.class);
    }
    
    // Generates 420 QueryKeys using random secret keys 
    // and makes sure none of them contain 0x00 or 0x1C
    public void testAllBytesNetworkSafe() throws Exception {
        Random rand = new Random();
        InetAddress[] addresses = new InetAddress[20];
        byte[] randAddressBytes = new byte[4];
        for(int i=addresses.length-1; i >= 0; --i) {
            rand.nextBytes(randAddressBytes);
            addresses[i] = InetAddress.getByAddress(randAddressBytes);
        }
        
        for(int i=20; i > 0; --i) {
            TEAMACCalculator key = new TEAMACCalculator();
            for(int j=addresses.length-1; j >= 0; --j) {
                byte[] keyBytes = key.getMACBytes(new AddressSecurityToken.AddressTokenData(addresses[j], 1024 + i));
                for(int k=keyBytes.length-1; k >= 0; --k) {
                    int keyByte = keyBytes[k];
                    if (keyByte == 0x00 || keyByte == 0x1C) {
                        byte[] printBuf = new byte[keyBytes.length+1];
                        System.arraycopy(keyBytes, 0, printBuf, 1, keyBytes.length);
                        printBuf[0] = (byte) 1;
                        fail("keyBytes contains illegal byte "+ 
                                (new BigInteger(1,printBuf)).toString(16));
                    }
                }
            }
        }
    }
    
    //  Generates 420 QueryKeys using random secret keys 
    // and makes sure that they all can be verified by
    // the QueryKeyGenerator that created them
    public void testKeyVerification() throws Exception {
        Random rand = new Random();
        InetAddress[] addresses = new InetAddress[20];
        byte[] randAddressBytes = new byte[4];
        for(int i=addresses.length-1; i >= 0; --i) {
            rand.nextBytes(randAddressBytes);
            addresses[i] = InetAddress.getByAddress(randAddressBytes);
        }
        
        for(int i=20; i > 0; --i) {
            TEAMACCalculator key = new TEAMACCalculator();
            for(int j=addresses.length-1; j >= 0; --j) {
                byte[] keyBytes = key.getMACBytes(new AddressSecurityToken.AddressTokenData(addresses[j], 1024 + i));
                if (! Arrays.equals(keyBytes, key.getMACBytes(new AddressSecurityToken.AddressTokenData(addresses[j], 1024 + i)))) {
                    byte[] printBuf = new byte[keyBytes.length+1];
                    System.arraycopy(keyBytes, 0, printBuf, 1, keyBytes.length);
                    printBuf[0] = (byte) 1;
                    fail("keyBytes fails to verify "+ 
                            (new BigInteger(1,printBuf)).toString(16));
                }
            }
        }
    }
    
    
    // Breaks abstraction, but ensures that TEA is implemented correctly
    public void testTEAtestVectors() throws Exception {
        TEAVectorTester key = new TEAVectorTester(0,0,0,0,0,0);
        long cipherBlock = key.publicEncrypt(0x0L);
        assertEquals("TEA test vecotr failed", 0x41EA3A0A94BAA940L, cipherBlock);
        assertEquals("TEA test vector failed.", 0x41EA3A0A, key.encrypt(0,0));
        
        // get at the other half of the block and test the 64-bit rotation code
        key = new TEAVectorTester(0,0,0,0,0,32); 
        assertEquals("TEA test vector failed.", 0x94BAA940, key.encrypt(0,0));
    }

    /**
     * This test verifies the following identity:
     * 
     * Let E be the CBC-CMAC encryption
     * m1 and m2 two messages and
     * t1 = E(m1), t2 = E(m2)
     * 
     * then the following holds:
     * 
     * E(concat(m, t1 ^ m2)) = t2
     *
     */
    public void testEncryptCBCMACIdentity() {
        TEAMACCalculator generator = new TEAMACCalculator();
        Random random = new Random();
        
        for (int i = 0; i < 100; i++) {
            byte[] msg1 = new byte[8];
            random.nextBytes(msg1);
            byte[] msg2 = new byte[8]; 
            random.nextBytes(msg2);
            
            long tag1 = generator.encryptCBCCMAC(msg1);
            long tag2 = generator.encryptCBCCMAC(msg2);
            
            byte[] concat = new byte[16];
            System.arraycopy(msg1, 0, concat, 0, msg1.length);
            byte[] tag1Inbytes = new byte[8];
            ByteUtils.long2leb(tag1, tag1Inbytes, 0);
            System.arraycopy(xor(tag1Inbytes, msg2), 0, concat, msg1.length, msg1.length);
            
            assertEquals(tag2, generator.encryptCBCCMAC(concat));
        }
    }
    
    private static byte[] xor(byte a1, byte... a2) {
        return xor(new byte[] { a1 }, a2);
    }
    
    private static byte[] xor(byte[] a1, byte... a2) {
        byte[] xored = new byte[a1.length];
        for (int i = 0; i < a1.length; i++) {
            xored[i] = (byte) (a1[i] ^ a2[i]);
        }
        return xored;
    }
    
    public void testXor() {
        assertEquals(new byte[4], xor(new byte[4], new byte[4]));
        assertEquals(new byte[] { 1 }, xor((byte)1, (byte)0));
        assertEquals(new byte[] { 0 }, xor((byte)1, (byte)1));
    }
    
    public void testEncryptCBCMACIterations() {
        // a single iteration on input of length <= 8 should return 
        // the same as encrypt
        TEAMACCalculator generator = new TEAMACCalculator();
        Random random = new Random();
        
        for (int i = 1; i <= 8; i++) {
            byte[] data = new byte[i];
            random.nextBytes(data);
            long asLong = ByteUtils.leb2long(data, 0, data.length);
            assertEquals(generator.encrypt(asLong), generator.encryptCBCCMAC(data));
        }
        
        // ensure multiple iterations are executed and all data is processed
        for (int i = 9; i <= 16; i++) {
            byte[] data = new byte[i];
            random.nextBytes(data);
            long asLong = ByteUtils.leb2long(data, 0, 8);
            assertNotSame(generator.encrypt(asLong), generator.encryptCBCCMAC(data));
        }
        
        // ensure multiple iterations are executed and all data is processed
        for (int i = 9; i <= 16; i++) {
            byte[] data = new byte[i];
            random.nextBytes(data);
            long asLong = ByteUtils.leb2long(data, 0, 8);
            long asLong2 = ByteUtils.leb2long(data, 8, data.length - 8);
            assertEquals(generator.encrypt(generator.encrypt(asLong) ^ asLong2), generator.encryptCBCCMAC(data));
        }
    }
    
    private class TEAVectorTester extends TEAMACCalculator {
        /** Set up a tester with given TEA encryption keys */
        public TEAVectorTester(int k0, int k1, int k2, int k3, 
                int preRotate, int postRotate) {
            super(k0,k1,k2,k3, preRotate, postRotate);
        }
        
        /** In the absence of 0x00 and 0x1C bytes, returns the
         * left int of the TEA encryption block after encrypting (left,right).
         * Use the postRotate to get at different parts of the TEA encryption
         * output.
         */
        public int encrypt(int left, int right) throws UnknownHostException {
            // Prepare right for the unusual encoding
            // of IP addresses as ints used in QueryKeyGenerator
            for(int i=0x80; i>0; i <<= 8) {
                if ((right & i) != 0) {
                    right = (~right) ^ (i-1) ^ i;
                }
            }
            byte[] ipBytes = new byte[4];
            for(int i=0; i <= 3; ++i) {
                ipBytes[i] = (byte) right;
                right >>>= 8;
            }
            
            byte[] resultBytes = getMACBytes(new AddressSecurityToken.AddressTokenData(InetAddress.getByAddress(ipBytes), left));
            
            int byteCount = resultBytes.length;
            int result = 0;
            for(int i=byteCount-4; i<byteCount ; ++i) {
                result <<= 8;
                result |= 0xFF & resultBytes[i];
            }
            
            return result;
        }
        
        // Accesses the private method super.encrypt(long)
        public long publicEncrypt(long arg)
            throws NoSuchMethodException, IllegalAccessException,
            java.lang.reflect.InvocationTargetException {
            Long result = (Long) PrivilegedAccessor.invokeMethod(this, "encrypt", 
                                     new Object[] {arg},
                                     new Class[] { long.class });
            return result;
        }
    }
    
}
