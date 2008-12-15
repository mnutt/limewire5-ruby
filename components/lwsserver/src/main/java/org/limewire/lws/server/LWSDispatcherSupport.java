/**
 * 
 */
package org.limewire.lws.server;


import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.NHttpResponseTrigger;
import org.apache.http.protocol.HttpContext;
import org.limewire.concurrent.ExecutorsHelper;

/**
 * Instances of this class will receive HTTP requests and are responsible to
 * doling them out to listeners.  This is abstract so we can have test
 * cases using some of the logic.
 */
public abstract class LWSDispatcherSupport implements LWSDispatcher {
    
    private final static Log LOG = LogFactory.getLog(LWSDispatcherSupport.class);
    private final Map<String, Handler> names2handlers = new HashMap<String, Handler>();
    private LWSReceivesCommandsFromDispatcher commandReceiver;
    private final Executor handlerExecutor = ExecutorsHelper.newProcessingQueue("lws-handlers");
    
    /** Package protected for testing. */
    public final static byte[] PING_BYTES = new byte[]{
        (byte)0x89, (byte)0x50, (byte)0x4E, (byte)0x47, (byte)0x0D, (byte)0x0A, (byte)0x1A, (byte)0x0A, (byte)0x00, 
        (byte)0x00, (byte)0x00, (byte)0x0D, (byte)0x49, (byte)0x48, (byte)0x44, (byte)0x52, (byte)0x00, (byte)0x00, 
        (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x08, (byte)0x06, (byte)0x00, 
        (byte)0x00, (byte)0x00, (byte)0x1F, (byte)0x15, (byte)0xC4, (byte)0x89, (byte)0x00, (byte)0x00, (byte)0x00, 
        (byte)0x01, (byte)0x73, (byte)0x52, (byte)0x47, (byte)0x42, (byte)0x00, (byte)0xAE, (byte)0xCE, (byte)0x1C, 
        (byte)0xE9, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x04, (byte)0x67, (byte)0x41, (byte)0x4D, (byte)0x41, 
        (byte)0x00, (byte)0x00, (byte)0xB1, (byte)0x8F, (byte)0x0B, (byte)0xFC, (byte)0x61, (byte)0x05, (byte)0x00, 
        (byte)0x00, (byte)0x00, (byte)0x20, (byte)0x63, (byte)0x48, (byte)0x52, (byte)0x4D, (byte)0x00, (byte)0x00, 
        (byte)0x7A, (byte)0x26, (byte)0x00, (byte)0x00, (byte)0x80, (byte)0x84, (byte)0x00, (byte)0x00, (byte)0xFA, 
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x80, (byte)0xE8, (byte)0x00, (byte)0x00, (byte)0x75, (byte)0x30, 
        (byte)0x00, (byte)0x00, (byte)0xEA, (byte)0x60, (byte)0x00, (byte)0x00, (byte)0x3A, (byte)0x98, (byte)0x00, 
        (byte)0x00, (byte)0x17, (byte)0x70, (byte)0x9C, (byte)0xBA, (byte)0x51, (byte)0x3C, (byte)0x00, (byte)0x00, 
        (byte)0x00, (byte)0x18, (byte)0x74, (byte)0x45, (byte)0x58, (byte)0x74, (byte)0x53, (byte)0x6F, (byte)0x66, 
        (byte)0x74, (byte)0x77, (byte)0x61, (byte)0x72, (byte)0x65, (byte)0x00, (byte)0x50, (byte)0x61, (byte)0x69, 
        (byte)0x6E, (byte)0x74, (byte)0x2E, (byte)0x4E, (byte)0x45, (byte)0x54, (byte)0x20, (byte)0x76, (byte)0x33, 
        (byte)0x2E, (byte)0x31, (byte)0x30, (byte)0x72, (byte)0xB2, (byte)0x25, (byte)0x92, (byte)0x00, (byte)0x00, 
        (byte)0x00, (byte)0x0B, (byte)0x49, (byte)0x44, (byte)0x41, (byte)0x54, (byte)0x18, (byte)0x57, (byte)0x63, 
        (byte)0xF8, (byte)0x0F, (byte)0x04, (byte)0x00, (byte)0x09, (byte)0xFB, (byte)0x03, (byte)0xFD, (byte)0x2B, 
        (byte)0xD5, (byte)0x08, (byte)0x45, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x49, (byte)0x45, 
        (byte)0x4E, (byte)0x44, (byte)0xAE, (byte)0x42, (byte)0x60, (byte)0x82,
    };    
    
    public LWSDispatcherSupport() {
        Handler[] hs = createHandlers();
        if (LOG.isDebugEnabled()) LOG.debug("creating " + hs.length + " handler(s)...");
        for (Handler h : hs) {
            this.names2handlers.put(h.name().toLowerCase(Locale.US), h);
            if (LOG.isDebugEnabled()) LOG.debug(" - " + h.name());
        }
    }
    
    // ------------------------------------------------------
    // Abstract
    // ------------------------------------------------------
    
    /**
     * Returns whether the web page has authenticated yet. This is used for
     * determining whether to handle a PING request or not.
     */
    protected abstract boolean isAuthenticated();
    
    
    /*
     * The abstraction is this.  There are two subclasses of this class:
     * 
     *   - LWSMainDispatcher (for deployment)
     *   - RemoteServerImpl.DispatcherImpl (for testing)
     * 
     * Both follow basically the pattern except the remote version is supposed
     * be a mock Wicket server, so we have to send urls in a different way.
     * In both cases below Here, StoreKey is the command, and the args are 
     * {"public" -> "JNGMLANSKC", "private" -> "LNUCOQSVOR"}
     * 
     * LWSMainDispatcher takes CGI parameters in the normal way:
     * 
     *   - http://some.url/StoreKey?public=JNGMLANSKC&private=LNUCOQSVOR
     *   
     * But RemoteServerImpl.DispatcherImpl takes them slightly differently:
     * 
     *   - http://some.url/StoreKey/public/JNGMLANSKC/private/LNUCOQSVOR
     *   
     * In particular the correct call that we will see would look like this
     * 
     *   - store\app\pages\client\ClientCom\command\StoreKey\public\JNGMLANSKC\private\LNUCOQSVOR
     *   
     * So we need to abstract out removing the command, and retrieving the arguments.
     */
    
    /**
     * Returns the command portion of a request or null if the parameter
     * <code>command</code> is missing or the parameter <code>command</code>
     * doesn't have an argument.
     * <p>
     * For example, in the case of {@link LWSDispatcherImpl} we would have the
     * following:
     * 
     * <pre>
     * http://some.url/StoreKey?public=JNGMLANSKC&amp;private=LNUCOQSVOR -&gt; StoreKey
     * http://some.url/?public=JNGMLANSKC&amp;private=LNUCOQSVOR         -&gt; null
     * </pre>
     * 
     * In the case of {@link RemoteServerImpl.DispatcherImpl} we would have
     * 
     * <pre>
     * store\app\pages\client\ClientCom\command\StoreKey\public\JNGMLANSKC\private\LNUCOQSVOR -&gt; StoreKey
     * store\app\pages\client\ClientCom\command                                               -&gt; null
     * store\app\pages\client\ClientCom\command\                                              -&gt; null
     * store\app\pages\client\ClientCom\ccccommand                                            -&gt; null
     * 
     * @param
     */
    protected abstract String getCommand(String request);
    
    /**
     * Returns the arguments portion of a request in the same order they appear.
     * 
     * <p>
     * 
     * For example, in the case of {@link LWSDispatcherImpl} we would have the
     * following:
     * 
     * <pre>
     * http://some.url/StoreKey?public=JNGMLANSKC&amp;private=LNUCOQSVOR -&gt; {"public" -> "JNGMLANSKC", "private" -> "LNUCOQSVOR"}
     * http://some.url/?public=JNGMLANSKC&amp;private=LNUCOQSVOR         -&gt; {"public" -> "JNGMLANSKC", "private" -> "LNUCOQSVOR"}
     * </pre>
     * 
     * In the case of {@link RemoteServerImpl.DispatcherImpl} we would have
     * 
     * <pre>
     * store\app\pages\client\ClientCom\command\StoreKey\public\JNGMLANSKC\private\LNUCOQSVOR -&gt; {"command" -> "StoreKey", "public" -> "JNGMLANSKC", "private" -> "LNUCOQSVOR"}
     * store\app\pages\client\ClientCom\command                                               -&gt; {"command" -> null}
     * store\app\pages\client\ClientCom\command\                                              -&gt; {"command" -> null}
     * store\app\pages\client\ClientCom\ccccommand                                            -&gt; {"command" -> null}
     * </pre>
     * 
     * @param request
     * @return arugments in the order they are placed in the URL
     */
    protected abstract Map<String,String> getArgs(String request);
    
    
    // ------------------------------------------------------
    // Interface
    // ------------------------------------------------------
    
    public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
            HttpContext context) throws HttpException, IOException {
        return null;
    }
    
    
    public final void handle(HttpRequest httpReq, final HttpResponse response,
            final NHttpResponseTrigger trigger, HttpContext c) throws HttpException, IOException {
        String request = httpReq.getRequestLine().getUri();
        final String command = getCommand(request);
        note("Have command {0} ", command);
        //
        // If the command is ping and we are authenticated, then send
        // back the special PING response
        //
        if (isAuthenticated() && command.equals(Commands.PING)) {
            note("Handling PING");
            handlerExecutor.execute(new Runnable() {
                public void run() {
                        response.setEntity(new ByteArrayEntity(PING_BYTES));
                        trigger.submitResponse(response);
                }
            });
            notifyConnectionListeners(true);
            return;
        }
        final Handler h = names2handlers.get(command.toLowerCase());
        if (h == null) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Couldn't create a handler for " + command);
            }
            String str = report(LWSDispatcherSupport.ErrorCodes.UNKNOWN_COMMAND);
            response.setEntity(new NStringEntity(str));
            trigger.submitResponse(response);
            return;
        }
        if (LOG.isDebugEnabled())
            LOG.debug("have handler: " + h.name());
        final Map<String, String> args = getArgs(request);
        note("Have args {0} ", args);
        handlerExecutor.execute(new Runnable() {
            public void run() {
                h.handle(args, new StringCallback() {
                    public void process(String input) {
                        try {
                            note("Have response {0}",input);
                            response.setEntity(new NStringEntity(input));
                            trigger.submitResponse(response);
                        } catch (UnsupportedEncodingException e) {
                            trigger.handleException(e);
                        }
                    }                
                });
            }
        });
            
    }
    
    public final boolean addConnectionListener(LWSConnectionListener lis) {
        return getCommandReceiver().addConnectionListener(lis);
    }

    public final boolean removeConnectionListener(LWSConnectionListener lis) {
        return getCommandReceiver().removeConnectionListener(lis);
    }
    
    public final void notifyConnectionListeners(boolean isConnected) {
        getCommandReceiver().setConnected(isConnected);
    }
    
    /**
     * Override this to create the {@link Handler}s to use.
     * 
     * @return the list of handlers that will take your requests.
     */
    protected abstract Handler[] createHandlers();
    
    /* (non-Javadoc)
     * @see org.limewire.store.server.Dispatcher#setDispatchee(org.limewire.store.server.Dispatchee)
     */
    public final void setCommandReceiver(LWSReceivesCommandsFromDispatcher commandReceiver) {
        this.commandReceiver = commandReceiver;
    }

    /**
     * Returns the {@link LWSReceivesCommandsFromDispatcher} instance.
     * 
     * @return the {@link LWSReceivesCommandsFromDispatcher} instance
     */
    final LWSReceivesCommandsFromDispatcher getCommandReceiver() {
        return commandReceiver;
    }
    
    /**
     * Create an instance of Handler from the top level name as well as trying a
     * static inner class and calls its {@link Handler#handle()} method.
     */
    public final void handle(String request, PrintStream out, StringCallback callback) {
        final String req = getCommand(request);
        if (isAuthenticated() && req.equals(Commands.PING)) {
            note("Handling PING");
            callback.process(new String(PING_BYTES));
            notifyConnectionListeners(true);
            return;
        }
        final Handler h = names2handlers.get(req.toLowerCase(Locale.US));
        if (h == null) {
            if (LOG.isErrorEnabled()) LOG.error("Couldn't create a handler for " + req);
            callback.process(report(LWSDispatcherSupport.ErrorCodes.UNKNOWN_COMMAND));
            return;
        }
        if (LOG.isDebugEnabled()) LOG.debug("have handler: " + h.name());
        final Map<String, String> args = getArgs(request);
        h.handle(args, callback);
    }    

    final void note(String pattern, Object... os) {
        if (LOG.isDebugEnabled()) LOG.info(MessageFormat.format(pattern, os));
    }    
   
    /**
     * Wraps the message <tt>error</tt> in the call back
     * {@link Constants.ERROR_CALLBACK}. <br>
     * Example: If the error message is <tt>"You stink!"</tt> the wrapped
     * message would be <tt>error("You stink!")</tt>.
     * 
     * @param error the error message
     * @return the message <tt>error</tt> in the call back
     */
    public static final String report(String error) {
        return wrapCallback(LWSDispatcherSupport.Constants.ERROR_CALLBACK, 
                            LWSServerUtil.wrapError(error));
    }

    /**
     * Wraps the message <tt>msg</tt> using callback function
     * <tt>callback</tt>. The msesage is surrounded by
     * {@link Constants.CALLBACK_QUOTE}s and all quotes in the message,
     * {@link Constants.CALLBACK_QUOTE}, are escaped.
     * 
     * @param callback the function in which <tt>msg</tt> is wrapped, this can
     *        be <code>null</code>
     * @param msg the message
     * @return the message <tt>msg</tt> using callback function
     *         <tt>callback</tt>
     */
    public static final String wrapCallback(final String callback, final String msg) {
        if (LWSServerUtil.isEmpty(callback)) {
            return msg;
        } else {
            char q = LWSDispatcherSupport.Constants.CALLBACK_QUOTE;
            String s = LWSDispatcherSupport.Constants.CALLBACK_QUOTE_STRING;
            return callback + "(" + q + (msg == null ? "" : msg.replace(s, "\\" + s)) + q + ")";
        }
    }

    /**
     * Something with a name.
     */
    abstract static class HasName {

        private final String name;

        public HasName(final String name) {
            this.name = name;
        }

        public HasName() {
            String n = getClass().getName();
            int ilast;
            ilast = n.lastIndexOf(".");
            if (ilast != -1)
                n = n.substring(ilast + 1);
            ilast = n.lastIndexOf("$");
            if (ilast != -1)
                n = n.substring(ilast + 1);
            this.name = n;
        }

        public final String name() {
            return name;
        }

    }

    // ------------------------------------------------------------
    // Handlers and Listeners
    // ------------------------------------------------------------
    
    /**
     * Handles commands.
     */
    protected interface Handler {

        /**
         * Perform some operation on the incoming message and return the result.
         * 
         * @param args CGI params
         * @param callback TODO
         */
        void handle(Map<String, String> args, StringCallback callback);

        /**
         * Returns the unique name of this instance.
         * 
         * @return the unique name of this instance
         */
        String name();
    }
    
    /**
     * Handles commands, but does NOT return a result.
     */
    interface Listener {

        /**
         * Perform some operation on the incoming message.
         * 
         * @param args CGI params
         * @param req incoming {@link Request}
         */
        void handle(Map<String, String> args);

        /**
         * Returns the unique name of this instance.
         * 
         * @return the unique name of this instance
         */
        String name();     
    }    
    
    /**
     * Generic base class for {@link Handler}s.
     */
    protected abstract static class AbstractHandler extends HasName implements Handler {
        protected AbstractHandler(String name) {
            super(name);
        }
        protected AbstractHandler() {
            super();
        }
        protected String report(String msg) {
            return LWSDispatcherSupport.report(msg);
        }
    }
    
    /**
     * Generic base class for {@link Listener}s.
     */
    protected abstract static class AbstractListener extends HasName implements Listener {
        protected AbstractListener(String name) {
            super(name);
        }
        protected AbstractListener() {
            super();
        }
    }

    /**
     * A {@link Handler} requiring a callback specified by the
     * parameter {@link Parameters#CALLBACK}.
     */
    protected abstract class HandlerWithCallback extends AbstractHandler {

        public final void handle(final Map<String, String> args, final StringCallback cb) {
            final String callback = args.get(LWSDispatcherSupport.Parameters.CALLBACK);
            if (callback == null) {
                cb.process(report(LWSDispatcherSupport.ErrorCodes.MISSING_CALLBACK_PARAMETER));
                return;
            }
            //
            // We want to make sure to check if the result is an error.  In which case
            // we want to wrap it in the error callback, rather than the normal one
            //
            handleRest(args, new StringCallback() {

                public void process(String res) {
                    String str;
                    if (LWSServerUtil.isError(res)) {
                        str = LWSDispatcherSupport.wrapCallback(Constants.ERROR_CALLBACK, res);
                    } else {
                        str = LWSDispatcherSupport.wrapCallback(callback, res);
                    }  
                    cb.process(str);
                }
                
            });
        }

        /**
         * Returns the result <b>IN PLAIN TEXT</b>. Override this to provide
         * functionality after the {@link Parameters#CALLBACK} argument has been
         * extracted. This method should <b>NOT</b> wrap the result in the
         * callback, nor should it be called from any other method except this
         * abstract class.
         * 
         * <br/><br/>
         * 
         * Instances of this class
         * must not use {@link #report(String)}, and <b>must</b> only pass back
         * error codes from {@link ErrorCodes}.  To ensure that {@link #report(String)}
         * is implemented to throw a {@link RuntimeException}.
         * 
         * @param args original, untouched arguments
         * @return result <b>IN PLAIN TEXT</b>
         */
        protected abstract void handleRest(Map<String, String> args, StringCallback callback);
        
        /**
         * Overrides {@link AbstractHandler#report(String)} by simply wrapping
         * the error with the error prefix as defined in {@link LWSServerUtil#wrapError(String)}
         * so that we don't wrap it in a callback.
         */
        @Override
        protected final String report(String error) {
            return LWSServerUtil.wrapError(error);
        }
    }
    
    /**
     * Something that can open an input stream on behalf of this component. The
     * default implementation would be <code>new URL(url).openConnection()</code>
     * but due to the <em>core's</em> connection manager we'll have to have a
     * way of hooking into that without this component knowing about it.
     * 
     * @see URLSocketOpenner
     */
    interface OpensSocket {
        
        /**
         * Opens a connection based on the passed in URL <code>url</code>.
         * 
         * @param host  URL to open
         * @param port TODO
         * @return a connection based on the passed in URL <code>url</code>.
         * @throws IOException if an IO error occurs
         */
        Socket open(String host, int port) throws IOException;
    }

    /**
     * Collection of all the commands we send.
     */
    public interface Commands {
    
        /**
         * Sent from Code to Local with no parameters.
         */
        String START_COM = "StartCom";
    
        /**
         * Sent from Local to Remote with parameters.
         * <ul>
         * <li>{@link LocalServer.Parameters#PUBLIC}</li>
         * <li>{@link LocalServer.Parameters#PRIVATE}</li>
         * </ul>
         */
        String STORE_KEY = "StoreKey";
    
        /**
         * Sent from Code to Remote with parameters.
         * <ul>
         * <li>{@link LocalServer.Parameters#PRIVATE}</li>
         * </ul>
         */
        String GIVE_KEY = "GiveKey";
    
        /**
         * Send from Code to Local with no parameters.
         */
        String DETATCH = "Detatch";
    
        /**
         * Sent from Code to Local with parameters.
         * <ul>
         * <li>{@link LocalServer.Parameters#PRIVATE}</li>
         * </ul>
         */
        String AUTHENTICATE = "Authenticate";
        
        /**
         * Sent from Code to Local no parameters for sending back the special ping response.
         */
        String PING = "Ping";        
        
        /**
         * Sent from Code to Local with parameters.
         * <ul>
         * <li>{@link LocalServer.Parameters#COMMAND}</li>
         * </ul>
         * for executing an actual command.
         */
        String MSG = "Msg";
    }

    /**
     * Parameter names.
     */
    public interface Parameters {
    
        /**
         * Name of the callback function.
         */
        String CALLBACK = "callback";
    
        /**
         * Private key.
         */
        String PRIVATE = "private";
    
        /**
         * Public key.
         */
        String PUBLIC = "public";
    
        /**
         * Shared key.
         */
        String SHARED = "shared";        
    
        /**
         * Name of the command to send to the {@link LWSReceivesCommandsFromDispatcher}.
         */
        String COMMAND = "command";
    
        /**
         * Message to send to the <tt>ECHO</tt> command.
         */
        String MSG = "msg";
    
        /**
         * Name of a URL.
         */
        String URL = "url";
        
        /**
         * IP to store. This is sent in StoreKey from the local server to the
         * remote server, but in the real system will be ignored.
         */
        String IP = "ip";
    
    }

    /**
     * Codes that are sent to the code (javascript) when an error occurs.
     * NOTE: All errors have dots in them: https://www.limewire.org/fisheye/cru/LWCR-109/review#c3737
     */
    public interface ErrorCodes {
    
        /**
         * Indicating an invalid public key.
         */
        String INVALID_PUBLIC_KEY = "invalid.public.key";
    
        /**
         * Indicating an invalid private key.
         */
        String INVALID_PRIVATE_KEY = "invalid.private.key";
    
        /**
         * Indicating an invalid shared key.
         */
        String INVALID_SHARED_KEY = "invalid.shared.key";        
    
        /**
         * Indicating an invalid public key or IP address.
         */
        String INVALID_PUBLIC_KEY_OR_IP = "invalid.public.key.or.ip.address";
    
        /**
         * Indicating the code has not included a callback parameter.
         */
        String MISSING_CALLBACK_PARAMETER = "missing.callback.parameter";
    
        /**
         * A command was not understood or did not have valid handler or
         * listener.
         */
        String UNKNOWN_COMMAND = "unknown.command";
    
        /**
         * No private key has been generated yet.
         */
        String UNITIALIZED_PRIVATE_KEY = "uninitialized.private.key";
    
        /**
         * No private key parameter was supplied.
         */
        String MISSING_PRIVATE_KEY_PARAMETER = "missing.private.parameter";
    
        /**
         * No shared key has been generated yet.
         */
        String UNITIALIZED_SHARED_KEY = "uninitialized.shared.key";
                
        /**
         * No shared key parameter was supplied.
         */
        String MISSING_SHARED_KEY_PARAMETER = "missing.shared.parameter";        
    
        /**
         * No public key parameter was supplied.
         */
        String MISSING_PUBLIC_KEY_PARAMETER = "missing.public.parameter";
    
        /**
         * No command parameter was supplied to decide on a handler.
         */
        String MISSING_COMMAND_PARAMETER = "missing.command.parameter";
    
        /**
         * No IP was given.  In ths real system this will not be used.
         */
        String MISSING_IP_PARAMETER = "missing.ip.parameter";
        
        /**
         * A parameter was missing.
         */
        String MISSING_PARAMETER = "missing.parameter";
    
    }

    /**
     * Reponses sent back from servers.
     */
    public interface Responses {
    
        /**
         * Success.
         */
        String OK = "ok";
    
        /**
         * When there was a command sent to the local host, but no
         * {@link LWSReceivesCommandsFromDispatcher} was set up to handle it.
         */
        String NO_DISPATCHEE = "no.dispatcher";
    
        /**
         * When there was a {@link LWSReceivesCommandsFromDispatcher} to handle this command, but it
         * didn't understand it.
         */
        String UNKNOWN_COMMAND = "unknown.command";
    
    }

    /**
     * A general place for constants.
     */
    public interface Constants {
    
        /**
         * The length of the public and private keys generated.
         */
        int KEY_LENGTH = 10; // TODO
    
        /**
         * Carriage return and line feed.
         */
        String NEWLINE = "\r\n";
    
        /**
         * The quote used to surround callbacks. We need to escape this in the
         * strings that we pass back to the callback.
         */
        char CALLBACK_QUOTE = '\'';
    
        /**
         * The String version of {@link #CALLBACK_QUOTE}.
         */
        String CALLBACK_QUOTE_STRING = String.valueOf(CALLBACK_QUOTE);
    
        /**
         * The callback in which error messages are wrapped.
         */
        String ERROR_CALLBACK = "error";
    
        /**
         * The string that separates arguments in the {@link Parameters#MSG}
         * argument when the command {@link Parameters#COMMAND} parameter is
         * <tt>Msg</tt>. This is the urlencoded version of <tt>&</tt>,
         * which is <tt>%26</tt>.
         */
        String ARGUMENT_SEPARATOR = "%26";
    
    }

}