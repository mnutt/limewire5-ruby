package org.limewire.io;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;
import org.limewire.util.StringUtils;

/**
 * Unit tests for ByteReader
 */
public class ByteReaderTest extends BaseTestCase {
    
	public ByteReaderTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(ByteReaderTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testLegacy() throws Exception {
		InputStream in;
		ByteReader bin;
		String s;       
	
		in=new ByteArrayInputStream(StringUtils.toAsciiBytes("abc\r\na\rbc\n"));
		bin=new ByteReader(in);
	
		s=bin.readLine(); assertEquals("abc", s);
		s=bin.readLine(); assertEquals("abc", s);
		s=bin.readLine(); assertNull(s);
		s=bin.readLine(); assertNull(s);
	
		in=new ByteArrayInputStream(StringUtils.toAsciiBytes("a\ne"));
		bin=new ByteReader(in);
	
		s=bin.readLine(); assertEquals("a", s);
		s=bin.readLine(); assertNull(s);  
	}
	
}