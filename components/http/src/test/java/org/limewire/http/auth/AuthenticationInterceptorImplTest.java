package org.limewire.http.auth;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.protocol.NHttpRequestHandler;
import org.apache.http.nio.protocol.NHttpResponseTrigger;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;

public class AuthenticationInterceptorImplTest extends BaseTestCase {

    private Mockery mockery;
    private Authenticator authenticator;
    private ProtectedHandler protectedHandler;
    private AuthenticationInterceptorImpl requestAuthenticatorImpl;
    private NHttpRequestHandler guardedHandler;

    @Override
    protected void setUp() throws Exception {
        mockery = new Mockery();
        authenticator = mockery.mock(Authenticator.class);
        protectedHandler = new ProtectedHandler();
        requestAuthenticatorImpl = new AuthenticationInterceptorImpl(authenticator);
        guardedHandler = requestAuthenticatorImpl.getGuardedHandler("/", protectedHandler);
    }
    
    
    
    public AuthenticationInterceptorImplTest(String name) {
        super(name);
    }

    public void testProcessNoCredentials() throws Exception {
        mockery.checking(new Expectations() {{
            never(authenticator).authenticate(with(any(Credentials.class)));
        }});
        protectedHandler.setShouldBeCalled(false);
        HttpRequest request = new BasicHttpRequest("GET", "/");
        runTest(request, createResponse(1, "WWW-Authenticate: Basic realm=\"secure\"", 401));
    }

    public void testGuardedHandlerDoesNotGuard() {
        Unprotectedhandler handler = new Unprotectedhandler();
        assertSame(handler, requestAuthenticatorImpl.getGuardedHandler("blakjdlkjb", handler));
    }
    
    public void testUnprotectedHandler() throws Exception {
        Unprotectedhandler handler = new Unprotectedhandler();
        NHttpRequestHandler guardedHandler = requestAuthenticatorImpl.getGuardedHandler("/test/", handler);
        HttpRequest request = new BasicHttpRequest("GET", "/test/");
        // this one on the other hand should be called
        handler.setShouldBeCalled(true);
        
        BasicHttpContext context = new BasicHttpContext();
        final NHttpResponseTrigger trigger = mockery.mock(NHttpResponseTrigger.class);
        mockery.checking(new Expectations() {{
            allowing(trigger).submitResponse(with(any(HttpResponse.class)));
        }});
        requestAuthenticatorImpl.process(request, context);
        guardedHandler.handle(request, createResponse(0, "", 200), trigger, context);
        
        mockery.assertIsSatisfied();
        handler.assertIsSatisfied();
    }
    

    public void testProcessWrongCredentials() throws Exception {
        mockery.checking(new Expectations() {{
            one(authenticator).authenticate(with(new BaseMatcher<Credentials>() {
                @Override
                public boolean matches(Object item) {
                    Credentials credentials = (Credentials)item;
                    assertEquals("hello", credentials.getUserPrincipal().getName());
                    assertEquals("world", credentials.getPassword());
                    return true; 
                }
                @Override
                public void describeTo(Description description) {
                }
            }));
            will(returnValue(false));
        }});
        protectedHandler.setShouldBeCalled(false);
        runTest(createRequest("hello", "world"), createResponse(1, "WWW-Authenticate: Basic realm=\"secure\"", 401));
    }
    
    public void testProcessValidCredentials() throws Exception {
        mockery.checking(new Expectations() {{
            one(authenticator).authenticate(with(new BaseMatcher<Credentials>() {
                @Override
                public boolean matches(Object item) {
                    Credentials credentials = (Credentials)item;
                    assertEquals("hello", credentials.getUserPrincipal().getName());
                    assertEquals("world", credentials.getPassword());
                    return true; 
                }
                @Override
                public void describeTo(Description description) {
                }
            }));
            will(returnValue(true));
        }});
        protectedHandler.setShouldBeCalled(false);
        runTest(createRequest("hello", "world"), createResponse(0, "", 200));
    }
    
    private HttpResponse createResponse(final int times, final String header, final int code) {
        final HttpResponse response = mockery.mock(HttpResponse.class);
        mockery.checking(new Expectations() {{
            exactly(times).of(response).addHeader(with(new BaseMatcher<Header>() {
                @Override
                public boolean matches(Object item) {
                    return item.toString().equals(header);
                }
                @Override
                public void describeTo(Description description) {
                }
            }));
            exactly(times).of(response).setStatusCode(code);
        }});
        return response;
    }
    
    private void runTest(HttpRequest request, HttpResponse response) throws Exception {
        BasicHttpContext context = new BasicHttpContext();
        final NHttpResponseTrigger trigger = mockery.mock(NHttpResponseTrigger.class);
        mockery.checking(new Expectations() {{
            allowing(trigger).submitResponse(with(any(HttpResponse.class)));
        }});
        requestAuthenticatorImpl.process(request, context);
        guardedHandler.handle(request, response, trigger, context);
        protectedHandler.assertIsSatisfied();
        mockery.assertIsSatisfied();
    }
    

    
    private Header createAuthorizationHeader(String username, String password) {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
        return BasicScheme.authenticate(credentials, "UTF-8", false);
    }
    
    private HttpRequest createRequest(String username, String password) {
        BasicHttpRequest request = new BasicHttpRequest("GET", "/");
        request.addHeader(createAuthorizationHeader(username, password));
        return request;
    }

    private class Unprotectedhandler extends Handler {
        
    }
    
    @RequiresAuthentication
    private class ProtectedHandler extends Handler {
        @Override
        public void handle(HttpRequest arg0, HttpResponse arg1, NHttpResponseTrigger arg2,
                HttpContext arg3) throws HttpException, IOException {
            // call super for setting common state
            super.handle(arg0, arg1, arg2, arg3);
            ServerAuthState serverAuthState = (ServerAuthState)arg3.getAttribute(ServerAuthState.AUTH_STATE);
            assertNotNull(serverAuthState.getCredentials());
        }
    }

    private class Handler implements NHttpRequestHandler {

        protected boolean shouldBeCalled;
        private boolean called = false;

        public void setShouldBeCalled(boolean shouldBeCalled) {
            this.shouldBeCalled = shouldBeCalled;
            this.called = false;
        }
        
        @Override
        public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
                HttpContext context) throws HttpException, IOException {
            return null;
        }

        @Override
        public void handle(HttpRequest request, HttpResponse response,
                NHttpResponseTrigger trigger, HttpContext context) throws HttpException,
                IOException {
            assertNotNull(context.getAttribute(ServerAuthState.AUTH_STATE));
            called = true;
        }
        
        public boolean assertIsSatisfied() {
            return shouldBeCalled == called;
        }
        
    }
}
