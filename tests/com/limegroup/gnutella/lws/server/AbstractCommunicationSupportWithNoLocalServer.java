package com.limegroup.gnutella.lws.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.LWSSettings;
import org.limewire.lws.server.LWSDispatcherSupport;
import org.limewire.lws.server.LWSServerUtil;
import org.limewire.net.SocketsManager;

import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.util.LimeTestCase;


/**
 * This is a simpler class than {@link AbstractCommunicationSupport} in the
 * <code>lwsserver</code> component, because it doesn't have a mock local
 * server representing that on the client. Instead it's an integration test so
 * only has a mock remote server and mock web page javascript code to send the
 * client commands.
 * 
 * <p>
 * 
 * There should be only one method for each class, or else multiple instances of
 * the remote server are created, and we need to share that among everything.
 */
abstract class AbstractCommunicationSupportWithNoLocalServer extends LimeTestCase {
    
    protected final Log LOG = LogFactory.getLog(getClass());
    
    public final int LOCAL_PORT  = LocalServerImpl.PORT;
    public final int REMOTE_PORT = RemoteServerImpl.PORT;
    
    protected final Map<String,String> EMPTY_ARGS = new HashMap<String,String>();
    
    /**
     * The number of times we'll try for a private key. Because we can't respond
     * in the client on the same thread we received a request, we hand back the
     * web page code the public key before giving it to the server. So it may
     * have not arrived yet.
     */
    private final int TIMES_TO_TRY_FOR_PRIVATE_KEY = 3;
    
    /**
     * Public key analogy for {@link TIMES_TO_TRY_FOR_PRIVATE_KEY}
     */
    private final int TIMES_TO_TRY_FOR_PUBLIC_KEY = 3;
    
    /**
     * Time to sleep between tries for the private key if we haven't gotten it
     * yet.
     */
    private final int SLEEP_TIME_BETWEEN_PRIVATE_KEY_TRIES = 300;
    
    /**
     * Public key analogy for {@link SLEEP_TIME_BETWEEN_PRIVATE_KEY_TRIES}
     */    
    private final int SLEEP_TIME_BETWEEN_PUBLIC_KEY_TRIES = 300;
    
    private RemoteServerImpl remoteServer;
    private Thread remoteThread;
    
    private CommandSender sender;
    private LWSManager lwsManager;
    private LifecycleManager lifecycleManager;
    
    private String privateKey;
    private String sharedKey;
    
        

    public AbstractCommunicationSupportWithNoLocalServer(String s) {
        super(s);
    }
    
    // -------------------------------------------------------
    // Access
    // -------------------------------------------------------

    protected final RemoteServerImpl getRemoteServer() {
        return this.remoteServer;
    }
    
    protected final CommandSender getCommandSender() {
        return this.sender;
    }
    
    protected final LWSManager getLWSManager() {
        return lwsManager;
    }

    /**
     * This is not a {@link Collections#emptyMap()} because we may want to add
     * to it.
     */
    protected final Map<String, String> NULL_ARGS = new HashMap<String,String>();
    
    protected final Map<String, String> DUMMY_CALLBACK_ARGS;
    
    {
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

    protected final Thread getRemoteThread() {
        return remoteThread;
    }
    
    private Injector inj;    

    @Override
    protected final void setUp() throws Exception {
        
        note("begin setUp");

        beforeSetup();

        LWSSettings.LWS_AUTHENTICATION_HOSTNAME.setValue("localhost");
        LWSSettings.LWS_AUTHENTICATION_PORT.setValue(8080);
        
        inj = LimeTestUtils.createInjector(Stage.PRODUCTION);
        remoteServer = new RemoteServerImpl(inj.getInstance(SocketsManager.class), LOCAL_PORT);
        lifecycleManager = inj.getInstance(LifecycleManager.class);
        lifecycleManager.start();
        remoteThread = remoteServer.start();
        lwsManager = getInstance(LWSManager.class);
        sender = new CommandSender();

        afterSetup();
        
        note("end setUp");
    }

    @Override
    protected final void tearDown() throws Exception {
        
        note("begin tearDown");

        beforeTearDown();
        
        doDetatch();

        remoteServer.shutDown();
        
        lifecycleManager.shutdown();
                
        privateKey = null;
        sharedKey = null;
        remoteThread = null;
       
        afterTearDown();
        
        note("end tearDown");
    }    
    
    protected final void doDetatch() {
        getCommandSender().detach(getPrivateKey(), getSharedKey());
        lwsManager.clearHandlersAndListeners();
    }

    protected final String doAuthenticate() {
        note("Authenticating");
        return doAuthenticate(getPrivateKey(), getSharedKey());
    }

    protected final void note(Object str) {
        LOG.debug(str);
    }

    protected final String doAuthenticate(final String privateKey, final String sharedKey) {
        Map<String, String> args = new HashMap<String, String>();
        args.put("private", privateKey);
        args.put("shared", sharedKey);
        note("Authenticating with private key '" + privateKey + "' and shared key '" + sharedKey + "'");
        return sendMessageFromWebpageToClient("Authenticate", args);        
    }
    
    protected final String sendPing() {
        Map<String, String> args = new HashMap<String, String>();
        return sendMessageFromWebpageToClient("Ping", args);          
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
        LOG.debug("have public and shared keys " + res);
        String parts[] = res.split(" ");
        if (parts.length < 2) {
            return new KeyPair(null, null, false);
        }
        return new KeyPair(parts[0], sharedKey = parts[1], true);
    }
   

    protected final String getPublicKey() {
        return sendMessageFromWebpageToClient(LWSDispatcherSupport.Commands.START_COM, NULL_ARGS);

    }
    
    /**
     * Returns the response after calling
     * {@link #sendMessageFromWebpageToClient(String, Map)} after adding the
     * private and shared keys.
     * <p>
     * Also, the only generic command the client understands is <code>Msg</code>,
     * with a argument of <code>command=</code><em>Command</em> where
     * <em>Command</em> would be something like <code>Download</code> or
     * <code>GetInfo</code>.
     * 
     * @param cmd Command to send
     * @param args arguments
     * @param includeDummyCallback <code>true</code> to include the callback
     *        <code>dummy</code>. This is used as a convenience in testing
     *        handlers when we don't need to have an explicit callback function.
     * @return the response after calling
     *         {@link #sendMessageFromWebpageToClient(String, Map)} after adding
     *         the private and shared keys.
     */
    protected final String sendCommandToClient(String cmd, Map<String, String> args,
            boolean includeDummyCallback) {
        Map<String, String> newArgs = new HashMap<String, String>(args);
        newArgs.put("command", cmd);
        newArgs.put("private", getPrivateKey());
        newArgs.put("shared", getSharedKey());
        if (includeDummyCallback) {
            newArgs.put("callback", "dummy");
        }
        return sendMessageFromWebpageToClient("Msg", newArgs);
    }

    /**
     * Returns the value of calling
     * {@link #sendCommandToClient(String, Map, boolean)} with the last argument
     * <code>false</code>, so this method requires an explicit callback
     * function in <code>args</code>.
     * 
     * @return the value of calling
     *         {@link #sendCommandToClient(String, Map, boolean)} with the last
     *         argument <code>false</code>, so this method requires an
     *         explicit callback function in <code>args</code>.
     * @see #sendCommandToClient(String, Map, boolean) *
     */
    protected final String sendCommandToClient(String cmd, Map<String, String> args) {
        return sendCommandToClient(cmd, args, false);
    }    
    
    protected final String sendMessageFromWebpageToClient(String cmd, Map<String, String> args) {
        args.put("callback", "dummy");
        String responseWithCallback = sender.sendMessage(cmd, args);
        return LWSServerUtil.removeCallback(responseWithCallback);
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
                //
                // All errors are of the form 
                //
                //   <String> [ '.' <String> ]+
                //
                // See LWSDispatcherSupport#ErrorCodes
                //
                assertTrue(have.indexOf('.') != -1);
            }
        };
    }
    
    /**
     * @see Injector#getInstance(Class)
     */
    protected final <T> T getInstance(Class<T> type) {
        return inj.getInstance(type);
    }


    // -----------------------------------------------------------
    // Private
    // -----------------------------------------------------------
    
    private void requestPrivateAndSharedKeys() {
        //
        // We'll try this a few times to make up for the race condition that exists and in unavoidable
        //
        String publicKey = null;
        for (int i=0; i<TIMES_TO_TRY_FOR_PUBLIC_KEY; i++) {
            KeyPair kp = getPublicAndSharedKeys();
            publicKey = kp.getPublicKey();
            if (publicKey != null) break;
            try {
                Thread.sleep(SLEEP_TIME_BETWEEN_PUBLIC_KEY_TRIES);
            } catch (Exception e) {
                //
                // Not crucial, but would like to see the error
                //
                e.printStackTrace();
            }
        }
        Map<String, String> args = new HashMap<String, String>();
        args.put(LWSDispatcherSupport.Parameters.PUBLIC, publicKey);
        
        String ip = "127.0.0.1";
        //
        // We'll try this TIMES_TO_TRY_FOR_PRIVATE_KEY times
        // See the comment at TIMES_TO_TRY_FOR_PRIVATE_KEY for why.
        //
        for (int i=0; i<TIMES_TO_TRY_FOR_PRIVATE_KEY; i++) {
            privateKey = remoteServer.lookupPrivateKey(publicKey, ip);
            if (LWSServerUtil.isValidPrivateKey(privateKey)) break;
            try {
                Thread.sleep(SLEEP_TIME_BETWEEN_PRIVATE_KEY_TRIES);
            } catch (InterruptedException e) {
                // ignore
            }
        }        
    }     
   
}
