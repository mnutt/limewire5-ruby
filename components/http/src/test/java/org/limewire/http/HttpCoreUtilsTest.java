package org.limewire.http;

import junit.framework.Test;

import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.limewire.util.BaseTestCase;

public class HttpCoreUtilsTest extends BaseTestCase {

    public HttpCoreUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HttpCoreUtilsTest.class);
    }

    public void testHasHeaderListValueExactMatch() {
        BasicHttpResponse msg = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, ""));
        BasicHeader header1 = new BasicHeader("key", "value1");
        BasicHeader header2 = new BasicHeader("key", "value2");
        msg.addHeader(header1);
        msg.addHeader(header2);
        
        assertTrue(HttpCoreUtils.hasHeaderListValue(msg, "key", "value1"));
        assertTrue(HttpCoreUtils.hasHeaderListValue(msg, "key", "value2"));
        assertFalse(HttpCoreUtils.hasHeaderListValue(msg, "key", "value3"));
        assertFalse(HttpCoreUtils.hasHeaderListValue(msg, "key1", "value1"));
        assertFalse(HttpCoreUtils.hasHeaderListValue(msg, "", ""));
        
        msg.removeHeaders("key");
        assertFalse(HttpCoreUtils.hasHeaderListValue(msg, "key", "value1"));
        assertFalse(HttpCoreUtils.hasHeaderListValue(msg, "key", "value2"));
        assertFalse(HttpCoreUtils.hasHeaderListValue(msg, "key1", "value1"));
        assertFalse(HttpCoreUtils.hasHeaderListValue(msg, "key1", "value2"));
    }

    public void testHasHeaderListValueMatchList() {
        BasicHttpResponse msg = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, ""));
        msg.addHeader(new BasicHeader("key", "foo,bar"));
        msg.addHeader(new BasicHeader("key", "  baz  "));
        msg.addHeader(new BasicHeader("key", "123"));
        msg.addHeader(new BasicHeader("key", "ab,   bc, DE"));
        
        assertTrue(HttpCoreUtils.hasHeaderListValue(msg, "key", "foo"));
        assertTrue(HttpCoreUtils.hasHeaderListValue(msg, "key", "bar"));
        assertTrue(HttpCoreUtils.hasHeaderListValue(msg, "key", "baz"));
        assertTrue(HttpCoreUtils.hasHeaderListValue(msg, "key", "123"));
        assertTrue(HttpCoreUtils.hasHeaderListValue(msg, "key", "ab"));
        assertTrue(HttpCoreUtils.hasHeaderListValue(msg, "key", "bc"));
        assertTrue(HttpCoreUtils.hasHeaderListValue(msg, "key", "DE"));
        assertFalse(HttpCoreUtils.hasHeaderListValue(msg, "key", "value"));
        assertFalse(HttpCoreUtils.hasHeaderListValue(msg, "key1", "bar"));
        assertFalse(HttpCoreUtils.hasHeaderListValue(msg, "key", "abc"));
        assertFalse(HttpCoreUtils.hasHeaderListValue(msg, "key", "foo,bar"));
        assertFalse(HttpCoreUtils.hasHeaderListValue(msg, "key", "foobar"));
        assertFalse(HttpCoreUtils.hasHeaderListValue(msg, "key", "de"));
        
        msg.removeHeaders("key");
        assertFalse(HttpCoreUtils.hasHeaderListValue(msg, "key", "foo"));
        assertFalse(HttpCoreUtils.hasHeaderListValue(msg, "key1", "foo"));
    }

}
