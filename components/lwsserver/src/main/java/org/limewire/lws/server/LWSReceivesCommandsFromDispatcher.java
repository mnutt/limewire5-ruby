package org.limewire.lws.server;

import java.util.Map;

/**
 * Implementations of this class are responsible for Dispatching commands after
 * the process of authentication has gone through. A {@link LWSDispatcher} will
 * recieve messages, listening to a web server, perform authentication, and then
 * call {@link #receiveCommand(String, Map)} with commands that come after
 * authentication. When receiving these commands, an implementation should take
 * the appropriate action for that command and arguments. <br/> <br/> Example:
 * Authentation has been performed and a {@link LWSDispatcher} receives the
 * following URL
 * 
 * <pre>
 * http://localhost:8091/store:Msg?command=DownloadSong&hash=123&amp;code=456&amp;privateKey=abc
 * </pre>
 * 
 * For a given {@link LWSReceivesCommandsFromDispatcher}, <code>d</code>, the {@link LWSDispatcher}
 * will then construct a {@link Map} of arguments, <code>args</code>
 * 
 * <pre>
 * Map&lt;String, String&gt; args = new HashMap&lt;String, String&gt;();
 * args.put(&quot;hash&quot;, &quot;123&quot;);
 * args.put(&quot;code&quot;, &quot;456&quot;);
 * </pre>
 * 
 * and call {@link performCommand} such as
 * 
 * <pre>
 * d.performCommand(&quot;DownloadSong&quot;, args);
 * </pre>
 * 
 * Note: The <code>privateKey</code> is not passed along, because that is just
 * used for authentication.
 * 
 */
public interface LWSReceivesCommandsFromDispatcher {

    /**
     * Responds to <code>cmd</code> with arguments <code>args</code>.
     * 
     * @param cmd the command to which we this responds
     * @param args the arguments to <code>cmd</code>
     * @return to <code>cmd</code> with arguments <code>args</code>
     */
    String receiveCommand(String cmd, Map<String, String> args);
    
    /**
     * Called when we're connected from the server, and <code>this</code>
     * should notify all the {@link LWSConnectionListener}s added by
     * {@link #addConnectionListener(LWSConnectionListener)}.
     * 
     * @param isConnected whether we're connected when a connection changes
     */
    void setConnected(boolean isConnected);
    
    /**
     * Returns <code>true</code> if <code>lis</code> was added as a listener,
     * <code>false</code> otherwise.
     * 
     * @param lis new listener
     * @return <code>true</code> if <code>lis</code> was added as a listener,
     *         <code>false</code> otherwise.
     */
    boolean addConnectionListener(LWSConnectionListener lis);

    /**
     * Returns <code>true</code> if <code>lis</code> was removed as a listener,
     * <code>false</code> otherwise.
     * 
     * @param lis old listener
     * @return <code>true</code> if <code>lis</code> was removed as a listener,
     *         <code>false</code> otherwise.
     */
    boolean removeConnectionListener(LWSConnectionListener lis);    

}