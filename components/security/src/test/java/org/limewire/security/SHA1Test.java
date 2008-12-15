/*
 * @(#)SHA1Test.java   1.9 2004-04-22
 * This file was freely contributed to the LimeWire project  and is covered
 * by its existing GPL licence, but it may be used individually as a public
 * domain implementation of a published algorithm (see below for references).
 * It was also freely contributed to the Bitzi public domain sources.
 * @author  Philippe Verdy
 */

/*
 * Sun may wish to change the following package name, if integrating this
 * class in the Sun JCE Security Provider for Java 1.5 (code-named Tiger).
 */
package org.limewire.security;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;
import org.limewire.util.TestUtils;

public class SHA1Test extends BaseTestCase {
    
    private static final SHA1 hash = new SHA1();
    
       
    public SHA1Test(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(SHA1Test.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }          

    public void testBasic() throws Exception {
// http://csrc.nist.gov/publications/fips/fips180-2/fips180-2.pdf
        tst(1, 1,
            "abc",
            "A9993E36 4706816A BA3E2571 7850C26C 9CD0D89D");
        tst(1, 2,
            "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq",
            "84983E44 1C3BD26e BAAE4AA1 F95129E5 E54670F1");
        tst(1, 3,
            1000000, "a",
            "34AA973C D4C4DAA4 F61EEB2B DBAD2731 6534016F");
        // See also http://csrc.ncsl.nist.gov/cryptval/shs/sha1-vectors.zip
    }
    
    public void testRate() throws Exception {
        final int ITERATIONS = 16000;
        final int BLOCKSIZE = 65536;
        byte[] input = new byte[BLOCKSIZE];
        for (int i = BLOCKSIZE; --i >= 0; )
            input[i] = (byte)i;

        long t0 = System.currentTimeMillis();
        for (int i = ITERATIONS; --i >= 0; );
        long t1 = System.currentTimeMillis();
        for (int i = ITERATIONS; --i >= 0; )
            hash.engineUpdate(input, 0, BLOCKSIZE);
        long t2 = System.currentTimeMillis();
        hash.engineReset();
        double rate = 1000.0 * ITERATIONS * BLOCKSIZE / ((t2 - t1) - (t1 - t0));
                    
        double limeRate = rate;
        //System.out.println("Our rate = " +
        //                    (float)(rate * 8) + " bits/s = " +
        //                    (float)(rate / (1024 * 1024)) + " Megabytes/s");
        // Java 1.5 beta-b32c, on Athlon XP 1800+: 47.62 Megabytes/s.

        MessageDigest md = MessageDigest.getInstance("SHA");
        t0 = System.currentTimeMillis();
        for (int i = ITERATIONS; --i >= 0; );
        t1 = System.currentTimeMillis();
        for (int i = ITERATIONS; --i >= 0; )
            md.update(input, 0, BLOCKSIZE);
        t2 = System.currentTimeMillis();
        md.reset();
        rate = 1000.0 * ITERATIONS * BLOCKSIZE / ((t2 - t1) - (t1 - t0));
            //System.out.println("JCE rate = " +
            //                    (float)(rate * 8) + " bits/s = " +
            //                    (float)(rate / (1024 * 1024)) + " Megabytes/s");
        // Java 1.5 beta-b32c, on Athlon XP 1800+: 22.78 Megabytes/s.
        
        assertGreaterThan(rate, limeRate);
    }
    
    public void testFiles() throws Exception {
        String dirString = "org/limewire/security";
        File testDir = TestUtils.getResourceFile(dirString);
        assertTrue(testDir.isDirectory());
        int tested = testDirectory(testDir);
        assertGreaterThan("didn't test enough", 10, tested);
    }
    
    public int testDirectory(File dir) throws Exception {
        int tested = 0;
        File[] files = dir.listFiles();
        for(int i = 0; i < files.length; i++) {
            File f = files[i];
            if(f.isDirectory())
                tested += testDirectory(f);
            else if(f.isFile() && f.exists()) {
                byte[] old = createSHA1(f, true);
                byte[] now = createSHA1(f, false);
                assertEquals(old, now);
                tested++;
            }
        }
        return tested;
    }
    
    
	private static byte[] createSHA1(final File file, boolean digest) 
      throws Exception {
		FileInputStream fis = new FileInputStream(file);   		
		// we can only calculate SHA1 for now
		MessageDigest md = null;
        if(digest)
            md = MessageDigest.getInstance("SHA");
        else
            md = new SHA1();
        
        try {
            byte[] buffer = new byte[16384];
            int read;
            while ((read=fis.read(buffer))!=-1)
                md.update(buffer,0,read);
        } finally {		
            fis.close();
        }

        return md.digest();
	}

    
    private static final void tst(final int set, final int vector,
                                     final String source,
                                     final String expect) {
     //   System.out.print("Set " + set + ", vector# " + vector + ": ");
        byte[] input = new byte[source.length()];
        for (int i = 0; i < input.length; i++)
            input[i] = (byte)source.charAt(i);
        hash.engineUpdate(input, 0, input.length);
        tstResult(expect);
    }

    private static final void tst(final int set, final int vector,
                                     final int times, final String source,
                                     final String expect) {
     //   System.out.print("Set " + set + ", vector# " + vector + ": ");
        byte[] input = new byte[source.length()];
        for (int i = 0; i < input.length; i++)
            input[i] = (byte)source.charAt(i);
        for (int i = 0; i < times; i++)
            hash.engineUpdate(input, 0, input.length);
        tstResult(expect);
    }

    private static final void tstResult(final String expect) {
        final String result = toHex(hash.engineDigest());
        assertEquals(expect.toUpperCase(), result);
    }

    private static final String toHex(final byte[] bytes) {
        StringBuffer buf = new StringBuffer(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            if ((i & 3) == 0 && i != 0)
               buf.append(' ');
            buf.append(HEX.charAt((bytes[i] >> 4) & 0xF))
               .append(HEX.charAt( bytes[i]       & 0xF));
        }
        return buf.toString();
    }

    private static final String HEX = "0123456789ABCDEF";
}
