package com.limegroup.gnutella.lws.server;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.limewire.lws.server.LWSDispatcherSupport;
import org.limewire.lws.server.LWSServerUtil;
import org.limewire.lws.server.StringCallback;
import org.limewire.net.SocketsManager;

import com.limegroup.gnutella.lws.server.AbstractServer;

/**
 * A simple remote {@link RemoteServer}.
 */
public class RemoteServerImpl extends AbstractServer implements RemoteServer {

    /** The port on which we'll connect this server. */
    public final static int PORT = 8080;
    
    private final LocalServerDelegate del;

    public RemoteServerImpl(SocketsManager socketsManager, int otherPort) {
        super(PORT, "Remote Server");
        setDispatcher(new DispatcherImpl());
        this.del = new LocalServerDelegate(socketsManager, "localhost", otherPort);
    }
    
    /**
     * We DO want the IP of the incoming request to go to our handlers.
     */
    @Override
    protected final boolean sendIPToHandlers() {
        return true;
    }
    
    /**
     * Returns the equivalent of
     * 
     * <pre>
     * {&quot;public&quot; -&gt; &quot;PCURJKKTXE&quot;, &quot;private&quot; -&gt; &quot;BMBTVRVCSX&quot; }
     * </pre>
     * 
     * for request
     * 
     * <pre>
     * store\app\pages\client\ClientCom\command\StoreKey\public\PCURJKKTXE\private\BMBTVRVCSX/
     * </pre>
     * 
     * @param request request of the form
     * 
     * <pre>
     * store\app\pages\client\ClientCom\command\StoreKey\public\PCURJKKTXE\private\BMBTVRVCSX/
     * </pre>
     * 
     * @return the equivalent of
     * 
     * <pre>
     * {&quot;public&quot; -&gt; &quot;PCURJKKTXE&quot;, &quot;private&quot; -&gt; &quot;BMBTVRVCSX&quot; }
     * </pre>
     * 
     * for request
     * 
     * <pre>
     * store\app\pages\client\ClientCom\command\StoreKey\public\PCURJKKTXE\private\BMBTVRVCSX/
     * </pre>
     */
    protected static Map<String, String> getArgs(String request) {
        Map<String,String> args = new HashMap<String,String>();
        String target = "ClientCom";
        int itarget = request.indexOf(target);
        if (itarget == -1) return args;
        String rest = request.substring(itarget + target.length());
        if (rest.equals("")) return args;
        if (!Character.isLetterOrDigit(rest.charAt(0))) rest = rest.substring(1);
        if (rest.equals("")) return args;
        for (StringTokenizer st = new StringTokenizer(rest,"/\\" + File.separatorChar, false);
             st.hasMoreTokens();) {
            String name = st.nextToken();
            if (!st.hasMoreTokens()) {
                args.put(name, null);
                break;
            }
            String value = st.nextToken();
            args.put(name,value);
        }
        return args;
    }

    /**
     * Returns <code>StoreKey</code> for the following String:
     * <pre>
     * store\app\pages\client\ClientCom\command\StoreKey\public\PCURJKKTXE\private\BMBTVRVCSX/
     * </pre>
     * 
     * @param request
     * @return
     */
    public static String getCommand(String request) {
        String target = "command";
        int itarget = request.indexOf(target);
        if (itarget == -1) return null;
        String rest = request.substring(itarget + target.length());
        if (rest.equals("")) return null;
        if (!Character.isLetterOrDigit(rest.charAt(0))) rest = rest.substring(1);
        if (rest.equals("")) return null;
        StringBuffer res = new StringBuffer();
        for (int i=0; i<rest.length(); i++) {
            char c = rest.charAt(i);
            if (!Character.isLetterOrDigit(c)) break;
            res.append(c);
        }
        return res.toString();
    }
    
    private final class DispatcherImpl extends LWSDispatcherSupport {

        @Override
        protected final Handler[] createHandlers() {
            return new Handler[] { new StoreKey(), new GiveKey(), };

        }

        // ------------------------------------------------------------
        // Handlers
        // ------------------------------------------------------------

        /**
         * Sent from local server with parameters {@link Parameters#PUBLIC} and
         * {@link Parameters#PRIVATE} to store the in coming ip and these
         * parameters for when the local code asks for it.
         */
        class StoreKey extends AbstractHandler {

            public void handle(final Map<String, String> args, StringCallback cb) {
                String publicKey = args.get(LWSDispatcherSupport.Parameters.PUBLIC);
                if (publicKey == null) {
                    cb.process(report(LWSDispatcherSupport.ErrorCodes.MISSING_PUBLIC_KEY_PARAMETER));
                    return;
                }
                String privateKey = args.get(LWSDispatcherSupport.Parameters.PRIVATE);
                if (privateKey == null) {
                    cb.process(report(LWSDispatcherSupport.ErrorCodes.MISSING_PRIVATE_KEY_PARAMETER));
                    return;
                }
                String ip = args.get(LWSDispatcherSupport.Parameters.IP);
                if (ip == null) {
                    cb.process(report(LWSDispatcherSupport.ErrorCodes.MISSING_IP_PARAMETER));
                    return;
                }                
                if (LWSServerUtil.isEmpty(publicKey)) {
                    cb.process(LWSDispatcherSupport.ErrorCodes.INVALID_PUBLIC_KEY);
                    return;
                }
                storeKeys(publicKey, privateKey, ip);
                cb.process(LWSDispatcherSupport.Responses.OK);
            }
        }

        /**
         * Sent from local code to retrieve the private key with parameter
         * {@link Parameters#PUBLIC}.
         * {@link Parameters#IP}
         */
        class GiveKey extends HandlerWithCallback {

            @Override
            public void handleRest(final Map<String, String> args, StringCallback cb) {
                String publicKey = args.get(LWSDispatcherSupport.Parameters.PUBLIC);
                if (publicKey == null) {
                    cb.process(report(LWSDispatcherSupport.ErrorCodes.MISSING_PUBLIC_KEY_PARAMETER));
                    return;
                }
                String ip = args.get(LWSDispatcherSupport.Parameters.IP);
                if (ip == null) {
                    cb.process(report(LWSDispatcherSupport.ErrorCodes.MISSING_IP_PARAMETER));
                    return;
                }                
                String privateKey = lookupPrivateKey(publicKey, ip);
                cb.process(privateKey);
            }
        }

        public void sendMessageToServer(String msg, Map<String,String> args, StringCallback cb) throws IOException {
            del.sendMessageToServer(msg, args, cb, LocalServerDelegate.NormalStyleURLConstructor.INSTANCE);
        }

        @Override
        protected Map<String, String> getArgs(String request) {
            return RemoteServerImpl.getArgs(request);
        }

        @Override
        protected String getCommand(String request) {
            return RemoteServerImpl.getCommand(request);
        }
        
        @Override
        protected boolean isAuthenticated() {
            // This doesn't matter
            return false;
        }

        public void deauthenticate() {
            // This isn't used
        }
    }    

    // ---------------------------------------------------------------
    // RemoteServer
    // ---------------------------------------------------------------
    
    /** A pair. */
    private static class Pair {

        final String key;
        final String ip;

        Pair(final String key, final String ip) {
            this.key = key;
            this.ip = ip;
        }

        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof Pair)) return false;
            Pair that = (Pair) o;
            return this.key.equals(that.key) && this.ip.equals(that.ip);
        }

        @Override
        public int hashCode() {
            return this.key.hashCode() << 16 + this.ip.hashCode();
        }

        @Override
        public String toString() {
            return "<" + key + "," + ip + ">";
        }
    }

    private final Map<Pair, String> pairs2privateKeys = new HashMap<Pair, String>();

    public boolean storeKeys(String publicKey, String privateKey, String ip) {
        boolean result = pairs2privateKeys.put(new Pair(publicKey, ip), privateKey) != null;
        return result;
    }

    public String lookupPrivateKey(String publicKey, String ip) {
        if (publicKey == null) {
            return LWSDispatcherSupport.ErrorCodes.INVALID_PUBLIC_KEY_OR_IP;
        }
        Pair p = new Pair(publicKey, ip);
        String privateKey = pairs2privateKeys.get(p);
        String res = privateKey == null 
                ? LWSDispatcherSupport.ErrorCodes.INVALID_PUBLIC_KEY_OR_IP
                : privateKey;
        return res;
    }
}
