package org.limewire.logging;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;

import junit.framework.Test;

public class LogImplTest extends BaseTestCase {
    
    
    private Mockery context;

    public LogImplTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(LogImplTest.class);
    }    
   
    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
    }

    public void testDebugWithArgs() {
        final org.apache.commons.logging.Log delegate = context.mock(Log.class);
        context.checking(new Expectations() {{
            allowing(delegate).isDebugEnabled();
            will(returnValue(true));
        }});
        Log log = new LogImpl(delegate);
        
        final Exception exception = new Exception();
        
        context.checking(new Expectations() {{
            one(delegate).debug("hello, world");
            one(delegate).debug("hello");
            one(delegate).debug("hello");
            one(delegate).debug("hello", exception);
        }});
        
        log.debugf("hello, {0}", "world");
        log.debug("hello");
        log.debugf("hello", "ignored");
        log.debug("hello", exception);
        
        context.assertIsSatisfied();
    }
    
    
}
