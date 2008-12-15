package org.limewire.http;

import junit.framework.TestCase;

import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.limewire.http.reactor.DefaultDispatchedIOReactor;

public class HttpIOReactorTest extends TestCase {

    public void testHttpIOReactor() throws Exception {
        try {
            new DefaultDispatchedIOReactor(null, null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testExecute() throws Exception {
        HttpParams params = new BasicHttpParams();
        DefaultDispatchedIOReactor reactor = new DefaultDispatchedIOReactor(params, null);
        try {
            reactor.execute(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testShutdown() throws Exception {
        HttpParams params = new BasicHttpParams();
        DefaultDispatchedIOReactor reactor = new DefaultDispatchedIOReactor(params, null);
        try {
            reactor.shutdown();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    // TODO: Test acceptConnection!

}
