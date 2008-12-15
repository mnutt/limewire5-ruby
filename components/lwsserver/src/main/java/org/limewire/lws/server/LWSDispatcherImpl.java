/**
 * 
 */
package org.limewire.lws.server;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * Dispatches commands after going through an authentication phase explained
 * here. <br/> This represents the state in the FSA. Basically, the operation of
 * the local server is the following:
 * 
 * <pre>
 * Code -&gt; Local : Authenticate  
 *       +------+                             
 *  + -- | Wait | &lt;---------------------------+
 *  |    +------+                             | Local -&gt; Remote : StoreKey
 *  |    | Code -&gt; Local : Detach             | Local -&gt; Code : PrivateKey
 *  |    |                                    |
 *  |    V         Code -&gt; Local : Detach     |
 *  |    +------+ &lt;-------------------------- +-------+
 *  |    | Idle |                             | Store |
 *  |    +------+ --------------------------&gt; +-------+
 *  |    &circ;         Code -&gt; Local : StartCom
 *  |    |
 *  |    | Code -&gt; Local : Detach
 *  |    |
 *  |    +---------------+
 *  +--&gt; | Communicating |
 *       +---------------+
 * </pre>
 * 
 */
public final class LWSDispatcherImpl extends LWSDispatcherSupport {
       
    private final LWSSenderOfMessagesToServer sender;
    private String publicKey;
    private String privateKey;
    private String sharedKey;
    
    public LWSDispatcherImpl(LWSSenderOfMessagesToServer sender) {
        this.sender = sender;
    }
    
    public void deauthenticate() {
        publicKey = null;
        privateKey = null;
        sharedKey = null;
    }

    @Override
    protected final Handler[] createHandlers() {
        return new Handler[] { 
                new StartCom(),
                new Authenticate(), 
                new Detatch(), 
                new Msg() 
         };
    }
    
    @Override
    protected final boolean isAuthenticated() {
        return publicKey != null && privateKey != null && sharedKey != null;
    }
    
      /**
      * Returns the arguments to the right of the <code>?</code>. <br>
      * <code>static</code> for testing
      * 
      * @param request may be <code>null</code>
      * @return the arguments to the right of the <code>?</code>
      */
    @Override
    protected Map<String, String> getArgs(String request) {
         if (request == null || request.length() == 0) {
             return Collections.emptyMap();
         }
         int ihuh = request.indexOf('?');
         if (ihuh == -1) {
             return Collections.emptyMap();
         }
         final String rest = request.substring(ihuh + 1);
         return LWSServerUtil.parseArgs(rest);
     }   
     
     @Override
     protected String getCommand(String request) {
         int iprefix = request.indexOf(PREFIX);
         String res = iprefix == -1 ? request : request.substring(iprefix + PREFIX.length());
         final char[] cs = { '#', '?' };
         for (char c : cs) {
             final int id = res.indexOf(c);
             if (id != -1)
                 res = res.substring(0, id);
         }
         return res;
     }     
    
    final String getPublicKey() {
        return publicKey;
    }

    final String getPrivateKey() {
        return privateKey;
    }

    final String getSharedKey() {
        return sharedKey;
    }

    private void regenerateKeys() {
        publicKey = LWSServerUtil.generateKey();
        privateKey = LWSServerUtil.generateKey();
        sharedKey = LWSServerUtil.generateKey();
        note("public key  : {0}", publicKey);
        note("private key : {0}", privateKey);
        note("shared key  : {0}", sharedKey);
    }

    // ------------------------------------------------------------
    // Handlers
    // ------------------------------------------------------------

    /**
     * A {@link Handler} that needs both a callback and private
     * key.
     */
    protected abstract class HandlerWithCallbackWithPrivateKey extends HandlerWithCallback {

        @Override
        protected final void handleRest(final Map<String, String> args, StringCallback cb) {
            //
            // Check the private key
            //
            String privateKey = getPrivateKey();
            if (privateKey == null) {
                cb.process(report(LWSDispatcherSupport.ErrorCodes.UNITIALIZED_PRIVATE_KEY));
                return;
            }
            String herPrivateKey = args.get(LWSDispatcherSupport.Parameters.PRIVATE);
            if (herPrivateKey == null) {
                cb.process(report(LWSDispatcherSupport.ErrorCodes.MISSING_PRIVATE_KEY_PARAMETER));
                return;
            }
            if (!herPrivateKey.equals(privateKey)) {
                cb.process(report(LWSDispatcherSupport.ErrorCodes.INVALID_PRIVATE_KEY));
                return;
            }
            //
            // Check the shared key
            //
            String sharedKey = getSharedKey();
            if (sharedKey == null) {
                cb.process(report(LWSDispatcherSupport.ErrorCodes.UNITIALIZED_SHARED_KEY));
                return;
            }
            String herSharedKey = args.get(LWSDispatcherSupport.Parameters.SHARED);
            if (herSharedKey == null) {
                cb.process(report(LWSDispatcherSupport.ErrorCodes.MISSING_SHARED_KEY_PARAMETER));
                return;
            }
            if (!herSharedKey.equals(sharedKey)) {
                cb.process(report(LWSDispatcherSupport.ErrorCodes.INVALID_SHARED_KEY));
                return;
            }            
            handleRest(herPrivateKey, herSharedKey, args, cb);
        }

        /**
         * The result <b>IN PLAIN TEXT</b> using the private key,
         * <tt>privateKey</tt>. Override this to do something meaningful
         * with the passed along private key, too.
         * 
         * @param privateKey private key pulled from the args
         * @param herSharedKey the shared key sent from a Browser
         * @param args original, untouched arguments
         * @return result <b>IN PLAIN TEXT</b> using the private key,
         *         <tt>privateKey</tt>
         */
        abstract void handleRest(String privateKey,String herSharedKey, Map<String, String> args, StringCallback cb);
    }

    /**
     * Issues a command to start authentication.
     */
    class StartCom extends HandlerWithCallback {
        @Override
        protected void handleRest(final Map<String, String> args, final StringCallback cb) {
            regenerateKeys();
            //
            // send the keys to the Server and wait for a response
            //
            final Map<String, String> sendArgs = new HashMap<String, String>();
            sendArgs.put(LWSDispatcherSupport.Parameters.PRIVATE, privateKey);
            sendArgs.put(LWSDispatcherSupport.Parameters.PUBLIC, publicKey);
            try {
                sender.sendMessageToServer(LWSDispatcherSupport.Commands.STORE_KEY, sendArgs, new StringCallback() {
                    public void process(String response) {
                        //
                        // This could have a trailing space, so be nice
                        //
                        cb.process(response.indexOf(Responses.OK) != -1 ? publicKey + " " + sharedKey : "0");
                    }
                });
            } catch (IOException e) {
                // ignored
            }
        }
    }

    /**
     * Sent from code with private key to authenticate.
     */
    class Authenticate extends HandlerWithCallbackWithPrivateKey {
        @Override
        protected void handleRest(String privateKey, String sharedKey, Map<String, String> args, StringCallback cb) {
            notifyConnectionListeners(true);
            cb.process(LWSDispatcherSupport.Responses.OK);
        }
    }

    /**
     * Send from code to end session.
     */
    class Detatch extends HandlerWithCallback {
        @Override
        protected void handleRest(Map<String, String> args, StringCallback cb) {
            privateKey = null;
            publicKey = null;
            sharedKey = null;
            notifyConnectionListeners(false);
            cb.process(LWSDispatcherSupport.Responses.OK);
        }
    }

    /**
     * Sent from code with parameter {@link Parameters#COMMAND}.
     */
    class Msg extends HandlerWithCallbackWithPrivateKey {
        @Override
        protected void handleRest(String privateKey,
                                    String herSharedKey,
                                    Map<String, String> args, StringCallback cb) {
            String cmd = args.get(LWSDispatcherSupport.Parameters.COMMAND);
            if (cmd == null) {
                cb.process(report(LWSDispatcherSupport.ErrorCodes.MISSING_COMMAND_PARAMETER));
                return;
            }
            if (getCommandReceiver() != null) {
                Map<String, String> newArgs = new HashMap<String, String>(args);
                String newCmd = LWSServerUtil.addURLEncodedArguments(cmd, newArgs);
                String res = getCommandReceiver().receiveCommand(newCmd, newArgs);
                cb.process(res);
                return;
            }
            cb.process(LWSDispatcherSupport.Responses.NO_DISPATCHEE);
        }
    }

}