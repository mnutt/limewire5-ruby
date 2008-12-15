package org.limewire.lws.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.limewire.net.SocketsManager;
import org.limewire.net.SocketsManagerImpl;
import org.limewire.util.BaseTestCase;

/**
 * Provides the basis methods for doing communication. Subclasses should test
 * each aspect of this communication separately.
 */
abstract class AbstractCommunicationSupport extends BaseTestCase {
    
    public final static int LOCAL_PORT  = LocalServerImpl.PORT;
    public final static int REMOTE_PORT = RemoteServerImpl.PORT;
    
    /**
     * The number of times we'll try for a private key. Because we can't respond
     * in the client on the same thread we received a request, we hand back the
     * web page code the public key before giving it to the server. So it may
     * have not arrived yet.
     */
    private final static int TIMES_TO_TRY_FOR_PRIVATE_KEY = 3;
    
    /**
     * Time to sleep between tries for the private key if we haven't gotten it
     * yet.
     */
    private final static int SLEEP_TIME_BETWEEN_PRIVATE_KEY_TRIES = 1000;

    private LocalServerImpl localServer;
    private RemoteServerImpl remoteServer;
    private FakeJavascriptCodeInTheWebpage code;
    private String privateKey;
    private String sharedKey;
    
    private Thread localThread;
    private Thread remoteThread;    

    public AbstractCommunicationSupport(String s) {
        super(s);
    }
    
    // -------------------------------------------------------
    // Access
    // -------------------------------------------------------
    
    protected final LocalServerImpl getLocalServer() {
        return this.localServer;
    }

    protected final RemoteServerImpl getRemoteServer() {
        return this.remoteServer;
    }

    protected final FakeJavascriptCodeInTheWebpage getCode() {
        return this.code;
    }

    /**
     * This is not a {@link Collections#emptyMap()} because we may want to add
     * to it.
     */
    protected static final Map<String, String> NULL_ARGS = new HashMap<String,String>();
    
    protected static final Map<String, String> DUMMY_CALLBACK_ARGS;
    static {
        DUMMY_CALLBACK_ARGS = new HashMap<String,String>();
        DUMMY_CALLBACK_ARGS.put(LWSDispatcherSupport.Parameters.CALLBACK, "dummy");        
    }


    /** Override with functionality <b>after</b> {@link #setUp()}. */
    protected void beforeSetup() { }

    /** Override with functionality <b>after</b> {@link #setUp()}. */
    protected void afterSetup() { }

    /** Override with functionality <b>after</b> {@link #tearDown()}. */
    protected void beforeTearDown() { }

    /** Override with functionality <b>after</b> {@link #tearDown()}. */
    protected void afterTearDown() { }

    /**
     * Returns a handler that ensures that the response returned is one of
     * the <tt>code</tt>s.
     * 
     * @param wants expected codes
     * @return a handler that ensures that the response returned is
     *         on of the <tt>code</tt>
     */
    protected final FakeJavascriptCodeInTheWebpage.Handler errorHandler(final String want) {
        return errorHandler(new String[]{want});
    }
    
    /**
     * Returns a {@link FakeJavascriptCodeInTheWebpage#Handler} for ensuring
     * there was some error, but the type doesn't matter.
     * 
     * @return a {@link FakeJavascriptCodeInTheWebpage#Handler} for ensuring
     *         there was some error, but the type doesn't matter.
     */
    protected final FakeJavascriptCodeInTheWebpage.Handler errorHandlerAny() {
        return new FakeJavascriptCodeInTheWebpage.Handler() {
            public void handle(final String res) {
                final String have = LWSServerUtil.unwrapError(LWSServerUtil.removeCallback(res));
                assertTrue(have.indexOf('.') != -1);
            }
        };
    }
    
    /**
     * Returns a {@link FakeJavascriptCodeInTheWebpage#Handler} for ensuring
     * there was an error from <code>wants</code>.
     * 
     * @return Returns a {@link FakeJavascriptCodeInTheWebpage#Handler} for
     *         ensuring there was an error from <code>wants</code>.
     */    
    protected final FakeJavascriptCodeInTheWebpage.Handler errorHandler(final String[] wants) {
        return new FakeJavascriptCodeInTheWebpage.Handler() {
            public void handle(final String res) {
                //
                // We first have to remove the parens and single quotes
                // from around the message, because we always pass a
                // callback back to javascript
                //
                final String have = LWSServerUtil.unwrapError(LWSServerUtil.removeCallback(res));
                //
                // Make sure it's *one* of the expected results
                //
                boolean haveIt = false;
                for (String want: wants) {
                    if (have.equals(want)) {
                        haveIt = true;
                        break;
                    }
                }
                if (!haveIt) {
                    fail("didn't received one message of: " + unwrap(wants) + " but did receive '" + have  + "'");
                }
            }
        };
    }

    protected final Thread getLocalThread() {
        return localThread;
    }

    protected final Thread getRemoteThread() {
        return remoteThread;
    }

    @Override
    protected final void setUp() throws Exception {

        beforeSetup();
        
        SocketsManager socketsManager = new SocketsManagerImpl();
        localServer     = new LocalServerImpl(socketsManager, "localhost", REMOTE_PORT);
        remoteServer    = new RemoteServerImpl(socketsManager, LOCAL_PORT);
        localThread     = localServer.start();
        remoteThread    = remoteServer.start();
        code            = new FakeJavascriptCodeInTheWebpage(socketsManager, localServer, remoteServer);

        afterSetup();
    }

    @Override
    protected final void tearDown() throws Exception {

        beforeTearDown();

        stop(localServer);
        stop(remoteServer);

        localThread = null;
        remoteThread = null;

        afterTearDown();
    }    

    // -------------------------------------------------------
    // Convenience
    // -------------------------------------------------------

    protected final String doAuthenticate() {
        return doAuthenticate(getPrivateKey());
    }

    protected final String doAuthenticate(final String privateKey) {
        Map<String, String> args = new HashMap<String, String>();
        args.put(LWSDispatcherSupport.Parameters.PRIVATE, privateKey);
        return sendMessageFromWebpageToClient(LWSDispatcherSupport.Commands.AUTHENTICATE, args);
    }

    protected final String getPrivateKey() {
        if (privateKey == null) {
            requestPrivateAndSharedKeys();
        }
        return privateKey;
    }
    
    protected final String getSharedKey() {
        if (sharedKey == null) {
            requestPrivateAndSharedKeys();
        }
        return sharedKey;
    }    
    
    protected final static class KeyPair {
        
        private final String publicKey;
        private final String sharedKey;
        private final boolean isValid;
        
        private KeyPair(String publicKey, String sharedKey, boolean isValid) {
            this.publicKey = publicKey;
            this.sharedKey = sharedKey;
            this.isValid = isValid;
        }
        
        protected final String getPublicKey() {
            return publicKey;
        }
        
        protected final String getSharedKey() {
            return sharedKey;
        }
        
        /**
         * Returns whether this request was valid or not.
         * 
         * @return whether this request was valid or not.
         */
        protected final boolean isValid() {
            return isValid;
        }
        
        @Override
        public final String toString() {
            return "<publicKey=" + getPublicKey() + ",sharedKey=" + getSharedKey() + ",isValid=" + isValid() + ">";
        }
    }

    protected final KeyPair getPublicAndSharedKeys() {
        String res = sendMessageFromWebpageToClient(LWSDispatcherSupport.Commands.START_COM, NULL_ARGS);
        String parts[] = res.split(" ");
        if (parts.length < 2) {
            return new KeyPair(null, null, false);
        }
        return new KeyPair(parts[0], sharedKey = parts[1], true);
    }

    // -----------------------------------------------------------
    // Private
    // -----------------------------------------------------------
    
    private void requestPrivateAndSharedKeys() {
        KeyPair kp = getPublicAndSharedKeys();
        String publicKey = kp.getPublicKey();
        try {
            Thread.sleep(100);
        } catch (Exception e) {
            //
            // Not crucial, but would like to see the error
            //
            e.printStackTrace();
        }
        Map<String, String> args = new HashMap<String, String>();
        args.put(LWSDispatcherSupport.Parameters.PUBLIC, publicKey);
        //
        // We'll try this TIMES_TO_TRY_FOR_PRIVATE_KEY times
        // See the comment at TIMES_TO_TRY_FOR_PRIVATE_KEY for why.
        //
        for (int i=0; i<TIMES_TO_TRY_FOR_PRIVATE_KEY; i++) {
            privateKey = sendMessageFromClientToRemoteServer(LWSDispatcherSupport.Commands.GIVE_KEY, args);
            if (LWSServerUtil.isValidPrivateKey(privateKey)) break;
            try {
                Thread.sleep(SLEEP_TIME_BETWEEN_PRIVATE_KEY_TRIES);
            } catch (InterruptedException e) {
                // ignore
            }
        }        
    }      
    
    private String unwrap(String[] ss) {
        StringBuffer sb = new StringBuffer("{ ");
        if (ss != null) {
            for (String s : ss) {
                sb.append(String.valueOf(s));
                sb.append(" ");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private String sendMessageFromWebpageToClient(final String cmd, final Map<String, String> args) {
        args.put(LWSDispatcherSupport.Parameters.CALLBACK, "dummy");
        final String[] result = new String[1];
        getCode().sendLocalMsg(cmd, args, new FakeJavascriptCodeInTheWebpage.Handler() {
            public void handle(final String res) {
                result[0] = LWSServerUtil.removeCallback(res);
            }
        });
        return result[0];
    }

    private String sendMessageFromClientToRemoteServer(final String cmd, final Map<String, String> args) {
        args.put(LWSDispatcherSupport.Parameters.CALLBACK, "dummy");
        final String[] result = new String[1];
        getCode().sendRemoteMsg(cmd, args, new FakeJavascriptCodeInTheWebpage.Handler() {
            public void handle(final String res) {
                result[0] = LWSServerUtil.removeCallback(res);
            }
        });
        return result[0];
    }
    
    private void stop(final AbstractServer t) {
        if (t != null) {
          t.shutDown();
        }
    }    
}
