package org.limewire.http.entity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import junit.framework.TestCase;

import org.apache.http.HttpRequest;
import org.apache.http.message.BasicHttpRequest;
import org.limewire.util.StringUtils;

public class NotFoundEntityTest extends TestCase {

    public void testNotFoundEntityUri() throws IOException {
        NotFoundEntity entity = new NotFoundEntity("foobar");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        entity.writeTo(out);
        String content = StringUtils.getASCIIString(out.toByteArray());
        assertTrue(content.indexOf("foobar") != -1);
        assertTrue(content.indexOf("<html>") != -1);
    }

    public void testNotFoundEntityHttpRequest() throws Exception {
        HttpRequest request = new BasicHttpRequest("GET", "foobar");
        NotFoundEntity entity = new NotFoundEntity(request);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        entity.writeTo(out);
        String content = StringUtils.getASCIIString(out.toByteArray());
        assertTrue(content.indexOf("foobar") != -1);
        assertTrue(content.indexOf("<html>") != -1);
    }

}
