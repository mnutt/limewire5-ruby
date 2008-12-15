package org.limewire.lws.server;

import java.util.Map;

import org.limewire.lws.server.LWSServerUtil;

import junit.framework.Test;
import junit.textui.TestRunner;

public class ParseArgsTest extends AbstractParseTestCase {
    
    public ParseArgsTest(String s) { super(s); }
    
    public static Test suite() {
        return buildTestSuite(ParseArgsTest.class);
    }
    
    public static void main(String[] args) {
        TestRunner.run(suite());
    }

	public void test() {
		allTest();
	}

	@Override
	Map<String, String> parse(String line) {
		return LWSServerUtil.parseArgs(line);
	}

	@Override
	String getNameValuePairSeparator() {
		return "&";
	}
}
