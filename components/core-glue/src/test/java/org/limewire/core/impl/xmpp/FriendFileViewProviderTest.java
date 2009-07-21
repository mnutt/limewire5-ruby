package org.limewire.core.impl.xmpp;

import junit.framework.Test;

import org.apache.http.HttpStatus;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.http.auth.ServerAuthState;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.library.FileView;
import com.limegroup.gnutella.library.FileViewManager;
import com.limegroup.gnutella.uploader.HttpException;

public class FriendFileViewProviderTest extends BaseTestCase {
    
    private Mockery context;
    private FileViewManager fileManager;
    private FriendFileViewProvider friendFileListProvider;
        
    
    public FriendFileViewProviderTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        fileManager = context.mock(FileViewManager.class);
        friendFileListProvider = new FriendFileViewProvider(fileManager);
    }
    
    public static Test suite() {
        return buildTestSuite(FriendFileViewProviderTest.class);
    }

    public void testGetListsForValidUserId() throws Exception {
        context.checking(new Expectations() {{
            one(fileManager).getFileViewForId("me@you.com");
            will(returnValue(context.mock(FileView.class)));
        }});
        
        HttpContext httpContext = new BasicHttpContext();
        ServerAuthState authState = new ServerAuthState();
        authState.setCredentials(new UsernamePasswordCredentials("me@you.com", "password"));
        httpContext.setAttribute(ServerAuthState.AUTH_STATE, authState);
        
        friendFileListProvider.getFileViews("me@you.com", httpContext);
        
        context.assertIsSatisfied();
    }
    
    public void testGetListsUserIdAndCredentialsDontMatch() {
        HttpContext httpContext = new BasicHttpContext();
        ServerAuthState authState = new ServerAuthState();
        authState.setCredentials(new UsernamePasswordCredentials("me@you.com", "password"));
        httpContext.setAttribute(ServerAuthState.AUTH_STATE, authState);
        
        try {
            friendFileListProvider.getFileViews("hello@world.com", httpContext);
            fail("expected exception");
        } catch (HttpException he) {
            assertEquals(HttpStatus.SC_UNAUTHORIZED, he.getErrorCode());
        }
    }
    
    public void testGetListsUserIdHasNoFileList() {
        context.checking(new Expectations() {{
            one(fileManager).getFileViewForId("me@you.com");
            will(returnValue(null));
        }});
        
        HttpContext httpContext = new BasicHttpContext();
        ServerAuthState authState = new ServerAuthState();
        authState.setCredentials(new UsernamePasswordCredentials("me@you.com", "password"));
        httpContext.setAttribute(ServerAuthState.AUTH_STATE, authState);
        
        try {
            friendFileListProvider.getFileViews("me@you.com", httpContext);
            fail("expected exception");
        } catch (HttpException he) {
            assertEquals(HttpStatus.SC_NOT_FOUND, he.getErrorCode());
        }
        
        context.assertIsSatisfied();
    }
    
    
    public void testGetFileListsWithBadCredentials() {
        HttpContext httpContext = new BasicHttpContext();
        try {
            friendFileListProvider.getFileViews(null, httpContext);
            fail("Did not throw exception for attempt without user.");
        } catch (HttpException e) {
            // Expected
        }
        
        try {
            friendFileListProvider.getFileViews("tester@test", httpContext);
            fail("Did not throw exception for attempt without an AuthState.");
        } catch (HttpException e) {
            // Expected
        }
        
        ServerAuthState authState = new ServerAuthState();
        httpContext.setAttribute(ServerAuthState.AUTH_STATE, authState);
        
        try {
            friendFileListProvider.getFileViews("tester@test", httpContext);
            fail("Did not throw exception for attempt without credentials.");
        } catch (HttpException e) {
            // Expected 
        }
    }
}