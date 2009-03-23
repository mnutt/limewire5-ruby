package org.limewire.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.io.IOUtils.ErrorType;
import org.limewire.service.MessageCallback;
import org.limewire.service.MessageService;
import org.limewire.util.BaseTestCase;
import org.limewire.util.StringUtils;


public final class IOUtilsTest extends BaseTestCase {

	/**
	 * Constructs a new <tt>IOUtilsTest</tt> with the specified name.
	 */
	public IOUtilsTest(String name) {
		super(name);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	/**
	 * Runs this suite of tests.
	 */
	public static Test suite() {
		return buildTestSuite(IOUtilsTest.class);
	}
	
	public void testHandleException() throws Exception {
	    tHandleException(new String[] {"no space left", "not enough space"}, "LimeWire was unable to write a necessary file because your hard drive is full. To continue using LimeWire you must free up space on your hard drive.", ErrorType.GENERIC);
	    tHandleException(new String[] {"being used by another process", "with a user-mapped section open"}, "LimeWire was unable to open a necessary file because another program has locked the file. LimeWire may act unexpectedly until this file is released.", ErrorType.GENERIC);
	    tHandleException(new String[] {"access is denied", "permission denied"}, "LimeWire was unable to write a necessary file because you do not have the necessary permissions. Your preferences may not be maintained the next time you start LimeWire, or LimeWire may behave in unexpected ways.", ErrorType.GENERIC);
	    tHandleException(new String[] {"invalid argument"}, "LimeWire cannot open a necessary file because the filename contains characters which are not supported by your operating system. LimeWire may behave in unexpected ways.", ErrorType.GENERIC);
	    
	    tHandleException(new String[] {"no space left", "not enough space"}, "LimeWire cannot download the selected file because your hard drive is full. To download more files, you must free up space on your hard drive.", ErrorType.DOWNLOAD);
        tHandleException(new String[] {"being used by another process", "with a user-mapped section open"}, "LimeWire was unable to download the selected file because another program is using the file. Please close the other program and retry the download.", ErrorType.DOWNLOAD);
        tHandleException(new String[] {"access is denied", "permission denied"}, "LimeWire was unable to create or continue writing an incomplete file for the selected download because you do not have permission to write files to the incomplete folder. To continue using LimeWire, please choose a different Save Folder.", ErrorType.DOWNLOAD);
        tHandleException(new String[] {"invalid argument"}, "LimeWire was unable to open the incomplete file for the selected download because the filename contains characters which are not supported by your operating system.", ErrorType.DOWNLOAD);
        
        assertFalse(IOUtils.handleException(new IOException("asdfoih"), null));
	}
	
	private void tHandleException(String[] input, final String output, ErrorType type) throws Exception {
	    for(String in : input) {
	        Mockery context = new Mockery();
	        final MessageCallback messageCallback = context.mock(MessageCallback.class);
	        MessageCallback oldCallback = MessageService.getCallback();
	        try {
    	        MessageService.setCallback(messageCallback);
    	        context.checking(new Expectations() {{
    	            exactly(1).of(messageCallback).showError(output);
    	        }});
    	        assertTrue(IOUtils.handleException(new IOException(in), type));
    	        context.assertIsSatisfied();
	        } finally {
	            MessageService.setCallback(oldCallback);
	        }
	    }
	}

	/**
	 * Tests the readWord method.
	 */
	public void testIOUtilsReadWord() throws Exception {
		String firstWord = "GET";
		String test0 = firstWord+" /get/0/file.txt";
		InputStream stream0 = new ByteArrayInputStream(StringUtils.toAsciiBytes(test0));
		String result = IOUtils.readWord(stream0, 3);
		assertEquals("result should equal first word", result, firstWord);


		InputStream stream1 = new ByteArrayInputStream(StringUtils.toAsciiBytes(test0));
		result = IOUtils.readWord(stream1, 4);
		assertEquals("result should equal first word", result, firstWord);
	}
	
	/**
	 * Ensures that readWord() doesn't handle multibyte words correctly. Test
	 * is only there, to prove documentation to be correct. 
	 */
	public void testReadWordDoesNotHandleMultibyteWord() throws Exception {
	    String multiByteWord = "\u30d5";
	    assertGreaterThan(multiByteWord.length(), StringUtils.toUTF8Bytes(multiByteWord).length);
	    String word = IOUtils.readWord(new ByteArrayInputStream(StringUtils.toUTF8Bytes(multiByteWord + " other stuff")), 100);
	    assertNotEquals(multiByteWord, word);
	}
	
	public void testReadFully() throws Exception {
	    byte[] read = new byte[7];
	    IOUtils.readFully(new TwoPartInputStream(new byte[] { 'h', 'e', 'y', }, new byte[] {' ', 'y', 'o', 'u' }), read);
	    assertEquals(new byte[] { 'h', 'e', 'y', ' ', 'y', 'o', 'u' }, read);
	}
	
	private class TwoPartInputStream extends InputStream {

	    private boolean first = true;
        private final byte[] firstPart;
        private final byte[] secondPart;
	    
	    public TwoPartInputStream(byte[] firstPart, byte[] secondPart) {
            this.firstPart = firstPart;
            this.secondPart = secondPart;
        }
	    
        @Override
        public int read() throws IOException {
            throw new IllegalStateException("should not be called");
        }
	    
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (first) {
                first = false;
                int length = Math.min(firstPart.length, len);
                System.arraycopy(firstPart, 0, b, off, length);
                return length;
            } else {
                int length = Math.min(secondPart.length, len);
                System.arraycopy(secondPart, 0, b, off, length);
                return length;
            }
        }
	}
}
