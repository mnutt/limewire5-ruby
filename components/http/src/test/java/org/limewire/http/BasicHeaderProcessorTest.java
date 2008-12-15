package org.limewire.http;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HttpContext;
import org.limewire.util.BaseTestCase;

public class BasicHeaderProcessorTest extends BaseTestCase {

    public BasicHeaderProcessorTest(String name) {
        super(name);
    }

    public void testAddRemoveClearInterceptors() {
        MyHeaderProcessor processor1 = new MyHeaderProcessor();
        MyHeaderProcessor processor2 = new MyHeaderProcessor();
        BasicHeaderProcessor processor = new BasicHeaderProcessor();
     
        assertEquals(0, processor.getInterceptors().length);
        processor.addInterceptor(processor1);
        assertEquals(1, processor.getInterceptors().length);
        processor.addInterceptor(processor2);
        assertEquals(2, processor.getInterceptors().length);
        assertEquals(new HeaderInterceptor[] { processor1, processor2 }, processor.getInterceptors());
        processor.removeInterceptor(processor1);
        assertEquals(1, processor.getInterceptors().length);
        assertEquals(new HeaderInterceptor[] { processor2 }, processor.getInterceptors());
        processor.removeInterceptor(processor1);
        assertEquals(1, processor.getInterceptors().length);
        assertEquals(new HeaderInterceptor[] { processor2 }, processor.getInterceptors());
        processor.clearInterceptors();
        assertEquals(0, processor.getInterceptors().length);
        processor.removeInterceptor(processor1);
        processor.removeInterceptor(processor2);
        assertEquals(0, processor.getInterceptors().length);
    }

    public void testProcess() throws Exception {
        BasicHttpResponse msg = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, ""));
        MyHeaderProcessor processor1 = new MyHeaderProcessor();
        MyHeaderProcessor processor2 = new MyHeaderProcessor();
        BasicHeaderProcessor processor = new BasicHeaderProcessor();
        
        processor.addInterceptor(processor1);
        processor.addInterceptor(processor2);
        processor.process(msg, null);
        assertEquals(0, processor1.count);
        assertEquals(0, processor2.count);
        
        BasicHeader header1 = new BasicHeader("key", "value1");
        BasicHeader header2 = new BasicHeader("key", "value2");
        msg.addHeader(header1);
        msg.addHeader(header2);
        processor.process(msg, null);
        assertEquals(2, processor1.count);
        assertEquals(header2, processor1.header);
        assertEquals(2, processor2.count);
        assertEquals(header2, processor2.header);
        
        processor.removeInterceptor(processor1);
        processor.process(msg, null);
        assertEquals(2, processor1.count);
        assertEquals(4, processor2.count);
        assertEquals(header2, processor2.header);
        
        msg.removeHeader(header2);
        processor.process(msg, null);
        assertEquals(2, processor1.count);
        assertEquals(5, processor2.count);
        assertEquals(header1, processor2.header);
        
        processor.clearInterceptors();
        processor.process(msg, null);
        assertEquals(2, processor1.count);
        assertEquals(5, processor2.count);
    }

    private class MyHeaderProcessor implements HeaderInterceptor {

        int count;

        Header header;

        public void process(Header header, HttpContext context)
                throws HttpException, IOException {
            this.count++;
            this.header = header;
        }

    }

}
