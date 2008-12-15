package com.limegroup.gnutella.lws.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.limewire.lws.server.LWSSenderOfMessagesToServer;
import org.limewire.lws.server.StringCallback;
import org.limewire.net.SocketsManager;
import org.limewire.service.ErrorService;

/**
 * This class defines instances of {@link LWSSenderOfMessagesToServer} that
 * simply communicate to a another server running on a given host and port. For
 * the purpose of testing it's used to talk to another server running on
 * <code>localhost</code>.
 */
public final class LocalServerDelegate {

    private final int port;
    private final SocketsManager socketsManager;
    private final String host;

    /**
     * @param host host name to which we communicate
     * @param port port to which we communicate
     * @param openner openner that will create a socket from <code>host</code>
     *        and <code>port</code>
     */
    public LocalServerDelegate(SocketsManager openner, String host, int port) {
        this.socketsManager = openner;
        this.host = host;
        this.port = port;
    }
    
    /**
     * This defines an interface to construct a URL based on a message and
     * arguments. This is used becaused when we're sending a URL to a server on
     * the client we encode the arguments as normal CGI parameters; when sending
     * to a mock remote server, we use a Wicket-style encoding. See
     * {@link #constructURL(String msg, Map<String, String> args)} for examples.
     */
    public interface URLConstructor {
        
        /**
         * Returns a URL based on the base message, <code>msg</code> and
         * arguments. Examples, for a message <code>"Foo"</code> and arguments
         * <code>{ "a" => "A", "b" => "B" }</code> would be:
         * 
         * <p>
         * 
         * <table>
         *  <tr>
         *   <th>Type</th>
         *   <th>URL</th>
         *  </tr>
         *  <tr>
         *   <td>Normal</td>
         *   <td><code>Foo?a=A&b=B</code></td>
         *  </tr>
         *  <tr>
         *   <td>Wicket</td>
         *   <td><code>store/app/pages/client/ClientCom/command/Foo/a/A/b/B</code></td>
         *  </tr>
         * </table>
         * 
         * @param msg base message
         * @param args arguments
         * @return a URL based on the base message, <code>msg</code> and
         *         arguments
         */
        String constructURL(String msg, Map<String, String> args);
    }
    
    /**
     * An implementation of {@link URLConstructor} using Wicket-style URLs. An
     * example, for a message <code>"Foo"</code> and arguments
     * <code>{ "a" => "A", "b" => "B" }</code> would be:
     * <code>store/app/pages/client/ClientCom/command/Foo/a/A/b/B</code>.
     */
    public static class WicketStyleURLConstructor implements URLConstructor {
        
        public final static WicketStyleURLConstructor INSTANCE = new WicketStyleURLConstructor();
        
        private WicketStyleURLConstructor() {}

        public String constructURL(String msg, Map<String, String> args) {
            StringBuffer sb = new StringBuffer("store/app/pages/client/ClientCom/command");
            sb.append("/").append(msg);
            for (String key : args.keySet()) {
                sb.append("/")
                  .append(key)
                  .append("/")
                  .append(args.get(key));
            }
            return sb.toString();
        } 
    }
    
    /**
     * An implementation of {@link URLConstructor} using normal CGI
     * parameter-encoding. An example, for a message <code>"Foo"</code> and
     * arguments <code>{ "a" => "A", "b" => "B" }</code> would be
     * <code>Foo?a=A&b=B</code>.
     */
    public static class NormalStyleURLConstructor implements URLConstructor {
        
        public final static NormalStyleURLConstructor INSTANCE = new NormalStyleURLConstructor();
        
        private NormalStyleURLConstructor() {}

        public String constructURL(String msg, Map<String, String> args) {
            StringBuffer sb = new StringBuffer(msg);
            boolean first = true;
            for (String key : args.keySet()) {
                sb.append(first ? "?" : "&")
                  .append(key)
                  .append("=")
                  .append(args.get(key));
                first = false;
            }
            return sb.toString();
        } 
    }

    public void sendMessageToServer(String msg, Map<String, String> args, StringCallback cb, URLConstructor ctor) {
        try {
            int tmpPort = port;
            Socket tmpSock = null;
            for (; tmpPort < port + 10; tmpPort++) {
               try { tmpSock = socketsManager.connect(new InetSocketAddress(host,port), 5000);} catch (IOException e) { /* skip */ }
                if (tmpSock != null) break;
            } 
            final Socket sock = tmpSock; 
            BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream())); 
            //
            // We'll always be adding this from the web page
            // to ensure a new script is loaded
            //
            Map<String,String> newArgs = new HashMap<String,String>(args);
            newArgs.put("_f", String.valueOf(System.currentTimeMillis()));
            String request = ctor.constructURL(msg, newArgs);
            wr.write("GET /" + request + " HTTP/1.1\n\r\n\r");
            wr.flush();
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            StringBuffer sb = new StringBuffer();
            //
            // We will have a similar response and can only give
            // StringCallbacks the actual response
            //
            // HTTP/1.1 200 OK
            // Last-modified: Fri, 19-10-2007 14:38:54 GMT
            // Server: null
            // Date: Fri Oct 19 14:38:54 EDT 2007
            // Content-length: 2
            //   
            // ok
            //
            // So just extract the 'ok'
            //
            String ln;
            boolean reading = false;
            //
            // Read past the header
            //
            while ((ln = in.readLine()) != null) {
                if (reading) {
                    sb.append(ln);
                    sb.append("\n");
                } else if (ln.equals("") || ln.equals("\n")) {
                    reading = true;
                }
            }
            String res = sb.toString();
            
            in.close();
            wr.close();
            sock.close();
            
            cb.process(res);
        } catch (IOException e) {
            ErrorService.error(e, "trying to connect on " + host + ":" + port);
        }
    }

}
